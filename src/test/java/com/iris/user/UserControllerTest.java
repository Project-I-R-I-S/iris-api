package com.iris.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iris.common.security.AuthenticatedUser;
import com.iris.common.security.JwtAuthenticationFilter;
import com.iris.user.dto.UpdateUserRequest;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

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

    private UserResponse sampleResponse() {
        return new UserResponse(userId, "user@example.com", "User", null, null, 175.0,
                null, null, "Asia/Kolkata", 2500, null, 400, true);
    }

    @Test
    void me_returnsTheAuthenticatedUsersProfile() throws Exception {
        when(userService.getById(userId)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("user@example.com"));
    }

    @Test
    void updateMe_delegatesToServiceWithTheAuthenticatedUsersId() throws Exception {
        UpdateUserRequest req = new UpdateUserRequest(
                "New Name", null, null, null, null, null, null, null, null, null);
        when(userService.update(eq(userId), any())).thenReturn(sampleResponse());

        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void updateMe_returns400ForANonPositiveHeight() throws Exception {
        UpdateUserRequest invalid = new UpdateUserRequest(
                null, null, null, -10.0, null, null, null, null, null, null);

        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void me_failsSafelyWhenNoAuthenticatedPrincipalIsPresent() throws Exception {
        // Security filters are disabled in this slice (addFilters = false), so this
        // exercises AuthenticatedUser's own defensive check rather than the real JWT
        // filter chain, which would reject an unauthenticated request with 401 first.
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isInternalServerError());
    }
}
