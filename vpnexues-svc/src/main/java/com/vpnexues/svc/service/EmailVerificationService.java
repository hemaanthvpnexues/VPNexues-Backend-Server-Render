package com.vpnexues.svc.service;

import com.vpnexues.svc.auth.OtpChallengeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/** Post-registration "add/verify email" flow — shares the same OTP challenge store as phone login. */
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final String CHANNEL = "EMAIL";

    private final OtpChallengeService otpChallengeService;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@vpnexues.com}")
    private String fromAddress;

    public void sendCode(String email) {
        String code = otpChallengeService.generateAndStore(CHANNEL, email);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("Your VPNexues verification code");
        message.setText("Your verification code is " + code + ". It expires in 5 minutes.");
        mailSender.send(message);
    }

    public boolean verifyCode(String email, String code) {
        return otpChallengeService.verify(CHANNEL, email, code);
    }
}
