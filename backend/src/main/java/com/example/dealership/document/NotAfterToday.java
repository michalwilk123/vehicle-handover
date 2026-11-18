package com.example.dealership.document;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/** The date must not be after the current dealership date. Null values are valid. */
@Documented
@Constraint(validatedBy = NotAfterTodayValidator.class)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface NotAfterToday {

    String message() default "must not be after today";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
