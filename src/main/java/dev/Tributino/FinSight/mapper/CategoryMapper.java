package dev.Tributino.FinSight.mapper;

import dev.Tributino.FinSight.domain.Category;
import dev.Tributino.FinSight.domain.User;
import dev.Tributino.FinSight.dto.category.CategoryResponse;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public CategoryResponse toResponse(Category category){
        return  new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getCategoryType()
        );
    }
}
