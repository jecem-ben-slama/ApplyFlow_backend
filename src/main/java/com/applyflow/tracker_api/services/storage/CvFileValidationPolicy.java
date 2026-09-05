package com.applyflow.tracker_api.services.storage;

import org.springframework.stereotype.Component;

/**
 * Provider-agnostic rules for what counts as an acceptable CV file:
 * the right mime type and within the size limit. Any CvStorageService
 * implementation can delegate to this instead of duplicating the checks,
 * so the rules (and their user-facing wording) stay consistent no matter
 * which storage provider a file came from.
 */
@Component
public class CvFileValidationPolicy {

    private static final String ACCEPTED_MIME_TYPE = "application/pdf";

    // 10 MB — generous for a CV PDF, tight enough to bound memory/bandwidth
    // per attach. Adjust as needed.
    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;

    /**
     * Throws IllegalArgumentException with a clear, user-facing message if
     * the given mime type or size (in bytes) fails the policy. A null size
     * is treated as unknown and skipped rather than rejected, since some
     * providers may not always report it.
     */
    public void validate(String mimeType, Long sizeBytes, String fileNameForMessage) {
        if (mimeType == null) {
            throw new IllegalArgumentException("Unable to determine file type for this file.");
        }
        if (!ACCEPTED_MIME_TYPE.equals(mimeType)) {
            throw new IllegalArgumentException(
                    "Only PDF files are supported. This file is: " + mimeType
                            + (fileNameForMessage != null ? " (" + fileNameForMessage + ")" : ""));
        }
        if (sizeBytes != null && sizeBytes > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException(
                    "This file is too large to attach (" + formatSize(sizeBytes) + "). "
                            + "The maximum allowed size is " + formatSize(MAX_FILE_SIZE_BYTES) + ".");
        }
    }

    private String formatSize(long bytes) {
        double mb = bytes / (1024.0 * 1024.0);
        return String.format("%.1f MB", mb);
    }
}