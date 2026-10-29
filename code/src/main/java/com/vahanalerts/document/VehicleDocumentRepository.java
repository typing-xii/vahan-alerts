package com.vahanalerts.document;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleDocumentRepository extends JpaRepository<VehicleDocument, Long> {

    List<VehicleDocument> findAllByVehicle_IdOrderByExpiryDateAsc(Long vehicleId);

    Optional<VehicleDocument> findByIdAndVehicle_Id(Long id, Long vehicleId);

    @EntityGraph(attributePaths = {"vehicle", "vehicle.owner"})
    List<VehicleDocument> findAllByExpiryDateBetween(LocalDate fromDate, LocalDate toDate);

}
