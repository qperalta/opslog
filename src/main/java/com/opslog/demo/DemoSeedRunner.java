package com.opslog.demo;

import com.opslog.common.Require;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;

/* off unless OPSLOG_DEMO_ENABLED=true; runs once at startup after Flyway */
@Component
@ConditionalOnProperty(name="opslog.demo.enabled", havingValue="true")
class DemoSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoSeedRunner.class);

    private final DemoSeeder seeder;
    private final DemoProperties properties;

    DemoSeedRunner(DemoSeeder seeder, DemoProperties properties) {
        this.seeder = seeder;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        Require.that(properties.password() != null && properties.password().length() >= 8,
                "OPSLOG_DEMO_PASSWORD (8+ characters) is required when the demo seed is enabled");
        var account = new DemoSeeder.Account(properties.email(), properties.password(), properties.displayName());
        boolean created = seeder.seed(account, LocalDate.now(ZoneOffset.UTC), properties.reset());
        if(created) log.info("Demo account {} seeded with {} days of history", properties.email(), DemoSeeder.DAYS);
        else log.info("Demo account {} already exists; set OPSLOG_DEMO_RESET=true to rebuild it", properties.email());
    }
}
