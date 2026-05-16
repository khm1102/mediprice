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
    <link rel="manifest" href="/manifest.json">
    <meta name="mobile-web-app-capable" content="yes">
    <meta name="apple-mobile-web-app-capable" content="yes">
    <meta name="apple-mobile-web-app-status-bar-style" content="default">
    <meta name="apple-mobile-web-app-title" content="MediPrice">
    <link rel="apple-touch-icon" href="/static/pwa/icons/icon-180.png">
    <title><c:out value="${pageTitle != null ? pageTitle.concat(' - MediPrice') : 'MediPrice'}" escapeXml="false"/></title>
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
    <script defer src="<c:url value='/static/js/api.js'/>"></script>
    <script defer src="<c:url value='/static/js/auth.js'/>"></script>
    <script defer src="<c:url value='/static/js/common.js'/>"></script>
</head>
<body class="bg-[#F9FAFB] min-h-screen flex flex-col">

<header class="bg-white border-b border-gray-200 sticky top-0 z-50 h-14 flex items-center">
    <div class="px-4 lg:px-6 w-full flex items-center justify-between">

        <%-- 로고 --%>
        <a href="<c:url value="/"/>" class="flex items-center">
            <span class="text-[#2563EB] font-bold text-xl tracking-tight">MediPrice</span>
        </a>

        <%-- 네비게이션 --%>
        <nav class="flex items-center">
            <div id="nav-guest">
                <button onclick="showToast('로그인 기능은 차후 개발 예정입니다', 'info')"
                        class="text-sm text-[#2563EB] font-medium border border-[#2563EB] rounded-lg hover:bg-[#2563EB] hover:text-white transition-colors min-h-[36px] px-4">
                    로그인
                </button>
            </div>
            <div id="nav-member" class="hidden">
                <button onclick="handleLogout()"
                        class="text-sm text-[#6B7280] hover:text-[#2563EB] transition-colors min-h-[44px] px-3">
                    로그아웃
                </button>
            </div>
        </nav>

    </div>
</header>

<main class="flex-1">
</main>