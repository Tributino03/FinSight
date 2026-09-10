package dev.Tributino.FinSight.controller;

import dev.Tributino.FinSight.domain.User;
import dev.Tributino.FinSight.dto.transaction.TransactionRequest;
import dev.Tributino.FinSight.dto.transaction.TransactionResponse;
import dev.Tributino.FinSight.enums.TransactionType;
import dev.Tributino.FinSight.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> findById(
            @PathVariable Long id,
            @AuthenticationPrincipal User loggedUser){
        return ResponseEntity.ok(transactionService.findById(id, loggedUser));
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<TransactionResponse>> findByAccountId(
            @PathVariable Long accountId,
            @AuthenticationPrincipal User loggedUser) {

        return ResponseEntity.ok(
                transactionService.findByAccountId(accountId, loggedUser)
        );
    }

    @GetMapping("/account/{accountId}/filter")
    public ResponseEntity<List<TransactionResponse>> filterTransactionsByAccountAndDate(
            @PathVariable Long accountId,
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate,
            @AuthenticationPrincipal User loggedUser) {

        return ResponseEntity.ok(
                transactionService.filterTransactionsByAccountAndDate(
                        accountId,
                        startDate,
                        endDate,
                        loggedUser
                )
        );
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<TransactionResponse>> findByCategoryId(
            @PathVariable Long categoryId,
            @AuthenticationPrincipal User loggedUser) {

        return ResponseEntity.ok(
                transactionService.findByCategoryId(categoryId, loggedUser)
        );
    }

    @GetMapping("/type")
    public ResponseEntity<List<TransactionResponse>> findByTransactionType(
            @RequestParam TransactionType transactionType,
            @AuthenticationPrincipal User loggedUser) {
        return ResponseEntity.ok(transactionService.findByTransactionType(transactionType, loggedUser));
    }

    @PostMapping("/account/{accountId}/category/{categoryId}/debit")
    public ResponseEntity<TransactionResponse> createDebit(
            @Valid @RequestBody TransactionRequest request,
            @PathVariable Long accountId,
            @PathVariable Long categoryId,
            @AuthenticationPrincipal User loggedUser) {

        TransactionResponse createdTransaction = transactionService.createDebit(request, accountId, categoryId, loggedUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTransaction);
    }

    @PostMapping("/account/{accountId}/category/{categoryId}/credit")
    public ResponseEntity<TransactionResponse> createCredit(
            @Valid @RequestBody TransactionRequest request,
            @PathVariable Long accountId,
            @PathVariable Long categoryId,
            @AuthenticationPrincipal User loggedUser) {

        TransactionResponse createdTransaction = transactionService.createCredit(request, accountId, categoryId, loggedUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTransaction);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<TransactionResponse> cancelTransaction(
            @PathVariable Long id,
            @AuthenticationPrincipal User loggedUser) {

        TransactionResponse cancelledTransaction = transactionService.cancelTransaction(id, loggedUser);

        return ResponseEntity.ok(cancelledTransaction);
    }
}