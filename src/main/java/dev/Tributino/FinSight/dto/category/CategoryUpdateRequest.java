package dev.Tributino.FinSight.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryUpdateRequest(
        @NotBlank(message = "Category name is required")
        @Size(min = 5, max = 100)
        String newName
) {
}
