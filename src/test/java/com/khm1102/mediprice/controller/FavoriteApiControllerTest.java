package com.khm1102.mediprice.controller;

import com.khm1102.mediprice.global.common.ApiResponse;
import com.khm1102.mediprice.global.exception.ErrorCode;
import com.khm1102.mediprice.global.exception.auth.AuthenticationException;
import com.khm1102.mediprice.global.security.MemberPrincipal;
import com.khm1102.mediprice.service.FavoriteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteApiControllerTest {

    @Mock FavoriteService favoriteService;

    private static final MemberPrincipal MEMBER = new MemberPrincipal(42L, "u@x", "MEMBER", "Name");
    private static final MemberPrincipal GUEST = new MemberPrincipal(null, "g-uuid", "GUEST", null);

    private FavoriteApiController controller() {
        return new FavoriteApiController(favoriteService);
    }

    /** principal이 null이면 컨트롤러 단 가드가 A005 예외를 던진다. */
    @Test
    void rejectsNullPrincipal() {
        FavoriteApiController c = controller();
        assertThatThrownBy(() -> c.getFavorites(null))
                .isInstanceOf(AuthenticationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);
        verify(favoriteService, never()).lookupFavorites(org.mockito.ArgumentMatchers.anyLong());
    }

    /** GUEST principal은 회원 API에 접근할 수 없다. */
    @Test
    void rejectsGuestPrincipal() {
        FavoriteApiController c = controller();
        assertThatThrownBy(() -> c.addFavorite(GUEST, Map.of("ykiho", "YK1")))
                .isInstanceOf(AuthenticationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);
        verify(favoriteService, never())
                .addFavorite(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString());
    }

    /** memberId가 null인 비정상 principal도 차단. */
    @Test
    void rejectsMemberPrincipalWithoutMemberId() {
        FavoriteApiController c = controller();
        MemberPrincipal broken = new MemberPrincipal(null, "u@x", "MEMBER", "Name");
        assertThatThrownBy(() -> c.removeFavorite(broken, "YK1"))
                .isInstanceOf(AuthenticationException.class);
        verify(favoriteService, never())
                .removeFavorite(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString());
    }

    /** 정상 MEMBER는 통과해 서비스가 호출된다. */
    @Test
    void allowsMemberPrincipal() {
        when(favoriteService.existsFavorite(42L, "YK1")).thenReturn(true);

        ApiResponse<Map<String, Boolean>> response = controller().getFavoriteStatus(MEMBER, "YK1");

        assertThat(response.success()).isTrue();
        assertThat(response.data()).containsEntry("isFavorite", true);
        verify(favoriteService).existsFavorite(42L, "YK1");
    }
}
