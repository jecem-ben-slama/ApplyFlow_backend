package com.applyflow.tracker_api.services.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CvStorageFactory {

    private final List<CvStorageService> storageServices;

    public CvStorageService getServiceForUrl(String fileUrl) {
        return storageServices.stream()
                .filter(service -> service.supports(fileUrl))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No storage provider configured to support URL: " + fileUrl));
    }
}