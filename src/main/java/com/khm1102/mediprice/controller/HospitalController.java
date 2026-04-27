package com.khm1102.mediprice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HospitalController {

    // 서블릿 컨텍스트는 AppConfig PropertySourcesPlaceholderConfigurer와 분리되어 있어
    // 시스템 프로퍼티 → 환경변수 순으로 읽음
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

    @GetMapping("/hospitals/{id}")
    public String hospitalDetail(@PathVariable Long id, Model model) {
        model.addAttribute("hospitalId", id);
        model.addAttribute("naverMapKey", NAVER_MAP_KEY);
        return "hospital-detail";
    }
}
