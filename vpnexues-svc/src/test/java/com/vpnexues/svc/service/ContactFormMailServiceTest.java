package com.vpnexues.svc.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;

/** Unit tests for the Contact-page email flow — no SMTP, no Spring context. */
class ContactFormMailServiceTest {

    private CapturingMailSender mailSender;
    private ContactFormMailService mailService;

    @BeforeEach
    void setUp() throws Exception {
        mailSender = new CapturingMailSender();
        mailService = new ContactFormMailService(mailSender);
        setField("fromAddress", "noreply@vpnexues.com");
        setField("receiverAddress", "jegatheeshwaranvpnexues@gmail.com");
    }

    private void setField(String name, String value) throws Exception {
        Field field = ContactFormMailService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(mailService, value);
    }

    @Test
    void sendsProfessionalEmailWithReplyTo() {
        boolean sent = mailService.sendContactEmail(
                "Priya Sharma", "priya@email.com", "+919943994603", "Bulk order query", "Need 50kg rice.");

        assertTrue(sent);
        assertTrue(mailSender.sent.size() == 1);
        SimpleMailMessage mail = mailSender.sent.get(0);
        assertTrue(mail.getTo() != null && mail.getTo().length == 1
                && "jegatheeshwaranvpnexues@gmail.com".equals(mail.getTo()[0]));
        assertTrue("[VPNexues Contact] Bulk order query".equals(mail.getSubject()));
        assertTrue("priya@email.com".equals(mail.getReplyTo()));
        String body = mail.getText();
        assertTrue(body != null && body.contains("Priya Sharma")
                && body.contains("priya@email.com")
                && body.contains("+919943994603")
                && body.contains("Need 50kg rice.")
                && body.contains("Submitted at:"));
    }

    @Test
    void skipsSendWhenReceiverNotConfigured() throws Exception {
        setField("receiverAddress", "");
        boolean sent = mailService.sendContactEmail("A", "a@b.com", null, "Hi", "Hello");
        assertFalse(sent);
        assertTrue(mailSender.sent.isEmpty());
    }

    @Test
    void mailOutageNeverThrows() {
        mailSender.failOnSend = true;
        boolean sent = mailService.sendContactEmail("A", "a@b.com", null, "Hi", "Hello");
        assertFalse(sent);
    }

    /** Minimal JavaMailSender stub — captures SimpleMailMessage sends. */
    static class CapturingMailSender implements JavaMailSender {
        final List<SimpleMailMessage> sent = new ArrayList<>();
        boolean failOnSend = false;

        @Override
        public void send(SimpleMailMessage simpleMessage) {
            if (failOnSend) {
                throw new MailException("SMTP down (test)") {
                };
            }
            sent.add(simpleMessage);
        }

        @Override
        public void send(SimpleMailMessage... simpleMessages) {
            for (SimpleMailMessage m : simpleMessages) {
                send(m);
            }
        }

        @Override
        public MimeMessage createMimeMessage() {
            return new MimeMessage((Session) null);
        }

        @Override
        public MimeMessage createMimeMessage(InputStream contentStream) {
            try {
                return new MimeMessage(Session.getInstance(new Properties()), contentStream);
            } catch (Exception e) {
                throw new MailException("test") {
                };
            }
        }

        @Override
        public void send(MimeMessage mimeMessage) {
        }

        @Override
        public void send(MimeMessage... mimeMessages) {
        }

        @Override
        public void send(MimeMessagePreparator mimeMessagePreparator) {
        }

        @Override
        public void send(MimeMessagePreparator... mimeMessagePreparators) {
        }
    }
}
