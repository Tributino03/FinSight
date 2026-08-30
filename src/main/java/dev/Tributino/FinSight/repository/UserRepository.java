package dev.Tributino.FinSight.repository;

import dev.Tributino.FinSight.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long>{
}
