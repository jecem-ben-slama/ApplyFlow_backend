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
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleDriveStorageService implements CvStorageService {

    private static final String ACCEPTED_MIME_TYPE = "application/pdf";

    // Only the standard public "Share > Copy link" format is accepted:
    // https://drive.google.com/file/d/{fileId}/view?usp=sharing (query string
    // optional)
    private static final Pattern PUBLIC_SHARE_LINK_PATTERN = Pattern
            .compile("^https://drive\\.google\\.com/file/d/([a-zA-Z0-9_-]+)/view(?:\\?.*)?$");

    private final RestTemplate restTemplate = new RestTemplate();
    private final SecurityContextService securityContextService;
    private final UserRepository userRepository;
    private final OAuth2TokenManager tokenManager;

    @Override
    public void validateFile(String fileUrl) {
        // Runs the same link-format, access, and type checks as downloadFile,
        // minus the actual byte download. Throws IllegalArgumentException
        // with a specific, user-facing reason on any problem.
        resolveAndValidateMetadata(fileUrl);
    }

    @Override
    public byte[] downloadFile(String fileUrl) {

        String fileId = resolveAndValidateMetadata(fileUrl);
        String accessToken = getFreshTokenForCurrentUser();

        // Download the actual bytes
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
        } catch (HttpClientErrorException e) {
            if (isPermissionOrNotFoundError(e.getStatusCode())) {
                log.warn("Access denied downloading Drive file bytes. fileId={}, status={}", fileId, e.getStatusCode());
                throw notAccessibleException();
            }
            log.error("Failed to download byte stream from Google Drive for File ID: {}", fileId, e);
            throw new RuntimeException("Google Drive CV download failed: " + e.getMessage(), e);
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
     * Parses the URL, fetches metadata, and confirms it's a PDF.
     * Returns the fileId on success. Throws IllegalArgumentException with a
     * clear, specific reason on any failure — shared by validateFile()
     * (add/edit time) and downloadFile() (send time), so both moments give
     * the user the same precise explanation.
     */
    private String resolveAndValidateMetadata(String fileUrl) {
        String fileId = extractFileIdFromUrl(fileUrl);
        String accessToken = getFreshTokenForCurrentUser();

        DriveFileMetadata metadata = getFileMetadata(fileId, accessToken);
        if (metadata == null || metadata.getMimeType() == null) {
            throw new IllegalArgumentException("Unable to determine file type for this Google Drive file.");
        }
        if (!ACCEPTED_MIME_TYPE.equals(metadata.getMimeType())) {
            log.warn("Rejected non-PDF Drive file. fileId={}, name={}, mimeType={}",
                    fileId, metadata.getName(), metadata.getMimeType());
            throw new IllegalArgumentException(
                    "Only PDF files are supported. This file is: " + metadata.getMimeType()
                            + (metadata.getName() != null ? " (" + metadata.getName() + ")" : ""));
        }
        return fileId;
    }

    /**
     * Only accepts the standard public share link produced by Drive's
     * "Share > Copy link" action:
     * https://drive.google.com/file/d/{fileId}/view?usp=sharing
     *
     * Anything else (open?id=, uc?export=, thumbnail?id=, malformed URLs, etc.)
     * is rejected with a clear, user-facing message.
     */
    private String extractFileIdFromUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("CV link must not be empty.");
        }

        Matcher matcher = PUBLIC_SHARE_LINK_PATTERN.matcher(url.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "This doesn't look like a public Google Drive share link. "
                            + "In Drive, right-click the file > \"Share\" > \"Copy link\" "
                            + "(make sure access is set to \"Anyone with the link\"). "
                            + "The link should look like: https://drive.google.com/file/d/FILE_ID/view?usp=sharing");
        }

        return matcher.group(1);
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
        } catch (HttpClientErrorException e) {
            // Drive returns 403 for "exists but you don't have access" and often 404
            // for a shared-but-not-published/deleted file, to avoid leaking existence.
            // Either way, from the caller's perspective this is "the link isn't
            // accessible to us" — a fixable input problem, not a server failure.
            if (isPermissionOrNotFoundError(e.getStatusCode())) {
                log.warn("Access denied fetching Drive file metadata. fileId={}, status={}", fileId, e.getStatusCode());
                throw notAccessibleException();
            }
            log.error("Failed to fetch Drive file metadata for File ID: {}", fileId, e);
            throw new RuntimeException("Google Drive metadata lookup failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to fetch Drive file metadata for File ID: {}", fileId, e);
            throw new RuntimeException("Google Drive metadata lookup failed: " + e.getMessage(), e);
        }
    }

    private boolean isPermissionOrNotFoundError(HttpStatusCode status) {
        return status == HttpStatus.FORBIDDEN || status == HttpStatus.NOT_FOUND;
    }

    private IllegalArgumentException notAccessibleException() {
        return new IllegalArgumentException(
                "This Google Drive file isn't accessible. Make sure it's shared with "
                        + "\"Anyone with the link\" (Share > General access > Anyone with the link) "
                        + "and that the link hasn't been revoked or the file deleted.");
    }

    private IllegalArgumentException guestNotSupportedException() {
        return new IllegalArgumentException(
                "Attaching a CV from Google Drive requires a Google account. "
                        + "Guest sessions aren't linked to Google Drive yet — "
                        + "sign in with Google to connect your Drive and attach CVs.");
    }

    private String getFreshTokenForCurrentUser() {
        Long currentUserId = securityContextService.getCurrentUserId();

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in database: " + currentUserId));

        // Guests never go through the Google OAuth flow, so they have no
        // access/refresh token to work with. Without this check,
        // tokenManager.getValidAccessToken(user) would be handed a user with
        // null tokens and fail with an opaque internal error instead of a
        // clear, actionable message.
        if (Boolean.TRUE.equals(user.getIsGuest())) {
            log.info("Guest user {} attempted to use Google Drive CV storage", user.getId());
            throw guestNotSupportedException();
        }

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