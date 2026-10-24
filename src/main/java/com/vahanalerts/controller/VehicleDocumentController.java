package com.vahanalerts.controller;

import java.util.List;

import com.vahanalerts.document.DocumentAttachmentStorage;
import com.vahanalerts.document.DocumentExpiryStatusCalculator;
import com.vahanalerts.document.DocumentForm;
import com.vahanalerts.document.DocumentListItem;
import com.vahanalerts.document.DocumentType;
import com.vahanalerts.document.InvalidDocumentAttachmentException;
import com.vahanalerts.document.VehicleDocument;
import com.vahanalerts.document.VehicleDocumentService;
import com.vahanalerts.user.VahanUserDetails;
import com.vahanalerts.vehicle.Vehicle;
import com.vahanalerts.vehicle.VehicleService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class VehicleDocumentController {

    private final VehicleService vehicleService;
    private final VehicleDocumentService documentService;
    private final DocumentExpiryStatusCalculator statusCalculator;

    public VehicleDocumentController(
            VehicleService vehicleService,
            VehicleDocumentService documentService,
            DocumentExpiryStatusCalculator statusCalculator) {
        this.vehicleService = vehicleService;
        this.documentService = documentService;
        this.statusCalculator = statusCalculator;
    }

    @GetMapping("/vehicles/{vehicleId}/documents")
    public String list(
            @PathVariable Long vehicleId,
            @AuthenticationPrincipal VahanUserDetails user,
            Model model) {
        Vehicle vehicle = vehicleService.findForOwner(vehicleId, user.getId());
        List<DocumentListItem> documents = documentService.findAllForOwner(vehicleId, user.getId())
                .stream()
                .map(document -> new DocumentListItem(
                        document, statusCalculator.calculate(document.getExpiryDate())))
                .toList();
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("documents", documents);
        model.addAttribute("currentUser", user);
        return "documents/list";
    }

    @GetMapping("/vehicles/{vehicleId}/documents/new")
    public String createForm(
            @PathVariable Long vehicleId,
            @AuthenticationPrincipal VahanUserDetails user,
            Model model) {
        prepareForm(vehicleId, user, model, new DocumentForm(), null);
        return "documents/form";
    }

    @PostMapping("/vehicles/{vehicleId}/documents")
    public String create(
            @PathVariable Long vehicleId,
            @AuthenticationPrincipal VahanUserDetails user,
            @Valid @ModelAttribute("documentForm") DocumentForm form,
            BindingResult bindingResult,
            @RequestParam(name = "attachment", required = false) MultipartFile attachment,
            Model model,
            RedirectAttributes redirectAttributes) {
        validateDates(form, bindingResult);
        if (bindingResult.hasErrors()) {
            prepareForm(vehicleId, user, model, form, null);
            return "documents/form";
        }
        try {
            VehicleDocument document = documentService.create(vehicleId, user.getId(), form, attachment);
            redirectAttributes.addFlashAttribute("successMessage", "Document added successfully.");
            return "redirect:/vehicles/" + vehicleId + "/documents/" + document.getId();
        } catch (InvalidDocumentAttachmentException exception) {
            bindingResult.reject("attachment.invalid", exception.getMessage());
            prepareForm(vehicleId, user, model, form, null);
            return "documents/form";
        }
    }

    @GetMapping("/vehicles/{vehicleId}/documents/{documentId}")
    public String detail(
            @PathVariable Long vehicleId,
            @PathVariable Long documentId,
            @AuthenticationPrincipal VahanUserDetails user,
            Model model) {
        Vehicle vehicle = vehicleService.findForOwner(vehicleId, user.getId());
        VehicleDocument document = documentService.findForOwner(vehicleId, documentId, user.getId());
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("document", document);
        model.addAttribute("expiryStatus", statusCalculator.calculate(document.getExpiryDate()));
        model.addAttribute("currentUser", user);
        return "documents/detail";
    }

    @GetMapping("/vehicles/{vehicleId}/documents/{documentId}/edit")
    public String editForm(
            @PathVariable Long vehicleId,
            @PathVariable Long documentId,
            @AuthenticationPrincipal VahanUserDetails user,
            Model model) {
        VehicleDocument document = documentService.findForOwner(vehicleId, documentId, user.getId());
        prepareForm(vehicleId, user, model, DocumentForm.from(document), document);
        return "documents/form";
    }

    @PostMapping("/vehicles/{vehicleId}/documents/{documentId}")
    public String update(
            @PathVariable Long vehicleId,
            @PathVariable Long documentId,
            @AuthenticationPrincipal VahanUserDetails user,
            @Valid @ModelAttribute("documentForm") DocumentForm form,
            BindingResult bindingResult,
            @RequestParam(name = "attachment", required = false) MultipartFile attachment,
            Model model,
            RedirectAttributes redirectAttributes) {
        VehicleDocument document = documentService.findForOwner(vehicleId, documentId, user.getId());
        validateDates(form, bindingResult);
        if (bindingResult.hasErrors()) {
            prepareForm(vehicleId, user, model, form, document);
            return "documents/form";
        }
        try {
            documentService.update(vehicleId, documentId, user.getId(), form, attachment);
            redirectAttributes.addFlashAttribute("successMessage", "Document updated successfully.");
            return "redirect:/vehicles/" + vehicleId + "/documents/" + documentId;
        } catch (InvalidDocumentAttachmentException exception) {
            bindingResult.reject("attachment.invalid", exception.getMessage());
            prepareForm(vehicleId, user, model, form, document);
            return "documents/form";
        }
    }

    @GetMapping("/vehicles/{vehicleId}/documents/{documentId}/delete")
    public String confirmDelete(
            @PathVariable Long vehicleId,
            @PathVariable Long documentId,
            @AuthenticationPrincipal VahanUserDetails user,
            Model model) {
        model.addAttribute("vehicle", vehicleService.findForOwner(vehicleId, user.getId()));
        model.addAttribute("document",
                documentService.findForOwner(vehicleId, documentId, user.getId()));
        model.addAttribute("currentUser", user);
        return "documents/delete";
    }

    @PostMapping("/vehicles/{vehicleId}/documents/{documentId}/delete")
    public String delete(
            @PathVariable Long vehicleId,
            @PathVariable Long documentId,
            @AuthenticationPrincipal VahanUserDetails user,
            RedirectAttributes redirectAttributes) {
        documentService.delete(vehicleId, documentId, user.getId());
        redirectAttributes.addFlashAttribute("successMessage", "Document deleted.");
        return "redirect:/vehicles/" + vehicleId + "/documents";
    }

    @GetMapping("/vehicles/{vehicleId}/documents/{documentId}/attachment")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable Long vehicleId,
            @PathVariable Long documentId,
            @AuthenticationPrincipal VahanUserDetails user) {
        VehicleDocument document = documentService.findForOwner(vehicleId, documentId, user.getId());
        Resource attachment = documentService.loadAttachment(vehicleId, documentId, user.getId());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getAttachmentMediaType()))
                .contentLength(document.getAttachmentSize())
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("vehicle-document").build().toString())
                .body(attachment);
    }

    private void prepareForm(
            Long vehicleId,
            VahanUserDetails user,
            Model model,
            DocumentForm form,
            VehicleDocument document) {
        model.addAttribute("vehicle", vehicleService.findForOwner(vehicleId, user.getId()));
        model.addAttribute("document", document);
        model.addAttribute("documentForm", form);
        model.addAttribute("documentTypes", List.of(DocumentType.values()));
        model.addAttribute("formAction", document == null
                ? "/vehicles/" + vehicleId + "/documents"
                : "/vehicles/" + vehicleId + "/documents/" + document.getId());
        model.addAttribute("currentUser", user);
    }

    private void validateDates(DocumentForm form, BindingResult bindingResult) {
        if (form.getIssueDate() != null && form.getIssueDate().isAfter(statusCalculator.today())) {
            bindingResult.rejectValue("issueDate", "document.issue.future",
                    "Issue date cannot be in the future.");
        }
        if (form.getType() != null && form.getType().isExpiryRequired() && form.getExpiryDate() == null) {
            bindingResult.rejectValue("expiryDate", "document.expiry.required",
                    "An expiry date is required for this document type.");
        }
        if (form.getIssueDate() != null && form.getExpiryDate() != null
                && form.getExpiryDate().isBefore(form.getIssueDate())) {
            bindingResult.rejectValue("expiryDate", "document.expiry.beforeIssue",
                    "Expiry date cannot be before the issue date.");
        }
    }
}
