package com.applyflow.tracker_api.repositories;

import com.applyflow.tracker_api.models.CvVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CvVariantRepository extends JpaRepository<CvVariant, Long> {
    // Allows your UI to fetch only French CVs or only English CVs
    List<CvVariant> findByLanguage(String language);
}