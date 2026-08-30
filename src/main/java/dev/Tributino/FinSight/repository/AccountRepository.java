package dev.Tributino.FinSight.repository;

import dev.Tributino.FinSight.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
}
