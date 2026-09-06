package dev.Tributino.FinSight.controller;

import dev.Tributino.FinSight.dto.account.AccountRequest;
import dev.Tributino.FinSight.dto.account.AccountResponse;
import dev.Tributino.FinSight.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> findAll() {
        return ResponseEntity.ok(accountService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.findById(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AccountResponse>> findByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(accountService.findByUserId(userId));
    }

    @PostMapping("/user/{userId}")
    public ResponseEntity<AccountResponse> create(
            @PathVariable Long userId,
            @Valid @RequestBody AccountRequest request) {

        AccountResponse createdAccount = accountService.create(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAccount);
    }

    @PutMapping("/{id}/name")
    public ResponseEntity<AccountResponse> updateName(
            @PathVariable Long id,
            @RequestParam String newName) {

        AccountResponse updatedAccount = accountService.updateName(id, newName);
        return ResponseEntity.ok(updatedAccount);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        accountService.delete(id);
        return ResponseEntity.noContent().build();
    }
}