package com.khm1102.mediprice.web;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 프론트엔드 정적 자원에 대한 회귀 방지선.
 * <p>
 * 자동 JS/JSP 테스트 인프라가 없으므로, 핵심 변경 사항이 추후 PR로 사라지지 않게 텍스트 패턴으로 검사한다.
 * <ul>
 *   <li>common.js: escapeHtml 유틸이 정의되어 있어야 한다.</li>
 *   <li>hospital.js / favorites.js: innerHTML 사용처에 escapeHtml 호출이 함께 있어야 한다.</li>
 *   <li>3개 JSP: medOft 섹션 라벨이 "의료장비"이고, 운영/주차 섹션 마크업이 존재해야 한다.</li>
 * </ul>
 */
class FrontendStaticCheckTest {

    private static final Path WEBAPP = Path.of("src/main/webapp");

    private static String read(String relative) throws IOException {
        return Files.readString(WEBAPP.resolve(relative), StandardCharsets.UTF_8);
    }

    @Test
    void commonJsExposesEscapeHtmlUtility() throws IOException {
        String js = read("static/js/common.js");
        assertThat(js).contains("const escapeHtml");
        assertThat(js).contains("textContent");
    }

    @Test
    void jspCUrlTagsUseSingleQuotedValueAttributesInsideHtmlAttributes() throws IOException {
        for (String path : java.util.List.of(
                "WEB-INF/views/common/header.jsp",
                "WEB-INF/views/hospitals.jsp",
                "WEB-INF/views/favorites.jsp",
                "WEB-INF/views/hospital-detail.jsp",
                "WEB-INF/views/legal/location-terms.jsp",
                "WEB-INF/views/legal/privacy.jsp",
                "WEB-INF/views/legal/terms.jsp"
        )) {
            assertThat(read(path))
                    .as("%s: c:url inside HTML attributes should use single quotes for its value attribute", path)
                    .doesNotContain("<c:url value=\"");
        }
    }

    @Test
    void hospitalJsAppliesEscapeHtmlWhereverInnerHtmlIsUsed() throws IOException {
        String js = read("static/js/hospital.js");
        long innerHtmlCount = js.lines().filter(l -> l.contains("innerHTML =")).count();
        long escapeUsage = js.lines().filter(l -> l.contains("escapeHtml(")).count();
        // innerHTML이 등장하는 코드 줄 수 이상으로 escapeHtml이 호출되어야 한다 (각 템플릿에서 최소 1회 이상).
        assertThat(escapeUsage)
                .as("hospital.js innerHTML 사용 라인 수=%d, escapeHtml 호출 수=%d", innerHtmlCount, escapeUsage)
                .isGreaterThanOrEqualTo(innerHtmlCount);
    }

    @Test
    void favoritesJsUsesSharedEscapeHtml() throws IOException {
        String js = read("static/js/favorites.js");
        // 로컬 escapeHtml 정의가 제거되어야 한다 (common.js 의존).
        assertThat(js).doesNotContain("const escapeHtml = (str)");
        // 그대로 escapeHtml은 호출되어야 한다 (카드 렌더링 등).
        assertThat(js).contains("escapeHtml(");
    }

    @Test
    void hospitalDetailJspRenamesMedOftAndAddsOperatingAndParkingSections() throws IOException {
        String jsp = read("WEB-INF/views/hospital-detail.jsp");
        assertThat(jsp).contains("id=\"section-medoft\"");
        assertThat(jsp).contains("의료장비");
        assertThat(jsp).contains("id=\"section-operating\"");
        assertThat(jsp).contains("id=\"operating-list\"");
        assertThat(jsp).contains("id=\"section-parking\"");
        assertThat(jsp).contains("id=\"parking-list\"");
    }

