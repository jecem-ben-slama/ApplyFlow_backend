package com.applyflow.tracker_api.config;

import com.applyflow.tracker_api.dtos.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.NoSuchElementException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Catch business validation rules
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        ApiResponse<Void> response = ApiResponse.error(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // 2. Catch resource lookup missing records
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoSuchElementException ex) {
        return new ResponseEntity<>(ApiResponse.error("Requested item not found: " + ex.getMessage()),
                HttpStatus.NOT_FOUND);
    }

    // 3. Catch database SQL state failures
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDatabaseConstraints(DataIntegrityViolationException ex) {
        return new ResponseEntity<>(
                ApiResponse.error("Database constraint violation. Ensure foreign keys are correct."),
                HttpStatus.CONFLICT);
    }

    // 4. Catch your SecurityContextService 401 exceptions cleanly
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatusException(ResponseStatusException ex) {
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message(ex.getReason() != null ? ex.getReason() : ex.getMessage())
                .data(null)
                .build();

        return new ResponseEntity<>(response, ex.getStatusCode());
    }

    // 5. The SINGLE unified catch-all fallback handler for general unexpected
    // errors
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        // Logs details out to server stdout console logs for back-end debugging
        ex.printStackTrace();

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message("An unexpected system error occurred. Please try again later.")
                .data(null)
                .build();

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}