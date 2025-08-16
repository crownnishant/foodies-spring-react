package com.foodiehub.foodiesapi.entities;

import com.foodiehub.foodiesapi.io.OrderItem;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Table(name = "orders")
public class OrderEntity {

    @Id
    private String id;
    @PrePersist
    public void prePersist(){
        this.id= UUID.randomUUID().toString();
    }
    private Long userId;
    private String userAddress;
    private String phoneNumber;
    private String email;

    @ElementCollection
    @CollectionTable(name = "order_items", joinColumns = @JoinColumn(name = "order_id"))
    private List<OrderItem> orderItems;
    private Double amount;
    private String paymentStatus;
    @Column(name = "razorpay_order_id", length = 40)
    private String razorPayOrderId;
    @Column(name = "razorpay_signature", length = 128)
    private String razorPaySignature;
    @Column(name = "razorpay_payment_id", length = 40)
    private String razorpayPaymentId;
    private String orderStatus;
}
