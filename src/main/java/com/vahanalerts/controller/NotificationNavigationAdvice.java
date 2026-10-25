package com.vahanalerts.controller;

import com.vahanalerts.notification.InAppNotificationService;
import com.vahanalerts.user.VahanUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class NotificationNavigationAdvice {

    private final InAppNotificationService notificationService;

    public NotificationNavigationAdvice(InAppNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @ModelAttribute("unreadNotificationCount")
    public long unreadNotificationCount(@AuthenticationPrincipal VahanUserDetails user) {
        return user == null ? 0 : notificationService.countUnreadForOwner(user.getId());
    }
}
