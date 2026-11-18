package com.example.dealership.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import com.example.dealership.vehicle.FuelType;
import com.example.dealership.vehicle.Vehicle;
import com.example.dealership.vehicle.VehicleRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class VehicleDocumentRepositoryTest {

    @Autowired
    private VehicleDocumentRepository documentRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void saveStoresDocumentThatCanBeReadBack() {
        Vehicle vehicle = vehicle("WF0XXXGCDX0000004");
        VehicleDocument saved = documentRepository.save(new VehicleDocument(vehicle, DocumentType.OC_POLICY,
                "OC/1", LocalDate.of(2026, 1, 10), LocalDate.of(2027, 1, 9)));
        entityManager.flush();
        entityManager.clear();

        VehicleDocument found = documentRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getVehicle().getId()).isEqualTo(vehicle.getId());
        assertThat(found.getType()).isEqualTo(DocumentType.OC_POLICY);
        assertThat(found.getNumber()).isEqualTo("OC/1");
        assertThat(found.getIssueDate()).isEqualTo(LocalDate.of(2026, 1, 10));
        assertThat(found.getExpiryDate()).isEqualTo(LocalDate.of(2027, 1, 9));
        assertThat(found.getStatus()).isEqualTo(DocumentStatus.PENDING);
    }

    @Test
    void lookupsAreScopedToTheVehicleAndSortedById() {
        Vehicle vehicle = vehicle("WF0XXXGCDX0000004");
        Vehicle other = vehicle("WF0XXXGCDX0000005");
        VehicleDocument first = documentRepository.save(document(vehicle, DocumentType.INVOICE));
        VehicleDocument second = documentRepository.save(document(vehicle, DocumentType.COC));
        VehicleDocument foreign = documentRepository.save(document(other, DocumentType.COC));
        entityManager.flush();

        assertThat(documentRepository.findByVehicleIdOrderByIdAsc(vehicle.getId()))
                .extracting(VehicleDocument::getId).containsExactly(first.getId(), second.getId());
        assertThat(documentRepository.findByIdAndVehicleId(foreign.getId(), vehicle.getId())).isEmpty();
        assertThat(documentRepository.findByIdAndVehicleId(foreign.getId(), other.getId())).isPresent();
    }

    @Test
    void deletingVehicleDeletesItsDocuments() {
        Vehicle vehicle = vehicle("WF0XXXGCDX0000004");
        documentRepository.save(document(vehicle, DocumentType.INVOICE));
        documentRepository.save(document(vehicle, DocumentType.COC));
        entityManager.flush();
        entityManager.clear();

        vehicleRepository.delete(vehicleRepository.findById(vehicle.getId()).orElseThrow());
        entityManager.flush();

        assertThat(documentRepository.findByVehicleIdOrderByIdAsc(vehicle.getId())).isEmpty();
    }

    @Test
    void databaseRejectsSecondActiveDocumentOfTheSameType() {
        Vehicle vehicle = vehicle("WF0XXXGCDX0000004");
        documentRepository.saveAndFlush(document(vehicle, DocumentType.COC));

        assertThatThrownBy(() -> documentRepository.saveAndFlush(document(vehicle, DocumentType.COC)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseAllowsNewActiveDocumentNextToRejectedOnes() {
        Vehicle vehicle = vehicle("WF0XXXGCDX0000004");
        Vehicle other = vehicle("WF0XXXGCDX0000005");
        for (int i = 0; i < 2; i++) {
            VehicleDocument rejected = document(vehicle, DocumentType.COC);
            rejected.setStatus(DocumentStatus.REJECTED);
            documentRepository.saveAndFlush(rejected);
        }
        documentRepository.saveAndFlush(document(vehicle, DocumentType.COC));
        documentRepository.saveAndFlush(document(other, DocumentType.COC));

        assertThat(documentRepository.existsByVehicleIdAndActiveType(vehicle.getId(), DocumentType.COC)).isTrue();
        assertThat(documentRepository.existsByVehicleIdAndActiveType(vehicle.getId(), DocumentType.INVOICE)).isFalse();
        assertThat(documentRepository.findByVehicleIdOrderByIdAsc(vehicle.getId())).hasSize(3);
    }

    private Vehicle vehicle(String vin) {
        return vehicleRepository.save(new Vehicle(vin, "Ford", "Focus", 2026, FuelType.PETROL, null));
    }

    private static VehicleDocument document(Vehicle vehicle, DocumentType type) {
        return new VehicleDocument(vehicle, type, "N-1", LocalDate.of(2026, 1, 10), null);
    }
}
