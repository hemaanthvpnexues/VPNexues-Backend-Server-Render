package com.vpnexues.svc.config;

import com.vpnexues.svc.auth.OtpChallengeService;
import com.vpnexues.svc.auth.OtpProvider;
import com.vpnexues.svc.auth.StubOtpProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OtpProviderConfig {

    @Bean
    public OtpProvider otpProvider(OtpChallengeService otpChallengeService,
                                  @Value("${app.otp.provider:stub}") String provider) {
        return switch (provider) {
            default -> new StubOtpProvider(otpChallengeService);
        };
    }
}
