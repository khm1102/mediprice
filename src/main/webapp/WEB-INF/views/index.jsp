<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="비급여 진료비 비교" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<style>
    /* ── 히어로 진입 ── */
    @keyframes mp-fade-up {
        from { opacity: 0; transform: translateY(32px); }
        to   { opacity: 1; transform: translateY(0); }
    }
    .hero-anim { animation: mp-fade-up 1.1s cubic-bezier(0.16, 1, 0.3, 1) both; }
    .hero-anim-1 { animation-delay: 0.10s; }
    .hero-anim-2 { animation-delay: 0.26s; }
    .hero-anim-3 { animation-delay: 0.42s; }
    .hero-anim-4 { animation-delay: 0.58s; }
    .hero-anim-5 { animation-delay: 0.74s; }

    /* ── 스크롤 연동 애니메이션 ── */
    [data-anim] {
        transition:
            opacity  1.0s cubic-bezier(0.16, 1, 0.3, 1) var(--ad, 0ms),
            transform 1.0s cubic-bezier(0.16, 1, 0.3, 1) var(--ad, 0ms);
    }
    /* 세로 (기본) */
    [data-anim]:not([data-anim="left"]):not([data-anim="right"]).mp-below { opacity: 0; transform: translateY(44px); }
    [data-anim]:not([data-anim="left"]):not([data-anim="right"]).mp-above { opacity: 0; transform: translateY(-44px); }
    /* 가로 (방향 고정) */
    [data-anim="left"].mp-below,  [data-anim="left"].mp-above  { opacity: 0; transform: translateX(-44px); }
    [data-anim="right"].mp-below, [data-anim="right"].mp-above { opacity: 0; transform: translateX(44px); }
    /* 진입 완료 */
    [data-anim].mp-visible { opacity: 1; transform: none; }

    /* ── 검색창 호버 ── */
    .mp-search {
        transition: transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1),
                    box-shadow 0.35s ease !important;
    }
    .mp-search:hover {
        transform: scale(1.028);
        box-shadow: 0 16px 56px rgba(0,0,0,0.20), 0 4px 16px rgba(0,0,0,0.10) !important;
    }

    /* ── 자주 찾는 항목 ── */
    .mp-quick-grid a {
        transition: transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1),
                    box-shadow 0.4s ease !important;
    }
    .mp-quick-grid a:hover {
        transform: translateY(-4px) scale(1.04);
    }
    .mp-quick-grid a .w-10 {
        transition: transform 0.45s cubic-bezier(0.34, 1.56, 0.64, 1);
    }
    .mp-quick-grid a:hover .w-10 {
        transform: rotate(-15deg);
    }

    /* ── 이용 방법 카드 ── */
    .mp-steps-grid > div[data-anim] {
        transition:
            opacity  1.0s cubic-bezier(0.16, 1, 0.3, 1) var(--ad, 0ms),
            transform 0.5s cubic-bezier(0.34, 1.56, 0.64, 1),
            box-shadow 0.5s ease;
    }
    .mp-steps-grid > div:hover {
        transform: translateY(-10px) scale(1.02) !important;
        box-shadow: 0 24px 56px rgba(37, 99, 235, 0.15),
                    0 6px 16px rgba(37, 99, 235, 0.08);
    }

    /* ── 비급여 항목 강조 ── */
    .mp-covered-card,
    .mp-noncovered-card {
        transition: transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1),
                    box-shadow 0.4s ease,
                    opacity 0.35s ease,
                    border-color 0.35s ease;
    }
    .mp-compare-grid:hover .mp-covered-card {
        opacity: 0.5;
        transform: scale(0.95);
    }
    .mp-compare-grid:hover .mp-noncovered-card {
        transform: scale(1.06) translateY(-5px);
        box-shadow: 0 12px 36px rgba(37, 99, 235, 0.28);
        border-color: rgba(37, 99, 235, 0.5);
    }
</style>

<%-- ════════════════════════════════════════════
     히어로
