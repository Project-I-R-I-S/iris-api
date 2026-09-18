package com.iris.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.iris.auth.dto.*;
import com.iris.common.exception.AuthException;
import com.iris.common.security.JwtService;
import com.iris.user.UserRepository;
import com.iris.user.dto.UserResponse;
import com.iris.user.model.User;
import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleTokenVerifier googleTokenVerifier;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       GoogleTokenVerifier googleTokenVerifier) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.googleTokenVerifier = googleTokenVerifier;
    }

    @Transactional
    public AuthResponse signup(SignupRequest req) {
        String email = req.email().toLowerCase().trim();

        if (userRepository.existsByEmail(email)) {
            throw new AuthException(HttpStatus.CONFLICT, "Email already registered");
        }

        User user = defaultProfileBuilder(email, req.displayName())
                .passwordHash(passwordEncoder.encode(req.password()))
                .emailVerified(false)
                .build();

        user = userRepository.save(user);
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        String email = req.email().toLowerCase().trim();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse loginWithGoogle(GoogleAuthRequest req) {
        GoogleIdToken.Payload payload = googleTokenVerifier.verify(req.idToken());

        String googleSub = payload.getSubject();
        String email = ((String) payload.getEmail()).toLowerCase();
        String name = (String) payload.get("name");
        Boolean emailVerified = payload.getEmailVerified();

        Optional<User> existing = userRepository.findByGoogleSubject(googleSub);
        if (existing.isEmpty()) {
            Optional<User> byEmail = userRepository.findByEmail(email);
            if (byEmail.isPresent()) {
                if (!Boolean.TRUE.equals(emailVerified)) {
                    throw new AuthException(HttpStatus.CONFLICT,
                            "An account with this email already exists. Sign in with password, " +
                                    "or use a verified Google account to link it.");
                }
                existing = byEmail;
            }
        }

        User user = existing.orElseGet(() -> defaultProfileBuilder(email, name).build());

        // Link Google account if not already linked, and mark verified.
        if (user.getGoogleSubject() == null) {
            user.setGoogleSubject(googleSub);
        }
        if (Boolean.TRUE.equals(emailVerified)) {
            user.setEmailVerified(true);
        }
        user = userRepository.save(user);

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest req) {
        Claims claims;
        try {
            claims = jwtService.parseAndValidate(req.refreshToken());
        } catch (Exception ex) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        if (!"refresh".equals(claims.get("type", String.class))) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Invalid token type");
        }

        String tokenHash = sha256(req.refreshToken());
        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "Refresh token not recognized"));

        if (!stored.isActive()) {
            if (stored.getRevokedAt() != null) {
                // Reuse of an already-rotated token: treat as compromised and revoke the whole family.
                refreshTokenRepository.revokeAllActiveForUser(stored.getUserId(), Instant.now());
            }
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Refresh token expired or revoked");
        }

        // Rotate: revoke old, issue new pair.
        stored.setRevokedAt(Instant.now());

        User user = userRepository.findById(UUID.fromString(claims.getSubject()))
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "User not found"));

        return buildAuthResponse(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        String tokenHash = sha256(refreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(rt -> rt.setRevokedAt(Instant.now()));
    }

    private User.UserBuilder defaultProfileBuilder(String email, String displayName) {
        return User.builder()
                .email(email)
                .displayName(displayName)
                .dayStartTime(LocalTime.of(7, 0))
                .dayEndTime(LocalTime.of(23, 0))
                .timezone("Asia/Kolkata")
                .dailyWaterGoalMl(2500)
                .dailyCaffeineLimitMg(400);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        RefreshToken record = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(sha256(refreshToken))
                .expiresAt(Instant.now().plus(jwtService.getRefreshTokenTtl()))
                .build();
        refreshTokenRepository.save(record);

        // TTL exposed to client so they know when to refresh proactively.
        long accessTtlSeconds = jwtService.getAccessTokenTtl().toSeconds();

        return new AuthResponse(accessToken, refreshToken, accessTtlSeconds, UserResponse.from(user));
    }

    private String sha256(String input) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
