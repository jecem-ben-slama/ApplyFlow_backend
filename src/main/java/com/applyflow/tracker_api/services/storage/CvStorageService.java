package com.applyflow.tracker_api.services.storage;

public interface CvStorageService {
    byte[] downloadFile(String fileUrl);

    boolean supports(String fileUrl);
}