package dev.Tributino.FinSight.repository;

import dev.Tributino.FinSight.domain.Account;
import dev.Tributino.FinSight.domain.Category;
import dev.Tributino.FinSight.domain.Transaction;
import dev.Tributino.FinSight.domain.User;
import dev.Tributino.FinSight.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId")
    List<Transaction> findByAccountId(Long accountId);

    @Query("SELECT t FROM Transaction t WHERE t.category.id = :categoryId")
    List<Transaction> findByCategoryId(Long categoryId);

    List<Transaction> findByTransactionDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    List<Transaction> findByTransactionType(TransactionType transactionType);
}