package dev.Tributino.FinSight.controller;

import dev.Tributino.FinSight.domain.User;
import dev.Tributino.FinSight.dto.account.AccountRequest;
import dev.Tributino.FinSight.dto.account.AccountResponse;
import dev.Tributino.FinSight.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.AccessDeniedException;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> findById(
            @PathVariable Long id,
            @AuthenticationPrincipal User loggedUser) throws AccessDeniedException {

        AccountResponse response = accountService.findById(id, loggedUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AccountResponse>> findByUserId(
            @PathVariable Long userId,
            @AuthenticationPrincipal User loggedUser) {

        return ResponseEntity.ok(accountService.findByUserId(userId, loggedUser));
    }
    @PostMapping
    public ResponseEntity<AccountResponse> create(
            @Valid @RequestBody AccountRequest request,
            @AuthenticationPrincipal User loggedUser) {

        AccountResponse createdAccount = accountService.create(request, loggedUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAccount);
    }

    @PutMapping("/{id}/name")
    public ResponseEntity<AccountResponse> updateName(
            @PathVariable Long id,
            @RequestParam String newName,
            @AuthenticationPrincipal User loggedUser) {
        AccountResponse updatedAccount = accountService.updateName(id, newName, loggedUser);
        return ResponseEntity.ok(updatedAccount);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User loggedUser) {
        accountService.delete(id, loggedUser);
        return ResponseEntity.noContent().build();
    }
}