<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="서비스 이용 동의" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<style>
    @keyframes slideUp {
        from { opacity: 0; transform: translateY(24px); }
        to   { opacity: 1; transform: translateY(0); }
    }
    @keyframes checkPop {
        0%   { transform: scale(0.8); }
        60%  { transform: scale(1.15); }
        100% { transform: scale(1); }
    }
    .consent-card  { animation: slideUp 0.45s cubic-bezier(.22,.68,0,1.2) both; }
    .consent-card:nth-child(2) { animation-delay: 0.06s; }
    .consent-card:nth-child(3) { animation-delay: 0.12s; }
    .consent-card:nth-child(4) { animation-delay: 0.18s; }

    /* 커스텀 체크박스 — 기본 input 숨김 */
    .check-item input[type="checkbox"],
    .check-all input[type="checkbox"] { display: none; }
    .check-box {
        width: 22px; height: 22px; flex-shrink: 0;
        border: 2px solid #D1D5DB;
        border-radius: 6px;
        display: flex; align-items: center; justify-content: center;
        transition: background 0.18s, border-color 0.18s;
        cursor: pointer;
    }
    .check-item input:checked ~ .check-box {
        background: #2563EB;
        border-color: #2563EB;
        animation: checkPop 0.25s ease both;
    }
    .check-item input:checked ~ .check-box svg { opacity: 1; }
    .check-box svg { opacity: 0; transition: opacity 0.1s; }

    /* 전체 동의 체크박스 */
    .check-all-box {
        width: 26px; height: 26px; flex-shrink: 0;
        border: 2px solid #D1D5DB;
        border-radius: 7px;
        display: flex; align-items: center; justify-content: center;
        transition: background 0.18s, border-color 0.18s;
        cursor: pointer;
    }
    .check-all input:checked ~ .check-all-box {
        background: #1D4ED8;
        border-color: #1D4ED8;
        animation: checkPop 0.25s ease both;
    }
    .check-all input:checked ~ .check-all-box svg { opacity: 1; }
    .check-all-box svg { opacity: 0; transition: opacity 0.1s; }

    /* 제출 버튼 활성화 효과 */
    #submit-btn:not(:disabled) {
        background: linear-gradient(135deg, #2563EB 0%, #1D4ED8 100%);
        box-shadow: 0 4px 14px rgba(37,99,235,0.4);
    }
    #submit-btn:not(:disabled):hover {
        transform: translateY(-1px);
        box-shadow: 0 6px 20px rgba(37,99,235,0.45);
    }
    #submit-btn { transition: all 0.2s; }
</style>

