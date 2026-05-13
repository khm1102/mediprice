<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="비급여 진료비 비교" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<%-- ════════════════════════════════════════════
     히어로
════════════════════════════════════════════ --%>
<div class="relative flex items-center px-4 bg-[#EEF4FF] overflow-hidden"
     style="min-height: calc(100svh - 3.5rem);">

    <%-- MRI 배경 장식 (우상단) --%>
    <svg class="absolute top-6 -right-8 w-72 h-72 lg:w-96 lg:h-96 text-[#2563EB] opacity-[0.06] pointer-events-none select-none"
         fill="currentColor" viewBox="0 0 297 297" aria-hidden="true">
        <path d="M179.293,23.839c-64.904,0-117.707,52.804-117.707,117.707c0,1.832,0.056,3.651,0.14,5.463H9.933
            c-5.486,0-9.933,4.448-9.933,9.933v46.189c0,5.486,4.448,9.933,9.933,9.933h22.349v43.209h-6.953c-4.663,0-8.443,3.78-8.443,8.443
            c0,4.663,3.78,8.443,8.443,8.443h30.793c4.663,0,8.443-3.78,8.443-8.443c0-4.663-3.78-8.443-8.443-8.443h-6.953v-43.209h36.713
            c21.533,28.059,55.39,46.189,93.411,46.189c64.904,0,117.707-52.804,117.707-117.707S244.196,23.839,179.293,23.839z
            M19.866,166.876h213.087c-5.257,11.091-13.854,20.295-24.483,26.323H19.866V166.876z M119.942,141.547
            c0-32.726,26.624-59.35,59.35-59.35s59.35,26.624,59.35,59.35c0,1.843-0.096,3.663-0.261,5.463H120.206
            C120.04,145.21,119.942,143.39,119.942,141.547z M179.293,239.388c-25.75,0-49.202-10.003-66.689-26.323h32.684
            c10.31,4.925,21.837,7.698,34.005,7.698c43.68,0,79.217-35.536,79.217-79.217S222.973,62.33,179.293,62.33
            s-79.217,35.536-79.217,79.217c0,1.839,0.086,3.657,0.214,5.463H81.611c-0.101-1.809-0.16-3.629-0.16-5.463
            c0-48.871,36.016-89.488,82.901-96.702c-0.758,1.266-1.201,2.741-1.201,4.324c0,4.663,3.78,8.443,8.443,8.443h15.396
            c4.663,0,8.443-3.78,8.443-8.443c0-1.583-0.443-3.058-1.201-4.324c46.884,7.214,82.901,47.83,82.901,96.702
            C277.134,195.497,233.243,239.388,179.293,239.388z"></path>
    </svg>

    <%-- 주사치료 배경 장식 (좌하단) --%>
    <svg class="absolute bottom-6 -left-6 w-56 h-56 lg:w-80 lg:h-80 text-[#2563EB] opacity-[0.06] pointer-events-none select-none"
         style="transform: rotate(-20deg);"
         fill="currentColor" viewBox="0 0 512 512" aria-hidden="true">
        <path d="M317.418,48.457c-8.281-8.281-21.703-8.281-29.984,0s-8.281,21.703,0,29.984l9.141,9.125L137.559,246.582
            c-18.766,18.734-28.156,43.391-28.156,67.922c0,14.141,3.141,28.344,9.375,41.391l-27.719,27.719
            c-2.594,2.578-4.625,5.531-6.047,8.734c-1.391,3.172-2.219,6.609-2.219,10.172c0,2.5,0.406,5.094,1.359,7.578
            c0.813,2.172,2.094,4.266,3.719,6.125l-87.875,87.844H31.73l72-71.984c1.844,1.609,3.938,2.891,6.109,3.703
            c2.5,0.953,5.094,1.359,7.563,1.359c3.594,0,7-0.828,10.203-2.219c3.188-1.422,6.125-3.453,8.734-6.047l27.719-27.703
            c13.031,6.234,27.234,9.391,41.375,9.391c24.547,0,49.203-9.422,67.922-28.156l159.016-159.031l9.125,9.125
            c8.281,8.281,21.703,8.281,29.984,0s8.281-21.719,0-29.969L317.418,48.457z M120.449,413.02c-0.656,0.656-1.375,1.125-1.953,1.375
            c-0.219,0.109-0.422,0.141-0.594,0.203l-12.547-12.578c0.047-0.141,0.094-0.359,0.188-0.578c0.25-0.578,0.703-1.297,1.375-1.938
            l24.172-24.188c2,2.453,4.125,4.813,6.391,7.063v0.031c0.125,0.094,0.25,0.203,0.359,0.344l0.063,0.031
            c2.156,2.141,4.391,4.172,6.719,6.063L120.449,413.02z M257.496,366.52c-14.422,14.391-33.172,21.547-52.063,21.578
            c-18.859-0.031-37.609-7.156-52.016-21.531c-7.203-7.188-12.594-15.484-16.188-24.344c-3.594-8.844-5.391-18.25-5.391-27.719
            c0.031-18.875,7.188-37.641,21.578-52.063l159.016-159l104.063,104.063L257.496,366.52z"></path>
        <path d="M505.793,95.691l-81.547-81.547c-8.281-8.281-21.703-8.281-29.984,0c-8.281,8.297-8.281,21.703,0,29.984
            l26.063,26.094l-28.5,28.5l29.391,29.406l28.516-28.516l26.078,26.078c8.281,8.281,21.703,8.281,29.984,0
            S514.074,103.973,505.793,95.691z"></path>
        <polygon points="363.949,186.129 333.824,156.004 326.762,163.051 356.887,193.176"></polygon>
        <polygon points="285.027,204.801 315.137,234.91 322.199,227.879 292.074,197.738"></polygon>
        <path d="M253.215,227.926c-6.047-6.047-9.688-11.219-12.234-15.984l-69.047,69.031
            c-18.484,18.5-18.484,48.563-0.031,67.047c18.484,18.484,48.578,18.469,67.078-0.016l76.984-77
            c-2.047-2.719-4.531-5.625-7.672-8.781C285.949,239.879,275.559,250.254,253.215,227.926z"></path>
    </svg>

    <div class="max-w-2xl mx-auto text-center w-full py-16 relative z-10">

        <span class="inline-flex items-center gap-1.5 text-xs text-blue-600 mb-6 bg-white rounded-full px-3.5 py-1.5 border border-blue-100" style="box-shadow: 0 1px 6px rgba(37,99,235,0.08);">
            <svg class="w-3.5 h-3.5 flex-shrink-0 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
            </svg>
            건강보험심사평가원 공식 데이터 기반
        </span>

        <h1 class="text-3xl lg:text-5xl font-bold text-gray-900 mb-4 leading-tight tracking-tight">
            비급여 진료비,<br>지금 바로 비교하세요
        </h1>
        <p class="text-gray-500 text-sm lg:text-base mb-10 leading-relaxed">
            병원마다 다른 가격,<br class="sm:hidden"/>검색 한 번으로 확인하세요
        </p>

        <div class="flex gap-2 bg-white rounded-2xl p-1.5 max-w-lg mx-auto" style="box-shadow: 0 8px 40px rgba(0,0,0,0.18), 0 2px 8px rgba(0,0,0,0.08);">
            <div class="flex-1 flex items-center gap-2 px-3">
                <svg class="w-4 h-4 text-gray-300 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                </svg>
                <input type="text" id="search-input"
                       placeholder="도수치료, MRI, 초음파..."
                       class="flex-1 text-sm text-gray-800 placeholder-gray-300 focus:outline-none py-2.5 bg-transparent"/>
            </div>
            <button onclick="handleSearch()"
                class="bg-[#2563EB] text-white px-5 rounded-xl text-sm font-semibold hover:bg-blue-700 transition-colors flex-shrink-0 min-h-[44px]">
                검색
            </button>
        </div>

        <p class="mt-6 text-[11px] text-gray-400 leading-relaxed">
            ※ 2026년 비급여 진료비용 공개를 위한 자료수집 및 검증기간(4월~8월)으로,<br/>
            해당 기간에는 변경사항이 반영되지 않아 조회 금액과 실제 금액 간 차이가 있을 수 있습니다.
        </p>

    </div>
