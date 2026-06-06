package com.khm1102.mediprice.controller;

import com.khm1102.mediprice.dto.FavoriteDto;
import com.khm1102.mediprice.global.common.ApiResponse;
import com.khm1102.mediprice.global.exception.ErrorCode;
import com.khm1102.mediprice.global.exception.auth.AuthenticationException;
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
        Long memberId = requireMember(principal);
        return ApiResponse.success(favoriteService.lookupFavorites(memberId));
    }

    /** 즐겨찾기 추가 */
    @PostMapping
    public ApiResponse<Void> addFavorite(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestBody Map<String, String> body) {
        Long memberId = requireMember(principal);
        String ykiho = body.get("ykiho");
        favoriteService.addFavorite(memberId, ykiho);
        return ApiResponse.success(null);
    }

    /** 즐겨찾기 제거 */
    @DeleteMapping("/{ykiho}")
    public ApiResponse<Void> removeFavorite(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable String ykiho) {
        Long memberId = requireMember(principal);
        favoriteService.removeFavorite(memberId, ykiho);
        return ApiResponse.success(null);
    }

    @GetMapping("/{ykiho}/status")
    public ApiResponse<Map<String, Boolean>> getFavoriteStatus(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable String ykiho) {
        Long memberId = requireMember(principal);
        boolean isFavorite = favoriteService.existsFavorite(memberId, ykiho);
        return ApiResponse.success(Map.of("isFavorite", isFavorite));
    }

    /**
     * SecurityConfig가 1차로 ROLE_MEMBER만 통과시키지만, 필터 우회나 설정 변경 회귀를 막기 위한 deep defense.
     */
    private static Long requireMember(MemberPrincipal principal) {
        if (principal == null || principal.isGuest() || principal.memberId() == null) {
            throw new AuthenticationException(ErrorCode.UNAUTHORIZED);
        }
        return principal.memberId();
    }
}
