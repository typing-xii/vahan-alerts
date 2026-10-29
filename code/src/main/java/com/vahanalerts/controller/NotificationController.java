package com.vahanalerts.controller;

import com.vahanalerts.notification.InAppNotificationService;
import com.vahanalerts.user.VahanUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class NotificationController {

    private final InAppNotificationService notificationService;

    public NotificationController(InAppNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/notifications")
    public String list(@AuthenticationPrincipal VahanUserDetails user, Model model) {
        model.addAttribute("currentUser", user);
        model.addAttribute("notifications", notificationService.findAllForOwner(user.getId()));
        return "notifications/list";
    }

    @PostMapping("/notifications/{notificationId}/read")
    public String markRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal VahanUserDetails user,
            RedirectAttributes redirectAttributes) {
        notificationService.markRead(notificationId, user.getId());
        redirectAttributes.addFlashAttribute("successMessage", "Notification marked as read.");
        return "redirect:/notifications";
    }

    @PostMapping("/notifications/read-all")
    public String markAllRead(
            @AuthenticationPrincipal VahanUserDetails user,
            RedirectAttributes redirectAttributes) {
        int updated = notificationService.markAllRead(user.getId());
        redirectAttributes.addFlashAttribute(
                "successMessage",
                updated == 0 ? "There are no unread notifications." : "All notifications marked as read.");
        return "redirect:/notifications";
    }
}
