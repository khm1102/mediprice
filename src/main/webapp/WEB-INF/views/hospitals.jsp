<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="병원 검색" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<script type="text/javascript"
    src="https://oapi.map.naver.com/openapi/v3/maps.js?ncpKeyId=${naverMapKey}"></script>
<script src="<c:url value='/static/js/MarkerClustering.js'/>?v=20260607-ux5"></script>
<script defer src="<c:url value='/static/js/map.js'/>?v=20260607-ux5"></script>
<script defer src="<c:url value='/static/js/hospital.js'/>?v=20260607-ux5"></script>

<style>
    html, body { overflow: hidden; height: 100%; }
    main { height: calc(100vh - 3.5rem); min-height: 0; overflow: hidden; }

    /* ── 모바일 기본 (≤1023px): 검색바 / 지도 / 목록 — 지도+목록 동시 노출 ── */
    .hospitals-grid {
        display: grid;
        grid-template-columns: 1fr;
        grid-template-rows: auto 55vw 1fr;
        grid-template-areas:
            "search"
            "map"
            "list";
        height: 100%;
    }
    .main-stack { display: contents; }
    .map-area   { grid-area: map; position: relative; overflow: hidden; }
    #panel-list { grid-area: list; position: relative; overflow-y: auto; }

    /* 모바일에서 pane 토글 불필요 — 항상 둘 다 보임 */
    @media (max-width: 1023px) {
        .pane-hidden { display: block !important; }
    }

    /* segmented control 모바일에서 숨김 */
    #mobile-pane-tabs { display: none; }

    /* ── 모바일 상세: 하단 시트 (목록 영역 위를 덮음) ── */
    #panel-detail {
        position: fixed;
        left: 0; right: 0; bottom: 0;
        top: auto;
        height: 70dvh;
        z-index: 200;
        background: #F2F4F6;
        border-radius: 20px 20px 0 0;
        box-shadow: 0 -4px 24px rgba(0,0,0,0.13);
        overflow-y: auto;
        -webkit-overflow-scrolling: touch;
        transform: translateY(110%);
        transition: transform 0.32s cubic-bezier(.4,0,.2,1);
    }
    #panel-detail.open { transform: translateY(0); }

    #pd-backdrop {
        display: none;
        position: fixed; inset: 0;
        background: rgba(0,0,0,0.25);
        z-index: 199;
    }
    #pd-backdrop.open { display: block; }

    /* ── 데스크톱 lg (1024~1279px): 좌측 360 + 지도 가변, 상세는 overlay 모달 ── */
    @media (min-width: 1024px) {
        #mobile-pane-tabs { display: none; }
        .hospitals-grid {
            grid-template-columns: 360px 1fr;
            grid-template-rows: auto 1fr;
            grid-template-areas:
                "search map"
                "list   map";
            height: 100%;
        }
        .panel-left {
            position: relative;
            z-index: 10;
            box-shadow: 4px 0 20px rgba(0, 0, 0, 0.07);
        }
        /* 데스크톱에선 .main-stack 자체는 사용하지 않고 list/map이 각 grid-area로 배치된다. */
        .main-stack { display: contents; }
        #panel-list { grid-area: list; position: relative; inset: auto; overflow-y: auto; }
        .map-area   { grid-area: map;  position: relative; inset: auto; border-bottom: none; }

        /* 상세 패널: lg에선 화면 중앙 overlay 모달 — 지도/목록을 침범하지 않는다. */
        #panel-detail {
            position: fixed;
            top: 50%; left: 50%; right: auto; bottom: auto;
            transform: translate(-50%, -50%) scale(0.98);
            width: min(640px, calc(100vw - 4rem));
            max-height: 80dvh;
            height: auto;
            border-radius: 20px;
            background: #F2F4F6;
            box-shadow: 0 24px 60px rgba(0,0,0,0.22);
            opacity: 0;
            pointer-events: none;
            transition: opacity 0.2s ease, transform 0.22s cubic-bezier(.4,0,.2,1);
            z-index: 210;
        }
        #panel-detail.open {
            opacity: 1;
            transform: translate(-50%, -50%) scale(1);
            pointer-events: auto;
        }
        /* lg에서는 backdrop을 다시 보여줘 overlay로 동작 */
        #pd-backdrop { display: block; opacity: 0; pointer-events: none; transition: opacity 0.2s ease; }
        #pd-backdrop.open { opacity: 1; pointer-events: auto; }
    }

    /* ── 모바일: 슬라이더 compact -─ */
    @media (max-width: 1023px) {
        #weight-slider { padding: 0 0 4px; }
        #weight-slider .slider-label { font-size: 10px; }
    }

    /* ── 데스크톱 xl (1280px+): 리스트 오른쪽에 붙는 슬라이드 패널 ── */
    @media (min-width: 1280px) {
        #panel-detail {
            position: fixed;
            top: 3.5rem; left: 360px; bottom: 0; right: auto;
            width: 380px;
            max-height: none;
            height: auto;
            border-radius: 0;
            background: #F2F4F6;
            box-shadow: 4px 0 20px rgba(0,0,0,0.10);
            opacity: 0;
            pointer-events: none;
            transform: translateX(-12px);
            transition: transform 0.28s cubic-bezier(.4,0,.2,1), opacity 0.2s ease;
            z-index: 210;
        }
        #panel-detail.open {
            transform: translateX(0);
            opacity: 1;
            pointer-events: auto;
        }
        #pd-backdrop { display: none !important; }
    }
