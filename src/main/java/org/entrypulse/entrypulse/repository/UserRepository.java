package org.entrypulse.entrypulse.repository;

import org.entrypulse.entrypulse.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);

    List<User> findByRole(User.Role role);

    List<User> findByRoleAndNameContainingIgnoreCase(
            User.Role role,
            String name
    );

    Page<User> findByRoleAndNameContainingIgnoreCaseOrRoleAndEmailContainingIgnoreCase(
            User.Role nameRole,
            String name,
            User.Role emailRole,
            String email,
            Pageable pageable
    );
}
