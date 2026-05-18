<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="즐겨찾기" scope="request"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="max-w-3xl mx-auto px-4 py-8">

    <span id="favorite-count" class="hidden"></span>

<%-- 비로그인 안내 --%>
    <div id="not-logged-in" class="hidden text-center py-16">
        <div class="w-16 h-16 bg-white rounded-2xl flex items-center justify-center mx-auto mb-4"
             style="box-shadow: 0 2px 10px rgba(0,0,0,0.08);">
            <svg class="w-7 h-7 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                      d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z"></path>
            </svg>
        </div>
        <p class="text-sm font-semibold text-gray-700 mb-1">로그인이 필요합니다</p>
        <p class="text-xs text-gray-400 mb-6">즐겨찾기를 이용하려면 로그인해주세요.</p>
        <a href="<c:url value="/auth/oauth2/authorize/google"/>"
           class="inline-flex items-center gap-2 px-6 py-2.5 bg-[#2563EB] text-white text-sm font-semibold rounded-xl hover:bg-blue-700 transition-colors">
            <svg class="w-4 h-4 flex-shrink-0" viewBox="0 0 24 24">
                <path fill="white" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"></path>
                <path fill="white" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"></path>
                <path fill="white" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"></path>
                <path fill="white" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"></path>
            </svg>
            구글로 로그인
        </a>
    </div>

    <%-- 로딩 --%>
    <div id="loading" class="space-y-3">
        <div class="h-24 bg-gray-100 rounded-xl animate-pulse"></div>
        <div class="h-24 bg-gray-100 rounded-xl animate-pulse"></div>
        <div class="h-24 bg-gray-100 rounded-xl animate-pulse"></div>
    </div>

    <%-- 즐겨찾기 없음 --%>
    <div id="empty-state" class="hidden text-center py-16">
        <div class="w-16 h-16 bg-white rounded-2xl flex items-center justify-center mx-auto mb-4"
             style="box-shadow: 0 2px 10px rgba(0,0,0,0.08);">
            <svg class="w-7 h-7 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                      d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0
                         00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0
                         00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1
                         1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1
                         1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0
                         00.951-.69l1.519-4.674z"></path>
            </svg>
        </div>
        <p class="text-sm font-semibold text-gray-700 mb-1">즐겨찾기한 병원이 없습니다</p>
        <p class="text-xs text-gray-400 mb-6">병원 검색 후 별 아이콘을 눌러 저장해보세요.</p>
        <a href="<c:url value="/hospitals"/>"
           class="inline-block px-6 py-2.5 bg-[#2563EB] text-white text-sm font-semibold rounded-xl hover:bg-blue-700 transition-colors">
            병원 검색하기
        </a>
    </div>

    <%-- 즐겨찾기 목록 --%>
    <div id="favorites-list" class="hidden space-y-3"></div>

</div>

<script defer src="<c:url value='/static/js/favorites.js'/>"></script>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
