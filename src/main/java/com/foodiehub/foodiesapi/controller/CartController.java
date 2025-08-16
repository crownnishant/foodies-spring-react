package com.foodiehub.foodiesapi.controller;

import com.foodiehub.foodiesapi.entities.RemoveItemRequest;
import com.foodiehub.foodiesapi.io.CartRequest;
import com.foodiehub.foodiesapi.io.CartResponse;
import com.foodiehub.foodiesapi.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cart")
@CrossOrigin("*")
public class CartController {

    private final CartService cartService;

    @GetMapping
    public CartResponse get() {
        return cartService.getCart(); }

    // Replace entire cart
    @PutMapping("/save")
    public CartResponse save(@RequestBody CartRequest request) {
        return cartService.saveCart(request);
    }

    // Add/increment
    @PostMapping("/add")
    public CartResponse add(@RequestBody CartRequest request) {
        return cartService.addToCart(request);
    }

    // Decrement via body (you may keep this if used)
    @PostMapping("/remove")
    public CartResponse remove(@RequestBody RemoveItemRequest req) {
        return cartService.removeFromCart(req);
    }

    // ✅ NEW: Decrement one via path variable (matches FE: DELETE /remove/{foodId})
    @DeleteMapping("/remove/{foodId}")
    public CartResponse removeOne(@PathVariable Long foodId) {
        return cartService.removeOne(foodId);
    }

    // Remove key completely via body (optional)
    @PostMapping("/remove-item")
    public CartResponse removeItem(@RequestBody RemoveItemRequest req) {
        return cartService.removeItemCompletely(req);
    }

    @DeleteMapping
    public void clear() { cartService.clearCart(); }
}
