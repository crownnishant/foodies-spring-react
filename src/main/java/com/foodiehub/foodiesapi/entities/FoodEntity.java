package com.foodiehub.foodiesapi.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "foods")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class FoodEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "price", nullable = false)
    private Double price;

    @Column(name = "category", nullable = false)
    private String category;
    private String imageUrl;
    private String imageKey;
}
