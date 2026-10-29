package com.vahanalerts.maintenance;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long> {

    List<MaintenanceRecord> findAllByVehicle_IdOrderByServiceDateDescIdDesc(Long vehicleId);

    List<MaintenanceRecord> findAllByVehicle_IdOrderByServiceDateAscIdAsc(Long vehicleId);

    Optional<MaintenanceRecord> findByIdAndVehicle_Id(Long id, Long vehicleId);
}
