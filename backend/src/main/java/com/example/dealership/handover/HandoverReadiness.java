package com.example.dealership.handover;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.example.dealership.document.DocumentStatus;
import com.example.dealership.document.DocumentType;
import com.example.dealership.document.VehicleDocument;

/** The handover readiness rules (INTENT.md R-01 to R-05), without persistence. */
public final class HandoverReadiness {

    private static final List<DocumentType> ALWAYS_REQUIRED = List.of(
            DocumentType.SALES_AGREEMENT, DocumentType.INVOICE, DocumentType.COC, DocumentType.OC_POLICY);

    private HandoverReadiness() {
    }

    /**
     * @param plannedHandoverDate the planned handover date, or null if none is set
     * @param dealershipRegisters true if the dealership registers the vehicle, which requires a power of attorney
     * @param documents all documents of the vehicle, including rejected ones
     * @param today the current dealership date, used for the expiry check when there is no handover date
     */
    public static HandoverReadinessResponse evaluate(LocalDate plannedHandoverDate, boolean dealershipRegisters,
            List<VehicleDocument> documents, LocalDate today) {
        LocalDate referenceDate = plannedHandoverDate != null ? plannedHandoverDate : today;
        List<DocumentType> missing = new ArrayList<>();
        List<ReadinessProblem> problems = new ArrayList<>();

        for (DocumentType type : requiredTypes(dealershipRegisters)) {
            Optional<VehicleDocument> active = activeDocument(documents, type);
            if (active.isEmpty()) {
                missing.add(type);
                continue;
            }
            VehicleDocument document = active.get();
            if (document.getStatus() == DocumentStatus.PENDING) {
                problems.add(new ReadinessProblem(type, ReadinessReason.NOT_VERIFIED));
            }
            // The document must be valid on the reference date; expiring on that day still passes.
            if (document.getExpiryDate() != null && document.getExpiryDate().isBefore(referenceDate)) {
                problems.add(new ReadinessProblem(type, ReadinessReason.EXPIRED));
            }
        }
        if (plannedHandoverDate == null) {
            problems.add(new ReadinessProblem(null, ReadinessReason.NO_HANDOVER_DATE));
        }
        return new HandoverReadinessResponse(missing.isEmpty() && problems.isEmpty(), List.copyOf(missing),
                List.copyOf(problems));
    }

    static List<DocumentType> requiredTypes(boolean dealershipRegisters) {
        if (!dealershipRegisters) {
            return ALWAYS_REQUIRED;
        }
        List<DocumentType> types = new ArrayList<>(ALWAYS_REQUIRED);
        types.add(DocumentType.REGISTRATION_POA);
        return List.copyOf(types);
    }

    private static Optional<VehicleDocument> activeDocument(List<VehicleDocument> documents, DocumentType type) {
        return documents.stream()
                .filter(document -> document.getType() == type && document.getStatus().isActive())
                .findFirst();
    }
}
