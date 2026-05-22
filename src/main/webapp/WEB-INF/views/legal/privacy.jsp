<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="개인정보처리방침" />
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
            <h1 class="text-2xl font-bold text-gray-900">개인정보처리방침</h1>
            <p class="text-sm text-gray-400 mt-1">시행일: 2026년 5월 21일 &nbsp;·&nbsp; 최종 수정일: 2026년 5월 21일</p>
        </div>

        <%-- 안내 배너 --%>
        <div class="bg-blue-50 border border-blue-200 rounded-2xl px-5 py-4 flex gap-3 mb-6">
            <svg class="w-5 h-5 text-[#2563EB] flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"/>
            </svg>
            <p class="text-sm text-blue-800 leading-relaxed">
                MediPrice는 이용자의 개인정보를 소중히 여기며, 「개인정보 보호법」 및 관련 법령을 준수합니다.<br>
                서비스는 필요한 최소한의 개인정보만 수집합니다.
            </p>
        </div>

        <div class="space-y-4">

            <%-- 제1조 --%>
            <div class="bg-white rounded-2xl p-6" style="box-shadow:0 2px 10px rgba(0,0,0,0.06);">
                <h2 class="text-base font-bold text-gray-900 mb-4">제1조 (개인정보의 처리 목적)</h2>
                <p class="text-sm text-gray-600 leading-relaxed mb-3">
                    MediPrice(이하 "서비스")는 다음의 목적을 위해 개인정보를 처리합니다.<br>
                    처리된 개인정보는 다음 목적 이외의 용도로 이용되지 않으며,<br>
                    이용 목적이 변경될 경우 별도의 동의를 받겠습니다.
                </p>
                <ul class="space-y-2 text-sm text-gray-600 leading-relaxed">
                    <li class="flex gap-2"><span class="text-[#2563EB] font-semibold flex-shrink-0">①</span> 회원 식별 및 서비스 로그인 처리</li>
                    <li class="flex gap-2"><span class="text-[#2563EB] font-semibold flex-shrink-0">②</span> 즐겨찾기 등 개인화 서비스 제공</li>
                    <li class="flex gap-2"><span class="text-[#2563EB] font-semibold flex-shrink-0">③</span> 서비스 품질 개선 및 이용 통계 분석</li>
                    <li class="flex gap-2"><span class="text-[#2563EB] font-semibold flex-shrink-0">④</span> 법령 준수 및 분쟁 해결</li>
                </ul>
            </div>

            <%-- 제2조 --%>
            <div class="bg-white rounded-2xl p-6" style="box-shadow:0 2px 10px rgba(0,0,0,0.06);">
                <h2 class="text-base font-bold text-gray-900 mb-4">제2조 (수집하는 개인정보의 항목)</h2>
                <div class="overflow-x-auto">
                    <table class="w-full text-sm">
                        <thead>
                            <tr class="border-b border-gray-100">
                                <th class="text-left text-xs text-gray-400 font-medium pb-3 pr-4 whitespace-nowrap">수집 방법</th>
                                <th class="text-left text-xs text-gray-400 font-medium pb-3 pr-4">수집 항목</th>
                                <th class="text-left text-xs text-gray-400 font-medium pb-3">목적</th>
                            </tr>
                        </thead>
                        <tbody class="divide-y divide-gray-50 text-gray-600">
                            <tr>
                                <td class="py-3 pr-4 whitespace-nowrap font-medium text-gray-700">Google 소셜 로그인</td>
                                <td class="py-3 pr-4">이메일 주소, 이름, 프로필 사진 URL</td>
                                <td class="py-3">회원 식별, 로그인 처리</td>
                            </tr>
                            <tr>
                                <td class="py-3 pr-4 whitespace-nowrap font-medium text-gray-700">서비스 이용 시 자동 수집</td>
                                <td class="py-3 pr-4">접속 IP, 쿠키(JWT 토큰), 브라우저 정보, 이용 기록</td>
                                <td class="py-3">서비스 제공, 보안, 통계</td>
                            </tr>
                            <tr>
                                <td class="py-3 pr-4 whitespace-nowrap font-medium text-gray-700">위치정보 서비스 이용 시</td>
                                <td class="py-3 pr-4">현재 위치 (위도·경도)</td>
                                <td class="py-3">주변 병원 검색</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
                <p class="text-xs text-gray-400 mt-3">
                    ※ 서비스는 별도의 회원가입 양식을 운영하지 않으며,<br>
                    Google OAuth2를 통한 소셜 로그인만 지원합니다.
                </p>
            </div>

            <%-- 제3조 --%>
            <div class="bg-white rounded-2xl p-6" style="box-shadow:0 2px 10px rgba(0,0,0,0.06);">
                <h2 class="text-base font-bold text-gray-900 mb-4">제3조 (개인정보의 처리 및 보유 기간)</h2>
                <ul class="space-y-2 text-sm text-gray-600 leading-relaxed">
                    <li>
                        ① 개인정보는 수집·이용 목적이 달성될 때까지 보유합니다.<br>
                        회원 탈퇴 또는 삭제 요청 시 지체 없이 파기합니다.
                    </li>
                    <li>② 단, 관련 법령에 따라 일정 기간 보존이 필요한 경우 해당 기간 동안 보관합니다.
                        <ul class="mt-2 ml-4 space-y-1 text-gray-500 text-xs">
                            <li>- 계약 또는 청약철회 기록: 5년 (전자상거래 등에서의 소비자보호에 관한 법률)</li>
                            <li>- 접속 로그 기록: 3개월 (통신비밀보호법)</li>
                        </ul>
                    </li>
                </ul>
            </div>

            <%-- 제4조 --%>
            <div class="bg-white rounded-2xl p-6" style="box-shadow:0 2px 10px rgba(0,0,0,0.06);">
                <h2 class="text-base font-bold text-gray-900 mb-4">제4조 (개인정보의 제3자 제공)</h2>
                <p class="text-sm text-gray-600 leading-relaxed mb-3">
                    서비스는 원칙적으로 이용자의 개인정보를 제3자에게 제공하지 않습니다.<br>
                    단, 다음의 경우에는 예외로 합니다.
                </p>
                <ul class="space-y-2 text-sm text-gray-600 leading-relaxed">
                    <li>① 이용자가 사전에 동의한 경우</li>
                    <li>
                        ② 법령의 규정에 의거하거나,<br>
                        수사 목적으로 법령에서 정한 절차와 방법에 따라 수사기관의 요구가 있는 경우
                    </li>
                </ul>
            </div>

            <%-- 제5조 — 외부 서비스 --%>
            <div class="bg-white rounded-2xl p-6" style="box-shadow:0 2px 10px rgba(0,0,0,0.06);">
                <h2 class="text-base font-bold text-gray-900 mb-4">제5조 (외부 서비스 활용)</h2>
                <p class="text-sm text-gray-600 leading-relaxed mb-3">
                    서비스는 아래 외부 서비스를 활용하며,<br>
                    해당 서비스의 개인정보처리방침이 별도로 적용됩니다.
                </p>
                <div class="space-y-3">
                    <div class="bg-gray-50 rounded-xl px-4 py-3">
                        <p class="text-sm font-semibold text-gray-700 mb-1">Google OAuth2 (로그인)</p>
                        <p class="text-xs text-gray-500 leading-relaxed">
                            Google 계정 로그인 시 Google의 OAuth2 서비스를 경유합니다.<br>
                            Google의 개인정보처리방침은
                            <a href="https://policies.google.com/privacy" target="_blank" rel="noopener" class="text-[#2563EB] hover:underline">policies.google.com/privacy</a>에서 확인하실 수 있습니다.
                        </p>
                    </div>
                    <div class="bg-gray-50 rounded-xl px-4 py-3">
                        <p class="text-sm font-semibold text-gray-700 mb-1">NAVER Maps JavaScript SDK (지도)</p>
                        <p class="text-xs text-gray-500 leading-relaxed">
                            병원 위치 표시를 위해 네이버 지도 SDK를 사용합니다.<br>
                            위치 정보는 서버에 저장되지 않으며, 지도 렌더링 목적으로만 사용됩니다.
                        </p>
                    </div>
                    <div class="bg-gray-50 rounded-xl px-4 py-3">
                        <p class="text-sm font-semibold text-gray-700 mb-1">건강보험심사평가원 공개 API</p>
                        <p class="text-xs text-gray-500 leading-relaxed">
                            비급여 진료비 데이터 조회를 위해 건강보험심사평가원의 공개 API를 사용합니다.<br>
                            이 과정에서 이용자의 개인정보는 전달되지 않습니다.
                        </p>
                    </div>
                </div>
            </div>

            <%-- 제6조 --%>
            <div class="bg-white rounded-2xl p-6" style="box-shadow:0 2px 10px rgba(0,0,0,0.06);">
                <h2 class="text-base font-bold text-gray-900 mb-4">제6조 (개인정보의 파기)</h2>
                <ul class="space-y-2 text-sm text-gray-600 leading-relaxed">
                    <li>
                        ① 서비스는 개인정보 보유 기간이 경과하거나 처리 목적이 달성된 경우<br>
                        해당 개인정보를 지체 없이 파기합니다.
                    </li>
                    <li>② 전자적 파일 형태의 개인정보는 복구 및 재생이 불가능한 방법으로 영구 삭제합니다.</li>
                    <li>
                        ③ 회원은 서비스 내 프로필 메뉴의 <strong class="text-gray-700">회원탈퇴</strong> 기능을 통해<br>
                        직접 계정 및 관련 개인정보(즐겨찾기 포함)를 즉시 삭제할 수 있습니다.
                    </li>
                    <li>④ 탈퇴가 어려운 경우 아래 연락처로 요청하시면 지체 없이 처리해 드립니다.
                        <div class="mt-2 ml-4 space-y-0.5">
                            <p class="font-medium text-[#2563EB]">이메일: kmj228@tukorea.ac.kr</p>
                            <p class="font-medium text-[#2563EB]">전화: 010-5924-8764</p>
                        </div>
                    </li>
                </ul>
            </div>

            <%-- 제7조 --%>
            <div class="bg-white rounded-2xl p-6" style="box-shadow:0 2px 10px rgba(0,0,0,0.06);">
                <h2 class="text-base font-bold text-gray-900 mb-4">제7조 (이용자의 권리)</h2>
                <ul class="space-y-2 text-sm text-gray-600 leading-relaxed">
                    <li>① 이용자는 언제든지 자신의 개인정보에 대한 열람, 정정, 삭제를 요청할 수 있습니다.</li>
                    <li>② 개인정보 관련 요청은 아래 연락처를 통해 접수하며, 서비스는 지체 없이 조치합니다.
                        <div class="mt-2 ml-4 space-y-0.5">
                            <p class="font-medium text-[#2563EB]">이메일: kmj228@tukorea.ac.kr</p>
                            <p class="font-medium text-[#2563EB]">전화: 010-5924-8764</p>
                        </div>
                    </li>
                </ul>
            </div>

            <%-- 제8조 --%>
            <div class="bg-white rounded-2xl p-6" style="box-shadow:0 2px 10px rgba(0,0,0,0.06);">
                <h2 class="text-base font-bold text-gray-900 mb-4">제8조 (개인정보 보호책임자)</h2>
                <div class="bg-gray-50 rounded-xl px-4 py-4 text-sm text-gray-600 space-y-1">
                    <p><span class="text-gray-400 w-24 inline-block">서비스명</span> MediPrice</p>
                    <p><span class="text-gray-400 w-24 inline-block">이메일</span>
                        <a href="mailto:kmj228@tukorea.ac.kr" class="text-[#2563EB] hover:underline">kmj228@tukorea.ac.kr</a>
                    </p>
                    <p><span class="text-gray-400 w-24 inline-block">전화</span>
                        <a href="tel:01059248764" class="text-[#2563EB] hover:underline">010-5924-8764</a>
                    </p>
                </div>
                <p class="text-xs text-gray-400 mt-3 leading-relaxed">
                    개인정보 처리와 관련한 불만처리 및 피해구제를 위하여 아래 기관에도 도움을 요청하실 수 있습니다.<br>
                    <a href="https://www.privacy.go.kr" target="_blank" rel="noopener" class="text-[#2563EB] hover:underline">개인정보보호위원회 (privacy.go.kr)</a>&nbsp;·&nbsp;
                    <a href="https://www.kopico.go.kr" target="_blank" rel="noopener" class="text-[#2563EB] hover:underline">개인정보 분쟁조정위원회 (kopico.go.kr)</a>
                </p>
            </div>

        </div>

    </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