════════════════════════════════════════════ --%>
<div class="relative flex items-center px-4 bg-[#EEF4FF] overflow-hidden"
     style="min-height: calc(100svh - 3.5rem);">

    <%-- MRI 배경 장식 --%>
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

    <%-- 주사치료 배경 장식 --%>
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

        <span class="inline-flex items-center gap-1.5 text-xs text-blue-600 mb-6 bg-white rounded-full px-3.5 py-1.5 border border-blue-100 hero-anim hero-anim-1" style="box-shadow: 0 1px 6px rgba(37,99,235,0.08);">
            <svg class="w-3.5 h-3.5 flex-shrink-0 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
            </svg>
            건강보험심사평가원 공식 데이터 기반
        </span>

        <h1 class="text-3xl lg:text-5xl font-bold text-gray-900 mb-4 leading-tight tracking-tight hero-anim hero-anim-2">
            비급여 진료비,<br>지금 바로 비교하세요
        </h1>
        <p class="text-gray-500 text-sm lg:text-base mb-10 leading-relaxed hero-anim hero-anim-3">
            병원마다 다른 가격,<br class="sm:hidden"/>검색 한 번으로 확인하세요
        </p>

        <div class="flex gap-2 bg-white rounded-2xl p-1.5 max-w-lg mx-auto hero-anim hero-anim-4 mp-search" style="box-shadow: 0 8px 40px rgba(0,0,0,0.18), 0 2px 8px rgba(0,0,0,0.08);">
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

        <p class="mt-6 text-[11px] text-gray-400 leading-relaxed hero-anim hero-anim-5">
            ※ 2026년 비급여 진료비용 공개를 위한 자료수집 및 검증기간(4월~8월)으로,<br/>
            해당 기간에는 변경사항이 반영되지 않아 조회 금액과 실제 금액 간 차이가 있을 수 있습니다.

    </div>
</div>

<%-- ════════════════════════════════════════════
     통계
════════════════════════════════════════════ --%>
<div class="bg-white">
    <div class="max-w-5xl mx-auto px-4 pt-10 pb-10 lg:pt-12 lg:pb-12">
        <div class="grid grid-cols-3 divide-x divide-gray-200">
            <div class="text-center px-2 lg:px-4" data-anim>
                <p class="text-xl lg:text-4xl font-bold text-[#2563EB] tracking-tight whitespace-nowrap"
                   data-counter-target="6" data-counter-prefix="최대 " data-counter-suffix="배">최대 6배</p>
                <p class="text-[10px] lg:text-sm text-gray-500 mt-2 leading-snug">MRI 병원 간<br>최저·최고 가격 차이</p>
            </div>
            <div class="text-center px-2 lg:px-4" data-anim style="--ad: 80ms">
                <p class="text-xl lg:text-4xl font-bold text-[#2563EB] tracking-tight whitespace-nowrap"
                   data-counter-target="75065" data-counter-start="70000" data-counter-suffix="">75,065</p>
                <p class="text-[10px] lg:text-sm text-gray-500 mt-2 leading-snug">전국 병·의원급<br>의료기관 수</p>
            </div>
            <div class="text-center px-2 lg:px-4" data-anim style="--ad: 160ms">
                <p class="text-xl lg:text-4xl font-bold text-[#2563EB] tracking-tight whitespace-nowrap"
                   data-counter-target="693" data-counter-start="400" data-counter-suffix="개">693개</p>
                <p class="text-[10px] lg:text-sm text-gray-500 mt-2 leading-snug">비급여<br>공개 항목 수</p>
            </div>
        </div>
    </div>
</div>

<%-- ════════════════════════════════════════════
     자주 찾는 항목