<div class="bg-[#F2F4F6] min-h-full pb-16 pt-8">
    <div class="max-w-lg mx-auto px-4">

        <%-- 상단: 서비스 로고 + 안내 문구 --%>
        <div class="text-center mb-8 consent-card">
            <div class="inline-flex items-center justify-center w-14 h-14 bg-[#EFF6FF] rounded-2xl mb-4">
                <svg class="w-7 h-7 text-[#2563EB]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8"
                          d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"/>
                </svg>
            </div>
            <h1 class="text-xl font-bold text-gray-900 mb-1">MediPrice 서비스 이용 동의</h1>
            <p class="text-sm text-gray-500 leading-relaxed">
                서비스를 이용하시려면 아래 약관에 동의해 주세요.
            </p>
        </div>

        <%-- 구글 계정 정보 배너 --%>
        <div class="bg-white rounded-2xl px-5 py-4 flex items-center gap-3 mb-4 consent-card"
             style="box-shadow:0 2px 10px rgba(0,0,0,0.06);">
            <div class="w-9 h-9 rounded-full bg-blue-50 flex items-center justify-center flex-shrink-0">
                <svg class="w-5 h-5" viewBox="0 0 24 24">
                    <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                    <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                    <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                    <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
                </svg>
            </div>
            <div class="min-w-0">
                <p class="text-sm font-semibold text-gray-800 truncate"><c:out value="${userName}"/></p>
                <p class="text-xs text-gray-400 truncate"><c:out value="${userEmail}"/></p>
            </div>
            <span class="ml-auto text-xs bg-green-50 text-green-600 font-medium px-2.5 py-1 rounded-full flex-shrink-0">Google 계정</span>
        </div>

        <%-- 오류 메시지 --%>
        <c:if test="${param.error == 'required'}">
            <div class="bg-red-50 border border-red-200 rounded-xl px-4 py-3 mb-4 flex items-center gap-2">
                <svg class="w-4 h-4 text-red-500 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
                </svg>
                <p class="text-sm text-red-700">필수 항목에 모두 동의해 주세요.</p>
            </div>
        </c:if>

        <form action="<c:url value='/auth/consent'/>" method="post">

            <%-- 전체 동의 카드 --%>
            <div class="bg-white rounded-2xl px-5 py-4 mb-3 consent-card"
                 style="box-shadow:0 2px 10px rgba(0,0,0,0.06);">
                <label class="check-all flex items-center gap-3 cursor-pointer select-none">
                    <input type="checkbox" id="check-all">
                    <div class="check-all-box">
                        <svg class="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
                            <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7"/>
                        </svg>
                    </div>
                    <div>
                        <p class="text-sm font-bold text-gray-900">전체 동의</p>
                        <p class="text-xs text-gray-400 mt-0.5">아래 4가지 항목에 모두 동의합니다</p>
                    </div>
                </label>
            </div>

            <%-- 개별 동의 카드 --%>
            <div class="bg-white rounded-2xl divide-y divide-gray-50 mb-4 consent-card"
                 style="box-shadow:0 2px 10px rgba(0,0,0,0.06);">

                <%-- 이용약관 (필수) --%>
                <div class="px-5 py-4">
                    <label class="check-item flex items-center gap-3 cursor-pointer select-none">
                        <input type="checkbox" name="termsAgreed" id="check-terms" value="true">
                        <div class="check-box">
                            <svg class="w-3.5 h-3.5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
                                <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7"/>
                            </svg>
                        </div>
                        <div class="flex-1 min-w-0">
                            <div class="flex items-center gap-1.5">
                                <span class="text-sm text-gray-800">이용약관 동의</span>
                                <span class="text-[10px] font-semibold text-red-500 bg-red-50 px-1.5 py-0.5 rounded-full">필수</span>
                            </div>
                            <p class="text-xs text-gray-400 mt-0.5 leading-relaxed">
                                비급여 진료비 비교 서비스 이용 조건 및 책임 한계 안내
                            </p>
                        </div>
                        <a href="<c:url value='/legal/terms'/>" target="_blank" rel="noopener"
                           onclick="event.stopPropagation();"
                           class="text-[11px] text-[#2563EB] hover:underline flex-shrink-0 ml-2">보기</a>
                    </label>
                </div>

                <%-- 개인정보처리방침 (필수) --%>
                <div class="px-5 py-4">
                    <label class="check-item flex items-center gap-3 cursor-pointer select-none">
                        <input type="checkbox" name="privacyAgreed" id="check-privacy" value="true">
                        <div class="check-box">
                            <svg class="w-3.5 h-3.5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
                                <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7"/>
                            </svg>
                        </div>
                        <div class="flex-1 min-w-0">
                            <div class="flex items-center gap-1.5">
                                <span class="text-sm text-gray-800">개인정보처리방침 동의</span>
                                <span class="text-[10px] font-semibold text-red-500 bg-red-50 px-1.5 py-0.5 rounded-full">필수</span>
                            </div>
                            <p class="text-xs text-gray-400 mt-0.5 leading-relaxed">
                                Google 계정 정보 수집 및 이용 안내 (이메일, 이름, 프로필)
                            </p>
                        </div>
                        <a href="<c:url value='/legal/privacy'/>" target="_blank" rel="noopener"
                           onclick="event.stopPropagation();"
                           class="text-[11px] text-[#2563EB] hover:underline flex-shrink-0 ml-2">보기</a>
                    </label>
                </div>

                <%-- 위치기반서비스 (필수) --%>
                <div class="px-5 py-4">
                    <label class="check-item flex items-center gap-3 cursor-pointer select-none">
                        <input type="checkbox" name="locationAgreed" id="check-location" value="true">
                        <div class="check-box">
                            <svg class="w-3.5 h-3.5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
                                <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7"/>
                            </svg>
                        </div>
                        <div class="flex-1 min-w-0">
                            <div class="flex items-center gap-1.5">
                                <span class="text-sm text-gray-800">위치기반서비스 이용약관 동의</span>
                                <span class="text-[10px] font-semibold text-red-500 bg-red-50 px-1.5 py-0.5 rounded-full">필수</span>
                            </div>
                            <p class="text-xs text-gray-400 mt-0.5 leading-relaxed">
                                주변 병원 거리순 검색에 현재 위치를 사용합니다
                            </p>
                        </div>
                        <a href="<c:url value='/legal/location'/>" target="_blank" rel="noopener"
                           onclick="event.stopPropagation();"
                           class="text-[11px] text-[#2563EB] hover:underline flex-shrink-0 ml-2">보기</a>
                    </label>
                </div>

                <%-- 만 14세 이상 (필수) --%>
                <div class="px-5 py-4">
                    <label class="check-item flex items-center gap-3 cursor-pointer select-none">
                        <input type="checkbox" name="ageAgreed" id="check-age" value="true">
                        <div class="check-box">
                            <svg class="w-3.5 h-3.5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
                                <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7"/>
                            </svg>
                        </div>
                        <div class="flex-1 min-w-0">
                            <div class="flex items-center gap-1.5">
                                <span class="text-sm text-gray-800">만 14세 이상 확인</span>
                                <span class="text-[10px] font-semibold text-red-500 bg-red-50 px-1.5 py-0.5 rounded-full">필수</span>
                            </div>
                            <p class="text-xs text-gray-400 mt-0.5 leading-relaxed">
                                만 14세 미만은 법정대리인 동의가 필요합니다 (「정보통신망법」 제31조)
                            </p>
                        </div>
                    </label>
                </div>

            </div>

            <%-- 제출 버튼 --%>
            <button type="submit" id="submit-btn" disabled
                    class="w-full py-3.5 rounded-2xl text-sm font-semibold text-white
                           bg-gray-200 text-gray-400 cursor-not-allowed">
                동의하고 시작하기
            </button>

            <p class="text-center text-xs text-gray-400 mt-3 leading-relaxed">
                위 4가지 항목에 모두 동의하시면 MediPrice 회원 가입이 완료됩니다.
            </p>

        </form>

    </div>
