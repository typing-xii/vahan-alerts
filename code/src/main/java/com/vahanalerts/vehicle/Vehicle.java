package com.vahanalerts.vehicle;

import java.time.Instant;

import com.vahanalerts.user.UserAccount;
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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "vehicle", uniqueConstraints = {
        @UniqueConstraint(name = "uk_vehicle_owner_registration", columnNames = {"owner_id", "registration_number"})
})
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserAccount owner;

    @Column(length = 100)
    private String nickname;

    @Column(name = "registration_number", nullable = false, length = 20)
    private String registrationNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 20)
    private VehicleType type;

    @Column(length = 100)
    private String make;

    @Column(length = 100)
    private String model;

    @Column(name = "manufacturing_year")
    private Integer manufacturingYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", length = 20)
    private FuelType fuelType;

    @Column(length = 1000)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Vehicle() {
    }

    public Vehicle(UserAccount owner, VehicleForm form, String registrationNumber) {
        this.owner = owner;
        updateFrom(form, registrationNumber);
    }

    public void updateFrom(VehicleForm form, String registrationNumber) {
        this.nickname = trimToNull(form.getNickname());
        this.registrationNumber = registrationNumber;
        this.type = form.getType();
        this.make = trimToNull(form.getMake());
        this.model = trimToNull(form.getModel());
        this.manufacturingYear = form.getManufacturingYear();
        this.fuelType = form.getFuelType();
        this.notes = trimToNull(form.getNotes());
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

    public UserAccount getOwner() {
        return owner;
    }

    public String getNickname() {
        return nickname;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public VehicleType getType() {
        return type;
    }

    public String getMake() {
        return make;
    }

    public String getModel() {
        return model;
    }

    public Integer getManufacturingYear() {
        return manufacturingYear;
    }

    public FuelType getFuelType() {
        return fuelType;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
