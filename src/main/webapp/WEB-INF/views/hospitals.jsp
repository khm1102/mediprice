<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="병원 검색" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<script type="text/javascript"
    src="https://oapi.map.naver.com/openapi/v3/maps.js?ncpKeyId=${naverMapKey}"></script>
<script defer src="/static/js/map.js?v=4"></script>
<script defer src="/static/js/hospital.js?v=4"></script>

<div class="max-w-6xl mx-auto px-4 py-6">

    <%-- 검색바 --%>
    <div class="flex gap-2 mb-6">
        <input type="text" id="search-input"
            placeholder="진료 항목을 입력하세요"
            class="flex-1 border border-gray-200 rounded-full px-5 py-2.5 text-sm focus:outline-none focus:border-blue-400 shadow-sm" />
        <button onclick="handleSearch()"
            class="bg-blue-600 text-white px-6 py-2.5 rounded-full text-sm hover:bg-blue-700 transition-colors">
            검색
        </button>
    </div>

    <div class="flex gap-4" style="height: 600px;">

        <%-- 지도 --%>
        <div class="relative flex-shrink-0" style="width: 420px;">
            <div id="map" class="rounded-xl overflow-hidden shadow-sm w-full h-full" style="background: #f1f5f9;"></div>
            <button onclick="recenterMap()"
                title="현재 위치로 이동"
                class="absolute bottom-3 right-3 z-10 bg-white rounded-full w-9 h-9 shadow-md flex items-center justify-center hover:bg-gray-50 transition-colors">
                <svg class="w-4 h-4 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M12 8c-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4-1.79-4-4-4zm8.94 3A8.994 8.994 0 0013 3.06V1h-2v2.06A8.994 8.994 0 003.06 11H1v2h2.06A8.994 8.994 0 0011 20.94V23h2v-2.06A8.994 8.994 0 0020.94 13H23v-2h-2.06z"/>
                </svg>
            </button>
        </div>

        <%-- 병원 목록 --%>
        <div class="flex-1 overflow-y-auto">
            <div id="hospital-list" class="space-y-3">

                <%-- 로딩 상태 --%>
                <div id="state-loading" class="flex flex-col items-center justify-center h-64 text-gray-400">
                    <div class="w-6 h-6 border-2 border-blue-400 border-t-transparent rounded-full animate-spin mb-3"></div>
                    <p class="text-sm">병원 정보를 불러오는 중...</p>
                </div>

                <%-- 빈 결과 상태 --%>
                <div id="state-empty" class="hidden flex-col items-center justify-center h-64 text-gray-400">
                    <svg class="w-12 h-12 mb-3 text-gray-200" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                            d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
                    </svg>
                    <p class="text-sm font-medium text-gray-500">검색 결과가 없어요</p>
                    <p class="text-xs text-gray-400 mt-1">다른 검색어나 지역으로 다시 시도해보세요</p>
                    <button onclick="document.getElementById('search-input').focus()"
                        class="mt-4 text-xs text-blue-500 hover:text-blue-700 underline">
                        다시 검색하기
                    </button>
                </div>

                <%-- 오류 상태 --%>
                <div id="state-error" class="hidden flex-col items-center justify-center h-64 text-gray-400">
                    <svg class="w-12 h-12 mb-3 text-gray-200" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                            d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/>
                    </svg>
                    <p class="text-sm font-medium text-gray-500">데이터를 불러오지 못했습니다</p>
                    <p class="text-xs text-gray-400 mt-1">잠시 후 다시 시도해주세요</p>
                    <button onclick="fetchHospitals(document.getElementById('search-input').value)"
                        class="mt-4 text-xs text-blue-500 hover:text-blue-700 underline">
                        다시 시도
                    </button>
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
        // URL 파라미터에서 직접 읽어 서버 인코딩 문제 방지
        const keyword = new URLSearchParams(location.search).get('keyword') || '';
        document.getElementById('search-input').value = keyword;
        fetchHospitals(keyword);
    });
</script>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
