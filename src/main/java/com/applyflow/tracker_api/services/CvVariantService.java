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

    public List<CvVariant> getAllCvVariants() {
        return cvVariantRepository.findAll();
    }

    public CvVariant getCvVariantById(Long id) {
        return cvVariantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("CV Variant not found with id: " + id));
    }

    @Transactional
    public CvVariant updateCvVariant(Long id, CvVariant details) {
        CvVariant existing = getCvVariantById(id);

        existing.setName(details.getName());
        existing.setLanguage(details.getLanguage());
        existing.setFileUrl(details.getFileUrl());

        return cvVariantRepository.save(existing);
    }

    public void deleteCvVariant(Long id) {
        CvVariant cv = getCvVariantById(id);
        cvVariantRepository.delete(cv);
    }
}