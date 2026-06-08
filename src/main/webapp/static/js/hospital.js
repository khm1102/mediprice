
// ── 현재 위치 취득 (모듈 캐시 + sessionStorage 5분 캐시) ──────────────────────
// 옛 구현은 timeout 8초 + 모듈 변수만 사용해서 페이지 이동마다 다시 8초씩 기다렸다.
// sessionStorage로 5분 캐시를 두면 검색 페이지 ↔ 상세 페이지 왕복에서 즉시 좌표 반환.

const GEO_FALLBACK = { lat: 37.5665, lng: 126.9780 }; // 서울 시청
const GEO_SESSION_KEY = 'mp.geo';
const GEO_SESSION_MAX_AGE_MS = 5 * 60 * 1000;
// 정확도가 이 값을 넘으면 (네트워크 기반·IP 기반 추정 등) 사용자에게 알림.
// GPS는 보통 5~50m, Wi-Fi/셀룰러는 100~수천m. 1500m면 결과가 동네 단위로 흔들린다.
const LOW_ACCURACY_THRESHOLD_M = 1500;

let _geoCache = null;

// 보안: 민감 좌표(lat/lng)는 sessionStorage 평문 저장 금지 (CodeQL js/clear-text-storage).
// 모듈 메모리 캐시 _geoCache + 브라우저의 navigator.geolocation maximumAge: 60000이 같은 탭 내
// 단기 캐시 역할을 하므로, 페이지 reload 시에만 한 번 더 위치를 받는다. sessionStorage에는
// 비민감 메타(fallback 여부, 정확도, 타임스탬프)만 둬서 fallback 배너 토글 정합성만 맞춘다.
const _readGeoFromSession = () => {
    try {
        const raw = sessionStorage.getItem(GEO_SESSION_KEY);
        if (!raw) return null;
        const parsed = JSON.parse(raw);
        if (!parsed?.ts) return null;
        if (Date.now() - parsed.ts > GEO_SESSION_MAX_AGE_MS) return null;
        // 좌표를 저장하지 않으므로 _geoCache 우회 복원은 불가 — 항상 null 반환해 호출처가 재취득하게 한다.
        return null;
    } catch {
        return null;
    }
};

const _writeGeoToSession = (lat, lng, fromFallback, accuracy) => {
    try {
        // 좌표는 의도적으로 저장하지 않음. 비민감 메타(fallback/accuracy/ts)만 저장.
        sessionStorage.setItem(GEO_SESSION_KEY,
                JSON.stringify({ fromFallback: !!fromFallback, accuracy, ts: Date.now() }));
    } catch {
        // sessionStorage 미지원(SafariPrivate 등) — 무시.
    }
};

const _useFallback = () => ({ ...GEO_FALLBACK, fromFallback: true, accuracy: null });

/**
 * 현재 위치 취득. 옛 호출처가 {lat,lng}만 사용해도 동작하도록 추가 키만 얹는다.
 * 반환: { lat, lng, fromFallback, accuracy }
 * - fromFallback=true: geolocation 미지원/거부/timeout → 서울 시청 기본값. 호출처가 알림 배너로 안내한다.
 * - accuracy: Geolocation API가 반환한 신뢰 반경(m). null이면 미지원 또는 fallback.
 */
const getCurrentPosition = () => {
    if (_geoCache) return Promise.resolve(_geoCache);
    const fromSession = _readGeoFromSession();
    if (fromSession) {
        _geoCache = fromSession;
        return Promise.resolve(_geoCache);
    }
    return new Promise((resolve) => {
        if (!navigator.geolocation) {
            _geoCache = _useFallback();
            resolve(_geoCache);
            return;
        }
        navigator.geolocation.getCurrentPosition(
            (pos) => {
                _geoCache = {
                    lat: pos.coords.latitude,
                    lng: pos.coords.longitude,
                    fromFallback: false,
                    accuracy: typeof pos.coords.accuracy === 'number' ? pos.coords.accuracy : null,
                };
                _writeGeoToSession(_geoCache.lat, _geoCache.lng, false, _geoCache.accuracy);
                resolve(_geoCache);
            },
            () => {
                _geoCache = _useFallback();
                _writeGeoToSession(_geoCache.lat, _geoCache.lng, true, null);
                resolve(_geoCache);
            },
            // timeout 8000 → 4000(빠른 fallback). maximumAge 60s — 브라우저 자체 캐시 활용.
            { timeout: 4000, maximumAge: 60000 }
        );
    });
};

/**
 * 검색바 아래 인라인 배너 토글. fromFallback이거나 accuracy가 임계값 초과면 노출.
 * hospitals.jsp의 #geo-fallback-notice가 있어야 동작 — favorites.jsp 등 다른 페이지에선 무동작.
 * 두 케이스를 메시지로 구분해 사용자가 정확도 문제임을 알 수 있게 한다.
 */
const notifyGeoFallback = (geo) => {
    const banner = document.getElementById('geo-fallback-notice');
    if (!banner) return;
    const message = banner.querySelector('[data-geo-message]');
    const lowAccuracy = !geo?.fromFallback
            && typeof geo?.accuracy === 'number'
            && geo.accuracy > LOW_ACCURACY_THRESHOLD_M;
    const visible = !!geo?.fromFallback || lowAccuracy;
    banner.classList.toggle('hidden', !visible);
    if (!visible || !message) return;
    if (geo.fromFallback) {
        message.innerHTML = '현재 위치를 가져오지 못해 <strong>서울 시청</strong> 기준으로 검색 중이에요. '
                + '지도를 옮겨 재검색하거나 위치 권한을 허용해 다시 시도해보세요.';
    } else {
        const m = Math.round(geo.accuracy);
        message.innerHTML = `현재 위치 정확도가 낮아요 (<strong>±${m}m</strong>). `
                + '실내·지하·기기 차이 영향일 수 있어요. 권한을 허용한 뒤 다시 시도하면 더 정확해집니다.';
    }
};

/**
 * "현재 위치 다시 시도" 버튼 핸들러 — 캐시를 비우고 같은 키워드로 재검색.
 * 반복 거부 시 또 fallback이 떨어지지만 사용자가 의도적으로 클릭한 시점에는
 * 브라우저가 권한 프롬프트를 다시 보여줄 수 있다.
 */
const retryGeoLocation = () => {
    _geoCache = null;
    try { sessionStorage.removeItem(GEO_SESSION_KEY); } catch {}
    const banner = document.getElementById('geo-fallback-notice');
    banner?.classList.add('hidden');
    const kw = document.getElementById('search-input')?.value?.trim();
    if (kw) fetchHospitals(kw);
};

// ── 비급여 항목 목록 취득 (캐시) ──────────────────────────────────────────────

let _itemsCache = null;
let _npayCdToGroupCache = null; // npayCd → groupName 매핑

const fetchItemsCache = async () => {
    if (_itemsCache) return _itemsCache;
    const data = await api.get('/api/items');
    if (!data.success) return [];
    const groups = data.data ?? [];
    // npayCd → groupName 매핑 빌드
    _npayCdToGroupCache = {};
    groups.forEach(group => {
        (group.items ?? []).forEach(item => {
            _npayCdToGroupCache[item.npayCd] = group.groupName ?? '기타';
        });
    });
    // 평탄화 시 mdivCdNm(중분류명)을 각 item에 부여 — 자동완성 드롭다운 보조 표기/빈 결과 추천에 사용.
    _itemsCache = groups.flatMap(group => (group.items ?? []).map(item => ({
        ...item,
        mdivCdNm: group.groupName ?? '기타',
    })));
    return _itemsCache;
};

// index.jsp의 추천 카드와 동일한 8개 인기 키워드. 검색바 아래 인기 칩과 빈 결과 fallback 추천에 공통 사용.
const POPULAR_KEYWORDS = [
    '도수치료', 'MRI', '체외충격파', '초음파',
    '주사치료', '수면내시경', '임플란트', '추나요법',
];

