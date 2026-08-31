package dev.Tributino.FinSight.service;

import dev.Tributino.FinSight.domain.Category;
import dev.Tributino.FinSight.domain.User;
import dev.Tributino.FinSight.enums.CategoryType;
import dev.Tributino.FinSight.repository.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserService userService;

    public CategoryService(CategoryRepository categoryRepository, UserService userService) {
        this.categoryRepository = categoryRepository;
        this.userService = userService;
    }

    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    public Category findById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));
        return category;
    }

    public List<Category> findIncomesByUser(Long userId) {
        User user = userService.findById(userId);
        return categoryRepository.findAvailableCategoriesByTypeAndUser(CategoryType.INCOME, user);
    }

    public List<Category> findExpensesByUser(Long userId) {
        User user = userService.findById(userId);
        return categoryRepository.findAvailableCategoriesByTypeAndUser(CategoryType.EXPENSE, user);
    }

    public Category createCustomCategory(Category categoryInput, Long userId) {
        User user = userService.findById(userId);

        Category newCategory = new Category(
                categoryInput.getName(),
                categoryInput.getCategoryType(),
                user
        );

        return categoryRepository.save(newCategory);
    }

    public Category updateCategory(Long categoryId, String newName) {
        Category category = findById(categoryId);

        validateCustomCategory(category);

        category.updateName(newName);
        return categoryRepository.save(category);
    }

    public void deleteCategory(Long categoryId) {
        Category category = findById(categoryId);

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