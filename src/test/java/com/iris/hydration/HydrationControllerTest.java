package com.iris.hydration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iris.common.security.AuthenticatedUser;
import com.iris.common.security.JwtAuthenticationFilter;
import com.iris.hydration.dto.DailyHydrationResponse;
import com.iris.hydration.dto.WaterEntryRequest;
import com.iris.hydration.dto.WaterEntryResponse;
import com.iris.user.UserService;
import com.iris.user.dto.UserResponse;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = HydrationController.class)
@AutoConfigureMockMvc(addFilters = false)
class HydrationControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HydrationService hydrationService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
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
    void create_returns201WithTheCreatedEntry() throws Exception {
        WaterEntryRequest req = new WaterEntryRequest(250, Instant.parse("2026-08-25T08:00:00Z"));
        WaterEntryResponse response = new WaterEntryResponse(
                UUID.randomUUID(), 250, req.consumedAt(), Instant.now());
        when(hydrationService.create(eq(userId), org.mockito.ArgumentMatchers.any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/hydration/entries")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amountMl").value(250));
    }

    @Test
    void create_returns400ForNonPositiveAmount() throws Exception {
        WaterEntryRequest invalid = new WaterEntryRequest(0, Instant.now());

        mockMvc.perform(post("/api/v1/hydration/entries")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listForDay_defaultsToKolkataTimezoneWhenNotSpecified() throws Exception {
        when(userService.getById(userId)).thenReturn(new UserResponse(
                userId, "user@example.com", "User", null, null, null,
                null, null, "Asia/Kolkata", 2500, null, 400, true));
        when(hydrationService.listForDay(eq(userId), eq(LocalDate.of(2026, 8, 25)), eq(ZoneId.of("Asia/Kolkata"))))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/hydration/entries").param("date", "2026-08-25"))
                .andExpect(status().isOk());

        verify(hydrationService).listForDay(userId, LocalDate.of(2026, 8, 25), ZoneId.of("Asia/Kolkata"));
    }

    @Test
    void daily_returnsTheAggregatedTotalFromTheService() throws Exception {
        DailyHydrationResponse response = DailyHydrationResponse.of(
                LocalDate.of(2026, 8, 25), 1000, new BigDecimal("350"), 2500);
        when(hydrationService.dailyTotal(eq(userId), eq(LocalDate.of(2026, 8, 25)), eq(ZoneId.of("UTC"))))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/hydration/daily")
                        .param("date", "2026-08-25")
                        .param("timezone", "UTC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waterMl").value(1000))
                .andExpect(jsonPath("$.totalMl").value(1350));
    }

    @Test
    void delete_returns204OnSuccess() throws Exception {
        UUID entryId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/hydration/entries/{id}", entryId))
                .andExpect(status().isNoContent());

        verify(hydrationService).delete(userId, entryId);
    }
}
