package com.vpnexues.svc.service;

import com.vpnexues.svc.auth.OtpProvider;
import com.vpnexues.svc.entity.User;
import com.vpnexues.svc.exception.BadRequestException;
import com.vpnexues.svc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerAuthService {

    private static final java.util.regex.Pattern EMAIL_PATTERN =
            java.util.regex.Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final OtpProvider otpProvider;
    private final UserRepository userRepository;

    public void sendOtp(String phone) {
        otpProvider.send(phone);
    }

    /**
     * Verifies the OTP and returns the matching User, creating one on first login (signup-by-OTP).
     * A brand-new user MUST supply both {@code name} and {@code email} — signup cannot complete
     * (and therefore first login cannot happen) until they are entered. An existing/returning
     * user's stored profile is never overwritten by whatever is typed at login; blank fields
     * on an old account are backfilled once, if provided.
     */
    public User verifyOtpAndResolveUser(String phone, String code, String name, String email) {
        if (!otpProvider.verify(phone, code)) {
            throw new BadRequestException("Invalid or expired OTP");
        }
        return userRepository.findByPhone(phone)
                .map(existingUser -> backfillProfile(existingUser, name, email))
                .orElseGet(() -> createNewUser(phone, null, name, email));
    }

    /**
     * Verifies Firebase authentication and returns the matching User, creating one on first login.
     * Firebase already verified the OTP on the client side — this method just finds/creates
     * the backend user record by Firebase UID or phone number.
     *
     * <p>Same signup rule as {@link #verifyOtpAndResolveUser}: a brand-new user requires
     * {@code name} + {@code email}; returning users log in directly without re-entering them.
     *
     * @param firebaseUid Firebase user UID (from the ID token)
     * @param phone       phone number (E.164 format)
     * @param name        required for new users, optional for returning users
     * @param email       required for new users, optional for returning users
     * @return the existing or newly created User
     */
    public User verifyWithFirebase(String firebaseUid, String phone, String name, String email) {
        // Try finding by Firebase UID first (returning user)
        return userRepository.findByFirebaseUid(firebaseUid)
                .map(existingUser -> backfillProfile(existingUser, name, email))
                .orElseGet(() -> userRepository.findByPhone(phone)
                        .map(existingUser -> {
                            // Link Firebase UID to existing account
                            existingUser.setFirebaseUid(firebaseUid);
                            return backfillProfile(existingUser, name, email);
                        })
                        .orElseGet(() -> createNewUser(phone, firebaseUid, name, email)));
    }

    /** Creates the very first account — rejects signup until name AND email are supplied. */
    private User createNewUser(String phone, String firebaseUid, String name, String email) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Name is required to create your account");
        }
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Email is required to create your account");
        }
        String trimmedEmail = email.trim();
        if (!EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
            throw new BadRequestException("Please enter a valid email address");
        }
        User user = new User();
        user.setPhone(phone);
        user.setFirebaseUid(firebaseUid);
        user.setName(name.trim());
        user.setEmail(trimmedEmail);
        return userRepository.save(user);
    }

    /**
     * Returning user: never overwrites stored values, but fills in any profile field
     * that is still blank on this old account (one-time backfill).
     */
    private User backfillProfile(User user, String name, String email) {
        if ((user.getName() == null || user.getName().isBlank()) && name != null && !name.isBlank()) {
            user.setName(name.trim());
        }
        if ((user.getEmail() == null || user.getEmail().isBlank()) && email != null && !email.isBlank()) {
            String trimmedEmail = email.trim();
            if (EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
                user.setEmail(trimmedEmail);
            }
        }
        return user;
    }
}
