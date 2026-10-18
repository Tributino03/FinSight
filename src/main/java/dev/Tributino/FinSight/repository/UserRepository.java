package dev.Tributino.FinSight.repository;

import dev.Tributino.FinSight.domain.User;
import dev.Tributino.FinSight.enums.Role;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE " +
            "(:name IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
            "(:role IS NULL OR u.role = :role) AND " +
            "(:enabled IS NULL OR u.enabled = :enabled)")
    Page<User> findByFilters(
            @Param("name") String name,
            @Param("email") String email,
            @Param("role") Role role,
            @Param("enabled") Boolean enabled,
            Pageable pageable
    );

    Page<User> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<User> findByEmailContainingIgnoreCase(String email, Pageable pageable);

    Page<User> findByRole(Role role, Pageable pageable);

    Page<User> findByEnabled(boolean enabled, Pageable pageable);

    long countByRoleAndEnabledTrue(Role role);
}