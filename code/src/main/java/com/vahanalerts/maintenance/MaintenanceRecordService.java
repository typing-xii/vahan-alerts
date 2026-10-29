package com.vahanalerts.maintenance;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import com.vahanalerts.vehicle.Vehicle;
import com.vahanalerts.vehicle.VehicleService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MaintenanceRecordService {

    private final VehicleService vehicleService;
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final Clock clock;

    public MaintenanceRecordService(
            VehicleService vehicleService,
            MaintenanceRecordRepository maintenanceRecordRepository,
            Clock clock) {
        this.vehicleService = vehicleService;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<MaintenanceRecord> findAllForOwner(Long vehicleId, Long ownerId) {
        vehicleService.findForOwner(vehicleId, ownerId);
        return maintenanceRecordRepository.findAllByVehicle_IdOrderByServiceDateDescIdDesc(vehicleId);
    }

    @Transactional(readOnly = true)
    public MaintenanceRecord findForOwner(Long vehicleId, Long recordId, Long ownerId) {
        vehicleService.findForOwner(vehicleId, ownerId);
        return maintenanceRecordRepository.findByIdAndVehicle_Id(recordId, vehicleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public MaintenanceSummary summarizeForOwner(Long vehicleId, Long ownerId) {
        List<MaintenanceRecord> records = findAllForOwner(vehicleId, ownerId);
        BigDecimal total = records.stream()
                .map(MaintenanceRecord::getServiceCostInr)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDate mostRecentDate = records.isEmpty() ? null : records.get(0).getServiceDate();
        return new MaintenanceSummary(records.size(), total, mostRecentDate);
    }

    @Transactional
    public MaintenanceRecord create(Long vehicleId, Long ownerId, MaintenanceForm form) {
        Vehicle vehicle = vehicleService.findForOwnerForUpdate(vehicleId, ownerId);
        validateServiceDate(form.getServiceDate());
        validateOdometerSequence(vehicleId, null, form);
        return maintenanceRecordRepository.saveAndFlush(new MaintenanceRecord(vehicle, form));
    }

    @Transactional
    public MaintenanceRecord update(
            Long vehicleId,
            Long recordId,
            Long ownerId,
            MaintenanceForm form) {
        vehicleService.findForOwnerForUpdate(vehicleId, ownerId);
        MaintenanceRecord record = maintenanceRecordRepository.findByIdAndVehicle_Id(recordId, vehicleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        validateServiceDate(form.getServiceDate());
        validateOdometerSequence(vehicleId, record, form);
        record.updateFrom(form);
        return maintenanceRecordRepository.saveAndFlush(record);
    }

    @Transactional
    public void delete(Long vehicleId, Long recordId, Long ownerId) {
        vehicleService.findForOwnerForUpdate(vehicleId, ownerId);
        MaintenanceRecord record = maintenanceRecordRepository.findByIdAndVehicle_Id(recordId, vehicleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        maintenanceRecordRepository.delete(record);
    }

    private void validateServiceDate(LocalDate serviceDate) {
        if (serviceDate.isAfter(LocalDate.now(clock))) {
            throw new FutureServiceDateException();
        }
    }

    private void validateOdometerSequence(
            Long vehicleId,
            MaintenanceRecord editedRecord,
            MaintenanceForm form) {
        List<MaintenanceRecord> records =
                maintenanceRecordRepository.findAllByVehicle_IdOrderByServiceDateAscIdAsc(vehicleId);
        MaintenanceRecord previous = null;
        MaintenanceRecord next = null;
        Long editedRecordId = editedRecord == null ? null : editedRecord.getId();

        for (MaintenanceRecord existing : records) {
            if (editedRecordId != null && editedRecordId.equals(existing.getId())) {
                continue;
            }

            int dateComparison = existing.getServiceDate().compareTo(form.getServiceDate());
            boolean comesBefore = dateComparison < 0
                    || (dateComparison == 0
                    && (editedRecordId == null || existing.getId() < editedRecordId));
            if (comesBefore) {
                previous = existing;
            } else if (next == null) {
                next = existing;
            }
        }

        if (previous != null && form.getOdometerKm() < previous.getOdometerKm()) {
            throw new OdometerSequenceException();
        }
        if (next != null && form.getOdometerKm() > next.getOdometerKm()) {
            throw new OdometerSequenceException();
        }
    }
}
