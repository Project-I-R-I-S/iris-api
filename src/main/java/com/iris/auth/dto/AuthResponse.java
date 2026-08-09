package com.iris.auth.dto;

import com.iris.user.dto.UserResponse;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresInSeconds,
        UserResponse user
) { }
