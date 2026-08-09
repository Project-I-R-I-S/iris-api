package com.iris.common.security;

import java.util.UUID;

/**
 * Lightweight principal placed on the SecurityContext for every authenticated request.
 * Feature services should call {@link #currentUserId()} rather than injecting HttpServletRequest.
 */
public record AuthenticatedUser(UUID userId, String email) {

    public static UUID currentUserId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new IllegalStateException("No authenticated user in security context");
        }
        return user.userId();
    }
}
