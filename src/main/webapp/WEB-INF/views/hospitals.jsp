<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="병원 검색" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<script type="text/javascript"
    src="https://oapi.map.naver.com/openapi/v3/maps.js?ncpKeyId=${naverMapKey}"></script>
<script defer src="/static/js/map.js"></script>
<script defer src="/static/js/hospital.js"></script>

<div class="max-w-6xl mx-auto px-4 py-6">

    <%-- 검색바 --%>
    <div class="flex gap-2 mb-6">
        <input type="text" id="search-input" value="${keyword}"
            placeholder="진료 항목을 입력하세요"
            class="flex-1 border border-gray-200 rounded-full px-5 py-2.5 text-sm focus:outline-none focus:border-blue-400 shadow-sm" />
        <button onclick="handleSearch()"
            class="bg-blue-600 text-white px-6 py-2.5 rounded-full text-sm hover:bg-blue-700 transition-colors">
            검색
        </button>
    </div>

    <div class="flex gap-4" style="height: 600px;">

        <%-- 지도 --%>
        <div id="map" class="rounded-xl overflow-hidden shadow-sm flex-shrink-0" style="width: 420px; background: #f1f5f9;"></div>

        <%-- 병원 목록 --%>
        <div class="flex-1 overflow-y-auto">
            <div id="hospital-list" class="space-y-3">

                <%-- 로딩 상태 --%>
                <div id="state-loading" class="flex flex-col items-center justify-center h-64 text-gray-400">
                    <div class="w-6 h-6 border-2 border-blue-400 border-t-transparent rounded-full animate-spin mb-3"></div>
                    <p class="text-sm">병원 정보를 불러오는 중...</p>
                </div>

                <%-- 오류/빈 상태 --%>
                <div id="state-error" class="hidden flex-col items-center justify-center h-64 text-gray-400">
                    <svg class="w-12 h-12 mb-3 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                            d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
                    </svg>
                    <p class="text-sm font-medium text-gray-500">데이터를 불러오지 못했습니다</p>
                    <p class="text-xs text-gray-400 mt-1">백엔드 API가 아직 준비 중입니다</p>
                    <button onclick="fetchHospitals()"
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

    const INITIAL_KEYWORD = '${keyword}';
    document.addEventListener('DOMContentLoaded', () => {
        fetchHospitals(INITIAL_KEYWORD);
    });
</script>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
