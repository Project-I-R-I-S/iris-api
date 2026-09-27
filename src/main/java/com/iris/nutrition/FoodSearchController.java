package com.iris.nutrition;

import com.iris.nutrition.dto.FoodSearchResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Food lookup for the Add Food screen's search-to-prepopulate flow (see
 * {@code FoodSearchService}). Sits under {@code /api/v1/nutrition} since it's
 * a capability of the nutrition feature, not its own feature — falls under
 * the same {@code anyRequest().authenticated()} rule as every other endpoint,
 * no SecurityConfig change needed.
 */
@RestController
@RequestMapping("/api/v1/nutrition/food-search")
public class FoodSearchController {

    private final FoodSearchService foodSearchService;

    public FoodSearchController(FoodSearchService foodSearchService) {
        this.foodSearchService = foodSearchService;
    }

    @GetMapping
    public List<FoodSearchResult> search(@RequestParam String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return foodSearchService.search(query);
    }
}