/**
 * 빈 결과 추천 칩 렌더링.
 * 입력 키워드가 어느 항목에 매칭되면 그 항목의 중분류(mdivCdNm) 안에서 다른 5개를 추천.
 * 매칭이 없으면 POPULAR_KEYWORDS 8개로 fallback.
 */
const renderEmptyStateChips = async (keyword) => {
    const container = document.getElementById('state-empty-chips');
    if (!container) return;
    const items = await fetchItemsCache();
    const kw = (keyword ?? '').trim().toLowerCase();
    let suggestions = [];

    if (kw) {
        const matched = items.find(it => (it.npayKorNm ?? '').toLowerCase().includes(kw));
        if (matched?.mdivCdNm) {
            suggestions = items
                .filter(it => it.mdivCdNm === matched.mdivCdNm
                    && !(it.npayKorNm ?? '').toLowerCase().includes(kw))
                .slice(0, 5)
                .map(it => it.npayKorNm);
        }
    }
    if (!suggestions.length) {
        suggestions = POPULAR_KEYWORDS;
    }
    // 칩 클릭은 페이지 리로드(handleSearch와 동일 흐름) — keyword query param 갱신으로 검색 가격 카드도 새로 동작.
    renderQuickChips(container, suggestions, (kw2) => {
        window.location.href = '/hospitals?keyword=' + encodeURIComponent(kw2);
    });
};

/**
 * 데스크톱: 카드 hover → 지도 마커 강조. 모바일/터치 디바이스(`(hover: none)`)는 무동작.
 * 위임 방식(컨테이너에 한 번만 부착)으로 추가/제거되는 카드에도 안전.
 */
// 상세 패널이 열려있는 병원 ykiho — 이 병원의 핀은 mouseout에서도 highlight 유지
let _selectedMarkerYkiho = null;

const setSelectedMarker = (ykiho) => {
    _selectedMarkerYkiho = ykiho;
    if (ykiho) highlightMarker?.(ykiho);
};

const clearSelectedMarker = () => {
    _selectedMarkerYkiho = null;
    clearMarkerHighlight?.();
};

const attachCardHoverHighlight = (listEl) => {
    if (!listEl) return;
    const canHover = window.matchMedia('(hover: hover)').matches;
    if (!canHover) return;
    if (listEl.dataset.hoverBound === '1') return;
    listEl.dataset.hoverBound = '1';

    listEl.addEventListener('mouseover', (e) => {
        const card = e.target.closest?.('.hospital-card[data-ykiho]');
        if (!card) return;
        const ykiho = card.getAttribute('data-ykiho');
        if (ykiho) highlightMarker?.(ykiho);
    });
    listEl.addEventListener('mouseout', (e) => {
        const card = e.target.closest?.('.hospital-card[data-ykiho]');
        const to = e.relatedTarget?.closest?.('.hospital-card[data-ykiho]');
        if (card && card === to) return;
        // 선택된(패널 열린) 병원은 mouseout에서도 highlight 유지
        if (_selectedMarkerYkiho) {
            highlightMarker?.(_selectedMarkerYkiho);
        } else {
            clearMarkerHighlight?.();
        }
    });
};

// 데스크톱(≥1024px) 판정 — 모바일 segmented control 동작은 이 폭 미만에서만 의미가 있다.
const _isDesktopViewport = () => window.matchMedia?.('(min-width: 1024px)').matches ?? false;

/**
 * 모바일 segmented control: 'list' | 'map' 전환.
 * - 모바일에서 panel-list와 map-area는 같은 grid 영역에서 absolute로 겹쳐 있다.
 * - 'map' 전환 시 Naver Map이 새 컨테이너 크기를 인식하게 refreshMapSize 호출.
 * - 데스크톱(≥1024px)에서는 list/map이 grid-area로 분리되므로 본 함수는 no-op.
 *   (옛 구현은 pane-hidden 클래스를 그대로 토글해 데스크톱에서도 한쪽이 숨겨지는 회귀가 있었다.)
 */
const togglePane = (pane) => {
    if (_isDesktopViewport()) return;
    const list = document.getElementById('panel-list');
    const map = document.querySelector('.map-area');
    if (!list || !map) return;

    if (pane === 'map') {
        list.classList.add('pane-hidden');
        map.classList.remove('pane-hidden');
        // 컨테이너 크기 변경 직후 trigger.
        requestAnimationFrame(() => refreshMapSize?.());
    } else {
        list.classList.remove('pane-hidden');
        map.classList.add('pane-hidden');
    }

    document.querySelectorAll('#mobile-pane-tabs .pane-tab').forEach(btn => {
        const isActive = btn.getAttribute('data-pane') === pane;
        btn.setAttribute('aria-pressed', isActive ? 'true' : 'false');
        if (isActive) {
            btn.classList.remove('bg-gray-100', 'text-gray-500', 'hover:bg-gray-200');
            btn.classList.add('bg-[#2563EB]', 'text-white');
        } else {
            btn.classList.add('bg-gray-100', 'text-gray-500', 'hover:bg-gray-200');
            btn.classList.remove('bg-[#2563EB]', 'text-white');
        }
    });
};

/**
 * 검색바 영역 초기화: 인기 칩 렌더 + 자동완성 dropdown 부착.
 * DOMContentLoaded 후 1회 호출.
 */
const initSearchUx = async () => {
    const input = document.getElementById('search-input');
    const dropdown = document.getElementById('search-suggestions');
    const chipsBar = document.getElementById('quick-chips');

    if (chipsBar) {
        renderQuickChips(chipsBar, POPULAR_KEYWORDS, (kw) => {
            window.location.href = '/hospitals?keyword=' + encodeURIComponent(kw);
        });
    }

    if (input && dropdown) {
        attachSuggestionInput({
            input,
            dropdown,
            loadItems: fetchItemsCache,
            onSelect: (it) => {
                const kw = it.npayKorNm ?? '';
                if (kw) window.location.href = '/hospitals?keyword=' + encodeURIComponent(kw);
            },
            fallbackKeywords: POPULAR_KEYWORDS,
        });
    }

    renderWeightSlider();
};

// npayCd → 그룹명 반환 (캐시 미준비 시 빈 문자열)
const resolveGroupName = (npayCd) => _npayCdToGroupCache?.[npayCd] ?? '';

// keyword → npayCd 변환 (이름에 keyword가 포함되는 첫 번째 항목)
const resolveNpayCd = async (keyword) => {
    const items = await fetchItemsCache();
    const lowerKw = keyword.trim().toLowerCase();
    return items.find(item => item.npayKorNm.toLowerCase().includes(lowerKw))?.npayCd ?? null;
};

/**
 * keyword → 매칭되는 npayCd 상위 10개 (scoreMatch 우선순위).
 * - exact > prefix > word-boundary prefix > includes(매칭 위치 앞일수록 우선)
 * - POPULAR_KEYWORDS에 포함된 항목명은 같은 단계 내에서 가산점.
 * - broad keyword("MRI") 등에서 매칭이 30개 초과면 console.warn (사용자에게 차단 X — 카드 라벨이 보조).
 */
const resolveNpayCds = async (keyword) => {
    const items = await fetchItemsCache();
    const lowerKw = keyword.trim().toLowerCase();
    const scored = items
        .map(it => ({ it, s: scoreMatch(it.npayKorNm ?? '', it.mdivCdNm ?? '', lowerKw, POPULAR_KEYWORDS) }))
        .filter(x => Number.isFinite(x.s));
    if (scored.length > 30) {
        console.warn(`[resolveNpayCds] "${keyword}" 매칭이 ${scored.length}개 — broad keyword.`);
    }
    return scored
        .sort((a, b) => a.s - b.s || (a.it.npayKorNm ?? '').localeCompare(b.it.npayKorNm ?? ''))
        .slice(0, 10)
        .map(x => x.it.npayCd);
};

// 진행 중인 검색을 취소하기 위한 AbortController. 빠른 키워드 변경 시 옛 응답이 새 결과를 덮어쓰지 않게 한다.
let _searchAbort = null;

const _startSearchAbort = () => {
    _searchAbort?.abort();
    _searchAbort = new AbortController();
    return _searchAbort.signal;
};

const _isAbort = (err) => err?.name === 'AbortError';