    @Test
    void hospitalsJspRenamesMedOftAndAddsOperatingAndParkingSections() throws IOException {
        String jsp = read("WEB-INF/views/hospitals.jsp");
        assertThat(jsp).contains("id=\"pd-section-medoft\"");
        assertThat(jsp).contains("의료장비");
        assertThat(jsp).contains("id=\"pd-section-operating\"");
        assertThat(jsp).contains("id=\"pd-operating-list\"");
        assertThat(jsp).contains("id=\"pd-section-parking\"");
        assertThat(jsp).contains("id=\"pd-parking-list\"");
    }

    @Test
    void favoritesJspRenamesMedOftAndAddsOperatingAndParkingSections() throws IOException {
        String jsp = read("WEB-INF/views/favorites.jsp");
        assertThat(jsp).contains("id=\"pd-section-medoft\"");
        assertThat(jsp).contains("의료장비");
        assertThat(jsp).contains("id=\"pd-section-operating\"");
        assertThat(jsp).contains("id=\"pd-operating-list\"");
        assertThat(jsp).contains("id=\"pd-section-parking\"");
        assertThat(jsp).contains("id=\"pd-parking-list\"");
    }

    /** authReady 캐시가 풀리기 전 isLoggedIn() 호출되는 회귀 방지 — favorites.js와 hospital.js 모두에 await가 있어야 한다. */
    @Test
    void favoritesAndHospitalJsAwaitAuthReadyBeforeLoginChecks() throws IOException {
        String fav = read("static/js/favorites.js");
        assertThat(fav).contains("await authReady");

        String hos = read("static/js/hospital.js");
        assertThat(hos).contains("await authReady");
    }

    /**
     * 회귀 방지: favorites.js renderFavoriteCard도 inline onclick 조립 방식을 다시 들이면 안 된다.
     * 모든 클릭 처리는 #favorites-list에 위임된 리스너 + data-* 속성으로만 한다.
     */
    @Test
    void favoritesCardUsesDelegatedClickInsteadOfInlineOnclick() throws IOException {
        String fav = read("static/js/favorites.js");
        // inline onclick + ykihoJs 조립 패턴이 다시 들어오면 fail.
        assertThat(fav)
                .doesNotContain("onclick=\"showHospitalInPanel(")
                .doesNotContain("onclick=\"handleFavoritesRemove(")
                .doesNotContain("const ykihoJs");
        // data-* 직렬화 + 위임 리스너 식별자.
        assertThat(fav).contains("data-ykiho=\"${ykihoAttr}\"");
        assertThat(fav).contains("data-lat=");
        assertThat(fav).contains("data-lng=");
        assertThat(fav).contains("_bindFavoritesListClicks");
        assertThat(fav).contains("event.target.closest('.fav-remove-btn')");
        assertThat(fav).contains("event.target.closest('.hospital-card')");
    }

    /** 폐기된 비회원 검색 횟수 제한 정책은 약관에도 application.yml에도 흔적이 남아 있으면 안 된다. */
    @Test
    void legalTermsAndYamlNoLongerReferenceGuestSearchLimit() throws IOException {
        String yml = Files.readString(Path.of("src/main/resources/application.yml"), StandardCharsets.UTF_8);
        assertThat(yml).doesNotContain("guest.search-limit");
        assertThat(yml).doesNotContain("GUEST_SEARCH_LIMIT");

        String terms = read("WEB-INF/views/legal/terms.jsp");
        assertThat(terms).doesNotContain("비회원");
        assertThat(terms).doesNotContain("검색 횟수");
        assertThat(terms).contains("회원 가입 없이 누구나");
    }

    // ── UX/반응형 개편 회귀 방지선 ────────────────────────────────────────────

    /** common.js에 자동완성 dropdown / 인기 칩 유틸이 정의되어 있어야 한다. */
    @Test
    void commonJsExposesSuggestionAndQuickChipUtilities() throws IOException {
        String js = read("static/js/common.js");
        assertThat(js).contains("const renderQuickChips");
        assertThat(js).contains("const attachSuggestionInput");
    }

