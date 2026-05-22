<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="이용약관" />
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
            <h1 class="text-2xl font-bold text-gray-900">이용약관</h1>
            <p class="text-sm text-gray-400 mt-1">시행일: 2026년 5월 21일 &nbsp;·&nbsp; 최종 수정일: 2026년 5월 21일</p>
        </div>

        <%-- 안내 배너 --%>
        <div class="bg-amber-50 border border-amber-200 rounded-2xl px-5 py-4 flex gap-3 mb-6">
            <svg class="w-5 h-5 text-amber-500 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
            <p class="text-sm text-amber-800 leading-relaxed">
                본 서비스를 이용하시면 아래 약관에 동의하신 것으로 간주합니다.<br>
                특히 <strong>제6조(책임 한계 및 면책)</strong>를 반드시 확인하시기 바랍니다.
            </p>
        </div>

        <div class="space-y-4">

            <%-- 제1조 --%>
            <div class="bg-white rounded-2xl p-6" style="box-shadow:0 2px 10px rgba(0,0,0,0.06);">
                <h2 class="text-base font-bold text-gray-900 mb-4">제1조 (목적)</h2>
                <p class="text-sm text-gray-600 leading-relaxed">
                    본 약관은 MediPrice(이하 "서비스")가 제공하는 비급여 진료비 비교 서비스의 이용 조건 및 절차,<br>
                    이용자와 서비스 간의 권리·의무 및 책임 사항을 규정함을 목적으로 합니다.
                </p>
            </div>

            <%-- 제2조 --%>
            <div class="bg-white rounded-2xl p-6" style="box-shadow:0 2px 10px rgba(0,0,0,0.06);">
                <h2 class="text-base font-bold text-gray-900 mb-4">제2조 (정의)</h2>
                <ul class="space-y-2 text-sm text-gray-600 leading-relaxed">
                    <li><span class="font-semibold text-gray-800">① "서비스"</span>란 MediPrice가 운영하는 비급여 진료비 비교 플랫폼을 말합니다.</li>
                    <li><span class="font-semibold text-gray-800">② "이용자"</span>란 본 약관에 동의하고 서비스를 이용하는 모든 자를 말합니다.</li>
                    <li><span class="font-semibold text-gray-800">③ "회원"</span>이란 소셜 로그인을 통해 계정을 등록한 이용자를 말합니다.</li>
                    <li>
                        <span class="font-semibold text-gray-800">④ "비급여 진료비 정보"</span>란 건강보험심사평가원이 공개한 비급여 진료비 데이터를 기반으로<br>
                        서비스가 제공하는 가격 정보를 말합니다.
                    </li>
                </ul>
            </div>

            <%-- 제3조 --%>
            <div class="bg-white rounded-2xl p-6" style="box-shadow:0 2px 10px rgba(0,0,0,0.06);">
                <h2 class="text-base font-bold text-gray-900 mb-4">제3조 (약관의 효력 및 변경)</h2>
                <ul class="space-y-2 text-sm text-gray-600 leading-relaxed">
                    <li>① 본 약관은 서비스 화면에 게시하거나 기타 방법으로 이용자에게 공지함으로써 효력이 발생합니다.</li>
                    <li>② 서비스는 「약관의 규제에 관한 법률」 등 관련 법령을 위반하지 않는 범위에서 약관을 변경할 수 있습니다.</li>
                    <li>
                        ③ 약관이 변경되는 경우, 서비스는 변경 사항을 시행일로부터 최소 7일 전에 공지합니다.<br>
                        이용자가 변경된 약관에 동의하지 않는 경우, 서비스 이용을 중단하고 회원 탈퇴를 요청할 수 있습니다.
                    </li>
                </ul>
            </div>

            <%-- 제4조 --%>
            <div class="bg-white rounded-2xl p-6" style="box-shadow:0 2px 10px rgba(0,0,0,0.06);">
                <h2 class="text-base font-bold text-gray-900 mb-4">제4조 (서비스 이용)</h2>
                <ul class="space-y-2 text-sm text-gray-600 leading-relaxed">
                    <li>① 서비스는 건강보험심사평가원의 공개 데이터를 기반으로 비급여 진료비 정보를 제공합니다.</li>
                    <li>
                        ② 본 서비스는 비영리·비상업적 목적으로 운영되며,<br>
                        이용자로부터 어떠한 금전적 대가도 수취하지 않습니다.
                    </li>
                    <li>
                        ③ 비회원은 제한된 검색 횟수 내에서 서비스를 이용할 수 있으며,<br>
                        회원으로 가입하면 횟수 제한 없이 이용할 수 있습니다.
                    </li>
                    <li>④ 서비스는 운영상 또는 기술상 이유로 사전 고지 없이 일시 중단될 수 있습니다.</li>
                    <li>
                        ⑤ 이용자는 타인의 개인정보를 도용하거나,<br>
                        서비스를 상업적 목적으로 무단 이용하는 행위를 하여서는 안 됩니다.
                    </li>
                </ul>
            </div>

            <%-- 제5조 --%>
            <div class="bg-white rounded-2xl p-6" style="box-shadow:0 2px 10px rgba(0,0,0,0.06);">
                <h2 class="text-base font-bold text-gray-900 mb-4">제5조 (정보의 정확성)</h2>
                <ul class="space-y-2 text-sm text-gray-600 leading-relaxed">
                    <li>
                        ① 서비스가 제공하는 비급여 진료비 정보는 건강보험심사평가원이 공개한 데이터를 기반으로 하며,<br>
                        해당 기관의 데이터 갱신 주기에 따라 현재 상황과 차이가 있을 수 있습니다.
                    </li>
                    <li>
                        ② 각 의료기관은 진료 방법, 재료, 소요 시간 및 환자 상태에 따라<br>
                        실제 청구 금액이 서비스에 표시된 금액과 다를 수 있습니다.
                    </li>
                    <li>
                        ③ 서비스에 표시된 가격 정보는 참고 목적으로만 제공되며,<br>
                        정확한 비용은 해당 의료기관에 직접 문의하시기 바랍니다.
                    </li>
                </ul>
            </div>

            <%-- 제6조 — 핵심 면책 조항 --%>
            <div class="bg-white rounded-2xl overflow-hidden" style="box-shadow:0 2px 10px rgba(0,0,0,0.06); border: 2px solid #FCA5A5;">
                <div class="bg-red-50 px-6 py-3 border-b border-red-100">
                    <h2 class="text-base font-bold text-red-700">제6조 (책임 한계 및 면책)</h2>
                </div>
                <div class="p-6 space-y-3 text-sm text-gray-600 leading-relaxed">
                    <p class="font-semibold text-gray-800">① 가격 정보의 면책</p>
                    <p class="bg-red-50 border border-red-100 rounded-xl px-4 py-3 text-red-800 text-sm leading-relaxed">
                        본 서비스에 표시된 비급여 진료비 정보는 건강보험심사평가원의 공개 데이터를 기반으로 제공되는 <strong>참고용 정보</strong>입니다.<br>
                        실제 의료기관에서 청구되는 금액은 진료 내용, 방법, 사용 재료, 시술 횟수 및 환자의 개별 상태에 따라<br>
                        서비스에 표시된 금액과 <strong>상이할 수 있습니다.</strong><br>
                        서비스는 표시된 가격과 실제 청구 금액의 차이로 인해 발생하는 어떠한 손해에 대해서도 법적 책임을 지지 않습니다.
                    </p>

                    <p class="font-semibold text-gray-800 mt-2">② 의료 행위에 관한 면책</p>
                    <p>
                        본 서비스는 의료 행위를 대리하거나 의학적 조언을 제공하지 않습니다.<br>
                        서비스의 정보를 근거로 한 의료적 결정으로 인해 발생하는 손해에 대해 서비스는 책임을 지지 않습니다.
                    </p>

                    <p class="font-semibold text-gray-800 mt-2">③ 데이터 오류 및 누락에 관한 면책</p>
                    <p>
                        건강보험심사평가원 원본 데이터의 오류, 누락 또는 지연 업데이트로 인해 발생하는<br>
                        정보의 부정확성에 대해 서비스는 책임을 지지 않습니다.
                    </p>

                    <p class="font-semibold text-gray-800 mt-2">④ 서비스 중단에 관한 면책</p>
                    <p>
                        천재지변, 시스템 점검, 통신 장애 등 불가항력적 사유로 서비스가 중단되는 경우<br>
                        이로 인한 손해에 대해 책임을 지지 않습니다.
                    </p>

                    <p class="font-semibold text-gray-800 mt-2">⑤ 제3자 서비스에 관한 면책</p>
                    <p>
                        서비스는 Google 소셜 로그인, 네이버 지도 등 제3자 서비스를 활용합니다.<br>
                        해당 외부 서비스의 오류·중단으로 인한 손해에 대해 서비스는 책임을 지지 않습니다.
                    </p>
                </div>
            </div>

            <%-- 제7조 --%>
            <div class="bg-white rounded-2xl p-6" style="box-shadow:0 2px 10px rgba(0,0,0,0.06);">
                <h2 class="text-base font-bold text-gray-900 mb-4">제7조 (이용자의 의무)</h2>
                <ul class="space-y-2 text-sm text-gray-600 leading-relaxed">
                    <li>① 이용자는 관련 법령, 본 약관 및 서비스의 이용 안내 사항을 준수하여야 합니다.</li>
                    <li>② 이용자는 다음 각 호의 행위를 하여서는 안 됩니다.
                        <ul class="mt-2 ml-4 space-y-1 text-gray-500">
                            <li>- 타인의 개인정보를 무단으로 수집·저장·공개하는 행위</li>
                            <li>- 서비스의 운영을 방해하거나 시스템에 악의적인 영향을 미치는 행위</li>
                            <li>- 서비스를 통해 얻은 정보를 서비스의 사전 동의 없이 상업적으로 이용하는 행위</li>
                            <li>- 기타 불법 또는 부당한 행위</li>
                        </ul>
                    </li>
                </ul>
            </div>

            <%-- 제8조 --%>
            <div class="bg-white rounded-2xl p-6" style="box-shadow:0 2px 10px rgba(0,0,0,0.06);">
                <h2 class="text-base font-bold text-gray-900 mb-4">제8조 (분쟁 해결 및 관할)</h2>
                <ul class="space-y-2 text-sm text-gray-600 leading-relaxed">
                    <li>① 서비스 이용과 관련하여 발생한 분쟁은 상호 협의에 의해 해결함을 원칙으로 합니다.</li>
                    <li>
                        ② 분쟁이 해결되지 않을 경우 대한민국 법률을 준거법으로 하며,<br>
                        관할 법원은 민사소송법에 따릅니다.
                    </li>
                    <li>③ 서비스 이용 관련 문의 및 분쟁 신고는 아래 연락처로 접수하시기 바랍니다.
                        <div class="mt-2 ml-4 space-y-0.5">
                            <p class="font-medium text-[#2563EB]">이메일: khaung228@gmail.com</p>
                            <p class="font-medium text-[#2563EB]">전화: 010-5924-8764</p>
                        </div>
                    </li>
                </ul>
            </div>

        </div>

    </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
