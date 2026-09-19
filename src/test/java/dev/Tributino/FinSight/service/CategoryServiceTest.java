package dev.Tributino.FinSight.service;

import dev.Tributino.FinSight.domain.Category;
import dev.Tributino.FinSight.domain.User;
import dev.Tributino.FinSight.dto.category.CategoryRequest;
import dev.Tributino.FinSight.dto.category.CategoryResponse;
import dev.Tributino.FinSight.enums.CategoryType;
import dev.Tributino.FinSight.mapper.CategoryMapper;
import dev.Tributino.FinSight.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService Ownership Tests")
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    private User owner;
    private User attacker;
    private Category ownerCategory;
    private Category defaultCategory;
    private Category attackerCategory;

    @BeforeEach
    void setUp() {
        owner = mock(User.class);
        attacker = mock(User.class);

        lenient().when(owner.getId()).thenReturn(1L);
        lenient().when(attacker.getId()).thenReturn(2L);

        ownerCategory = mock(Category.class);
        defaultCategory = mock(Category.class);
        attackerCategory = mock(Category.class);

        lenient().when(ownerCategory.getUser()).thenReturn(owner);
        lenient().when(defaultCategory.getUser()).thenReturn(null);
        lenient().when(attackerCategory.getUser()).thenReturn(attacker);
    }

    @Nested
    @DisplayName("findAllByUser")
    class FindAllByUser {

        @Test
        @DisplayName("should return both owner categories and default system categories")
        void shouldReturnOwnerAndDefaultCategories() {
            CategoryResponse defaultResponse = mock(CategoryResponse.class);
            CategoryResponse ownerResponse = mock(CategoryResponse.class);

            List<Category> availableCategories = List.of(defaultCategory, ownerCategory);

            when(categoryRepository.findAllAvailableByUser(owner)).thenReturn(availableCategories);
            when(categoryMapper.toResponse(defaultCategory)).thenReturn(defaultResponse);
            when(categoryMapper.toResponse(ownerCategory)).thenReturn(ownerResponse);

            List<CategoryResponse> result = categoryService.findAllByUser(owner);

            assertThat(result).hasSize(2).containsExactly(defaultResponse, ownerResponse);
            verify(categoryRepository).findAllAvailableByUser(owner);
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return category when user is the owner")
        void shouldReturnCategoryWhenUserIsOwner() {
            CategoryResponse response = mock(CategoryResponse.class);

            when(categoryRepository.findById(10L)).thenReturn(Optional.of(ownerCategory));
            when(categoryMapper.toResponse(ownerCategory)).thenReturn(response);

            CategoryResponse result = categoryService.findById(10L, owner);

            assertThat(result).isNotNull().isEqualTo(response);
            verify(categoryRepository).findById(10L);
            verify(categoryMapper).toResponse(ownerCategory);
        }

        @Test
        @DisplayName("should return category when category is a default system category")
        void shouldReturnCategoryWhenCategoryIsDefault() {
            CategoryResponse response = mock(CategoryResponse.class);

            when(categoryRepository.findById(10L)).thenReturn(Optional.of(defaultCategory));
            when(categoryMapper.toResponse(defaultCategory)).thenReturn(response);

            CategoryResponse result = categoryService.findById(10L, owner);

            assertThat(result).isNotNull().isEqualTo(response);
            verify(categoryRepository).findById(10L);
            verify(categoryMapper).toResponse(defaultCategory);
        }

        @Test
        @DisplayName("should throw AccessDeniedException when category belongs to another user")
        void shouldThrowAccessDeniedExceptionWhenCategoryBelongsToAnotherUser() {
            when(categoryRepository.findById(10L)).thenReturn(Optional.of(ownerCategory));

            assertThatThrownBy(() -> categoryService.findById(10L, attacker))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Access denied: This category belongs to another user.");

            verify(categoryMapper, never()).toResponse(any());
        }
    }

    @Nested
    @DisplayName("findIncomesByUser")
    class FindIncomesByUser {

        @Test
        @DisplayName("should return available income categories for logged user")
        void shouldReturnAvailableIncomeCategories() {
            CategoryResponse response = mock(CategoryResponse.class);

            when(categoryRepository.findAvailableCategoriesByTypeAndUser(CategoryType.INCOME, owner))
                    .thenReturn(List.of(ownerCategory));
            when(categoryMapper.toResponse(ownerCategory)).thenReturn(response);

            List<CategoryResponse> result = categoryService.findIncomesByUser(owner);

            assertThat(result).hasSize(1).contains(response);
            verify(categoryRepository).findAvailableCategoriesByTypeAndUser(CategoryType.INCOME, owner);
        }
    }

    @Nested
    @DisplayName("findExpensesByUser")
    class FindExpensesByUser {

        @Test
        @DisplayName("should return available expense categories for logged user")
        void shouldReturnAvailableExpenseCategories() {
            CategoryResponse response = mock(CategoryResponse.class);

            when(categoryRepository.findAvailableCategoriesByTypeAndUser(CategoryType.EXPENSE, owner))
                    .thenReturn(List.of(ownerCategory));
            when(categoryMapper.toResponse(ownerCategory)).thenReturn(response);

            List<CategoryResponse> result = categoryService.findExpensesByUser(owner);

            assertThat(result).hasSize(1).contains(response);
            verify(categoryRepository).findAvailableCategoriesByTypeAndUser(CategoryType.EXPENSE, owner);
        }
    }

    @Nested
    @DisplayName("createCustomCategory")
    class CreateCustomCategory {

        @Test
        @DisplayName("should create category attached to logged user")
        void shouldCreateCategoryAttachedToLoggedUser() {
            CategoryRequest request = new CategoryRequest("Freelance", CategoryType.INCOME);
            CategoryResponse response = mock(CategoryResponse.class);

            when(categoryRepository.save(any(Category.class))).thenReturn(ownerCategory);
            when(categoryMapper.toResponse(ownerCategory)).thenReturn(response);

            CategoryResponse result = categoryService.createCustomCategory(request, owner);

            assertThat(result).isEqualTo(response);
            verify(categoryRepository).save(any(Category.class));
            verify(categoryMapper).toResponse(ownerCategory);
        }
    }

    @Nested
    @DisplayName("updateCategory")
    class UpdateCategory {

        @Test
        @DisplayName("should update category name when user is the owner")
        void shouldUpdateCategoryWhenUserIsOwner() {
            String newName = "Updated Groceries";
            CategoryResponse response = mock(CategoryResponse.class);

            when(categoryRepository.findById(10L)).thenReturn(Optional.of(ownerCategory));
            when(categoryRepository.save(ownerCategory)).thenReturn(ownerCategory);
            when(categoryMapper.toResponse(ownerCategory)).thenReturn(response);

            CategoryResponse result = categoryService.updateCategory(10L, newName, owner);

            assertThat(result).isEqualTo(response);
            verify(ownerCategory).updateName(newName);
            verify(categoryRepository).save(ownerCategory);
        }

        @Test
        @DisplayName("should throw AccessDeniedException when updating category belonging to another user")
        void shouldThrowAccessDeniedExceptionWhenUpdatingCategoryOfAnotherUser() {
            when(categoryRepository.findById(10L)).thenReturn(Optional.of(ownerCategory));

            assertThatThrownBy(() -> categoryService.updateCategory(10L, "New Name", attacker))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Access denied: This category belongs to another user.");

            verify(ownerCategory, never()).updateName(anyString());
            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when attempting to update default system category")
        void shouldThrowIllegalArgumentExceptionWhenUpdatingDefaultCategory() {
            when(categoryRepository.findById(10L)).thenReturn(Optional.of(defaultCategory));

            assertThatThrownBy(() -> categoryService.updateCategory(10L, "New Name", owner))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("You cannot modify or delete the system's default categories.");

            verify(defaultCategory, never()).updateName(anyString());
            verify(categoryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteCategory")
    class DeleteCategory {

        @Test
        @DisplayName("should archive category when user is the owner")
        void shouldArchiveCategoryWhenUserIsOwner() {
            when(categoryRepository.findById(10L)).thenReturn(Optional.of(ownerCategory));

            categoryService.deleteCategory(10L, owner);

            verify(ownerCategory).archive();
            verify(categoryRepository).save(ownerCategory);
        }

        @Test
        @DisplayName("should throw AccessDeniedException when deleting category belonging to another user")
        void shouldThrowAccessDeniedExceptionWhenDeletingCategoryOfAnotherUser() {
            when(categoryRepository.findById(10L)).thenReturn(Optional.of(ownerCategory));

            assertThatThrownBy(() -> categoryService.deleteCategory(10L, attacker))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Access denied: This category belongs to another user.");

            verify(ownerCategory, never()).archive();
            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when attempting to delete default system category")
        void shouldThrowIllegalArgumentExceptionWhenDeletingDefaultCategory() {
            when(categoryRepository.findById(10L)).thenReturn(Optional.of(defaultCategory));

            assertThatThrownBy(() -> categoryService.deleteCategory(10L, owner))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("You cannot modify or delete the system's default categories.");

            verify(defaultCategory, never()).archive();
            verify(categoryRepository, never()).save(any());
        }
    }
}