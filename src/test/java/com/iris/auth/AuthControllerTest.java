package com.iris.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iris.auth.dto.*;
import com.iris.common.exception.AuthException;
import com.iris.common.security.JwtAuthenticationFilter;
import com.iris.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UserResponse sampleUser() {
        return new UserResponse(UUID.randomUUID(), "user@example.com", "User", null, null, null,
                null, null, "Asia/Kolkata", 2500, null, 400, false);
    }

    @Test
    void signup_returns201WithTokenPair() throws Exception {
        AuthResponse response = new AuthResponse("access", "refresh", 3600, sampleUser());
        when(authService.signup(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new SignupRequest("user@example.com", "password123", "User"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access"));
    }

    @Test
    void signup_returns409WhenEmailAlreadyRegistered() throws Exception {
        when(authService.signup(any()))
                .thenThrow(new AuthException(HttpStatus.CONFLICT, "Email already registered"));

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new SignupRequest("user@example.com", "password123", "User"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already registered"));
    }

    @Test
    void signup_returns400ForAnInvalidEmailOrShortPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new SignupRequest("not-an-email", "short", "User"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_returns401ForInvalidCredentials() throws Exception {
        when(authService.login(any()))
                .thenThrow(new AuthException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("user@example.com", "wrong"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_returns200OnSuccess() throws Exception {
        when(authService.login(any())).thenReturn(new AuthResponse("access", "refresh", 3600, sampleUser()));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("user@example.com", "correct"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").value("refresh"));
    }

    @Test
    void refresh_returns401ForAnInvalidToken() throws Exception {
        when(authService.refresh(any()))
                .thenThrow(new AuthException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new RefreshRequest("bad-token"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_returns204() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new RefreshRequest("some-token"))))
                .andExpect(status().isNoContent());
    }
}
