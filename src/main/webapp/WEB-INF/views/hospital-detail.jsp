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
    <div id="state-error" class="hidden flex flex-col items-center justify-center py-24 text-gray-400">
        <svg class="w-12 h-12 mb-3 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
        </svg>
        <p class="text-sm font-medium text-gray-500">데이터를 불러오지 못했습니다</p>
        <p class="text-xs text-gray-400 mt-1">백엔드 API가 아직 준비 중입니다</p>
        <a href="/hospitals" class="mt-4 text-xs text-blue-500 hover:text-blue-700 underline">
            목록으로 돌아가기
        </a>
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
