package com.briefin.domain.auth.repository;

import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.briefin.domain.auth.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
}
