package com.example.dealership.document;

public enum DocumentStatus {
    PENDING,
    VERIFIED,
    REJECTED;

    /** A rejected document is history: it does not count as the document of its type. */
    public boolean isActive() {
        return this != REJECTED;
    }

    /**
     * Allowed changes: PENDING to VERIFIED or REJECTED, and VERIFIED to PENDING or REJECTED. A rejected document stays
     * rejected; a correction is a new document. Sending the current status again is not a change.
     */
    public boolean canChangeTo(DocumentStatus target) {
        return switch (this) {
            case PENDING -> target == VERIFIED || target == REJECTED;
            case VERIFIED -> target == PENDING || target == REJECTED;
            case REJECTED -> false;
        };
    }
}
