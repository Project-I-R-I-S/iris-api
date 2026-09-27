package com.iris.nutrition;

import com.iris.common.security.AuthenticatedUser;
import com.iris.common.security.JwtAuthenticationFilter;
import com.iris.nutrition.dto.FoodSearchResult;
import com.iris.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FoodSearchController.class)
@AutoConfigureMockMvc(addFilters = false)
class FoodSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FoodSearchService foodSearchService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter; // keeps the slice from constructing the real filter
    @MockBean
    private UserService userService;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void authenticate() {
        var principal = new AuthenticatedUser(userId, "user@example.com");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void search_returnsResultsFromTheService() throws Exception {
        FoodSearchResult result = new FoodSearchResult(
                "usda:173944", "Chicken, broiler or fryers, breast, meat only, raw", null,
                new BigDecimal("100"), "g",
                new BigDecimal("120.0"), new BigDecimal("22.5"), new BigDecimal("0.0"), new BigDecimal("2.62"),
                null, null, new BigDecimal("45.0"), null);
        when(foodSearchService.search("chicken breast")).thenReturn(List.of(result));

        mockMvc.perform(get("/api/v1/nutrition/food-search").param("query", "chicken breast"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sourceId").value("usda:173944"))
                .andExpect(jsonPath("$[0].name").value("Chicken, broiler or fryers, breast, meat only, raw"));

        verify(foodSearchService).search("chicken breast");
    }

    @Test
    void search_returnsEmptyListWithoutCallingTheServiceWhenQueryIsBlank() throws Exception {
        mockMvc.perform(get("/api/v1/nutrition/food-search").param("query", "  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verifyNoInteractions(foodSearchService);
    }

    @Test
    void search_returns400WhenQueryParamIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/nutrition/food-search"))
                .andExpect(status().isBadRequest());
    }
}
