</main>

<footer class="bg-white border-t border-gray-100 mt-auto">
    <div class="max-w-5xl mx-auto px-4 pt-10 pb-6">

        <%-- 상단: 4컬럼 정보 --%>
        <div class="grid grid-cols-2 lg:grid-cols-4 gap-8 pb-8 border-b border-gray-100">

            <%-- 서비스 소개 --%>
            <div class="col-span-2 lg:col-span-1">
                <div class="flex items-center gap-2 mb-3">
                    <span class="text-sm font-bold text-gray-900">MediPrice</span>
                    <span class="text-[10px] bg-blue-50 text-[#2563EB] font-semibold px-2 py-0.5 rounded-full">Beta</span>
                </div>
                <p class="text-[12px] text-gray-400 leading-relaxed mb-4">
                    건강보험심사평가원 공개 데이터 기반<br>비급여 진료비 비교 플랫폼
                </p>
                <div class="space-y-1.5">
                    <a href="mailto:khaung228@gmail.com"
                       class="flex items-center gap-1.5 text-[12px] text-gray-500 hover:text-[#2563EB] transition-colors">
                        <svg class="w-3.5 h-3.5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                                  d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"/>
                        </svg>
                        khaung228@gmail.com
                    </a>
                    <a href="tel:01059248764"
                       class="flex items-center gap-1.5 text-[12px] text-gray-500 hover:text-[#2563EB] transition-colors">
                        <svg class="w-3.5 h-3.5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                                  d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z"/>
                        </svg>
                        010-5924-8764
                    </a>
                </div>
            </div>

            <%-- 약관 --%>
            <div>
                <p class="text-[11px] font-semibold text-gray-500 mb-3 uppercase tracking-widest">약관</p>
                <ul class="space-y-2">
                    <li>
                        <a href="/legal/terms"
                           class="text-[12px] text-gray-400 hover:text-[#2563EB] transition-colors">
                            이용약관
                        </a>
                    </li>
                    <li>
                        <a href="/legal/privacy"
                           class="text-[12px] text-gray-400 hover:text-[#2563EB] transition-colors">
                            개인정보처리방침
                        </a>
                    </li>
                    <li>
                        <a href="/legal/location"
                           class="text-[12px] text-gray-400 hover:text-[#2563EB] transition-colors">
                            위치기반서비스 이용약관
                        </a>
                    </li>
                </ul>
            </div>

            <%-- 데이터 출처 --%>
            <div>
                <p class="text-[11px] font-semibold text-gray-500 mb-3 uppercase tracking-widest">데이터 출처</p>
                <ul class="space-y-2 text-[12px] text-gray-400 leading-relaxed">
                    <li>건강보험심사평가원<br>비급여 진료비용 공개 API</li>
                    <li>「의료법」 제45조의2</li>
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

        </div>

        <%-- 저작권 --%>
        <div class="pt-4 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-1">
            <p class="text-[11px] text-gray-400">&copy; 2026 MediPrice. All rights reserved.</p>
            <p class="text-[11px] text-gray-300">건강보험심사평가원 공식 데이터 기반 · 「의료법」 제45조의2</p>
        </div>

    </div>
</footer>

<script>
    if ('serviceWorker' in navigator) {
        window.addEventListener('load', () => {
            navigator.serviceWorker.register('/sw.js').catch(() => {});
        });
    }
</script>
</body>
</html>
