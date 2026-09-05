package dev.Tributino.FinSight.service;

import dev.Tributino.FinSight.domain.Category;
import dev.Tributino.FinSight.domain.User;
import dev.Tributino.FinSight.dto.category.CategoryRequest; // Mudado de Response para Request
import dev.Tributino.FinSight.dto.category.CategoryResponse;
import dev.Tributino.FinSight.enums.CategoryType;
import dev.Tributino.FinSight.mapper.CategoryMapper;
import dev.Tributino.FinSight.repository.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserService userService;
    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryRepository categoryRepository, UserService userService, CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.userService = userService;
        this.categoryMapper = categoryMapper;
    }

    public List<CategoryResponse> findAll() {
        return categoryRepository
                .findAll()
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    public CategoryResponse findById(Long id) {
        Category category = findEntityById(id);
        return categoryMapper.toResponse(category);
    }

    Category findEntityById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));
    }

    public List<CategoryResponse> findIncomesByUser(Long userId) {
        User user = userService.findById(userId);
        return categoryRepository.findAvailableCategoriesByTypeAndUser(CategoryType.INCOME, user)
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    public List<CategoryResponse> findExpensesByUser(Long userId) {
        User user = userService.findById(userId);
        return categoryRepository.findAvailableCategoriesByTypeAndUser(CategoryType.EXPENSE, user)
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    public CategoryResponse createCustomCategory(CategoryRequest categoryRequest, Long userId) {
        User user = userService.findById(userId);

        Category newCategory = new Category(
                categoryRequest.name(),
                categoryRequest.categoryType(),
                user
        );

        Category savedCategory = categoryRepository.save(newCategory);
        return categoryMapper.toResponse(savedCategory);
    }

    public CategoryResponse updateCategory(Long categoryId, String newName) {
        Category category = findEntityById(categoryId);

        validateCustomCategory(category);

        category.updateName(newName);
        Category updatedCategory = categoryRepository.save(category);

        return categoryMapper.toResponse(updatedCategory);
    }

    public void deleteCategory(Long categoryId) {
        Category category = findEntityById(categoryId);

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