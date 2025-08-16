package com.foodiehub.foodiesapi.entities;

public record AddToCartRequest(
        Long foodId,
        Integer quantity
) {
}
