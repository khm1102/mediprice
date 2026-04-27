<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="비급여 진료비 비교" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<%-- 네이버맵 SDK --%>
<script type="text/javascript"
    src="https://oapi.map.naver.com/openapi/v3/maps.js?ncpKeyId=${naverMapKey}"></script>
<script defer src="/static/js/map.js"></script>

<%-- 히어로 --%>
<div class="bg-white border-b border-gray-100 py-12 text-center">
    <h1 class="text-3xl font-bold text-gray-800 mb-2">내 주변 비급여 진료비 비교</h1>
    <p class="text-gray-400 mb-8 text-sm">병원마다 다른 비급여 진료비, 한눈에 비교하세요</p>

    <div class="max-w-xl mx-auto flex gap-2 px-4">
        <input type="text" id="search-input"
            placeholder="진료 항목을 입력하세요 (예: 도수치료, 주사)"
            class="flex-1 border border-gray-200 rounded-full px-5 py-3 text-sm focus:outline-none focus:border-blue-400 shadow-sm" />
        <button onclick="handleSearch()"
            class="bg-blue-600 text-white px-6 py-3 rounded-full text-sm hover:bg-blue-700 transition-colors shadow-sm">
            검색
        </button>
    </div>
</div>

<%-- 지도 --%>
<div id="map" class="w-full" style="height: 520px; background: #f1f5f9;">
    <div id="map-loading" class="flex items-center justify-center h-full text-gray-400 text-sm">
        지도를 불러오는 중...
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
</script>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
