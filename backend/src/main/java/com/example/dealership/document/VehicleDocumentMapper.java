package com.example.dealership.document;

import com.example.dealership.vehicle.Vehicle;

public final class VehicleDocumentMapper {

    private VehicleDocumentMapper() {
    }

    public static VehicleDocument toEntity(Vehicle vehicle, VehicleDocumentRequest request) {
        return new VehicleDocument(
                vehicle,
                request.type(),
                request.number(),
                request.issueDate(),
                request.expiryDate());
    }

    public static VehicleDocumentResponse toResponse(VehicleDocument document) {
        return new VehicleDocumentResponse(
                document.getId(),
                document.getType(),
                document.getNumber(),
                document.getIssueDate(),
                document.getExpiryDate(),
                document.getStatus());
    }
}
