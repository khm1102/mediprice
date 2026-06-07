/**
 * 공통 유틸 함수
 */

/**
 * HTML 특수문자 이스케이프. innerHTML 템플릿에 외부 데이터를 합칠 때 반드시 사용.
 * textContent 트릭으로 브라우저 HTML 인코딩 규칙을 그대로 빌려온다.
 * @param {*} str
 * @returns {string}
 */
const escapeHtml = (str) => {
    if (str == null) return '';
    const div = document.createElement('div');
    div.textContent = String(str);
    return div.innerHTML;
};

/**
 * 숫자를 한국 원화 형식으로 포맷
 * @param {number} amount
 * @returns {string} 예: 150,000원
 */
const formatPrice = (amount) => {
    if (amount == null) return '정보 없음';
    return amount.toLocaleString('ko-KR') + '원';
};

/**
 * 거리를 보기 좋게 포맷
 * @param {number} meters
 * @returns {string} 예: 1.2km, 500m
 */
const formatDistance = (meters) => {
    if (meters == null) return '';
    if (meters >= 1000) return (meters / 1000).toFixed(1) + 'km';
    return Math.round(meters) + 'm';
};

/**
 * 토스트 알림 표시
 * @param {string} message
 * @param {'info'|'error'} type
 */
const showToast = (() => {
    // keyframe 한 번만 주입
    if (!document.getElementById('toast-style')) {
        const style = document.createElement('style');
        style.id = 'toast-style';
        style.textContent = `
            @keyframes toastIn {
                from { opacity: 0; bottom: 1rem; }
                to   { opacity: 1; bottom: 2rem; }
            }
            @keyframes toastOut {
                from { opacity: 1; bottom: 2rem; }
                to   { opacity: 0; bottom: 1rem; }
            }
            .toast-in  { animation: toastIn  0.3s ease forwards; }
            .toast-out { animation: toastOut 0.35s ease forwards; }
        `;
        document.head.appendChild(style);
    }

    return (message, type = 'info') => {
        const bgColor = type === 'error' ? '#ef4444' : '#1f2937';
        const toast = document.createElement('div');
        toast.textContent = message;
        toast.classList.add('toast-in');
        Object.assign(toast.style, {
            position: 'fixed',
            left: '50%',
            transform: 'translateX(-50%)',
            background: bgColor,
            color: '#fff',
            fontSize: '0.875rem',
            padding: '0.625rem 1.25rem',
            borderRadius: '9999px',
            boxShadow: '0 4px 16px rgba(0,0,0,0.18)',
            zIndex: '9999',
            whiteSpace: 'nowrap',
        });
        document.body.appendChild(toast);

        setTimeout(() => {
            toast.classList.replace('toast-in', 'toast-out');
            setTimeout(() => toast.remove(), 380);
        }, 2500);
    };
})();

/**
 * 키워드 배열을 인기 칩(가로 스크롤 가능)으로 렌더한다.
 * 클릭 시 onSelect(keyword) 호출. innerHTML 안전화를 위해 escapeHtml 사용.
 * @param {HTMLElement} container 칩이 들어갈 컨테이너
 * @param {string[]} keywords     칩 텍스트로 쓸 키워드들
 * @param {(kw: string) => void} onSelect
 */
const renderQuickChips = (container, keywords, onSelect) => {
    if (!container) return;
    container.innerHTML = keywords.map(k => `
        <button type="button" data-quick-keyword="${escapeHtml(k)}"
                class="quick-chip flex-shrink-0 px-3 py-1.5 rounded-full text-xs font-medium
                       bg-blue-50 text-[#2563EB] hover:bg-blue-100 active:bg-blue-200 transition-colors whitespace-nowrap">
            ${escapeHtml(k)}
        </button>`).join('');
    container.querySelectorAll('.quick-chip').forEach(btn => {
        btn.addEventListener('click', () => {
            const kw = btn.getAttribute('data-quick-keyword') ?? '';
            if (kw && onSelect) onSelect(kw);
        });
    });
};

/**
 * 키워드 매칭 우선순위 점수. 작을수록 우선.
 * - 0: exact ("MRI" 입력 → "MRI" 항목)
 * - 1: prefix ("MRI" 입력 → "MRI 척추")
 * - 2: word-boundary prefix (다른 단어가 kw로 시작)
 * - 3 + indexOf/100: includes — 매칭 위치가 앞일수록 우선
 * - 인기 키워드(POPULAR boost): 같은 단계 내에서 -0.5
 * 매칭 안 됨: Infinity.
 *
 * @param {string} name           npayKorNm
 * @param {string} mdivCdNm       중분류명 (정렬엔 직접 사용 안 함, dropdown sub로만 노출)
 * @param {string} kw             소문자 키워드
 * @param {string[]} [popular]    인기 키워드(npayKorNm 자체 비교)
 * @returns {number}
 */
const scoreMatch = (name, mdivCdNm, kw, popular = []) => {
    if (!name || !kw) return Infinity;
    const lower = String(name).toLowerCase();
    let base;
    if (lower === kw) {
        base = 0;
    } else if (lower.startsWith(kw)) {
        base = 1;
    } else {
        // word-boundary prefix: 다른 토큰이 kw로 시작하면 2.
        const wordStart = lower.split(/[\s/·().·-]+/).some((token, i) => i > 0 && token.startsWith(kw));
        if (wordStart) {
            base = 2;
        } else {
            const idx = lower.indexOf(kw);
            if (idx < 0) return Infinity;
            base = 3 + idx / 100;
        }
    }
    if (popular.includes(name)) base -= 0.5;
    return base;
};

