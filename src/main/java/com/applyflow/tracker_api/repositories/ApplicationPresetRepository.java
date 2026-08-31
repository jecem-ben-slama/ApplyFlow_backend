package com.applyflow.tracker_api.repositories;

import com.applyflow.tracker_api.models.ApplicationPreset;
import com.applyflow.tracker_api.models.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplicationPresetRepository extends JpaRepository<ApplicationPreset, Long> {

     Optional<ApplicationPreset> findByIdAndUserId(Long id, Long userId);

     /** Used on create — is this name already taken by any preset this user owns? */
     boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);

     @Modifying(clearAutomatically = true, flushAutomatically = true)
     @Query("UPDATE ApplicationPreset p SET p.user = :realUser WHERE p.user.id = :guestId")
     void reassignOwner(@Param("guestId") Long guestId, @Param("realUser") User realUser);

     /**
      * Used on update — same check, but excluding the preset currently being edited.
      */
     boolean existsByUserIdAndNameIgnoreCaseAndIdNot(Long userId, String name, Long id);

     // Same nullable-param pattern as ApplicationRepository.findByUserIdWithFilters:
     // every occurrence of a nullable string used inside LOWER()/LIKE needs its
     // own explicit CAST(:x AS string), not just the null-check occurrence.
     @EntityGraph(attributePaths = { "skills" })
     @Query("""
               SELECT p FROM ApplicationPreset p
               WHERE p.user.id = :userId
               AND (CAST(:language AS string) IS NULL OR LOWER(p.language) = LOWER(CAST(:language AS string)))
               AND (CAST(:keyword AS string) IS NULL OR
                    LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR
                    LOWER(p.jobTitle) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
               """)
     Page<ApplicationPreset> findByUserIdWithFilters(@Param("userId") Long userId,
               @Param("keyword") String keyword,
               @Param("language") String language,
               Pageable pageable);
}