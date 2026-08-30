package dev.Tributino.FinSight.repository;

import dev.Tributino.FinSight.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