// 정렬 모드 ('mixed' | 'price' | 'distance'). localStorage에 사용자 선택 영구 저장.
const SORT_STORAGE_KEY = 'mp.sort';
const SORT_MODES = ['mixed', 'price', 'distance'];
const SORT_LABELS = { mixed: '추천', price: '가격순', distance: '가까운 순' };
let _currentSort = (() => {
    try { return SORT_MODES.includes(localStorage.getItem(SORT_STORAGE_KEY))
        ? localStorage.getItem(SORT_STORAGE_KEY) : 'mixed'; }
    catch { return 'mixed'; }
})();

const _setCurrentSort = (mode) => {
    if (!SORT_MODES.includes(mode)) return;
    _currentSort = mode;
    try { localStorage.setItem(SORT_STORAGE_KEY, mode); } catch {}
};

// mixed 모드 가중치 슬라이더 — wPrice(0~1), wDistance는 1-wPrice로 자동 도출.
// 백엔드 v2 함수가 결과 집합 내 MAX OVER로 가격을 정규화하므로 합이 정확히 1일 필요는 없지만,
// UI는 0~1 단일 슬라이더로 단순화.
const WEIGHT_STORAGE_KEY = 'mp.wPrice';
const WEIGHT_DEFAULT = 0.7;
let _currentWPrice = (() => {
    try {
        const raw = parseFloat(localStorage.getItem(WEIGHT_STORAGE_KEY));
        if (Number.isFinite(raw) && raw >= 0 && raw <= 1) return raw;
    } catch {}
    return WEIGHT_DEFAULT;
})();

const _setCurrentWPrice = (v) => {
    const clamped = Math.max(0, Math.min(1, Number(v) || 0));
    _currentWPrice = clamped;
    try { localStorage.setItem(WEIGHT_STORAGE_KEY, String(clamped)); } catch {}
};

let _weightDebounceTimer = null;
const _scheduleWeightRefetch = () => {
    if (_weightDebounceTimer) clearTimeout(_weightDebounceTimer);
    _weightDebounceTimer = setTimeout(() => {
        const kw = document.getElementById('search-input')?.value?.trim();
        if (kw) fetchHospitals(kw);
    }, 300);
};

/**
 * mixed 모드에서만 #weight-slider 컨테이너에 슬라이더 마크업을 주입한다.
 * 다른 모드면 컨테이너를 비우고 hidden. 입력(input) 이벤트는 300ms debounce 후 refetch.
 */
const renderWeightSlider = () => {
    const container = document.getElementById('weight-slider');
    if (!container) return;
    // 항상 mixed 모드로 슬라이더만 표시
    _setCurrentSort('mixed');
    const pct = Math.round(_currentWPrice * 100);
    const isMobile = window.innerWidth < 1024;
    container.innerHTML = `
        <div class="${isMobile ? 'py-1' : 'py-0'}">
            <div class="flex items-center gap-2">
                <span class="text-[10px] text-gray-400 whitespace-nowrap slider-label">거리</span>
                <input type="range" min="0" max="1" step="0.05"
                       value="${_currentWPrice}"
                       data-weight-slider
                       aria-label="정렬 기준 (0=거리 우선, 1=가격 우선)"
                       class="flex-1 h-1 accent-[#2563EB]" />
                <span class="text-[10px] text-gray-400 whitespace-nowrap slider-label">가격</span>
            </div>
            <div class="flex justify-center mt-1">
                <span data-weight-readout class="text-[10px] text-[#2563EB] font-semibold tabular-nums slider-label">
                    ${pct < 30 ? '거리 우선' : pct > 70 ? '가격 우선' : '거리 · 가격 혼합'}
                </span>
            </div>
        </div>`;
    const slider = container.querySelector('[data-weight-slider]');
    const readout = container.querySelector('[data-weight-readout]');
    slider?.addEventListener('input', (e) => {
        const v = parseFloat(e.target.value);
        _setCurrentWPrice(v);
        if (readout) {
            readout.textContent = v < 0.3 ? '거리 우선' : v > 0.7 ? '가격 우선' : '거리 · 가격 혼합';
        }
        _scheduleWeightRefetch();
    });
};

/**
 * 검색 결과 정렬 모드 segmented control 렌더 + 클릭 핸들러 부착.
 * 클릭 시 현재 키워드로 fetchHospitals 재호출.
 */
const renderSortTabs = () => {
    const container = document.getElementById('sort-tabs');
    if (!container) return;
    container.innerHTML = SORT_MODES.map(mode => {
        const active = mode === _currentSort;
        const cls = active
            ? 'bg-[#2563EB] text-white'
            : 'bg-gray-100 text-gray-500 hover:bg-gray-200';
        return `<button type="button" data-sort="${escapeHtml(mode)}"
                aria-pressed="${active ? 'true' : 'false'}"
                class="sort-tab flex-1 h-9 rounded-lg text-xs font-semibold ${cls} transition-colors">
            ${escapeHtml(SORT_LABELS[mode])}
        </button>`;
    }).join('');
    container.querySelectorAll('.sort-tab').forEach(btn => {
        btn.addEventListener('click', () => {
            const mode = btn.getAttribute('data-sort');
            if (!mode || mode === _currentSort) return;
            _setCurrentSort(mode);
            renderSortTabs();
            const kw = document.getElementById('search-input')?.value?.trim();
            if (kw) fetchHospitals(kw);
        });
    });
    // 정렬 모드 변경 시 슬라이더 표시 여부도 같이 갱신.
    renderWeightSlider();
};

/**
 * 단일 백엔드 호출 — /api/hospitals/search.
 * 옛 코드는 npayCds 길이만큼 /api/hospitals를 병렬 호출하고 프론트에서 ykiho별 최저가 병합했다.
 * 이제 백엔드 v2 프로시저가 DISTINCT ON으로 한 번에 처리하므로 단일 fetch면 충분하다.
 */
const searchHospitalsByNpayCds = async (npayCds, lat, lng, signal) => {
    if (!npayCds.length) return [];
    const params = new URLSearchParams({
        lat: String(lat),
        lng: String(lng),
        npayCds: npayCds.join(','),
        sort: _currentSort,
    });
    // mixed 모드에서만 사용자 가중치를 전송 — price/distance 모드는 백엔드에서 가중치 무시.
    if (_currentSort === 'mixed') {
        params.set('wPrice', String(_currentWPrice));
        params.set('wDistance', (1 - _currentWPrice).toFixed(2));
    }
    const res = await api.get('/api/hospitals/search?' + params.toString(), { signal });
    return res?.success ? (res.data ?? []) : [];
};

const searchHospitalsByAssistant = async (query, lat, lng, signal) => {
    const res = await api.post('/api/hospitals/assistant-search', {
        query,
        lat,
        lng,
        sort: _currentSort
    }, { signal });
    return res?.success ? res.data : null;
};

// ── 상태 표시 ─────────────────────────────────────────────────────────────────

const showState = (id) => {
    ['state-loading', 'state-prompt', 'state-empty', 'state-error', 'state-content'].forEach(s => {
        const el = document.getElementById(s);
        if (el) el.classList.toggle('hidden', s !== id);
    });
    // hospitals 페이지: 카드 목록은 id가 null일 때(결과 표시)만 보임
    const hospitalList = document.getElementById('hospital-list');
    if (hospitalList) hospitalList.classList.toggle('hidden', id !== null);
};

// ── 병원 목록 ─────────────────────────────────────────────────────────────────

/**
 * 카드 컨테이너(#hospital-list)에 한 번만 위임 click 리스너를 등록한다.
 * <ul>
 *   <li>.fav-btn 클릭 → handleFavoriteClick (즐겨찾기 토글, 카드 진입 차단)</li>
 *   <li>.hospital-card 클릭 → showHospitalInPanel (상세 패널)</li>
 * </ul>
 * keyword/ykiho/좌표를 inline JS string에 박아 넣던 옛 방식의 XSS 가능성을 차단한다.
 * dataset.boundClicks 플래그로 같은 list에 중복 등록되는 것을 막는다.
 */
