<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="비급여 진료비 비교" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="max-w-6xl mx-auto px-4 py-16 text-center">
    <h1 class="text-3xl font-bold text-gray-800 mb-3">
        내 주변 비급여 진료비 비교
    </h1>
    <p class="text-gray-500 mb-10">
        병원마다 다른 비급여 진료비, 한눈에 비교하세요.
    </p>

    <%-- 검색창 --%>
    <div class="max-w-xl mx-auto flex gap-2">
        <input
            type="text"
            id="search-input"
            placeholder="진료 항목을 입력하세요 (예: 도수치료, 주사)"
            class="flex-1 border border-gray-300 rounded-full px-5 py-3 text-sm focus:outline-none focus:border-blue-500 shadow-sm"
        />
        <button
            onclick="handleSearch()"
            class="bg-blue-600 text-white px-6 py-3 rounded-full text-sm hover:bg-blue-700 transition-colors shadow-sm">
            검색
        </button>
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
