package com.iris.nutrition;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iris.common.exception.ResourceNotFoundException;
import com.iris.common.security.AuthenticatedUser;
import com.iris.common.security.JwtAuthenticationFilter;
import com.iris.nutrition.dto.FoodEntryRequest;
import com.iris.nutrition.dto.FoodEntryResponse;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = NutritionController.class)
@AutoConfigureMockMvc(addFilters = false)
class NutritionControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NutritionService nutritionService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter; // keeps the slice from constructing the real filter

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

    private FoodEntryRequest validRequest() {
        return new FoodEntryRequest(
                "Banana", null, "snack",
                new BigDecimal("120"), "g", new BigDecimal("105"),
                null, null, null, null, null, null, null,
                null, null, Instant.parse("2026-08-25T08:00:00Z"));
    }

    @Test
    void create_returns201WithTheCreatedEntryScopedToTheAuthenticatedUser() throws Exception {
        FoodEntryResponse response = new FoodEntryResponse(
                UUID.randomUUID(), "Banana", null, "snack",
                new BigDecimal("120"), "g", new BigDecimal("105"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                null, null, BigDecimal.ZERO, null, null, Instant.parse("2026-08-25T08:00:00Z"));
        when(nutritionService.create(eq(userId), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/nutrition/entries")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Banana"));

        verify(nutritionService).create(eq(userId), any());
    }

    @Test
    void create_returns400WhenRequiredFieldsAreMissing() throws Exception {
        FoodEntryRequest invalid = new FoodEntryRequest(
                "", null, "", null, "", null,
                null, null, null, null, null, null, null,
                null, null, null);

        mockMvc.perform(post("/api/v1/nutrition/entries")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void listForDay_passesTheAuthenticatedUserAndParsedDateToTheService() throws Exception {
        ZoneId utc = ZoneId.of("UTC");
        when(nutritionService.listForDay(eq(userId), eq(LocalDate.of(2026, 8, 25)), eq(utc)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/nutrition/entries")
                        .param("date", "2026-08-25")
                        .param("timezone", "UTC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(nutritionService).listForDay(userId, LocalDate.of(2026, 8, 25), utc);
    }

    @Test
    void delete_returns204OnSuccess() throws Exception {
        UUID entryId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/nutrition/entries/{id}", entryId))
                .andExpect(status().isNoContent());

        verify(nutritionService).delete(userId, entryId);
    }

    @Test
    void delete_returns404WhenTheEntryIsNotOwnedByTheUser() throws Exception {
        UUID entryId = UUID.randomUUID();
        org.mockito.Mockito.doThrow(ResourceNotFoundException.forId("Food entry", entryId))
                .when(nutritionService).delete(userId, entryId);

        mockMvc.perform(delete("/api/v1/nutrition/entries/{id}", entryId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
}
