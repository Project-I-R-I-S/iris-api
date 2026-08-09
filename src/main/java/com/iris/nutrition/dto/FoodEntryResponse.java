package com.iris.nutrition.dto;

import com.iris.nutrition.model.FoodEntry;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FoodEntryResponse(
        UUID id,
        String name,
        String brand,
        String mealType,
        BigDecimal servingSize,
        String servingUnit,
        BigDecimal calories,
        BigDecimal proteinG,
        BigDecimal carbsG,
        BigDecimal fatG,
        BigDecimal fiberG,
        BigDecimal sugarG,
        BigDecimal sodiumMg,
        BigDecimal caffeineMg,
        BigDecimal fluidMl,
        String notes,
        Instant consumedAt
) {
    public static FoodEntryResponse from(FoodEntry e) {
        return new FoodEntryResponse(
                e.getId(), e.getName(), e.getBrand(), e.getMealType(),
                e.getServingSize(), e.getServingUnit(),
                e.getCalories(), e.getProteinG(), e.getCarbsG(), e.getFatG(),
                e.getFiberG(), e.getSugarG(), e.getSodiumMg(), e.getCaffeineMg(),
                e.getFluidMl(), e.getNotes(), e.getConsumedAt()
        );
    }
}
