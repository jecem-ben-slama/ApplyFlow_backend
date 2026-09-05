package com.applyflow.tracker_api.services.storage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Google Drive-specific concerns only: parsing Drive share links, calling
 * the Drive API for metadata/bytes, and translating Drive's error responses
 * into user-facing messages. File-type/size rules live in
 * CvFileValidationPolicy, and OAuth token provisioning + guest-check logic
 * lives in GoogleOAuthTokenProvider — both injected rather than
 * reimplemented here.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleDriveStorageService implements CvStorageService {

    // Only the standard public "Share > Copy link" format is accepted:
    // https://drive.google.com/file/d/{fileId}/view?usp=sharing (query string
    // optional)
    private static final Pattern PUBLIC_SHARE_LINK_PATTERN = Pattern
            .compile("^https://drive\\.google\\.com/file/d/([a-zA-Z0-9_-]+)/view(?:\\?.*)?$");

    private final RestTemplate restTemplate = new RestTemplate();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final GoogleOAuthTokenProvider tokenProvider;
    private final CvFileValidationPolicy validationPolicy;

    @Override
    public void validateFile(String fileUrl) {
        // Runs the same link-format, access, type, and size checks as
        // downloadFile, minus the actual byte download. Throws
        // IllegalArgumentException with a specific, user-facing reason on
        // any problem.
        resolveAndValidateMetadata(fileUrl);
    }

    @Override
    public InputStreamSource downloadFile(String fileUrl) {

        // Validate up front so a bad/oversized/inaccessible link fails fast,
        // before we ever return a source to the caller.
        String fileId = resolveAndValidateMetadata(fileUrl);

        // Lazy + re-openable: nothing is fetched until getInputStream() is
        // called, and each call opens a fresh connection. This matters
        // because JavaMail's DataHandler may read the stream more than once
        // (e.g. once to sniff content, once to write it out) — a one-shot
        // InputStream would silently produce a truncated attachment on the
        // second read.
        return () -> openDriveMediaStream(fileId);
    }

    @Override
    public boolean supports(String fileUrl) {
        return fileUrl != null && fileUrl.contains("drive.google.com");
    }

    private InputStream openDriveMediaStream(String fileId) {
        String accessToken = tokenProvider.getFreshTokenForCurrentUser();
        String downloadUrl = "https://www.googleapis.com/drive/v3/files/" + fileId + "?alt=media";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(downloadUrl))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<InputStream> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofInputStream());

            int status = response.statusCode();
            if (status == HttpStatus.FORBIDDEN.value() || status == HttpStatus.NOT_FOUND.value()) {
                log.warn("Access denied downloading Drive file bytes. fileId={}, status={}", fileId, status);
                throw notAccessibleException();
            }
            if (status != HttpStatus.OK.value()) {
                log.error("Unexpected status downloading Drive file bytes. fileId={}, status={}", fileId, status);
                throw new RuntimeException("Google Drive CV download failed with status " + status);
            }

            return response.body();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Failed to open byte stream from Google Drive for File ID: {}", fileId, e);
            throw new RuntimeException("Google Drive CV download failed: " + e.getMessage(), e);
        }
    }

    /**
     * Parses the URL, fetches metadata, and delegates to the validation
     * policy to confirm type/size. Returns the fileId on success. Throws
     * IllegalArgumentException with a clear, specific reason on any failure
     * — shared by validateFile() (add/edit time) and downloadFile() (send
     * time), so both moments give the user the same precise explanation,
     * and so a file swapped out for something larger/wrong-typed after
     * being added is still caught right before it's actually fetched.
     */
    private String resolveAndValidateMetadata(String fileUrl) {
        String fileId = extractFileIdFromUrl(fileUrl);
        String accessToken = tokenProvider.getFreshTokenForCurrentUser();

        DriveFileMetadata metadata = getFileMetadata(fileId, accessToken);
        if (metadata == null) {
            throw new IllegalArgumentException("Unable to determine file type for this Google Drive file.");
        }
        if (metadata.getMimeType() == null || !"application/pdf".equals(metadata.getMimeType())) {
            log.warn("Rejected non-PDF Drive file. fileId={}, name={}, mimeType={}",
                    fileId, metadata.getName(), metadata.getMimeType());
        }

        // Delegates the actual type/size decision (and its exact wording)
        // to the shared policy, so the rules stay identical across every
        // storage provider instead of being reimplemented per provider.
        validationPolicy.validate(metadata.getMimeType(), metadata.getSize(), metadata.getName());
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
        String metaUrl = "https://www.googleapis.com/drive/v3/files/" + fileId + "?fields=id,name,mimeType,size";

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

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class DriveFileMetadata {
        private String id;
        private String name;
        private String mimeType;
        private Long size; // Drive returns this as a string in JSON; Jackson coerces to Long fine
    }
}