</style>

<div class="hospitals-grid">

    <%-- 검색바 + 자동완성 dropdown + 인기 칩 + 모바일 segmented control --%>
    <div style="grid-area: search;" class="panel-left bg-white border-b border-gray-200 flex-shrink-0">
        <div class="px-4 pt-3 flex gap-2 relative">
            <div class="flex-1 flex items-center gap-2 bg-[#F2F4F6] rounded-xl px-3.5 focus-within:ring-2 focus-within:ring-[#2563EB]/30 transition-all">
                <svg class="w-4 h-4 text-gray-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                </svg>
                <input type="text" id="search-input"
                    placeholder="진료 항목을 입력하세요"
                    autocomplete="off"
                    aria-autocomplete="list"
                    aria-controls="search-suggestions"
                    class="flex-1 bg-transparent text-sm text-gray-800 placeholder-gray-400 focus:outline-none py-2.5"/>
            </div>
            <button onclick="handleSearch()"
                class="bg-[#2563EB] text-white px-5 rounded-xl text-sm font-semibold hover:bg-blue-700 transition-colors min-h-[44px] flex-shrink-0">
                검색
            </button>

            <%-- 자동완성 dropdown — input 바로 아래 절대 위치 --%>
            <div id="search-suggestions"
                 role="listbox"
                 class="hidden fixed z-[500] bg-white rounded-xl border border-gray-200 overflow-hidden max-h-64 overflow-y-auto"
                 style="box-shadow: 0 8px 24px rgba(0,0,0,0.10);"></div>
        </div>

        <%-- 위치 fallback / 낮은 정확도 안내 — notifyGeoFallback이 메시지 문구를 동적으로 갱신. --%>
        <div id="geo-fallback-notice"
             class="hidden mx-4 mt-2 px-3 py-2 bg-amber-50 border border-amber-100 rounded-lg flex items-start gap-2.5">
            <svg class="w-3.5 h-3.5 text-amber-400 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
            </svg>
            <p data-geo-message class="flex-1 text-[11px] text-amber-700 leading-relaxed">
                현재 위치를 가져오지 못해 <strong>서울 시청</strong> 기준으로 검색 중이에요.
                지도를 옮겨 재검색하거나 위치 권한을 허용해 다시 시도해보세요.
            </p>
            <button type="button" onclick="retryGeoLocation()"
                    class="flex-shrink-0 text-[11px] font-semibold text-amber-700 hover:underline whitespace-nowrap">
                다시 시도
            </button>
        </div>

        <%-- 정렬 토글: [추천 | 가격순 | 가까운 순]. 백엔드 /api/hospitals/search 의 sort 파라미터에 매핑. --%>
        <%-- 가중치 슬라이더(mixed 모드 전용)는 renderWeightSlider가 #weight-slider에 주입. --%>
        <div class="px-4 pt-1.5 pb-2.5">
            <div id="weight-slider"></div>
        </div>

        <%-- 모바일 segmented control: 목록 / 지도 토글 (데스크톱에선 hidden) --%>
        <div id="mobile-pane-tabs" class="px-4 pb-3 gap-1.5">
            <button type="button" data-pane="list" onclick="togglePane('list')"
                    class="pane-tab flex-1 h-9 rounded-lg text-xs font-semibold bg-[#2563EB] text-white transition-colors"
                    aria-pressed="true">
                목록
            </button>
            <button type="button" data-pane="map" onclick="togglePane('map')"
                    class="pane-tab flex-1 h-9 rounded-lg text-xs font-semibold bg-gray-100 text-gray-500 hover:bg-gray-200 transition-colors"
                    aria-pressed="false">
                지도
            </button>
        </div>
    </div>

    <%-- 모바일에선 list/map이 같은 영역에서 토글, 데스크톱에선 grid-area로 분리 --%>
    <div class="main-stack">

    <%-- 지도 --%>
    <div class="map-area pane-hidden">
        <div id="map" class="w-full h-full" style="background: #e8edf5;"></div>

        <%-- 이 지역에서 재검색 버튼 --%>
        <div id="map-research-btn"
             class="hidden absolute top-3 z-10 pointer-events-none"
             style="left: 50%; transform: translateX(-50%);">
            <button onclick="handleReSearch()"
                class="pointer-events-auto bg-white text-sm font-semibold text-gray-700 px-4 py-2 rounded-full
                       flex items-center gap-1.5 hover:bg-gray-50 active:bg-gray-100 transition-colors select-none whitespace-nowrap"
                style="box-shadow: 0 2px 16px rgba(0,0,0,0.18);">
                <svg class="w-3.5 h-3.5 text-gray-500 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
                </svg>
                이 지역에서 재검색
            </button>
        </div>

        <%-- 현재 위치로 이동 버튼 --%>
        <button onclick="recenterMap()"
            title="현재 위치로 이동"
            class="absolute bottom-4 right-4 z-10 bg-white rounded-full w-11 h-11 flex items-center justify-center hover:bg-gray-50 transition-colors"
            style="box-shadow: 0 2px 12px rgba(0,0,0,0.15);">
            <svg class="w-4 h-4 text-[#2563EB]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M12 8c-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4-1.79-4-4-4zm8.94 3A8.994 8.994 0 0013 3.06V1h-2v2.06A8.994 8.994 0 003.06 11H1v2h2.06A8.994 8.994 0 0011 20.94V23h2v-2.06A8.994 8.994 0 0020.94 13H23v-2h-2.06z"></path>
            </svg>
        </button>
    </div>

    <%-- 병원 목록 패널 --%>
    <div id="panel-list" class="panel-left bg-[#F2F4F6]">

        <%-- 검색 안내 (키워드 없이 진입) --%>
        <div id="state-prompt" class="absolute inset-0 flex flex-col items-center justify-center text-center px-8">
            <div class="w-14 h-14 bg-white rounded-2xl flex items-center justify-center mb-4" style="box-shadow: 0 2px 10px rgba(0,0,0,0.08);">
                <svg class="w-6 h-6 text-[#2563EB]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                </svg>
            </div>
            <p class="text-sm font-semibold text-gray-700">진료 항목을 검색해보세요</p>
            <p class="text-xs text-gray-400 mt-1.5 leading-relaxed">도수치료, MRI, 초음파 등<br>비급여 항목을 입력하면<br>내 주변 병원 가격을 비교할 수 있어요</p>
        </div>

        <%-- 로딩 --%>
        <div id="state-loading" class="hidden absolute inset-0 flex flex-col items-center justify-center text-gray-400">
            <div class="w-5 h-5 border-2 border-[#2563EB] border-t-transparent rounded-full animate-spin mb-3"></div>
            <p class="text-sm">병원 정보를 불러오는 중...</p>
        </div>

        <%-- 검색 결과 없음 --%>
        <div id="state-empty" class="hidden absolute inset-0 flex flex-col items-center justify-center text-center px-6">
            <div class="w-14 h-14 bg-white rounded-2xl flex items-center justify-center mb-4" style="box-shadow: 0 2px 10px rgba(0,0,0,0.08);">
                <svg class="w-6 h-6 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                </svg>
            </div>
            <p class="text-sm font-semibold text-gray-600">검색 결과가 없어요</p>
            <p class="text-xs text-gray-400 mt-1">다른 검색어로 다시 시도해보세요</p>

            <%-- 추천 칩 (같은 중분류의 다른 항목 or 인기 항목 fallback) --%>
            <p class="text-[11px] text-gray-500 font-medium mt-5 mb-2">이런 항목은 어떠세요?</p>
            <div id="state-empty-chips" class="flex flex-wrap gap-1.5 justify-center max-w-xs"></div>
        </div>

        <%-- 오류 --%>
        <div id="state-error" class="hidden absolute inset-0 flex flex-col items-center justify-center text-center px-8">
            <div class="w-14 h-14 bg-white rounded-2xl flex items-center justify-center mb-4" style="box-shadow: 0 2px 10px rgba(0,0,0,0.08);">
                <svg class="w-6 h-6 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"></path>
                    <path stroke-linecap="round" stroke-width="1.5" d="M12 10v3.5"></path>
                    <circle cx="12" cy="16.5" r="0.7" fill="currentColor" stroke="none"></circle>
                </svg>
            </div>
            <p class="text-sm font-semibold text-gray-600">데이터를 불러오지 못했습니다</p>
            <p class="text-xs text-gray-400 mt-1">잠시 후 다시 시도해주세요</p>
            <button onclick="fetchHospitals(document.getElementById('search-input').value)"
                class="mt-4 text-xs text-[#2563EB] font-medium hover:underline min-h-[44px] px-2">다시 시도</button>
        </div>

        <%-- 병원 카드 목록 --%>
        <div id="hospital-list" class="hidden p-3 space-y-2 h-full overflow-y-auto"></div>

    </div>

    </div>  <%-- /main-stack --%>