════════════════════════════════════════════ --%>
<div class="bg-[#F2F4F6]">
    <div class="max-w-5xl mx-auto px-4 py-10">

        <p class="text-[11px] font-bold text-gray-400 uppercase tracking-widest mb-5" data-anim>자주 찾는 항목</p>

        <div class="grid grid-cols-2 lg:grid-cols-4 gap-3 mp-quick-grid">

            <%-- 도수치료 --%>
            <a href="<c:url value='/hospitals?keyword=도수치료'/>"
               class="flex items-center gap-3 bg-white rounded-2xl p-4 hover:shadow-md transition-all group min-h-[68px]"
               style="box-shadow: 0 2px 10px rgba(0,0,0,0.09);" data-anim>
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
               style="box-shadow: 0 2px 10px rgba(0,0,0,0.09); --ad: 50ms" data-anim>
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
               style="box-shadow: 0 2px 10px rgba(0,0,0,0.09); --ad: 100ms" data-anim>
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
               style="box-shadow: 0 2px 10px rgba(0,0,0,0.09); --ad: 150ms" data-anim>
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

            <%-- 대상포진 --%>
            <a href="<c:url value='/hospitals?keyword=대상포진'/>"
               class="flex items-center gap-3 bg-white rounded-2xl p-4 hover:shadow-md transition-all group min-h-[68px]"
               style="box-shadow: 0 2px 10px rgba(0,0,0,0.09); --ad: 200ms" data-anim>
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
                    <p class="text-sm font-semibold text-gray-800 group-hover:text-[#2563EB] transition-colors truncate">대상포진</p>
                    <p class="text-xs text-gray-400 mt-0.5 truncate">예방접종</p>
                </div>
            </a>

            <%-- 진정내시경 --%>
            <a href="<c:url value='/hospitals?keyword=진정내시경'/>"
               class="flex items-center gap-3 bg-white rounded-2xl p-4 hover:shadow-md transition-all group min-h-[68px]"
               style="box-shadow: 0 2px 10px rgba(0,0,0,0.09); --ad: 250ms" data-anim>
                <div class="w-10 h-10 bg-blue-50 rounded-xl flex items-center justify-center flex-shrink-0 group-hover:bg-blue-100 transition-colors">
                    <svg class="w-5 h-5 text-[#2563EB]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                            d="M21.752 15.002A9.718 9.718 0 0118 15.75c-5.385 0-9.75-4.365-9.75-9.75 0-1.33.266-2.597.748-3.752A9.753 9.753 0 003 11.25C3 16.635 7.365 21 12.75 21a9.753 9.753 0 009.002-5.998z"></path>
                    </svg>
                </div>
                <div class="min-w-0">
                    <p class="text-sm font-semibold text-gray-800 group-hover:text-[#2563EB] transition-colors truncate">진정내시경</p>
                    <p class="text-xs text-gray-400 mt-0.5 truncate">소화기내과</p>
                </div>
            </a>

            <%-- 임플란트 --%>
            <a href="<c:url value='/hospitals?keyword=임플란트'/>"
               class="flex items-center gap-3 bg-white rounded-2xl p-4 hover:shadow-md transition-all group min-h-[68px]"
               style="box-shadow: 0 2px 10px rgba(0,0,0,0.09); --ad: 300ms" data-anim>
                <div class="w-10 h-10 bg-blue-50 rounded-xl flex items-center justify-center flex-shrink-0 group-hover:bg-blue-100 transition-colors">
                    <svg class="w-5 h-5 text-[#2563EB]" fill="currentColor" stroke="currentColor" stroke-width="6" stroke-linejoin="round" viewBox="0 0 90.81 122.88">
                        <path d="M6.34,5.95c3.14-3.02,7.01-4.76,11.33-5.37c4.41-0.62,9.29-0.05,14.32,1.55c2.76,0.88,4.91,1.62,6.67,2.23 c3.33,1.15,5.12,1.78,6.74,1.78c1.95,0.01,4.3-0.84,9.39-2.66c1.58-0.57,3.31-1.19,3.48-1.25c4.4-1.55,8.68-2.32,12.62-2.23 c3.98,0.09,7.62,1.03,10.7,2.89c4.38,2.65,6.8,6.2,8.05,10.23c1.2,3.84,1.3,8.04,1.07,12.29c-0.33,6.13-1.39,12.13-3.19,17.99 c-1.72,5.61-4.11,11.09-7.16,16.45c1.55,9.74,1.98,18.5,1.71,26.24c-0.27,7.99-1.26,14.88-2.51,20.63 c-1.36,6.3-3.32,11.17-5.49,13.69c-1.24,1.44-2.63,2.26-4.13,2.36c-1.59,0.1-3.09-0.59-4.42-2.19c-1.86-2.24-3.55-6.67-4.81-13.78 c-0.35-1.97-0.55-4.14-0.64-5.08l-0.03-0.27l0,0l-0.06-0.56c-1.38-14-2.74-27.78-15.03-27.92c-5.86,1.34-8.95,4.1-10.67,8.18 c-1.88,4.47-2.33,10.72-2.85,18.36l0,0.03c-0.07,1.06-0.31,4.63-0.74,7.23c-1.19,7.21-2.9,11.7-4.8,13.95 c-1.33,1.56-2.82,2.25-4.41,2.16c-1.5-0.08-2.9-0.88-4.14-2.3c-2.19-2.5-4.14-7.35-5.39-13.73c-1.03-5.27-1.82-12.26-2.22-20.83 c-0.35-7.56-0.39-16.36-0.02-26.3c-2.11-4.83-3.99-9.69-5.52-14.6c-1.58-5.08-2.79-10.23-3.49-15.48 c-0.58-4.39-1.02-8.41-0.43-12.26C0.9,13.31,2.61,9.55,6.34,5.95L6.34,5.95z M18.29,5.06c-3.39,0.48-6.4,1.82-8.81,4.15 c-2.9,2.8-4.23,5.71-4.71,8.82c-0.51,3.32-0.11,6.98,0.43,11c0.66,4.94,1.81,9.84,3.33,14.72c1.52,4.89,3.42,9.74,5.55,14.6l0,0 c0.13,0.3,0.2,0.64,0.19,0.99c-0.39,10.02-0.35,18.88,0.01,26.47c0.38,8.29,1.15,15.05,2.15,20.16c1.1,5.61,2.67,9.71,4.34,11.62 c0.42,0.48,0.76,0.75,0.99,0.76c0.14,0.01,0.37-0.17,0.71-0.56c1.41-1.66,2.75-5.42,3.79-11.75c0.39-2.35,0.62-5.79,0.69-6.81 l0-0.03c0.54-8.05,1.02-14.63,3.2-19.82c2.33-5.52,6.41-9.22,14.09-10.91l0,0.01c0.15-0.03,0.31-0.05,0.47-0.05 c16.65-0.07,18.22,15.84,19.81,32l0.05,0.56l0,0l0,0.01l0.03,0.28c0.08,0.84,0.26,2.78,0.61,4.73c1.11,6.3,2.44,10.02,3.82,11.68 c0.32,0.39,0.54,0.57,0.66,0.56c0.22-0.01,0.55-0.29,0.97-0.78c1.68-1.96,3.29-6.12,4.49-11.71c1.2-5.56,2.16-12.18,2.42-19.82 c0.26-7.67-0.18-16.38-1.78-26.12c-0.1-0.51-0.02-1.06,0.27-1.55c3.08-5.29,5.46-10.69,7.16-16.21c1.69-5.51,2.69-11.14,2.99-16.89 c0.2-3.79,0.13-7.49-0.88-10.71c-0.94-3.03-2.76-5.7-6.07-7.69c-2.39-1.45-5.27-2.18-8.45-2.25c-3.39-0.07-7.13,0.61-11.03,1.98 c-2.39,0.85-2.96,1.05-3.46,1.23c-5.6,2.01-8.19,2.93-10.91,2.93c-2.39-0.01-4.43-0.72-8.22-2.03c-1.73-0.6-3.85-1.34-6.57-2.2 C26.23,5.04,22.03,4.54,18.29,5.06L18.29,5.06z"/>
                    </svg>
                </div>
                <div class="min-w-0">
                    <p class="text-sm font-semibold text-gray-800 group-hover:text-[#2563EB] transition-colors truncate">임플란트</p>
                    <p class="text-xs text-gray-400 mt-0.5 truncate">치과</p>
                </div>
            </a>

            <%-- 추나요법 --%>
            <a href="<c:url value='/hospitals?keyword=추나요법'/>"
               class="flex items-center gap-3 bg-white rounded-2xl p-4 hover:shadow-md transition-all group min-h-[68px]"
               style="box-shadow: 0 2px 10px rgba(0,0,0,0.09); --ad: 450ms" data-anim>
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

        <p class="text-[11px] font-bold text-gray-400 uppercase tracking-widest mb-8" data-anim>이용 방법</p>

        <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mp-steps-grid">

            <div class="rounded-2xl p-6" style="background: #F0F6FF;" data-anim>
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

            <div class="rounded-2xl p-6" style="background: #F0F6FF; --ad: 120ms" data-anim>
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

            <div class="rounded-2xl p-6" style="background: #F0F6FF; --ad: 240ms" data-anim>
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
                <div class="p-8 lg:p-10 flex flex-col justify-center" data-anim="left">
                    <p class="text-[11px] font-bold text-gray-400 uppercase tracking-widest mb-4">비급여 진료비란?</p>
                    <h2 class="text-xl lg:text-2xl font-bold text-gray-900 mb-4 leading-snug">
                        건강보험이 적용되지 않아<br>환자가 전액 부담하는 진료비
                    </h2>
                    <p class="text-sm text-gray-500 leading-relaxed mb-4">
                        급여 항목과 달리 비급여 항목은<br class="block sm:hidden lg:block"/>
                        건강보험 혜택 없이 전액 본인 부담입니다.<br>
                        병원이 자체적으로 금액을 정하기 때문에<br class="block sm:hidden lg:block"/>
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
                <div class="bg-white border-t lg:border-t-0 lg:border-l border-gray-100 p-8 lg:p-10 grid grid-cols-2 gap-3 content-center mp-compare-grid" data-anim="right">

                    <div class="bg-white rounded-2xl p-4 border border-gray-100 mp-covered-card">
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

                    <div class="bg-[#EEF4FF] rounded-2xl p-4 border border-blue-100 mp-noncovered-card">
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
                            <a href="<c:url value='/hospitals?keyword=체외충격파'/>" class="text-xs bg-gray-100 text-gray-500 rounded-full px-2.5 py-1 font-medium hover:bg-gray-200 transition-colors">체외충격파</a>
                            <a href="<c:url value='/hospitals?keyword=진정내시경'/>" class="text-xs bg-gray-100 text-gray-500 rounded-full px-2.5 py-1 font-medium hover:bg-gray-200 transition-colors">진정내시경</a>
                        </div>
                    </div>

                </div>

            </div>
        </div>
    </div>
