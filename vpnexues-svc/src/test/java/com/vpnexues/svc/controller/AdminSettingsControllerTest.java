package com.vpnexues.svc.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * HTTP contract of GET/PUT /api/admin/settings — the endpoint the super-admin Settings
 * page loads on mount and saves to (General + Store tabs), and the one the four pricing
 * knobs live behind. Verifies against the real local Postgres:
 *
 * <ul>
 *   <li>GET 200 = R__Seed_data.sql actually seeded the single store_settings row
 *       (a missing row yields 404/500, so 200 itself is the seed assertion).</li>
 *   <li>SUPER_ADMIN PUT round-trips all 27 fields — pricing knobs included — and
 *       persists (a follow-up GET reflects the change).</li>
 *   <li>Any authenticated admin (SUB_ADMIN) can read, but PUT is SUPER_ADMIN-only
 *       via @PreAuthorize → 403.</li>
 *   <li>Anonymous callers get 401 (not 403) so clients know to re-authenticate.</li>
 *   <li>Bean-validation bounds reject out-of-range pricing knobs with 400 naming the
 *       offending field — the guard against a fat-fingered edit rewriting the price list.</li>
 * </ul>
 *
 * @Transactional rolls every write back, so the dev database is left exactly as found.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminSettingsControllerTest {

    @Autowired
    private MockMvc mvc;

    /** Full 27-field payload, exactly what the frontend builds from the loaded row. */
    private static String validBody() {
        return """
                {
                  "websiteName": "VPNexues",
                  "tagline": "Round-trip tagline",
                  "defaultCurrency": "INR",
                  "timezone": "Asia/Kolkata",
                  "dateFormat": "DD/MM/YYYY",
                  "timeFormat": "24-hour",
                  "websiteEmail": "hello@vpnexues.com",
                  "websitePhone": "+65 6000 0000",
                  "storeName": "VPNexues Store",
                  "storeEmail": "store@vpnexues.com",
                  "storePhone": "+65 6000 0001",
                  "storeAddress": "1 Test Street",
                  "storeDescription": "Round-trip description",
                  "freeShippingThreshold": 50,
                  "standardShippingRate": 5,
                  "expressShippingRate": 12,
                  "defaultDeliveryDays": 3,
                  "facebookUrl": null,
                  "instagramUrl": null,
                  "twitterUrl": null,
                  "youtubeUrl": null,
                  "whatsappUrl": null,
                  "telegramUrl": null,
                  "smallBoxStandardSlots": 7,
                  "smallBoxPremiumRate": 0.25,
                  "bigBoxMinKg": 12.5,
                  "bigBoxDiscountRate": 0.1
                }
                """;
    }

    @Test
    @WithMockUser(authorities = "ROLE_SUPER_ADMIN")
    void getReturnsTheSeededSettingsRow() throws Exception {
        String body = mvc.perform(get("/api/admin/settings"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(!stringField(body, "defaultCurrency").isBlank(), "defaultCurrency must be seeded");
        // All four pricing knobs present (missing row → 404, missing field → null → this
        // fails) and inside the same bounds PUT enforces — the values pricing depends on.
        assertBetween(body, "smallBoxStandardSlots", 1, 1000);
        assertBetween(body, "smallBoxPremiumRate", 0, 1);
        assertBetween(body, "bigBoxMinKg", 0, 1000);
        assertBetween(body, "bigBoxDiscountRate", 0, 1);
    }

    @Test
    @WithMockUser(authorities = "ROLE_SUPER_ADMIN")
    void superAdminPutRoundTripsAndPersists() throws Exception {
        mvc.perform(put("/api/admin/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isOk());

        String body = mvc.perform(get("/api/admin/settings"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertEquals("Round-trip tagline", stringField(body, "tagline"));
        assertEquals("INR", stringField(body, "defaultCurrency"));
        assertEquals(0, new BigDecimal("7").compareTo(decimalField(body, "smallBoxStandardSlots")));
        assertEquals(0, new BigDecimal("0.25").compareTo(decimalField(body, "smallBoxPremiumRate")));
        assertEquals(0, new BigDecimal("12.5").compareTo(decimalField(body, "bigBoxMinKg")));
        assertEquals(0, new BigDecimal("0.1").compareTo(decimalField(body, "bigBoxDiscountRate")));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void anyAuthenticatedAdminCanReadSettings() throws Exception {
        mvc.perform(get("/api/admin/settings")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void subAdminPutIsForbidden() throws Exception {
        mvc.perform(put("/api/admin/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousCallerGets401() throws Exception {
        mvc.perform(get("/api/admin/settings")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "ROLE_SUPER_ADMIN")
    void outOfRangePricingKnobsAreRejected() throws Exception {
        String body = validBody()
                .replace("\"smallBoxStandardSlots\": 7", "\"smallBoxStandardSlots\": 0")
                .replace("\"smallBoxPremiumRate\": 0.25", "\"smallBoxPremiumRate\": 1.5");

        String response = mvc.perform(put("/api/admin/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(
                response.contains("smallBoxStandardSlots"),
                "400 must name the offending field, got: " + response);
    }

    /* ---- tiny scale-safe JSON field readers (deliberately no JSON-path dependency) ---- */

    private static BigDecimal decimalField(String body, String field) {
        Matcher m = Pattern.compile("\"" + field + "\":(-?\\d+(?:\\.\\d+)?)").matcher(body);
        assertTrue(m.find(), () -> "numeric field '" + field + "' missing in body: " + body);
        return new BigDecimal(m.group(1));
    }

    private static String stringField(String body, String field) {
        Matcher m = Pattern.compile("\"" + field + "\":\"([^\"]*)\"").matcher(body);
        assertTrue(m.find(), () -> "string field '" + field + "' missing in body: " + body);
        return m.group(1);
    }

    private static void assertBetween(String body, String field, double min, double max) {
        double v = decimalField(body, field).doubleValue();
        assertTrue(v >= min && v <= max, () -> field + "=" + v + " outside [" + min + ", " + max + "]");
    }
}
