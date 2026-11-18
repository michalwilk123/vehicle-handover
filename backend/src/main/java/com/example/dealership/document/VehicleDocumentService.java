package com.example.dealership.document;

import java.util.List;

import com.example.dealership.error.ConflictException;
import com.example.dealership.error.NotFoundException;
import com.example.dealership.vehicle.Vehicle;
import com.example.dealership.vehicle.VehicleRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class VehicleDocumentService {

    private final VehicleRepository vehicleRepository;
    private final VehicleDocumentRepository documentRepository;

    public VehicleDocumentService(VehicleRepository vehicleRepository, VehicleDocumentRepository documentRepository) {
        this.vehicleRepository = vehicleRepository;
        this.documentRepository = documentRepository;
    }

    public List<VehicleDocumentResponse> findAll(Long vehicleId) {
        if (!vehicleRepository.existsById(vehicleId)) {
            throw vehicleNotFound(vehicleId);
        }
        return documentRepository.findByVehicleIdOrderByIdAsc(vehicleId).stream()
                .map(VehicleDocumentMapper::toResponse)
                .toList();
    }

    @Transactional
    public VehicleDocumentResponse create(Long vehicleId, VehicleDocumentRequest request) {
        Vehicle vehicle = lockVehicle(vehicleId);
        if (documentRepository.existsByVehicleIdAndActiveType(vehicleId, request.type())) {
            throw new ConflictException("Vehicle " + vehicleId + " already has an active " + request.type()
                    + " document. Reject it before adding a new one");
        }
        // The unique constraint on active documents backs this check; a violation becomes 409.
        VehicleDocument document = documentRepository.saveAndFlush(VehicleDocumentMapper.toEntity(vehicle, request));
        return VehicleDocumentMapper.toResponse(document);
    }

    @Transactional
    public VehicleDocumentResponse changeStatus(Long vehicleId, Long documentId, DocumentStatusRequest request) {
        lockVehicle(vehicleId);
        VehicleDocument document = getDocument(vehicleId, documentId);
        if (!document.getStatus().canChangeTo(request.status())) {
            throw new ConflictException("Document status cannot change from " + document.getStatus()
                    + " to " + request.status());
        }
        document.setStatus(request.status());
        return VehicleDocumentMapper.toResponse(documentRepository.saveAndFlush(document));
    }

    private Vehicle lockVehicle(Long vehicleId) {
        return vehicleRepository.findByIdForUpdate(vehicleId).orElseThrow(() -> vehicleNotFound(vehicleId));
    }

    private static NotFoundException vehicleNotFound(Long vehicleId) {
        return new NotFoundException("Vehicle with id " + vehicleId + " not found");
    }

    private VehicleDocument getDocument(Long vehicleId, Long documentId) {
        return documentRepository.findByIdAndVehicleId(documentId, vehicleId)
                .orElseThrow(() -> new NotFoundException(
                        "Document with id " + documentId + " not found for vehicle " + vehicleId));
    }
}
