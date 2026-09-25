package com.vpnexues.svc.util;

import java.text.Normalizer;
import java.util.Locale;

public final class Slugify {

    private Slugify() {
    }

    /** Normalizes Unicode, lowercases, replaces runs of non-alphanumerics with a single hyphen, trims hyphens. */
    public static String slugify(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        // Normalize Unicode to ASCII approximation, then strip remaining non-ASCII
        String normalized = Normalizer.normalize(input.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("[\\s-]+", "-")
                .replaceAll("(^-+)|(-+$)", "");
        return normalized;
    }
}
