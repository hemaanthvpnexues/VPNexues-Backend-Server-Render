package com.vpnexues.svc.auth;

/**
 * Swappable OTP delivery. StubOtpProvider (default, OTP_PROVIDER=stub) logs the code
 * instead of sending it. Swap to a real SMS/WhatsApp provider later via that one env
 * var — no controller/service code needs to change.
 */
public interface OtpProvider {

    void send(String phone);

    boolean verify(String phone, String code);
}
