package com.vahanalerts.vehicle;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findAllByOwner_IdOrderByCreatedAtDesc(Long ownerId);

    Optional<Vehicle> findByIdAndOwner_Id(Long id, Long ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vehicle v where v.id = :vehicleId and v.owner.id = :ownerId")
    Optional<Vehicle> findByIdAndOwner_IdForUpdate(
            @Param("vehicleId") Long vehicleId,
            @Param("ownerId") Long ownerId);

    boolean existsByOwner_IdAndRegistrationNumber(Long ownerId, String registrationNumber);

    boolean existsByOwner_IdAndRegistrationNumberAndIdNot(
            Long ownerId,
            String registrationNumber,
            Long vehicleId);
}
