package com.applyflow.tracker_api.repositories;

import com.applyflow.tracker_api.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Looks up a user by their unique Google Subject ID string
    Optional<User> findByGoogleSub(String googleSub);

    // Optional fallback to find a user by their registered email
    Optional<User> findByEmail(String email);
}