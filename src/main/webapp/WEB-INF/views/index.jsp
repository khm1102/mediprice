<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="비급여 진료비 비교" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<%-- 히어로 --%>
<div style="background: linear-gradient(135deg, #1d4ed8 0%, #3b82f6 55%, #60a5fa 100%);" class="py-20 px-4 text-center">
    <div class="max-w-2xl mx-auto">
        <div class="inline-flex items-center gap-2 bg-white bg-opacity-20 text-white text-xs font-medium px-3 py-1.5 rounded-full mb-6">
            <span style="width:6px;height:6px;background:#86efac;border-radius:50%;display:inline-block;animation:pulse 2s infinite;"></span>
            건강보험심사평가원 공식 데이터 기반
        </div>
        <h1 class="text-4xl font-bold text-white mb-4 leading-tight">
            병원별 비급여 진료비<br>한눈에 비교하세요
        </h1>
        <p class="text-blue-100 text-sm mb-10 leading-relaxed">
            도수치료, MRI, 주사치료 등 병원마다 다른 비급여 가격,<br>
            MediPrice에서 투명하게 확인하고 현명하게 선택하세요
        </p>

        <%-- 검색바 --%>
        <div class="max-w-xl mx-auto">
            <div class="flex gap-2 bg-white rounded-2xl p-2 shadow-2xl">
                <div class="flex-1 flex items-center gap-3 px-3">
                    <svg class="w-5 h-5 text-gray-300 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                            d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
                    </svg>
                    <input type="text" id="search-input"
                        placeholder="진료 항목을 입력하세요 (예: 도수치료, MRI)"
                        class="flex-1 text-sm text-gray-700 placeholder-gray-300 focus:outline-none py-2 bg-transparent" />
                </div>
                <button onclick="handleSearch()"
                    class="bg-blue-600 text-white px-6 py-3 rounded-xl text-sm font-semibold hover:bg-blue-700 transition-colors flex-shrink-0">
                    검색
                </button>
            </div>
        </div>
    </div>
</div>

<%-- 안내 배너 --%>
<div class="bg-amber-50 border-b border-amber-100">
    <div class="max-w-2xl mx-auto px-4 py-4 flex items-start gap-3">
        <svg class="w-4 h-4 text-amber-500 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/>
        </svg>
        <p class="text-xs text-amber-700 leading-relaxed">
            2026년 비급여 진료비용 공개를 위한 자료수집 및 검증기간(4월~8월)으로 <br>
            해당 기간에는 변경사항이 반영되지 않아 조회 정보와 의료기관에서 실제 운영 중인 금액 간 차이가 있을 수 있습니다.
        </p>
    </div>
</div>

