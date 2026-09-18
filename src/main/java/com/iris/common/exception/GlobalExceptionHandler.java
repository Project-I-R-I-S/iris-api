package com.iris.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.DateTimeException;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), req);
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiError> handleAuth(AuthException ex, HttpServletRequest req) {
        return build(ex.getStatus(), ex.getStatus().getReasonPhrase(), ex.getMessage(), req);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage(), req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ApiError.FieldViolation> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiError.FieldViolation(fe.getField(), fe.getDefaultMessage()))
                .toList();
        var body = ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                "Request validation failed",
                req.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Bad Request", "Malformed request body", req);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Bad Request",
                "Invalid value for parameter '" + ex.getName() + "'", req);
    }

    @ExceptionHandler(DateTimeException.class)
    public ResponseEntity<ApiError> handleDateTime(DateTimeException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), req);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "Not Found", "The requested resource was not found", req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred", req);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String error, String message, HttpServletRequest req) {
        var body = ApiError.of(status.value(), error, message, req.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
