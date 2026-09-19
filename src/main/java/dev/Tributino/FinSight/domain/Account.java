package dev.Tributino.FinSight.domain;

import dev.Tributino.FinSight.enums.AccountType;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
@NoArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AccountType accountType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    public Account(
            String name,
            AccountType accountType,
            BigDecimal balance,
            User user
    ) {
        validateName(name);
        validateAccountType(accountType);
        validateBalance(balance);
        validateUser(user);

        this.name = name;
        this.accountType = accountType;
        this.balance = balance;
        this.user = user;
    }

    public void credit(BigDecimal amount) {
        validateAmount(amount);
        this.balance = this.balance.add(amount);
    }

    public void debit(BigDecimal amount) {
        validateAmount(amount);
        if (this.balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance.");
        }
        this.balance = this.balance.subtract(amount);
    }

    private void validateBalance(BigDecimal balance) {
        if (balance == null || balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("The account balance cannot be less than zero.");
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("The amount must be greater than zero.");
        }
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("The account name cannot be empty.");
        }
    }

    private void validateAccountType(AccountType accountType) {
        if (accountType == null) {
            throw new IllegalArgumentException("The account type is required.");
        }
    }

    private void validateUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("The account user is required.");
        }
    }

    public void updateName(String name) {
        validateName(name);
        this.name = name;
    }

    public Long getId() { return id; }

    public String getName() { return name; }

    public AccountType getAccountType() { return accountType; }

    public BigDecimal getBalance() { return balance; }

    public User getUser() { return user; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}