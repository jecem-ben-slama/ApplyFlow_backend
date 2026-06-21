package com.applyflow.tracker_api.services;

import com.applyflow.tracker_api.models.CvVariant;
import com.applyflow.tracker_api.repositories.CvVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CvVariantService {

    private final CvVariantRepository cvVariantRepository;

    public CvVariant createCvVariant(CvVariant cvVariant) {
        return cvVariantRepository.save(cvVariant);
    }

    public List<CvVariant> getCvVariantsForUser(Long userId) {
        return cvVariantRepository.findByUserId(userId);
    }

    public List<CvVariant> getCvVariantsForUserByLanguage(Long userId, String language) {
        return cvVariantRepository.findByUserIdAndLanguage(userId, language);
    }

    public CvVariant getCvVariantByIdAndUser(Long id, Long userId) {
        return cvVariantRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("CV Variant not found or access denied."));
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