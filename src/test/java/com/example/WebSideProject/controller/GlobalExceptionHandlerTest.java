package com.example.WebSideProject.controller;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void databaseFailureReturnsTraceableSafeResponse() {
        ResponseEntity<GlobalExceptionHandler.ApiErrorResponse> response =
                handler.handleDatabase(new DataAccessResourceFailureException("password=secret"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getHeaders().getFirst("X-Request-Id")).hasSize(8);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("DATABASE_UNAVAILABLE");
        assertThat(response.getBody().message()).doesNotContain("secret");
        assertThat(response.getBody().requestId())
                .isEqualTo(response.getHeaders().getFirst("X-Request-Id"));
    }
}
