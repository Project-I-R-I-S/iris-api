package com.iris.nutrition;

import com.iris.nutrition.dto.FoodSearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FoodSearchServiceTest {

    private static final String BASE_URL = "https://api.nal.usda.gov/fdc/v1";

    private static final String CHICKEN_BREAST_JSON = """
            {
              "foods": [
                {
                  "fdcId": 173944,
                  "description": "Chicken, broiler or fryers, breast, meat only, raw",
                  "brandOwner": null,
                  "servingSize": 100,
                  "servingSizeUnit": "g",
                  "foodNutrients": [
                    {"nutrientName": "Energy", "unitName": "KCAL", "value": 120.0},
                    {"nutrientName": "Protein", "unitName": "G", "value": 22.5},
                    {"nutrientName": "Total lipid (fat)", "unitName": "G", "value": 2.62},
                    {"nutrientName": "Carbohydrate, by difference", "unitName": "G", "value": 0.0},
                    {"nutrientName": "Sodium, Na", "unitName": "MG", "value": 45.0}
                  ]
                }
              ]
            }
            """;

    private MockRestServiceServer server;
    private FoodSearchService foodSearchService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        foodSearchService = new FoodSearchService(builder, BASE_URL, "TEST_KEY");
    }

    private void expectSearchRequest(String query, String responseJson) {
        server.expect(method(GET))
                .andExpect(requestToUriTemplate(
                        BASE_URL + "/foods/search?query={query}&pageSize={pageSize}&api_key={apiKey}",
                        query, 15, "TEST_KEY"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));
    }

    @Test
    void search_mapsUsdaResponseOntoFoodSearchResult() {
        expectSearchRequest("chicken breast", CHICKEN_BREAST_JSON);

        List<FoodSearchResult> results = foodSearchService.search("chicken breast");

        assertThat(results).hasSize(1);
        FoodSearchResult result = results.get(0);
        assertThat(result.sourceId()).isEqualTo("usda:173944");
        assertThat(result.name()).isEqualTo("Chicken, broiler or fryers, breast, meat only, raw");
        assertThat(result.baseServingSize()).isEqualByComparingTo("100");
        assertThat(result.baseServingUnit()).isEqualTo("g");
        assertThat(result.calories()).isEqualByComparingTo("120.0");
        assertThat(result.proteinG()).isEqualByComparingTo("22.5");
        assertThat(result.fatG()).isEqualByComparingTo("2.62");
        assertThat(result.carbsG()).isEqualByComparingTo("0.0");
        assertThat(result.sodiumMg()).isEqualByComparingTo("45.0");
        assertThat(result.fiberG()).isNull();
        assertThat(result.caffeineMg()).isNull();

        server.verify();
    }

    @Test
    void search_cachesResultsForTheSameQueryCaseInsensitively() {
        // Only one HTTP call is expected — the second and third searches
        // below must be served from cache instead of hitting USDA again.
        expectSearchRequest("chicken breast", CHICKEN_BREAST_JSON);

        foodSearchService.search("Chicken Breast");
        foodSearchService.search("chicken breast");
        foodSearchService.search("  CHICKEN BREAST  ");

        server.verify();
    }

    @Test
    void search_returnsEmptyListWhenUsdaHasNoMatches() {
        expectSearchRequest("asdfghjkl", "{\"foods\": []}");

        List<FoodSearchResult> results = foodSearchService.search("asdfghjkl");

        assertThat(results).isEmpty();
    }
}
