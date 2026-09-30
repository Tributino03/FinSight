package dev.Tributino.FinSight.dto.account;

import jakarta.validation.constraints.NotBlank;

public record AccountUpdateRequest(
        @NotBlank(message = "Account name is required")
        String newName
) {
}
