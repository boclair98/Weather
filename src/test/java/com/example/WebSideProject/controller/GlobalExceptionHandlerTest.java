package com.example.WebSideProject.controller;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void databaseFailureReturnsTraceableSafeResponse() {
        ResponseEntity<GlobalExceptionHandler.ApiErrorResponse> response =
                handler.handleDatabase(new DataAccessResourceFailureException("password=secret"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getHeaders().getFirst("X-Request-Id")).hasSize(12);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("DATABASE_UNAVAILABLE");
        assertThat(response.getBody().message()).doesNotContain("secret");
        assertThat(response.getBody().requestId())
                .isEqualTo(response.getHeaders().getFirst("X-Request-Id"));
    }

    @Test
    void missingStaticResourceIsReportedAsNotFound() {
        ResponseEntity<GlobalExceptionHandler.ApiErrorResponse> response =
                handler.handleNotFound(new NoResourceFoundException(HttpMethod.GET, "favicon.ico"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void unavailableOptionalFeatureReturnsServiceUnavailable() {
        ResponseEntity<GlobalExceptionHandler.ApiErrorResponse> response =
                handler.handleUnavailableFeature(new IllegalStateException("경로 API가 설정되지 않았습니다."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("FEATURE_UNAVAILABLE");
        assertThat(response.getBody().requestId()).hasSize(12);
    }
}
