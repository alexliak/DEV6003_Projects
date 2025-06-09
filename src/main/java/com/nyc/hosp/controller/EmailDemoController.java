package com.nyc.hosp.controller;

import com.nyc.hosp.service.MockEmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/email-demo")
@PreAuthorize("hasRole('ADMIN')")
public class EmailDemoController {
    
    @Autowired
    private MockEmailService mockEmailService;
    
    @GetMapping
    public String viewEmails(Model model) {
        model.addAttribute("emails", mockEmailService.getAllSentEmails());
        return "email/demo";
    }
    
    @GetMapping("/clear")
    public String clearEmails() {
        mockEmailService.clearAllEmails();
        return "redirect:/email-demo";
    }
}