</div><%-- /히어로 --%>

<%-- ════════════════════════════════════════════
     통계
════════════════════════════════════════════ --%>
<div class="bg-white">
    <div class="max-w-5xl mx-auto px-4 pt-10 pb-10 lg:pt-12 lg:pb-12">
        <div class="grid grid-cols-3 divide-x divide-gray-200">
            <div class="text-center px-4">
                <p class="text-3xl lg:text-4xl font-bold text-[#2563EB] tracking-tight">최대 6배</p>
                <p class="text-xs lg:text-sm text-gray-500 mt-2 leading-snug">MRI 병원 간<br>최저·최고 가격 차이</p>
            </div>
            <div class="text-center px-4">
                <p class="text-3xl lg:text-4xl font-bold text-[#2563EB] tracking-tight">75,065</p>
                <p class="text-xs lg:text-sm text-gray-500 mt-2 leading-snug">전국 병·의원급<br>의료기관 수</p>
            </div>
            <div class="text-center px-4">
                <p class="text-3xl lg:text-4xl font-bold text-[#2563EB] tracking-tight">693개</p>
                <p class="text-xs lg:text-sm text-gray-500 mt-2 leading-snug">비급여<br>공개 항목 수</p>
            </div>
        </div>
    </div>
</div>

<%-- ════════════════════════════════════════════
     자주 찾는 항목
