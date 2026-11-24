package dev.Tributino.FinSight.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String resetPasswordBaseUrl;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${app.mail.from}") String fromAddress,
            @Value("${app.frontend.reset-password-url}") String resetPasswordBaseUrl
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.resetPasswordBaseUrl = resetPasswordBaseUrl;
    }

    public void sendPasswordResetEmail(String toEmail, String rawToken, Duration ttl) {
        String resetLink = UriComponentsBuilder.fromUriString(resetPasswordBaseUrl)
                .fragment("token=" + rawToken)
                .toUriString();

        long expiresInMinutes = ttl.toMinutes();
        String subject = "FinSight — Redefinição de senha";
        String plainText = buildPlainTextBody(resetLink, expiresInMinutes);
        String htmlBody = buildHtmlBody(resetLink, expiresInMinutes);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(plainText, htmlBody);

            mailSender.send(message);
            log.info("Password reset email dispatched to {}", maskEmail(toEmail));
        } catch (MessagingException | MailException ex) {
            log.error("Failed to send password reset email to {}", maskEmail(toEmail), ex);
            throw new IllegalStateException("Could not send password reset email", ex);
        }
    }

    private String buildPlainTextBody(String resetLink, long expiresInMinutes) {
        return """
                Recebemos uma solicitação para redefinir a senha da sua conta FinSight.
                Acesse o link abaixo para criar uma nova senha (válido por %d minutos):

                %s

                Se você não solicitou essa alteração, ignore este e-mail.
                """.formatted(expiresInMinutes, resetLink);
    }

    private String buildHtmlBody(String resetLink, long expiresInMinutes) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 520px; margin: 0 auto; color: #1f2937;">
                    <h2>Redefinição de senha</h2>
                    <p>Recebemos uma solicitação para redefinir a senha da sua conta <strong>FinSight</strong>.</p>
                    <p>Clique no botão abaixo para cadastrar uma nova senha. Este link expira em <strong>%d minutos</strong> e só pode ser utilizado uma única vez:</p>
                    <p style="margin: 24px 0;">
                        <a href="%s"
                           style="background-color: #111827; color: #ffffff; padding: 12px 20px; text-decoration: none; border-radius: 6px; display: inline-block;">
                            Redefinir minha senha
                        </a>
                    </p>
                    <p style="font-size: 12px; color: #6b7280;">
                        Se você não solicitou esta alteração, nenhuma ação é necessária.
                    </p>
                </div>
                """.formatted(expiresInMinutes, resetLink);
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}