package com.khm1102.mediprice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HospitalController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/hospitals")
    public String hospitals() {
        return "hospitals";
    }
}
