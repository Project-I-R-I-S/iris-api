package com.iris.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.iris.auth.dto.AuthResponse;
import com.iris.auth.dto.GoogleAuthRequest;
import com.iris.auth.dto.LoginRequest;
import com.iris.auth.dto.RefreshRequest;
import com.iris.auth.dto.SignupRequest;
import com.iris.common.exception.AuthException;
import com.iris.common.security.JwtService;
import com.iris.user.UserRepository;
import com.iris.user.model.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private GoogleTokenVerifier googleTokenVerifier;

    @InjectMocks
    private AuthService authService;

    private User persistedUser(UUID id, String email) {
        return User.builder().id(id).email(email).passwordHash("hashed").build();
    }

    private void stubTokenIssuance() {
        when(jwtService.generateAccessToken(any(), any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");
        when(jwtService.getRefreshTokenTtl()).thenReturn(Duration.ofDays(30));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---------- signup ----------

    @Test
    void signup_rejectsAnAlreadyRegisteredEmail() {
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(
                new SignupRequest("taken@example.com", "password123", "Name")))
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> assertThat(((AuthException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(userRepository, never()).save(any());
    }

    @Test
    void signup_hashesThePasswordAndPersistsSensibleDefaults() {
        stubTokenIssuance();
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        AuthResponse response = authService.signup(
                new SignupRequest("New@Example.com", "password123", "New User"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("new@example.com"); // normalized
        assertThat(saved.getPasswordHash()).isEqualTo("bcrypt-hash");
        assertThat(saved.getDailyWaterGoalMl()).isEqualTo(2500);
        assertThat(saved.getDailyCaffeineLimitMg()).isEqualTo(400);
        assertThat(saved.isEmailVerified()).isFalse();
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void signup_persistsARefreshTokenHashNeverTheRawToken() {
        stubTokenIssuance();
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        authService.signup(new SignupRequest("a@example.com", "password123", "A"));

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getTokenHash()).isNotEqualTo("refresh-token");
        assertThat(captor.getValue().getTokenHash()).hasSize(64); // SHA-256 hex digest length
    }

    // ---------- login ----------

    @Test
    void login_rejectsUnknownEmail() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "whatever")))
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> assertThat(((AuthException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void login_rejectsWrongPassword() {
        User user = persistedUser(UUID.randomUUID(), "user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("user@example.com", "wrong")))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void login_rejectsGoogleOnlyAccountsWithNoPasswordHash() {
        User googleOnlyUser = User.builder().id(UUID.randomUUID()).email("g@example.com").passwordHash(null).build();
        when(userRepository.findByEmail("g@example.com")).thenReturn(Optional.of(googleOnlyUser));

        assertThatThrownBy(() -> authService.login(new LoginRequest("g@example.com", "anything")))
                .isInstanceOf(AuthException.class);

        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void login_succeedsWithCorrectCredentials() {
        stubTokenIssuance();
        User user = persistedUser(UUID.randomUUID(), "user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct", "hashed")).thenReturn(true);

        AuthResponse response = authService.login(new LoginRequest("user@example.com", "correct"));

        assertThat(response.accessToken()).isEqualTo("access-token");
    }

    // ---------- Google sign-in ----------

    private GoogleIdToken.Payload googlePayload(String subject, String email, boolean verified) {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setSubject(subject);
        payload.setEmail(email);
        payload.setEmailVerified(verified);
        payload.set("name", "Google User");
        return payload;
    }

    @Test
    void loginWithGoogle_createsANewUserOnFirstSignIn() {
        stubTokenIssuance();
        when(googleTokenVerifier.verify("valid-id-token"))
                .thenReturn(googlePayload("google-sub-1", "newgoogle@example.com", true));
        when(userRepository.findByGoogleSubject("google-sub-1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("newgoogle@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        authService.loginWithGoogle(new GoogleAuthRequest("valid-id-token"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getGoogleSubject()).isEqualTo("google-sub-1");
        assertThat(captor.getValue().isEmailVerified()).isTrue();
        assertThat(captor.getValue().getPasswordHash()).isNull();
    }

    @Test
    void loginWithGoogle_linksExistingEmailPasswordAccountBySubjectOnFirstMatch() {
        stubTokenIssuance();
        User existing = persistedUser(UUID.randomUUID(), "existing@example.com");
        when(googleTokenVerifier.verify("valid-id-token"))
                .thenReturn(googlePayload("google-sub-2", "existing@example.com", true));
        when(userRepository.findByGoogleSubject("google-sub-2")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.loginWithGoogle(new GoogleAuthRequest("valid-id-token"));

        assertThat(existing.getGoogleSubject()).isEqualTo("google-sub-2");
        assertThat(existing.getPasswordHash()).isEqualTo("hashed"); // untouched
    }

    @Test
    void loginWithGoogle_doesNotOverwriteAnAlreadyLinkedSubject() {
        stubTokenIssuance();
        User existing = persistedUser(UUID.randomUUID(), "existing@example.com");
        existing.setGoogleSubject("original-subject");
        when(googleTokenVerifier.verify("valid-id-token"))
                .thenReturn(googlePayload("original-subject", "existing@example.com", true));
        when(userRepository.findByGoogleSubject("original-subject")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.loginWithGoogle(new GoogleAuthRequest("valid-id-token"));

        assertThat(existing.getGoogleSubject()).isEqualTo("original-subject");
    }

    // ---------- refresh ----------

    @Test
    void refresh_rejectsAnUnparsableToken() {
        when(jwtService.parseAndValidate("garbage")).thenThrow(new RuntimeException("bad token"));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("garbage")))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void refresh_rejectsAnAccessTokenPresentedAsARefreshToken() {
        Claims claims = mock(Claims.class);
        when(claims.get("type", String.class)).thenReturn("access");
        when(jwtService.parseAndValidate("some-token")).thenReturn(claims);

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("some-token")))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void refresh_rejectsATokenNotFoundInStorage() {
        Claims claims = mock(Claims.class);
        when(claims.get("type", String.class)).thenReturn("refresh");
        when(jwtService.parseAndValidate("some-token")).thenReturn(claims);
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("some-token")))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void refresh_rejectsARevokedToken() {
        UUID userId = UUID.randomUUID();
        Claims claims = mock(Claims.class);
        when(claims.get("type", String.class)).thenReturn("refresh");
        when(jwtService.parseAndValidate("some-token")).thenReturn(claims);
        RefreshToken revoked = RefreshToken.builder()
                .userId(userId).revokedAt(Instant.now().minusSeconds(60))
                .expiresAt(Instant.now().plusSeconds(3600)).build();
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("some-token")))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void refresh_rejectsAnExpiredToken() {
        Claims claims = mock(Claims.class);
        when(claims.get("type", String.class)).thenReturn("refresh");
        when(jwtService.parseAndValidate("some-token")).thenReturn(claims);
        RefreshToken expired = RefreshToken.builder()
                .userId(UUID.randomUUID())
                .expiresAt(Instant.now().minusSeconds(1)).build();
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("some-token")))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void refresh_rotatesTheTokenAndIssuesANewPairOnSuccess() {
        stubTokenIssuance();
        UUID userId = UUID.randomUUID();
        User user = persistedUser(userId, "user@example.com");

        Claims claims = mock(Claims.class);
        when(claims.get("type", String.class)).thenReturn("refresh");
        when(claims.getSubject()).thenReturn(userId.toString());
        when(jwtService.parseAndValidate("old-refresh-token")).thenReturn(claims);

        RefreshToken stored = RefreshToken.builder()
                .userId(userId)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(stored));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        AuthResponse response = authService.refresh(new RefreshRequest("old-refresh-token"));

        assertThat(stored.getRevokedAt()).isNotNull(); // old token revoked
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        verify(refreshTokenRepository).save(any(RefreshToken.class)); // new pair persisted
    }

    @Test
    void refresh_rejectsWhenTheUserBehindTheTokenNoLongerExists() {
        UUID userId = UUID.randomUUID();
        Claims claims = mock(Claims.class);
        when(claims.get("type", String.class)).thenReturn("refresh");
        when(claims.getSubject()).thenReturn(userId.toString());
        when(jwtService.parseAndValidate("old-refresh-token")).thenReturn(claims);
        RefreshToken stored = RefreshToken.builder()
                .userId(userId).expiresAt(Instant.now().plusSeconds(3600)).build();
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(stored));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("old-refresh-token")))
                .isInstanceOf(AuthException.class);
    }

    // ---------- logout ----------

    @Test
    void logout_revokesTheMatchingStoredToken() {
        RefreshToken stored = RefreshToken.builder().userId(UUID.randomUUID()).build();
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(stored));

        authService.logout("some-refresh-token");

        assertThat(stored.getRevokedAt()).isNotNull();
    }

    @Test
    void logout_isANoOpWhenTheTokenIsNotFound() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatCode(() -> authService.logout("unknown-token")).doesNotThrowAnyException();
    }
}
