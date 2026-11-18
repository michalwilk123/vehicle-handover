package com.example.dealership.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.example.dealership.error.ConflictException;
import com.example.dealership.error.NotFoundException;
import com.example.dealership.vehicle.FuelType;
import com.example.dealership.vehicle.Vehicle;
import com.example.dealership.vehicle.VehicleRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VehicleDocumentServiceTest {

    private static final Vehicle VEHICLE =
            new Vehicle("WVWZZZ1KZAW000001", "Volkswagen", "Golf", 2025, FuelType.PETROL, null);

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private VehicleDocumentRepository documentRepository;

    @InjectMocks
    private VehicleDocumentService documentService;

    @Test
    void findAllReturnsDocumentsOfExistingVehicle() {
        when(vehicleRepository.existsById(1L)).thenReturn(true);
        when(documentRepository.findByVehicleIdOrderByIdAsc(1L))
                .thenReturn(List.of(document(DocumentType.INVOICE), document(DocumentType.COC)));

        assertThat(documentService.findAll(1L))
                .extracting(VehicleDocumentResponse::type)
                .containsExactly(DocumentType.INVOICE, DocumentType.COC);
    }

    @Test
    void findAllThrowsNotFoundWhenVehicleDoesNotExist() {
        when(vehicleRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> documentService.findAll(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Vehicle with id 99 not found");
    }

    @Test
    void createSavesPendingDocument() {
        when(vehicleRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(VEHICLE));
        when(documentRepository.existsByVehicleIdAndActiveType(1L, DocumentType.COC)).thenReturn(false);
        when(documentRepository.saveAndFlush(any(VehicleDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VehicleDocumentResponse response = documentService.create(1L, new VehicleDocumentRequest(
                DocumentType.COC, "COC-1", LocalDate.of(2026, 1, 10), null));

        assertThat(response.type()).isEqualTo(DocumentType.COC);
        assertThat(response.number()).isEqualTo("COC-1");
        assertThat(response.status()).isEqualTo(DocumentStatus.PENDING);
    }

    @Test
    void changeStatusThrowsNotFoundForDocumentOfAnotherVehicle() {
        when(vehicleRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(VEHICLE));
        when(documentRepository.findByIdAndVehicleId(5L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.changeStatus(2L, 5L, new DocumentStatusRequest(DocumentStatus.VERIFIED)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Document with id 5 not found for vehicle 2");
    }

    @Test
    void createThrowsConflictWhenTypeHasActiveDocument() {
        when(vehicleRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(VEHICLE));
        when(documentRepository.existsByVehicleIdAndActiveType(1L, DocumentType.COC)).thenReturn(true);

        assertThatThrownBy(() -> documentService.create(1L, cocRequest()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Vehicle 1 already has an active COC document. Reject it before adding a new one");
        verify(documentRepository, never()).saveAndFlush(any());
    }

    @Test
    void createThrowsNotFoundWhenVehicleDoesNotExist() {
        when(vehicleRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.create(99L, cocRequest()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Vehicle with id 99 not found");
    }

    @Test
    void changeStatusVerifiesPendingDocumentAndKeepsItActive() {
        VehicleDocument document = document(DocumentType.COC);
        when(vehicleRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(VEHICLE));
        when(documentRepository.findByIdAndVehicleId(5L, 1L)).thenReturn(Optional.of(document));
        when(documentRepository.saveAndFlush(document)).thenReturn(document);

        VehicleDocumentResponse response = documentService.changeStatus(1L, 5L, statusRequest(DocumentStatus.VERIFIED));

        assertThat(response.status()).isEqualTo(DocumentStatus.VERIFIED);
        assertThat(document.getActiveType()).isEqualTo(DocumentType.COC);
    }

    @Test
    void changeStatusRejectsVerifiedDocumentAndReleasesItsType() {
        VehicleDocument document = document(DocumentType.COC);
        document.setStatus(DocumentStatus.VERIFIED);
        when(vehicleRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(VEHICLE));
        when(documentRepository.findByIdAndVehicleId(5L, 1L)).thenReturn(Optional.of(document));
        when(documentRepository.saveAndFlush(document)).thenReturn(document);

        VehicleDocumentResponse response = documentService.changeStatus(1L, 5L, statusRequest(DocumentStatus.REJECTED));

        assertThat(response.status()).isEqualTo(DocumentStatus.REJECTED);
        assertThat(document.getActiveType()).isNull();
    }

    @Test
    void changeStatusThrowsConflictForRejectedDocument() {
        VehicleDocument document = document(DocumentType.COC);
        document.setStatus(DocumentStatus.REJECTED);
        when(vehicleRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(VEHICLE));
        when(documentRepository.findByIdAndVehicleId(5L, 1L)).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> documentService.changeStatus(1L, 5L, statusRequest(DocumentStatus.VERIFIED)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Document status cannot change from REJECTED to VERIFIED");
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.REJECTED);
        verify(documentRepository, never()).saveAndFlush(any());
    }

    @Test
    void changeStatusThrowsConflictForSameStatus() {
        when(vehicleRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(VEHICLE));
        when(documentRepository.findByIdAndVehicleId(5L, 1L)).thenReturn(Optional.of(document(DocumentType.COC)));

        assertThatThrownBy(() -> documentService.changeStatus(1L, 5L, statusRequest(DocumentStatus.PENDING)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void changeStatusThrowsNotFoundWhenVehicleDoesNotExist() {
        when(vehicleRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.changeStatus(99L, 5L, statusRequest(DocumentStatus.VERIFIED)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Vehicle with id 99 not found");
    }

    private static VehicleDocumentRequest cocRequest() {
        return new VehicleDocumentRequest(DocumentType.COC, "COC-1", LocalDate.of(2026, 1, 10), null);
    }

    private static DocumentStatusRequest statusRequest(DocumentStatus status) {
        return new DocumentStatusRequest(status);
    }

    private static VehicleDocument document(DocumentType type) {
        return new VehicleDocument(VEHICLE, type, "N-1", LocalDate.of(2026, 1, 10), null);
    }
}