    /** hospitals.jsp 검색바에 자동완성 dropdown / 빈 결과 추천 컨테이너가 있어야 한다. */
    @Test
    void hospitalsJspHasSearchAutocompleteAndPaneTabsMarkup() throws IOException {
        String jsp = read("WEB-INF/views/hospitals.jsp");
        assertThat(jsp).contains("id=\"search-suggestions\"");
        assertThat(jsp).contains("id=\"state-empty-chips\"");
        assertThat(jsp).contains("id=\"weight-slider\"");
    }

    /** hospital.js에 신규 핵심 식별자(togglePane / renderEmptyStateChips / initSearchUx / 검색 가격 카드 / hover) 가 있어야 한다. */
    @Test
    void hospitalJsExposesNewUxIdentifiers() throws IOException {
        String js = read("static/js/hospital.js");
        assertThat(js).contains("const togglePane");
        assertThat(js).contains("const renderEmptyStateChips");
        assertThat(js).contains("const initSearchUx");
        assertThat(js).contains("_renderSearchPriceCard");
        assertThat(js).contains("attachCardHoverHighlight");
        assertThat(js).contains("POPULAR_KEYWORDS");
    }

    /** map.js에 현재 위치 마커(showCurrentLocation) + segmented 전환 후 사이즈 갱신(refreshMapSize) API가 있어야 한다. */
    @Test
    void mapJsExposesCurrentLocationAndRefreshHooks() throws IOException {
        String js = read("static/js/map.js");
        assertThat(js).contains("const showCurrentLocation");
        assertThat(js).contains("const refreshMapSize");
    }

    /** 카드 템플릿은 가격을 큰 폰트(text-xl)로 표시하고, 가격 미신고도 명시한다. */
    @Test
    void hospitalCardPromotesPriceWithLargeFont() throws IOException {
        String js = read("static/js/hospital.js");
        // renderHospitalCard 안에 text-xl 클래스가 가격 블록에 들어가야 한다.
        assertThat(js).contains("text-xl font-bold text-[#2563EB]");
        assertThat(js).contains("가격 미신고");
    }

    /** 상세 패널/풀스크린: 검색 가격 카드 섹션이 진료과목 details 보다 앞에 등장하고, 부가 섹션은 details 요소다. */
    @Test
    void detailLayoutPlacesSearchPriceCardBeforeCollapsibleSections() throws IOException {
        for (String path : new String[]{
                "WEB-INF/views/hospitals.jsp",
                "WEB-INF/views/hospital-detail.jsp",
                "WEB-INF/views/favorites.jsp"
        }) {
            String jsp = read(path);
            int searchCard = jsp.indexOf("search-price");
            int dgsbjtDetails = jsp.indexOf("section-dgsbjt");
            assertThat(searchCard)
                    .as("%s: search-price 섹션 마크업이 존재해야 한다", path)
                    .isGreaterThan(-1);
            assertThat(dgsbjtDetails)
                    .as("%s: 진료과목 섹션 마크업이 존재해야 한다", path)
                    .isGreaterThan(-1);
            assertThat(searchCard)
                    .as("%s: 검색 가격 카드는 진료과목 details 앞에 있어야 한다", path)
                    .isLessThan(dgsbjtDetails);
            // 부가 섹션이 details/summary로 감싸져 있는지
            assertThat(jsp).contains("<details");
            assertThat(jsp).contains("<summary");
        }
    }

    /** favorites.jsp의 map-overlay는 제거되었고, 지도 영역의 인터랙션을 막지 않는다. */
    @Test
    void favoritesMapOverlayIsRemoved() throws IOException {
        String jsp = read("WEB-INF/views/favorites.jsp");
        assertThat(jsp).doesNotContain("id=\"map-overlay\"");
        // CSS 정의도 제거
        assertThat(jsp).doesNotContain("#map-overlay");
    }

