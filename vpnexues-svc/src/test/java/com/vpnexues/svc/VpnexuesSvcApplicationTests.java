package com.vpnexues.svc;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Boots the full application context against the local Postgres (Flyway migrates + Hibernate
 * validates the schema) — the cheapest possible check that nothing is structurally broken.
 * See build.gradle's `tasks.named('test')` for how .env gets loaded for this to work.
 */
@SpringBootTest
class VpnexuesSvcApplicationTests {

    @Test
    void contextLoads() {
    }
}
