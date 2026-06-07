<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="theme-color" content="#2563EB">
    <meta name="description" content="비급여 진료비 비교 플랫폼. 내 주변 병원의 비급여 진료비를 한눈에 비교하세요.">
    <meta name="referrer" content="strict-origin-when-cross-origin">
    <%-- PWA --%>
    <link rel="manifest" href="<c:url value='/manifest.json'/>">
    <meta name="mobile-web-app-capable" content="yes">
    <meta name="apple-mobile-web-app-capable" content="yes">
    <meta name="apple-mobile-web-app-status-bar-style" content="default">
    <meta name="apple-mobile-web-app-title" content="MediPrice">
    <link rel="apple-touch-icon" href="<c:url value='/static/pwa/icons/icon-180.png'/>">
    <title>
        <c:choose>
            <c:when test="${not empty pageTitle}">
                <c:out value="${pageTitle}" /> - MediPrice
            </c:when>
            <c:otherwise>MediPrice</c:otherwise>
        </c:choose>
    </title>
<link rel="stylesheet" href="https://cdn.jsdelivr.net/gh/spoqa/spoqa-han-sans@latest/css/SpoqaHanSansNeo.css">
    <script>
        tailwind = { config: {
            theme: {
                extend: {
                    fontFamily: {
                        sans: ['Spoqa Han Sans Neo', 'Apple SD Gothic Neo', 'ui-sans-serif', 'system-ui', 'sans-serif']
                    }
                }
            }
        }};
    </script>
    <script src="https://cdn.tailwindcss.com"></script>
    <script defer src="<c:url value='/static/js/api.js'/>?v=20260607-ux6"></script>
    <script defer src="<c:url value='/static/js/auth.js'/>?v=20260607-ux6"></script>
    <script defer src="<c:url value='/static/js/common.js'/>?v=20260607-ux6"></script>
</head>
<body class="bg-[#F9FAFB] min-h-screen flex flex-col">

<%-- ── 베타 설문 배너 ── 링크는 아래 SURVEY_URL 변수만 변경 --%>
<div id="survey-banner" class="bg-[#EFF6FF] border-b border-blue-100">
    <div class="max-w-5xl mx-auto px-4 py-2 flex items-center justify-between gap-3">
        <p class="text-xs text-[#1D4ED8] flex-1 text-center">
            MediPrice 베타 서비스 중입니다
            <a id="survey-link" href="#" target="_blank" rel="noopener noreferrer"
               class="font-semibold underline underline-offset-2 hover:text-blue-800 ml-1">
                설문조사 참여하기 →
            </a>
        </p>
        <button onclick="document.getElementById('survey-banner').remove()"
                class="flex-shrink-0 text-blue-400 hover:text-blue-600 transition-colors p-0.5"
                aria-label="닫기">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
        </button>
    </div>
</div>
<script>
    // 설문 링크 — 여기만 변경
    const SURVEY_URL = 'https://forms.gle/d4fVKeFnV2PFr8pXA';
    document.getElementById('survey-link').href = SURVEY_URL;
</script>

