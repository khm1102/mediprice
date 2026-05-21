<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="즐겨찾기" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<script type="text/javascript"
    src="https://oapi.map.naver.com/openapi/v3/maps.js?ncpKeyId=${naverMapKey}"></script>
<script src="<c:url value="/static/js/MarkerClustering.js"/>"></script>
<script defer src="<c:url value="/static/js/map.js"/>"></script>
<script defer src="<c:url value="/static/js/hospital.js"/>"></script>
<script defer src="<c:url value="/static/js/favorites.js"/>"></script>

<style>
    html, body { overflow: hidden; height: 100%; }
    main { height: calc(100vh - 3.5rem); min-height: 0; overflow: hidden; }

    .favorites-grid {
        display: grid;
        grid-template-columns: 1fr;
        grid-template-rows: auto 40vh 1fr;
        grid-template-areas:
            "header"
            "map"
            "list";
        height: 100%;
    }
    .list-area  { overflow-y: auto; }
    .map-area   { border-bottom: 2px solid #E5E7EB; position: relative; }

    /* 지도 인터랙션 차단 오버레이 */
    #map-overlay {
        position: absolute;
        inset: 0;
        z-index: 10;
        cursor: default;
    }

    /* 상세 패널: 모바일 */
    #panel-detail {
        position: fixed;
        left: 0; right: 0; bottom: 0;
        top: 3.5rem;
        z-index: 300;
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
        z-index: 299;
    }
    #pd-backdrop.open { display: block; }

    @media (min-width: 1024px) {
        .favorites-grid {
            grid-template-columns: 400px 1fr;
            grid-template-rows: auto 1fr;
            grid-template-areas:
                "header map"
                "list   map";
        }
        .panel-left {
            position: relative;
            z-index: 10;
            box-shadow: 4px 0 20px rgba(0,0,0,0.07);
        }
        .map-area { border-bottom: none; }

        /* 상세 패널: 데스크탑 */
        #panel-detail {
            position: fixed;
            top: 3.5rem; left: 400px; bottom: 0;
            right: auto;
            width: 380px;
            border-radius: 0;
            box-shadow: 4px 0 20px rgba(0,0,0,0.12);
            transform: translateX(-110%);
            transition: transform 0.32s cubic-bezier(.4,0,.2,1);
            z-index: 20;
        }
        #panel-detail.open { transform: translateX(0); }
        #pd-backdrop { display: none !important; }
        #panel-detail { z-index: 50; }
    }
</style>

<div class="favorites-grid">

    <%-- 헤더 --%>
    <div style="grid-area: header;" class="panel-left bg-white border-b border-gray-200 px-4 py-3 flex items-center justify-between flex-shrink-0">
        <h1 class="text-base font-bold text-gray-900">즐겨찾기</h1>
        <span id="favorite-count" class="hidden text-sm text-gray-400 font-medium"></span>
    </div>

    <%-- 지도 (읽기 전용) --%>
    <div style="grid-area: map;" class="map-area">
        <div id="map" class="w-full h-full" style="background: #e8edf5;"></div>
        <div id="map-overlay"></div>
    </div>

    <%-- 즐겨찾기 목록 패널 --%>
    <div id="panel-list" style="grid-area: list; position: relative;" class="panel-left list-area bg-[#F2F4F6]">

        <%-- 비로그인 --%>
        <div id="not-logged-in" class="hidden absolute inset-0 flex flex-col items-center justify-center text-center px-8">
            <div class="w-14 h-14 bg-white rounded-2xl flex items-center justify-center mb-4"
                 style="box-shadow: 0 2px 10px rgba(0,0,0,0.08);">
                <svg class="w-6 h-6 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                          d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z"></path>
                </svg>
            </div>
            <p class="text-sm font-semibold text-gray-700 mb-1">로그인이 필요합니다</p>
            <p class="text-xs text-gray-400 mb-5">즐겨찾기를 이용하려면 로그인해주세요.</p>
            <a href="<c:url value="/auth/oauth2/authorize/google"/>"
               class="inline-flex items-center gap-2 px-5 py-2.5 bg-[#2563EB] text-white text-sm font-semibold rounded-xl hover:bg-blue-700 transition-colors">
                구글로 로그인
            </a>
        </div>

        <%-- 로딩 --%>
        <div id="loading" class="absolute inset-0 flex flex-col items-center justify-center text-gray-400">
            <div class="w-5 h-5 border-2 border-[#2563EB] border-t-transparent rounded-full animate-spin mb-3"></div>
            <p class="text-sm">즐겨찾기를 불러오는 중...</p>
        </div>

        <%-- 즐겨찾기 없음 --%>
        <div id="empty-state" class="hidden absolute inset-0 flex flex-col items-center justify-center text-center px-8">
            <div class="w-14 h-14 bg-white rounded-2xl flex items-center justify-center mb-4"
                 style="box-shadow: 0 2px 10px rgba(0,0,0,0.08);">
                <svg class="w-6 h-6 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                          d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0
                             00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0
                             00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1
                             1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1
                             1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0
                             00.951-.69l1.519-4.674z"></path>
                </svg>
            </div>
            <p class="text-sm font-semibold text-gray-700">즐겨찾기한 병원이 없습니다</p>
            <p class="text-xs text-gray-400 mt-1.5 leading-relaxed">병원 검색 후 별 아이콘을<br>눌러 저장해보세요</p>
            <a href="<c:url value="/hospitals"/>"
               class="mt-5 inline-block px-5 py-2.5 bg-[#2563EB] text-white text-sm font-semibold rounded-xl hover:bg-blue-700 transition-colors">
                병원 검색하기
            </a>
        </div>

        <%-- 즐겨찾기 카드 목록 --%>
        <div id="favorites-list" class="hidden p-3 space-y-2"></div>

    </div>

