package dev.Tributino.FinSight.controller;

import dev.Tributino.FinSight.dto.transaction.TransactionRequest;
import dev.Tributino.FinSight.dto.transaction.TransactionResponse;
import dev.Tributino.FinSight.enums.TransactionType;
import dev.Tributino.FinSight.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> findAll() {
        return ResponseEntity.ok(transactionService.findAll());
    }

    @GetMapping("{id}")
    public ResponseEntity<TransactionResponse> findById(@PathVariable Long id){
        return ResponseEntity.ok(transactionService.findById(id));
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<TransactionResponse>> findByAccountId(
            @PathVariable Long accountId) {

        return ResponseEntity.ok(
                transactionService.findByAccountId(accountId)
        );
    }

    @GetMapping("/account/{accountId}/filter")
    public ResponseEntity<List<TransactionResponse>> filterTransactionsByAccountAndDate(
            @PathVariable Long accountId,
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate) {

        return ResponseEntity.ok(
                transactionService.filterTransactionsByAccountAndDate(
                        accountId,
                        startDate,
                        endDate
                )
        );
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<TransactionResponse>> findByCategoryId(
            @PathVariable Long categoryId) {

        return ResponseEntity.ok(
                transactionService.findByCategoryId(categoryId)
        );
    }

    @GetMapping("/type")
    public ResponseEntity<List<TransactionResponse>> findByTransactionType(@RequestParam TransactionType transactionType) {
        return ResponseEntity.ok(transactionService.findByTransactionType(transactionType));
    }

    @PostMapping("/account/{accountId}/category/{categoryId}/debit")
    public ResponseEntity<TransactionResponse> createDebit(
            @Valid @RequestBody TransactionRequest request,
            @PathVariable Long accountId,
            @PathVariable Long categoryId) {

        TransactionResponse createdTransaction = transactionService.createDebit(request, accountId, categoryId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTransaction);
    }

    @PostMapping("/account/{accountId}/category/{categoryId}/credit")
    public ResponseEntity<TransactionResponse> createCredit(
            @Valid @RequestBody TransactionRequest request,
            @PathVariable Long accountId,
            @PathVariable Long categoryId) {

        TransactionResponse createdTransaction = transactionService.createCredit(request, accountId, categoryId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTransaction);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<TransactionResponse> cancelTransaction(@PathVariable Long id) {

        TransactionResponse cancelledTransaction = transactionService.cancelTransaction(id);

        return ResponseEntity.ok(cancelledTransaction);
    }
}

