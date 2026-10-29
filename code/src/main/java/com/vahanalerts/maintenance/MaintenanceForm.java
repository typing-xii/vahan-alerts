package com.vahanalerts.maintenance;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public class MaintenanceForm {

    @NotNull(message = "Enter the service date.")
    private LocalDate serviceDate;

    @NotNull(message = "Enter the odometer reading.")
    @PositiveOrZero(message = "Odometer reading cannot be negative.")
    private Long odometerKm;

    @NotNull(message = "Choose a service type.")
    private ServiceType serviceType;

    @Size(max = 150, message = "Workshop name must be 150 characters or fewer.")
    private String workshopName;

    @NotNull(message = "Enter the service cost.")
    @PositiveOrZero(message = "Service cost cannot be negative.")
    @Digits(integer = 10, fraction = 2, message = "Service cost can have up to 10 whole digits and 2 decimal places.")
    private BigDecimal serviceCostInr;

    @Size(max = 2000, message = "Description must be 2000 characters or fewer.")
    private String description;

    public static MaintenanceForm from(MaintenanceRecord record) {
        MaintenanceForm form = new MaintenanceForm();
        form.serviceDate = record.getServiceDate();
        form.odometerKm = record.getOdometerKm();
        form.serviceType = record.getServiceType();
        form.workshopName = record.getWorkshopName();
        form.serviceCostInr = record.getServiceCostInr();
        form.description = record.getDescription();
        return form;
    }

    public LocalDate getServiceDate() {
        return serviceDate;
    }

    public void setServiceDate(LocalDate serviceDate) {
        this.serviceDate = serviceDate;
    }

    public Long getOdometerKm() {
        return odometerKm;
    }

    public void setOdometerKm(Long odometerKm) {
        this.odometerKm = odometerKm;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }

    public String getWorkshopName() {
        return workshopName;
    }

    public void setWorkshopName(String workshopName) {
        this.workshopName = workshopName;
    }

    public BigDecimal getServiceCostInr() {
        return serviceCostInr;
    }

    public void setServiceCostInr(BigDecimal serviceCostInr) {
        this.serviceCostInr = serviceCostInr;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