<header class="bg-white border-b border-gray-200 sticky top-0 z-50 h-14 flex items-center">
    <div class="px-4 lg:px-6 w-full flex items-center justify-between">

        <%-- 로고 --%>
        <a href="<c:url value='/'/>" class="flex items-center gap-2">
            <span class="text-[#2563EB] font-bold text-xl tracking-tight">MediPrice</span>
            <span class="text-[10px] bg-blue-50 text-[#2563EB] font-semibold px-2 py-0.5 rounded-full leading-none">Beta</span>
        </a>

        <%-- 네비게이션 --%>
        <nav class="flex items-center gap-1">
            <%-- 비로그인 --%>
            <div id="nav-guest" class="flex items-center gap-2">
                <a href="<c:url value='/auth/oauth2/authorize/google'/>"
                   class="flex items-center gap-2 text-sm text-[#2563EB] font-medium border border-[#2563EB]
                          rounded-lg hover:bg-[#2563EB] hover:text-white transition-colors min-h-[36px] px-4
                          group">
                    <svg class="w-4 h-4 flex-shrink-0" viewBox="0 0 24 24">
                        <path fill="#4285F4"  class="group-hover:fill-white transition-none"
                              d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"></path>
                        <path fill="#34A853"  class="group-hover:fill-white transition-none"
                              d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"></path>
                        <path fill="#FBBC05"  class="group-hover:fill-white transition-none"
                              d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"></path>
                        <path fill="#EA4335"  class="group-hover:fill-white transition-none"
                              d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"></path>
                    </svg>
                    로그인
                </a>
            </div>
            <%-- 로그인 후: 프로필 버튼 + 드롭다운 --%>
            <div id="nav-member" class="hidden relative">
                <button id="profile-btn"
                        class="w-9 h-9 rounded-full bg-[#EFF6FF] flex items-center justify-center
                               hover:bg-blue-100 transition-colors focus:outline-none select-none">
                    <svg class="w-5 h-5 text-[#2563EB]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8"
                              d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2M12 11a4 4 0 100-8 4 4 0 000 8z"></path>
                    </svg>
                </button>

                <%-- 드롭다운 메뉴 --%>
                <div id="profile-menu"
                     class="hidden absolute right-0 mt-2 w-64 bg-white rounded-2xl border border-gray-100 z-[100]"
                     style="box-shadow: 0 8px 32px rgba(0,0,0,0.12); top: 100%;">

                    <%-- 사용자 정보 --%>
                    <div class="px-4 pt-4 pb-3 border-b border-gray-100">
                        <div class="flex items-center gap-3">
                            <div class="w-10 h-10 rounded-full bg-[#EFF6FF] flex items-center justify-center flex-shrink-0">
                                <svg class="w-5 h-5 text-[#2563EB]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8"
                                          d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2M12 11a4 4 0 100-8 4 4 0 000 8z"></path>
                                </svg>
                            </div>
                            <div class="min-w-0">
                                <p id="menu-name" class="text-sm font-semibold text-gray-900 truncate"></p>
                                <p id="menu-email" class="text-xs text-gray-400 truncate mt-0.5"></p>
                            </div>
                        </div>
                    </div>

                    <%-- 즐겨찾기 링크 --%>
                    <a href="<c:url value='/favorites'/>"
                       class="flex items-center gap-2 pl-6 pr-4 py-3 hover:bg-gray-50 transition-colors">
                        <svg class="w-4 h-4 text-gray-400 flex-shrink-0" fill="none" stroke="currentColor"
                             viewBox="0 0 24 24" stroke-width="1.8">
                            <path stroke-linecap="round" stroke-linejoin="round"
                                  d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0
                                     00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0
                                     00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1
                                     1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1
                                     1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0
                                     00.951-.69l1.519-4.674z"></path>
                        </svg>
                        <span class="text-sm text-gray-700">즐겨찾기</span>
                    </a>

                    <%-- 로그아웃 / 회원탈퇴 --%>
                    <div class="flex border-t border-gray-100 rounded-b-2xl overflow-hidden">
                        <button onclick="handleLogout()"
                                class="flex-1 flex items-center justify-center gap-2 px-4 py-3 hover:bg-gray-50 transition-colors">
                            <svg class="w-4 h-4 text-red-400 flex-shrink-0" fill="none" stroke="currentColor"
                                 viewBox="0 0 24 24" stroke-width="1.8">
                                <path stroke-linecap="round" stroke-linejoin="round"
                                      d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"></path>
                            </svg>
                            <span class="text-sm text-red-500">로그아웃</span>
                        </button>
                        <button onclick="openWithdrawDialog()"
                                class="flex-1 flex items-center justify-center px-4 py-3 hover:bg-gray-50 transition-colors
                                       border-l border-gray-100">
                            <span class="text-sm text-gray-400">회원탈퇴</span>
                        </button>
                    </div>
                </div>
            </div>
        </nav>

    </div>
</header>

<%-- 회원탈퇴 확인 다이얼로그 --%>
<div id="withdraw-dialog"
     class="hidden fixed inset-0 z-[500] flex items-center justify-center px-4">
    <div id="withdraw-backdrop"
         class="absolute inset-0 bg-black/40 backdrop-blur-sm"
         onclick="closeWithdrawDialog()"></div>
    <div id="withdraw-card"
         class="relative bg-white rounded-2xl p-6 w-full max-w-xs text-center"
         style="box-shadow:0 24px 64px rgba(0,0,0,0.18);">
        <%-- 아이콘 --%>
        <div class="w-14 h-14 bg-red-50 rounded-2xl flex items-center justify-center mx-auto mb-4">
            <svg class="w-7 h-7 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="1.8">
                <path stroke-linecap="round" stroke-linejoin="round"
                      d="M13 7a4 4 0 11-8 0 4 4 0 018 0zM9 14a6 6 0 00-6 6v1h12v-1a6 6 0 00-6-6zM21 12h-6"/>
            </svg>
        </div>
        <p class="text-base font-bold text-gray-900 mb-1">정말 탈퇴하시겠어요?</p>
        <p class="text-xs text-gray-400 leading-relaxed mb-6">
            탈퇴하면 즐겨찾기 등 모든 데이터가 삭제되며<br>복구할 수 없습니다.
        </p>
        <div class="flex gap-2">
            <button onclick="closeWithdrawDialog()"
                    class="flex-1 h-11 rounded-xl bg-gray-100 text-sm font-semibold text-gray-600
                           hover:bg-gray-200 transition-colors">
                취소
            </button>
            <button onclick="confirmWithdraw()"
                    class="flex-1 h-11 rounded-xl bg-red-500 text-sm font-semibold text-white
                           hover:bg-red-600 transition-colors">
                탈퇴하기
            </button>
        </div>
    </div>
</div>

<%-- 쿠키 사용 동의 배너 (localStorage로 표시 여부 관리) --%>
<div id="cookie-banner"
     class="hidden fixed bottom-0 left-0 right-0 z-[200] px-4 py-3 bg-gray-900/95 backdrop-blur-sm"
     style="border-top: 1px solid rgba(255,255,255,0.08);">
    <div class="max-w-5xl mx-auto flex flex-col sm:flex-row items-start sm:items-center gap-3">
        <div class="flex items-start gap-2.5 flex-1 min-w-0">
            <svg class="w-4 h-4 text-gray-400 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8"
                      d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
            <p class="text-xs text-gray-300 leading-relaxed">
                MediPrice는 서비스 제공 및 보안을 위해 쿠키(JWT 인증 토큰)를 사용합니다.
                자세한 내용은
                <a href="/legal/privacy" class="text-blue-400 hover:underline">개인정보처리방침</a>을 참조하세요.
            </p>
        </div>
        <button id="cookie-accept-btn"
                onclick="acceptCookies()"
                class="flex-shrink-0 text-xs font-semibold bg-[#2563EB] hover:bg-blue-700
                       text-white px-4 py-2 rounded-lg transition-colors whitespace-nowrap">
            확인
        </button>
    </div>
</div>

<script>
    (() => {
        if (!localStorage.getItem('cookie_consent_accepted')) {
            const banner = document.getElementById('cookie-banner');
            if (banner) banner.classList.remove('hidden');
        }
    })();

    const acceptCookies = () => {
        localStorage.setItem('cookie_consent_accepted', '1');
        const banner = document.getElementById('cookie-banner');
        if (banner) {
            banner.style.transition = 'opacity 0.3s, transform 0.3s';
            banner.style.opacity = '0';
            banner.style.transform = 'translateY(8px)';
            setTimeout(() => banner.remove(), 320);
        }
    };
</script>

<main class="flex-1">
