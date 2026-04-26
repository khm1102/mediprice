package com.khm1102.mediprice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Controller
public class HealthController {

    @GetMapping("/health")
    public String health(Model model) {
        model.addAttribute("serverTime",
                OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        return "health";
    }
}
