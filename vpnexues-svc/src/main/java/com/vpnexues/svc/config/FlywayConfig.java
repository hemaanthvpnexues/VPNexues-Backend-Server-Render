package com.vpnexues.svc.config;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.boot.jpa.autoconfigure.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Manual Flyway wiring. Spring Boot 4.1's spring-boot-autoconfigure module no longer
 * ships a FlywayAutoConfiguration (confirmed absent from the jar) — flyway-core alone
 * does not self-register with Spring Boot anymore, so this replaces what that
 * auto-configuration used to do: run migrations, then force JPA's EntityManagerFactory
 * to depend on this bean so schema validation never races the migration.
 */
@Configuration
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        Flyway fw = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .outOfOrder(true)
                .validateOnMigrate(false)
                .load();
        fw.repair();
        return fw;
    }

    @Configuration
    static class FlywayEntityManagerFactoryDependsOnPostProcessor extends EntityManagerFactoryDependsOnPostProcessor {
        FlywayEntityManagerFactoryDependsOnPostProcessor() {
            super(Flyway.class);
        }
    }
}
