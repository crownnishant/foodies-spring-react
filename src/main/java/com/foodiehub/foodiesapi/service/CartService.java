package com.foodiehub.foodiesapi.service;

import com.foodiehub.foodiesapi.entities.RemoveItemRequest;
import com.foodiehub.foodiesapi.io.CartRequest;
import com.foodiehub.foodiesapi.io.CartResponse;

public interface CartService {

    CartResponse addToCart(CartRequest request);
    CartResponse saveCart(CartRequest request);
    CartResponse getCart();
    void clearCart();
    CartResponse removeFromCart(RemoveItemRequest request);
    CartResponse removeItemCompletely(RemoveItemRequest request);
    CartResponse removeOne(Long foodId);
}
