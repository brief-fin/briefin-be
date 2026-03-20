package com.briefin.domain.users.repository;

import com.briefin.domain.users.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
@Repository

public interface UsersRepository extends JpaRepository<Users, UUID> {
}
