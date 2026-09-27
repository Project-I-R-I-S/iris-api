package com.iris.nutrition.dto;

import java.math.BigDecimal;

/**
 * A food/drink match returned by {@code GET /api/v1/nutrition/food-search}, used
 * to prepopulate {@link FoodEntryRequest}'s nutrition fields when the user picks
 * a search result instead of typing calories/macros by hand. Fields mirror
 * {@link FoodEntryResponse} (minus the entry-specific id/mealType/notes/consumedAt)
 * so the mobile client can reuse one nutrition-fields shape for both.
 *
 * <p>{@code sourceId} is prefixed with the data source ("usda:173944") so more
 * sources (Nutritionix, Edamam) can be added later behind the same shape.
 */
public record FoodSearchResult(
        String sourceId,
        String name,
        String brand,
        BigDecimal baseServingSize,
        String baseServingUnit,
        BigDecimal calories,
        BigDecimal proteinG,
        BigDecimal carbsG,
        BigDecimal fatG,
        BigDecimal fiberG,
        BigDecimal sugarG,
        BigDecimal sodiumMg,
        BigDecimal caffeineMg
) { }