</div>

<%-- 상세 패널 backdrop (모바일/데스크톱 lg overlay 공용) --%>
<div id="pd-backdrop" onclick="showHospitalList()"></div>

<%-- 병원 상세 패널 --%>
<div id="panel-detail">

    <%-- 모바일: 드래그 핸들 --%>
    <div class="lg:hidden flex justify-center pt-3 pb-1">
        <div class="w-10 h-1 bg-gray-300 rounded-full"></div>
    </div>

    <%-- 상세 로딩 --%>
    <div id="pd-loading" class="flex flex-col items-center justify-center py-20 text-gray-400">
        <div class="w-5 h-5 border-2 border-[#2563EB] border-t-transparent rounded-full animate-spin mb-3"></div>
        <p class="text-sm">병원 정보를 불러오는 중...</p>
    </div>

    <%-- 상세 오류 --%>
    <div id="pd-error" class="hidden flex-col items-center justify-center py-20">
        <p class="text-sm font-semibold text-gray-600">정보를 불러오지 못했습니다</p>
        <button onclick="showHospitalList()" class="mt-3 text-xs text-[#2563EB] font-medium hover:underline min-h-[44px] px-2">닫기</button>
    </div>

    <%-- 상세 콘텐츠 --%>
    <div id="pd-content" class="hidden">
        <div class="px-4 py-4 space-y-3">

            <%-- 닫기 버튼 --%>
            <div class="flex items-center justify-between">
                <p class="text-xs text-gray-400">병원 상세</p>
                <button onclick="showHospitalList()"
                    class="w-8 h-8 flex items-center justify-center rounded-full hover:bg-gray-200 transition-colors text-gray-400 hover:text-gray-700">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                    </svg>
                </button>
            </div>

                <%-- 병원 헤더 카드 --%>
                <div class="bg-white rounded-2xl p-5" style="box-shadow: 0 2px 12px rgba(0,0,0,0.07);">
                    <div class="flex items-start justify-between gap-3 mb-4">
                        <div class="flex-1 min-w-0">
                            <h2 id="pd-name" class="text-base font-bold text-gray-900 mb-0.5 leading-snug"></h2>
                            <p id="pd-type" class="text-sm text-gray-400 mb-2"></p>
                            <p id="pd-address" class="text-sm text-gray-500 leading-relaxed"></p>
                        </div>
                        <div class="flex flex-row items-center gap-1.5 flex-shrink-0">
                            <span id="pd-distance" class="text-sm font-semibold text-[#2563EB] bg-blue-50 px-2.5 py-1 rounded-lg"></span>
                            <button id="pd-fav-btn"
                                    data-ykiho=""
                                    data-favorited="false"
                                    onclick="handleFavoriteClick(this.dataset.ykiho, this, event)"
                                    class="fav-btn p-1.5 rounded-xl transition-colors text-gray-300 hover:text-yellow-400 hover:bg-yellow-50"
                                    title="즐겨찾기 추가">
                                <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
                                    <path d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0
                                             00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0
                                             00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1
                                             1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1
                                             1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0
                                             00.951-.69l1.519-4.674z"></path>
                                </svg>
                            </button>
                        </div>
                    </div>
                    <div class="border-t border-gray-100 pt-4 space-y-2.5">
                        <a id="pd-phone" href="#" class="flex items-center gap-2.5 text-sm text-[#2563EB] font-medium hover:underline min-h-[32px]">
                            <div class="w-7 h-7 bg-blue-50 rounded-lg flex items-center justify-center flex-shrink-0">
                                <svg class="w-3.5 h-3.5 text-[#2563EB]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z"></path>
                                </svg>
                            </div>
                        </a>

                        <%-- 길찾기 --%>
                        <a id="pd-directions" href="#" target="_blank" rel="noopener noreferrer"
                           class="flex items-center gap-2.5 text-sm text-gray-600 font-medium hover:text-[#2563EB] transition-colors min-h-[32px]">
                            <div class="w-7 h-7 bg-gray-50 rounded-lg flex items-center justify-center flex-shrink-0">
                                <svg class="w-3.5 h-3.5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                                        d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7"></path>
                                </svg>
                            </div>
                            네이버 지도로 길찾기
                        </a>
                        <div data-field="pd-url" class="hidden" style="display:none">
                            <div class="flex items-center gap-2.5">
                                <div class="w-7 h-7 bg-gray-50 rounded-lg flex items-center justify-center flex-shrink-0">
                                    <svg class="w-3.5 h-3.5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1"></path>
                                    </svg>
                                </div>
                                <a id="pd-url" href="#" target="_blank" rel="noopener noreferrer"
                                   class="text-sm text-[#2563EB] hover:underline truncate max-w-[220px]"></a>
                            </div>
                        </div>
                    </div>
                </div>

                <%-- 안내 문구 --%>
                <div class="bg-amber-50 border border-amber-100 rounded-2xl px-4 py-3 flex gap-2.5">
                    <svg class="w-4 h-4 text-amber-400 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                    </svg>
                    <p class="text-xs text-amber-700 leading-relaxed">
                        병원마다 시술 방법·시간이 다를 수 있어요.<br>
                        가격은 참고용이니 병원에 직접 문의해 주세요.
                    </p>
                </div>

                <%-- 검색 항목 가격 카드 (검색 키워드 매칭 시만 노출) --%>
                <div id="pd-section-search-price" class="hidden bg-gradient-to-br from-blue-50 to-white border border-blue-100 rounded-2xl p-5">
                    <p class="text-[11px] font-semibold text-[#2563EB] uppercase tracking-wide">내 검색 항목 가격</p>
                    <p id="pd-search-item-name" class="text-sm text-gray-700 mt-1.5 truncate"></p>
                    <p id="pd-search-item-price" class="text-2xl font-bold text-[#2563EB] mt-1"></p>
                </div>

                <%-- 비급여 진료비 (가격이 최상단, 항상 펼침) --%>
                <div class="bg-white rounded-2xl p-5" style="box-shadow: 0 2px 12px rgba(0,0,0,0.07);">
                    <h3 class="text-sm font-semibold text-gray-700 mb-4">비급여 진료비</h3>
                    <div id="pd-price-loading" class="flex items-center justify-center gap-2 py-8 text-gray-400">
                        <div class="w-4 h-4 border-2 border-[#2563EB] border-t-transparent rounded-full animate-spin"></div>
                        <span class="text-xs">진료비 정보를 불러오는 중...</span>
                    </div>
                    <div id="pd-price-empty" class="hidden text-center py-8 text-gray-400 text-sm">등록된 비급여 진료비 정보가 없습니다</div>
                    <table id="pd-price-table" class="w-full hidden" style="table-layout:fixed;">
                        <colgroup>
                            <col style="width:auto;">
                            <col style="width:80px;">
                        </colgroup>
                        <thead>
                            <tr class="text-left text-xs text-gray-400 border-b border-gray-200">
                                <th class="pb-3 font-medium">항목명</th>
                                <th class="pb-3 font-medium text-right">가격</th>
                            </tr>
                        </thead>
                        <tbody id="pd-price-tbody" class="divide-y divide-gray-100"></tbody>
                    </table>
                </div>

                <%-- 부가 정보 — 접이식 details 그룹 (가격 비교 동선을 가장 짧게) --%>
                <details id="pd-section-dgsbjt" class="hidden bg-white rounded-2xl group" style="box-shadow: 0 2px 12px rgba(0,0,0,0.07);">
                    <summary class="cursor-pointer list-none p-5 flex items-center justify-between text-sm font-semibold text-gray-700">
                        진료과목
                        <svg class="w-4 h-4 text-gray-400 transition-transform group-open:rotate-180" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
                        </svg>
                    </summary>
                    <div id="pd-dgsbjt-list" class="flex flex-wrap gap-1.5 px-5 pb-5"></div>
                </details>

                <details id="pd-section-medoft" class="hidden bg-white rounded-2xl group" style="box-shadow: 0 2px 12px rgba(0,0,0,0.07);">
                    <summary class="cursor-pointer list-none p-5 flex items-center justify-between text-sm font-semibold text-gray-700">
                        의료장비
                        <svg class="w-4 h-4 text-gray-400 transition-transform group-open:rotate-180" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
                        </svg>
                    </summary>
                    <div id="pd-medoft-list" class="space-y-1 px-5 pb-5"></div>
                </details>

                <details id="pd-section-operating" class="hidden bg-white rounded-2xl group" style="box-shadow: 0 2px 12px rgba(0,0,0,0.07);">
                    <summary class="cursor-pointer list-none p-5 flex items-center justify-between text-sm font-semibold text-gray-700">
                        진료시간
                        <svg class="w-4 h-4 text-gray-400 transition-transform group-open:rotate-180" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
                        </svg>
                    </summary>
                    <div id="pd-operating-list" class="space-y-1 px-5 pb-5"></div>
                </details>

                <details id="pd-section-parking" class="hidden bg-white rounded-2xl group" style="box-shadow: 0 2px 12px rgba(0,0,0,0.07);">
                    <summary class="cursor-pointer list-none p-5 flex items-center justify-between text-sm font-semibold text-gray-700">
                        주차 정보
                        <svg class="w-4 h-4 text-gray-400 transition-transform group-open:rotate-180" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
                        </svg>
                    </summary>
                    <div id="pd-parking-list" class="space-y-1 px-5 pb-5"></div>
                </details>


            </div>
        </div>
    </div>

