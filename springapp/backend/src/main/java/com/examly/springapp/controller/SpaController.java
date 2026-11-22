package com.examly.springapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping(value = {
            "/",
            "/catalog",
            "/tickets", "/tickets/**",
            "/approvals",
            "/queue", "/queue/**",
            "/audit-chain",
            "/notifications", "/notifications/**",
            "/analytics", "/analytics/**",
            "/profile",
            "/preferences",
            "/admin/metrics",
            "/users", "/users/**",
            "/roles", "/roles/**",
            "/profiles", "/profiles/**",
            "/user-role-mappings", "/user-role-mappings/**",
            "/audit", "/audit/**"
    })
    public String renderIndex() {
        return "forward:/index.html";
    }
}
