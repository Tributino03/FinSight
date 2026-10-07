package dev.Tributino.FinSight.dto.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccountUpdateRequest(
        @NotBlank(message = "Account name is required")
        @Size(min = 5, max = 100)
        String newName
) {
}
