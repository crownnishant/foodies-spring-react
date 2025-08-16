package com.foodiehub.foodiesapi.io;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FoodResponse {
    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private Double price;
    private String category;
}