════════════════════════════════════════════ --%>
<div class="bg-[#F2F4F6]">
    <div class="max-w-5xl mx-auto px-4 py-10">

        <p class="text-[11px] font-bold text-gray-400 uppercase tracking-widest mb-5">자주 찾는 항목</p>

        <div class="grid grid-cols-2 lg:grid-cols-5 gap-3">

            <%-- 도수치료 --%>
            <a href="<c:url value='/hospitals?keyword=도수치료'/>"
               class="flex items-center gap-3 bg-white rounded-2xl p-4 hover:shadow-md transition-all group min-h-[68px]"
               style="box-shadow: 0 2px 10px rgba(0,0,0,0.09);">
                <div class="w-10 h-10 bg-blue-50 rounded-xl flex items-center justify-center flex-shrink-0 group-hover:bg-blue-100 transition-colors">
                    <svg class="w-5 h-5 text-[#2563EB]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                            d="M12.3562 15.2061L10.9313 16.6311C10.5378 17.0246 10.562 17.674 10.7105 18.2103C10.9908 19.2235 10.6058 20.519 9.86257 21.2622C8.87884 22.2459 7.28391 22.2459 6.30018 21.2622C5.31645 20.2785 5.31646 18.6835 6.30018 17.6998C5.31646 18.6835 3.72152 18.6835 2.73779 17.6998C1.75407 16.7161 1.75407 15.1212 2.73779 14.1374C3.48101 13.3942 4.77646 13.0092 5.7897 13.2895C6.32603 13.438 6.97541 13.4622 7.3689 13.0687L13.0687 7.3689C13.4622 6.97541 13.438 6.32603 13.2895 5.7897C13.0092 4.77646 13.3942 3.48102 14.1374 2.73779C15.1212 1.75407 16.7161 1.75407 17.6998 2.73779C18.6835 3.72152 18.6835 5.31646 17.6998 6.30018C18.6835 5.31646 20.2785 5.31646 21.2622 6.30018C22.2459 7.28391 22.2459 8.87884 21.2622 9.86257C20.519 10.6058 19.2235 10.9908 18.2103 10.7105C17.674 10.562 17.0246 10.5378 16.6311 10.9313L15.2061 12.3562"></path>
                    </svg>
                </div>
                <div class="min-w-0">
                    <p class="text-sm font-semibold text-gray-800 group-hover:text-[#2563EB] transition-colors truncate">도수치료</p>
                    <p class="text-xs text-gray-400 mt-0.5 truncate">정형외과 · 재활</p>
                </div>
            </a>

            <%-- MRI --%>
            <a href="<c:url value='/hospitals?keyword=MRI'/>"
               class="flex items-center gap-3 bg-white rounded-2xl p-4 hover:shadow-md transition-all group min-h-[68px]"
               style="box-shadow: 0 2px 10px rgba(0,0,0,0.09);">
                <div class="w-10 h-10 bg-blue-50 rounded-xl flex items-center justify-center flex-shrink-0 group-hover:bg-blue-100 transition-colors">
                    <svg class="w-5 h-5 text-[#2563EB]" fill="currentColor" viewBox="0 0 297 297">
                        <path d="M179.293,23.839c-64.904,0-117.707,52.804-117.707,117.707c0,1.832,0.056,3.651,0.14,5.463H9.933
                            c-5.486,0-9.933,4.448-9.933,9.933v46.189c0,5.486,4.448,9.933,9.933,9.933h22.349v43.209h-6.953c-4.663,0-8.443,3.78-8.443,8.443
                            c0,4.663,3.78,8.443,8.443,8.443h30.793c4.663,0,8.443-3.78,8.443-8.443c0-4.663-3.78-8.443-8.443-8.443h-6.953v-43.209h36.713
                            c21.533,28.059,55.39,46.189,93.411,46.189c64.904,0,117.707-52.804,117.707-117.707S244.196,23.839,179.293,23.839z
                            M19.866,166.876h213.087c-5.257,11.091-13.854,20.295-24.483,26.323H19.866V166.876z M119.942,141.547
                            c0-32.726,26.624-59.35,59.35-59.35s59.35,26.624,59.35,59.35c0,1.843-0.096,3.663-0.261,5.463H120.206
                            C120.04,145.21,119.942,143.39,119.942,141.547z M179.293,239.388c-25.75,0-49.202-10.003-66.689-26.323h32.684
                            c10.31,4.925,21.837,7.698,34.005,7.698c43.68,0,79.217-35.536,79.217-79.217S222.973,62.33,179.293,62.33
                            s-79.217,35.536-79.217,79.217c0,1.839,0.086,3.657,0.214,5.463H81.611c-0.101-1.809-0.16-3.629-0.16-5.463
                            c0-48.871,36.016-89.488,82.901-96.702c-0.758,1.266-1.201,2.741-1.201,4.324c0,4.663,3.78,8.443,8.443,8.443h15.396
                            c4.663,0,8.443-3.78,8.443-8.443c0-1.583-0.443-3.058-1.201-4.324c46.884,7.214,82.901,47.83,82.901,96.702
                            C277.134,195.497,233.243,239.388,179.293,239.388z"></path>
                    </svg>
                </div>
                <div class="min-w-0">
                    <p class="text-sm font-semibold text-gray-800 group-hover:text-[#2563EB] transition-colors truncate">MRI</p>
                    <p class="text-xs text-gray-400 mt-0.5 truncate">영상의학과</p>
                </div>
            </a>

            <%-- 체외충격파 --%>
            <a href="<c:url value='/hospitals?keyword=체외충격파'/>"
               class="flex items-center gap-3 bg-white rounded-2xl p-4 hover:shadow-md transition-all group min-h-[68px]"
               style="box-shadow: 0 2px 10px rgba(0,0,0,0.09);">
                <div class="w-10 h-10 bg-blue-50 rounded-xl flex items-center justify-center flex-shrink-0 group-hover:bg-blue-100 transition-colors">
                    <svg class="w-5 h-5 text-[#2563EB]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                            d="M3.75 13.5l10.5-11.25L12 10.5h8.25L9.75 21.75 12 13.5H3.75z"></path>
                    </svg>
                </div>
                <div class="min-w-0">
                    <p class="text-sm font-semibold text-gray-800 group-hover:text-[#2563EB] transition-colors truncate">체외충격파</p>
                    <p class="text-xs text-gray-400 mt-0.5 truncate">정형외과</p>
                </div>
            </a>

            <%-- 초음파 --%>
            <a href="<c:url value='/hospitals?keyword=초음파'/>"
               class="flex items-center gap-3 bg-white rounded-2xl p-4 hover:shadow-md transition-all group min-h-[68px]"
               style="box-shadow: 0 2px 10px rgba(0,0,0,0.09);">
                <div class="w-10 h-10 bg-blue-50 rounded-xl flex items-center justify-center flex-shrink-0 group-hover:bg-blue-100 transition-colors">
                    <svg class="w-5 h-5 text-[#2563EB]" fill="currentColor" viewBox="0 0 256 256">
                        <path d="M215,94.5l-70.9-70.9c-9.1,9.1-23.8,9.1-32.9,0L40.3,94.5C88.6,142.8,166.8,142.8,215,94.5z M145.6,52.1
                            c7.7-5.3,18.3-3.3,23.6,4.4c5.3,7.7,3.3,18.3-4.4,23.6c-7.7,5.3-18.3,3.3-23.6-4.4C135.9,68,137.8,57.5,145.6,52.1z M89.5,114.9
                            c-2.1-3.1-3.1-6.7-3.6-10.2c-0.8-5.4,3-16.2,3-16.2c-4.3,0.4-14.9,0.7-19.5,1.1c-4.1,0.4-7.8-2.7-8.1-6.8c-0.4-4.1,2.7-7.8,6.8-8.1
                            c6.5-0.5,24.5-1.8,24.5-1.8c5.5-0.5,10.3,3.6,10.8,9.1l-1,14.3l18.9-12.5l-6.3-6.6c-1.4-1.4-2.1-3.4-2-5.4l0.9-18.6
                            c0.2-4,3.6-7.1,7.6-6.9c4,0.2,7.2,3.6,7,7.6c0,0-0.5,11.2-0.8,15.5c3.4,3.6,17.2,18.1,17.2,18.1c0.8,0.8,1.2,1.7,1.6,2.7
                            c2.5,4.8,1.1,10.8-3.5,13.9c-3.2,2.2-20.4,14-23.9,16.3C109.4,127,96.2,124.6,89.5,114.9z M241,4H14v178h122.2l13.3,34.6
                            c3.8,10.4,12.7,17.5,22.8,19.4l6.2,18.9l52.3,0l-12.4-35.7c6.5-8,8.8-19.3,5-29.6L220,182h21V4z M230,171h-15.7l-12.9-35.3
                            c0,0,0,0,0,0c-1.5-3.8-5.3-6.6-9.7-6.6c-5.8,0-10.4,4.7-10.4,10.4c0,2.4,2.6,8.8,3.1,10.2l7.8,21.3H25V14h205V171z"></path>
                    </svg>
                </div>
                <div class="min-w-0">
                    <p class="text-sm font-semibold text-gray-800 group-hover:text-[#2563EB] transition-colors truncate">초음파</p>
                    <p class="text-xs text-gray-400 mt-0.5 truncate">내과 · 산부인과</p>
                </div>
            </a>

            <%-- 주사치료 --%>
            <a href="<c:url value='/hospitals?keyword=주사치료'/>"
               class="flex items-center gap-3 bg-white rounded-2xl p-4 hover:shadow-md transition-all group min-h-[68px]"
               style="box-shadow: 0 2px 10px rgba(0,0,0,0.09);">
                <div class="w-10 h-10 bg-blue-50 rounded-xl flex items-center justify-center flex-shrink-0 group-hover:bg-blue-100 transition-colors">
                    <svg class="w-5 h-5 text-[#2563EB]" fill="currentColor" viewBox="0 0 512 512">
                        <path d="M317.418,48.457c-8.281-8.281-21.703-8.281-29.984,0s-8.281,21.703,0,29.984l9.141,9.125L137.559,246.582
                            c-18.766,18.734-28.156,43.391-28.156,67.922c0,14.141,3.141,28.344,9.375,41.391l-27.719,27.719
                            c-2.594,2.578-4.625,5.531-6.047,8.734c-1.391,3.172-2.219,6.609-2.219,10.172c0,2.5,0.406,5.094,1.359,7.578
                            c0.813,2.172,2.094,4.266,3.719,6.125l-87.875,87.844H31.73l72-71.984c1.844,1.609,3.938,2.891,6.109,3.703
                            c2.5,0.953,5.094,1.359,7.563,1.359c3.594,0,7-0.828,10.203-2.219c3.188-1.422,6.125-3.453,8.734-6.047l27.719-27.703
                            c13.031,6.234,27.234,9.391,41.375,9.391c24.547,0,49.203-9.422,67.922-28.156l159.016-159.031l9.125,9.125
                            c8.281,8.281,21.703,8.281,29.984,0s8.281-21.719,0-29.969L317.418,48.457z M120.449,413.02c-0.656,0.656-1.375,1.125-1.953,1.375
                            c-0.219,0.109-0.422,0.141-0.594,0.203l-12.547-12.578c0.047-0.141,0.094-0.359,0.188-0.578c0.25-0.578,0.703-1.297,1.375-1.938
                            l24.172-24.188c2,2.453,4.125,4.813,6.391,7.063v0.031c0.125,0.094,0.25,0.203,0.359,0.344l0.063,0.031
                            c2.156,2.141,4.391,4.172,6.719,6.063L120.449,413.02z M257.496,366.52c-14.422,14.391-33.172,21.547-52.063,21.578
                            c-18.859-0.031-37.609-7.156-52.016-21.531c-7.203-7.188-12.594-15.484-16.188-24.344c-3.594-8.844-5.391-18.25-5.391-27.719
                            c0.031-18.875,7.188-37.641,21.578-52.063l159.016-159l104.063,104.063L257.496,366.52z"></path>
                        <path d="M505.793,95.691l-81.547-81.547c-8.281-8.281-21.703-8.281-29.984,0c-8.281,8.297-8.281,21.703,0,29.984
                            l26.063,26.094l-28.5,28.5l29.391,29.406l28.516-28.516l26.078,26.078c8.281,8.281,21.703,8.281,29.984,0
                            S514.074,103.973,505.793,95.691z"></path>
                        <polygon points="363.949,186.129 333.824,156.004 326.762,163.051 356.887,193.176"></polygon>
                        <polygon points="285.027,204.801 315.137,234.91 322.199,227.879 292.074,197.738"></polygon>
                        <path d="M253.215,227.926c-6.047-6.047-9.688-11.219-12.234-15.984l-69.047,69.031
                            c-18.484,18.5-18.484,48.563-0.031,67.047c18.484,18.484,48.578,18.469,67.078-0.016l76.984-77
                            c-2.047-2.719-4.531-5.625-7.672-8.781C285.949,239.879,275.559,250.254,253.215,227.926z"></path>
                    </svg>
                </div>
                <div class="min-w-0">
                    <p class="text-sm font-semibold text-gray-800 group-hover:text-[#2563EB] transition-colors truncate">주사치료</p>
                    <p class="text-xs text-gray-400 mt-0.5 truncate">통증의학과</p>
                </div>
            </a>

            <%-- 수면내시경 --%>
            <a href="<c:url value='/hospitals?keyword=수면내시경'/>"
               class="flex items-center gap-3 bg-white rounded-2xl p-4 hover:shadow-md transition-all group min-h-[68px]"
               style="box-shadow: 0 2px 10px rgba(0,0,0,0.09);">
                <div class="w-10 h-10 bg-blue-50 rounded-xl flex items-center justify-center flex-shrink-0 group-hover:bg-blue-100 transition-colors">
                    <svg class="w-5 h-5 text-[#2563EB]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                            d="M21.752 15.002A9.718 9.718 0 0118 15.75c-5.385 0-9.75-4.365-9.75-9.75 0-1.33.266-2.597.748-3.752A9.753 9.753 0 003 11.25C3 16.635 7.365 21 12.75 21a9.753 9.753 0 009.002-5.998z"></path>
                    </svg>
                </div>
                <div class="min-w-0">
                    <p class="text-sm font-semibold text-gray-800 group-hover:text-[#2563EB] transition-colors truncate">수면내시경</p>
                    <p class="text-xs text-gray-400 mt-0.5 truncate">소화기내과</p>
                </div>
            </a>

            <%-- 보톡스 --%>
            <a href="<c:url value='/hospitals?keyword=보톡스'/>"
               class="flex items-center gap-3 bg-white rounded-2xl p-4 hover:shadow-md transition-all group min-h-[68px]"
               style="box-shadow: 0 2px 10px rgba(0,0,0,0.09);">
                <div class="w-10 h-10 bg-blue-50 rounded-xl flex items-center justify-center flex-shrink-0 group-hover:bg-blue-100 transition-colors">
                    <svg class="w-5 h-5 text-[#2563EB]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                            d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09z"></path>
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                            d="M18.259 8.715L18 9.75l-.259-1.035a3.375 3.375 0 00-2.455-2.456L14.25 6l1.036-.259a3.375 3.375 0 002.455-2.456L18 2.25l.259 1.035a3.375 3.375 0 002.456 2.456L21.75 6l-1.035.259a3.375 3.375 0 00-2.456 2.456z"></path>
                    </svg>
                </div>
                <div class="min-w-0">
                    <p class="text-sm font-semibold text-gray-800 group-hover:text-[#2563EB] transition-colors truncate">보톡스</p>
                    <p class="text-xs text-gray-400 mt-0.5 truncate">피부과 · 성형외과</p>
                </div>
            </a>

            <%-- 필러 --%>
            <a href="<c:url value='/hospitals?keyword=필러'/>"
               class="flex items-center gap-3 bg-white rounded-2xl p-4 hover:shadow-md transition-all group min-h-[68px]"
               style="box-shadow: 0 2px 10px rgba(0,0,0,0.09);">
                <div class="w-10 h-10 bg-blue-50 rounded-xl flex items-center justify-center flex-shrink-0 group-hover:bg-blue-100 transition-colors">
                    <svg class="w-5 h-5 text-[#2563EB]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                            d="M12 3c-4.97 5.076-7.5 8.834-7.5 11.5a7.5 7.5 0 0015 0C19.5 11.834 16.97 8.076 12 3z"></path>
                    </svg>
                </div>
                <div class="min-w-0">
                    <p class="text-sm font-semibold text-gray-800 group-hover:text-[#2563EB] transition-colors truncate">필러</p>
                    <p class="text-xs text-gray-400 mt-0.5 truncate">피부과 · 성형외과</p>
                </div>
            </a>

            <%-- 레이저시술 --%>
            <a href="<c:url value='/hospitals?keyword=레이저시술'/>"
               class="flex items-center gap-3 bg-white rounded-2xl p-4 hover:shadow-md transition-all group min-h-[68px]"
               style="box-shadow: 0 2px 10px rgba(0,0,0,0.09);">
                <div class="w-10 h-10 bg-blue-50 rounded-xl flex items-center justify-center flex-shrink-0 group-hover:bg-blue-100 transition-colors">
                    <svg class="w-5 h-5 text-[#2563EB]" fill="currentColor" viewBox="0 0 512 512">
                        <path d="M16.613 14.686l35.98 46.98c-5.556 6.996-9.327 14.675-11 22.42-2.15 9.954-.637 20.843 6.82 28.3 7.46 7.46 18.346 8.97 28.3 6.82 5.185-1.12 10.34-3.183 15.283-6.087l19.78 25.83c-5.053 7.936-8.567 16.252-10.346 24.49-3.037 14.06-.845 28.715 9.07 38.63 9.915 9.915 24.572 12.107 38.633 9.07 4.758-1.027 9.54-2.643 14.263-4.786l26.323 34.373c-4.797 8.88-8.23 17.956-10.16 26.892-4.22 19.544-1.12 39.23 12.075 52.427 13.195 13.195 32.883 16.295 52.428 12.074 4.237-.914 8.506-2.183 12.77-3.755l28.09 36.676c-6.446 12.33-11.057 24.856-13.704 37.114-6.097 28.228-1.585 55.878 16.805 74.268s46.04 22.902 74.268 16.807c5.34-1.155 10.734-2.682 16.14-4.57 10.45 4.697 22.042 7.33 34.272 7.33 46.236 0 83.514-37.32 83.514-83.556 0-12.21-2.62-23.783-7.298-34.22 1.898-5.428 3.434-10.842 4.592-16.204 6.095-28.227 1.554-55.848-16.836-74.24-13.218-13.216-31.205-19.28-50.758-19.383-7.65-.04-15.542.832-23.48 2.547-11.507 2.485-23.252 6.7-34.846 12.537l-37.31-28.57c1.57-4.256 2.834-8.517 3.747-12.746 4.222-19.544 1.122-39.234-12.073-52.43-9.278-9.277-21.767-13.563-35.158-13.824-.894-.017-1.79-.016-2.69.002-4.8.097-9.695.695-14.58 1.75-8.928 1.928-17.996 5.357-26.868 10.145l-34.39-26.334c2.14-4.715 3.75-9.49 4.777-14.238 3.035-14.062.843-28.718-9.07-38.633-7.438-7.436-17.54-10.528-28.06-10.328-3.506.066-7.06.498-10.574 1.257-8.228 1.778-16.534 5.286-24.463 10.332l-25.84-19.788c2.897-4.935 4.956-10.084 6.074-15.26 2.15-9.956.638-20.842-6.82-28.3-5.594-5.594-13.117-7.843-20.73-7.71-2.536.043-5.082.352-7.57.89-7.74 1.67-15.41 5.436-22.4 10.982l-46.984-35.98zm77.213 42.752c3.488-.127 5.814.84 7.254 2.28 1.92 1.92 3.003 5.42 1.768 11.14-.513 2.376-1.486 4.995-2.858 7.68l-20.838-15.96c3.754-2.346 7.502-3.917 10.79-4.627 1.43-.308 2.722-.47 3.884-.513zm-29.322 19.78L80.47 98.07c-2.693 1.378-5.32 2.355-7.7 2.87-5.723 1.235-9.22.152-11.14-1.768s-3.004-5.417-1.77-11.14c.713-3.293 2.29-7.05 4.644-10.81zm111.674 39.686c6.395-.194 11.303 1.62 14.586 4.903 4.377 4.377 6.14 11.643 4.017 21.472-.445 2.066-1.082 4.213-1.887 6.402l-35.868-27.467c4.22-2.096 8.38-3.587 12.266-4.426 2.458-.532 4.756-.82 6.888-.886zm-52.04 38.186l27.48 35.885c-2.198.81-4.354 1.452-6.43 1.9-9.83 2.123-17.096.36-21.473-4.018-4.377-4.377-6.14-11.645-4.018-21.474.84-3.895 2.336-8.063 4.44-12.293zm162.133 39.443c9.408.11 17.085 3.027 22.468 8.41 7.657 7.657 10.328 19.953 7.022 35.264-.33 1.53-.74 3.087-1.194 4.656l-55.27-42.328c4.838-2.073 9.607-3.624 14.18-4.61 3.826-.828 7.466-1.28 10.89-1.376.643-.02 1.278-.024 1.905-.017zm-83.815 62.824l42.344 55.29c-1.58.458-3.144.87-4.683 1.203-15.31 3.306-27.607.636-35.263-7.02-7.657-7.656-10.33-19.954-7.022-35.266.99-4.58 2.544-9.36 4.623-14.207zm223.16 29.672c15.535 0 28.615 4.717 37.85 13.952 12.687 12.688 16.832 32.637 11.947 56.235-11.877-13.486-28.038-23.1-46.402-26.713l-44.63-34.178c7.452-3.184 14.83-5.58 21.954-7.117 6.748-1.458 13.202-2.18 19.28-2.18zm-127.953 94.646l32.307 42.185c2.768 20.375 12.847 38.37 27.51 51.296-23.602 4.89-43.553.744-56.242-11.945-12.85-12.848-16.955-33.14-11.773-57.132 1.706-7.9 4.467-16.115 8.197-24.402z"></path>
                    </svg>
                </div>
                <div class="min-w-0">
                    <p class="text-sm font-semibold text-gray-800 group-hover:text-[#2563EB] transition-colors truncate">레이저 시술</p>
                    <p class="text-xs text-gray-400 mt-0.5 truncate">피부과</p>
                </div>
            </a>

            <%-- 추나요법 --%>
            <a href="<c:url value='/hospitals?keyword=추나요법'/>"
               class="flex items-center gap-3 bg-white rounded-2xl p-4 hover:shadow-md transition-all group min-h-[68px]"
               style="box-shadow: 0 2px 10px rgba(0,0,0,0.09);">
                <div class="w-10 h-10 bg-blue-50 rounded-xl flex items-center justify-center flex-shrink-0 group-hover:bg-blue-100 transition-colors">
                    <svg class="w-5 h-5 text-[#2563EB]" fill="currentColor" viewBox="0 0 280.919 280.919">
                        <path d="M213.999,23.952h-12.268V9.597c0-3.36-1.709-6.418-4.563-8.175c-2.82-1.74-6.409-1.894-9.37-0.399
                            c-9.173,4.633-27.754,7.627-47.338,7.627c-19.585,0-38.166-2.994-47.343-7.629c-2.954-1.49-6.54-1.34-9.363,0.396
                            c-2.859,1.762-4.566,4.819-4.566,8.18v14.355H66.92c-8.656,0-15.698,7.042-15.698,15.698v10.91
                            c0,8.656,7.042,15.698,15.698,15.698h12.268v7.164c0,5.686,3.323,10.924,8.466,13.345c17.352,8.167,47.033,8.581,52.807,8.581
                            c16.676,0,39.3-2.253,52.786-8.577c5.154-2.417,8.485-7.658,8.485-13.354v-7.159h12.268c8.656,0,15.698-7.042,15.698-15.698V39.65
                            C229.697,30.994,222.655,23.952,213.999,23.952z M214.697,50.56c0,0.385-0.313,0.698-0.698,0.698h-19.768
                            c-4.143,0-7.5,3.357-7.5,7.5v14.5c-11.3,5.231-31.962,7.09-46.271,7.09c-14.341,0-35.023-1.857-46.272-7.085V58.759
                            c0-4.143-3.357-7.5-7.5-7.5H66.92c-0.385,0-0.698-0.313-0.698-0.698V39.65c0-0.385,0.313-0.698,0.698-0.698h19.768
                            c4.143,0,7.5-3.357,7.5-7.5V17.579c11.762,3.854,28.312,6.07,46.272,6.07s34.512-2.216,46.271-6.069v13.872
                            c0,4.143,3.357,7.5,7.5,7.5h19.768c0.385,0,0.698,0.313,0.698,0.698V50.56z"></path>
                        <path d="M212.094,116.558h-10.362v-14.355c0-3.311-1.751-6.444-4.563-8.175c-2.82-1.74-6.409-1.894-9.37-0.399
                            c-9.173,4.633-27.754,7.627-47.338,7.627c-19.585,0-38.166-2.994-47.343-7.629c-2.961-1.493-6.55-1.34-9.355,0.393
                            c-2.821,1.733-4.574,4.869-4.574,8.184v14.355H68.825c-9.707,0-17.604,7.896-17.604,17.604v7.101
                            c0,9.706,7.896,17.603,17.604,17.603h10.362v7.164c0,5.686,3.323,10.924,8.466,13.345c17.352,8.167,47.033,8.581,52.807,8.581
                            c16.678,0,39.303-2.253,52.786-8.577c5.154-2.417,8.485-7.658,8.485-13.354v-7.159h10.362c9.707,0,17.604-7.896,17.604-17.603
                            v-7.101C229.697,124.454,221.801,116.558,212.094,116.558z M214.697,141.262c0,1.435-1.168,2.603-2.604,2.603h-17.862
                            c-4.143,0-7.5,3.357-7.5,7.5v14.5c-11.297,5.231-31.961,7.09-46.271,7.09c-14.341,0-35.023-1.857-46.272-7.085v-14.505
                            c0-4.143-3.357-7.5-7.5-7.5H68.825c-1.436,0-2.604-1.168-2.604-2.603v-7.101c0-1.436,1.168-2.604,2.604-2.604h17.862
                            c4.143,0,7.5-3.357,7.5-7.5v-13.873c11.762,3.854,28.312,6.07,46.272,6.07s34.512-2.216,46.271-6.069v13.872
                            c0,4.143,3.357,7.5,7.5,7.5h17.862c1.436,0,2.604,1.168,2.604,2.604V141.262z"></path>
                        <path d="M108.576,26.972c-4.143,0-7.5,3.357-7.5,7.5v28.832c0,4.143,3.357,7.5,7.5,7.5s7.5-3.357,7.5-7.5V34.472
                            C116.076,30.329,112.719,26.972,108.576,26.972z"></path>
                        <path d="M108.576,120.294c-4.143,0-7.5,3.357-7.5,7.5v27.832c0,4.143,3.357,7.5,7.5,7.5s7.5-3.357,7.5-7.5v-27.832
                            C116.076,123.651,112.719,120.294,108.576,120.294z"></path>
                        <path d="M212.094,209.522h-10.362v-14.356c0-3.311-1.751-6.444-4.57-8.18c-2.82-1.734-6.406-1.887-9.363-0.395
                            c-9.173,4.633-27.754,7.627-47.338,7.627c-19.585,0-38.166-2.994-47.343-7.629c-2.961-1.493-6.55-1.34-9.355,0.393
                            c-2.821,1.733-4.574,4.869-4.574,8.184v14.356H68.825c-9.707,0-17.604,7.896-17.604,17.603v7.101
                            c0,9.707,7.896,17.604,17.604,17.604h10.362v7.163c0,5.684,3.322,10.922,8.467,13.346c17.354,8.167,47.032,8.581,52.806,8.581
                            c16.677,0,39.301-2.253,52.786-8.578c5.154-2.417,8.485-7.658,8.485-13.354v-7.158h10.362c9.707,0,17.604-7.896,17.604-17.604
                            v-7.101C229.697,217.419,221.801,209.522,212.094,209.522z M214.697,234.225c0,1.436-1.168,2.604-2.604,2.604h-17.862
                            c-4.143,0-7.5,3.357-7.5,7.5v14.499c-11.299,5.232-31.962,7.091-46.271,7.091c-14.341,0-35.023-1.857-46.272-7.085v-14.505
                            c0-4.143-3.357-7.5-7.5-7.5H68.825c-1.436,0-2.604-1.168-2.604-2.604v-7.101c0-1.436,1.168-2.603,2.604-2.603h17.862
                            c4.143,0,7.5-3.357,7.5-7.5v-13.873c11.762,3.854,28.312,6.069,46.272,6.069s34.512-2.216,46.271-6.069v13.873
                            c0,4.143,3.357,7.5,7.5,7.5h17.862c1.436,0,2.604,1.167,2.604,2.603V234.225z"></path>
                        <path d="M108.576,213.258c-4.143,0-7.5,3.357-7.5,7.5v27.833c0,4.143,3.357,7.5,7.5,7.5s7.5-3.357,7.5-7.5v-27.833
                            C116.076,216.615,112.719,213.258,108.576,213.258z"></path>
                    </svg>
                </div>
                <div class="min-w-0">
                    <p class="text-sm font-semibold text-gray-800 group-hover:text-[#2563EB] transition-colors truncate">추나요법</p>
                    <p class="text-xs text-gray-400 mt-0.5 truncate">한의원</p>
                </div>
            </a>

        </div>
    </div>
