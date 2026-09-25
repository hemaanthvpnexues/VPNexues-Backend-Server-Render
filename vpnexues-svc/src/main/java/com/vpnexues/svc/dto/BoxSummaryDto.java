package com.vpnexues.svc.dto;

import java.math.BigDecimal;

/**
 * Live progress summary for an in-progress Small/Big Box in the cart — server-computed so the
 * builder UI's threshold/discount messaging always agrees with what checkout will actually charge.
 *
 * @param boxType         SMALL_BOX or BIG_BOX
 * @param progressValue   item count (Small Box) or kg (Big Box)
 * @param threshold       10 for both (10 items / 10 kg)
 * @param minimumMet      progressValue >= threshold
 * @param subtotal        pre-discount/pre-premium-adjusted line total sum
 * @param premiumOrDiscount Small Box: total +15% premium already included in subtotal (informational).
 *                          Big Box: the 12% discount amount (0 until minimumMet).
 * @param total           amount this box will actually add to itemTotal (Big Box: subtotal - discount)
 */
public record BoxSummaryDto(
        String boxType,
        BigDecimal progressValue,
        BigDecimal threshold,
        boolean minimumMet,
        BigDecimal subtotal,
        BigDecimal premiumOrDiscount,
        BigDecimal total) {
}
