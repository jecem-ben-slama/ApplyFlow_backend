package com.applyflow.tracker_api.services.storage;

public interface CvStorageService {

    byte[] downloadFile(String fileUrl);

    boolean supports(String fileUrl);

    /**
     * Cheap validation: confirms the URL is well-formed, points at an
     * accessible file, and is the right type — without downloading the file
     * body. Implementations should throw IllegalArgumentException with a
     * clear, user-facing message on any problem (bad link format, not
     * shared/private, wrong file type, etc).
     */
    void validateFile(String fileUrl);
}