package com.iris.nutrition.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "food_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodEntry {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String name;

    private String brand;

    @Column(name = "meal_type", nullable = false)
    private String mealType;   // breakfast, lunch, dinner, snack, drink

    @Column(name = "serving_size", nullable = false)
    private BigDecimal servingSize;

    @Column(name = "serving_unit", nullable = false)
    private String servingUnit;

    @Column(nullable = false)
    private BigDecimal calories;

    @Column(name = "protein_g", nullable = false)
    private BigDecimal proteinG;

    @Column(name = "carbs_g", nullable = false)
    private BigDecimal carbsG;

    @Column(name = "fat_g", nullable = false)
    private BigDecimal fatG;

    @Column(name = "fiber_g", nullable = false)
    private BigDecimal fiberG;

    @Column(name = "sugar_g")
    private BigDecimal sugarG;

    @Column(name = "sodium_mg")
    private BigDecimal sodiumMg;

    @Column(name = "caffeine_mg", nullable = false)
    private BigDecimal caffeineMg;

    @Column(name = "fluid_ml")
    private BigDecimal fluidMl;

    private String notes;

    @Column(name = "consumed_at", nullable = false)
    private Instant consumedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