    /** hospitals.jsp / favorites.jsp 모두 데스크톱 lg(1024) + xl(1280) 두 breakpoint를 갖는다. */
    @Test
    void responsiveBreakpointsAreLgAndXl() throws IOException {
        for (String path : new String[]{
                "WEB-INF/views/hospitals.jsp",
                "WEB-INF/views/favorites.jsp"
        }) {
            String jsp = read(path);
            assertThat(jsp).contains("min-width: 1024px");
            assertThat(jsp).contains("min-width: 1280px");
        }
    }

    // ── 회귀 방지(2차) ────────────────────────────────────────────────────────

    /**
     * hospitals.jsp에서 `.pane-hidden { display:none }` 규칙이 모바일 한정
     * (`@media (max-width: 1023px)`) 안에 있어야 한다.
     * 데스크톱 grid에선 list/map이 grid-area로 분리되므로 pane-hidden을 전역으로 두면
     * 기본 마크업의 `class="map-area pane-hidden"`이 데스크톱에서도 지도를 숨겨버린다.
     */
    @Test
    void paneHiddenIsScopedToMobileMediaQuery() throws IOException {
        String jsp = read("WEB-INF/views/hospitals.jsp");

        // ① 모바일 미디어쿼리 시작점
        int mobileMqIdx = jsp.indexOf("@media (max-width: 1023px)");
        assertThat(mobileMqIdx)
                .as("@media (max-width: 1023px) 블록이 존재해야 한다")
                .isGreaterThan(-1);

        // ② `.pane-hidden { display` 형태의 셀렉터 정의는 jsp 전체에서 정확히 한 번(모바일 미디어쿼리 안)만 등장해야 한다.
        //    HTML 마크업의 `class="map-area pane-hidden"`은 점(.)이 없어 매치되지 않는다.
        //    여러 번 정의되면 전역에 추가됐다는 신호 → 데스크톱 지도 숨김 회귀.
        int count = 0;
        int idx = 0;
        while ((idx = jsp.indexOf(".pane-hidden { display", idx)) != -1) {
            count++;
            idx++;
        }
        assertThat(count)
                .as(".pane-hidden 셀렉터는 정확히 1회(모바일 미디어쿼리 안)만 정의되어야 한다")
                .isEqualTo(1);

        // ③ 정의 위치가 모바일 미디어쿼리 시작점 직후에 있어야 (200자 이내).
        int paneDefIdx = jsp.indexOf(".pane-hidden { display", mobileMqIdx);
        assertThat(paneDefIdx)
                .as(".pane-hidden 정의는 모바일 미디어쿼리 다음에 등장해야 한다")
                .isGreaterThan(mobileMqIdx);
        assertThat(paneDefIdx - mobileMqIdx)
                .as(".pane-hidden 정의는 모바일 미디어쿼리 블록 내부에 있어야 한다 (시작점 가까이)")
                .isLessThan(200);
    }

    /**
     * hospital.js의 togglePane은 데스크톱(≥1024px)에서 no-op이어야 한다.
     * 함수 본문에 viewport 가드(matchMedia 또는 동일 의도의 헬퍼)가 존재해야 한다.
     */
    @Test
    void togglePaneIsNoOpOnDesktop() throws IOException {
        String js = read("static/js/hospital.js");
        // 첫 번째 const togglePane 정의 이후 ~ 다음 const 까지의 블록에서 matchMedia(또는 _isDesktopViewport) 등장.
        int start = js.indexOf("const togglePane");
        assertThat(start).as("togglePane 정의가 존재해야 한다").isGreaterThan(-1);
        int nextConst = js.indexOf("\nconst ", start + 1);
        String body = nextConst > start ? js.substring(start, nextConst) : js.substring(start);
        assertThat(body)
                .as("togglePane는 데스크톱 viewport 가드를 가져야 한다")
                .containsAnyOf("_isDesktopViewport(", "matchMedia('(min-width: 1024px)'", "matchMedia(\"(min-width: 1024px)");
    }

