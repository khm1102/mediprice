<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="병원 상세" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<style>
    html, body { background-color: #F2F4F6; }
</style>

<script defer src="<c:url value='/static/js/hospital.js'/>?v=20260606-ux2"></script>

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
            <a href="<c:url value='/hospitals'/>"
               class="inline-flex items-center justify-center bg-[#2563EB] text-white text-sm font-semibold px-6 rounded-xl hover:bg-blue-700 transition-colors min-h-[44px]">
                병원 목록으로
            </a>
        </div>
    </div>

    <%-- 병원 정보 --%>
    <div id="state-content" class="hidden space-y-3">

        <%-- 뒤로가기 --%>
        <a id="back-btn" href="<c:url value='/hospitals'/>"
           class="inline-flex items-center gap-1 text-sm text-gray-400 hover:text-[#2563EB] transition-colors min-h-[44px] -ml-1">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"></path>
            </svg>
            병원 목록
        </a>
        <script>
            (function() {
                const kw = new URLSearchParams(location.search).get('keyword');
                if (kw) {
                    document.getElementById('back-btn').href = '/hospitals?keyword=' + encodeURIComponent(kw);
                }
            })();
        </script>

        <%-- 병원 헤더 카드 --%>
        <div class="bg-white rounded-2xl p-5" style="box-shadow: 0 2px 12px rgba(0,0,0,0.07);">
            <div class="flex items-start justify-between gap-3 mb-4">
                <div class="flex-1 min-w-0">
                    <h1 id="hospital-name" class="text-lg font-bold text-gray-900 mb-0.5 leading-snug"></h1>
                    <p id="hospital-type" class="text-sm text-gray-400 mb-2"></p>
                    <p id="hospital-address" class="text-sm text-gray-500 leading-relaxed"></p>
                </div>
                <div class="flex flex-row items-center gap-1.5 flex-shrink-0">
                    <span id="hospital-distance" class="text-sm font-semibold text-[#2563EB] bg-blue-50 px-2.5 py-1 rounded-lg"></span>
                    <button id="detail-fav-btn"
                            data-ykiho=""
                            data-favorited="false"
                            onclick="handleFavoriteClick(this.dataset.ykiho, this, event)"
                            class="fav-btn p-1.5 rounded-xl transition-colors text-gray-300 hover:text-yellow-400 hover:bg-yellow-50"
                            title="즐겨찾기 추가">
                        <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
                            <path d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0
                                     00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0
                                     00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1
                                     1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1
                                     1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0
                                     00.951-.69l1.519-4.674z"></path>
                        </svg>
                    </button>
                </div>
            </div>

            <div class="border-t border-gray-100 pt-4 space-y-2.5">
                <%-- 전화번호 --%>
                <a id="hospital-phone" href="#"
                   class="flex items-center gap-2.5 text-sm text-[#2563EB] font-medium hover:underline min-h-[32px]">
                    <div class="w-7 h-7 bg-blue-50 rounded-lg flex items-center justify-center flex-shrink-0">
                        <svg class="w-3.5 h-3.5 text-[#2563EB]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                                d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z"></path>
                        </svg>
                    </div>
                </a>

                <%-- 길찾기 --%>
                <a id="hospital-directions" href="#" target="_blank" rel="noopener noreferrer"
                   class="flex items-center gap-2.5 text-sm text-gray-600 font-medium hover:text-[#2563EB] transition-colors min-h-[32px]">
                    <div class="w-7 h-7 bg-gray-50 rounded-lg flex items-center justify-center flex-shrink-0">
                        <svg class="w-3.5 h-3.5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                                d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7"></path>
                        </svg>
                    </div>
                    네이버 지도로 길찾기
                </a>


                <%-- 홈페이지 --%>
                <div data-field="hosp-url" class="hidden" style="display:none">
                    <div class="flex items-center gap-2.5">
                        <div class="w-7 h-7 bg-gray-50 rounded-lg flex items-center justify-center flex-shrink-0">
                            <svg class="w-3.5 h-3.5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                                    d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1"></path>
                            </svg>
                        </div>
                        <a id="hospital-url" href="#" target="_blank" rel="noopener noreferrer"
                           class="text-sm text-[#2563EB] hover:underline truncate max-w-[260px]"></a>
                    </div>
                </div>
            </div>
        </div>

        <%-- 검색 항목 가격 카드 (검색 키워드 매칭 시만 노출) --%>
        <div id="section-search-price" class="hidden bg-gradient-to-br from-blue-50 to-white border border-blue-100 rounded-2xl p-5">
            <p class="text-[11px] font-semibold text-[#2563EB] uppercase tracking-wide">내 검색 항목 가격</p>
            <p id="search-item-name" class="text-sm text-gray-700 mt-1.5 truncate"></p>
            <p id="search-item-price" class="text-2xl font-bold text-[#2563EB] mt-1"></p>
        </div>

        <%-- 비급여 진료비 (최상단) --%>
        <div class="bg-white rounded-2xl p-5" style="box-shadow: 0 2px 12px rgba(0,0,0,0.07);">
            <h2 class="text-sm font-semibold text-gray-700 mb-4">비급여 진료비</h2>

            <div id="price-empty" class="hidden text-center py-10 text-gray-400 text-sm">
                등록된 비급여 진료비 정보가 없습니다
            </div>

            <table id="price-table" class="w-full hidden" style="table-layout:fixed;">
                <colgroup>
                    <col style="width:auto;">
                    <col style="width:90px;">
                </colgroup>
                <thead>
                    <tr class="text-left text-xs text-gray-400 border-b border-gray-200">
                        <th class="pb-3 font-medium">항목명</th>
                        <th class="pb-3 font-medium text-right">가격</th>
                    </tr>
                </thead>
                <tbody id="price-tbody" class="divide-y divide-gray-100"></tbody>
            </table>
        </div>

        <%-- 부가 정보 — 접이식 details 그룹 --%>
        <details id="section-dgsbjt" class="hidden bg-white rounded-2xl group" style="box-shadow: 0 2px 12px rgba(0,0,0,0.07);">
            <summary class="cursor-pointer list-none p-5 flex items-center justify-between text-sm font-semibold text-gray-700">
                진료과목
                <svg class="w-4 h-4 text-gray-400 transition-transform group-open:rotate-180" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
                </svg>
            </summary>
            <div id="dgsbjt-list" class="flex flex-wrap gap-1.5 px-5 pb-5"></div>
        </details>

        <details id="section-medoft" class="hidden bg-white rounded-2xl group" style="box-shadow: 0 2px 12px rgba(0,0,0,0.07);">
            <summary class="cursor-pointer list-none p-5 flex items-center justify-between text-sm font-semibold text-gray-700">
                의료장비
                <svg class="w-4 h-4 text-gray-400 transition-transform group-open:rotate-180" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
                </svg>
            </summary>
            <div id="medoft-list" class="space-y-1 px-5 pb-5"></div>
        </details>

        <details id="section-operating" class="hidden bg-white rounded-2xl group" style="box-shadow: 0 2px 12px rgba(0,0,0,0.07);">
            <summary class="cursor-pointer list-none p-5 flex items-center justify-between text-sm font-semibold text-gray-700">
                진료시간
                <svg class="w-4 h-4 text-gray-400 transition-transform group-open:rotate-180" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
                </svg>
            </summary>
            <div id="operating-list" class="space-y-1 px-5 pb-5"></div>
        </details>

        <details id="section-parking" class="hidden bg-white rounded-2xl group" style="box-shadow: 0 2px 12px rgba(0,0,0,0.07);">
            <summary class="cursor-pointer list-none p-5 flex items-center justify-between text-sm font-semibold text-gray-700">
                주차 정보
                <svg class="w-4 h-4 text-gray-400 transition-transform group-open:rotate-180" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
                </svg>
            </summary>
            <div id="parking-list" class="space-y-1 px-5 pb-5"></div>
        </details>


    </div>
</div>
</div>

<script>
    document.addEventListener('DOMContentLoaded', () => {
        const ykiho = new URLSearchParams(location.search).get('ykiho');
        fetchHospitalDetail(ykiho);
    });
</script>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