const _bindHospitalListClicks = (list) => {
    if (!list || list.dataset.boundClicks === 'true') return;
    list.dataset.boundClicks = 'true';
    list.addEventListener('click', (event) => {
        const favBtn = event.target.closest('.fav-btn');
        if (favBtn) {
            event.stopPropagation();
            handleFavoriteClick(favBtn.dataset.ykiho ?? '', favBtn, event);
            return;
        }
        const card = event.target.closest('.hospital-card');
        if (!card) return;
        const ykiho = card.dataset.ykiho ?? '';
        if (!ykiho) return;
        const distance = parseFloat(card.dataset.distance) || 0;
        const lat = parseFloat(card.dataset.lat) || 0;
        const lng = parseFloat(card.dataset.lng) || 0;
        const kw = new URLSearchParams(location.search).get('keyword') ?? '';
        showHospitalInPanel(ykiho, distance, encodeURIComponent(kw), lat, lng);
    });
};

const renderHospitalCard = (hospital) => {
    const lat = hospital.lat ?? 0;
    const lng = hospital.lng ?? 0;
    // ykiho/거리/좌표는 data-* 속성으로 직렬화하고 클릭 처리는 위임 리스너로 한다.
    // 옛 inline onclick="..." 문자열 조립 방식은 keyword/ykiho에 ', (, ) 가 섞이면 onclick 속성
    // 안의 JS가 깨지거나 XSS로 이어질 수 있었다 (encodeURIComponent도 따옴표는 escape하지 않는다).
    const ykihoAttr = escapeHtml(hospital.ykiho ?? '');

    // 가격 우선 — 가격이 있으면 우상단에 text-xl로 강조, 없으면 '가격 미신고' 회색 라벨.
    const priceBlock = hospital.curAmt != null
        ? `<p class="text-xl font-bold text-[#2563EB] leading-none whitespace-nowrap">${escapeHtml(formatPrice(hospital.curAmt))}</p>`
        : `<p class="text-xs text-gray-400 leading-none whitespace-nowrap">가격 미신고</p>`;

    // broad keyword("MRI", "초음파")에서 어떤 항목 가격인지 사용자가 즉시 알 수 있게 가격 위에 작은 라벨.
    // 옛 v1 응답(matchedNpayKorNm 없음)에선 자연스럽게 라벨이 사라진다.
    const matchedLabel = hospital.matchedNpayKorNm
        ? `<p class="text-[10px] text-gray-400 font-medium mb-0.5 whitespace-nowrap">${escapeHtml(hospital.matchedNpayKorNm)} 기준</p>`
        : '';

    // 종별(clCd) × 항목(npayCd) 평균 대비 비율. 음수는 평균보다 싸다(emerald), 양수는 비싸다(rose).
    // NonPayItemClcdStat에 (npayCd, clcdKey) 통계가 없으면 백엔드가 null로 보내므로 라벨 자체가 사라진다.
    const statLabel = (hospital.avgAmt != null && hospital.diffPct != null)
        ? (() => {
            const pct = Math.round(Math.abs(hospital.diffPct));
            const sign = hospital.diffPct < 0 ? '-' : (hospital.diffPct > 0 ? '+' : '');
            const colorCls = hospital.diffPct < 0
                ? 'text-emerald-600'
                : (hospital.diffPct > 0 ? 'text-rose-600' : 'text-gray-400');
            return `<p class="text-[10px] ${colorCls} font-medium mt-0.5 whitespace-nowrap">종별 평균 대비 ${sign}${pct}%</p>`;
        })()
        : '';

    // 병원명 + 종별 (한 줄, 종별이 있으면 점으로 구분)
    const nameLine = (hospital.clCdNm ?? '').trim()
        ? `${escapeHtml(hospital.yadmNm ?? '')}<span class="text-xs text-gray-400 font-normal ml-1.5">· ${escapeHtml(hospital.clCdNm ?? '')}</span>`
        : escapeHtml(hospital.yadmNm ?? '');

    // 주소 + 거리 (한 줄)
    const addrLine = [escapeHtml(hospital.addr ?? ''), escapeHtml(formatDistance(hospital.distance))]
        .filter(Boolean).join(' · ');

    const typeBadge = (hospital.clCdNm ?? '').trim()
        ? `<span class="text-[10px] text-gray-400 font-medium flex-shrink-0 whitespace-nowrap">${escapeHtml(hospital.clCdNm)}</span>`
        : '';
    const distBadge = hospital.distance != null
        ? `<span class="text-[10px] text-gray-400 flex-shrink-0 whitespace-nowrap">${escapeHtml(formatDistance(hospital.distance))}</span>`
        : '';

    return `
        <div data-ykiho="${ykihoAttr}"
           data-distance="${hospital.distance ?? 0}"
           data-lat="${lat}"
           data-lng="${lng}"
           class="hospital-card block hover:opacity-95 transition-all cursor-pointer"
           style="box-shadow: 0 2px 10px rgba(0,0,0,0.09); border-radius: 1rem;">
            <div class="bg-white rounded-2xl px-4 py-3">
                <div class="flex items-start gap-2">
                    <button type="button"
                            data-ykiho="${ykihoAttr}"
                            data-favorited="false"
                            class="fav-btn flex-shrink-0 mt-0.5 p-0.5 rounded-lg transition-colors text-gray-300 hover:text-yellow-400 hover:bg-yellow-50"
                            title="즐겨찾기 추가">
                        <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 24 24">
                            <path d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0
                                     00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0
                                     00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1
                                     1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1
                                     1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0
                                     00.951-.69l1.519-4.674z"/>
                        </svg>
                    </button>
                    <div class="flex-1 min-w-0">
                        <div class="flex items-start justify-between gap-2">
                            <p class="font-bold text-gray-900 text-sm lg:text-base leading-tight">${escapeHtml(hospital.yadmNm ?? '')}</p>
                            <div class="flex-shrink-0 text-right ml-2">
                                ${priceBlock}
                                ${statLabel}
                            </div>
                        </div>
                        <div class="flex items-center gap-1.5 mt-0.5 min-w-0 flex-nowrap overflow-hidden">
                            ${typeBadge}
                            ${typeBadge && distBadge ? '<span class="text-[10px] text-gray-300 flex-shrink-0">·</span>' : ''}
                            ${distBadge}
                            ${hospital.addr ? `<span class="text-[10px] text-gray-300 flex-shrink-0">·</span><span class="text-[10px] text-gray-400 truncate">${escapeHtml(hospital.addr)}</span>` : ''}
                        </div>
                    </div>
                </div>
            </div>
        </div>`;
};

const renderAssistantSummary = () => {
    if (!_lastAssistantResult?.message) return '';
    const chips = (_lastAssistantResult.matchedItems ?? [])
        .slice(0, 3)
        .map(item => item.npayKorNm)
        .filter(Boolean)
        .map(name => `
            <span class="text-[10px] text-[#2563EB] bg-blue-50 rounded-full px-2 py-1 whitespace-nowrap">
                ${escapeHtml(name)}
            </span>`)
        .join('');
    const chipBlock = chips
        ? `<div class="flex flex-wrap gap-1.5 mt-2">${chips}</div>`
        : '';
    return `
        <div class="bg-white px-4 py-3"
             style="box-shadow: 0 2px 10px rgba(0,0,0,0.08); border-radius: 1rem;">
            <p class="text-xs font-semibold text-gray-700 leading-relaxed">
                ${escapeHtml(_lastAssistantResult.message)}
            </p>
            ${chipBlock}
        </div>`;
};

// ── 검색 결과 ykiho 맵 (상세 즉시 렌더링용) ──────────────────────────────────
let _hospitalMap = {};
let _lastAssistantResult = null;

