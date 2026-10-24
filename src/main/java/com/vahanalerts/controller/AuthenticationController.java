package com.vahanalerts.controller;

import java.nio.charset.StandardCharsets;

import com.vahanalerts.user.PhoneNumberAlreadyRegisteredException;
import com.vahanalerts.user.RegistrationForm;
import com.vahanalerts.user.UserRegistrationService;
import com.vahanalerts.user.VahanUserDetails;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthenticationController {

    private final UserRegistrationService userRegistrationService;

    public AuthenticationController(UserRegistrationService userRegistrationService) {
        this.userRegistrationService = userRegistrationService;
    }

    @GetMapping("/register")
    public String registrationForm(Model model) {
        model.addAttribute("registrationForm", new RegistrationForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("registrationForm") RegistrationForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (form.getPassword() != null
                && form.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            bindingResult.rejectValue("password", "password.tooLong",
                    "Password must use no more than 72 UTF-8 bytes.");
        }
        if (form.getPassword() != null
                && form.getPasswordConfirmation() != null
                && !form.getPassword().equals(form.getPasswordConfirmation())) {
            bindingResult.rejectValue("passwordConfirmation", "password.mismatch",
                    "The passwords do not match.");
        }
        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            userRegistrationService.register(form);
        } catch (PhoneNumberAlreadyRegisteredException exception) {
            bindingResult.rejectValue("phoneNumber", "phoneNumber.duplicate",
                    exception.getMessage());
            return "register";
        }

        redirectAttributes.addFlashAttribute("registered", true);
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login(@AuthenticationPrincipal VahanUserDetails user) {
        if (user != null) {
            return "redirect:/";
        }
        return "login";
    }
}