    /**
     * map.js의 buildPinHtml은 외부 데이터(name)를 escapeHtml로 감싸 사용해야 한다.
     * 옛 코드는 `${shortName}`을 그대로 템플릿 리터럴에 넣어 XSS 위험이 있었다.
     */
    @Test
    void buildPinHtmlUsesEscapeHtml() throws IOException {
        String js = read("static/js/map.js");
        int start = js.indexOf("const buildPinHtml");
        assertThat(start).as("buildPinHtml 정의가 존재해야 한다").isGreaterThan(-1);
        int end = js.indexOf("\n};", start);
        String body = end > start ? js.substring(start, end) : js.substring(start);
        assertThat(body).contains("escapeHtml(");
        // name이 null이어도 .length 등에서 깨지지 않게 null-safe 변환 패턴 유지.
        assertThat(body).contains("String(name");
    }

    // ── 성능/비즈니스 개선 회귀 방지선 ────────────────────────────────────────

    /** api.js의 get/post/delete는 모두 {signal} 옵션을 받아야 한다. */
    @Test
    void apiJsAcceptsSignalOption() throws IOException {
        String js = read("static/js/api.js");
        assertThat(js).contains("async get(url, { signal } = {})");
        assertThat(js).contains("async post(url, data, { signal } = {})");
        assertThat(js).contains("async delete(url, { signal } = {})");
        // fetch 호출에도 signal 전달
        assertThat(js).contains("signal");
    }

    /** hospital.js는 단일 검색 API(/api/hospitals/search)를 호출하고 옛 다중 호출 함수는 제거. */
    @Test
    void hospitalJsUsesUnifiedSearchEndpoint() throws IOException {
        String js = read("static/js/hospital.js");
        assertThat(js).contains("/api/hospitals/search");
        // 옛 분할 호출 함수는 사라져야 한다.
        assertThat(js).doesNotContain("const searchByMultipleNpayCds");
    }

    /** AbortController 도입 — 검색/상세 fetch가 신호로 묶여야 race를 차단한다. */
    @Test
    void hospitalJsUsesAbortControllerForSearchAndDetail() throws IOException {
        String js = read("static/js/hospital.js");
        assertThat(js).contains("let _searchAbort");
        assertThat(js).contains("let _detailAbort");
        assertThat(js).contains("new AbortController");
        assertThat(js).contains("signal");
    }

    /** 위치 캐시는 sessionStorage 백업 + 타임아웃 4초로 단축. */
    @Test
    void geoCacheUsesSessionStorageAndShorterTimeout() throws IOException {
        String js = read("static/js/hospital.js");
        assertThat(js).contains("sessionStorage");
        assertThat(js).contains("timeout: 4000");
        assertThat(js).contains("maximumAge: 60000");
    }

    /** 즐겨찾기 캐시는 페이지 라이프타임 동안 1회 fetch + 토글 시 set 갱신. */
    @Test
    void favoritesStatesAreCachedAcrossCalls() throws IOException {
        String js = read("static/js/hospital.js");
        assertThat(js).contains("_favoritesYkihoSet");
    }

    /** 정렬 토글: localStorage + segmented control 렌더 + data-sort 마크업. */
    @Test
    void sortTabsAreWiredToBackendSortParam() throws IOException {
        String hospitalJs = read("static/js/hospital.js");
        assertThat(hospitalJs).contains("const renderWeightSlider");
        assertThat(hospitalJs).contains("_currentSort");
        assertThat(hospitalJs).contains("'mp.sort'");
        // /search 호출에 sort 파라미터 첨부
        assertThat(hospitalJs).contains("sort: _currentSort");

        String hospitalsJsp = read("WEB-INF/views/hospitals.jsp");
        assertThat(hospitalsJsp).contains("id=\"weight-slider\"");
    }

