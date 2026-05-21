<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="위치기반서비스 이용약관" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="bg-[#F2F4F6] min-h-full pb-12">
    <div class="max-w-3xl mx-auto px-4 py-10">

        <%-- 헤더 --%>
        <div class="mb-8">
            <a href="<c:url value="/"/>"
               class="inline-flex items-center gap-1.5 text-xs text-gray-400 hover:text-[#2563EB] transition-colors mb-4">
                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
                </svg>
                홈으로
            </a>
            <h1 class="text-2xl font-bold text-gray-900">위치기반서비스 이용약관</h1>
            <p class="text-sm text-gray-400 mt-1">시행일: 2026년 5월 21일 &nbsp;·&nbsp; 최종 수정일: 2026년 5월 21일</p>
        </div>

        <%-- 안내 배너 --%>
        <div class="bg-green-50 border border-green-200 rounded-2xl px-5 py-4 flex gap-3 mb-6">
            <svg class="w-5 h-5 text-green-600 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"/>
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"/>
            </svg>
            <p class="text-sm text-green-800 leading-relaxed">
                본 약관은 「위치정보의 보호 및 이용 등에 관한 법률」에 따라 작성되었습니다.<br>
                위치 정보는 주변 병원 검색 목적으로만 사용되며, 서버에 저장되지 않습니다.
            </p>
        </div>

        <div class="space-y-4">

            <%-- 제1조 --%>
            <div class="bg-white rounded-2xl p-6" style="box-shadow:0 2px 10px rgba(0,0,0,0.06);">
                <h2 class="text-base font-bold text-gray-900 mb-4">제1조 (목적)</h2>
                <p class="text-sm text-gray-600 leading-relaxed">
                    본 약관은 MediPrice(이하 "서비스")가 제공하는 위치기반 서비스의 이용과 관련하여<br>
                    서비스와 이용자 간의 권리·의무 및 책임 사항을 규정함을 목적으로 합니다.<br>
                    본 약관은 「위치정보의 보호 및 이용 등에 관한 법률」을 근거로 합니다.
                </p>
            </div>

            <%-- 제2조 --%>
            <div class="bg-white rounded-2xl p-6" style="box-shadow:0 2px 10px rgba(0,0,0,0.06);">
                <h2 class="text-base font-bold text-gray-900 mb-4">제2조 (위치기반서비스의 내용)</h2>
                <p class="text-sm text-gray-600 leading-relaxed mb-3">
                    서비스는 이용자의 현재 위치 정보를 활용하여 다음 서비스를 제공합니다.
                </p>
                <ul class="space-y-2 text-sm text-gray-600 leading-relaxed">
                    <li class="flex gap-2">
                        <span class="text-green-600 font-semibold flex-shrink-0">①</span>
                        <span>이용자 현재 위치 기반 반경 내 의료기관 검색 및 지도 표시</span>
                    </li>
                    <li class="flex gap-2">
                        <span class="text-green-600 font-semibold flex-shrink-0">②</span>
                        <span>검색 결과 의료기관과의 거리 계산 및 정렬</span>
                    </li>
                    <li class="flex gap-2">
                        <span class="text-green-600 font-semibold flex-shrink-0">③</span>
                        <span>지도에 현재 위치 및 주변 의료기관 마커 표시</span>
                    </li>
                </ul>
            </div>

            <%-- 제3조 --%>
            <div class="bg-white rounded-2xl p-6" style="box-shadow:0 2px 10px rgba(0,0,0,0.06);">
                <h2 class="text-base font-bold text-gray-900 mb-4">제3조 (위치정보의 수집·이용)</h2>
                <ul class="space-y-3 text-sm text-gray-600 leading-relaxed">
                    <li>
                        <span class="font-semibold text-gray-800">① 수집 방법</span>
                        <p class="mt-1 ml-4">
                            브라우저의 Geolocation API를 통해 이용자가 명시적으로 위치 접근을 허용한 경우에 한해 수집합니다.<br>
                            위치 접근을 거부하더라도 서비스의 기본 기능은 이용할 수 있습니다.
                        </p>
                    </li>
                    <li>
                        <span class="font-semibold text-gray-800">② 수집 항목</span>
                        <p class="mt-1 ml-4">위도(latitude), 경도(longitude)</p>
                    </li>
                    <li>
                        <span class="font-semibold text-gray-800">③ 이용 목적</span>
                        <p class="mt-1 ml-4">주변 의료기관 검색 및 거리 계산 (주변 병원 검색 시 1회성 이용)</p>
                    </li>
                    <li>
                        <span class="font-semibold text-gray-800">④ 보유 및 파기</span>
                        <p class="mt-1 ml-4">
                            수집된 위치 정보는 서버에 저장되지 않으며, 검색 요청 처리 완료 후 즉시 파기됩니다.<br>
                            단, 지도 중심 좌표는 브라우저 세션 내 메모리에서만 유지됩니다.
                        </p>
                    </li>
                </ul>
            </div>

            <%-- 제4조 --%>
            <div class="bg-white rounded-2xl p-6" style="box-shadow:0 2px 10px rgba(0,0,0,0.06);">
                <h2 class="text-base font-bold text-gray-900 mb-4">제4조 (위치정보의 제3자 제공)</h2>
                <ul class="space-y-2 text-sm text-gray-600 leading-relaxed">
                    <li>① 서비스는 이용자의 위치 정보를 원칙적으로 제3자에게 제공하지 않습니다.</li>
                    <li>
                        ② 단, 서비스는 지도 표시를 위해 NAVER Maps JavaScript SDK를 활용하며,<br>
                        지도 SDK에 위도·경도 좌표가 전달될 수 있습니다.<br>
                        이는 지도 렌더링 목적으로만 사용되며, 네이버의 개인정보처리방침이 적용됩니다.
                        <p class="mt-1 ml-4 text-xs text-gray-400">
                            네이버 개인정보처리방침:
                            <a href="https://privacy.naver.com" target="_blank" rel="noopener" class="text-[#2563EB] hover:underline">privacy.naver.com</a>
                        </p>
                    </li>
                </ul>
            </div>

            <%-- 제5조 --%>
            <div class="bg-white rounded-2xl p-6" style="box-shadow:0 2px 10px rgba(0,0,0,0.06);">
                <h2 class="text-base font-bold text-gray-900 mb-4">제5조 (이용자의 권리)</h2>
                <ul class="space-y-2 text-sm text-gray-600 leading-relaxed">
                    <li>
                        ① 이용자는 언제든지 브라우저 설정을 통해 위치 정보 제공을 거부할 수 있습니다.<br>
                        이 경우 위치 기반 검색 기능은 이용이 제한될 수 있으나,<br>
                        키워드 검색 등 다른 기능은 계속 이용 가능합니다.
                    </li>
                    <li>② 이용자는 위치정보 이용·제공에 대한 동의를 사후에도 언제든지 철회할 수 있습니다.</li>
                    <li>③ 위치정보 관련 문의 및 이의 제기는 아래 연락처로 접수하시기 바랍니다.
                        <p class="mt-2 ml-4 font-medium text-[#2563EB]">이메일: khaung228@gmail.com</p>
                    </li>
                </ul>
            </div>

            <%-- 제6조 --%>
            <div class="bg-white rounded-2xl p-6" style="box-shadow:0 2px 10px rgba(0,0,0,0.06);">
                <h2 class="text-base font-bold text-gray-900 mb-4">제6조 (위치정보 관련 면책)</h2>
                <ul class="space-y-2 text-sm text-gray-600 leading-relaxed">
                    <li>
                        ① 이용자가 위치 정보 제공을 거부한 경우,<br>
                        위치 기반 서비스 이용 불가로 인한 불편에 대해 서비스는 책임을 지지 않습니다.
                    </li>
                    <li>
                        ② 브라우저 또는 단말기의 GPS 오류로 인해 부정확한 위치가 제공된 경우<br>
                        이로 인한 검색 결과의 부정확성에 대해 서비스는 책임을 지지 않습니다.
                    </li>
                </ul>
            </div>

            <%-- 제7조 --%>
            <div class="bg-white rounded-2xl p-6" style="box-shadow:0 2px 10px rgba(0,0,0,0.06);">
                <h2 class="text-base font-bold text-gray-900 mb-4">제7조 (위치정보 관리책임자)</h2>
                <div class="bg-gray-50 rounded-xl px-4 py-4 text-sm text-gray-600 space-y-1">
                    <p><span class="text-gray-400 w-24 inline-block">서비스명</span> MediPrice</p>
                    <p><span class="text-gray-400 w-24 inline-block">이메일</span>
                        <a href="mailto:khaung228@gmail.com" class="text-[#2563EB] hover:underline">khaung228@gmail.com</a>
                    </p>
                </div>
            </div>

        </div>

    </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
