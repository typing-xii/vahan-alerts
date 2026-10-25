package com.vahanalerts.controller;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import com.vahanalerts.maintenance.FutureServiceDateException;
import com.vahanalerts.maintenance.MaintenanceForm;
import com.vahanalerts.maintenance.MaintenanceRecord;
import com.vahanalerts.maintenance.MaintenanceRecordService;
import com.vahanalerts.maintenance.MaintenanceSummary;
import com.vahanalerts.maintenance.OdometerSequenceException;
import com.vahanalerts.maintenance.ServiceType;
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
public class MaintenanceController {

    private final VehicleService vehicleService;
    private final MaintenanceRecordService maintenanceRecordService;
    private final Clock clock;

    public MaintenanceController(
            VehicleService vehicleService,
            MaintenanceRecordService maintenanceRecordService,
            Clock clock) {
        this.vehicleService = vehicleService;
        this.maintenanceRecordService = maintenanceRecordService;
        this.clock = clock;
    }

    @GetMapping("/vehicles/{vehicleId}/maintenance")
    public String list(
            @PathVariable Long vehicleId,
            @AuthenticationPrincipal VahanUserDetails user,
            Model model) {
        Vehicle vehicle = vehicleService.findForOwner(vehicleId, user.getId());
        List<MaintenanceRecord> records =
                maintenanceRecordService.findAllForOwner(vehicleId, user.getId());
        MaintenanceSummary summary =
                maintenanceRecordService.summarizeForOwner(vehicleId, user.getId());
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("records", records);
        model.addAttribute("summary", summary);
        model.addAttribute("currentUser", user);
        return "maintenance/list";
    }

    @GetMapping("/vehicles/{vehicleId}/maintenance/new")
    public String createForm(
            @PathVariable Long vehicleId,
            @AuthenticationPrincipal VahanUserDetails user,
            Model model) {
        prepareForm(vehicleId, user, model, new MaintenanceForm(), null);
        return "maintenance/form";
    }

    @PostMapping("/vehicles/{vehicleId}/maintenance")
    public String create(
            @PathVariable Long vehicleId,
            @AuthenticationPrincipal VahanUserDetails user,
            @Valid @ModelAttribute("maintenanceForm") MaintenanceForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        validateServiceDate(form, bindingResult);
        if (bindingResult.hasErrors()) {
            prepareForm(vehicleId, user, model, form, null);
            return "maintenance/form";
        }
        try {
            MaintenanceRecord record = maintenanceRecordService.create(vehicleId, user.getId(), form);
            redirectAttributes.addFlashAttribute("successMessage", "Service record added successfully.");
            return "redirect:/vehicles/" + vehicleId + "/maintenance/" + record.getId();
        } catch (FutureServiceDateException exception) {
            bindingResult.rejectValue("serviceDate", "maintenance.date.future", exception.getMessage());
        } catch (OdometerSequenceException exception) {
            bindingResult.rejectValue("odometerKm", "maintenance.odometer.sequence", exception.getMessage());
        }
        prepareForm(vehicleId, user, model, form, null);
        return "maintenance/form";
    }

    @GetMapping("/vehicles/{vehicleId}/maintenance/{recordId}")
    public String detail(
            @PathVariable Long vehicleId,
            @PathVariable Long recordId,
            @AuthenticationPrincipal VahanUserDetails user,
            Model model) {
        model.addAttribute("vehicle", vehicleService.findForOwner(vehicleId, user.getId()));
        model.addAttribute("record",
                maintenanceRecordService.findForOwner(vehicleId, recordId, user.getId()));
        model.addAttribute("currentUser", user);
        return "maintenance/detail";
    }

    @GetMapping("/vehicles/{vehicleId}/maintenance/{recordId}/edit")
    public String editForm(
            @PathVariable Long vehicleId,
            @PathVariable Long recordId,
            @AuthenticationPrincipal VahanUserDetails user,
            Model model) {
        MaintenanceRecord record =
                maintenanceRecordService.findForOwner(vehicleId, recordId, user.getId());
        prepareForm(vehicleId, user, model, MaintenanceForm.from(record), record);
        return "maintenance/form";
    }

    @PostMapping("/vehicles/{vehicleId}/maintenance/{recordId}")
    public String update(
            @PathVariable Long vehicleId,
            @PathVariable Long recordId,
            @AuthenticationPrincipal VahanUserDetails user,
            @Valid @ModelAttribute("maintenanceForm") MaintenanceForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        MaintenanceRecord record =
                maintenanceRecordService.findForOwner(vehicleId, recordId, user.getId());
        validateServiceDate(form, bindingResult);
        if (bindingResult.hasErrors()) {
            prepareForm(vehicleId, user, model, form, record);
            return "maintenance/form";
        }
        try {
            maintenanceRecordService.update(vehicleId, recordId, user.getId(), form);
            redirectAttributes.addFlashAttribute("successMessage", "Service record updated successfully.");
            return "redirect:/vehicles/" + vehicleId + "/maintenance/" + recordId;
        } catch (FutureServiceDateException exception) {
            bindingResult.rejectValue("serviceDate", "maintenance.date.future", exception.getMessage());
        } catch (OdometerSequenceException exception) {
            bindingResult.rejectValue("odometerKm", "maintenance.odometer.sequence", exception.getMessage());
        }
        prepareForm(vehicleId, user, model, form, record);
        return "maintenance/form";
    }

    @GetMapping("/vehicles/{vehicleId}/maintenance/{recordId}/delete")
    public String confirmDelete(
            @PathVariable Long vehicleId,
            @PathVariable Long recordId,
            @AuthenticationPrincipal VahanUserDetails user,
            Model model) {
        model.addAttribute("vehicle", vehicleService.findForOwner(vehicleId, user.getId()));
        model.addAttribute("record",
                maintenanceRecordService.findForOwner(vehicleId, recordId, user.getId()));
        model.addAttribute("currentUser", user);
        return "maintenance/delete";
    }

    @PostMapping("/vehicles/{vehicleId}/maintenance/{recordId}/delete")
    public String delete(
            @PathVariable Long vehicleId,
            @PathVariable Long recordId,
            @AuthenticationPrincipal VahanUserDetails user,
            RedirectAttributes redirectAttributes) {
        maintenanceRecordService.delete(vehicleId, recordId, user.getId());
        redirectAttributes.addFlashAttribute("successMessage", "Service record deleted.");
        return "redirect:/vehicles/" + vehicleId + "/maintenance";
    }

    private void prepareForm(
            Long vehicleId,
            VahanUserDetails user,
            Model model,
            MaintenanceForm form,
            MaintenanceRecord record) {
        model.addAttribute("vehicle", vehicleService.findForOwner(vehicleId, user.getId()));
        model.addAttribute("record", record);
        model.addAttribute("maintenanceForm", form);
        model.addAttribute("serviceTypes", List.of(ServiceType.values()));
        model.addAttribute("formAction", record == null
                ? "/vehicles/" + vehicleId + "/maintenance"
                : "/vehicles/" + vehicleId + "/maintenance/" + record.getId());
        model.addAttribute("currentUser", user);
    }

    private void validateServiceDate(MaintenanceForm form, BindingResult bindingResult) {
        LocalDate today = LocalDate.now(clock);
        if (form.getServiceDate() != null && form.getServiceDate().isAfter(today)) {
            bindingResult.rejectValue("serviceDate", "maintenance.date.future",
                    "Service date cannot be in the future.");
        }
    }
}