    /** 상세 progressive rendering — /basics, /extras 분리 호출. */
    @Test
    void hospitalDetailUsesProgressiveBasicsAndExtras() throws IOException {
        String js = read("static/js/hospital.js");
        assertThat(js).contains("/basics");
        assertThat(js).contains("/extras");
        assertThat(js).contains("_renderBasicsSection");
        assertThat(js).contains("_renderExtrasSection");
    }

    // ── 검색 v2 UX 보강 회귀 방지선 ────────────────────────────────────────

    /** 카드에 matchedNpayKorNm 라벨이 렌더된다 ("MRI 척추 기준"). */
    @Test
    void hospitalCardShowsMatchedNpayKorNm() throws IOException {
        String js = read("static/js/hospital.js");
        assertThat(js).contains("matchedNpayKorNm");
        assertThat(js).contains("기준");
        // 외부 데이터이므로 escapeHtml 필수.
        Pattern escaped = Pattern.compile(
                "escapeHtml\\s*\\(\\s*hospital\\.matchedNpayKorNm\\s*\\)",
                Pattern.CASE_INSENSITIVE);
        assertThat(escaped.matcher(js).find())
                .as("matchedNpayKorNm은 escapeHtml로 감싸야 한다")
                .isTrue();
    }

    /** getCurrentPosition은 fromFallback 플래그를 노출하고, 호출처가 배너로 안내한다. */
    @Test
    void geoFallbackExposesFlagAndShowsBanner() throws IOException {
        String js = read("static/js/hospital.js");
        assertThat(js).contains("fromFallback");
        assertThat(js).contains("notifyGeoFallback");
        assertThat(js).contains("retryGeoLocation");

        String jsp = read("WEB-INF/views/hospitals.jsp");
        assertThat(jsp).contains("id=\"geo-fallback-notice\"");
        assertThat(jsp).contains("retryGeoLocation()");
    }

    /**
     * 회귀 방지: 카드에는 inline {@code onclick="${onclick}"} 또는 즐겨찾기 버튼의
     * inline onclick이 다시 들어오면 안 된다. 키워드/ykiho에 ' ( ) 가 섞이면 attribute JS가
     * 깨지거나 XSS로 이어지므로 모든 click 처리는 위임 리스너 + data-* 속성으로 한다.
     */
    @Test
    void hospitalCardUsesDelegatedClickInsteadOfInlineOnclick() throws IOException {
        String js = read("static/js/hospital.js");
        // renderHospitalCard가 더 이상 inline onclick을 조립하면 안 된다.
        assertThat(js)
                .as("renderHospitalCard에 inline onclick 문자열 조립이 다시 들어오면 안 된다")
                .doesNotContain("onclick=\"${onclick}\"")
                .doesNotContain("onclick=\"handleFavoriteClick(")
                .doesNotContain("const onclick = `showHospitalInPanel(");
        // data-* 직렬화 + 위임 리스너 식별자가 있어야 한다.
        assertThat(js).contains("data-distance=");
        assertThat(js).contains("data-lat=");
        assertThat(js).contains("data-lng=");
        assertThat(js).contains("_bindHospitalListClicks");
        // 위임 리스너 내부에서 즐겨찾기 버튼 → 카드 진입 차단 분기가 있어야 한다.
        assertThat(js).contains("event.target.closest('.fav-btn')");
        assertThat(js).contains("event.target.closest('.hospital-card')");
    }

