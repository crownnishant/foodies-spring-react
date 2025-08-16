package com.foodiehub.foodiesapi.service.impl;

import com.foodiehub.foodiesapi.entities.CartEntity;
import com.foodiehub.foodiesapi.entities.RemoveItemRequest;
import com.foodiehub.foodiesapi.io.CartRequest;
import com.foodiehub.foodiesapi.io.CartResponse;
import com.foodiehub.foodiesapi.repository.CartRepository;
import com.foodiehub.foodiesapi.service.CartService;
import com.foodiehub.foodiesapi.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final UserService userService;

    // ----------------- ADD (MERGE/INCREMENT) -----------------
    // Keep as a partial "add" API (do NOT use this to sync the full cart)
    @Transactional
    @Override
    public CartResponse addToCart(CartRequest request) {
        Long userId = userService.findByUserId();

        CartEntity cart = cartRepository.findByUserId(userId).orElseGet(() -> {
            CartEntity c = new CartEntity();
            c.setUserId(userId);
            c.setItems(new HashMap<>());
            return c;
        });

        if (request != null && request.getItems() != null) {
            for (Map.Entry<String, Integer> e : request.getItems().entrySet()) {
                String key = e.getKey();
                Integer qty = e.getValue();
                if (qty == null || qty <= 0) continue;

                Long foodId;
                try {
                    foodId = Long.valueOf(key);
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Invalid foodId key: " + key);
                }

                // increment/merge ONLY the provided entries
                cart.getItems().merge(foodId, qty, Integer::sum);
            }
        }

        CartEntity saved = cartRepository.save(cart);
        return toResponse(saved);
    }

    // ----------------- SAVE (REPLACE) -----------------
    // NEW: Replace stored cart with exactly what client sends
    @Transactional
    @Override
    public CartResponse saveCart(CartRequest request) {
        Long userId = userService.findByUserId();

        CartEntity cart = cartRepository.findByUserId(userId).orElseGet(() -> {
            CartEntity c = new CartEntity();
            c.setUserId(userId);
            c.setItems(new HashMap<>());
            return c;
        });

        Map<Long, Integer> fresh = new HashMap<>();
        if (request != null && request.getItems() != null) {
            for (Map.Entry<String, Integer> e : request.getItems().entrySet()) {
                Integer qty = e.getValue();
                if (qty == null || qty <= 0) continue;

                Long foodId;
                try {
                    foodId = Long.valueOf(e.getKey());
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Invalid foodId key: " + e.getKey());
                }
                fresh.put(foodId, qty);
            }
        }

        cart.setItems(fresh); // <-- REPLACE, do not merge
        CartEntity saved = cartRepository.save(cart);
        return toResponse(saved);
    }

    // ----------------- GET -----------------
    @Override
    public CartResponse getCart() {
        Long userId = userService.findByUserId();
        CartEntity entity = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    CartEntity c = new CartEntity();
                    c.setUserId(userId);
                    c.setItems(new HashMap<>());
                    return c;
                });
        return toResponse(entity);
    }

    // ----------------- CLEAR -----------------
    @Transactional
    @Override
    public void clearCart() {
        Long userId = userService.findByUserId();
        cartRepository.deleteByUserId(userId);
    }

    // ----------------- DECREMENT BY 1 -----------------
    @Transactional
    @Override
    public CartResponse removeFromCart(RemoveItemRequest request) {
        Long userId = userService.findByUserId();
        CartEntity entity = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));

        if (request == null || request.getFoodId() == null) {
            throw new IllegalArgumentException("foodId is required");
        }

        Map<Long, Integer> items = entity.getItems();
        Long foodId = request.getFoodId();

        Integer current = items.get(foodId);
        if (current != null) {
            int remaining = current - 1;
            if (remaining > 0) items.put(foodId, remaining);
            else items.remove(foodId);
            entity = cartRepository.save(entity);
        }
        return toResponse(entity);
    }

    // ----------------- REMOVE ITEM COMPLETELY -----------------
    @Transactional
    @Override
    public CartResponse removeItemCompletely(RemoveItemRequest request) {
        Long userId = userService.findByUserId();
        CartEntity entity = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));

        if (request == null || request.getFoodId() == null) {
            throw new IllegalArgumentException("foodId is required");
        }

        Map<Long, Integer> items = entity.getItems();
        items.remove(request.getFoodId());
        entity = cartRepository.save(entity);

        return toResponse(entity);
    }

    @Transactional
    @Override
    public CartResponse removeOne(Long foodId) {
        if (foodId == null) {
            throw new IllegalArgumentException("foodId is required");
        }

        Long userId = userService.findByUserId();
        CartEntity entity = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));

        Map<Long, Integer> items = entity.getItems();
        if (items == null) {
            items = new HashMap<>();
            entity.setItems(items);
        }

        // decrement by 1; remove key if goes to 0
        items.computeIfPresent(foodId, (k, v) -> (v != null && v > 1) ? v - 1 : null);

        entity = cartRepository.save(entity);
        return toResponse(entity);
    }


    // ----------------- DTO MAPPER -----------------
    private CartResponse toResponse(CartEntity entity) {
        Map<String, Integer> items = new HashMap<>();
        entity.getItems().forEach((k, v) -> items.put(String.valueOf(k), v));

        return CartResponse.builder()
                .id(entity.getId())
                .userId(String.valueOf(entity.getUserId()))
                .items(items)
                .build();
    }}