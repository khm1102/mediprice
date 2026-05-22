package com.khm1102.mediprice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/favorites")
public class FavoriteController {

    private static final String NAVER_MAP_KEY =
            System.getProperty("NAVER_MAP_KEY",
                System.getenv("NAVER_MAP_KEY") != null ? System.getenv("NAVER_MAP_KEY") : "");

    @GetMapping
    public String favoritesPage(Model model) {
        model.addAttribute("naverMapKey", NAVER_MAP_KEY);
        return "favorites";
    }
}
