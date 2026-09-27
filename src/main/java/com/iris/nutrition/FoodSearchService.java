package com.iris.nutrition;

import com.iris.nutrition.dto.FoodSearchResult;
import com.iris.nutrition.dto.UsdaSearchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Looks up food nutrition data so the Add Food screen can prepopulate
 * calories/macros for a food the user picks, instead of the user typing them
 * in by hand (see {@code FoodSearchController}). USDA FoodData Central is the
 * only source wired up for now; more sources (Nutritionix, Edamam) can be
 * added later as additional adapters behind {@link #search}, returning the
 * same {@link FoodSearchResult} shape, once API keys for them are available.
 */
@Service
public class FoodSearchService {

    private static final int MAX_RESULTS = 15;
    private static final int CACHE_MAX_ENTRIES = 200;
    private static final long CACHE_TTL_MILLIS = 10 * 60 * 1000L;

    // Mirrors AddFoodScreen.tsx's SERVING_UNITS list — USDA's servingSizeUnit
    // is normalized onto this set so the mobile serving-unit dropdown always
    // has a matching value to preselect.
    private static final List<String> KNOWN_UNITS =
            List.of("g", "kg", "mg", "ml", "l", "oz", "lb", "cup", "tbsp", "tsp", "piece(s)", "slice(s)", "serving");

    // USDA nutrientName -> the FoodSearchResult field it fills. Matched by
    // name rather than nutrientId, since ids aren't perfectly stable across
    // USDA's Foundation/SR Legacy/Branded food data types.
    private static final Map<String, String> NUTRIENT_NAME_TO_FIELD = Map.of(
            "Energy", "calories",
            "Protein", "proteinG",
            "Carbohydrate, by difference", "carbsG",
            "Total lipid (fat)", "fatG",
            "Fiber, total dietary", "fiberG",
            "Sugars, total including NLEA", "sugarG",
            "Sodium, Na", "sodiumMg",
            "Caffeine", "caffeineMg"
    );

    private final RestClient usdaClient;
    private final String apiKey;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public FoodSearchService(RestClient.Builder restClientBuilder,
                              @Value("${iris.nutrition.usda.base-url}") String baseUrl,
                              @Value("${iris.nutrition.usda.api-key}") String apiKey) {
        this.usdaClient = restClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    public List<FoodSearchResult> search(String query) {
        String key = query.trim().toLowerCase();
        CacheEntry cached = cache.get(key);
        if (cached != null && cached.isFresh()) {
            return cached.results();
        }

        UsdaSearchResponse response = usdaClient.get()
                .uri(uriBuilder -> uriBuilder.path("/foods/search")
                        .queryParam("query", key)
                        .queryParam("pageSize", MAX_RESULTS)
                        .queryParam("api_key", apiKey)
                        .build())
                .retrieve()
                .body(UsdaSearchResponse.class);

        List<FoodSearchResult> results = response == null || response.foods() == null
                ? List.of()
                : response.foods().stream().map(this::toResult).toList();

        cacheResults(key, results);
        return results;
    }

    private FoodSearchResult toResult(UsdaSearchResponse.UsdaFood food) {
        Map<String, BigDecimal> macros = new HashMap<>();
        if (food.foodNutrients() != null) {
            for (UsdaSearchResponse.UsdaNutrient nutrient : food.foodNutrients()) {
                String field = NUTRIENT_NAME_TO_FIELD.get(nutrient.nutrientName());
                if (field != null && nutrient.value() != null) {
                    macros.put(field, nutrient.value());
                }
            }
        }

        BigDecimal baseServingSize = food.servingSize() != null ? food.servingSize() : BigDecimal.valueOf(100);

        return new FoodSearchResult(
                "usda:" + food.fdcId(),
                food.description(),
                food.brandOwner(),
                baseServingSize,
                normalizeUnit(food.servingSizeUnit()),
                macros.get("calories"),
                macros.get("proteinG"),
                macros.get("carbsG"),
                macros.get("fatG"),
                macros.get("fiberG"),
                macros.get("sugarG"),
                macros.get("sodiumMg"),
                macros.get("caffeineMg")
        );
    }

    private String normalizeUnit(String usdaUnit) {
        if (usdaUnit == null) return "g";
        String lower = usdaUnit.trim().toLowerCase();
        return KNOWN_UNITS.contains(lower) ? lower : "g";
    }

    private void cacheResults(String key, List<FoodSearchResult> results) {
        if (cache.size() >= CACHE_MAX_ENTRIES && !cache.containsKey(key)) {
            cache.entrySet().stream()
                    .min((a, b) -> Long.compare(a.getValue().cachedAtMillis(), b.getValue().cachedAtMillis()))
                    .map(Map.Entry::getKey)
                    .ifPresent(cache::remove);
        }
        cache.put(key, new CacheEntry(results, System.currentTimeMillis()));
    }

    private record CacheEntry(List<FoodSearchResult> results, long cachedAtMillis) {
        boolean isFresh() {
            return System.currentTimeMillis() - cachedAtMillis < CACHE_TTL_MILLIS;
        }
    }
}
