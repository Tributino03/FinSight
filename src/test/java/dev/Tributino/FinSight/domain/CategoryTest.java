package dev.Tributino.FinSight.domain;

import dev.Tributino.FinSight.enums.CategoryType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CategoryTest {

    private Category createCustomCategory(String name, CategoryType type) {
        return new Category(name, type, new User());
    }

    private Category createSystemCategory(String name, CategoryType type) {
        return new Category(name, type, null);
    }

    @Nested
    @DisplayName("Constructor Validation Tests")
    class ConstructorValidationTests {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Should throw exception when category name is null or blank in constructor")
        void shouldThrowExceptionWhenNameIsInvalid(String invalidName) {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new Category(invalidName, CategoryType.INCOME, new User())
            );

            assertEquals("The category name cannot be empty.", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when category type is null")
        void shouldThrowExceptionWhenCategoryTypeIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new Category("Books", null, new User())
            );

            assertEquals("The category type is required.", exception.getMessage());
        }

        @Test
        @DisplayName("Should create custom category when user is provided")
        void shouldCreateCustomCategoryWhenUserIsProvided() {
            User owner = new User();
            Category category = new Category("Salary", CategoryType.INCOME, owner);

            assertEquals("Salary", category.getName());
            assertEquals(CategoryType.INCOME, category.getCategoryType());
            assertEquals(owner, category.getUser());
            assertTrue(category.isActive());
        }

        @Test
        @DisplayName("Should create system default category when user is null")
        void shouldCreateSystemCategoryWhenUserIsNull() {
            Category systemCategory = createSystemCategory("Groceries", CategoryType.EXPENSE);

            assertEquals("Groceries", systemCategory.getName());
            assertEquals(CategoryType.EXPENSE, systemCategory.getCategoryType());
            assertNull(systemCategory.getUser());
            assertTrue(systemCategory.isActive());
        }
    }

    @Nested
    @DisplayName("Category Type Tests (INCOME vs EXPENSE)")
    class CategoryTypeTests {

        @Test
        @DisplayName("Should correctly identify an INCOME category")
        void shouldCreateIncomeCategory() {
            Category incomeCategory = createCustomCategory("Freelance", CategoryType.INCOME);

            assertEquals(CategoryType.INCOME, incomeCategory.getCategoryType());
        }

        @Test
        @DisplayName("Should correctly identify an EXPENSE category")
        void shouldCreateExpenseCategory() {
            Category expenseCategory = createCustomCategory("Rent", CategoryType.EXPENSE);

            assertEquals(CategoryType.EXPENSE, expenseCategory.getCategoryType());
        }
    }

    @Nested
    @DisplayName("Activation and Usability Tests (ACTIVE vs INACTIVE)")
    class UsabilityTests {

        @Test
        @DisplayName("Should be active by default upon creation")
        void shouldBeActiveByDefault() {
            Category category = createCustomCategory("Health", CategoryType.EXPENSE);

            assertTrue(category.isActive());
            assertDoesNotThrow(category::ensureActive);
        }

        @Test
        @DisplayName("Should archive category and set active status to false")
        void shouldArchiveCategory() {
            Category category = createCustomCategory("Subscriptions", CategoryType.EXPENSE);

            category.archive();

            assertFalse(category.isActive());
        }

        @Test
        @DisplayName("Should throw exception when ensureActive is called on an inactive category")
        void shouldThrowExceptionWhenCategoryIsInactive() {
            Category category = createCustomCategory("Old Gym", CategoryType.EXPENSE);
            category.archive();

            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    category::ensureActive
            );

            assertEquals("Cannot use an inactive category.", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Update Name Tests")
    class UpdateNameTests {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Should throw exception when updating category name with null or blank value")
        void shouldThrowExceptionWhenUpdatingNameWithInvalidValue(String invalidName) {
            Category category = createCustomCategory("Investments", CategoryType.INCOME);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> category.updateName(invalidName)
            );

            assertEquals("The category name cannot be empty.", exception.getMessage());
        }

        @Test
        @DisplayName("Should update category name successfully")
        void shouldUpdateCategoryNameSuccessfully() {
            Category category = createCustomCategory("Books", CategoryType.EXPENSE);

            category.updateName("Education & Books");

            assertEquals("Education & Books", category.getName());
        }
    }
}