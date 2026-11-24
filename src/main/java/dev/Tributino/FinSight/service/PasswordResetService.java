package dev.Tributino.FinSight.service;

import dev.Tributino.FinSight.domain.PasswordResetToken;
import dev.Tributino.FinSight.domain.User;
import dev.Tributino.FinSight.event.PasswordResetRequestedEvent;
import dev.Tributino.FinSight.exception.InternalProcessingException;
import dev.Tributino.FinSight.exception.InvalidPasswordResetTokenException;
import dev.Tributino.FinSight.repository.PasswordResetTokenRepository;
import dev.Tributino.FinSight.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

@Service
public class PasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTE_LENGTH = 32;
    private static final int MAX_BCRYPT_PASSWORD_BYTES = 72;
    private static final Duration TOKEN_TTL = Duration.ofMinutes(15);

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;

    public PasswordResetService(PasswordResetTokenRepository passwordResetTokenRepository,
                                UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                Clock clock,
                                ApplicationEventPublisher eventPublisher) {
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void requestPasswordReset(String rawEmail) {
        String normalizedEmail = rawEmail.trim().toLowerCase(Locale.ROOT);

        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);
        if (userOpt.isEmpty() || !userOpt.get().isEnabled()) {
            return;
        }

        User user = userOpt.get();
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plus(TOKEN_TTL);

        passwordResetTokenRepository.invalidateActiveByUser(user.getId(), now);

        String rawToken = generateRawToken();
        String tokenHash = hash(rawToken);

        PasswordResetToken resetToken = new PasswordResetToken(user, tokenHash, expiresAt, now);
        passwordResetTokenRepository.save(resetToken);

        eventPublisher.publishEvent(new PasswordResetRequestedEvent(user.getEmail(), rawToken, TOKEN_TTL));
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        if (newPassword.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_PASSWORD_BYTES) {
            throw new IllegalArgumentException("Password exceeds maximum length of 72 bytes");
        }

        String tokenHash = hash(rawToken);
        Instant now = Instant.now(clock);

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(InvalidPasswordResetTokenException::new);

        Long userId = resetToken.getUser().getId();

        int consumed = passwordResetTokenRepository.consume(tokenHash, now);
        if (consumed == 0) {
            throw new InvalidPasswordResetTokenException();
        }

        passwordResetTokenRepository.invalidateActiveByUser(userId, now);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InternalProcessingException(
                        "User not found for password reset token", null));

        user.changePassword(passwordEncoder.encode(newPassword));
    }

    private String generateRawToken() {
        byte[] randomBytes = new byte[TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashedBytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new InternalProcessingException("SHA-256 algorithm not available", ex);
        }
    }
}