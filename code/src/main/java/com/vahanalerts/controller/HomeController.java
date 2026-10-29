package com.vahanalerts.controller;

import com.vahanalerts.user.VahanUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(@AuthenticationPrincipal VahanUserDetails user, Model model) {
        if (user != null) {
            model.addAttribute("currentUser", user);
        }
        return "index";
    }
}
