package com.applyflow.tracker_api.controllers;

import com.applyflow.tracker_api.config.SecurityContextService;
import com.applyflow.tracker_api.dtos.ApiResponse;
import com.applyflow.tracker_api.models.User;
import com.applyflow.tracker_api.repositories.UserRepository;
import com.applyflow.tracker_api.services.EmailService;
import com.applyflow.tracker_api.services.OAuth2TokenManager;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
@Slf4j
public class EmailController {

    private final UserRepository userRepository;
    private final OAuth2TokenManager oAuth2TokenManager;
    private final EmailService emailService;
    private final SecurityContextService securityContextService;

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<String>> sendApplicationEmail(@RequestBody EmailSendRequest request) {
        Long authenticatedUserId = securityContextService.getCurrentUserId();
        log.info("Secure email request authenticated for user ID: {} making outbound dispatch to target: {}",
                authenticatedUserId, request.getRecipientEmail());

        User user = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new RuntimeException(
                        "User context details not found in database for ID: " + authenticatedUserId));

        // Guard check: guest sessions have no linked Google account, so there's
        // no token to fetch. Failing here (instead of letting it fall through to
        // OAuth2TokenManager) avoids a confusing token-refresh error and gives
        // the same clear message the service layer would otherwise produce.
        if (Boolean.TRUE.equals(user.getIsGuest())) {
            throw new IllegalArgumentException(
                    "Sending email requires a connected Google account. "
                            + "Guest sessions aren't linked to Gmail — "
                            + "sign in with Google to send application emails.");
        }

        String activeAccessToken = oAuth2TokenManager.getValidAccessToken(user);

        emailService.sendApplicationEmail(
                user.getEmail(),
                activeAccessToken,
                request.getRecipientEmail(),
                request.getSubject(),
                request.getBody(),
                request.getCvVariantId(), // Pass the CV Variant ID directly
                request.getApplicationId()); // Needed to build the tracking pixel URL

        return ResponseEntity.ok(
                ApiResponse.success("Email successfully dispatched via user's Google account!"));
    }

    @Data
    public static class EmailSendRequest {
        private String recipientEmail;
        private String subject;
        private String body;
        private Long cvVariantId; // Resolves ID instead of a direct string URL
        private Long applicationId; // Links this send to an Application for open tracking
    }
}