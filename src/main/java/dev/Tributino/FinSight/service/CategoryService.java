package dev.Tributino.FinSight.service;

import dev.Tributino.FinSight.domain.Category;
import dev.Tributino.FinSight.domain.User;
import dev.Tributino.FinSight.dto.category.CategoryRequest;
import dev.Tributino.FinSight.dto.category.CategoryResponse;
import dev.Tributino.FinSight.enums.CategoryType;
import dev.Tributino.FinSight.mapper.CategoryMapper;
import dev.Tributino.FinSight.repository.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    public CategoryResponse findById(Long id, User loggedUser) {
        Category category = findEntityByIdAndUser(id, loggedUser);
        return categoryMapper.toResponse(category);
    }

    Category findEntityByIdAndUser(Long id, User loggedUser) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));

        if (category.getUser() != null && !category.getUser().getId().equals(loggedUser.getId())) {
            throw new AccessDeniedException("Access denied: This category belongs to another user.");
        }
        return category;
    }

    public List<CategoryResponse> findIncomesByUser(User loggedUser) {
        return categoryRepository.findAvailableCategoriesByTypeAndUser(CategoryType.INCOME, loggedUser)
                .stream().map(categoryMapper::toResponse).toList();
    }

    public List<CategoryResponse> findExpensesByUser(User loggedUser) {
        return categoryRepository.findAvailableCategoriesByTypeAndUser(CategoryType.EXPENSE, loggedUser)
                .stream().map(categoryMapper::toResponse).toList();
    }

    public CategoryResponse createCustomCategory(CategoryRequest categoryRequest, User loggedUser) {
        Category newCategory = new Category(
                categoryRequest.name(),
                categoryRequest.categoryType(),
                loggedUser
        );

        Category savedCategory = categoryRepository.save(newCategory);
        return categoryMapper.toResponse(savedCategory);
    }

    public CategoryResponse updateCategory(Long categoryId, String newName, User loggedUser) {
        Category category = findEntityByIdAndUser(categoryId, loggedUser);
        validateCustomCategory(category);

        category.updateName(newName);
        Category updatedCategory = categoryRepository.save(category);
        return categoryMapper.toResponse(updatedCategory);
    }

    public void deleteCategory(Long categoryId, User loggedUser) {
        Category category = findEntityByIdAndUser(categoryId, loggedUser);
        validateCustomCategory(category);

        category.archive();
        categoryRepository.save(category);
    }

    private void validateCustomCategory(Category category) {
        if (category.getUser() == null) {
            throw new IllegalArgumentException("You cannot modify or delete the system's default categories.");
        }
    }
}