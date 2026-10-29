package com.vahanalerts.controller;

import java.util.List;

import com.vahanalerts.user.VahanUserDetails;
import com.vahanalerts.vehicle.DuplicateVehicleRegistrationException;
import com.vahanalerts.vehicle.FuelType;
import com.vahanalerts.vehicle.Vehicle;
import com.vahanalerts.vehicle.VehicleForm;
import com.vahanalerts.vehicle.VehicleService;
import com.vahanalerts.vehicle.VehicleType;
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
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping("/vehicles")
    public String list(@AuthenticationPrincipal VahanUserDetails user, Model model) {
        model.addAttribute("vehicles", vehicleService.findAllForOwner(user.getId()));
        model.addAttribute("currentUser", user);
        return "vehicles/list";
    }

    @GetMapping("/vehicles/new")
    public String createForm(@AuthenticationPrincipal VahanUserDetails user, Model model) {
        model.addAttribute("vehicleForm", new VehicleForm());
        model.addAttribute("currentUser", user);
        model.addAttribute("formAction", "/vehicles");
        addFormOptions(model);
        return "vehicles/form";
    }

    @PostMapping("/vehicles")
    public String create(
            @AuthenticationPrincipal VahanUserDetails user,
            @Valid @ModelAttribute("vehicleForm") VehicleForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("currentUser", user);
            model.addAttribute("formAction", "/vehicles");
            addFormOptions(model);
            return "vehicles/form";
        }
        try {
            Vehicle vehicle = vehicleService.create(user.getId(), form);
            redirectAttributes.addFlashAttribute("successMessage", "Vehicle added successfully.");
            return "redirect:/vehicles/" + vehicle.getId();
        } catch (DuplicateVehicleRegistrationException exception) {
            bindingResult.rejectValue("registrationNumber", "vehicle.registration.duplicate",
                    exception.getMessage());
            model.addAttribute("currentUser", user);
            model.addAttribute("formAction", "/vehicles");
            addFormOptions(model);
            return "vehicles/form";
        }
    }

    @GetMapping("/vehicles/{id}")
    public String detail(
            @PathVariable Long id,
            @AuthenticationPrincipal VahanUserDetails user,
            Model model) {
        model.addAttribute("vehicle", vehicleService.findForOwner(id, user.getId()));
        model.addAttribute("currentUser", user);
        return "vehicles/detail";
    }

    @GetMapping("/vehicles/{id}/edit")
    public String editForm(
            @PathVariable Long id,
            @AuthenticationPrincipal VahanUserDetails user,
            Model model) {
        Vehicle vehicle = vehicleService.findForOwner(id, user.getId());
        model.addAttribute("vehicleForm", VehicleForm.from(vehicle));
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("currentUser", user);
        model.addAttribute("formAction", "/vehicles/" + id);
        addFormOptions(model);
        return "vehicles/form";
    }

    @PostMapping("/vehicles/{id}")
    public String update(
            @PathVariable Long id,
            @AuthenticationPrincipal VahanUserDetails user,
            @Valid @ModelAttribute("vehicleForm") VehicleForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        Vehicle vehicle = vehicleService.findForOwner(id, user.getId());
        if (bindingResult.hasErrors()) {
            model.addAttribute("vehicle", vehicle);
            model.addAttribute("currentUser", user);
            model.addAttribute("formAction", "/vehicles/" + id);
            addFormOptions(model);
            return "vehicles/form";
        }
        try {
            vehicleService.update(id, user.getId(), form);
            redirectAttributes.addFlashAttribute("successMessage", "Vehicle updated successfully.");
            return "redirect:/vehicles/" + id;
        } catch (DuplicateVehicleRegistrationException exception) {
            bindingResult.rejectValue("registrationNumber", "vehicle.registration.duplicate",
                    exception.getMessage());
            model.addAttribute("vehicle", vehicle);
            model.addAttribute("currentUser", user);
            model.addAttribute("formAction", "/vehicles/" + id);
            addFormOptions(model);
            return "vehicles/form";
        }
    }

    @GetMapping("/vehicles/{id}/delete")
    public String confirmDelete(
            @PathVariable Long id,
            @AuthenticationPrincipal VahanUserDetails user,
            Model model) {
        model.addAttribute("vehicle", vehicleService.findForOwner(id, user.getId()));
        model.addAttribute("currentUser", user);
        return "vehicles/delete";
    }

    @PostMapping("/vehicles/{id}/delete")
    public String delete(
            @PathVariable Long id,
            @AuthenticationPrincipal VahanUserDetails user,
            RedirectAttributes redirectAttributes) {
        vehicleService.delete(id, user.getId());
        redirectAttributes.addFlashAttribute("successMessage", "Vehicle deleted.");
        return "redirect:/vehicles";
    }

    private void addFormOptions(Model model) {
        List<VehicleType> types = List.of(VehicleType.values());
        List<FuelType> fuelTypes = List.of(FuelType.values());
        model.addAttribute("vehicleTypes", types);
        model.addAttribute("fuelTypes", fuelTypes);
    }
}