</div>

<script>
    /* ════════════════════════════════════════════
       양방향 스크롤 애니메이션
    ════════════════════════════════════════════ */
    const mpObserver = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            const el = entry.target;

            if (entry.isIntersecting) {
                /* 진입 */
                el.style.transition = '';
                el.classList.remove('mp-below', 'mp-above');
                el.classList.add('mp-visible');
            } else {
                /* 이탈 */
                el.style.transition = 'none';
                el.classList.remove('mp-visible');

                if (entry.boundingClientRect.top < 0) {
                    /* 위쪽으로 이탈 */
                    el.classList.add('mp-above');
                    el.classList.remove('mp-below');
                } else {
                    /* 아래쪽으로 이탈  */
                    el.classList.add('mp-below');
                    el.classList.remove('mp-above');
                }

                /* 두 프레임 후 transition 복원 */
                requestAnimationFrame(() => requestAnimationFrame(() => {
                    el.style.transition = '';
                }));
            }
        });
    }, { threshold: 0.12 });

    document.querySelectorAll('[data-anim]').forEach(el => {
        el.classList.add('mp-below');
        mpObserver.observe(el);
    });

    /* ════════════════════════════════════════════
       통계 숫자 카운터 애니메이션
    ════════════════════════════════════════════ */
    const animateCounter = (el) => {
        const target   = parseInt(el.dataset.counterTarget, 10);
        const from     = parseInt(el.dataset.counterStart  ?? '0', 10);
        const prefix   = el.dataset.counterPrefix ?? '';
        const suffix   = el.dataset.counterSuffix ?? '';
        const duration = 2200;
        const startTime = performance.now();

        const tick = (now) => {
            const progress = Math.min((now - startTime) / duration, 1);
            /* ease-out cubic */
            const eased = 1 - Math.pow(1 - progress, 3);
            const value = Math.round(from + eased * (target - from));
            el.textContent = prefix + value.toLocaleString('ko-KR') + suffix;
            if (progress < 1) requestAnimationFrame(tick);
        };
        requestAnimationFrame(tick);
    };

    const counterObserver = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (!entry.isIntersecting) return;
            const el = entry.target;
            if (el.dataset.counted) return;
            el.dataset.counted = 'true';
            counterObserver.unobserve(el);
            animateCounter(el);
        });
    }, { threshold: 0.5 });

    document.querySelectorAll('[data-counter-target]').forEach(el => {
        counterObserver.observe(el);
    });
</script>

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