</div>

<%-- 백드롭 (모바일 전용) --%>
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
                                data-favorited="true"
                                onclick="handleFavoritesRemove(this.dataset.ykiho, event)"
                                class="fav-btn p-1.5 rounded-xl transition-colors text-yellow-400 hover:text-yellow-500 hover:bg-yellow-50"
                                title="즐겨찾기 해제">
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
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                                    d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z"></path>
                            </svg>
                        </div>
                    </a>
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
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                                        d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1"></path>
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

            <%-- 진료과목 --%>
            <div id="pd-section-dgsbjt" class="hidden bg-white rounded-2xl p-5" style="box-shadow: 0 2px 12px rgba(0,0,0,0.07);">
                <h3 class="text-sm font-semibold text-gray-700 mb-3">진료과목</h3>
                <div id="pd-dgsbjt-list" class="flex flex-wrap gap-1.5"></div>
            </div>

            <%-- 진료시간 --%>
            <div id="pd-section-medoft" class="hidden bg-white rounded-2xl p-5" style="box-shadow: 0 2px 12px rgba(0,0,0,0.07);">
                <h3 class="text-sm font-semibold text-gray-700 mb-3">진료시간</h3>
                <div id="pd-medoft-list" class="space-y-1"></div>
            </div>

            <%-- 비급여 진료비 --%>
            <div class="bg-white rounded-2xl p-5" style="box-shadow: 0 2px 12px rgba(0,0,0,0.07);">
                <h3 class="text-sm font-semibold text-gray-700 mb-4">비급여 진료비</h3>
                <div id="pd-price-loading" class="flex items-center justify-center gap-2 py-8 text-gray-400">
                    <div class="w-4 h-4 border-2 border-[#2563EB] border-t-transparent rounded-full animate-spin"></div>
                    <span class="text-xs">진료비 정보를 불러오는 중...</span>
                </div>
                <div id="pd-price-empty" class="hidden text-center py-8 text-gray-400 text-sm">등록된 비급여 진료비 정보가 없습니다</div>
                <table id="pd-price-table" class="w-full hidden">
                    <thead>
                        <tr class="text-left text-xs text-gray-400 border-b border-gray-200">
                            <th class="pb-3 font-medium">항목명</th>
                            <th class="pb-3 font-medium text-right whitespace-nowrap w-px">가격</th>
                        </tr>
                    </thead>
                    <tbody id="pd-price-tbody" class="divide-y divide-gray-100"></tbody>
                </table>
            </div>

        </div>
    </div>
</div>

</main>
</body>
</html>
