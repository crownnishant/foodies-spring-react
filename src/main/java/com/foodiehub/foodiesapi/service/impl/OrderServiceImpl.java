package com.foodiehub.foodiesapi.service.impl;

import com.foodiehub.foodiesapi.entities.OrderEntity;
import com.foodiehub.foodiesapi.io.OrderItem;
import com.foodiehub.foodiesapi.io.OrderRequest;
import com.foodiehub.foodiesapi.io.OrderResponse;
import com.foodiehub.foodiesapi.repository.CartRepository;
import com.foodiehub.foodiesapi.repository.OrderRepository;
import com.foodiehub.foodiesapi.service.OrderService;
import com.foodiehub.foodiesapi.service.UserService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserService userService;
    private final CartRepository cartRepository;

    @Value("${razorpay_key}")
    private String RAZORPAY_KEY;

    @Value("${razorpay_secret}")
    private String RAZORPAY_SECRET;

    // Match your UI calculation: shipping ₹10 (if subtotal > 0), tax = 10% of subtotal
    private static final BigDecimal SHIPPING_FLAT = new BigDecimal("10.00");
    private static final BigDecimal TAX_RATE = new BigDecimal("0.10"); // 10%

    @Override
    public OrderResponse createOrderWithPayment(OrderRequest request) throws RazorpayException {
        // 1) Recompute GRAND TOTAL (subtotal + shipping + tax) from request
        BigDecimal totalInRupees = computeTotalFromRequest(request);
        if (totalInRupees.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Order amount must be > 0");
        }

        // 2) Convert rupees → integer paise for Razorpay
        long amountInPaise = totalInRupees
                .movePointRight(2)                  // rupees → paise
                .setScale(0, RoundingMode.HALF_UP)  // integer paise
                .longValueExact();

        // 3) Create DB order first
        Long loggedInUserId = userService.findByUserId();
        OrderEntity newOrder = convertToEntity(request);
        newOrder.setUserId(loggedInUserId);
        newOrder.setAmount(totalInRupees.doubleValue()); // store rupees for display
        newOrder.setPaymentStatus("created");
        newOrder = orderRepository.save(newOrder);

        // 4) Create Razorpay order (integer paise, short receipt)
        RazorpayClient razorpayClient = new RazorpayClient(RAZORPAY_KEY, RAZORPAY_SECRET);

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInPaise);      // ✅ integer paise
        orderRequest.put("currency", "INR");
        orderRequest.put("payment_capture", 1);
        orderRequest.put("receipt", buildReceipt(newOrder.getId())); // ≤ 40 chars

        JSONObject notes = new JSONObject();
        notes.put("db_order_id", newOrder.getId());
        notes.put("user_id", String.valueOf(loggedInUserId));
        orderRequest.put("notes", notes);

        Order razorpayOrder = razorpayClient.orders.create(orderRequest);

        // 5) Save Razorpay order id
        newOrder.setRazorPayOrderId(razorpayOrder.get("id"));
        newOrder = orderRepository.save(newOrder);

        // 6) Return to UI (includes paise + currency so modal shows ₹725 etc.)
        return convertToResponse(newOrder, amountInPaise, "INR");
    }

    @Override
    @Transactional
    public void verifyPayment(Map<String, String> paymentData, String ignoredStatusParam) {
        String orderId   = paymentData.get("razorpay_order_id");
        String paymentId = paymentData.get("razorpay_payment_id");
        String signature = paymentData.get("razorpay_signature");

        OrderEntity order = orderRepository.findByRazorPayOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with Razorpay Order ID: " + orderId));

        // Verify HMAC signature
        String body = orderId + "|" + paymentId;
        String expected = hmacSha256(body, RAZORPAY_SECRET);

        if (!expected.equals(signature)) {
            order.setPaymentStatus("failed");
            orderRepository.save(order);
            throw new IllegalArgumentException("Invalid Razorpay signature");
        }

        order.setPaymentStatus("paid");
        order.setRazorPaySignature(signature);
        order.setRazorpayPaymentId(paymentId);
        orderRepository.save(order);

        // Clear cart on successful payment
        cartRepository.deleteByUserId(order.getUserId());
    }

    @Override
    public List<OrderResponse> getUserOrders() {
        Long loggedInUserId = userService.findByUserId();
        List<OrderEntity> list = orderRepository.findByUserId(loggedInUserId);
        return list.stream()
                .map(o -> convertToResponse(o, null, null))
                .collect(Collectors.toList());
    }

    @Override
    public void removeUserOrders(String orderID) {
        orderRepository.deleteById(orderID);
    }

    @Override
    public List<OrderResponse> getOrdersOfAllUsers() {
        List<OrderEntity> list = orderRepository.findAll();
        return list.stream()
                .map(o -> convertToResponse(o, null, null))
                .collect(Collectors.toList());
    }

    @Override
    public void updateOrderStatus(String orderId, String status) {
        OrderEntity entity = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));
        entity.setOrderStatus(status);
        orderRepository.save(entity);
    }

    // ---------- helpers ----------

    /** Matches your UI: subtotal + shipping (₹10) + tax (10% of subtotal). */
    private BigDecimal computeTotalFromRequest(OrderRequest request) {
        if (request.getOrderItems() == null || request.getOrderItems().isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        // Subtotal = sum of line prices (UI sends unitPrice * quantity as price)
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItem it : request.getOrderItems()) {
            BigDecimal line = BigDecimal.valueOf(it.getPrice());
            subtotal = subtotal.add(line);
        }
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);

        // Shipping: ₹10 if any item, else ₹0
        BigDecimal shipping = subtotal.compareTo(BigDecimal.ZERO) > 0 ? SHIPPING_FLAT : BigDecimal.ZERO;

        // Tax: 10% of subtotal
        BigDecimal tax = subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);

        return subtotal.add(shipping).add(tax).setScale(2, RoundingMode.HALF_UP);
    }

    /** Keep receipt ≤ 40 chars; include sanitized DB ID. */
    private String buildReceipt(String dbOrderId) {
        String clean = dbOrderId == null ? "" : dbOrderId.replaceAll("[^A-Za-z0-9]", "");
        String prefix = "ord_"; // 4 chars
        int maxIdLen = 40 - prefix.length();
        if (clean.length() > maxIdLen) clean = clean.substring(0, maxIdLen);
        return prefix + clean;
    }

    private String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate HMAC", e);
        }
    }

    private OrderResponse convertToResponse(OrderEntity order, Long amountInPaise, String currency) {
        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .userAddress(order.getUserAddress())
                .phoneNumber(order.getPhoneNumber())
                .email(order.getEmail())
                .amount(order.getAmount())             // rupees for display
                .amountInPaise(amountInPaise)          // may be null for history endpoints
                .currency(currency)                    // may be null for history endpoints
                .paymentStatus(order.getPaymentStatus())
                .razorPayOrderId(order.getRazorPayOrderId())
                .razorpayPaymentId(order.getRazorpayPaymentId())
                .orderStatus(order.getOrderStatus())
                .orderedItems(order.getOrderItems())
                .build();
    }

    private OrderEntity convertToEntity(OrderRequest request) {
        return OrderEntity.builder()
                .userAddress(request.getUserAddress())
                // amount from client is ignored; we recompute on server
                .orderItems(request.getOrderItems())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .orderStatus(request.getOrderStatus())
                .build();
    }
}