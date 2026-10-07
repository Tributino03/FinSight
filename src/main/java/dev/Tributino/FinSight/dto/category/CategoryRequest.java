package dev.Tributino.FinSight.dto.category;

import dev.Tributino.FinSight.enums.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryRequest(

        @NotBlank(message = "Category name is required")
        @Size(min = 5, max = 100)
        String name,

        @NotNull(message = "Category type is required")
        CategoryType categoryType

) {
}