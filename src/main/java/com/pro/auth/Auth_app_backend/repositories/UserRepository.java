package com.pro.auth.Auth_app_backend.repositories;

import com.pro.auth.Auth_app_backend.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findbyemail(String email);
    boolean existsByEmail(String email);
}
