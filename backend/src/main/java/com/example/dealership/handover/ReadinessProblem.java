package com.example.dealership.handover;

import com.example.dealership.document.DocumentType;

/** {@code documentType} is null for a problem of the vehicle itself, such as a missing handover date. */
public record ReadinessProblem(DocumentType documentType, ReadinessReason reason) {
}
