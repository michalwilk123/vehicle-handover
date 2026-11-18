package com.example.dealership.handover;

import java.util.List;

import com.example.dealership.document.DocumentType;

public record HandoverReadinessResponse(
        boolean ready,
        List<DocumentType> missingDocumentTypes,
        List<ReadinessProblem> problems
) {
}
