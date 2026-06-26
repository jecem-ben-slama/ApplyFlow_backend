package com.applyflow.tracker_api.dtos;

import java.time.LocalDateTime;


public record UserDto(
                Long id,
                String email,
                String googleSub,
                String firstName,
                String lastName,
                String name,
                String pictureUrl,
                LocalDateTime createdAt,
                LocalDateTime updatedAt) {
}