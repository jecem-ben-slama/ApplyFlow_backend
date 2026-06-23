package com.applyflow.tracker_api.controllers;

import com.applyflow.tracker_api.config.SecurityContextService;
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
    private final SecurityContextService securityContextService; // Secure helper service injected

    @PostMapping("/send")
    public ResponseEntity<String> sendApplicationEmail(@RequestBody EmailSendRequest request) {
        // 1. Resolve and verify the actual authenticated User ID from Spring Security
        // context
        Long authenticatedUserId = securityContextService.getCurrentUserId();
        log.info("Secure email request authenticated for user ID: {} making an outbound dispatch to target: {}",
                authenticatedUserId, request.getRecipientEmail());

        // 2. Fetch the true database entity belonging exclusively to that authenticated
        // session
        User user = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new RuntimeException(
                        "User context details not found in database for ID: " + authenticatedUserId));

        // 3. Refresh token on-demand to guarantee a valid, active access token
        String activeAccessToken = oAuth2TokenManager.getValidAccessToken(user);

        // 4. Dispatch the email dynamically over the user's authentic SMTP pipeline
        emailService.sendApplicationEmailDynamic(
                user.getEmail(),
                activeAccessToken,
                request.getRecipientEmail(),
                request.getSubject(),
                request.getBody());

        return ResponseEntity.ok("Email successfully dispatched via user's Google account!");
    }

    /**
     * DTO matching incoming frontend button event payloads.
     * REMOVED: userId property is deleted to ensure zero client-side tampering.
     */
    @Data
    public static class EmailSendRequest {
        private String recipientEmail;
        private String subject;
        private String body;
    }
}