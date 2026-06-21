package com.applyflow.tracker_api.dtos;

import java.time.LocalDateTime;

/**
 * A clean data carrier representing the authenticated user state.
 * Sensitive properties like the Google refresh token and raw entity graph
 * references
 * are excluded to keep payload serialization fast and isolated.
 */
public record UserDto(
        Long id,
        String email,
        String googleSub,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}