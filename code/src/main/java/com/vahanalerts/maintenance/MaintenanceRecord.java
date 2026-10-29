package com.vahanalerts.maintenance;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.vahanalerts.vehicle.Vehicle;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "vehicle_maintenance_record")
public class MaintenanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;

    @Column(name = "odometer_km", nullable = false)
    private Long odometerKm;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, length = 32)
    private ServiceType serviceType;

    @Column(name = "workshop_name", length = 150)
    private String workshopName;

    @Column(name = "service_cost_inr", nullable = false, precision = 12, scale = 2)
    private BigDecimal serviceCostInr;

    @Column(length = 2000)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MaintenanceRecord() {
    }

    public MaintenanceRecord(Vehicle vehicle, MaintenanceForm form) {
        this.vehicle = vehicle;
        updateFrom(form);
    }

    public void updateFrom(MaintenanceForm form) {
        serviceDate = form.getServiceDate();
        odometerKm = form.getOdometerKm();
        serviceType = form.getServiceType();
        workshopName = trimToNull(form.getWorkshopName());
        serviceCostInr = form.getServiceCostInr();
        description = trimToNull(form.getDescription());
    }

    @PrePersist
    void initializeTimestamps() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void updateTimestamp() {
        updatedAt = Instant.now();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public Long getId() {
        return id;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public LocalDate getServiceDate() {
        return serviceDate;
    }

    public Long getOdometerKm() {
        return odometerKm;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public String getWorkshopName() {
        return workshopName;
    }

    public BigDecimal getServiceCostInr() {
        return serviceCostInr;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
