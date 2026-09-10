package dev.Tributino.FinSight.service;

import dev.Tributino.FinSight.domain.Account;
import dev.Tributino.FinSight.domain.Category;
import dev.Tributino.FinSight.domain.Transaction;
import dev.Tributino.FinSight.domain.User;
import dev.Tributino.FinSight.dto.transaction.TransactionRequest;
import dev.Tributino.FinSight.dto.transaction.TransactionResponse;
import dev.Tributino.FinSight.enums.TransactionStatus;
import dev.Tributino.FinSight.enums.TransactionType;
import dev.Tributino.FinSight.mapper.TransactionMapper;
import dev.Tributino.FinSight.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final CategoryService categoryService;
    private final TransactionMapper transactionMapper;

    public TransactionService(TransactionRepository transactionRepository, AccountService accountService, CategoryService categoryService, TransactionMapper transactionMapper) {
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
        this.categoryService = categoryService;
        this.transactionMapper = transactionMapper;
    }

    public TransactionResponse findById(Long id, User loggedUser) {
        Transaction transaction = findEntityByIdAndUser(id, loggedUser);
        return transactionMapper.toResponse(transaction);
    }

    private Transaction findEntityByIdAndUser(Long id, User loggedUser) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found"));

        if (transaction.getAccount() != null && !transaction.getAccount().getUser().getId().equals(loggedUser.getId())) {
            throw new AccessDeniedException("Access denied: This transaction belongs to another user.");
        }

        return transaction;
    }

    public List<TransactionResponse> findByAccountId (Long accountId, User loggedUser) {
        accountService.findEntityByIdAndUser(accountId, loggedUser);

        return transactionRepository
                .findByAccountId(accountId)
                .stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    public List<TransactionResponse> filterTransactionsByAccountAndDate(
            Long accountId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            User loggedUser) {

        accountService.findEntityByIdAndUser(accountId, loggedUser);

        List<Transaction> transactions = transactionRepository.findByAccountId(accountId);

        return transactions.stream()
                .filter(t -> !t.getTransactionDate().isBefore(startDate) && !t.getTransactionDate().isAfter(endDate))
                .map(transactionMapper::toResponse)
                .toList();
    }

    public List<TransactionResponse> findByCategoryId(Long categoryId, User loggedUser) {
        categoryService.findEntityByIdAndUser(categoryId, loggedUser);

        return transactionRepository
                .findByCategoryId(categoryId)
                .stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    public List<TransactionResponse> findByTransactionType(TransactionType transactionType, User loggedUser) {
        return transactionRepository
                .findByTransactionTypeAndAccount_UserId(transactionType, loggedUser.getId())
                .stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    @Transactional
    public TransactionResponse createDebit(
            TransactionRequest request,
            Long accountId,
            Long categoryId,
            User loggedUser) {

        Account account = accountService.findEntityByIdAndUser(accountId, loggedUser);
        Category category = categoryService.findEntityByIdAndUser(categoryId, loggedUser);
        category.ensureActive();

        account.debit(request.amount());

        Transaction newTransaction = new Transaction(
                request.amount(),
                request.description(),
                TransactionType.DEBIT,
                request.paymentMethod(),
                TransactionStatus.COMPLETED,
                request.transactionDate(),
                account,
                category
        );

        Transaction savedTransaction = transactionRepository.save(newTransaction);
        return transactionMapper.toResponse(savedTransaction);
    }

    @Transactional
    public TransactionResponse createCredit(
            TransactionRequest request,
            Long accountId,
            Long categoryId,
            User loggedUser) {

        Account account = accountService.findEntityByIdAndUser(accountId, loggedUser);
        Category category = categoryService.findEntityByIdAndUser(categoryId, loggedUser);
        category.ensureActive();

        account.credit(request.amount());

        Transaction newTransaction = new Transaction(
                request.amount(),
                request.description(),
                TransactionType.CREDIT,
                request.paymentMethod(),
                TransactionStatus.COMPLETED,
                request.transactionDate(),
                account,
                category
        );

        Transaction savedTransaction = transactionRepository.save(newTransaction);
        return transactionMapper.toResponse(savedTransaction);
    }

    @Transactional
    public TransactionResponse cancelTransaction(Long transactionId, User loggedUser) {
        Transaction transaction = findEntityByIdAndUser(transactionId, loggedUser);
        Account account = transaction.getAccount();

        transaction.cancel();

        if (transaction.getTransactionType() == TransactionType.DEBIT) {
            account.credit(transaction.getAmount());
        } else {
            account.debit(transaction.getAmount());
        }

        Transaction cancelTransaction = transactionRepository.save(transaction);

        return transactionMapper.toResponse(cancelTransaction);
    }
}