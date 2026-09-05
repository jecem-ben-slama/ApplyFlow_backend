package com.applyflow.tracker_api.services.storage;

import org.springframework.core.io.InputStreamSource;

public interface CvStorageService {

    /**
     * Returns a lazy, re-openable source of the file's bytes. No network
     * call happens until getInputStream() is actually invoked on the
     * returned source — implementations should open a fresh connection on
     * each call, since JavaMail may read the stream more than once.
     */
    InputStreamSource downloadFile(String fileUrl);

    boolean supports(String fileUrl);

    /**
     * Cheap validation: confirms the URL is well-formed, points at an
     * accessible file, is the right type, and is within the allowed size —
     * without downloading the file body. Implementations should throw
     * IllegalArgumentException with a clear, user-facing message on any
     * problem (bad link format, not shared/private, wrong file type, too
     * large, etc).
     */
    void validateFile(String fileUrl);
}