package dev.Tributino.FinSight.controller;

import dev.Tributino.FinSight.dto.passwordresettoken.ForgotPasswordRequest;
import dev.Tributino.FinSight.dto.passwordresettoken.MessageResponse;
import dev.Tributino.FinSight.dto.passwordresettoken.ResetPasswordRequest;
import dev.Tributino.FinSight.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/password")
public class PasswordResetTokenController {

    private static final String GENERIC_FORGOT_MESSAGE =
            "If the email is registered, you will receive password reset instructions shortly.";

    private final PasswordResetService passwordResetService;

    public PasswordResetTokenController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/forgot")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestPasswordReset(request.email());
        return ResponseEntity.ok(new MessageResponse(GENERIC_FORGOT_MESSAGE));
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }
}