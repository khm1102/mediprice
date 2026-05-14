<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="병원 검색" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<script type="text/javascript"
    src="https://oapi.map.naver.com/openapi/v3/maps.js?ncpKeyId=${naverMapKey}"></script>
<script defer src="<c:url value="/static/js/map.js?v=10"/>"></script>
<script defer src="<c:url value="/static/js/hospital.js?v=20"/>"></script>

<style>
    html, body { overflow: hidden; height: 100%; }
    main { min-height: 0; }

    .hospitals-grid {
        display: grid;
        grid-template-columns: 1fr;
        grid-template-rows: auto 45vh 1fr;
        grid-template-areas:
            "search"
            "map"
            "list";
        height: 100%;
    }
    .list-area {
        overflow-y: auto;
    }
    .map-area {
        border-bottom: 2px solid #E5E7EB;
    }
    @media (min-width: 1024px) {
        .hospitals-grid {
            grid-template-columns: 400px 1fr;
            grid-template-rows: auto 1fr;
            grid-template-areas:
                "search map"
                "list map";
        }
        .panel-left {
            position: relative;
            z-index: 10;
            box-shadow: 4px 0 20px rgba(0, 0, 0, 0.07);
        }
        .map-area {
            border-bottom: none;
        }
    }
</style>

<div class="hospitals-grid">

    <%-- 검색바 --%>
    <div style="grid-area: search;" class="panel-left bg-white border-b border-gray-200 px-4 py-3 flex gap-2 flex-shrink-0">
        <div class="flex-1 flex items-center gap-2 bg-[#F2F4F6] rounded-xl px-3.5 focus-within:ring-2 focus-within:ring-[#2563EB]/30 transition-all">
            <svg class="w-4 h-4 text-gray-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
            </svg>
            <input type="text" id="search-input"
                placeholder="진료 항목을 입력하세요"
                class="flex-1 bg-transparent text-sm text-gray-800 placeholder-gray-400 focus:outline-none py-2.5"/>
        </div>
        <button onclick="handleSearch()"
            class="bg-[#2563EB] text-white px-5 rounded-xl text-sm font-semibold hover:bg-blue-700 transition-colors min-h-[44px] flex-shrink-0">
            검색
        </button>
    </div>

    <%-- 지도 --%>
    <div style="grid-area: map;" class="map-area relative">
        <div id="map" class="w-full h-full" style="background: #e8edf5;"></div>
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
    <div id="panel-list" style="grid-area: list; position: relative;" class="panel-left list-area bg-[#F2F4F6]">

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
        <div id="state-empty" class="hidden absolute inset-0 flex flex-col items-center justify-center text-center px-8">
            <div class="w-14 h-14 bg-white rounded-2xl flex items-center justify-center mb-4" style="box-shadow: 0 2px 10px rgba(0,0,0,0.08);">
                <svg class="w-6 h-6 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                </svg>
            </div>
            <p class="text-sm font-semibold text-gray-600">검색 결과가 없어요</p>
            <p class="text-xs text-gray-400 mt-1">다른 검색어로 다시 시도해보세요</p>
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

    <%-- 병원 상세 패널 (목록과 같은 grid-area 공유, JS로 전환) --%>
    <div id="panel-detail" style="grid-area: list; display:none;" class="panel-left list-area bg-[#F2F4F6]">

        <%-- 상세 로딩 --%>
        <div id="pd-loading" class="flex flex-col items-center justify-center h-full text-gray-400">
            <div class="w-5 h-5 border-2 border-[#2563EB] border-t-transparent rounded-full animate-spin mb-3"></div>
            <p class="text-sm">병원 정보를 불러오는 중...</p>
        </div>

        <%-- 상세 오류 --%>
        <div id="pd-error" class="hidden flex-col items-center justify-center h-full">
            <p class="text-sm font-semibold text-gray-600">정보를 불러오지 못했습니다</p>
            <button onclick="showHospitalList()" class="mt-3 text-xs text-[#2563EB] font-medium hover:underline min-h-[44px] px-2">목록으로</button>
        </div>

        <%-- 상세 콘텐츠 --%>
        <div id="pd-content" class="hidden h-full overflow-y-auto">
            <div class="max-w-xl mx-auto px-4 py-5 space-y-3">

                <%-- 뒤로가기 --%>
                <button onclick="showHospitalList()"
                    class="inline-flex items-center gap-1 text-sm text-gray-400 hover:text-[#2563EB] transition-colors min-h-[44px] -ml-1">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"></path>
                    </svg>
                    병원 목록
                </button>

                <%-- 병원 헤더 카드 --%>
                <div class="bg-white rounded-2xl p-5" style="box-shadow: 0 2px 12px rgba(0,0,0,0.07);">
                    <div class="flex items-start justify-between gap-3 mb-4">
                        <div class="flex-1 min-w-0">
                            <h2 id="pd-name" class="text-base font-bold text-gray-900 mb-0.5 leading-snug"></h2>
                            <p id="pd-type" class="text-sm text-gray-400 mb-2"></p>
                            <p id="pd-address" class="text-sm text-gray-500 leading-relaxed"></p>
                        </div>
                        <span id="pd-distance" class="text-sm font-semibold text-[#2563EB] flex-shrink-0 bg-blue-50 px-2.5 py-1 rounded-lg"></span>
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
                        <div data-field="pd-dr-count" class="hidden" style="display:none">
                            <div class="flex items-center gap-2.5 text-sm text-gray-600">
                                <div class="w-7 h-7 bg-gray-50 rounded-lg flex items-center justify-center flex-shrink-0">
                                    <svg class="w-3.5 h-3.5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z"></path>
                                    </svg>
                                </div>
                                <span>의사 <span id="pd-dr-count" class="font-medium text-gray-800"></span></span>
                            </div>
                        </div>
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

                <%-- 특수진단 --%>
                <div id="pd-section-spcl" class="hidden bg-white rounded-2xl p-5" style="box-shadow: 0 2px 12px rgba(0,0,0,0.07);">
                    <h3 class="text-sm font-semibold text-gray-700 mb-3">특수진단</h3>
                    <div id="pd-spcl-list" class="flex flex-wrap gap-1.5"></div>
                </div>

                <%-- 교통/주차 --%>
                <div id="pd-section-trnsprt" class="hidden bg-white rounded-2xl p-5" style="box-shadow: 0 2px 12px rgba(0,0,0,0.07);">
                    <h3 class="text-sm font-semibold text-gray-700 mb-3">교통 / 주차</h3>
                    <div class="space-y-3">
                        <div data-field="pd-park" class="hidden" style="display:none">
                            <div class="flex items-start gap-2.5">
                                <div class="w-6 h-6 bg-gray-50 rounded-md flex items-center justify-center flex-shrink-0 mt-0.5">
                                    <svg class="w-3.5 h-3.5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M5 17H3a2 2 0 01-2-2V5a2 2 0 012-2h11a2 2 0 012 2v3m0 0h3l3 3v5h-3m-3 0H9m6 0a2 2 0 11-4 0 2 2 0 014 0zM7 17a2 2 0 11-4 0 2 2 0 014 0z"></path>
                                    </svg>
                                </div>
                                <p id="pd-park" class="text-sm text-gray-600 leading-relaxed whitespace-pre-line"></p>
                            </div>
                        </div>
                        <div data-field="pd-traf" class="hidden" style="display:none">
                            <div class="flex items-start gap-2.5">
                                <div class="w-6 h-6 bg-gray-50 rounded-md flex items-center justify-center flex-shrink-0 mt-0.5">
                                    <svg class="w-3.5 h-3.5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7"></path>
                                    </svg>
                                </div>
                                <p id="pd-traf" class="text-sm text-gray-600 leading-relaxed"></p>
                            </div>
                        </div>
                    </div>
                </div>

            </div>
        </div>
    </div>

</div>

<script>
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
        fetchHospitals(keyword);
    });
</script>

</main>
</body>
</html>
