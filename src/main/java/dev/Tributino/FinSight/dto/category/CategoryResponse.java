package dev.Tributino.FinSight.dto.category;

import dev.Tributino.FinSight.enums.CategoryType;

public record CategoryResponse(
        Long id,
        String name,
        CategoryType categoryType
) {
}