// ── 테스트용 더미 데이터 ──────────────────────────────────────────────────────
const _DUMMY_HOSPITALS = [
    { ykiho: 'TEST001', yadmNm: '테스트병원 강남점', clCdNm: '의원', addr: '서울특별시 강남구 테헤란로 123', lat: 37.5012, lng: 127.0396, distance: 320, curAmt: 35000, matchedNpayKorNm: '이학요법료/도수치료', avgAmt: 60000, diffPct: -42 },
    { ykiho: 'TEST002', yadmNm: '메디프라이스재활의학과의원', clCdNm: '의원', addr: '서울특별시 서초구 서초대로 456', lat: 37.4969, lng: 127.0278, distance: 850, curAmt: 50000, matchedNpayKorNm: '이학요법료/도수치료', avgAmt: 60000, diffPct: -17 },
    { ykiho: 'TEST003', yadmNm: '서울정형외과병원', clCdNm: '병원', addr: '서울특별시 강남구 논현로 789', lat: 37.5100, lng: 127.0285, distance: 1200, curAmt: 80000, matchedNpayKorNm: '이학요법료/도수치료', avgAmt: 60000, diffPct: 33 },
    { ykiho: 'TEST004', yadmNm: '강남척추관절병원', clCdNm: '병원', addr: '서울특별시 강남구 역삼로 321', lat: 37.4980, lng: 127.0350, distance: 1800, curAmt: 45000, matchedNpayKorNm: '이학요법료/도수치료', avgAmt: 60000, diffPct: -25 },
    { ykiho: 'TEST005', yadmNm: '청담통증의학과의원', clCdNm: '의원', addr: '서울특별시 강남구 청담동 111', lat: 37.5220, lng: 127.0470, distance: 2500, curAmt: 60000, matchedNpayKorNm: '이학요법료/도수치료', avgAmt: 60000, diffPct: 0 },
];

const fetchHospitals = async (keyword) => {
    if (!keyword?.trim()) {
        _lastAssistantResult = null;
        showState('state-prompt');
        return;
    }

    // 테스트 모드
    if (keyword.trim() === '테스트') {
        hideReSearchBtn?.();
        _lastAssistantResult = null;
        renderHospitalResults(keyword, _DUMMY_HOSPITALS);
        return;
    }

    hideReSearchBtn?.();
    showState('state-loading');
    const signal = _startSearchAbort();

    try {
        // 1. 위치 취득. fallback이면 검색바 아래 안내 배너 표시.
        const geo = await getCurrentPosition();
        notifyGeoFallback(geo);
        const { lat, lng } = geo;

        // 2. 자연어 질문을 서버에서 비급여 항목 후보로 해석하고 기존 v2 병원 검색을 재사용.
        const assistantResult = await searchHospitalsByAssistant(keyword, lat, lng, signal);
        _lastAssistantResult = assistantResult;
        const results = assistantResult?.hospitals ?? [];

        if (!results.length) {
            await renderEmptyStateChips(keyword);
            showState('state-empty');
            return;
        }

        renderHospitalResults(keyword, results);

    } catch (err) {
        if (_isAbort(err)) return; // 이전 요청 abort — silent
        showState('state-error');
    }
};

const renderHospitalResults = (keyword, sorted) => {
        // ykiho → hospital 빠른 조회 맵 구성
        _hospitalMap = {};
        sorted.forEach(h => { _hospitalMap[h.ykiho] = h; });

        // 카드 렌더링
        const list = document.getElementById('hospital-list');
        list.innerHTML = renderAssistantSummary() + sorted.map(h => renderHospitalCard(h)).join('');
        _bindHospitalListClicks(list);
        showState(null);
        // 모바일에서만 자동으로 목록 탭을 활성화. 데스크톱은 list/map이 항상 함께 보이므로 무동작.
        if (!_isDesktopViewport()) togglePane?.('list');
        loadFavoriteStates();
        // 데스크톱: 카드 hover → 마커 강조. 터치 디바이스(hover: none)는 무동작.
        attachCardHoverHighlight(list);

        // 지도 마커 — 핀 클릭 시 왼쪽 목록에서 카드 강조
        const _kw = new URLSearchParams(location.search).get('keyword') ?? '';
        clearMarkers?.();
        sorted.forEach(h => {
            if (h.lat && h.lng) {
                addMarker?.(h.lat, h.lng, h.yadmNm, h.curAmt ?? null, false, h.ykiho, () => {
                    showHospitalInPanel(h.ykiho, h.distance ?? 0, encodeURIComponent(_kw), h.lat, h.lng);
                });
            }
        });
        initClustering?.();
};

// ── 핀 클릭 시 목록 카드 강조 + 스크롤 ─────────────────────────────────────────

let _highlightedYkiho = null;

const _defaultCardShadow = () => '0 2px 10px rgba(0,0,0,0.09)';

const clearHospitalHighlight = () => {
    if (!_highlightedYkiho) return;
    const prev = document.querySelector(`.hospital-card[data-ykiho="${_highlightedYkiho}"]`);
    if (prev) prev.style.boxShadow = _defaultCardShadow(prev);
    _highlightedYkiho = null;
};

const highlightHospitalCard = (ykiho) => {
    // 상세 패널이 열려있으면 목록으로 복귀
    showHospitalList();

    // 이전 강조 제거
    clearHospitalHighlight();

    const card = document.querySelector(`.hospital-card[data-ykiho="${ykiho}"]`);
    if (!card) return;

    _highlightedYkiho = ykiho;

    // 파란 링 강조
    card.style.boxShadow = '0 0 0 2.5px #2563EB, 0 4px 20px rgba(37,99,235,0.22)';

    // 스크롤 컨테이너 내에서 부드럽게 이동
    const listEl = document.getElementById('hospital-list');
    if (listEl) {
        const cardTop    = card.offsetTop;
        const cardHeight = card.offsetHeight;
        const contTop    = listEl.scrollTop;
        const contHeight = listEl.clientHeight;
        const isVisible  = cardTop >= contTop && (cardTop + cardHeight) <= (contTop + contHeight);
        if (!isVisible) {
            listEl.scrollTo({ top: cardTop - 12, behavior: 'smooth' });
        }
    }
};

// ── 패널 전환  ────────────────────────────────────────

const showHospitalList = () => {
    document.getElementById('panel-detail')?.classList.remove('open');
    document.getElementById('pd-backdrop')?.classList.remove('open');
    // 진행 중인 basics/extras 요청을 취소 — 패널이 닫혔는데 응답이 늦게 도착해 렌더하지 않도록.
    _detailAbort?.abort();
    _detailAbort = null;
    clearHospitalHighlight();
    clearSelectedMarker();
};

// ── 상세 패널 섹션 초기화  ────────────────────────────
const _resetDetailSections = () => {
    ['pd-section-search-price', 'pd-section-dgsbjt', 'pd-section-medoft', 'pd-section-operating', 'pd-section-parking'].forEach(id => {
        const el = document.getElementById(id);
        if (!el) return;
        el.classList.add('hidden');
        // details 태그면 닫힌 상태로 리셋해서 다음 병원 진입 시 펼침 상태가 새지 않게 한다.
        if (el.tagName === 'DETAILS') el.open = false;
    });
    document.getElementById('pd-price-empty')?.classList.add('hidden');
    document.getElementById('pd-price-table')?.classList.add('hidden');
    const tbody = document.getElementById('pd-price-tbody');
    if (tbody) tbody.innerHTML = '';
    document.querySelector('[data-field="pd-url"]')?.classList.add('hidden');
    document.getElementById('pd-price-loading')?.classList.remove('hidden');
};

/**
 * 검색 키워드와 매칭되는 가격 항목 1개를 골라 상단 "내 검색 항목 가격" 카드에 표시한다.
 * 매칭 없거나 keyword 비어있으면 카드 hidden 유지.
 * @param {string} sectionId    pd-section-search-price 또는 section-search-price
 * @param {string} nameId       항목명 노드 ID
 * @param {string} priceId      가격 노드 ID
 * @param {Array} prices        h.prices
 * @param {string} keyword
 */
const _renderSearchPriceCard = (sectionId, nameId, priceId, prices, keyword) => {
    const section = document.getElementById(sectionId);
    if (!section) return;
    const kw = (keyword ?? '').trim().toLowerCase();
    if (!kw || !prices?.length) return;
    const match = prices.find(p => (p.npayKorNm ?? '').toLowerCase().includes(kw));
    if (!match || match.curAmt == null) return;
    const nameEl = document.getElementById(nameId);
    const priceEl = document.getElementById(priceId);
    if (nameEl) nameEl.textContent = match.npayKorNm ?? '';
    if (priceEl) priceEl.textContent = formatPrice(match.curAmt);
    section.classList.remove('hidden');
};

