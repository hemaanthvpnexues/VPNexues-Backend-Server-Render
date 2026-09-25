package com.vpnexues.svc.controller;

import com.vpnexues.svc.dto.SendEmailCodeRequest;
import com.vpnexues.svc.dto.UpdateProfileRequest;
import com.vpnexues.svc.dto.UserDto;
import com.vpnexues.svc.dto.VerifyEmailCodeRequest;
import com.vpnexues.svc.entity.User;
import com.vpnexues.svc.exception.BadRequestException;
import com.vpnexues.svc.exception.NotFoundException;
import com.vpnexues.svc.repository.AddressRepository;
import com.vpnexues.svc.repository.CartRepository;
import com.vpnexues.svc.repository.OrderRepository;
import com.vpnexues.svc.repository.OtpChallengeRepository;
import com.vpnexues.svc.repository.PaymentRepository;
import com.vpnexues.svc.repository.UserRepository;
import com.vpnexues.svc.security.CookieNames;
import com.vpnexues.svc.service.EmailVerificationService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Profile self-service for the logged-in customer — email, profile, and permanent account deletion. */
@RestController
@RequiredArgsConstructor
public class AccountController {

    private final EmailVerificationService emailVerificationService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final OtpChallengeRepository otpChallengeRepository;

    @Value("${app.cookie.secure:false}")
    private boolean secureCookies;

    @PostMapping("/api/account/email/send-code")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sendEmailCode(@AuthenticationPrincipal UUID userId, @RequestBody @Valid SendEmailCodeRequest req) {
        emailVerificationService.sendCode(req.email());
    }

    @PostMapping("/api/account/email/verify")
    public UserDto verifyEmail(@AuthenticationPrincipal UUID userId, @RequestBody @Valid VerifyEmailCodeRequest req) {
        if (!emailVerificationService.verifyCode(req.email(), req.code())) {
            throw new BadRequestException("Invalid or expired code");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        user.setEmail(req.email());
        user.setEmailVerified(true);
        userRepository.save(user);
        return UserDto.of(user);
    }

    @PutMapping("/api/account/profile")
    public UserDto updateProfile(@AuthenticationPrincipal UUID userId, @RequestBody @Valid UpdateProfileRequest req) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        user.setName(req.name().trim());
        userRepository.save(user);
        return UserDto.of(user);
    }

    @DeleteMapping("/api/account")
    @Transactional
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal UUID userId, HttpServletResponse response) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));

        // 1. Delete payments first (FK -> orders)
        var payments = paymentRepository.findByOrderUserIdOrderByCreatedAtDesc(userId);
        if (!payments.isEmpty()) {
            paymentRepository.deleteAll(payments);
            paymentRepository.flush();
        }

        // 2. Delete orders (cascades to order_items via orphanRemoval)
        var orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (!orders.isEmpty()) {
            orderRepository.deleteAll(orders);
            orderRepository.flush();
        }

        // 3. Delete cart (cascades to cart_items)
        cartRepository.findByUserId(userId).ifPresent(cart -> {
            cartRepository.delete(cart);
            cartRepository.flush();
        });

        // 4. Delete addresses
        var addresses = addressRepository.findByUserId(userId);
        if (!addresses.isEmpty()) {
            addressRepository.deleteAll(addresses);
            addressRepository.flush();
        }

        // 5. Delete OTP challenges for this phone / email
        if (user.getPhone() != null) {
            otpChallengeRepository.findByChannelAndIdentifier("PHONE", user.getPhone())
                    .ifPresent(otpChallengeRepository::delete);
        }
        if (user.getEmail() != null) {
            otpChallengeRepository.findByChannelAndIdentifier("EMAIL", user.getEmail())
                    .ifPresent(otpChallengeRepository::delete);
            otpChallengeRepository.findByChannelAndIdentifier("PHONE", user.getEmail())
                    .ifPresent(otpChallengeRepository::delete);
        }

        // 6. Finally delete user
        userRepository.delete(user);
        userRepository.flush();

        // Clear auth cookie so browser is logged out immediately
        ResponseCookie clearCookie = ResponseCookie.from(CookieNames.CUSTOMER_ACCESS_TOKEN, "")
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite(secureCookies ? "None" : "Lax")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, clearCookie.toString());

        return ResponseEntity.noContent().build();
    }
}
