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
    private final SecurityContextService securityContextService;

    @PostMapping("/send")
    public ResponseEntity<String> sendApplicationEmail(@RequestBody EmailSendRequest request) {
        Long authenticatedUserId = securityContextService.getCurrentUserId();
        log.info("Secure email request authenticated for user ID: {} making outbound dispatch to target: {}",
                authenticatedUserId, request.getRecipientEmail());

        User user = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new RuntimeException(
                        "User context details not found in database for ID: " + authenticatedUserId));

        String activeAccessToken = oAuth2TokenManager.getValidAccessToken(user);

        emailService.sendApplicationEmail(
                user.getEmail(),
                activeAccessToken,
                request.getRecipientEmail(),
                request.getSubject(),
                request.getBody(),
                request.getCvVariantId()); // Pass the CV Variant ID directly

        return ResponseEntity.ok("Email successfully dispatched via user's Google account!");
    }

    @Data
    public static class EmailSendRequest {
        private String recipientEmail;
        private String subject;
        private String body;
        private Long cvVariantId; // Resolves ID instead of a direct string URL
    }
}