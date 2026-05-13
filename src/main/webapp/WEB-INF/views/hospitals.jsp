<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="병원 검색" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<script type="text/javascript"
    src="https://oapi.map.naver.com/openapi/v3/maps.js?ncpKeyId=${naverMapKey}"></script>
<script defer src="<c:url value="/static/js/map.js?v=4"/>"></script>
<script defer src="<c:url value="/static/js/hospital.js?v=6"/>"></script>

<style>
    html, body { overflow: hidden; height: 100%; }

    .hospitals-grid {
        display: grid;
        grid-template-columns: 1fr;
        grid-template-rows: auto 45vh 1fr;
        grid-template-areas:
            "search"
            "map"
            "list";
        height: calc(100vh - 56px);
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

    <%-- 병원 목록 --%>
    <div style="grid-area: list;" class="panel-left list-area bg-[#F2F4F6] p-3">
        <div id="hospital-list" class="space-y-2">

            <%-- 로딩 상태 --%>
            <div id="state-loading" class="flex flex-col items-center justify-center py-16 text-gray-400">
                <div class="w-5 h-5 border-2 border-[#2563EB] border-t-transparent rounded-full animate-spin mb-3"></div>
                <p class="text-sm">병원 정보를 불러오는 중...</p>
            </div>

            <%-- 빈 결과 상태 --%>
            <div id="state-empty" class="hidden flex-col items-center justify-center py-16">
                <div class="w-14 h-14 bg-white rounded-2xl flex items-center justify-center mb-4" style="box-shadow: 0 2px 10px rgba(0,0,0,0.08);">
                    <svg class="w-6 h-6 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                            d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                    </svg>
                </div>
                <p class="text-sm font-semibold text-gray-600">검색 결과가 없어요</p>
                <p class="text-xs text-gray-400 mt-1">다른 검색어로 다시 시도해보세요</p>
                <button onclick="document.getElementById('search-input').focus()"
                    class="mt-4 text-xs text-[#2563EB] font-medium hover:underline min-h-[44px] px-2">
                    다시 검색하기
                </button>
            </div>

            <%-- 오류 상태 --%>
            <div id="state-error" class="hidden flex-col items-center justify-center py-16">
                <div class="w-14 h-14 bg-white rounded-2xl flex items-center justify-center mb-4" style="box-shadow: 0 2px 10px rgba(0,0,0,0.08);">
                    <svg class="w-6 h-6 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                            d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"></path>
                        <path stroke-linecap="round" stroke-width="1.5" d="M12 10v3.5"></path>
                        <circle cx="12" cy="16.5" r="0.7" fill="currentColor" stroke="none"></circle>
                    </svg>
                </div>
                <p class="text-sm font-semibold text-gray-600">데이터를 불러오지 못했습니다</p>
                <p class="text-xs text-gray-400 mt-1">잠시 후 다시 시도해주세요</p>
                <button onclick="fetchHospitals(document.getElementById('search-input').value)"
                    class="mt-4 text-xs text-[#2563EB] font-medium hover:underline min-h-[44px] px-2">
                    다시 시도
                </button>
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

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
