package dev.Tributino.FinSight.listener;

import dev.Tributino.FinSight.event.PasswordResetRequestedEvent;
import dev.Tributino.FinSight.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PasswordResetEmailListenerTest {

    @Mock
    private EmailService emailService;

    private PasswordResetEmailListener listener;

    @BeforeEach
    void setUp() {
        listener = new PasswordResetEmailListener(emailService);
    }

    @Test
    @DisplayName("Should delegate event data to EmailService")
    void delegatesValidEventToEmailService() {
        PasswordResetRequestedEvent event = new PasswordResetRequestedEvent(
                "gustavo@finsight.dev",
                "raw-secret-token",
                Duration.ofMinutes(15)
        );

        listener.onPasswordResetRequested(event);

        verify(emailService).sendPasswordResetEmail(
                "gustavo@finsight.dev",
                "raw-secret-token",
                Duration.ofMinutes(15)
        );
    }

    @Test
    @DisplayName("Should swallow RuntimeException when EmailService fails")
    void doesNotPropagateExceptionWhenEmailServiceFails() {
        PasswordResetRequestedEvent event = new PasswordResetRequestedEvent(
                "gustavo@finsight.dev",
                "raw-secret-token",
                Duration.ofMinutes(15)
        );

        doThrow(new IllegalStateException("SMTP connection timeout"))
                .when(emailService)
                .sendPasswordResetEmail(event.email(), event.rawToken(), event.ttl());

        assertThatCode(() -> listener.onPasswordResetRequested(event))
                .doesNotThrowAnyException();

        verify(emailService).sendPasswordResetEmail(
                "gustavo@finsight.dev",
                "raw-secret-token",
                Duration.ofMinutes(15)
        );
    }
}