package com.vpnexues.svc.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vpnexues.svc.entity.User;
import com.vpnexues.svc.entity.UserDevice;
import com.vpnexues.svc.repository.UserDeviceRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for the trusted-device gate behind OTP-less phone login:
 * only a matching, unexpired token renews into a session credential; the raw
 * token is never stored; the per-user device cap evicts the least-recently seen device.
 */
class TrustedDeviceServiceTest {

    private UserDeviceRepository repository;
    private TrustedDeviceService service;
    private HttpServletRequest request;
    private User user;

    @BeforeEach
    void setUp() {
        repository = mock(UserDeviceRepository.class);
        when(repository.save(any(UserDevice.class))).thenAnswer(inv -> inv.getArgument(0));
        service = new TrustedDeviceService(repository);
        ReflectionTestUtils.setField(service, "ttlDays", 180L);

        request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.7");
        when(request.getHeader("User-Agent")).thenReturn("TestBrowser/1.0");

        user = new User();
        user.setId(UUID.randomUUID());
        user.setPhone("+918489783585");
    }

    @Test
    void renewIfTrustedRejectsMissingOrForeignToken() {
        when(repository.findByUserAndDeviceTokenHashAndRevokedAtIsNull(any(), any()))
                .thenReturn(Optional.empty());

        assertTrue(service.renewIfTrusted(user, null, request).isEmpty(), "no cookie → OTP challenge");
        assertTrue(service.renewIfTrusted(user, "  ", request).isEmpty(), "blank cookie → OTP challenge");
        assertTrue(service.renewIfTrusted(user, "deadbeef", request).isEmpty(), "unknown token → OTP challenge");
        verify(repository, never()).save(any(UserDevice.class));
    }

    @Test
    void trustStoresOnlyHashAndReturnsRawToken() {
        String raw = service.trust(user, request);

        assertEquals(64, raw.length(), "32 random bytes → 64 hex chars");
        var saved = org.mockito.ArgumentCaptor.forClass(UserDevice.class);
        verify(repository).save(saved.capture());

        UserDevice device = saved.getValue();
        assertNotEquals(raw, device.getDeviceTokenHash(), "raw token must not be stored");
        assertEquals(TrustedDeviceService.sha256(raw), device.getDeviceTokenHash());
        assertEquals(user, device.getUser());
        assertEquals("203.0.113.7", device.getIp());
        assertEquals("TestBrowser/1.0", device.getUserAgent());
        assertNull(device.getRevokedAt());
        assertTrue(Math.abs(device.getLastSeenAt().getEpochSecond() - Instant.now().getEpochSecond()) <= 2,
                "last_seen_at is set to now");
    }

    @Test
    void renewIfTrustedRotatesTokenAndBumpsLastSeen() {
        String oldRaw = "a".repeat(64);
        UserDevice existing = activeDevice(TrustedDeviceService.sha256(oldRaw));
        when(repository.findByUserAndDeviceTokenHashAndRevokedAtIsNull(user, TrustedDeviceService.sha256(oldRaw)))
                .thenReturn(Optional.of(existing));

        Optional<String> renewed = service.renewIfTrusted(user, oldRaw, request);

        assertTrue(renewed.isPresent(), "matching unexpired token → trusted");
        assertNotEquals(oldRaw, renewed.get(), "token rotates on every trusted login");
        assertEquals(TrustedDeviceService.sha256(renewed.get()), existing.getDeviceTokenHash(),
                "row must track the rotated hash");
        assertNull(existing.getRevokedAt());
        verify(repository).save(existing);
    }

    @Test
    void expiredTokenIsTreatedAsUntrusted() {
        String oldRaw = "b".repeat(64);
        UserDevice stale = activeDevice(TrustedDeviceService.sha256(oldRaw));
        stale.setCreatedAt(Instant.now().minus(181, ChronoUnit.DAYS));
        when(repository.findByUserAndDeviceTokenHashAndRevokedAtIsNull(any(), any()))
                .thenReturn(Optional.of(stale));

        assertTrue(service.renewIfTrusted(user, oldRaw, request).isEmpty(), "expired token → OTP challenge");
        verify(repository, never()).save(any(UserDevice.class));
    }

    @Test
    void deviceCapRevokesLeastRecentlySeenOverflow() {
        UserDevice oldest = activeDevice("h1");
        when(repository.countByUserAndRevokedAtIsNull(user))
                .thenReturn((long) TrustedDeviceService.MAX_ACTIVE_DEVICES + 1);
        when(repository.findFirstByUserAndRevokedAtIsNullOrderByLastSeenAtAsc(user))
                .thenReturn(Optional.of(oldest));

        service.trust(user, request);

        assertNotNull(oldest.getRevokedAt(), "oldest overflow device gets revoked");
        verify(repository).save(oldest);
    }

    private UserDevice activeDevice(String hash) {
        UserDevice device = new UserDevice();
        device.setUser(user);
        device.setDeviceTokenHash(hash);
        device.setCreatedAt(Instant.now().minus(10, ChronoUnit.DAYS));
        device.setLastSeenAt(Instant.now().minus(5, ChronoUnit.DAYS));
        return device;
    }
}
