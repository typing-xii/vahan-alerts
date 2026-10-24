package com.vahanalerts.vehicle;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findAllByOwner_IdOrderByCreatedAtDesc(Long ownerId);

    Optional<Vehicle> findByIdAndOwner_Id(Long id, Long ownerId);

    boolean existsByOwner_IdAndRegistrationNumber(Long ownerId, String registrationNumber);

    boolean existsByOwner_IdAndRegistrationNumberAndIdNot(
            Long ownerId,
            String registrationNumber,
            Long vehicleId);
}
