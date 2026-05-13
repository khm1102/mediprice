<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html;charset=UTF-8" isErrorPage="true" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>페이지를 찾을 수 없습니다 - MediPrice</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="bg-[#F9FAFB] min-h-screen flex flex-col">

<header class="bg-white border-b border-gray-100 h-14 flex items-center">
    <div class="max-w-6xl mx-auto px-4 w-full">
        <a href="<c:url value="/"/>" class="text-[#2563EB] font-bold text-xl tracking-tight">MediPrice</a>
    </div>
</header>

<main class="flex-1 flex items-center justify-center px-4">
    <div class="text-center">
        <p class="text-8xl font-bold text-blue-100 mb-4 leading-none">404</p>
        <h1 class="text-lg font-semibold text-gray-700 mb-2">페이지를 찾을 수 없어요</h1>
        <p class="text-sm text-[#6B7280] mb-8">주소를 다시 확인해주세요</p>
        <a href="<c:url value="/"/>"
           class="inline-block bg-[#2563EB] text-white text-sm px-6 py-2.5 rounded-full hover:bg-blue-700 transition-colors min-h-[44px] leading-[19px]">
            홈으로 돌아가기
        </a>
    </div>
</main>

<footer class="bg-white border-t border-gray-100">
    <div class="max-w-6xl mx-auto px-4 py-4 text-center text-xs text-[#6B7280]">
        &copy; 2026 MediPrice. 비급여 진료비 비교 플랫폼.
    </div>
</footer>

</body>
</html>
