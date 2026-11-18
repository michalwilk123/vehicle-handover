package com.example.dealership.document;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleDocumentRepository extends JpaRepository<VehicleDocument, Long> {

    List<VehicleDocument> findByVehicleIdOrderByIdAsc(Long vehicleId);

    Optional<VehicleDocument> findByIdAndVehicleId(Long id, Long vehicleId);

    boolean existsByVehicleIdAndActiveType(Long vehicleId, DocumentType activeType);
}
