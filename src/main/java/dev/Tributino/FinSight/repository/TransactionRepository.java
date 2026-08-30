package dev.Tributino.FinSight.repository;

import dev.Tributino.FinSight.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
