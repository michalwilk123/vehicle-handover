package com.example.dealership.handover;

import static com.example.dealership.document.DocumentType.COC;
import static com.example.dealership.document.DocumentType.INVOICE;
import static com.example.dealership.document.DocumentType.OC_POLICY;
import static com.example.dealership.document.DocumentType.REGISTRATION_POA;
import static com.example.dealership.document.DocumentType.SALES_AGREEMENT;
import static com.example.dealership.handover.ReadinessReason.EXPIRED;
import static com.example.dealership.handover.ReadinessReason.NOT_VERIFIED;
import static com.example.dealership.handover.ReadinessReason.NO_HANDOVER_DATE;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.example.dealership.document.DocumentStatus;
import com.example.dealership.document.DocumentType;
import com.example.dealership.document.VehicleDocument;

import org.junit.jupiter.api.Test;

/** The cases of the readiness table in INTENT.md, plus ordering rules. */
class HandoverReadinessTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);
    private static final LocalDate HANDOVER = LocalDate.of(2026, 7, 1);

    private final List<VehicleDocument> documents = new ArrayList<>(List.of(
            document(SALES_AGREEMENT, DocumentStatus.VERIFIED, null),
            document(INVOICE, DocumentStatus.VERIFIED, null),
            document(COC, DocumentStatus.VERIFIED, null),
            document(OC_POLICY, DocumentStatus.VERIFIED, HANDOVER.plusYears(1))));

    @Test
    void customerRegistersAndAllRequiredDocumentsAreVerified() {
        assertReady(evaluate(HANDOVER, false));
    }

    @Test
    void dealershipRegistersWithoutPowerOfAttorney() {
        HandoverReadinessResponse result = evaluate(HANDOVER, true);

        assertThat(result.ready()).isFalse();
        assertThat(result.missingDocumentTypes()).containsExactly(REGISTRATION_POA);
        assertThat(result.problems()).isEmpty();
    }

    @Test
    void dealershipRegistersWithVerifiedPowerOfAttorneyWithoutExpiryDate() {
        documents.add(document(REGISTRATION_POA, DocumentStatus.VERIFIED, null));

        assertReady(evaluate(HANDOVER, true));
    }

    @Test
    void optionalPendingPowerOfAttorneyIsIgnoredWhenCustomerRegisters() {
        documents.add(document(REGISTRATION_POA, DocumentStatus.PENDING, TODAY.minusDays(1)));

        assertReady(evaluate(HANDOVER, false));
    }

    @Test
    void onlyRejectedCocMeansCocIsMissing() {
        replace(COC, DocumentStatus.REJECTED, null);
        documents.add(document(COC, DocumentStatus.REJECTED, null));

        HandoverReadinessResponse result = evaluate(HANDOVER, false);

        assertThat(result.ready()).isFalse();
        assertThat(result.missingDocumentTypes()).containsExactly(COC);
        assertThat(result.problems()).isEmpty();
    }

    @Test
    void pendingCocIsNotVerified() {
        replace(COC, DocumentStatus.PENDING, null);

        HandoverReadinessResponse result = evaluate(HANDOVER, false);

        assertThat(result.ready()).isFalse();
        assertThat(result.missingDocumentTypes()).isEmpty();
        assertThat(result.problems()).containsExactly(new ReadinessProblem(COC, NOT_VERIFIED));
    }

    @Test
    void policyExpiringBeforeHandoverDateIsExpired() {
        replace(OC_POLICY, DocumentStatus.VERIFIED, HANDOVER.minusDays(1));

        HandoverReadinessResponse result = evaluate(HANDOVER, false);

        assertThat(result.ready()).isFalse();
        assertThat(result.missingDocumentTypes()).isEmpty();
        assertThat(result.problems()).containsExactly(new ReadinessProblem(OC_POLICY, EXPIRED));
    }

    @Test
    void policyExpiringOnHandoverDateIsStillValid() {
        replace(OC_POLICY, DocumentStatus.VERIFIED, HANDOVER);

        assertReady(evaluate(HANDOVER, false));
    }

    @Test
    void policyWithoutExpiryDatePasses() {
        replace(OC_POLICY, DocumentStatus.VERIFIED, null);

        assertReady(evaluate(HANDOVER, false));
    }

    @Test
    void pendingExpiredPolicyHasBothProblemsInOrder() {
        replace(OC_POLICY, DocumentStatus.PENDING, HANDOVER.minusDays(1));

        HandoverReadinessResponse result = evaluate(HANDOVER, false);

        assertThat(result.ready()).isFalse();
        assertThat(result.problems()).containsExactly(
                new ReadinessProblem(OC_POLICY, NOT_VERIFIED),
                new ReadinessProblem(OC_POLICY, EXPIRED));
    }

    @Test
    void noHandoverDateWithValidPolicyHasOnlyThatProblem() {
        replace(OC_POLICY, DocumentStatus.VERIFIED, TODAY);

        HandoverReadinessResponse result = evaluate(null, false);

        assertThat(result.ready()).isFalse();
        assertThat(result.missingDocumentTypes()).isEmpty();
        assertThat(result.problems()).containsExactly(new ReadinessProblem(null, NO_HANDOVER_DATE));
    }

    @Test
    void noHandoverDateChecksExpiryAgainstTodayAndReportsMissingTypes() {
        replace(OC_POLICY, DocumentStatus.VERIFIED, TODAY.minusDays(1));
        documents.removeIf(document -> document.getType() == COC);

        HandoverReadinessResponse result = evaluate(null, false);

        assertThat(result.ready()).isFalse();
        assertThat(result.missingDocumentTypes()).containsExactly(COC);
        assertThat(result.problems()).containsExactly(
                new ReadinessProblem(OC_POLICY, EXPIRED),
                new ReadinessProblem(null, NO_HANDOVER_DATE));
    }

    @Test
    void arraysFollowTheDocumentTypeOrder() {
        documents.clear();
        documents.add(document(REGISTRATION_POA, DocumentStatus.PENDING, null));
        documents.add(document(INVOICE, DocumentStatus.PENDING, null));
        documents.add(document(SALES_AGREEMENT, DocumentStatus.PENDING, TODAY.minusDays(1)));

        HandoverReadinessResponse result = evaluate(null, true);

        assertThat(result.missingDocumentTypes()).containsExactly(COC, OC_POLICY);
        assertThat(result.problems()).containsExactly(
                new ReadinessProblem(SALES_AGREEMENT, NOT_VERIFIED),
                new ReadinessProblem(SALES_AGREEMENT, EXPIRED),
                new ReadinessProblem(INVOICE, NOT_VERIFIED),
                new ReadinessProblem(REGISTRATION_POA, NOT_VERIFIED),
                new ReadinessProblem(null, NO_HANDOVER_DATE));
    }

    @Test
    void rejectedExpiredDocumentDoesNotHideTheActiveOne() {
        documents.add(0, document(OC_POLICY, DocumentStatus.REJECTED, TODAY.minusYears(1)));

        assertReady(evaluate(HANDOVER, false));
    }

    private HandoverReadinessResponse evaluate(LocalDate handoverDate, boolean dealershipRegisters) {
        return HandoverReadiness.evaluate(handoverDate, dealershipRegisters, documents, TODAY);
    }

    private void replace(DocumentType type, DocumentStatus status, LocalDate expiryDate) {
        documents.removeIf(document -> document.getType() == type);
        documents.add(document(type, status, expiryDate));
    }

    private static void assertReady(HandoverReadinessResponse result) {
        assertThat(result.ready()).isTrue();
        assertThat(result.missingDocumentTypes()).isEmpty();
        assertThat(result.problems()).isEmpty();
    }

    private static VehicleDocument document(DocumentType type, DocumentStatus status, LocalDate expiryDate) {
        VehicleDocument document = new VehicleDocument(null, type, "N-1", TODAY.minusMonths(1), expiryDate);
        if (status == DocumentStatus.VERIFIED || status == DocumentStatus.REJECTED) {
            document.setStatus(status);
        }
        return document;
    }
}
