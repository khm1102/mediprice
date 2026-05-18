package com.khm1102.mediprice.controller;

import com.khm1102.mediprice.dto.FavoriteDto;
import com.khm1102.mediprice.global.common.ApiResponse;
import com.khm1102.mediprice.global.security.MemberPrincipal;
import com.khm1102.mediprice.service.FavoriteService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteApiController {

    private final FavoriteService favoriteService;

    public FavoriteApiController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    /** 즐겨찾기 조회 */
    @GetMapping
    public ApiResponse<List<FavoriteDto>> getFavorites(
            @AuthenticationPrincipal MemberPrincipal principal) {
        return ApiResponse.success(favoriteService.lookupFavorites(principal.memberId()));
    }

    /** 즐겨찾기 추가 */
    @PostMapping
    public ApiResponse<Void> addFavorite(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestBody Map<String, String> body) {
        String ykiho = body.get("ykiho");
        favoriteService.addFavorite(principal.memberId(), ykiho);
        return ApiResponse.success(null);
    }

    /** 즐겨찾기 제거 */
    @DeleteMapping("/{ykiho}")
    public ApiResponse<Void> removeFavorite(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable String ykiho) {
        favoriteService.removeFavorite(principal.memberId(), ykiho);
        return ApiResponse.success(null);
    }

    @GetMapping("/{ykiho}/status")
    public ApiResponse<Map<String, Boolean>> getFavoriteStatus(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable String ykiho) {
        boolean isFavorite = favoriteService.existsFavorite(principal.memberId(), ykiho);
        return ApiResponse.success(Map.of("isFavorite", isFavorite));
    }
}
