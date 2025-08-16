package com.foodiehub.foodiesapi.repository;

import com.foodiehub.foodiesapi.entities.CartEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<CartEntity, Long> {

    Optional<CartEntity> findByUserId(Long userId);

    void deleteByUserId(Long userId);

}
