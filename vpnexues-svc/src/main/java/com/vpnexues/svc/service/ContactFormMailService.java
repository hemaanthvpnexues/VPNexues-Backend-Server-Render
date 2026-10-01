package com.vpnexues.svc.service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends Contact-page form submissions to the configured company inbox.
 *
 * <p>Recipient comes only from backend config ({@code CONTACT_RECEIVER_EMAIL}) — never from
 * frontend input. Plain-text mail (like {@link CareerApplicationService}) so user content
 * needs no HTML escaping. The sender's address is set as Reply-To so support can reply
 * directly to the customer.
 */
@Service
@RequiredArgsConstructor
public class ContactFormMailService {

    private static final Logger log = LoggerFactory.getLogger(ContactFormMailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@vpnexues.com}")
    private String fromAddress;

    @Value("${contact.mail.receiver:}")
    private String receiverAddress;

    /** Logs whether contact email delivery is armed — never logs the address itself. */
    @PostConstruct
    void logMailConfig() {
        boolean mailHostSet = false;
        try {
            String host = mailSender instanceof org.springframework.mail.javamail.JavaMailSenderImpl impl
                    ? impl.getHost()
                    : null;
            mailHostSet = host != null && !host.isBlank();
        } catch (Exception ex) {
            log.debug("Could not inspect mail sender host", ex);
        }
        log.info("Contact mail configured: receiver={}, smtpHost={}",
                (receiverAddress != null && !receiverAddress.isBlank()), mailHostSet);
    }

    /**
     * @return true if an email was accepted for delivery, false when skipped
     *         (no receiver configured) or the mail provider rejected it.
     *         Never throws — callers persist the message regardless.
     */
    public boolean sendChatTranscriptEmail(
            String visitorName, String visitorEmail, String subject, java.util.List<String> transcriptLines) {
        if (receiverAddress == null || receiverAddress.isBlank()) {
            log.warn("CONTACT_RECEIVER_EMAIL is not configured — skipping chat transcript email for '{}'", subject);
            return false;
        }
        try {
            StringBuilder body = new StringBuilder();
            body.append("New live-chat escalation via vpnexues.com/support\n\n");
            body.append("Visitor: ").append(visitorName).append("\n");
            body.append("Email:   ").append(visitorEmail).append("\n\n");
            body.append("Transcript:\n");
            for (String line : transcriptLines) {
                body.append("  ").append(line).append("\n");
            }
            body.append("\nSubmitted at: ").append(ZonedDateTime.now(ZoneId.of("Asia/Kolkata"))
                    .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a z")));
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(fromAddress);
            mail.setTo(receiverAddress.trim());
            mail.setReplyTo(visitorEmail);
            mail.setSubject("[VPNexues Chat] " + subject);
            mail.setText(body.toString());
            mailSender.send(mail);
            return true;
        } catch (Exception ex) {
            log.warn("Failed to send chat transcript email for '{}': {}", subject, ex.getMessage());
            return false;
        }
    }

    /**
     * @return true if an email was accepted for delivery, false when skipped
     *         (no receiver configured) or the mail provider rejected it.
     *         Never throws — callers persist the message regardless.
     */
    public boolean sendContactEmail(String name, String email, String phone, String subject, String message) {
        if (receiverAddress == null || receiverAddress.isBlank()) {
            log.warn("CONTACT_RECEIVER_EMAIL is not configured — skipping contact email for subject '{}'", subject);
            return false;
        }
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(fromAddress);
            mail.setTo(receiverAddress.trim());
            mail.setReplyTo(email);
            mail.setSubject("[VPNexues Contact] " + subject);
            mail.setText(buildBody(name, email, phone, subject, message));
            mailSender.send(mail);
            return true;
        } catch (Exception ex) {
            log.warn("Failed to send contact email for subject '{}': {}", subject, ex.getMessage());
            return false;
        }
    }

    private String buildBody(String name, String email, String phone, String subject, String message) {
        String submittedAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"))
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a z"));
        StringBuilder body = new StringBuilder();
        body.append("New Contact Form Submission\n\n");
        body.append("Name:  ").append(name).append("\n");
        body.append("Email: ").append(email).append("\n");
        body.append("Phone: ").append((phone == null || phone.isBlank()) ? "N/A" : phone).append("\n\n");
        body.append("Subject:\n").append(subject).append("\n\n");
        body.append("Message:\n").append(message).append("\n\n");
        body.append("Submitted at: ").append(submittedAt);
        return body.toString();
    }
}
