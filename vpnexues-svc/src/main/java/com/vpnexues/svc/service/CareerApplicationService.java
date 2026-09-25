package com.vpnexues.svc.service;

import com.vpnexues.svc.dto.CreateCareerApplicationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CareerApplicationService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@vpnexues.com}")
    private String fromAddress;

    private static final String HR_EMAIL = "hrvpnexues@gmail.com";

    public void sendApplicationEmail(CreateCareerApplicationRequest req) {
        String subject = "New Application: " + req.role() + " — " + req.name();

        StringBuilder body = new StringBuilder();
        body.append("New job application received via vpnexues.com/careers\n\n");
        body.append("Candidate Details:\n");
        body.append("  Name:          ").append(req.name()).append("\n");
        body.append("  Email:         ").append(req.email()).append("\n");
        body.append("  Phone:         ").append(blankIfNull(req.phone())).append("\n");
        body.append("  Location:      ").append(blankIfNull(req.location())).append("\n");
        body.append("  Role:          ").append(req.role()).append("\n");
        body.append("  Availability:  ").append(blankIfNull(req.availability())).append("\n");
        body.append("  How Found Us:  ").append(blankIfNull(req.source())).append("\n");
        body.append("  Resume Link:   ").append(blankIfNull(req.resume())).append("\n");
        body.append("  LinkedIn:      ").append(blankIfNull(req.linkedin())).append("\n");
        body.append("  GitHub:        ").append(blankIfNull(req.github())).append("\n\n");

        if (req.whyJoin() != null && !req.whyJoin().isBlank()) {
            body.append("Why Join:\n  ").append(req.whyJoin()).append("\n\n");
        }

        body.append("Applied at: ").append(blankIfNull(req.timestamp()));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(HR_EMAIL);
        message.setSubject(subject);
        message.setText(body.toString());
        mailSender.send(message);
    }

    private String blankIfNull(String value) {
        return (value == null || value.isBlank()) ? "N/A" : value;
    }
}
