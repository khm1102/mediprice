package com.khm1102.mediprice.controller;

import com.khm1102.mediprice.service.NonPayItemService;
import com.khm1102.mediprice.dto.NonPayItemGroupDto;

import com.khm1102.mediprice.global.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/items")
public class NonPayItemApiController {

    private final NonPayItemService service;

    public NonPayItemApiController(NonPayItemService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<NonPayItemGroupDto>> searchItems() {
        return ApiResponse.success(service.searchGroupedItems());
    }
}