</div>

<%-- ════════════════════════════════════════════
     이용 방법
════════════════════════════════════════════ --%>
<div class="bg-white">
    <div class="max-w-5xl mx-auto px-4 py-10">

        <p class="text-[11px] font-bold text-gray-400 uppercase tracking-widest mb-8">이용 방법</p>

        <div class="grid grid-cols-1 md:grid-cols-3 gap-4">

            <div class="rounded-2xl p-6" style="background: #F0F6FF;">
                <div class="flex items-center justify-between mb-5">
                    <div class="w-11 h-11 bg-white rounded-xl flex items-center justify-center" style="box-shadow: 0 2px 8px rgba(37,99,235,0.15);">
                        <svg class="w-5 h-5 text-[#2563EB]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                                d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                        </svg>
                    </div>
                    <span class="text-3xl font-black text-[#2563EB]/20 leading-none">01</span>
                </div>
                <p class="text-sm font-semibold text-gray-800 mb-2">진료 항목 검색</p>
                <p class="text-xs text-gray-500 leading-relaxed">받고 싶은 시술이나 검사 항목을<br>검색창에 입력하세요</p>
            </div>

            <div class="rounded-2xl p-6" style="background: #F0F6FF;">
                <div class="flex items-center justify-between mb-5">
                    <div class="w-11 h-11 bg-white rounded-xl flex items-center justify-center" style="box-shadow: 0 2px 8px rgba(37,99,235,0.15);">
                        <svg class="w-5 h-5 text-[#2563EB]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                                d="M15 10.5a3 3 0 11-6 0 3 3 0 016 0z"></path>
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                                d="M19.5 10.5c0 7.142-7.5 11.25-7.5 11.25S4.5 17.642 4.5 10.5a7.5 7.5 0 1115 0z"></path>
                        </svg>
                    </div>
                    <span class="text-3xl font-black text-[#2563EB]/20 leading-none">02</span>
                </div>
                <p class="text-sm font-semibold text-gray-800 mb-2">주변 병원 확인</p>
                <p class="text-xs text-gray-500 leading-relaxed">현재 위치 기반으로<br>가까운 병원을 지도에서 확인하세요</p>
            </div>

            <div class="rounded-2xl p-6" style="background: #F0F6FF;">
                <div class="flex items-center justify-between mb-5">
                    <div class="w-11 h-11 bg-white rounded-xl flex items-center justify-center" style="box-shadow: 0 2px 8px rgba(37,99,235,0.15);">
                        <svg class="w-5 h-5 text-[#2563EB]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                                d="M3 6l3 1m0 0l-3 9a5.002 5.002 0 006.001 0M6 7l3 9M6 7l6-2m6 2l3-1m-3 1l-3 9a5.002 5.002 0 006.001 0M18 7l3 9m-3-9l-6-2m0-2v2m0 16V5m0 16H9m3 0h3"></path>
                        </svg>
                    </div>
                    <span class="text-3xl font-black text-[#2563EB]/20 leading-none">03</span>
                </div>
                <p class="text-sm font-semibold text-gray-800 mb-2">가격 비교 후 선택</p>
                <p class="text-xs text-gray-500 leading-relaxed">병원별 최저·최고 가격을<br>비교하고 합리적으로 선택하세요</p>
            </div>

        </div>
    </div>
