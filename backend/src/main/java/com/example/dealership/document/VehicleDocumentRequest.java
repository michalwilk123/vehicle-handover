package com.example.dealership.document;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VehicleDocumentRequest(
        @NotNull
        DocumentType type,

        @NotBlank
        @Size(max = 100)
        String number,

        @NotNull
        @NotAfterToday
        LocalDate issueDate,

        LocalDate expiryDate
) {

    // Surrounding spaces in the number are ignored, so the length limit applies to the trimmed value.
    public VehicleDocumentRequest {
        number = number == null ? null : number.strip();
    }
}
