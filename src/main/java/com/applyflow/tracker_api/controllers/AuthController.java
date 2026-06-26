package com.applyflow.tracker_api.controllers;

import com.applyflow.tracker_api.config.SecurityContextService;
import com.applyflow.tracker_api.dtos.ApiResponse;
import com.applyflow.tracker_api.dtos.UserDto;
import com.applyflow.tracker_api.models.User;
import com.applyflow.tracker_api.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

        private final UserRepository userRepository;
        private final SecurityContextService securityContextService;

        @GetMapping("/me")
        public ApiResponse<UserDto> getCurrentUser() {
                // 1. Resolve secure user context primary key
                Long userId = securityContextService.getCurrentUserId();

                // 2. Load underlying core profile details
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "User profile missing."));

                // 3. Construct full name safely (combining first and last name)
                String fullName = (user.getFirstName() != null && user.getLastName() != null)
                                ? user.getFirstName() + " " + user.getLastName()
                                : null;

                // 4. Map directly into the updated immutable record layout
                UserDto userDto = new UserDto(
                                user.getId(),
                                user.getEmail(),
                                user.getGoogleSub(),
                                user.getFirstName(),
                                user.getLastName(),
                                fullName,
                                user.getPictureUrl(),
                                user.getCreatedAt(),
                                user.getUpdatedAt());

                // 5. Return clean data completely free of nested collections
                return ApiResponse.<UserDto>builder()
                                .success(true)
                                .message("Session verified successfully.")
                                .data(userDto)
                                .build();
        }
}