package com.khm1102.mediprice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HospitalController {

    private static final String NAVER_MAP_KEY =
            System.getProperty("NAVER_MAP_KEY",
                System.getenv("NAVER_MAP_KEY") != null ? System.getenv("NAVER_MAP_KEY") : "");

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("naverMapKey", NAVER_MAP_KEY);
        return "index";
    }

    @GetMapping("/hospitals")
    public String hospitals(@RequestParam(required = false) String keyword, Model model) {
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("naverMapKey", NAVER_MAP_KEY);
        return "hospitals";
    }

    @GetMapping("/hospital")
    public String hospitalDetail(@RequestParam String ykiho, Model model) {
        model.addAttribute("naverMapKey", NAVER_MAP_KEY);
        return "hospital-detail";
    }
}