/**
 * 검색 input에 자동완성 dropdown을 부착한다.
 * - focus 시 빈 입력이면 인기 항목(fallback) 표시
 * - 입력 시 items 배열에서 includes 매칭 결과 최대 8개 표시
 * - 키 ArrowDown/ArrowUp으로 항목 이동, Enter로 선택, Escape로 닫기
 * - mousedown으로 항목 선택 (blur 보다 먼저 발화)
 *
 * @param {Object} opts
 * @param {HTMLInputElement} opts.input         검색 input
 * @param {HTMLElement} opts.dropdown           dropdown 컨테이너 (hidden 클래스 토글)
 * @param {() => Promise<Array<{npayKorNm: string, mdivCdNm?: string}>>} opts.loadItems
 * @param {(item) => void} opts.onSelect        항목 선택 시 콜백 (item 객체 전달)
 * @param {string[]} [opts.fallbackKeywords]    빈 input focus 시 보여줄 인기 키워드
 * @param {string[]} [opts.popularKeywords]     scoreMatch에서 가산점(-0.5) 주는 인기 항목명. 기본 fallbackKeywords와 동일.
 */
const attachSuggestionInput = ({ input, dropdown, loadItems, onSelect, fallbackKeywords = [], popularKeywords }) => {
    const popular = popularKeywords ?? fallbackKeywords ?? [];
    if (!input || !dropdown) return;
    let activeIdx = -1;
    let rows = [];

    const close = () => {
        dropdown.classList.add('hidden');
        activeIdx = -1;
    };

    const renderRows = (rs) => {
        rows = rs;
        if (!rs.length) {
            dropdown.classList.add('hidden');
            return;
        }
        dropdown.innerHTML = rs.map((it, i) => {
            const name = escapeHtml(it.npayKorNm ?? '');
            const sub = escapeHtml(it.mdivCdNm ?? '');
            return `
                <button type="button" data-idx="${i}"
                        class="suggestion-row w-full text-left px-3.5 py-2.5 hover:bg-gray-50 flex items-center justify-between gap-3 ${i === activeIdx ? 'bg-gray-50' : ''}">
                    <span class="text-sm text-gray-800 truncate">${name}</span>
                    ${sub ? `<span class="text-[11px] text-gray-400 flex-shrink-0">${sub}</span>` : ''}
                </button>`;
        }).join('');
        // fixed 위치 동적 계산 — input 아래 정렬
        const rect = input.getBoundingClientRect();
        dropdown.style.top  = (rect.bottom + 4) + 'px';
        dropdown.style.left = rect.left + 'px';
        dropdown.style.width = rect.width + 'px';
        dropdown.classList.remove('hidden');
        dropdown.querySelectorAll('.suggestion-row').forEach(btn => {
            // mousedown으로 처리 — input의 blur보다 먼저 발화해 dropdown이 닫히기 전 선택을 보장.
            btn.addEventListener('mousedown', (e) => {
                e.preventDefault();
                const idx = Number(btn.getAttribute('data-idx'));
                if (Number.isInteger(idx) && rows[idx]) {
                    onSelect?.(rows[idx]);
                    close();
                }
            });
        });
    };

    const matchAndRender = async () => {
        const items = await loadItems();
        const kw = input.value.trim().toLowerCase();
        if (!kw) {
            // 빈 입력 + focus 상태면 fallback 키워드를 npayKorNm 형태로 노출.
            if (fallbackKeywords.length) {
                renderRows(fallbackKeywords.map(k => ({ npayKorNm: k, mdivCdNm: '인기' })));
            } else {
                close();
            }
            return;
        }
        // exact/prefix/word-boundary/includes + 인기 가산을 적용한 우선순위 정렬.
        // broad keyword("MRI")에서 exact match가 뒤로 밀리는 회귀를 방지한다.
        const matched = items
            .map(it => ({ it, s: scoreMatch(it.npayKorNm ?? '', it.mdivCdNm ?? '', kw, popular) }))
            .filter(x => Number.isFinite(x.s))
            .sort((a, b) => a.s - b.s || (a.it.npayKorNm ?? '').localeCompare(b.it.npayKorNm ?? ''))
            .slice(0, 8)
            .map(x => x.it);
        renderRows(matched);
    };

    input.addEventListener('focus', matchAndRender);
    input.addEventListener('input', matchAndRender);
    input.addEventListener('keydown', (e) => {
        if (dropdown.classList.contains('hidden')) return;
        if (e.key === 'ArrowDown') {
            e.preventDefault();
            activeIdx = Math.min(rows.length - 1, activeIdx + 1);
            renderRows(rows);
        } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            activeIdx = Math.max(0, activeIdx - 1);
            renderRows(rows);
        } else if (e.key === 'Enter' && activeIdx >= 0 && rows[activeIdx]) {
            e.preventDefault();
            onSelect?.(rows[activeIdx]);
            close();
        } else if (e.key === 'Escape') {
            close();
        }
    });
    input.addEventListener('blur', () => {
        // 마우스 클릭 처리 시간을 잠시 준다.
        setTimeout(close, 120);
    });
};
