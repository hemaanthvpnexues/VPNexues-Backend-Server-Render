package com.vpnexues.svc.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vpnexues.svc.auth.OtpProvider;
import com.vpnexues.svc.entity.User;
import com.vpnexues.svc.exception.BadRequestException;
import com.vpnexues.svc.repository.UserRepository;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Signup profile rules — no DB, no Spring context (same style as SupportChatServiceTest).
 *
 * <p>The headline case is {@link #signupCompletesWithoutNameOrEmail()}: the shipping frontend
 * posts no {@code email} and does not mark its name input required, so a first-time visitor
 * used to get a 400 and could never finish signup.
 */
class CustomerAuthServiceTest {

    private static final String PHONE = "+918838542439";
    private static final String OTP = "123456";

    private final Map<String, User> byPhone = new HashMap<>();
    private CustomerAuthService service;

    @BeforeEach
    void setUp() {
        byPhone.clear();
        service = new CustomerAuthService(new AcceptingOtpProvider(), fakeUsers());
    }

    @Test
    void signupCompletesWithoutNameOrEmail() {
        User user = service.verifyOtpAndResolveUser(PHONE, OTP, null, null);

        assertNull(user.getName());
        assertNull(user.getEmail());
        assertEquals(PHONE, user.getPhone());
        assertTrue(byPhone.containsKey(PHONE), "user must be persisted");
    }

    @Test
    void signupCompletesWithBlankNameSentByFrontend() {
        User user = service.verifyOtpAndResolveUser(PHONE, OTP, "   ", "");

        assertNull(user.getName());
        assertNull(user.getEmail());
        assertTrue(byPhone.containsKey(PHONE));
    }

    @Test
    void signupStoresTrimmedNameAndEmailWhenSupplied() {
        User user = service.verifyOtpAndResolveUser(PHONE, OTP, "  Arun Kumar ", "  arun@example.com ");

        assertEquals("Arun Kumar", user.getName());
        assertEquals("arun@example.com", user.getEmail());
    }

    @Test
    void signupRejectsMalformedEmailButStillAcceptsTheName() {
        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> service.verifyOtpAndResolveUser(PHONE, OTP, "Arun", "not-an-email"));

        assertTrue(ex.getMessage().contains("valid email"));
        assertFalse(byPhone.containsKey(PHONE), "nothing must be persisted on rejection");
    }

    @Test
    void signupRejectsProfileLongerThanTheVarcharColumn() {
        assertThrows(
                BadRequestException.class,
                () -> service.verifyOtpAndResolveUser(PHONE, OTP, "x".repeat(256), null));

        assertThrows(
                BadRequestException.class,
                () -> service.verifyOtpAndResolveUser(PHONE, OTP, null, "a@" + "x".repeat(256) + ".com"));

        assertFalse(byPhone.containsKey(PHONE));
    }

    @Test
    void invalidOtpStillFails() {
        assertThrows(
                BadRequestException.class,
                () -> service.verifyOtpAndResolveUser(PHONE, "000000", null, null));
    }

    @Test
    void firebaseSignupAlsoWorksWithoutEmail() {
        User user = service.verifyWithFirebase("firebase-uid-1", PHONE, "Arun", null);

        assertEquals("Arun", user.getName());
        assertNull(user.getEmail());
        assertEquals("firebase-uid-1", user.getFirebaseUid());
    }

    @Test
    void returningUserKeepsStoredProfileWhenLoginSendsBlankFields() {
        User existing = new User();
        existing.setPhone(PHONE);
        existing.setName("Arun Kumar");
        existing.setEmail("arun@example.com");
        byPhone.put(PHONE, existing);

        User user = service.verifyOtpAndResolveUser(PHONE, OTP, "Someone Else", "other@example.com");

        assertEquals("Arun Kumar", user.getName());
        assertEquals("arun@example.com", user.getEmail());
    }

    @Test
    void returningUserWithBlankStoredEmailGetsBackfilledOnce() {
        User existing = new User();
        existing.setPhone(PHONE);
        byPhone.put(PHONE, existing);

        User user = service.verifyOtpAndResolveUser(PHONE, OTP, "Arun", "arun@example.com");

        assertEquals("Arun", user.getName());
        assertEquals("arun@example.com", user.getEmail());
    }

    private UserRepository fakeUsers() {
        return (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class<?>[] {UserRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByPhone" -> byPhone.values().stream()
                            .filter(u -> u.getPhone().equals(args[0]))
                            .findFirst();
                    case "findByFirebaseUid" -> byPhone.values().stream()
                            .filter(u -> args[0] != null && args[0].equals(u.getFirebaseUid()))
                            .findFirst();
                    case "save" -> {
                        User stored = (User) args[0];
                        byPhone.put(stored.getPhone(), stored);
                        yield stored;
                    }
                    case "toString" -> "UserRepository(fake)";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException("not stubbed: " + method.getName());
                });
    }

    private static final class AcceptingOtpProvider implements OtpProvider {

        @Override
        public void send(String phone) {
            // delivery is not under test here
        }

        @Override
        public boolean verify(String phone, String code) {
            return OTP.equals(code);
        }
    }
}
