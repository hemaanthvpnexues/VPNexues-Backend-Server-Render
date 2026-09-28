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
     * Signup is deliberately permissive about the profile: the shipping frontend sends no
     * {@code email} at all and its name field is not mandatory, so both values are optional
     * here — otherwise a first-time visitor cannot complete signup. Either one can be added
     * later through the account "add/verify email" flow. An existing/returning user's stored
     * profile is never overwritten by whatever is typed at login; blank fields on an old
     * account are backfilled once, if provided.
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
     * <p>Same signup rule as {@link #verifyOtpAndResolveUser}: {@code name} and {@code email}
     * are optional for new users (validated when present); returning users log in directly
     * without re-entering them.
     *
     * @param firebaseUid Firebase user UID (from the ID token)
     * @param phone       phone number (E.164 format)
     * @param name        optional profile, stored for new users and backfilled when blank
     * @param email       optional profile, validated when present, backfilled when blank
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

    /**
     * Creates the very first account. Neither profile field is mandatory — see
     * {@link #verifyOtpAndResolveUser} for why. Anything supplied is trimmed and validated
     * against the {@code users.name}/{@code users.email} {@code VARCHAR(255)} columns so a
     * bad value fails as a 400 here instead of a 500 at insert time.
     */
    private User createNewUser(String phone, String firebaseUid, String name, String email) {
        User user = new User();
        user.setPhone(phone);
        user.setFirebaseUid(firebaseUid);
        if (name != null && !name.isBlank()) {
            String trimmedName = name.trim();
            if (trimmedName.length() > 255) {
                throw new BadRequestException("Name must be 255 characters or fewer");
            }
            user.setName(trimmedName);
        }
        if (email != null && !email.isBlank()) {
            String trimmedEmail = email.trim();
            if (trimmedEmail.length() > 255) {
                throw new BadRequestException("Email must be 255 characters or fewer");
            }
            if (!EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
                throw new BadRequestException("Please enter a valid email address");
            }
            user.setEmail(trimmedEmail);
        }
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
