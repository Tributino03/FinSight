package dev.Tributino.FinSight.listener;
import dev.Tributino.FinSight.event.PasswordResetRequestedEvent;
import dev.Tributino.FinSight.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PasswordResetEmailListener {

    private final EmailService emailService;
    private static final Logger log = LoggerFactory.getLogger(PasswordResetEmailListener.class);

    public PasswordResetEmailListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        try {
            emailService.sendPasswordResetEmail(event.email(), event.rawToken(), event.ttl());
        } catch (RuntimeException ex) {
            log.error("Unexpected failure while handling password reset email", ex);
        }
    }
}