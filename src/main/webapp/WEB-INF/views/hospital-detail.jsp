<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="병원 상세" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<script defer src="<c:url value="/static/js/hospital.js?v=6"/>"></script>

<div class="bg-[#F2F4F6] min-h-full">
<div class="max-w-xl mx-auto px-4 py-6">

    <%-- 로딩 상태 --%>
    <div id="state-loading" class="flex flex-col items-center justify-center py-24 text-gray-400">
        <div class="w-5 h-5 border-2 border-[#2563EB] border-t-transparent rounded-full animate-spin mb-3"></div>
        <p class="text-sm">병원 정보를 불러오는 중...</p>
    </div>

    <%-- 오류 상태 --%>
    <div id="state-error" class="hidden flex flex-col items-center justify-center py-20">
        <div class="bg-white rounded-3xl p-10 text-center max-w-sm w-full" style="box-shadow: 0 2px 16px rgba(0,0,0,0.08);">
            <div class="w-16 h-16 bg-blue-50 rounded-2xl flex items-center justify-center mx-auto mb-5">
                <svg class="w-7 h-7 text-[#2563EB] opacity-60" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                        d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-2 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"></path>
                </svg>
            </div>
            <h2 class="text-base font-semibold text-gray-800 mb-2">병원 정보를 준비 중이에요</h2>
            <p class="text-sm text-gray-400 mb-7 leading-relaxed">진료비 데이터 서비스를<br>곧 제공할 예정입니다</p>
            <a href="<c:url value="/hospitals"/>"
               class="inline-flex items-center justify-center bg-[#2563EB] text-white text-sm font-semibold px-6 rounded-xl hover:bg-blue-700 transition-colors min-h-[44px]">
                병원 목록으로
            </a>
        </div>
    </div>

    <%-- 병원 정보 --%>
    <div id="state-content" class="hidden space-y-3">

        <%-- 뒤로가기 --%>
        <a href="<c:url value="/hospitals"/>"
           class="inline-flex items-center gap-1 text-sm text-gray-400 hover:text-[#2563EB] transition-colors min-h-[44px] -ml-1">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"></path>
            </svg>
            병원 목록
        </a>

        <%-- 병원 헤더 카드 --%>
        <div class="bg-white rounded-2xl p-5" style="box-shadow: 0 2px 12px rgba(0,0,0,0.07);">
            <div class="flex items-start justify-between gap-3 mb-4">
                <div class="flex-1 min-w-0">
                    <h1 id="hospital-name" class="text-lg font-bold text-gray-900 mb-0.5 leading-snug"></h1>
                    <p id="hospital-type" class="text-sm text-gray-400 mb-2"></p>
                    <p id="hospital-address" class="text-sm text-gray-500 leading-relaxed"></p>
                </div>
                <span id="hospital-distance" class="text-sm font-semibold text-[#2563EB] flex-shrink-0 bg-blue-50 px-2.5 py-1 rounded-lg"></span>
            </div>
            <div class="pt-4 border-t border-gray-100">
                <a id="hospital-phone" href="#"
                   class="inline-flex items-center gap-2 text-sm text-[#2563EB] font-medium hover:underline">
                    <div class="w-7 h-7 bg-blue-50 rounded-lg flex items-center justify-center flex-shrink-0">
                        <svg class="w-3.5 h-3.5 text-[#2563EB]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                                d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z"></path>
                        </svg>
                    </div>
                </a>
            </div>
        </div>

        <%-- 비급여 가격 테이블 --%>
        <div class="bg-white rounded-2xl p-5" style="box-shadow: 0 2px 12px rgba(0,0,0,0.07);">
            <h2 class="text-sm font-semibold text-gray-700 mb-4">비급여 진료비</h2>

            <div id="price-empty" class="hidden text-center py-10 text-gray-400 text-sm">
                등록된 비급여 진료비 정보가 없습니다
            </div>

            <table id="price-table" class="w-full hidden">
                <thead>
                    <tr class="text-left text-xs text-gray-400 border-b border-gray-200">
                        <th class="pb-3 font-medium">항목명</th>
                        <th class="pb-3 font-medium text-right">최저</th>
                        <th class="pb-3 font-medium text-right">최고</th>
                    </tr>
                </thead>
                <tbody id="price-tbody" class="divide-y divide-gray-100 text-sm text-gray-700"></tbody>
            </table>
        </div>

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
