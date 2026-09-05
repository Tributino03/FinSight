package dev.Tributino.FinSight.dto.category;

import dev.Tributino.FinSight.enums.CategoryType;

public record CategoryRequest(
        String name,
        CategoryType categoryType
) {
}
