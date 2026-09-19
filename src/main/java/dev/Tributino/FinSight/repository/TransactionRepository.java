package dev.Tributino.FinSight.repository;

import dev.Tributino.FinSight.domain.Account;
import dev.Tributino.FinSight.domain.Category;
import dev.Tributino.FinSight.domain.Transaction;
import dev.Tributino.FinSight.domain.User;
import dev.Tributino.FinSight.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT t FROM Transaction t WHERE t.account.user = :user")
    List<Transaction> findAllByUser(@Param("user") User user);

    List<Transaction> findByAccountId(Long accountId);

    List<Transaction> findByCategoryId(Long categoryId);

    List<Transaction> findByTransactionTypeAndAccount_UserId(TransactionType transactionType, Long userId);

    List<Transaction> findByAccountIdAndTransactionDateBetween(
            Long accountId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

}