package com.vpnexues.svc.auth;

import com.vpnexues.svc.exception.BadRequestException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Delivers login OTPs via the Meta WhatsApp Cloud API (activate with {@code OTP_PROVIDER=whatsapp}).
 *
 * <p>Requires a verified WhatsApp Business phone number, a permanent access token, and — specifically
 * for OTP — a pre-approved "Authentication" category message template in WhatsApp Manager; Meta does
 * not allow free-form business-initiated messages for this. The request body below follows the
 * documented Cloud API "Authentication Templates" body-parameter shape (one text parameter: the code).
 * If your approved template also has a copy-code button component, add a matching {@code button}
 * component to {@code components} below — it must match your template's actual layout exactly or
 * Meta will reject the send.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.otp.provider", havingValue = "whatsapp")
public class WhatsAppOtpProvider implements OtpProvider {

    private static final String CHANNEL = "PHONE";

    private final OtpChallengeService otpChallengeService;
    private final RestClient restClient = RestClient.create();

    @Value("${app.whatsapp.access-token}")
    private String accessToken;

    @Value("${app.whatsapp.phone-number-id}")
    private String phoneNumberId;

    @Value("${app.whatsapp.api-version}")
    private String apiVersion;

    @Value("${app.whatsapp.template-name}")
    private String templateName;

    @Value("${app.whatsapp.template-language}")
    private String templateLanguage;

    @Override
    public void send(String phone) {
        String code = otpChallengeService.generateAndStore(CHANNEL, phone);
        sendTemplateMessage(phone, code);
    }

    @Override
    public boolean verify(String phone, String code) {
        return otpChallengeService.verify(CHANNEL, phone, code);
    }

    private void sendTemplateMessage(String phone, String code) {
        String url = "https://graph.facebook.com/%s/%s/messages".formatted(apiVersion, phoneNumberId);
        Map<String, Object> body = Map.of(
                "messaging_product", "whatsapp",
                "to", phone,
                "type", "template",
                "template", Map.of(
                        "name", templateName,
                        "language", Map.of("code", templateLanguage),
                        "components", List.of(Map.of(
                                "type", "body",
                                "parameters", List.of(Map.of("type", "text", "text", code))))));
        try {
            restClient
                    .post()
                    .uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("WhatsApp OTP send failed for phone={}", phone, e);
            throw new BadRequestException("Could not send WhatsApp OTP, please try again");
        }
    }
}
