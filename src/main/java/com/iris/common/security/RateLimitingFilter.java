package com.iris.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iris.common.exception.ApiError;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory per-client rate limiting for the auth endpoints. Single-instance only —
 * horizontal scaling will need a shared store (e.g. Redis-backed bucket4j).
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Map<String, Bucket> SIGNUP_LOGIN_BUCKETS = new ConcurrentHashMap<>();
    private static final Map<String, Bucket> REFRESH_BUCKETS = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    public RateLimitingFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        Map<String, Bucket> buckets = limitedBucketsFor(path);

        if (buckets == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientKey = clientKey(request);
        Bucket bucket = buckets.computeIfAbsent(clientKey, key -> newBucket(buckets));

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }

        writeTooManyRequests(response, request.getRequestURI());
    }

    private Map<String, Bucket> limitedBucketsFor(String path) {
        if (path.equals("/api/v1/auth/signup") || path.equals("/api/v1/auth/login")) {
            return SIGNUP_LOGIN_BUCKETS;
        }
        if (path.equals("/api/v1/auth/refresh")) {
            return REFRESH_BUCKETS;
        }
        return null;
    }

    private Bucket newBucket(Map<String, Bucket> buckets) {
        int capacity = buckets == SIGNUP_LOGIN_BUCKETS ? 5 : 20;
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillIntervally(capacity, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private String clientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletResponse response, String path) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError body = ApiError.of(HttpStatus.TOO_MANY_REQUESTS.value(), "Too Many Requests",
                "Rate limit exceeded. Please try again later.", path);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
