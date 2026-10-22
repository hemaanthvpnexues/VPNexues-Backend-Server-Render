package com.vpnexues.svc.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Access-pass view model.
 *
 * <p>{@code code} is the plaintext passcode and is populated <b>only</b> by the generate
 * endpoint — it is shown once in the dashboard and then only this response ever carried it.
 * Every other endpoint (status, history) returns {@code null}; the DB only holds the hash.
 *
 * <p>{@code status} is one of {@code ACTIVE | USED | EXPIRED | INVALIDATED}.
 */
public record AdminAccessPassDto(
        UUID id,
        UUID adminId,
        String code,
        Instant generatedAt,
        Instant expiresAt,
        int attemptsRemaining,
        String status,
        String generatedByName,
        Instant usedAt,
        String usedIp) {}
