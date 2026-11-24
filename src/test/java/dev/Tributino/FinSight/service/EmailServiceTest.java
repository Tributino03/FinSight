package dev.Tributino.FinSight.service;

import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class EmailServiceTest {

    private static final String FROM_ADDRESS = "no-reply@finsight.dev";
    private static final String RESET_BASE_URL = "http://localhost:3000/reset-password";
    private static final String RAW_TOKEN = "secretTokenValue123456789";

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender, FROM_ADDRESS, RESET_BASE_URL);
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
    }

    private record ExtractedContent(String plainText, String html) {
    }

    private ExtractedContent extractContent(MimeMessage message) throws Exception {
        message.saveChanges();
        StringBuilder plain = new StringBuilder();
        StringBuilder html = new StringBuilder();
        collectParts(message, plain, html);
        return new ExtractedContent(plain.toString(), html.toString());
    }

    private void collectParts(Part part, StringBuilder plain, StringBuilder html) throws Exception {
        Object content = part.getContent();
        if (content instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                collectParts(multipart.getBodyPart(i), plain, html);
            }
        } else if (part.isMimeType("text/plain")) {
            plain.append(content);
        } else if (part.isMimeType("text/html")) {
            html.append(content);
        }
    }

    @Test
    @DisplayName("Should build MimeMessage with expected headers, fragment link, TTL and masked log")
    void sendsEmailWithExpectedHeadersFragmentLinkAndMaskedLog(CapturedOutput output) throws Exception {
        emailService.sendPasswordResetEmail("joao@x.com", RAW_TOKEN, Duration.ofMinutes(15));

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        MimeMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getFrom())
                .extracting(address -> ((InternetAddress) address).getAddress())
                .containsExactly(FROM_ADDRESS);
        assertThat(sentMessage.getRecipients(Message.RecipientType.TO))
                .extracting(address -> ((InternetAddress) address).getAddress())
                .containsExactly("joao@x.com");
        assertThat(sentMessage.getSubject()).isEqualTo("FinSight — Redefinição de senha");

        ExtractedContent content = extractContent(sentMessage);
        String expectedLink = RESET_BASE_URL + "#token=" + RAW_TOKEN;

        assertThat(content.plainText())
                .contains(expectedLink)
                .doesNotContain("?token=")
                .contains("15 minutos");

        assertThat(content.html())
                .contains("href=\"" + expectedLink + "\"")
                .doesNotContain("?token=")
                .contains("<strong>15 minutos</strong>");

        assertThat(output.getOut())
                .contains("j***@x.com")
                .doesNotContain("joao@x.com")
                .doesNotContain(RAW_TOKEN);
    }

    @Test
    @DisplayName("Should render dynamic TTL minutes rather than a hardcoded value")
    void rendersDynamicTtlMinutes() throws Exception {
        emailService.sendPasswordResetEmail("joao@x.com", RAW_TOKEN, Duration.ofMinutes(30));

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        ExtractedContent content = extractContent(messageCaptor.getValue());
        assertThat(content.plainText()).contains("30 minutos").doesNotContain("15 minutos");
        assertThat(content.html()).contains("<strong>30 minutos</strong>").doesNotContain("15 minutos");
    }

    @Test
    @DisplayName("Should throw IllegalStateException and log masked email without token when send fails")
    void throwsIllegalStateExceptionWithoutLeakingTokenOnSendFailure(CapturedOutput output) {
        doThrow(new MailSendException("SMTP server unreachable"))
                .when(mailSender)
                .send(any(MimeMessage.class));

        assertThatThrownBy(() ->
                emailService.sendPasswordResetEmail("joao@x.com", RAW_TOKEN, Duration.ofMinutes(15))
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Could not send password reset email");

        assertThat(output.getOut())
                .contains("Failed to send password reset email to j***@x.com")
                .doesNotContain("joao@x.com")
                .doesNotContain(RAW_TOKEN);
    }

    @Test
    @DisplayName("Should mask single-character local part email as *** in logs")
    void masksSingleCharacterLocalPartEmail(CapturedOutput output) {
        emailService.sendPasswordResetEmail("a@x.com", RAW_TOKEN, Duration.ofMinutes(15));

        assertThat(output.getOut())
                .contains("Password reset email dispatched to ***")
                .doesNotContain("a@x.com")
                .doesNotContain(RAW_TOKEN);
    }
}