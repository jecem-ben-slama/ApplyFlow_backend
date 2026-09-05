package com.applyflow.tracker_api.services;

import com.applyflow.tracker_api.models.CvVariant;
import com.applyflow.tracker_api.repositories.CvVariantRepository;
import com.applyflow.tracker_api.services.storage.CvStorageFactory;
import com.applyflow.tracker_api.services.storage.CvStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CvVariantService {

    private final CvVariantRepository cvVariantRepository;
    private final CvStorageFactory storageFactory;

    @Transactional
    public CvVariant createCvVariant(CvVariant cvVariant) {
        validateFileUrl(cvVariant.getFileUrl());
        return cvVariantRepository.save(cvVariant);
    }

    @Transactional(readOnly = true)
    public Page<CvVariant> getCvVariantsForUser(Long userId, String language, String search, Pageable pageable) {
        return cvVariantRepository.findByUserIdWithFilters(userId, language, search, pageable);
    }

    @Transactional(readOnly = true)
    public CvVariant getCvVariantByIdAndUser(Long id, Long userId) {
        return cvVariantRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "CV not found or access denied."));
    }

    @Transactional
    public CvVariant updateCvVariant(Long id, Long userId, CvVariant details) {
        CvVariant existing = getCvVariantByIdAndUser(id, userId);

        validateFileUrl(details.getFileUrl());

        existing.setName(details.getName());
        existing.setLanguage(details.getLanguage());
        existing.setFileUrl(details.getFileUrl());

        return cvVariantRepository.save(existing);
    }

    @Transactional
    public void deleteCvVariant(Long id, Long userId) {
        CvVariant cv = getCvVariantByIdAndUser(id, userId);
        cvVariantRepository.delete(cv);
    }

    /**
     * Confirms the CV link is well-formed, points at an accessible file, and
     * is the right type — before we ever save it. This means a bad link is
     * rejected the moment the user adds or edits a CV, instead of failing
     * silently later when they try to send an application email.
     */
    private void validateFileUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new IllegalArgumentException("A CV file link is required.");
        }

        CvStorageService storageService = storageFactory.getServiceForUrl(fileUrl);
        storageService.validateFile(fileUrl);
    }
}