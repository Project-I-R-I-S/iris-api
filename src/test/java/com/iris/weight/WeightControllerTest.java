package com.iris.weight;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iris.common.security.AuthenticatedUser;
import com.iris.common.security.JwtAuthenticationFilter;
import com.iris.user.UserService;
import com.iris.user.dto.UserResponse;
import com.iris.weight.dto.WeightEntryRequest;
import com.iris.weight.dto.WeightEntryResponse;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = WeightController.class)
@AutoConfigureMockMvc(addFilters = false)
class WeightControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WeightService weightService;
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
    void create_returns201WithBmiInTheResponse() throws Exception {
        WeightEntryRequest req = new WeightEntryRequest(new BigDecimal("81.0"), Instant.now(), null);
        WeightEntryResponse response = new WeightEntryResponse(
                UUID.randomUUID(), req.weightKg(), req.recordedAt(), null, new BigDecimal("25.0"), Instant.now());
        when(weightService.create(eq(userId), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/weight/entries")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bmi").value(25.0));
    }

    @Test
    void create_returns400ForZeroOrNegativeWeight() throws Exception {
        WeightEntryRequest invalid = new WeightEntryRequest(BigDecimal.ZERO, Instant.now(), null);

        mockMvc.perform(post("/api/v1/weight/entries")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void latest_returns204WhenTheUserHasNoEntries() throws Exception {
        when(weightService.latest(userId)).thenReturn(null);

        mockMvc.perform(get("/api/v1/weight/entries/latest"))
                .andExpect(status().isNoContent());
    }

    @Test
    void listInRange_passesFromToAndTimezoneThrough() throws Exception {
        when(userService.getById(userId)).thenReturn(new UserResponse(
                userId, "user@example.com", "User", null, null, null,
                null, null, "Asia/Kolkata", 2500, null, 400, true));
        when(weightService.listInRange(eq(userId), eq(java.time.LocalDate.of(2026, 8, 1)),
                eq(java.time.LocalDate.of(2026, 8, 25)), eq(java.time.ZoneId.of("Asia/Kolkata"))))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/weight/entries")
                        .param("from", "2026-08-01")
                        .param("to", "2026-08-25"))
                .andExpect(status().isOk());

        verify(weightService).listInRange(userId, java.time.LocalDate.of(2026, 8, 1),
                java.time.LocalDate.of(2026, 8, 25), java.time.ZoneId.of("Asia/Kolkata"));
    }
}
