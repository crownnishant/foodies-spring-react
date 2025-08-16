package com.foodiehub.foodiesapi.repository;

import com.foodiehub.foodiesapi.entities.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, String> {

   List<OrderEntity> findByUserId(Long userId);
   Optional<OrderEntity> findByRazorPayOrderId(String razorpayOrderId);
}
