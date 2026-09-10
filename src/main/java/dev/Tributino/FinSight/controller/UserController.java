package dev.Tributino.FinSight.controller;

import dev.Tributino.FinSight.domain.User;
import dev.Tributino.FinSight.dto.user.RegisterRequest;
import dev.Tributino.FinSight.dto.user.UpdateUserRequest;
import dev.Tributino.FinSight.dto.user.UserResponse;
import dev.Tributino.FinSight.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/create")
    public ResponseEntity<UserResponse> create(
            @Valid @RequestBody RegisterRequest request
    ) {
        UserResponse response = userService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            @AuthenticationPrincipal User loggedUser
    ) {
        return ResponseEntity.ok(
                userService.findById(loggedUser.getId())
        );
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> update(
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal User loggedUser
    ) {
        return ResponseEntity.ok(
                userService.update(loggedUser.getId(), request)
        );
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal User loggedUser
    ) {
        userService.delete(loggedUser.getId());

        return ResponseEntity.noContent().build();
    }
}