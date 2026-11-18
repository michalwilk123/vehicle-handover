package com.example.dealership.document;

import java.time.Clock;
import java.time.LocalDate;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NotAfterTodayValidator implements ConstraintValidator<NotAfterToday, LocalDate> {

    private final Clock clock;

    // Spring creates the validator and injects the dealership clock.
    public NotAfterTodayValidator(Clock clock) {
        this.clock = clock;
    }

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        return value == null || !value.isAfter(LocalDate.now(clock));
    }
}
