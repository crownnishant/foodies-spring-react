package com.foodiehub.foodiesapi.io;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OrderRequest {
    private List<OrderItem> orderItems;
    private String userAddress;
    private Double amount;
    private String email;
    private String phoneNumber;
    private String orderStatus;
}