</div>

<script>
    (() => {
        const checkAll    = document.getElementById('check-all');
        const checkTerms   = document.getElementById('check-terms');
        const checkPrivacy = document.getElementById('check-privacy');
        const checkLocation = document.getElementById('check-location');
        const checkAge    = document.getElementById('check-age');
        const submitBtn   = document.getElementById('submit-btn');

        const required = [checkTerms, checkPrivacy, checkLocation, checkAge];
        const all      = [checkTerms, checkPrivacy, checkLocation, checkAge];

        const updateSubmit = () => {
            const ok = required.every(c => c.checked);
            submitBtn.disabled = !ok;
            if (ok) {
                submitBtn.classList.remove('bg-gray-200', 'text-gray-400', 'cursor-not-allowed');
                submitBtn.classList.add('text-white');
            } else {
                submitBtn.classList.add('bg-gray-200', 'text-gray-400', 'cursor-not-allowed');
            }
        };

        const updateAllCheck = () => {
            checkAll.checked = all.every(c => c.checked);
        };

        checkAll.addEventListener('change', () => {
            all.forEach(c => { c.checked = checkAll.checked; });
            updateSubmit();
        });

        all.forEach(c => {
            c.addEventListener('change', () => {
                updateAllCheck();
                updateSubmit();
            });
        });

        updateSubmit();
    })();
</script>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
