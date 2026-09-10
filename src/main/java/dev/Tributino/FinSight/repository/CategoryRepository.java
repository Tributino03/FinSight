package dev.Tributino.FinSight.repository;

import dev.Tributino.FinSight.domain.Category;
import dev.Tributino.FinSight.domain.User;
import dev.Tributino.FinSight.enums.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("SELECT c FROM Category c WHERE c.categoryType = :type AND c.active = true AND (c.user = :user OR c.user IS NULL)")
    List<Category> findAvailableCategoriesByTypeAndUser(@Param("type") CategoryType type, @Param("user") User user);

}