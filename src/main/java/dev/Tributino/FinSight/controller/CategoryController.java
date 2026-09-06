package dev.Tributino.FinSight.controller;

import dev.Tributino.FinSight.dto.category.CategoryRequest;
import dev.Tributino.FinSight.dto.category.CategoryResponse;
import dev.Tributino.FinSight.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> findAll() {
        return ResponseEntity.ok(categoryService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.findById(id));
    }

    @GetMapping("/user/{userId}/income")
    public ResponseEntity<List<CategoryResponse>> findIncomesByUser(
            @PathVariable Long userId) {

        return ResponseEntity.ok(categoryService.findIncomesByUser(userId));
    }

    @GetMapping("/user/{userId}/expense")
    public ResponseEntity<List<CategoryResponse>> findExpensesByUser(
            @PathVariable Long userId) {

        return ResponseEntity.ok(categoryService.findExpensesByUser(userId));
    }

    @PostMapping("/user/{userId}")
    public ResponseEntity<CategoryResponse> createCustomCategory(@Valid @RequestBody CategoryRequest categoryRequest, @PathVariable Long userId) {

        CategoryResponse createdCategory = categoryService.createCustomCategory(categoryRequest, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCategory);

    }

    @PutMapping("{categoryId}/name")
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable Long categoryId, @RequestParam String newName) {

        CategoryResponse updateCategory = categoryService.updateCategory(categoryId, newName);
        return ResponseEntity.ok(updateCategory);

    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long categoryId) {
        categoryService.deleteCategory(categoryId);
        return ResponseEntity.noContent().build();
    }

}
