package com.applyflow.tracker_api.services;

import com.applyflow.tracker_api.models.Category;
import com.applyflow.tracker_api.models.User;
import com.applyflow.tracker_api.repositories.CategoryRepository;
import com.applyflow.tracker_api.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public List<Category> getAllCategories(Long userId) {
        return categoryRepository.findAllByUserId(userId);
    }

    public Category getCategoryById(Long categoryId, Long userId) {
        return categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Category not found"));
    }

    @Transactional
    public Category createCategory(Long userId, String name) {
        if (categoryRepository.existsByNameAndUserId(name, userId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "A category with that name already exists");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        Category category = Category.builder()
                .name(name)
                .user(user)
                .build();

        return categoryRepository.save(category);
    }

    @Transactional
    public Category updateCategory(Long categoryId, Long userId, String name) {
        Category category = categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Category not found"));

        if (!category.getName().equals(name)
                && categoryRepository.existsByNameAndUserId(name, userId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "A category with that name already exists");
        }

        category.setName(name);
        return categoryRepository.save(category);
    }

    @Transactional
    public void deleteCategory(Long categoryId, Long userId) {
        Category category = categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Category not found"));

        if (!category.getSkills().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete category — " + category.getSkills().size()
                            + " skill(s) are still assigned to it. Reassign them first.");
        }

        categoryRepository.delete(category);
    }
}