package com.example.WebSideProject.controller;

import com.example.WebSideProject.config.RequestIdFilter;
import com.example.WebSideProject.service.EmailVerificationCooldownException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.dao.DataAccessException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("입력값을 확인해주세요.");

        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, e, false);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", e.getMessage(), e, false);
    }

    @ExceptionHandler(EmailVerificationCooldownException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailVerificationCooldown(EmailVerificationCooldownException e) {
        ResponseEntity<ApiErrorResponse> response = error(
                HttpStatus.TOO_MANY_REQUESTS,
                "VERIFICATION_COOLDOWN",
                e.getMessage(),
                e,
                false
        );
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(e.getRetryAfterSeconds()))
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(response.getBody());
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiErrorResponse> handleSecurity(SecurityException e) {
        return error(HttpStatus.FORBIDDEN, "ACCESS_DENIED", e.getMessage(), e, false);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiErrorResponse> handleDatabase(DataAccessException e) {
        return error(
                HttpStatus.SERVICE_UNAVAILABLE,
                "DATABASE_UNAVAILABLE",
                "데이터베이스 연결이 원활하지 않습니다. 잠시 후 다시 시도해주세요.",
                e,
                true
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleUnavailableFeature(IllegalStateException e) {
        return error(
                HttpStatus.SERVICE_UNAVAILABLE,
                "FEATURE_UNAVAILABLE",
                e.getMessage(),
                e,
                false
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(NoResourceFoundException e) {
        return error(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "요청한 리소스를 찾을 수 없습니다.",
                e,
                false
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(Exception e) {
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
                e,
                true
        );
    }

    private ResponseEntity<ApiErrorResponse> error(
            HttpStatus status,
            String code,
            String message,
            Exception exception,
            boolean logStackTrace
    ) {
        String requestId = RequestIdFilter.currentOrNew();
        if (logStackTrace) {
            log.error("API 오류 requestId={}, code={}", requestId, code, exception);
        } else {
            log.warn("API 요청 거부 requestId={}, code={}, message={}", requestId, code, message);
        }

        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(new ApiErrorResponse(
                        "https://weather.coders.kr/problems/" + code.toLowerCase(),
                        status.getReasonPhrase(),
                        status.value(),
                        code,
                        message,
                        requestId,
                        Instant.now()
                ));
    }

    public record ApiErrorResponse(
            String type,
            String title,
            int status,
            String code,
            String message,
            String requestId,
            Instant timestamp
    ) {
    }
}
