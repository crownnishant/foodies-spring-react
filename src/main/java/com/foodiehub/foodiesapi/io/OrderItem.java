package com.foodiehub.foodiesapi.io;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Embeddable
public class OrderItem {
    private Long foodId;
    private int quantity;
    private Double price;
    private String category;
    private String imageUrl;
    private String description;
    private String name;
}
