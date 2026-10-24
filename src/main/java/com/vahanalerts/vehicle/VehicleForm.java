package com.vahanalerts.vehicle;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class VehicleForm {

    @Size(max = 100, message = "Nickname must be 100 characters or fewer.")
    private String nickname;

    @NotNull(message = "Enter a registration number.")
    @Size(min = 3, max = 20, message = "Registration number must be between 3 and 20 characters.")
    @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9 -]{1,18}[A-Za-z0-9]",
            message = "Use letters, numbers, spaces, or hyphens for the registration number.")
    private String registrationNumber;

    @NotNull(message = "Choose a vehicle type.")
    private VehicleType type;

    @Size(max = 100, message = "Make must be 100 characters or fewer.")
    private String make;

    @Size(max = 100, message = "Model must be 100 characters or fewer.")
    private String model;

    @Min(value = 1886, message = "Manufacturing year must be 1886 or later.")
    @Max(value = 2100, message = "Enter a valid manufacturing year.")
    private Integer manufacturingYear;

    private FuelType fuelType;

    @Size(max = 1000, message = "Notes must be 1000 characters or fewer.")
    private String notes;

    public static VehicleForm from(Vehicle vehicle) {
        VehicleForm form = new VehicleForm();
        form.nickname = vehicle.getNickname();
        form.registrationNumber = vehicle.getRegistrationNumber();
        form.type = vehicle.getType();
        form.make = vehicle.getMake();
        form.model = vehicle.getModel();
        form.manufacturingYear = vehicle.getManufacturingYear();
        form.fuelType = vehicle.getFuelType();
        form.notes = vehicle.getNotes();
        return form;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber == null ? null : registrationNumber.trim();
    }

    public VehicleType getType() {
        return type;
    }

    public void setType(VehicleType type) {
        this.type = type;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getManufacturingYear() {
        return manufacturingYear;
    }

    public void setManufacturingYear(Integer manufacturingYear) {
        this.manufacturingYear = manufacturingYear;
    }

    public FuelType getFuelType() {
        return fuelType;
    }

    public void setFuelType(FuelType fuelType) {
        this.fuelType = fuelType;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
