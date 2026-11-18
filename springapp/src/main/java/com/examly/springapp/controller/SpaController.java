package com.examly.springapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping(value = {"/", "/users", "/users/**", "/roles", "/roles/**", "/profiles", "/profiles/**", "/user-role-mappings", "/user-role-mappings/**"})
    public String renderIndex() {
        return "forward:/index.html";
    }
}
