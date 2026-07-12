package com.applyflow.tracker_api.controllers;

import java.time.LocalDateTime;
import java.util.Base64;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.applyflow.tracker_api.models.ApplicationInteraction;
import com.applyflow.tracker_api.repositories.ApplicationRepository;
import com.applyflow.tracker_api.repositories.InteractionRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/track")
@RequiredArgsConstructor
public class TrackingController {

    private final InteractionRepository interactionRepository;
    private final ApplicationRepository applicationRepository;

    private static final byte[] PIXEL = Base64.getDecoder().decode(
            "R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7");

    @GetMapping(value = "/open/{applicationId}", produces = MediaType.IMAGE_GIF_VALUE)
public ResponseEntity<byte[]> trackOpen(
        @PathVariable Long applicationId,
        HttpServletRequest request) {

    applicationRepository.findById(applicationId).ifPresent(app -> {
        ApplicationInteraction interaction = ApplicationInteraction.builder()
                .application(app)
                .interactionType("EMAIL_OPENED")
                .timestamp(LocalDateTime.now())
                .ipAddress(extractClientIp(request))
                .build();
        interactionRepository.save(interaction);
    });

    return ResponseEntity.ok()
            .header("Cache-Control", "no-cache, no-store, must-revalidate")
            .header("Pragma", "no-cache")
            .header("Expires", "0")
            .body(PIXEL);
}

private String extractClientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
        return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr();
}
}