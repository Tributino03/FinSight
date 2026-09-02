package dev.Tributino.FinSight.service;

import dev.Tributino.FinSight.domain.Account;
import dev.Tributino.FinSight.domain.Category;
import dev.Tributino.FinSight.domain.Transaction;
import dev.Tributino.FinSight.enums.TransactionStatus;
import dev.Tributino.FinSight.enums.TransactionType;
import dev.Tributino.FinSight.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final CategoryService categoryService;

    public TransactionService(TransactionRepository transactionRepository, AccountService accountService, CategoryService categoryService) {
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
        this.categoryService = categoryService;
    }

    public List<Transaction> findAll() {
        return transactionRepository.findAll();
    }

    public Transaction findById (Long id){
        return transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found"));
    }

    List<Transaction> findByAccountId (Long accountId) {
        return transactionRepository.findByAccountId(accountId);
    }

    public List<Transaction> filterTransactionsManually(List<Transaction> transactions, LocalDateTime startDate, LocalDateTime endDate) {
        return transactions.stream()
                .filter(t -> !t.getTransactionDate().isBefore(startDate) && !t.getTransactionDate().isAfter(endDate))
                .toList();
    }

    List<Transaction> findByCategoryId(Long categoryId) {
        return transactionRepository.findByCategoryId(categoryId);
    }

    List<Transaction> findByTransactionType(TransactionType transactionType) {
        return transactionRepository.findByTransactionType(transactionType);
    }

    @Transactional
    public Transaction createDebit(Transaction transactionData, Long accountId, Long categoryId) {
        Account account = accountService.findById(accountId);
        Category category = categoryService.findById(categoryId);
        category.ensureActive();

        account.debit(transactionData.getAmount());

        Transaction newTransaction = new Transaction(
                transactionData.getAmount(),
                transactionData.getDescription(),
                TransactionType.DEBIT,
                transactionData.getPaymentMethod(),
                TransactionStatus.COMPLETED,
                transactionData.getTransactionDate(),
                account,
                category
        );

        return transactionRepository.save(newTransaction);
    }

    @Transactional
    public Transaction createCredit(Transaction transactionData, Long accountId, Long categoryId) {
        Account account = accountService.findById(accountId);
        Category category = categoryService.findById(categoryId);
        category.ensureActive();

        account.credit(transactionData.getAmount());

        Transaction newTransaction = new Transaction(
                transactionData.getAmount(),
                transactionData.getDescription(),
                TransactionType.CREDIT,
                transactionData.getPaymentMethod(),
                TransactionStatus.COMPLETED,
                transactionData.getTransactionDate(),
                account,
                category
        );

        return transactionRepository.save(newTransaction);
    }

    @Transactional
    public Transaction cancelTransaction(Long transactionId) {
        Transaction transaction = findById(transactionId);
        Account account = transaction.getAccount();

        transaction.cancel();

        if (transaction.getTransactionType() == TransactionType.DEBIT) {
            account.credit(transaction.getAmount());
        } else{
            account.debit(transaction.getAmount());
        }

        return transactionRepository.save(transaction);
    }
}