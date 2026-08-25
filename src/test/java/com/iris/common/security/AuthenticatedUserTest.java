package com.iris.common.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticatedUserTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void currentUserId_returnsIdOfThePrincipalOnTheSecurityContext() {
        UUID userId = UUID.randomUUID();
        var principal = new AuthenticatedUser(userId, "user@example.com");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));

        assertThat(AuthenticatedUser.currentUserId()).isEqualTo(userId);
    }

    @Test
    void currentUserId_throwsWhenNoAuthenticationIsPresent() {
        assertThatThrownBy(AuthenticatedUser::currentUserId)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void currentUserId_throwsWhenPrincipalIsNotAnAuthenticatedUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("some-other-principal", null, List.of()));

        assertThatThrownBy(AuthenticatedUser::currentUserId)
                .isInstanceOf(IllegalStateException.class);
    }
}
