package com.example.dealership;

import java.time.Clock;
import java.time.Instant;

import com.example.dealership.config.ClockConfig;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class FixedClockTestConfig {

    @Bean
    @Primary
    Clock fixedDealershipClock() {
        return Clock.fixed(Instant.parse("2026-06-15T10:00:00Z"), ClockConfig.DEALERSHIP_ZONE);
    }
}
