package com.applyflow.tracker_api.repositories;

import com.applyflow.tracker_api.models.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.skills WHERE c.user.id = :userId")
    List<Category> findAllWithSkillsByUserId(@Param("userId") Long userId);

    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.skills WHERE c.id = :id AND c.user.id = :userId")
    Optional<Category> findByIdAndUserIdWithSkills(@Param("id") Long id, @Param("userId") Long userId);

    Optional<Category> findByIdAndUserId(Long id, Long userId);

    boolean existsByNameAndUserId(String name, Long userId);
    
    List<Category> findAllByUserId(Long userId); // Spring Data resolves this to user.id

    @Query("SELECT c.name FROM Category c WHERE c.user.id = :userId")
    List<String> findAllNamesByUserId(@Param("userId") Long userId);
}