// ── 운영/주차 정보 렌더 유틸 (상세 패널 + 풀스크린 페이지 공통) ──
const _yesNoLabel = (v) => v === 'Y' ? '운영' : v === 'N' ? '미운영' : null;

const _operatingItems = (info) => {
    if (!info) return [];
    const items = [];
    if (info.rcvWeek)   items.push({ label: '평일 접수', value: info.rcvWeek });
    if (info.rcvSat)    items.push({ label: '토요일 접수', value: info.rcvSat });
    if (info.lunchWeek) items.push({ label: '점심시간', value: info.lunchWeek });
    if (info.noTrmtSun) items.push({ label: '일요일', value: info.noTrmtSun });
    if (info.noTrmtHoli) items.push({ label: '공휴일', value: info.noTrmtHoli });
    const day = _yesNoLabel(info.emyDayYn);
    const night = _yesNoLabel(info.emyNgtYn);
    if (day)   items.push({ label: '낮 응급실', value: day });
    if (night) items.push({ label: '야간 응급실', value: night });
    return items;
};

const _parkingItems = (info) => {
    if (!info) return [];
    const items = [];
    if (info.parkQty)    items.push({ label: '주차 가능 대수', value: info.parkQty });
    if (info.parkXpnsYn) items.push({ label: '주차료', value: info.parkXpnsYn === 'Y' ? '유료' : '무료' });
    if (info.parkEtc)    items.push({ label: '기타', value: info.parkEtc });
    return items;
};

const _renderInfoSection = (sectionId, listId, items) => {
    const section = document.getElementById(sectionId);
    const list = document.getElementById(listId);
    if (!section || !list) return;
    if (!items.length) return;
    section.classList.remove('hidden');
    list.innerHTML = items.map(it => `
        <p class="text-sm text-gray-600 leading-relaxed">
            <span class="text-gray-400">${escapeHtml(it.label)}</span>
            <span class="ml-2">${escapeHtml(it.value)}</span>
        </p>`).join('');
};

// ── 네이버 지도 길찾기 URL 생성 ─────────────────────────────────────────────
const _buildNaverDirectionsUrl = (name, addr) => {
    // 주소로 검색, 없으면 병원 이름 폴백
    const query = addr?.trim() || name;
    return `https://map.naver.com/p/search/${encodeURIComponent(query)}`;
};

// ── 병원 기본 정보 즉시 렌더 ──────────────────────────
const _renderBasicInfo = (h, dist) => {
    document.getElementById('pd-name').textContent    = h.yadmNm ?? '';
    document.getElementById('pd-type').textContent    = h.clCdNm ?? '';
    document.getElementById('pd-address').textContent = h.addr ?? '';

    const distEl = document.getElementById('pd-distance');
    if (dist) {
        distEl.textContent = formatDistance(parseFloat(dist));
        distEl.classList.remove('hidden');
    } else {
        distEl.classList.add('hidden');
    }

    // 전화번호는 API 응답 전까지 비움
    const phoneEl = document.getElementById('pd-phone');
    phoneEl.textContent = '';
    phoneEl.removeAttribute('href');

    // 길찾기 URL
    const dirEl = document.getElementById('pd-directions');
    if (dirEl) {
        dirEl.href = _buildNaverDirectionsUrl(h.yadmNm ?? '', h.addr);
    }
};

// ── 상세 API 응답으로 나머지 정보 렌더 ─────────────────────────────────────
/**
 * basics 응답으로 렌더 — 전화/홈페이지/비급여 가격 카드 + 가격 표.
 * basics는 DB only이므로 외부 호출 지연 없이 즉시(수~수십 ms) 도착.
 */
const _renderBasicsSection = async (h, kw) => {
    // 전화번호
    const phoneEl = document.getElementById('pd-phone');
    if (h.telNo) {
        phoneEl.textContent = h.telNo;
        phoneEl.href = 'tel:' + h.telNo;
    } else {
        phoneEl.textContent = '전화번호 정보 없음';
        phoneEl.removeAttribute('href');
    }

    // 홈페이지
    const urlEl = document.getElementById('pd-url');
    if (urlEl && h.hospUrl) {
        urlEl.href = h.hospUrl.startsWith('http') ? h.hospUrl : 'https://' + h.hospUrl;
        urlEl.textContent = h.hospUrl;
        urlEl.closest('[data-field="pd-url"]')?.classList.remove('hidden');
    }

    // 비급여 가격 테이블 + 검색 항목 가격 카드 (상단)
    document.getElementById('pd-price-loading')?.classList.add('hidden');
    const prices = h.prices ?? [];
    _renderSearchPriceCard('pd-section-search-price', 'pd-search-item-name', 'pd-search-item-price', prices, kw);
    if (!prices.length) {
        document.getElementById('pd-price-empty').classList.remove('hidden');
        return;
    }

    await fetchItemsCache();
    const searchKw = kw.toLowerCase().trim();
    const groups = [];
    const groupIndex = {};
    prices.forEach(p => {
        const gName = (p.npayKorNm ?? '').split('/')[0].trim() || '기타';
        if (groupIndex[gName] === undefined) {
            groupIndex[gName] = groups.length;
            groups.push({ name: gName, items: [], matched: false });
        }
        groups[groupIndex[gName]].items.push(p);
    });
    if (searchKw) {
        groups.forEach(g => {
            g.matched = g.name.toLowerCase().includes(searchKw)
                || g.items.some(p => (p.npayKorNm ?? '').toLowerCase().includes(searchKw));
        });
        groups.sort((a, b) => (b.matched ? 1 : 0) - (a.matched ? 1 : 0));
    }
    const rows = [];
    groups.forEach((group, gi) => {
        if (groups.length > 1) {
            if (gi > 0) rows.push(`<tr><td colspan="2" class="pt-2 pb-0"><div class="border-t border-gray-200"></div></td></tr>`);
            const hStyle = group.matched ? 'color:#2563EB;font-size:11px;font-weight:700;' : 'color:#9CA3AF;font-size:11px;font-weight:600;';
            const badge  = group.matched ? `<span style="margin-left:5px;background:#EFF6FF;color:#2563EB;font-size:10px;font-weight:600;padding:1px 6px;border-radius:10px;">검색 항목</span>` : '';
            rows.push(`<tr><td colspan="2" class="pt-3 pb-1.5"><span style="${hStyle}">${escapeHtml(group.name)}</span>${badge}</td></tr>`);
        }
        group.items.forEach(p => {
            const segs = (p.npayKorNm ?? '').split('/');
            const disp = groups.length > 1 ? (segs.slice(1).join(' / ').trim() || segs[0]) : p.npayKorNm ?? '';
            rows.push(`<tr>
                <td class="py-2.5 pr-3 text-sm leading-snug" style="word-break:break-all;color:${group.matched?'#111827':'#6B7280'};">${escapeHtml(disp)}</td>
                <td class="py-2.5 text-right text-sm" style="font-weight:${group.matched?'700':'500'};color:${group.matched?'#2563EB':'#9CA3AF'};white-space:nowrap;">${escapeHtml(formatPrice(p.curAmt))}</td>
            </tr>`);
        });
    });
    document.getElementById('pd-price-tbody').innerHTML = rows.join('');
    document.getElementById('pd-price-table').classList.remove('hidden');
};

/**
 * extras 응답으로 렌더 — 진료과목/의료장비/운영/주차 (HIRA 5종, slow).
 * 캐시 hit이면 거의 즉시, miss이면 500~2000ms 후 도착.
 */
