package com.example.dealership.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    /** Business dates ("today") are dealership dates. */
    public static final ZoneId DEALERSHIP_ZONE = ZoneId.of("Europe/Warsaw");

    @Bean
    public Clock clock() {
        return Clock.system(DEALERSHIP_ZONE);
    }
}
