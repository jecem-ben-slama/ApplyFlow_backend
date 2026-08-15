package com.applyflow.tracker_api.config.exceptions;

import com.applyflow.tracker_api.dtos.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataAccessException;

import java.net.ConnectException;
import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j // Injects logger for clean production log streams on Render
public class GlobalExceptionHandler {

    // Postgres SQLState codes (ANSI SQL standard — stable across PG versions)
    private static final String PG_FOREIGN_KEY_VIOLATION = "23503";
    private static final String PG_UNIQUE_VIOLATION = "23505";
    private static final String PG_NOT_NULL_VIOLATION = "23502";
    private static final String PG_CHECK_VIOLATION = "23514";
    private static final String PG_STRING_DATA_RIGHT_TRUNCATION = "22001";

    // 1. Catch business validation rules
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        ApiResponse<Void> response = ApiResponse.error(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // 1b. Catch business "valid request, wrong current state" rules
    // (e.g. deleting something that's currently blocked by app-level logic,
    // not just a raw DB constraint)
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalStateException(IllegalStateException ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.CONFLICT);
    }

    // 2. Catch resource lookup missing records (generic JDK exception)
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoSuchElementException ex) {
        return new ResponseEntity<>(ApiResponse.error("Requested item not found: " + ex.getMessage()),
                HttpStatus.NOT_FOUND);
    }

    // 2b. Catch custom "not found" exceptions thrown deliberately from the
    // service layer (preferred over NoSuchElementException/RuntimeException
    // going forward — use this in new code)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    // 2c. Catch @Valid request body validation failures (missing/invalid
    // fields caught at the DTO layer, before anything touches the DB)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationErrors(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return new ResponseEntity<>(ApiResponse.error(errors), HttpStatus.BAD_REQUEST);
    }

    // 3. Catch database SQL state constraint failures (FK violations, unique
    // violations, not-null violations, check violations, value-too-long).
    // Uses Postgres SQLState codes as the primary signal, with
    // message-keyword matching as a fallback if no SQLException is found.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDatabaseConstraints(DataIntegrityViolationException ex) {

        String sqlState = getSqlState(ex);
        String rootMessage = getRootCauseMessage(ex);
        log.warn("DATA INTEGRITY VIOLATION - sqlState={}, message={}", sqlState, rootMessage);

        // --- Primary check: Postgres SQLState code ---
        if (sqlState != null) {
            switch (sqlState) {
                case PG_FOREIGN_KEY_VIOLATION:
                    return conflict("This item cannot be deleted or modified because it is still linked to other "
                            + "records. Remove or reassign those related records first.");
                case PG_UNIQUE_VIOLATION:
                    return conflict(
                            "This item already exists. Please use a different value (duplicate entry detected).");
                case PG_NOT_NULL_VIOLATION:
                    return badRequest("A required field was missing when saving this record.");
                case PG_CHECK_VIOLATION:
                    return badRequest(
                            "The submitted data failed a validation rule. Please check the values and try again.");
                case PG_STRING_DATA_RIGHT_TRUNCATION:
                    return badRequest(
                            "One of the submitted values is too long for its field. Please shorten it and try again.");
                default:
                    // Unrecognized SQLState — fall through to message-based fallback below
                    break;
            }
        }

        // --- Fallback: keyword matching on the root cause message ---
        String lowerMessage = rootMessage.toLowerCase();
        if (isForeignKeyViolation(lowerMessage)) {
            return conflict("This item cannot be deleted because it is still linked to other records. "
                    + "Remove or reassign those related records first.");
        }
        if (isUniqueViolation(lowerMessage)) {
            return conflict("This item already exists. Please use a different value (duplicate entry detected).");
        }
        if (isNotNullViolation(lowerMessage)) {
            return badRequest("A required field was missing when saving this record.");
        }

        // Truly unrecognized constraint violation
        return conflict("Database constraint violation. Please check the submitted data and try again.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResource() {
        return ResponseEntity.notFound().build();
    }

    // 4. Catch Network/Database Connection Drops explicitly (e.g., database
    // pool exhausts or goes to sleep)
    @ExceptionHandler({ DataAccessResourceFailureException.class, ConnectException.class })
    public ResponseEntity<ApiResponse<Void>> handleDatabaseConnectionErrors(Exception ex) {
        log.error("CRITICAL: Database connection failed or dropped! Context: ", ex);
        return new ResponseEntity<>(
                ApiResponse
                        .error("The database service is temporarily unavailable. Please try again in a few moments."),
                HttpStatus.SERVICE_UNAVAILABLE); // 503 Service Unavailable
    }

    // 5. Catch Hibernate/Schema mapping errors (e.g., a table or column name
    // doesn't exist)
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericDatabaseErrors(DataAccessException ex) {
        log.error("DATABASE SCHEMA ERROR: Query execution or Hibernate structure matching failed: ", ex);
        return new ResponseEntity<>(
                ApiResponse
                        .error("A data access error occurred while processing your request. Please contact support."),
                HttpStatus.INTERNAL_SERVER_ERROR); // 500 Internal Server Error
    }

    // 6. Catch your SecurityContextService 401 exceptions cleanly
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatusException(ResponseStatusException ex) {
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message(ex.getReason() != null ? ex.getReason() : ex.getMessage())
                .data(null)
                .build();

        return new ResponseEntity<>(response, ex.getStatusCode());
    }

    // 7. The SINGLE unified catch-all fallback handler for general unexpected
    // errors
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        log.error("UNHANDLED RUNTIME EXCEPTION DETECTED: ", ex);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message("An unexpected system error occurred. Please try again later.")
                .data(null)
                .build();

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private ResponseEntity<ApiResponse<Void>> conflict(String message) {
        return new ResponseEntity<>(ApiResponse.error(message), HttpStatus.CONFLICT);
    }

    private ResponseEntity<ApiResponse<Void>> badRequest(String message) {
        return new ResponseEntity<>(ApiResponse.error(message), HttpStatus.BAD_REQUEST);
    }

    private String getSqlState(Throwable ex) {
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof SQLException sqlEx) {
                return sqlEx.getSQLState();
            }
            cause = cause.getCause();
        }
        return null;
    }

    private String getRootCauseMessage(Throwable ex) {
        Throwable rootCause = ex;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return rootCause.getMessage() != null ? rootCause.getMessage() : ex.getMessage();
    }

    private boolean isForeignKeyViolation(String rootMessage) {
        return rootMessage.contains("foreign key")
                || rootMessage.contains("violates foreign key constraint");
    }

    private boolean isUniqueViolation(String rootMessage) {
        return rootMessage.contains("unique constraint")
                || rootMessage.contains("violates unique constraint");
    }

    private boolean isNotNullViolation(String rootMessage) {
        return rootMessage.contains("null value in column")
                || rootMessage.contains("violates not-null constraint");
    }

}