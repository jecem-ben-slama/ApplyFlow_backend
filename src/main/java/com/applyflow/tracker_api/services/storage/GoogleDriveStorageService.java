package com.applyflow.tracker_api.services.storage;

import com.applyflow.tracker_api.config.SecurityContextService;
import com.applyflow.tracker_api.models.User;
import com.applyflow.tracker_api.repositories.UserRepository;
import com.applyflow.tracker_api.services.OAuth2TokenManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleDriveStorageService implements CvStorageService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final SecurityContextService securityContextService;
    private final UserRepository userRepository;
    private final OAuth2TokenManager tokenManager;

    @Override
    public byte[] downloadFile(String fileUrl) {
        log.info("Processing Google Drive CV download via Strategy Pattern for URL: {}", fileUrl);

        String fileId = extractFileIdFromUrl(fileUrl);
        String downloadUrl = "https://www.googleapis.com/drive/v3/files/" + fileId + "?alt=media";

        // Fetch the fresh token using the authenticated user's context
        String accessToken = getFreshTokenForCurrentUser();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    downloadUrl,
                    HttpMethod.GET,
                    entity,
                    byte[].class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to download byte stream from Google Drive for File ID: {}", fileId, e);
            throw new RuntimeException("Google Drive CV download failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean supports(String fileUrl) {
        return fileUrl != null && fileUrl.contains("drive.google.com");
    }

    private String extractFileIdFromUrl(String url) {
        if (url.contains("/d/")) {
            String[] parts = url.split("/d/");
            String idPart = parts[1];

            if (idPart.contains("/")) {
                idPart = idPart.substring(0, idPart.indexOf("/"));
            }
            if (idPart.contains("?")) {
                idPart = idPart.substring(0, idPart.indexOf("?"));
            }
            return idPart;
        }
        throw new IllegalArgumentException("Unsupported or invalid Google Drive URL format: " + url);
    }

    private String getFreshTokenForCurrentUser() {
        // 1. Get the authenticated user's ID from the security context
        Long currentUserId = securityContextService.getCurrentUserId();

        // 2. Fetch the User entity from the database to retrieve their email/refresh
        // token context
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in database: " + currentUserId));

        // 3. Provision a fresh access token using your token manager
        log.info("Provisioning fresh access token for user: {} via Security Context", user.getEmail());
        return tokenManager.getValidAccessToken(user);
    }
}