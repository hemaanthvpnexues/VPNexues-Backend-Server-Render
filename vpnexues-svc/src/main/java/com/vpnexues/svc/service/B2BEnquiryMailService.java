package com.vpnexues.svc.service;

import com.vpnexues.svc.entity.B2BEnquiry;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Emails for B2B enquiries: a confirmation to the customer who submitted the form and a
 * notification copy to the company inbox so the team sees new enquiries instantly.
 *
 * <p>Follows {@link ContactFormMailService}: the team recipient comes only from backend
 * config ({@code contact.mail.receiver}) — never from frontend input; plain-text mail so
 * user content needs no HTML escaping; never throws — the enquiry is already saved and
 * must not fail because of mail delivery.
 */
@Service
@RequiredArgsConstructor
public class B2BEnquiryMailService {

    private static final Logger log = LoggerFactory.getLogger(B2BEnquiryMailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@vpnexues.com}")
    private String fromAddress;

    @Value("${contact.mail.receiver:}")
    private String teamInbox;

    /**
     * "We got your request" mail to the customer's email — sent after the enquiry row
     * is saved. Skipped silently when the address is blank; failures are logged, never thrown.
     */
    public void sendCustomerConfirmation(B2BEnquiry enquiry) {
        String to = enquiry.getEmail();
        if (to == null || to.isBlank()) {
            return;
        }
        try {
            StringBuilder body = new StringBuilder();
            body.append("Dear ").append(enquiry.getContactName()).append(",\n\n");
            body.append("Thank you for your B2B enquiry");
            if (enquiry.getCompanyName() != null && !enquiry.getCompanyName().isBlank()) {
                body.append(" for ").append(enquiry.getCompanyName());
            }
            body.append(".\n\n");
            body.append("Your request has been received. Our team will review it and contact you soon.\n\n");
            if (enquiry.getProductInterest() != null && !enquiry.getProductInterest().isBlank()) {
                body.append("Products of interest: ").append(enquiry.getProductInterest()).append("\n");
            }
            if (enquiry.getQuantity() != null && !enquiry.getQuantity().isBlank()) {
                body.append("Estimated quantity: ").append(enquiry.getQuantity()).append("\n");
            }
            body.append("\nSubmitted at: ").append(now()).append("\n\n");
            body.append("Regards,\nVPNexues Team");

            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(fromAddress);
            mail.setTo(to.trim());
            mail.setReplyTo(fromAddress);
            mail.setSubject("We've received your request — VPNexues");
            mail.setText(body.toString());
            mailSender.send(mail);
            log.info("B2B confirmation mail accepted for delivery");
        } catch (Exception ex) {
            log.warn("Failed to send B2B confirmation mail: {}", ex.getMessage());
        }
    }

    /**
     * New-enquiry notification to the configured team inbox (head/manager).
     * Skipped with a warning when CONTACT_RECEIVER_EMAIL is not configured.
     */
    public void sendTeamNotification(B2BEnquiry enquiry) {
        if (teamInbox == null || teamInbox.isBlank()) {
            log.warn("contact.mail.receiver is not configured — skipping B2B team notification");
            return;
        }
        try {
            StringBuilder body = new StringBuilder();
            body.append("New B2B enquiry via vpnexues.com/pricing/b2b\n\n");
            body.append("Company:   ").append(enquiry.getCompanyName()).append("\n");
            body.append("Contact:   ").append(enquiry.getContactName()).append("\n");
            body.append("Email:     ").append(enquiry.getEmail()).append("\n");
            body.append("Phone:     ").append(blankToNa(enquiry.getPhone())).append("\n");
            body.append("Country:   ").append(blankToNa(enquiry.getCountryCode())).append("\n");
            body.append("Products:  ").append(blankToNa(enquiry.getProductInterest())).append("\n");
            body.append("Quantity:  ").append(blankToNa(enquiry.getQuantity())).append("\n");
            body.append("Est value: ").append(enquiry.getEstimatedValue() == null ? "N/A" : enquiry.getEstimatedValue()).append("\n");
            body.append("Status:    ").append(enquiry.getStatus().name()).append("\n\n");
            body.append("Submitted at: ").append(now()).append("\n");
            body.append("Review in Super Admin → Support → B2B Enquiries");

            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(fromAddress);
            mail.setTo(teamInbox.trim());
            mail.setReplyTo(enquiry.getEmail());
            mail.setSubject("[VPNexues B2B] New enquiry from " + enquiry.getCompanyName());
            mail.setText(body.toString());
            mailSender.send(mail);
            log.info("B2B team notification mail accepted for delivery");
        } catch (Exception ex) {
            log.warn("Failed to send B2B team notification mail: {}", ex.getMessage());
        }
    }

    private String blankToNa(String value) {
        return (value == null || value.isBlank()) ? "N/A" : value;
    }

    private String now() {
        return ZonedDateTime.now(ZoneId.of("Asia/Kolkata"))
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a z"));
    }
}
