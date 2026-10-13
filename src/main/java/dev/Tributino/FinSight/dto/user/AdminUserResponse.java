package dev.Tributino.FinSight.dto.user;

import dev.Tributino.FinSight.enums.Role;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String name,
        String email,
        Role role,
        boolean enabled,
        LocalDateTime createdAt
) {
}