const _renderExtrasSection = (e) => {
    if (!e) return;
    // 진료과목
    const dgSect = document.getElementById('pd-section-dgsbjt');
    if (dgSect) {
        const list = e.dgsbjtList ?? [];
        if (list.length) {
            dgSect.classList.remove('hidden');
            document.getElementById('pd-dgsbjt-list').innerHTML =
                list.map(d => `<span class="inline-block bg-blue-50 text-[#2563EB] text-xs font-medium px-2.5 py-1 rounded-full">${escapeHtml(d)}</span>`).join('');
        }
    }
    // 의료장비
    const moSect = document.getElementById('pd-section-medoft');
    if (moSect) {
        const list = e.medOftList ?? [];
        if (list.length) {
            moSect.classList.remove('hidden');
            document.getElementById('pd-medoft-list').innerHTML =
                list.map(t => `<p class="text-sm text-gray-600 leading-relaxed">${escapeHtml(t)}</p>`).join('');
        }
    }
    _renderInfoSection('pd-section-operating', 'pd-operating-list', _operatingItems(e.operatingInfo));
    _renderInfoSection('pd-section-parking', 'pd-parking-list', _parkingItems(e.parkingInfo));
};

// 진행 중인 상세 fetch 취소용 — 다른 ykiho를 빠르게 누르거나 패널을 닫을 때 사용.
let _detailAbort = null;

const showHospitalInPanel = async (ykiho, dist, keyword, lat, lng) => {
    const pd = document.getElementById('panel-detail');
    if (!pd) return;

    pd.classList.add('open');
    document.getElementById('pd-backdrop')?.classList.add('open');
    clearHospitalHighlight();
    setSelectedMarker(ykiho);

    if (lat && lng) focusMapOnHospital?.(parseFloat(lat), parseFloat(lng));

    const pdLoading = document.getElementById('pd-loading');
    const pdError   = document.getElementById('pd-error');
    const pdContent = document.getElementById('pd-content');

    _resetDetailSections();

    // 즐겨찾기 버튼 ykiho 설정 및 상태 로드
    const pdFavBtn = document.getElementById('pd-fav-btn');
    if (pdFavBtn) {
        pdFavBtn.dataset.ykiho = ykiho;
        updateFavBtn(pdFavBtn, false);
    }
    loadFavoriteStates();

    const kw     = keyword ? decodeURIComponent(keyword) : '';
    const cached = _hospitalMap[ykiho];

    if (cached) {
        _renderBasicInfo(cached, dist);
        pdLoading.classList.add('hidden');
        pdError.classList.add('hidden');
        pdContent.classList.remove('hidden');
        pd.scrollTop = 0;
    } else {
        pdLoading.classList.remove('hidden');
        pdError.classList.add('hidden');
        pdContent.classList.add('hidden');
    }

    // 옛 상세 요청 취소 후 새 컨트롤러 발급. basics와 extras를 같은 signal로 묶는다.
    _detailAbort?.abort();
    _detailAbort = new AbortController();
    const signal = _detailAbort.signal;

    // basics(fast) — 가격 카드/표를 즉시 렌더. extras는 별도 promise로 병렬 진행.
    const basicsPromise = api.get('/api/hospitals/' + encodeURIComponent(ykiho) + '/basics', { signal })
        .then(async (data) => {
            if (!data?.success || !data?.data) throw new Error('no data');
            const b = data.data;
            if (!cached) {
                _renderBasicInfo(b, dist);
                pdLoading.classList.add('hidden');
                pdContent.classList.remove('hidden');
                pd.scrollTop = 0;
            }
            await _renderBasicsSection(b, kw);
        })
        .catch((err) => {
            if (_isAbort(err)) return;
            document.getElementById('pd-price-loading')?.classList.add('hidden');
            if (pdContent.classList.contains('hidden')) {
                pdLoading.classList.add('hidden');
                pdError.classList.remove('hidden');
            } else {
                document.getElementById('pd-price-empty')?.classList.remove('hidden');
            }
        });

    // extras(slow) — 도착하면 부가 섹션만 렌더. 실패해도 가격 카드는 이미 표시되어 있음.
    const extrasPromise = api.get('/api/hospitals/' + encodeURIComponent(ykiho) + '/extras', { signal })
        .then((data) => {
            if (data?.success && data?.data) _renderExtrasSection(data.data);
        })
        .catch((err) => {
            if (_isAbort(err)) return;
            // 부가 섹션은 hidden 유지 — 사용자에게 별도 에러 노출 안 함.
        });

    await Promise.allSettled([basicsPromise, extrasPromise]);
};

// ── 즐겨찾기 버튼 ───────────────────────────────────────────────────────────────

const updateFavBtn = (btnEl, isFav) => {
    btnEl.dataset.favorited = isFav ? 'true' : 'false';
    btnEl.title = isFav ? '즐겨찾기 해제' : '즐겨찾기 추가';
    if (isFav) {
        btnEl.classList.add('text-yellow-400');
        btnEl.classList.remove('text-gray-300', 'hover:text-yellow-400', 'hover:bg-yellow-50');
        btnEl.classList.add('hover:text-yellow-500', 'hover:bg-yellow-50');
    } else {
        btnEl.classList.remove('text-yellow-400', 'hover:text-yellow-500');
        btnEl.classList.add('text-gray-300', 'hover:text-yellow-400', 'hover:bg-yellow-50');
    }

    // 카드 전체 강조
    const card = btnEl.closest('.hospital-card');
    if (!card) return;

    card.style.boxShadow = '0 2px 10px rgba(0,0,0,0.09)';
};

const handleFavoriteClick = async (ykiho, btnEl, event) => {
    event.stopPropagation();

    // HttpOnly mp_token + /api/auth/me 캐시 패턴 — authReady가 끝나기 전 클릭 시
    // 로그인 상태인데도 미로그인으로 오인하지 않도록 대기.
    await authReady;

    // 비로그인 → 구글 로그인으로
    if (!isLoggedIn()) {
        window.location.href = '/auth/oauth2/authorize/google';
        return;
    }

    const isFav = btnEl.dataset.favorited === 'true';

    // 낙관적 UI 업데이트 (즉시 반영)
    updateFavBtn(btnEl, !isFav);
    btnEl.disabled = true;

    try {
        if (isFav) {
            await api.delete('/api/favorites/' + encodeURIComponent(ykiho));
            _favoritesYkihoSet?.delete(ykiho);
        } else {
            await api.post('/api/favorites', { ykiho });
            (_favoritesYkihoSet ??= new Set()).add(ykiho);
        }
    } catch {
        // 실패 시 원상복구
        updateFavBtn(btnEl, isFav);
    } finally {
        btnEl.disabled = false;
    }
};

// 즐겨찾기 ykiho 캐시 — 페이지 라이프타임 동안 1회만 fetch. 토글 시 set 갱신.
let _favoritesYkihoSet = null;

const _invalidateFavoritesCache = () => { _favoritesYkihoSet = null; };

const loadFavoriteStates = async () => {
    await authReady;
    if (!isLoggedIn()) {
        _favoritesYkihoSet = null;
        return;
    }

    if (!_favoritesYkihoSet) {
        try {
            const data = await api.get('/api/favorites');
            if (!data?.success) return;
            _favoritesYkihoSet = new Set((data.data ?? []).map(f => f.ykiho));
        } catch {
            // 즐겨찾기 상태 로드 실패는 무시 (UI에 영향 없음)
            return;
        }
    }

    document.querySelectorAll('.fav-btn').forEach(btn => {
        const ykiho = btn.dataset.ykiho;
        if (ykiho) {
            updateFavBtn(btn, _favoritesYkihoSet.has(ykiho));
        }
    });
};

// ── 지도 중심 기준 재검색 ───────────────────────────────────────────────────────

const fetchHospitalsByLocation = async (lat, lng, keyword) => {
    if (!keyword?.trim()) return;

    hideReSearchBtn?.();
    showState('state-loading');
    const signal = _startSearchAbort();

    try {
        const assistantResult = await searchHospitalsByAssistant(keyword, lat, lng, signal);
        _lastAssistantResult = assistantResult;
        const results = assistantResult?.hospitals ?? [];

        if (!results.length) {
            await renderEmptyStateChips(keyword);
            showState('state-empty');
            return;
        }

        renderHospitalResults(keyword, results);

    } catch (err) {
        if (_isAbort(err)) return;
        showState('state-error');
    }
};

