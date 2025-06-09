package com.nyc.hosp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class TestController {
    
    @GetMapping("/test-audit")
    @ResponseBody
    public String test() {
        return "Test endpoint working! Try /admin/audit-logs";
    }
    
    @GetMapping("/admin/audit-test")
    @ResponseBody
    public String adminTest() {
        return "Admin path working!";
    }
}