<script>
    // ── 이 지역에서 재검색 ──
    const showReSearchBtn = () => {
        document.getElementById('map-research-btn')?.classList.remove('hidden');
    };
    const hideReSearchBtn = () => {
        document.getElementById('map-research-btn')?.classList.add('hidden');
    };
    const handleReSearch = () => {
        const center = getMapCenter?.();
        if (!center) return;
        const keyword = document.getElementById('search-input').value.trim();
        if (!keyword) return;
        fetchHospitalsByLocation(center.lat, center.lng, keyword);
    };

    // ── 검색 ──
    const handleSearch = () => {
        const keyword = document.getElementById('search-input').value.trim();
        if (!keyword) return;
        window.location.href = '/hospitals?keyword=' + encodeURIComponent(keyword);
    };

    document.getElementById('search-input').addEventListener('keydown', (e) => {
        if (e.key === 'Enter') handleSearch();
    });

    document.addEventListener('DOMContentLoaded', () => {
        const keyword = new URLSearchParams(location.search).get('keyword') || '';
        document.getElementById('search-input').value = keyword;
        // 인기 칩 + 자동완성 dropdown은 검색 결과와 무관하게 비동기로 채워둔다.
        initSearchUx();
        fetchHospitals(keyword);
    });
</script>

</main>
</body>
</html>
