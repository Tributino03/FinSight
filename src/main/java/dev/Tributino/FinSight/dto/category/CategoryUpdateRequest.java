package dev.Tributino.FinSight.dto.category;

import jakarta.validation.constraints.NotBlank;

public record CategoryUpdateRequest(
        @NotBlank(message = "Category name is required")
        String newName
) {
}
