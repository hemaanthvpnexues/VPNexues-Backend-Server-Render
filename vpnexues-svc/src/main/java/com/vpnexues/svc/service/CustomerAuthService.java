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

    private final OtpProvider otpProvider;
    private final UserRepository userRepository;

    public void sendOtp(String phone) {
        otpProvider.send(phone);
    }

    /**
     * Verifies the OTP and returns the matching User, creating one on first login (signup-by-OTP).
     * {@code name}, if provided, is only applied when creating a brand-new user — an existing/returning
     * user's stored name is never overwritten by whatever happens to be typed at login.
     */
    public User verifyOtpAndResolveUser(String phone, String code, String name) {
        if (!otpProvider.verify(phone, code)) {
            throw new BadRequestException("Invalid or expired OTP");
        }
        return findOrCreateUser(phone, name);
    }

    /**
     * Verifies Firebase authentication and returns the matching User, creating one on first login.
     * Firebase already verified the OTP on the client side — this method just finds/creates
     * the backend user record by Firebase UID or phone number.
     *
     * @param firebaseUid Firebase user UID (from the ID token)
     * @param phone       phone number (E.164 format)
     * @param name        optional name for new users
     * @return the existing or newly created User
     */
    public User verifyWithFirebase(String firebaseUid, String phone, String name) {
        // Try finding by Firebase UID first (returning user)
        return userRepository.findByFirebaseUid(firebaseUid)
                .orElseGet(() -> {
                    // Try finding by phone (user existed before Firebase migration)
                    return userRepository.findByPhone(phone)
                            .map(existingUser -> {
                                // Link Firebase UID to existing account
                                existingUser.setFirebaseUid(firebaseUid);
                                return existingUser;
                            })
                            .orElseGet(() -> {
                                // Brand new user — create with Firebase UID
                                User user = new User();
                                user.setPhone(phone);
                                user.setFirebaseUid(firebaseUid);
                                if (name != null && !name.isBlank()) {
                                    user.setName(name.trim());
                                }
                                return userRepository.save(user);
                            });
                });
    }

    private User findOrCreateUser(String phone, String name) {
        return userRepository.findByPhone(phone).orElseGet(() -> {
            User user = new User();
            user.setPhone(phone);
            if (name != null && !name.isBlank()) {
                user.setName(name.trim());
            }
            return userRepository.save(user);
        });
    }
}
