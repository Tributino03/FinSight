package dev.Tributino.FinSight.dto.category;

import dev.Tributino.FinSight.enums.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CategoryRequest(

        @NotBlank(message = "Category name is required")
        String name,

        @NotNull(message = "Category type is required")
        CategoryType categoryType

) {
}