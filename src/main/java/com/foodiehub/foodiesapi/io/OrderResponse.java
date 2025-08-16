package com.foodiehub.foodiesapi.io;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderResponse {
    private String id;
    private Long userId;
    private String userAddress;
    private String phoneNumber;
    private String email;
    private Double amount;                 // rupees (for display)
    private Long amountInPaise;            // NEW: integer paise for Razorpay
    private String currency;               // NEW: e.g., "INR"
    private String paymentStatus;
    @JsonProperty("razorpayOrderId")
    private String razorPayOrderId;
    private String razorpayPaymentId;
    private String orderStatus;
    private List<OrderItem> orderedItems;
}
