package com.applyflow.tracker_api.controllers;

import com.applyflow.tracker_api.config.SecurityContextService;
import com.applyflow.tracker_api.dtos.ApiResponse;
import com.applyflow.tracker_api.dtos.SkillDto;
import com.applyflow.tracker_api.models.Skill;
import com.applyflow.tracker_api.models.User;
import com.applyflow.tracker_api.services.SkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class SkillController {

        private final SkillService skillService;
        private final SecurityContextService securityContextService;

        @PostMapping
        public ResponseEntity<ApiResponse<SkillDto>> createSkill(@RequestBody SkillDto skillDto) {
                Long userId = securityContextService.getCurrentUserId();

                Skill skillEntity = Skill.builder()
                                .name(skillDto.getName())
                                .sentenceEn(skillDto.getSentenceEn())
                                .sentenceFr(skillDto.getSentenceFr())
                                .user(User.builder().id(userId).build())
                                .build();

                Skill saved = skillService.createSkill(skillEntity, skillDto.getCategoryId(), userId);
                return new ResponseEntity<>(
                                ApiResponse.success("Skill created successfully", convertToDto(saved)),
                                HttpStatus.CREATED);
        }

        @GetMapping
        public ResponseEntity<ApiResponse<Page<SkillDto>>> getAllSkills(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(defaultValue = "id") String sortBy,
                        @RequestParam(defaultValue = "asc") String direction,
                        @RequestParam(required = false) Long categoryId) {

                Long userId = securityContextService.getCurrentUserId();

                Sort sort = direction.equalsIgnoreCase("desc")
                                ? Sort.by(sortBy).descending()
                                : Sort.by(sortBy).ascending();
                Pageable pageable = PageRequest.of(page, size, sort);

                Page<SkillDto> responseData = skillService
                                .getSkillsForUser(userId, categoryId, pageable)
                                .map(this::convertToDto);

                return ResponseEntity.ok(ApiResponse.success("Skills retrieved successfully", responseData));
        }

        @GetMapping("/{id}")
        public ResponseEntity<ApiResponse<SkillDto>> getSkillById(@PathVariable Long id) {
                Long userId = securityContextService.getCurrentUserId();
                Skill skill = skillService.getSkillByIdAndUser(id, userId);
                return ResponseEntity.ok(ApiResponse.success("Skill retrieved successfully", convertToDto(skill)));
        }

        @PutMapping("/{id}")
        public ResponseEntity<ApiResponse<SkillDto>> updateSkill(
                        @PathVariable Long id,
                        @RequestBody SkillDto skillDto) {

                Long userId = securityContextService.getCurrentUserId();

                Skill skillDetails = Skill.builder()
                                .name(skillDto.getName())
                                .sentenceEn(skillDto.getSentenceEn())
                                .sentenceFr(skillDto.getSentenceFr())
                                .build();

                Skill updated = skillService.updateSkill(id, userId, skillDetails, skillDto.getCategoryId());
                return ResponseEntity.ok(ApiResponse.success("Skill updated successfully", convertToDto(updated)));
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<ApiResponse<Void>> deleteSkill(@PathVariable Long id) {
                Long userId = securityContextService.getCurrentUserId();
                skillService.deleteSkill(id, userId);
                return ResponseEntity.ok(ApiResponse.success("Skill deleted successfully"));
        }

        private SkillDto convertToDto(Skill skill) {
                return SkillDto.builder()
                                .id(skill.getId())
                                .name(skill.getName())
                                .sentenceEn(skill.getSentenceEn())
                                .sentenceFr(skill.getSentenceFr())
                                .userId(skill.getUser() != null ? skill.getUser().getId() : null)
                                .categoryId(skill.getCategory() != null ? skill.getCategory().getId() : null)
                                .categoryName(skill.getCategory() != null ? skill.getCategory().getName() : null)
                                .build();
        }
}