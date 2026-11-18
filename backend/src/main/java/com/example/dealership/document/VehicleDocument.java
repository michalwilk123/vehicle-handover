package com.example.dealership.document;

import java.time.LocalDate;

import com.example.dealership.vehicle.Vehicle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "vehicle_document")
public class VehicleDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DocumentType type;

    @Column(nullable = false, length = 100)
    private String number;

    @Column(nullable = false)
    private LocalDate issueDate;

    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentStatus status;

    // Equals the type while the document is active and is null once it is rejected. The unique constraint on
    // (vehicle_id, active_type) allows one active document per type; nulls do not take part in it.
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private DocumentType activeType;

    protected VehicleDocument() {
    }

    public VehicleDocument(Vehicle vehicle, DocumentType type, String number, LocalDate issueDate,
            LocalDate expiryDate) {
        this.vehicle = vehicle;
        this.type = type;
        this.number = number;
        this.issueDate = issueDate;
        this.expiryDate = expiryDate;
        setStatus(DocumentStatus.PENDING);
    }

    public Long getId() {
        return id;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public DocumentType getType() {
        return type;
    }

    public String getNumber() {
        return number;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public void setStatus(DocumentStatus status) {
        this.status = status;
        this.activeType = status.isActive() ? type : null;
    }

    public DocumentType getActiveType() {
        return activeType;
    }
}
