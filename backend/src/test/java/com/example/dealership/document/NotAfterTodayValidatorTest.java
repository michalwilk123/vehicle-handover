package com.example.dealership.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import com.example.dealership.config.ClockConfig;

import org.junit.jupiter.api.Test;

class NotAfterTodayValidatorTest {

    // 2026-06-15T22:30Z is already 2026-06-16 00:30 in Warsaw (UTC+2 in summer).
    private final NotAfterTodayValidator validator = new NotAfterTodayValidator(
            Clock.fixed(Instant.parse("2026-06-15T22:30:00Z"), ClockConfig.DEALERSHIP_ZONE));

    @Test
    void acceptsTodayInWarsawEvenWhenUtcIsStillOnThePreviousDay() {
        assertThat(validator.isValid(LocalDate.of(2026, 6, 16), null)).isTrue();
        assertThat(validator.isValid(LocalDate.of(2026, 6, 15), null)).isTrue();
    }

    @Test
    void rejectsDateAfterToday() {
        assertThat(validator.isValid(LocalDate.of(2026, 6, 17), null)).isFalse();
    }

    @Test
    void acceptsMissingDateSoThatNotNullReportsIt() {
        assertThat(validator.isValid(null, null)).isTrue();
    }
}
