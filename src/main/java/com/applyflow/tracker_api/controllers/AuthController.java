package com.applyflow.tracker_api.controllers;

import com.applyflow.tracker_api.config.SecurityContextService;
import com.applyflow.tracker_api.config.exceptions.ResourceNotFoundException;
import com.applyflow.tracker_api.dtos.AccountDeletionRequestDto;
import com.applyflow.tracker_api.dtos.ApiResponse;
import com.applyflow.tracker_api.dtos.UserDto;
import com.applyflow.tracker_api.models.User;
import com.applyflow.tracker_api.repositories.UserRepository;
import com.applyflow.tracker_api.services.AccountDeletionService;
import com.applyflow.tracker_api.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

        private final UserRepository userRepository;
        private final SecurityContextService securityContextService;
        private final AuthService authService;
        private final AccountDeletionService accountDeletionService;

        @GetMapping("/me")
        public ApiResponse<UserDto> getCurrentUser(HttpServletRequest request) {
                Long userId = securityContextService.getCurrentUserId();

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User profile missing."));

                String fullName = (user.getFirstName() != null && user.getLastName() != null)
                                ? user.getFirstName() + " " + user.getLastName()
                                : null;

                // One-time reactivation notice — read and clear so it never repeats.
                Boolean reactivatedFlag = (Boolean) request.getSession().getAttribute("accountReactivated");
                boolean reactivated = reactivatedFlag != null && reactivatedFlag;
                if (reactivated) {
                        request.getSession().removeAttribute("accountReactivated");
                }

                UserDto userDto = new UserDto(
                                user.getId(),
                                user.getEmail(),
                                user.getGoogleSub(),
                                user.getFirstName(),
                                user.getLastName(),
                                fullName,
                                user.getPictureUrl(),
                                user.getCreatedAt(),
                                user.getUpdatedAt(),
                                reactivated,
                                Boolean.TRUE.equals(user.getIsGuest()));

                return ApiResponse.<UserDto>builder()
                                .success(true)
                                .message("Session verified successfully.")
                                .data(userDto)
                                .build();
        }

        /**
         * Starts an anonymous guest session. Creates a bare User row (no Google
         * identity yet) and establishes a real session cookie for it, exactly
         * like a normal login — every other endpoint works identically for
         * guests since SecurityContextService resolves the principal the same way.
         */
        @PostMapping("/guest")
        public ApiResponse<UserDto> createGuestSession(HttpServletRequest request, HttpServletResponse response) {
                User guest = User.builder()
                                .isGuest(true)
                                .guestToken(UUID.randomUUID().toString())
                                .build();
                userRepository.save(guest);

                authService.establishGuestSession(request, response, guest.getId());

                UserDto userDto = new UserDto(
                                guest.getId(),
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                guest.getCreatedAt(),
                                guest.getUpdatedAt(),
                                false,
                                true);

                return ApiResponse.<UserDto>builder()
                                .success(true)
                                .message("Guest session started.")
                                .data(userDto)
                                .build();
        }

        @PostMapping("/logout")
        public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
                authService.logout(request, response);

                return ApiResponse.<Void>builder()
                                .success(true)
                                .message("Logged out successfully.")
                                .build();
        }

        /**
         * The client must send the exact phrase "delete {their email}"; the
         * service independently verifies it against the account's real email
         * before scheduling anything, since anyone could otherwise hit this
         * endpoint directly and skip the frontend check entirely.
         */
        @DeleteMapping("/account")
        public ApiResponse<Void> requestAccountDeletion(
                        @RequestBody AccountDeletionRequestDto deletionRequest,
                        HttpServletRequest request,
                        HttpServletResponse response) {
                Long userId = securityContextService.getCurrentUserId();

                accountDeletionService.requestDeletion(userId, deletionRequest.getConfirmationPhrase());
                authService.logout(request, response); // kill the session immediately

                return ApiResponse.<Void>builder()
                                .success(true)
                                .message("Account scheduled for deletion. Log back in within 7 days to cancel.")
                                .build();
        }
}