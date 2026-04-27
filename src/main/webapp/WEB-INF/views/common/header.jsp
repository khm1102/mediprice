<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${pageTitle != null ? pageTitle.concat(' - MediPrice') : 'MediPrice'}</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <script defer src="/static/js/api.js"></script>
    <script defer src="/static/js/auth.js"></script>
    <script defer src="/static/js/common.js"></script>
</head>
<body class="bg-gray-50 min-h-screen flex flex-col">

<header class="bg-white shadow-sm sticky top-0 z-50">
    <div class="max-w-6xl mx-auto px-4 py-3 flex items-center justify-between">

        <%-- 로고 --%>
        <a href="/" class="text-blue-600 font-bold text-xl tracking-tight">
            MediPrice
        </a>

        <%-- 네비게이션 --%>
        <nav class="flex items-center gap-3">
            <div id="nav-guest" class="flex items-center gap-3">
                <button onclick="showToast('로그인 기능은 차후 개발 예정입니다', 'info')"
                        class="text-sm text-gray-600 hover:text-blue-600 transition-colors">
                    로그인
                </button>
                <button onclick="showToast('회원가입 기능은 차후 개발 예정입니다', 'info')"
                        class="text-sm bg-blue-600 text-white px-4 py-1.5 rounded-full hover:bg-blue-700 transition-colors">
                    회원가입
                </button>
            </div>
            <div id="nav-member" class="hidden flex items-center gap-3">
                <button onclick="handleLogout()"
                        class="text-sm text-gray-600 hover:text-blue-600 transition-colors">
                    로그아웃
                </button>
            </div>
        </nav>

    </div>
</header>

<main class="flex-1">
