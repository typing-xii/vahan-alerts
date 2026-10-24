package com.vahanalerts.vehicle;

import java.util.List;
import java.util.Locale;

import com.vahanalerts.user.UserAccount;
import com.vahanalerts.user.UserAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final UserAccountRepository userAccountRepository;

    public VehicleService(VehicleRepository vehicleRepository, UserAccountRepository userAccountRepository) {
        this.vehicleRepository = vehicleRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional(readOnly = true)
    public List<Vehicle> findAllForOwner(Long ownerId) {
        return vehicleRepository.findAllByOwner_IdOrderByCreatedAtDesc(ownerId);
    }

    @Transactional(readOnly = true)
    public Vehicle findForOwner(Long vehicleId, Long ownerId) {
        return vehicleRepository.findByIdAndOwner_Id(vehicleId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional
    public Vehicle create(Long ownerId, VehicleForm form) {
        UserAccount owner = userAccountRepository.findById(ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String normalizedRegistration = normalizeRegistration(form.getRegistrationNumber());
        if (vehicleRepository.existsByOwner_IdAndRegistrationNumber(ownerId, normalizedRegistration)) {
            throw new DuplicateVehicleRegistrationException();
        }

        try {
            return vehicleRepository.saveAndFlush(new Vehicle(owner, form, normalizedRegistration));
        } catch (DataIntegrityViolationException exception) {
            if (vehicleRepository.existsByOwner_IdAndRegistrationNumber(ownerId, normalizedRegistration)) {
                throw new DuplicateVehicleRegistrationException();
            }
            throw exception;
        }
    }

    @Transactional
    public void update(Long vehicleId, Long ownerId, VehicleForm form) {
        Vehicle vehicle = findForOwner(vehicleId, ownerId);
        String normalizedRegistration = normalizeRegistration(form.getRegistrationNumber());
        if (vehicleRepository.existsByOwner_IdAndRegistrationNumberAndIdNot(
                ownerId, normalizedRegistration, vehicleId)) {
            throw new DuplicateVehicleRegistrationException();
        }

        vehicle.updateFrom(form, normalizedRegistration);
        try {
            vehicleRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            if (vehicleRepository.existsByOwner_IdAndRegistrationNumber(ownerId, normalizedRegistration)) {
                throw new DuplicateVehicleRegistrationException();
            }
            throw exception;
        }
    }

    @Transactional
    public void delete(Long vehicleId, Long ownerId) {
        Vehicle vehicle = findForOwner(vehicleId, ownerId);
        vehicleRepository.delete(vehicle);
    }

    private String normalizeRegistration(String registrationNumber) {
        return registrationNumber.trim().replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }
}
