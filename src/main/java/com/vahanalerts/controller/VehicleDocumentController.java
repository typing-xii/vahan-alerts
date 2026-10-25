package com.vahanalerts.controller;

import java.util.List;
import java.util.Objects;

import com.vahanalerts.document.DocumentExpiryStatusCalculator;
import com.vahanalerts.document.DocumentForm;
import com.vahanalerts.document.DocumentListItem;
import com.vahanalerts.document.DocumentType;
import com.vahanalerts.document.VehicleDocument;
import com.vahanalerts.document.VehicleDocumentService;
import com.vahanalerts.notification.DocumentExpiryReminderGenerator;
import com.vahanalerts.user.VahanUserDetails;
import com.vahanalerts.vehicle.Vehicle;
import com.vahanalerts.vehicle.VehicleService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class VehicleDocumentController {

    private final VehicleService vehicleService;
    private final VehicleDocumentService documentService;
    private final DocumentExpiryStatusCalculator statusCalculator;
    private final DocumentExpiryReminderGenerator reminderGenerator;

    public VehicleDocumentController(
            VehicleService vehicleService,
            VehicleDocumentService documentService,
            DocumentExpiryStatusCalculator statusCalculator,
            DocumentExpiryReminderGenerator reminderGenerator) {
        this.vehicleService = vehicleService;
        this.documentService = documentService;
        this.statusCalculator = statusCalculator;
        this.reminderGenerator = reminderGenerator;
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

    @GetMapping("/documents/new")
    public String createForm(
            @AuthenticationPrincipal VahanUserDetails user,
            Model model) {
        prepareForm(null, user, model, new DocumentForm(), null);
        return "documents/form";
    }

    @PostMapping({"/documents", "/vehicles/{vehicleId}/documents"})
    public String create(
            @PathVariable(required = false) Long vehicleId,
            @AuthenticationPrincipal VahanUserDetails user,
            @Valid @ModelAttribute("documentForm") DocumentForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (vehicleId != null) {
            vehicleService.findForOwner(vehicleId, user.getId());
        }
        if (form.getVehicleId() != null) {
            vehicleService.findForOwner(form.getVehicleId(), user.getId());
        }
        validateDates(form, bindingResult);
        if (bindingResult.hasErrors()) {
            prepareForm(vehicleId, user, model, form, null);
            return "documents/form";
        }
        Long selectedVehicleId = form.getVehicleId();
        VehicleDocument document = documentService.create(selectedVehicleId, user.getId(), form);
        reminderGenerator.generateDueReminders();
        redirectAttributes.addFlashAttribute("successMessage", "Document added successfully.");
        return "redirect:/vehicles/" + selectedVehicleId + "/documents/" + document.getId();
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
            Model model,
            RedirectAttributes redirectAttributes) {
        VehicleDocument document = documentService.findForOwner(vehicleId, documentId, user.getId());
        if (!Objects.equals(form.getVehicleId(), vehicleId)) {
            bindingResult.rejectValue("vehicleId", "document.vehicle.immutable",
                    "A document cannot be moved to another vehicle.");
        }
        validateDates(form, bindingResult);
        if (bindingResult.hasErrors()) {
            prepareForm(vehicleId, user, model, form, document);
            return "documents/form";
        }
        documentService.update(vehicleId, documentId, user.getId(), form);
        reminderGenerator.generateDueReminders();
        redirectAttributes.addFlashAttribute("successMessage", "Document updated successfully.");
        return "redirect:/vehicles/" + vehicleId + "/documents/" + documentId;
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

    private void prepareForm(
            Long vehicleId,
            VahanUserDetails user,
            Model model,
            DocumentForm form,
            VehicleDocument document) {
        List<Vehicle> vehicles = vehicleService.findAllForOwner(user.getId());
        Vehicle selectedVehicle = vehicleId == null
                ? null
                : vehicleService.findForOwner(vehicleId, user.getId());
        if (form.getVehicleId() == null && selectedVehicle != null) {
            form.setVehicleId(selectedVehicle.getId());
        } else if (form.getVehicleId() == null && !vehicles.isEmpty()) {
            form.setVehicleId(vehicles.get(0).getId());
        }
        model.addAttribute("vehicle", selectedVehicle);
        model.addAttribute("vehicles", vehicles);
        model.addAttribute("document", document);
        model.addAttribute("documentForm", form);
        model.addAttribute("documentTypes", List.of(DocumentType.values()));
        model.addAttribute("formAction", document == null
                ? vehicleId == null ? "/documents" : "/vehicles/" + vehicleId + "/documents"
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
