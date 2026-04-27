<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="병원 상세" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<script defer src="/static/js/hospital.js"></script>

<div class="max-w-4xl mx-auto px-4 py-8">

    <%-- 로딩 상태 --%>
    <div id="state-loading" class="flex flex-col items-center justify-center py-24 text-gray-400">
        <div class="w-6 h-6 border-2 border-blue-400 border-t-transparent rounded-full animate-spin mb-3"></div>
        <p class="text-sm">병원 정보를 불러오는 중...</p>
    </div>

    <%-- 오류 상태 --%>
    <div id="state-error" class="hidden flex flex-col items-center justify-center py-24">
        <div class="bg-white rounded-2xl shadow-sm p-10 text-center max-w-sm w-full">
            <div class="w-16 h-16 bg-blue-50 rounded-full flex items-center justify-center mx-auto mb-4">
                <svg class="w-8 h-8 text-blue-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                        d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-2 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"/>
                </svg>
            </div>
            <h2 class="text-base font-semibold text-gray-700 mb-1">병원 정보를 준비 중이에요</h2>
            <p class="text-sm text-gray-400 mb-6">진료비 데이터 서비스를 곧 제공할 예정입니다</p>
            <a href="/hospitals"
               class="inline-block bg-blue-600 text-white text-sm px-5 py-2 rounded-full hover:bg-blue-700 transition-colors">
                병원 목록으로
            </a>
        </div>
    </div>

    <%-- 병원 정보 (API 연결 후 표시) --%>
    <div id="state-content" class="hidden">

        <%-- 병원 헤더 --%>
        <div class="bg-white rounded-2xl shadow-sm p-6 mb-4">
            <div class="flex items-start justify-between">
                <div>
                    <h1 id="hospital-name" class="text-xl font-bold text-gray-800 mb-1"></h1>
                    <p id="hospital-type" class="text-sm text-gray-400 mb-2"></p>
                    <p id="hospital-address" class="text-sm text-gray-600"></p>
                </div>
                <span id="hospital-distance" class="text-sm text-blue-500 font-medium flex-shrink-0 ml-4"></span>
            </div>
            <div class="mt-4 pt-4 border-t border-gray-100">
                <a id="hospital-phone" href="#" class="text-sm text-blue-600 hover:underline"></a>
            </div>
        </div>

        <%-- 비급여 가격 테이블 --%>
        <div class="bg-white rounded-2xl shadow-sm p-6">
            <h2 class="text-base font-semibold text-gray-700 mb-4">비급여 진료비</h2>

            <div id="price-empty" class="hidden text-center py-8 text-gray-400 text-sm">
                등록된 비급여 진료비 정보가 없습니다
            </div>

            <table id="price-table" class="w-full hidden">
                <thead>
                    <tr class="text-left text-xs text-gray-400 border-b border-gray-100">
                        <th class="pb-3 font-medium">항목명</th>
                        <th class="pb-3 font-medium text-right">최저</th>
                        <th class="pb-3 font-medium text-right">최고</th>
                    </tr>
                </thead>
                <tbody id="price-tbody" class="divide-y divide-gray-50 text-sm text-gray-700"></tbody>
            </table>
        </div>

    </div>
</div>

<script>
    const HOSPITAL_ID = ${hospitalId};
    document.addEventListener('DOMContentLoaded', () => {
        fetchHospitalDetail(HOSPITAL_ID);
    });
</script>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
