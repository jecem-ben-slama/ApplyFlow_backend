package com.applyflow.tracker_api.services;

import com.applyflow.tracker_api.models.CvVariant;
import com.applyflow.tracker_api.repositories.CvVariantRepository;
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

    @Transactional
    public CvVariant createCvVariant(CvVariant cvVariant) {
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
                        HttpStatus.NOT_FOUND, "CV variant not found or access denied."));
    }

    @Transactional
    public CvVariant updateCvVariant(Long id, Long userId, CvVariant details) {
        CvVariant existing = getCvVariantByIdAndUser(id, userId);

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
}