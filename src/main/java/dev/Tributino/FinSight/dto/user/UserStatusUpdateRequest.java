package dev.Tributino.FinSight.dto.user;

import jakarta.validation.constraints.NotNull;

public record UserStatusUpdateRequest(
        @NotNull Boolean enabled
) {}