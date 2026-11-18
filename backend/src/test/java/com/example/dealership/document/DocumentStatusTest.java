package com.example.dealership.document;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DocumentStatusTest {

    @ParameterizedTest(name = "{0} -> {1}: {2}")
    @CsvSource({
            "PENDING, VERIFIED, true",
            "PENDING, REJECTED, true",
            "VERIFIED, REJECTED, true",
            "PENDING, PENDING, false",
            "VERIFIED, VERIFIED, false",
            "VERIFIED, PENDING, false",
            "REJECTED, VERIFIED, false",
            "REJECTED, PENDING, false",
            "REJECTED, REJECTED, false",
    })
    void canChangeToAllowsOnlyTheDocumentedTransitions(DocumentStatus from, DocumentStatus to, boolean allowed) {
        assertThat(from.canChangeTo(to)).isEqualTo(allowed);
    }

    @ParameterizedTest
    @CsvSource({"PENDING, true", "VERIFIED, true", "REJECTED, false"})
    void onlyRejectedDocumentsAreInactive(DocumentStatus status, boolean active) {
        assertThat(status.isActive()).isEqualTo(active);
    }
}
