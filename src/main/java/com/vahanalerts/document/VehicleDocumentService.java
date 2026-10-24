package com.vahanalerts.document;

import java.util.List;

import com.vahanalerts.vehicle.Vehicle;
import com.vahanalerts.vehicle.VehicleService;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class VehicleDocumentService {

    private final VehicleService vehicleService;
    private final VehicleDocumentRepository documentRepository;
    private final DocumentAttachmentStorage attachmentStorage;

    public VehicleDocumentService(
            VehicleService vehicleService,
            VehicleDocumentRepository documentRepository,
            DocumentAttachmentStorage attachmentStorage) {
        this.vehicleService = vehicleService;
        this.documentRepository = documentRepository;
        this.attachmentStorage = attachmentStorage;
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
            Long vehicleId,
            Long ownerId,
            DocumentForm form,
            MultipartFile attachment) {
        Vehicle vehicle = vehicleService.findForOwner(vehicleId, ownerId);
        VehicleDocument document = new VehicleDocument(vehicle, form);
        if (hasUpload(attachment)) {
            StoredDocumentAttachment stored = attachmentStorage.store(attachment);
            document.setAttachment(stored.key(), stored.mediaType(), stored.size());
            deleteAfterRollback(stored.key());
        }
        return documentRepository.saveAndFlush(document);
    }

    @Transactional
    public VehicleDocument update(
            Long vehicleId,
            Long documentId,
            Long ownerId,
            DocumentForm form,
            MultipartFile attachment) {
        VehicleDocument document = findForOwner(vehicleId, documentId, ownerId);
        String previousKey = document.getAttachmentKey();
        String replacementKey = null;
        if (hasUpload(attachment)) {
            StoredDocumentAttachment stored = attachmentStorage.store(attachment);
            replacementKey = stored.key();
            document.setAttachment(stored.key(), stored.mediaType(), stored.size());
            deleteAfterRollback(stored.key());
        } else if (form.isRemoveAttachment() && previousKey != null) {
            document.removeAttachment();
        }
        document.updateFrom(form);
        VehicleDocument saved = documentRepository.saveAndFlush(document);
        if (previousKey != null && !previousKey.equals(replacementKey)
                && (replacementKey != null || form.isRemoveAttachment())) {
            deleteAfterCommit(previousKey);
        }
        return saved;
    }

    @Transactional
    public void delete(Long vehicleId, Long documentId, Long ownerId) {
        VehicleDocument document = findForOwner(vehicleId, documentId, ownerId);
        String attachmentKey = document.getAttachmentKey();
        documentRepository.delete(document);
        documentRepository.flush();
        if (attachmentKey != null) {
            deleteAfterCommit(attachmentKey);
        }
    }

    @Transactional(readOnly = true)
    public Resource loadAttachment(Long vehicleId, Long documentId, Long ownerId) {
        VehicleDocument document = findForOwner(vehicleId, documentId, ownerId);
        if (document.getAttachmentKey() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return attachmentStorage.load(document.getAttachmentKey());
    }

    private boolean hasUpload(MultipartFile attachment) {
        return attachment != null && !attachment.isEmpty();
    }

    private void deleteAfterRollback(String key) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    attachmentStorage.delete(key);
                }
            }
        });
    }

    private void deleteAfterCommit(String key) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                attachmentStorage.delete(key);
            }
        });
    }
}