    /**
     * 카드에 종별 평균 대비 라벨이 출력된다. 평균이 없으면 라벨 자체가 사라지도록 가드돼야 한다.
     * 음수(평균보다 쌈)와 양수(평균보다 비쌈)에서 색을 다르게 잡고, 절댓값 % 표시.
     */
    @Test
    void hospitalCardShowsStatLabelGuardedByAvgAndDiff() throws IOException {
        String js = read("static/js/hospital.js");
        // 가드: avgAmt + diffPct 둘 다 있어야 라벨 생성
        assertThat(js).contains("hospital.avgAmt != null && hospital.diffPct != null");
        // 부호별 색상 분기 + '종별 평균 대비' 텍스트
        assertThat(js).contains("text-emerald-600");
        assertThat(js).contains("text-rose-600");
        assertThat(js).contains("종별 평균 대비");
        // 카드 렌더에 statLabel 슬롯이 실제로 박혀야 함
        assertThat(js).contains("${statLabel}");
    }

    /**
     * mixed 모드 가중치 슬라이더가 등록되고, wPrice/wDistance가 검색 URL에 실린다.
     * <ul>
     *   <li>localStorage 키 + 기본값 상수가 hospital.js에 존재해야 한다.</li>
     *   <li>renderWeightSlider가 정렬 토글과 함께 호출돼야 mixed→다른 모드 전환 시 슬라이더가 사라진다.</li>
     *   <li>JSP는 슬라이더 컨테이너(#weight-slider)를 가져야 한다.</li>
     *   <li>searchHospitalsByNpayCds는 mixed 모드에서만 wPrice/wDistance를 URL에 set 해야 한다.</li>
     * </ul>
     */
    @Test
    void mixedSortExposesWeightSliderAndForwardsParams() throws IOException {
        String js = read("static/js/hospital.js");
        assertThat(js).contains("WEIGHT_STORAGE_KEY");
        assertThat(js).contains("WEIGHT_DEFAULT");
        assertThat(js).contains("renderWeightSlider");
        // searchHospitalsByNpayCds가 wPrice/wDistance를 mixed 모드일 때만 set
        assertThat(js).contains("if (_currentSort === 'mixed')")
                .contains("params.set('wPrice'")
                .contains("params.set('wDistance'");

        String jsp = read("WEB-INF/views/hospitals.jsp");
        assertThat(jsp).contains("id=\"weight-slider\"");
    }

    /**
     * Geolocation 정확도가 LOW_ACCURACY_THRESHOLD_M을 초과하면 fallback과 같이 배너를 띄우고,
     * 메시지에 ±NNNm을 노출한다.
     */
    @Test
    void lowAccuracyTriggersBannerWithMeterReadout() throws IOException {
        String js = read("static/js/hospital.js");
        // 임계값 상수 + 라이브 정확도 캡처 + 메시지 본문 패턴
        assertThat(js).contains("LOW_ACCURACY_THRESHOLD_M");
        assertThat(js).contains("pos.coords.accuracy");
        // sessionStorage에는 좌표 미저장 — CodeQL clear-text-storage 회귀 방지.
        assertThat(js)
                .as("민감 좌표를 sessionStorage에 다시 저장하면 안 된다")
                .doesNotContain("JSON.stringify({ lat, lng,");
        // 배너 메시지에 정확도 m 단위 표기
        assertThat(js).contains("±${m}m");

        // JSP는 data-geo-message 위치를 가진 <p>를 둬야 JS가 innerHTML로 갱신할 수 있다.
        String jsp = read("WEB-INF/views/hospitals.jsp");
        assertThat(jsp).contains("data-geo-message");
    }

    /** scoreMatch 우선순위 함수가 common.js에 정의되고, 검색 두 경로에서 활용된다. */
    @Test
    void scoreMatchPrioritizesExactPrefixAndPopular() throws IOException {
        String common = read("static/js/common.js");
        assertThat(common).contains("const scoreMatch");
        // attachSuggestionInput가 scoreMatch를 활용
        assertThat(common).contains("scoreMatch(");

        String hospital = read("static/js/hospital.js");
        // resolveNpayCds도 scoreMatch를 활용
        assertThat(hospital).contains("scoreMatch(");
        assertThat(hospital).contains("POPULAR_KEYWORDS");
    }
}
