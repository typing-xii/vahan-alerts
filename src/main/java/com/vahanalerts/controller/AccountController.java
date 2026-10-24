package com.vahanalerts.controller;

import com.vahanalerts.user.VahanUserDetails;
import com.vahanalerts.user.WhatsAppConsentService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AccountController {

    private final WhatsAppConsentService whatsAppConsentService;

    public AccountController(WhatsAppConsentService whatsAppConsentService) {
        this.whatsAppConsentService = whatsAppConsentService;
    }

    @GetMapping("/account")
    public String account(@AuthenticationPrincipal VahanUserDetails user, Model model) {
        model.addAttribute("currentUser", user);
        model.addAttribute("account", whatsAppConsentService.findAccount(user.getId()));
        return "account";
    }

    @PostMapping("/account/whatsapp-consent/withdraw")
    public String withdrawWhatsAppConsent(
            @AuthenticationPrincipal VahanUserDetails user,
            RedirectAttributes redirectAttributes) {
        whatsAppConsentService.withdrawConsent(user.getId());
        redirectAttributes.addFlashAttribute("consentWithdrawn", true);
        return "redirect:/account";
    }
}
