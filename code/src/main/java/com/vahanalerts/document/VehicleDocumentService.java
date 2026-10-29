package com.vahanalerts.document;

import java.util.List;

import com.vahanalerts.vehicle.Vehicle;
import com.vahanalerts.vehicle.VehicleService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VehicleDocumentService {

    private final VehicleService vehicleService;
    private final VehicleDocumentRepository documentRepository;

    public VehicleDocumentService(
            VehicleService vehicleService,
            VehicleDocumentRepository documentRepository) {
        this.vehicleService = vehicleService;
        this.documentRepository = documentRepository;
    }

    @Transactional(readOnly = true)
    public List<VehicleDocument> findAllForOwner(Long vehicleId, Long ownerId) {
        vehicleService.findForOwner(vehicleId, ownerId);
        return documentRepository.findAllByVehicle_IdOrderByExpiryDateAsc(vehicleId);
    }

    @Transactional(readOnly = true)
    public VehicleDocument findForOwner(Long vehicleId, Long documentId, Long ownerId) {
        vehicleService.findForOwner(vehicleId, ownerId);
        return documentRepository.findByIdAndVehicle_Id(documentId, vehicleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional
    public VehicleDocument create(
            Long selectedVehicleId,
            Long ownerId,
            DocumentForm form) {
        Vehicle vehicle = vehicleService.findForOwner(selectedVehicleId, ownerId);
        return documentRepository.saveAndFlush(new VehicleDocument(vehicle, form));
    }

    @Transactional
    public VehicleDocument update(
            Long vehicleId,
            Long documentId,
            Long ownerId,
            DocumentForm form) {
        VehicleDocument document = findForOwner(vehicleId, documentId, ownerId);
        document.updateFrom(form);
        return documentRepository.saveAndFlush(document);
    }

    @Transactional
    public void delete(Long vehicleId, Long documentId, Long ownerId) {
        VehicleDocument document = findForOwner(vehicleId, documentId, ownerId);
        documentRepository.delete(document);
    }
}
