<%@ taglib prefix="c" uri="jakarta.tags.core" %>
</main>

<footer class="bg-white border-t border-gray-100 mt-auto">
    <div class="max-w-5xl mx-auto px-4 py-10">

        <div class="grid grid-cols-1 sm:grid-cols-3 gap-8">

            <%-- 데이터 출처 --%>
            <div>
                <p class="text-[11px] font-semibold text-gray-500 mb-3 uppercase tracking-widest">데이터 출처</p>
                <ul class="space-y-2 text-[12px] text-gray-400">
                    <li>건강보험심사평가원<br>비급여 진료비용 정보 공개 API</li>
                    <li>건강보험심사평가원<br>비급여 진료비용 통계</li>
                    <li>보건복지부 고시 제2025-48호</li>
                    <li>「의료법」 제45조의2<br>비급여 진료비용 공개제도</li>
                </ul>
            </div>

            <%-- 관련 기관 --%>
            <div>
                <p class="text-[11px] font-semibold text-gray-500 mb-3 uppercase tracking-widest">관련 기관</p>
                <ul class="space-y-2">
                    <li>
                        <a href="https://www.hira.or.kr" target="_blank" rel="noopener"
                           class="text-[12px] text-gray-400 hover:text-[#2563EB] transition-colors">
                            건강보험심사평가원 ↗
                        </a>
                    </li>
                    <li>
                        <a href="https://www.mohw.go.kr" target="_blank" rel="noopener"
                           class="text-[12px] text-gray-400 hover:text-[#2563EB] transition-colors">
                            보건복지부 ↗
                        </a>
                    </li>
                </ul>
            </div>

            <%-- 유의사항 --%>
            <div>
                <p class="text-[11px] font-semibold text-gray-500 mb-3 uppercase tracking-widest">유의사항</p>
                <p class="text-[12px] text-gray-400 leading-relaxed">
                    본 서비스의 비급여 진료비 정보는<br>
                    건강보험심사평가원의<br>
                    공개 데이터를 기반으로 하며,<br>
                    실제 청구 금액과 다를 수 있습니다.<br>
                    의료 결정 전 반드시 해당 의료기관에<br>
                    직접 확인하시기 바랍니다.
                </p>
            </div>

        </div>

        <%-- 저작권 --%>
        <div class="mt-8 pt-6 border-t border-gray-100 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-1.5">
            <p class="text-[11px] text-gray-400">&copy; 2026 MediPrice. 비급여 진료비 비교 플랫폼.</p>
            <p class="text-[11px] text-gray-300">건강보험심사평가원 공식 데이터 기반 · 「의료법」 제45조의2</p>
        </div>

    </div>
</footer>

</body>
</html>
