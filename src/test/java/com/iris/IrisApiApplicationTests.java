package com.iris;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iris.auth.dto.LoginRequest;
import com.iris.auth.dto.RefreshRequest;
import com.iris.auth.dto.SignupRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class IrisApiApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void contextLoads() {
    }

    @Test
    void signupLoginRefreshLogout_endToEndAgainstRealDatabase() throws Exception {
        String email = "e2e-" + UUID.randomUUID() + "@example.com";
        String password = "correct-horse-battery-staple";

        SignupRequest signup = new SignupRequest(email, password, "E2E Test User");
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(signup)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.user.email").value(email));

        LoginRequest login = new LoginRequest(email, password);
        String loginBody = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn().getResponse().getContentAsString();

        String loginRefreshToken = objectMapper.readTree(loginBody).get("refreshToken").asText();

        RefreshRequest refresh = new RefreshRequest(loginRefreshToken);
        String refreshBody = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(refresh)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn().getResponse().getContentAsString();

        String rotatedRefreshToken = objectMapper.readTree(refreshBody).get("refreshToken").asText();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new RefreshRequest(rotatedRefreshToken))))
                .andExpect(status().isNoContent());
    }

    @Test
    void refreshTokenReuse_revokesWholeFamily_againstRealDatabase() throws Exception {
        String email = "e2e-reuse-" + UUID.randomUUID() + "@example.com";
        String password = "correct-horse-battery-staple";

        SignupRequest signup = new SignupRequest(email, password, "E2E Reuse Test User");
        String signupBody = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(signup)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String originalRefreshToken = objectMapper.readTree(signupBody).get("refreshToken").asText();

        String rotatedBody = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new RefreshRequest(originalRefreshToken))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String rotatedRefreshToken = objectMapper.readTree(rotatedBody).get("refreshToken").asText();

        // Reusing the already-rotated token must be treated as compromise and revoke the whole family.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new RefreshRequest(originalRefreshToken))))
                .andExpect(status().isUnauthorized());

        // The newest token in the family must now be revoked too, even though the request above threw.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new RefreshRequest(rotatedRefreshToken))))
                .andExpect(status().isUnauthorized());
    }
}
