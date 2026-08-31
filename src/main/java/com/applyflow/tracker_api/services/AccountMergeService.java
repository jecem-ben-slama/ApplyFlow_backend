package com.applyflow.tracker_api.services;

import com.applyflow.tracker_api.models.Category;
import com.applyflow.tracker_api.models.Template;
import com.applyflow.tracker_api.models.User;
import com.applyflow.tracker_api.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountMergeService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TemplateRepository templateRepository;
    private final SkillRepository skillRepository;
    private final CvVariantRepository cvVariantRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationPresetRepository applicationPresetRepository;

    /**
     * Reassigns every entity owned by the guest to the real user instead of
     * deleting it. Categories/templates that collide by name with something
     * the real user already owns get suffixed rather than dropped or left to
     * violate the unique constraint.
     */
    @Transactional
    public void mergeGuestInto(User guest, User realUser) {
        Long guestId = guest.getId();
        Long realUserId = realUser.getId();

        reassignCategoriesWithRenaming(guestId, realUser);
        reassignTemplatesWithRenaming(guestId, realUser);

        // No name-collision risk on these — plain bulk reassignment via JPQL.
        skillRepository.reassignOwner(guestId, realUser);
        cvVariantRepository.reassignOwner(guestId, realUser);
        applicationRepository.reassignOwner(guestId, realUser);
        applicationPresetRepository.reassignOwner(guestId, realUser);

        // The @Modifying queries above run outside the persistence context's
        // normal dirty-checking, so we reload the guest fresh before deleting
        // it — otherwise Hibernate may still think the (now-reassigned) child
        // rows belong to guest's in-memory collections and cascade-delete them.
        User freshGuest = userRepository.findById(guestId)
                .orElseThrow(() -> new IllegalStateException(
                        "Guest user " + guestId + " vanished mid-merge"));
        userRepository.delete(freshGuest);

        log.info("Merged guest {} into user {}", guestId, realUserId);
    }

    private void reassignCategoriesWithRenaming(Long guestId, User realUser) {
        Set<String> existingNames = new HashSet<>(
                categoryRepository.findAllNamesByUserId(realUser.getId()));

        for (Category category : categoryRepository.findAllByUserId(guestId)) {
            category.setName(dedupeName(category.getName(), existingNames));
            category.setUser(realUser);
            categoryRepository.save(category);
        }
    }

    private void reassignTemplatesWithRenaming(Long guestId, User realUser) {
        Set<String> existingNames = new HashSet<>(
                templateRepository.findAllNamesByUserId(realUser.getId()));

        for (Template template : templateRepository.findAllByUserId(guestId)) {
            template.setName(dedupeName(template.getName(), existingNames));
            template.setUser(realUser);
            templateRepository.save(template);
        }
    }

    private String dedupeName(String originalName, Set<String> existingNames) {
        if (!existingNames.contains(originalName)) {
            existingNames.add(originalName);
            return originalName;
        }
        String candidate = originalName + " (imported)";
        int suffix = 2;
        while (existingNames.contains(candidate)) {
            candidate = originalName + " (imported " + suffix++ + ")";
        }
        existingNames.add(candidate);
        return candidate;
    }
}