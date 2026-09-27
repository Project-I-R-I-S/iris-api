package com.iris.nutrition.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

/**
 * Raw shape of USDA FoodData Central's {@code GET /foods/search} response —
 * kept private to {@code FoodSearchService}, which maps it onto
 * {@link FoodSearchResult}. Only the fields we actually use are declared;
 * everything else is ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UsdaSearchResponse(List<UsdaFood> foods) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UsdaFood(
            long fdcId,
            String description,
            String brandOwner,
            BigDecimal servingSize,
            String servingSizeUnit,
            List<UsdaNutrient> foodNutrients
    ) { }

    // Matched by nutrientName rather than nutrientId in FoodSearchService,
    // since numeric ids aren't perfectly stable across USDA's Foundation/SR
    // Legacy/Branded food data types.
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UsdaNutrient(
            String nutrientName,
            String unitName,
            BigDecimal value
    ) { }
}
