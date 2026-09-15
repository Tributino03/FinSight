package dev.Tributino.FinSight.domain;

import dev.Tributino.FinSight.enums.CategoryType;
import dev.Tributino.FinSight.enums.PaymentMethod;
import dev.Tributino.FinSight.enums.TransactionStatus;
import dev.Tributino.FinSight.enums.TransactionType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus transactionStatus;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    protected Transaction() {}

    public Transaction(
            BigDecimal amount,
            String description,
            TransactionType transactionType,
            PaymentMethod paymentMethod,
            TransactionStatus transactionStatus,
            LocalDateTime transactionDate,
            Account account,
            Category category) {

        validateAmount(amount);
        validateDescription(description);
        validateTransactionType(transactionType);
        validatePaymentMethod(paymentMethod);
        validateTransactionStatus(transactionStatus);
        validateTransactionDate(transactionDate);
        validateAccount(account);
        validateCategory(category);
        validateCategoryCompatibility(transactionType, category);
        validateOwnership(account, category);
        category.ensureActive();

        this.amount = amount;
        this.description = description;
        this.transactionType = transactionType;
        this.paymentMethod = paymentMethod;
        this.transactionStatus = transactionStatus;
        this.transactionDate = transactionDate;
        this.account = account;
        this.category = category;
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("The transaction amount must be greater than zero.");
        }
    }

    private void validateDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("The transaction description cannot be empty.");
        }
    }

    private void validateTransactionType(TransactionType transactionType) {
        if (transactionType == null) {
            throw new IllegalArgumentException("The transaction type is required.");
        }
    }

    private void validatePaymentMethod(PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            throw new IllegalArgumentException("The payment method is required.");
        }
    }

    private void validateTransactionStatus(TransactionStatus transactionStatus) {
        if (transactionStatus == null) {
            throw new IllegalArgumentException("The transaction status is required.");
        }
    }

    private void validateTransactionDate(LocalDateTime transactionDate) {
        if (transactionDate == null) {
            throw new IllegalArgumentException("The transaction date is required.");
        }
    }

    private void validateAccount(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("The transaction account is required.");
        }
    }

    private void validateCategory(Category category) {
        if (category == null) {
            throw new IllegalArgumentException("The transaction category is required.");
        }
    }

    private void validateCategoryCompatibility(TransactionType transactionType, Category category) {
        if (transactionType == TransactionType.CREDIT && category.getCategoryType() != CategoryType.INCOME) {
            throw new IllegalArgumentException("Credit transactions require an INCOME category.");
        }
        if (transactionType == TransactionType.DEBIT && category.getCategoryType() != CategoryType.EXPENSE) {
            throw new IllegalArgumentException("Debit transactions require an EXPENSE category.");
        }
    }

    private void validateOwnership(Account account, Category category) {
        if (category.getUser() != null && !Objects.equals(account.getUser(), category.getUser())) {
            throw new IllegalArgumentException("The custom category does not belong to the account owner.");
        }
    }

    public void cancel() {
        if (this.transactionStatus == TransactionStatus.CANCELLED) {
            throw new IllegalStateException("This transaction is already cancelled.");
        }

        if (this.transactionStatus != TransactionStatus.COMPLETED) {
            throw new IllegalStateException("This transaction cannot be cancelled.");
        }

        this.transactionStatus = TransactionStatus.CANCELLED;
    }

    public void setDescription(String description) {
        validateDescription(description);
        this.description = description;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        validateTransactionDate(transactionDate);
        this.transactionDate = transactionDate;
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public TransactionStatus getTransactionStatus() {
        return transactionStatus;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public Account getAccount() {
        return account;
    }

    public Category getCategory() {
        return category;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}