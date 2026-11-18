package com.example.dealership.document;

import java.time.LocalDate;

public record VehicleDocumentResponse(
        Long id,
        DocumentType type,
        String number,
        LocalDate issueDate,
        LocalDate expiryDate,
        DocumentStatus status
) {
}