// ── 병원 상세 ─────────────────────────

/**
 * 풀스크린 상세 페이지 (hospital-detail.jsp) 렌더.
 * Progressive rendering — basics(fast: DB only)를 먼저 받아 가격 카드/표 표시,
 * extras(slow: HIRA 5종)는 도착하면 진료과목·장비·운영·주차에 합친다.
 */
const fetchHospitalDetail = async (ykiho) => {
    showState('state-loading');

    if (!ykiho) {
        showState('state-error');
        return;
    }

    _detailAbort?.abort();
    _detailAbort = new AbortController();
    const signal = _detailAbort.signal;

    const searchKwRaw = (new URLSearchParams(location.search).get('keyword') ?? '').trim();

    const renderBasics = async (b) => {
        // ── 기본 정보 ──
        document.getElementById('hospital-name').textContent    = b.yadmNm ?? '';
        document.getElementById('hospital-type').textContent    = b.clCdNm ?? '';
        document.getElementById('hospital-address').textContent = b.addr ?? '';

        // 거리
        const distEl = document.getElementById('hospital-distance');
        if (distEl) {
            const distParam = new URLSearchParams(location.search).get('dist');
            if (distParam) {
                distEl.textContent = formatDistance(parseFloat(distParam));
            } else {
                distEl.classList.add('hidden');
            }
        }

        // 전화번호
        const phoneEl = document.getElementById('hospital-phone');
        if (b.telNo) {
            phoneEl.textContent = b.telNo;
            phoneEl.href = 'tel:' + b.telNo;
        } else {
            phoneEl.textContent = '전화번호 정보 없음';
            phoneEl.removeAttribute('href');
        }

        // 길찾기
        const dirEl = document.getElementById('hospital-directions');
        if (dirEl) {
            dirEl.href = _buildNaverDirectionsUrl(b.yadmNm ?? '', b.addr);
        }

        // 홈페이지
        const urlEl = document.getElementById('hospital-url');
        if (urlEl && b.hospUrl) {
            urlEl.href = b.hospUrl.startsWith('http') ? b.hospUrl : 'https://' + b.hospUrl;
            urlEl.textContent = b.hospUrl;
            urlEl.closest('[data-field="hosp-url"]')?.classList.remove('hidden');
        }

        // 즐겨찾기 버튼 초기화
        const detailFavBtn = document.getElementById('detail-fav-btn');
        if (detailFavBtn) detailFavBtn.dataset.ykiho = ykiho;
        loadFavoriteStates();

        showState('state-content');

        // ── 비급여 가격 테이블 + 검색 항목 가격 카드 ──
        const prices = b.prices ?? [];
        _renderSearchPriceCard('section-search-price', 'search-item-name', 'search-item-price', prices, searchKwRaw);
        if (!prices.length) {
            document.getElementById('price-empty').classList.remove('hidden');
        } else {
            // 아이템 캐시가 준비된 상태일 때만 그룹 활용
            await fetchItemsCache();

            // 검색 키워드
            const searchKw = searchKwRaw.toLowerCase();

            // 항목명의 첫 번째 '/' 앞 텍스트를 그룹 키로 사용
            const groups = [];
            const groupIndex = {};
            prices.forEach(p => {
                const gName = (p.npayKorNm ?? '').split('/')[0].trim() || '기타';
                if (groupIndex[gName] === undefined) {
                    groupIndex[gName] = groups.length;
                    groups.push({ name: gName, items: [], matched: false });
                }
                groups[groupIndex[gName]].items.push(p);
            });

            // 검색어와 일치하는 그룹을 맨 앞으로
            // 그룹명뿐 아니라 그룹 내 항목명 전체에서 키워드 검색
            if (searchKw) {
                groups.forEach(g => {
                    g.matched = g.name.toLowerCase().includes(searchKw)
                        || g.items.some(p => (p.npayKorNm ?? '').toLowerCase().includes(searchKw));
                });
                groups.sort((a, b) => (b.matched ? 1 : 0) - (a.matched ? 1 : 0));
            }

            const tbody = document.getElementById('price-tbody');
            const rows = [];
            groups.forEach((group, gi) => {
                if (groups.length > 1) {
                    if (gi > 0) {
                        rows.push(`<tr><td colspan="2" class="pt-2 pb-0"><div class="border-t border-gray-200"></div></td></tr>`);
                    }
                    // 검색어 매칭 그룹은 파란색 강조 헤더
                    const headerStyle = group.matched
                        ? `color:#2563EB; font-size:11px; font-weight:700;`
                        : `color:#9CA3AF; font-size:11px; font-weight:600;`;
                    const matchBadge = group.matched
                        ? `<span style="margin-left:5px; background:#EFF6FF; color:#2563EB;
                            font-size:10px; font-weight:600; padding:1px 6px; border-radius:10px;">검색 항목</span>`
                        : '';
                    rows.push(`
                        <tr>
                            <td colspan="2" class="pt-3 pb-1.5">
                                <span style="${headerStyle}">${escapeHtml(group.name)}</span>${matchBadge}
                            </td>
                        </tr>`);
                }
                group.items.forEach(p => {
                    const segments = (p.npayKorNm ?? '').split('/');
                    const displayName = groups.length > 1
                        ? segments.slice(1).join(' / ').trim() || segments[0]
                        : p.npayKorNm ?? '';
                    rows.push(`
                        <tr>
                            <td class="py-2.5 pr-3 text-sm leading-snug"
                                style="word-break:break-all; color:${group.matched ? '#111827' : '#6B7280'};">
                                ${escapeHtml(displayName)}
                            </td>
                            <td class="py-2.5 text-right text-sm"
                                style="white-space:nowrap; font-weight:${group.matched ? '700' : '500'};
                                       color:${group.matched ? '#2563EB' : '#9CA3AF'};">
                                ${escapeHtml(formatPrice(p.curAmt))}
                            </td>
                        </tr>`);
                });
            });
            tbody.innerHTML = rows.join('');
            document.getElementById('price-table').classList.remove('hidden');
        }
    };

    const renderExtras = (e) => {
        if (!e) return;
        // ── 진료과목 ──
        const dgsbjtSection = document.getElementById('section-dgsbjt');
        if (dgsbjtSection) {
            const list = e.dgsbjtList ?? [];
            if (list.length) {
                dgsbjtSection.classList.remove('hidden');
                document.getElementById('dgsbjt-list').innerHTML =
                    list.map(d => `<span class="inline-block bg-blue-50 text-[#2563EB] text-xs font-medium px-2.5 py-1 rounded-full">${escapeHtml(d)}</span>`).join('');
            }
        }
        // ── 의료장비 ──
        const medOftSection = document.getElementById('section-medoft');
        if (medOftSection) {
            const list = e.medOftList ?? [];
            if (list.length) {
                medOftSection.classList.remove('hidden');
                document.getElementById('medoft-list').innerHTML =
                    list.map(t => `<p class="text-sm text-gray-600 leading-relaxed">${escapeHtml(t)}</p>`).join('');
            }
        }
        _renderInfoSection('section-operating', 'operating-list', _operatingItems(e.operatingInfo));
        _renderInfoSection('section-parking', 'parking-list', _parkingItems(e.parkingInfo));
    };

    // basics(fast) + extras(slow) 병렬 호출.
    const basicsPromise = api.get('/api/hospitals/' + encodeURIComponent(ykiho) + '/basics', { signal })
        .then(async (data) => {
            if (!data?.success || !data?.data) throw new Error('no data');
            await renderBasics(data.data);
        })
        .catch((err) => {
            if (_isAbort(err)) return;
            showState('state-error');
        });

    const extrasPromise = api.get('/api/hospitals/' + encodeURIComponent(ykiho) + '/extras', { signal })
        .then((data) => {
            if (data?.success && data?.data) renderExtras(data.data);
        })
        .catch((err) => {
            if (_isAbort(err)) return;
            // 부가 섹션 실패는 별도 에러 노출 없이 hidden 유지.
        });

    await Promise.allSettled([basicsPromise, extrasPromise]);
};