<%-- 자주 찾는 항목 --%>
<div class="max-w-2xl mx-auto px-4 py-12">
    <p class="text-xs font-semibold text-gray-400 uppercase tracking-widest text-center mb-6">자주 찾는 항목</p>

    <div class="grid grid-cols-2 gap-3" style="grid-template-columns: repeat(2, 1fr);">
        <a href="/hospitals?keyword=%EB%8F%84%EC%88%98%EC%B9%98%EB%A3%8C"
           class="flex items-center gap-3 bg-white border border-gray-100 rounded-xl px-4 py-3.5 hover:border-blue-300 hover:shadow-sm transition-all group">
            <span class="text-2xl">🦴</span>
            <div>
                <p class="text-sm font-medium text-gray-700 group-hover:text-blue-600 transition-colors">도수치료</p>
                <p class="text-xs text-gray-400">정형외과 · 재활의학과</p>
            </div>
        </a>
        <a href="/hospitals?keyword=MRI"
           class="flex items-center gap-3 bg-white border border-gray-100 rounded-xl px-4 py-3.5 hover:border-blue-300 hover:shadow-sm transition-all group">
            <span class="text-2xl">🔬</span>
            <div>
                <p class="text-sm font-medium text-gray-700 group-hover:text-blue-600 transition-colors">MRI</p>
                <p class="text-xs text-gray-400">영상의학과</p>
            </div>
        </a>
        <a href="/hospitals?keyword=%EC%B2%B4%EC%99%B8%EC%B6%A9%EA%B2%A9%ED%8C%8C"
           class="flex items-center gap-3 bg-white border border-gray-100 rounded-xl px-4 py-3.5 hover:border-blue-300 hover:shadow-sm transition-all group">
            <span class="text-2xl">⚡</span>
            <div>
                <p class="text-sm font-medium text-gray-700 group-hover:text-blue-600 transition-colors">체외충격파</p>
                <p class="text-xs text-gray-400">정형외과</p>
            </div>
        </a>
        <a href="/hospitals?keyword=%EC%B4%88%EC%9D%8C%ED%8C%8C"
           class="flex items-center gap-3 bg-white border border-gray-100 rounded-xl px-4 py-3.5 hover:border-blue-300 hover:shadow-sm transition-all group">
            <span class="text-2xl">📡</span>
            <div>
                <p class="text-sm font-medium text-gray-700 group-hover:text-blue-600 transition-colors">초음파</p>
                <p class="text-xs text-gray-400">내과 · 산부인과</p>
            </div>
        </a>
        <a href="/hospitals?keyword=%EC%A3%BC%EC%82%AC%EC%B9%98%EB%A3%8C"
           class="flex items-center gap-3 bg-white border border-gray-100 rounded-xl px-4 py-3.5 hover:border-blue-300 hover:shadow-sm transition-all group">
            <span class="text-2xl">💉</span>
            <div>
                <p class="text-sm font-medium text-gray-700 group-hover:text-blue-600 transition-colors">주사치료</p>
                <p class="text-xs text-gray-400">통증의학과</p>
            </div>
        </a>
        <a href="/hospitals?keyword=%EC%88%98%EB%A9%B4%EB%82%B4%EC%8B%9C%EA%B2%BD"
           class="flex items-center gap-3 bg-white border border-gray-100 rounded-xl px-4 py-3.5 hover:border-blue-300 hover:shadow-sm transition-all group">
            <span class="text-2xl">🩺</span>
            <div>
                <p class="text-sm font-medium text-gray-700 group-hover:text-blue-600 transition-colors">수면내시경</p>
                <p class="text-xs text-gray-400">소화기내과</p>
            </div>
        </a>
        <a href="/hospitals?keyword=%EB%B3%B4%ED%86%A1%EC%8A%A4"
           class="flex items-center gap-3 bg-white border border-gray-100 rounded-xl px-4 py-3.5 hover:border-blue-300 hover:shadow-sm transition-all group">
            <span class="text-2xl">✨</span>
            <div>
                <p class="text-sm font-medium text-gray-700 group-hover:text-blue-600 transition-colors">보톡스</p>
                <p class="text-xs text-gray-400">피부과 · 성형외과</p>
            </div>
        </a>
        <a href="/hospitals?keyword=%ED%95%84%EB%9F%AC"
           class="flex items-center gap-3 bg-white border border-gray-100 rounded-xl px-4 py-3.5 hover:border-blue-300 hover:shadow-sm transition-all group">
            <span class="text-2xl">💆</span>
            <div>
                <p class="text-sm font-medium text-gray-700 group-hover:text-blue-600 transition-colors">필러</p>
                <p class="text-xs text-gray-400">피부과 · 성형외과</p>
            </div>
        </a>
        <a href="/hospitals?keyword=%EB%A0%88%EC%9D%B4%EC%A0%80%20%EC%8B%9C%EC%88%A0"
           class="flex items-center gap-3 bg-white border border-gray-100 rounded-xl px-4 py-3.5 hover:border-blue-300 hover:shadow-sm transition-all group">
            <span class="text-2xl">🔆</span>
            <div>
                <p class="text-sm font-medium text-gray-700 group-hover:text-blue-600 transition-colors">레이저 시술</p>
                <p class="text-xs text-gray-400">피부과</p>
            </div>
        </a>
        <a href="/hospitals?keyword=%ED%94%BC%EB%B6%80%EA%B3%BC"
           class="flex items-center gap-3 bg-white border border-gray-100 rounded-xl px-4 py-3.5 hover:border-blue-300 hover:shadow-sm transition-all group">
            <span class="text-2xl">🏥</span>
            <div>
                <p class="text-sm font-medium text-gray-700 group-hover:text-blue-600 transition-colors">피부과</p>
                <p class="text-xs text-gray-400">피부과 전문</p>
            </div>
        </a>
    </div>
</div>

<%-- 서비스 소개 카드 --%>
<div class="bg-white border-t border-gray-100 py-12">
    <div class="max-w-2xl mx-auto px-4">
        <p class="text-xs font-semibold text-gray-400 uppercase tracking-widest text-center mb-8">MediPrice가 다른 이유</p>
        <div class="flex gap-6 text-center justify-center">
            <div style="flex:1;">
                <div class="w-12 h-12 bg-blue-50 rounded-2xl flex items-center justify-center mx-auto mb-3">
                    <svg class="w-6 h-6 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                            d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
                    </svg>
                </div>
                <p class="text-sm font-semibold text-gray-700 mb-1">공식 데이터</p>
                <p class="text-xs text-gray-400 leading-relaxed">건강보험심사평가원<br>공개 데이터 사용</p>
            </div>
            <div style="flex:1;">
                <div class="w-12 h-12 bg-blue-50 rounded-2xl flex items-center justify-center mx-auto mb-3">
                    <svg class="w-6 h-6 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                            d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"/>
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                            d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"/>
                    </svg>
                </div>
                <p class="text-sm font-semibold text-gray-700 mb-1">위치 기반</p>
                <p class="text-xs text-gray-400 leading-relaxed">내 주변 가까운<br>병원부터 비교</p>
            </div>
            <div style="flex:1;">
                <div class="w-12 h-12 bg-blue-50 rounded-2xl flex items-center justify-center mx-auto mb-3">
                    <svg class="w-6 h-6 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                            d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
                    </svg>
                </div>
                <p class="text-sm font-semibold text-gray-700 mb-1">가격 비교</p>
                <p class="text-xs text-gray-400 leading-relaxed">최저·최고 가격<br>한눈에 비교</p>
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
</script>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
