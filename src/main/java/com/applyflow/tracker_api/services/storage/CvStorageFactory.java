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
                        "This CV link isn't from a supported storage provider. "
                                + "Currently only Google Drive share links are supported."));
    }
}