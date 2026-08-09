package com.iris.nutrition.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record FoodEntryRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 120) String brand,
        @NotBlank String mealType,
        @NotNull @PositiveOrZero BigDecimal servingSize,
        @NotBlank String servingUnit,
        @NotNull @PositiveOrZero BigDecimal calories,
        @PositiveOrZero BigDecimal proteinG,
        @PositiveOrZero BigDecimal carbsG,
        @PositiveOrZero BigDecimal fatG,
        @PositiveOrZero BigDecimal fiberG,
        @PositiveOrZero BigDecimal sugarG,
        @PositiveOrZero BigDecimal sodiumMg,
        @PositiveOrZero BigDecimal caffeineMg,
        @PositiveOrZero BigDecimal fluidMl,
        @Size(max = 500) String notes,
        @NotNull Instant consumedAt
) { }
