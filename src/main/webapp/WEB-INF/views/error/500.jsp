<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="서버 오류" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="bg-[#F2F4F6] min-h-full flex items-center justify-center py-20 px-4">
    <div class="text-center max-w-sm w-full">

        <%-- 아이콘 블록 --%>
        <div class="relative inline-flex items-center justify-center w-28 h-28 mb-8">
            <div class="absolute inset-0 bg-red-50 rounded-3xl rotate-6"></div>
            <div class="absolute inset-0 bg-red-50/60 rounded-3xl -rotate-3"></div>
            <div class="relative flex flex-col items-center justify-center">
                <span class="text-4xl font-black text-red-400 leading-none tracking-tighter">500</span>
                <span class="text-[10px] font-semibold text-red-300 tracking-widest uppercase mt-0.5">Server Error</span>
            </div>
        </div>

        <h1 class="text-xl font-bold text-gray-900 mb-2">일시적인 오류가 발생했어요</h1>
        <p class="text-sm text-gray-400 leading-relaxed mb-8">
            서버에 문제가 생겼습니다. 잠시 후 다시 시도해 주세요.<br>
            문제가 계속되면 아래 이메일로 문의해 주세요.
        </p>

        <div class="flex flex-col sm:flex-row gap-3 justify-center mb-6">
            <a href="<c:url value='/'/>"
               class="inline-flex items-center justify-center gap-2 px-6 py-2.5 rounded-xl
                      bg-[#2563EB] text-white text-sm font-semibold hover:bg-blue-700 transition-colors"
               style="box-shadow:0 4px 14px rgba(37,99,235,0.35);">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                          d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0
                             01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"/>
                </svg>
                홈으로 가기
            </a>
            <button onclick="location.reload()"
                    class="inline-flex items-center justify-center gap-2 px-6 py-2.5 rounded-xl
                           bg-white text-gray-600 text-sm font-medium border border-gray-200
                           hover:bg-gray-50 transition-colors">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                          d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0
                             01-15.357-2m15.357 2H15"/>
                </svg>
                새로고침
            </button>
        </div>

        <a href="mailto:khaung228@gmail.com"
           class="inline-flex items-center gap-1.5 text-xs text-gray-400 hover:text-[#2563EB] transition-colors">
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8"
                      d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0
                         00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"/>
            </svg>
            khaung228@gmail.com
        </a>

    </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
