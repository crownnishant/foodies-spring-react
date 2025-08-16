package com.foodiehub.foodiesapi.service;

import com.foodiehub.foodiesapi.io.OrderRequest;
import com.foodiehub.foodiesapi.io.OrderResponse;
import com.razorpay.RazorpayException;

import java.util.List;
import java.util.Map;

public interface OrderService {

    OrderResponse createOrderWithPayment(OrderRequest request) throws RazorpayException;
    void verifyPayment(Map<String, String> paymentData, String status);

//list of orders for the logged-in user
    List<OrderResponse> getUserOrders();
    void removeUserOrders(String orderId);

//list of all orders present in DB for Admin panel
     List<OrderResponse> getOrdersOfAllUsers();

//update Order status by Admin panel
     void updateOrderStatus(String orderId, String status);
}
