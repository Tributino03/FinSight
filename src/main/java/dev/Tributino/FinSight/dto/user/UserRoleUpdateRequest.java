package dev.Tributino.FinSight.dto.user;

import dev.Tributino.FinSight.enums.Role;
import jakarta.validation.constraints.NotNull;

public record UserRoleUpdateRequest(@NotNull Role role) {}