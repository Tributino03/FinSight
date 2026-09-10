package dev.Tributino.FinSight.controller;

import dev.Tributino.FinSight.domain.User;
import dev.Tributino.FinSight.dto.category.CategoryRequest;
import dev.Tributino.FinSight.dto.category.CategoryResponse;
import dev.Tributino.FinSight.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> findById(
            @PathVariable Long id,
            @AuthenticationPrincipal User loggedUser) {
        return ResponseEntity.ok(categoryService.findById(id, loggedUser));
    }

    @GetMapping("/income")
    public ResponseEntity<List<CategoryResponse>> findIncomesByUser(@AuthenticationPrincipal User loggedUser) {
        return ResponseEntity.ok(categoryService.findIncomesByUser(loggedUser));
    }

    @GetMapping("/expense")
    public ResponseEntity<List<CategoryResponse>> findExpensesByUser(@AuthenticationPrincipal User loggedUser) {
        return ResponseEntity.ok(categoryService.findExpensesByUser(loggedUser));
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCustomCategory(
            @Valid @RequestBody CategoryRequest categoryRequest,
            @AuthenticationPrincipal User loggedUser) {

        CategoryResponse createdCategory = categoryService.createCustomCategory(categoryRequest, loggedUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCategory);
    }

    @PutMapping("/{categoryId}/name")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable Long categoryId,
            @RequestParam String newName,
            @AuthenticationPrincipal User loggedUser) {

        CategoryResponse updateCategory = categoryService.updateCategory(categoryId, newName, loggedUser);
        return ResponseEntity.ok(updateCategory);
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Long categoryId,
            @AuthenticationPrincipal User loggedUser) {

        categoryService.deleteCategory(categoryId, loggedUser);
        return ResponseEntity.noContent().build();
    }
}