</div>

<%-- ════════════════════════════════════════════
     비급여란?
════════════════════════════════════════════ --%>
<div class="bg-[#F2F4F6]">
    <div class="max-w-5xl mx-auto px-4 py-10">
        <div class="bg-white rounded-3xl overflow-hidden" style="box-shadow: 0 2px 12px rgba(0,0,0,0.07);">
            <div class="lg:grid lg:grid-cols-2">

                <%-- 텍스트 영역 --%>
                <div class="p-8 lg:p-10 flex flex-col justify-center">
                    <p class="text-[11px] font-bold text-gray-400 uppercase tracking-widest mb-4">비급여 진료비란?</p>
                    <h2 class="text-xl lg:text-2xl font-bold text-gray-900 mb-4 leading-snug">
                        건강보험이 적용되지 않아<br>환자가 전액 부담하는 진료비
                    </h2>
                    <p class="text-sm text-gray-500 leading-relaxed mb-4">
                        급여 항목과 달리 비급여 항목은<br class="hidden lg:block"/>
                        건강보험 혜택 없이 전액 본인 부담입니다.<br>
                        병원이 자체적으로 금액을 정하기 때문에<br class="hidden lg:block"/>
                        같은 시술도 병원마다 수배 이상 차이 날 수 있습니다.
                    </p>
                    <p class="text-[11px] text-gray-400 leading-relaxed mb-6">
                        * 관련근거: 국민건강보험법 제41조 제4항,<br/>
                        국민건강보험 요양급여의 기준에 관한 규칙 제9조 제1항
                    </p>
                    <div>
                        <a href="<c:url value='/hospitals'/>"
                           class="inline-flex items-center gap-2 text-sm font-semibold text-white bg-[#2563EB] rounded-full px-5 py-2.5 hover:bg-blue-700 transition-colors min-h-[44px]">
                            병원 검색 시작하기
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
                            </svg>
                        </a>
                    </div>
                </div>

                <%-- 카드 영역 --%>
                <div class="bg-white border-t lg:border-t-0 lg:border-l border-gray-100 p-8 lg:p-10 grid grid-cols-2 gap-3 content-center">

                    <div class="bg-white rounded-2xl p-4 border border-gray-100">
                        <div class="flex items-center gap-2 mb-2.5">
                            <div class="w-5 h-5 bg-gray-300 rounded-full flex items-center justify-center flex-shrink-0">
                                <svg class="w-2.5 h-2.5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7"></path>
                                </svg>
                            </div>
                            <p class="text-xs font-bold text-gray-400">급여 항목</p>
                        </div>
                        <p class="text-xs text-gray-400 leading-relaxed">보험 적용<br>일부만 본인 부담<br>전국 동일 가격</p>
                    </div>

                    <div class="bg-[#EEF4FF] rounded-2xl p-4 border border-blue-100">
                        <div class="flex items-center gap-2 mb-2.5">
                            <div class="w-5 h-5 bg-[#2563EB] rounded-full flex items-center justify-center flex-shrink-0">
                                <svg viewBox="0 0 12 12" width="10" height="10" fill="white" xmlns="http://www.w3.org/2000/svg">
                                    <rect x="5" y="1" width="2" height="6.5" rx="1"></rect>
                                    <circle cx="6" cy="10.5" r="1.2"></circle>
                                </svg>
                            </div>
                            <p class="text-xs font-bold text-[#2563EB]">비급여 항목</p>
                        </div>
                        <p class="text-xs text-gray-600 leading-relaxed">보험 미적용<br>전액 본인 부담<br>병원마다 다른 가격</p>
                    </div>

                    <div class="col-span-2 bg-white rounded-2xl p-4 border border-gray-100">
                        <p class="text-xs font-semibold text-gray-400 mb-2.5">대표적인 비급여 항목</p>
                        <div class="flex flex-wrap gap-1.5">
                            <a href="<c:url value='/hospitals?keyword=도수치료'/>" class="text-xs bg-gray-100 text-gray-500 rounded-full px-2.5 py-1 font-medium hover:bg-gray-200 transition-colors">도수치료</a>
                            <a href="<c:url value='/hospitals?keyword=MRI'/>" class="text-xs bg-gray-100 text-gray-500 rounded-full px-2.5 py-1 font-medium hover:bg-gray-200 transition-colors">MRI</a>
                            <a href="<c:url value='/hospitals?keyword=초음파'/>" class="text-xs bg-gray-100 text-gray-500 rounded-full px-2.5 py-1 font-medium hover:bg-gray-200 transition-colors">초음파</a>
                            <a href="<c:url value='/hospitals?keyword=보톡스'/>" class="text-xs bg-gray-100 text-gray-500 rounded-full px-2.5 py-1 font-medium hover:bg-gray-200 transition-colors">보톡스</a>
                            <a href="<c:url value='/hospitals?keyword=레이저'/>" class="text-xs bg-gray-100 text-gray-500 rounded-full px-2.5 py-1 font-medium hover:bg-gray-200 transition-colors">레이저</a>
                            <a href="<c:url value='/hospitals?keyword=수면내시경'/>" class="text-xs bg-gray-100 text-gray-500 rounded-full px-2.5 py-1 font-medium hover:bg-gray-200 transition-colors">수면내시경</a>
                        </div>
                    </div>

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
</script>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
