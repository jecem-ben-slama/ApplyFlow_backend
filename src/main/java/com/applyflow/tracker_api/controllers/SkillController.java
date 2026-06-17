package com.applyflow.tracker_api.controllers;

import com.applyflow.tracker_api.dtos.ApiResponse;
import com.applyflow.tracker_api.dtos.SkillDto;
import com.applyflow.tracker_api.models.Skill;
import com.applyflow.tracker_api.services.SkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    @PostMapping
    public ResponseEntity<ApiResponse<SkillDto>> createSkill(@RequestBody SkillDto skillDto) {
        Skill skillEntity = Skill.builder()
                .displayName(skillDto.getDisplayName())
                .technicalName(skillDto.getTechnicalName())
                .sentenceEn(skillDto.getSentenceEn())
                .sentenceFr(skillDto.getSentenceFr())
                .build();

        Skill savedSkill = skillService.createSkill(skillEntity);
        SkillDto responseData = convertToDto(savedSkill);

        return new ResponseEntity<>(
                ApiResponse.success("Skill created successfully", responseData),
                HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<SkillDto>>> getAllSkills(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        // 1. Set up the sorting direction
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        // 2. Create the Pageable object
        Pageable pageable = PageRequest.of(page, size, sort);

        // 3. Get paged entities from service and map them cleanly to DTOs
        Page<SkillDto> pagedDtos = skillService.getAllSkills(pageable)
                .map(this::convertToDto);

        return ResponseEntity.ok(ApiResponse.success("Master skills retrieved successfully", pagedDtos));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSkill(@PathVariable Long id) {
        skillService.deleteSkill(id);
        return ResponseEntity.ok(ApiResponse.success("Skill deleted successfully"));
    }

    private SkillDto convertToDto(Skill skill) {
        return SkillDto.builder()
                .id(skill.getId())
                .displayName(skill.getDisplayName())
                .technicalName(skill.getTechnicalName())
                .sentenceEn(skill.getSentenceEn())
                .sentenceFr(skill.getSentenceFr())
                .build();
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SkillDto>> updateSkill(
            @PathVariable Long id,
            @RequestBody SkillDto skillDto) {

        // 1. Map the incoming DTO data to your entity structure
        Skill skillDetails = Skill.builder()
                .displayName(skillDto.getDisplayName())
                .technicalName(skillDto.getTechnicalName())
                .sentenceEn(skillDto.getSentenceEn())
                .sentenceFr(skillDto.getSentenceFr())
                .build();

        // 2. Pass both the ID and the details to the service layer
        Skill updatedSkill = skillService.updateSkill(id, skillDetails);
        SkillDto responseData = convertToDto(updatedSkill);

        return ResponseEntity.ok(ApiResponse.success("Skill updated successfully", responseData));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SkillDto>> getSkillById(@PathVariable Long id) {
        // 1. Fetch the entity from the service layer
        Skill skill = skillService.getSkillById(id);

        // 2. Convert it to a DTO
        SkillDto responseData = convertToDto(skill);

        // 3. Return it wrapped in your success envelope
        return ResponseEntity.ok(ApiResponse.success("Skill retrieved successfully", responseData));
    }
}