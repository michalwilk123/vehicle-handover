package com.example.dealership.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @NotBlank
        @Size(max = 50)
        String firstName,

        @NotBlank
        @Size(max = 50)
        String lastName,

        @NotBlank
        @Email
        @Size(max = 254)
        String email,

        @NotBlank
        @Size(max = 50)
        @Pattern(regexp = "[0-9 +\\-.()]*[0-9][0-9 +\\-.()]*",
                message = "must contain a digit and only digits, spaces, +, -, . and parentheses")
        String phone,

        @Pattern(regexp = "[0-9]{10}", message = "must be 10 digits")
        String nip
) {

    // Validation and storage see the same values: surrounding spaces are ignored, and an empty NIP means none.
    public CustomerRequest {
        firstName = trim(firstName);
        lastName = trim(lastName);
        email = trim(email);
        phone = trim(phone);
        nip = trim(nip);
        if (nip != null && nip.isEmpty()) {
            nip = null;
        }
    }

    private static String trim(String value) {
        return value == null ? null : value.strip();
    }
}
