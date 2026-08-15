package com.applyflow.tracker_api.services.storage;

import com.applyflow.tracker_api.config.SecurityContextService;
import com.applyflow.tracker_api.models.User;
import com.applyflow.tracker_api.repositories.UserRepository;
import com.applyflow.tracker_api.services.OAuth2TokenManager;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleDriveStorageService implements CvStorageService {

    private static final String ACCEPTED_MIME_TYPE = "application/pdf";

    private final RestTemplate restTemplate = new RestTemplate();
    private final SecurityContextService securityContextService;
    private final UserRepository userRepository;
    private final OAuth2TokenManager tokenManager;

    @Override
    public byte[] downloadFile(String fileUrl) {

        String fileId = extractFileIdFromUrl(fileUrl);
        String accessToken = getFreshTokenForCurrentUser();

        // 1. Validate the file is actually a PDF before pulling bytes
        DriveFileMetadata metadata = getFileMetadata(fileId, accessToken);
        if (metadata == null || metadata.getMimeType() == null) {
            throw new IllegalArgumentException("Unable to determine file type for Drive file: " + fileId);
        }
        if (!ACCEPTED_MIME_TYPE.equals(metadata.getMimeType())) {
            log.warn("Rejected non-PDF Drive file. fileId={}, name={}, mimeType={}",
                    fileId, metadata.getName(), metadata.getMimeType());
            throw new IllegalArgumentException(
                    "Only PDF files are supported. Found: " + metadata.getMimeType()
                            + (metadata.getName() != null ? " (" + metadata.getName() + ")" : ""));
        }

        // 2. Download the actual bytes
        String downloadUrl = "https://www.googleapis.com/drive/v3/files/" + fileId + "?alt=media";

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

    /**
     * Handles the common Drive URL shapes:
     * https://drive.google.com/file/d/{id}/view?usp=sharing
     * https://drive.google.com/open?id={id}
     * https://drive.google.com/uc?export=view&id={id}
     * https://drive.google.com/uc?export=download&id={id}
     * https://drive.google.com/thumbnail?id={id}&sz=w400
     */
    private String extractFileIdFromUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("File URL must not be empty");
        }

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

        // Fallback: parse the "id" query parameter (open?id=, uc?id=, thumbnail?id=,
        // etc.)
        Map<String, String> queryParams = UriComponentsBuilder.fromUriString(url)
                .build()
                .getQueryParams()
                .toSingleValueMap();

        String id = queryParams.get("id");
        if (id != null && !id.isBlank()) {
            return id;
        }

        throw new IllegalArgumentException("Unsupported or invalid Google Drive URL format: " + url);
    }

    private DriveFileMetadata getFileMetadata(String fileId, String accessToken) {
        String metaUrl = "https://www.googleapis.com/drive/v3/files/" + fileId + "?fields=id,name,mimeType";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<DriveFileMetadata> response = restTemplate.exchange(
                    metaUrl,
                    HttpMethod.GET,
                    entity,
                    DriveFileMetadata.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to fetch Drive file metadata for File ID: {}", fileId, e);
            throw new RuntimeException("Google Drive metadata lookup failed: " + e.getMessage(), e);
        }
    }

    private String getFreshTokenForCurrentUser() {
        Long currentUserId = securityContextService.getCurrentUserId();

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in database: " + currentUserId));

        log.info("Provisioning fresh access token for user: {} via Security Context", user.getEmail());
        return tokenManager.getValidAccessToken(user);
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class DriveFileMetadata {
        private String id;
        private String name;
        private String mimeType;
    }
}