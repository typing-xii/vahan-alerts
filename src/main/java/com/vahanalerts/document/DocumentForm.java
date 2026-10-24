package com.vahanalerts.document;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class DocumentForm {

    @NotNull(message = "Choose a document type.")
    private DocumentType type;

    @Size(max = 100, message = "Document number must be 100 characters or fewer.")
    private String documentNumber;

    private LocalDate issueDate;

    private LocalDate expiryDate;

    @Size(max = 1000, message = "Notes must be 1000 characters or fewer.")
    private String notes;

    private boolean removeAttachment;

    public static DocumentForm from(VehicleDocument document) {
        DocumentForm form = new DocumentForm();
        form.type = document.getType();
        form.documentNumber = document.getDocumentNumber();
        form.issueDate = document.getIssueDate();
        form.expiryDate = document.getExpiryDate();
        form.notes = document.getNotes();
        return form;
    }

    public DocumentType getType() {
        return type;
    }

    public void setType(DocumentType type) {
        this.type = type;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isRemoveAttachment() {
        return removeAttachment;
    }

    public void setRemoveAttachment(boolean removeAttachment) {
        this.removeAttachment = removeAttachment;
    }
}
