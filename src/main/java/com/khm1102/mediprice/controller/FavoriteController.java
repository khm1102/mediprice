package com.khm1102.mediprice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/favorites")
public class FavoriteController {

    @GetMapping
    public String favoritesPage() {
        return "favorites";
    }
}
