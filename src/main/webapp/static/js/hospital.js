
// ── 현재 위치 취득 (캐시 포함) ────────────────────────────────────────────────

let _geoCache = null;

const getCurrentPosition = () => {
    if (_geoCache) return Promise.resolve(_geoCache);
    return new Promise((resolve) => {
        if (!navigator.geolocation) {
            _geoCache = { lat: 37.5665, lng: 126.9780 };
            resolve(_geoCache);
            return;
        }
        navigator.geolocation.getCurrentPosition(
            (pos) => {
                _geoCache = { lat: pos.coords.latitude, lng: pos.coords.longitude };
                resolve(_geoCache);
            },
            () => {
                _geoCache = { lat: 37.5665, lng: 126.9780 }; // 서울 기본값
                resolve(_geoCache);
            },
            { timeout: 8000 }
        );
    });
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
    // NonPayItemGroupDto[] → Item[] { npayCd, npayKorNm } 평탄화
    _itemsCache = groups.flatMap(group => group.items ?? []);
    return _itemsCache;
};

// npayCd → 그룹명 반환 (캐시 미준비 시 빈 문자열)
const resolveGroupName = (npayCd) => _npayCdToGroupCache?.[npayCd] ?? '';

// keyword → npayCd 변환 (이름에 keyword가 포함되는 첫 번째 항목)
const resolveNpayCd = async (keyword) => {
    const items = await fetchItemsCache();
    const lowerKw = keyword.trim().toLowerCase();
    return items.find(item => item.npayKorNm.toLowerCase().includes(lowerKw))?.npayCd ?? null;
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

const renderHospitalCard = (hospital) => {
    const priceText = hospital.curAmt != null
        ? `<p class="text-xs text-[#2563EB] font-semibold mt-1.5">${formatPrice(hospital.curAmt)}</p>`
        : '';

    const _kw = new URLSearchParams(location.search).get('keyword') ?? '';
    const lat = hospital.lat ?? 0;
    const lng = hospital.lng ?? 0;
    const onclick = `showHospitalInPanel('${hospital.ykiho}', ${hospital.distance ?? 0}, '${encodeURIComponent(_kw)}', ${lat}, ${lng})`;
    const ykihoEsc = hospital.ykiho.replace(/'/g, "\\'");

    return `
        <div onclick="${onclick}"
           data-ykiho="${hospital.ykiho}"
           class="hospital-card block hover:opacity-90 transition-all cursor-pointer"
           style="box-shadow: 0 2px 10px rgba(0,0,0,0.09); border-radius: 1rem;">
            <div class="bg-white rounded-2xl p-4 min-h-[76px]">
                <div class="flex items-start justify-between gap-3">
                    <div class="flex-1 min-w-0">
                        <p class="font-semibold text-gray-900 text-sm truncate">${hospital.yadmNm ?? ''}</p>
                        <p class="text-xs text-gray-400 mt-0.5">${hospital.clCdNm ?? ''}</p>
                        <p class="text-xs text-gray-400 mt-1 truncate">${hospital.addr ?? ''}</p>
                        ${priceText}
                    </div>
                    <div class="flex items-center gap-0.5 flex-shrink-0">
                        <span class="text-xs text-gray-400 font-medium">${formatDistance(hospital.distance)}</span>
                        <button onclick="handleFavoriteClick('${ykihoEsc}', this, event)"
                                data-ykiho="${hospital.ykiho}"
                                data-favorited="false"
                                class="fav-btn p-1 rounded-lg transition-colors text-gray-300 hover:text-yellow-400 hover:bg-yellow-50"
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
                    </div>
                </div>
            </div>
        </div>`;
};

// ── 검색 결과 ykiho 맵 (상세 즉시 렌더링용) ──────────────────────────────────
let _hospitalMap = {};

const fetchHospitals = async (keyword) => {
    if (!keyword?.trim()) {
        showState('state-prompt');
        return;
    }

    showState('state-loading');

    try {
        // 1. 위치 취득
        const { lat, lng } = await getCurrentPosition();

        // 2. keyword → npayCd 변환
        const npayCd = await resolveNpayCd(keyword);
        if (!npayCd) {
            showState('state-empty');
            return;
        }

        // 3. 병원 검색 API
        const params = new URLSearchParams({ lat, lng, npayCd });
        const data = await api.get('/api/hospitals?' + params.toString());

        if (!data.success) {
            showState('state-error');
            return;
        }
        if (!data.data?.length) {
            showState('state-empty');
            return;
        }

        // 4. 가격 오름차순 정렬 (curAmt 기준, null은 마지막)
        const sorted = [...data.data].sort((a, b) =>
            (a.curAmt ?? Infinity) - (b.curAmt ?? Infinity)
        );

        renderHospitalResults(keyword, sorted);

    } catch {
        showState('state-error');
    }
};

const renderHospitalResults = (keyword, sorted) => {
        // ykiho → hospital 빠른 조회 맵 구성
        _hospitalMap = {};
        sorted.forEach(h => { _hospitalMap[h.ykiho] = h; });

        // 카드 렌더링
        const list = document.getElementById('hospital-list');
        list.innerHTML = sorted.map(h => renderHospitalCard(h)).join('');
        showState(null);
        loadFavoriteStates();

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
    clearHospitalHighlight();
    clearMarkerHighlight?.();
};

// ── 상세 패널 섹션 초기화  ────────────────────────────
const _resetDetailSections = () => {
    ['pd-section-dgsbjt', 'pd-section-medoft'].forEach(id => {
        document.getElementById(id)?.classList.add('hidden');
    });
    document.getElementById('pd-price-empty')?.classList.add('hidden');
    document.getElementById('pd-price-table')?.classList.add('hidden');
    const tbody = document.getElementById('pd-price-tbody');
    if (tbody) tbody.innerHTML = '';
    document.querySelector('[data-field="pd-url"]')?.classList.add('hidden');
    document.getElementById('pd-price-loading')?.classList.remove('hidden');
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
const _renderFullDetails = async (h, kw) => {
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

    // 진료과목
    const dgSect = document.getElementById('pd-section-dgsbjt');
    if (dgSect) {
        const list = h.dgsbjtList ?? [];
        if (list.length) {
            dgSect.classList.remove('hidden');
            document.getElementById('pd-dgsbjt-list').innerHTML =
                list.map(d => `<span class="inline-block bg-blue-50 text-[#2563EB] text-xs font-medium px-2.5 py-1 rounded-full">${d}</span>`).join('');
        }
    }

    // 진료시간
    const moSect = document.getElementById('pd-section-medoft');
    if (moSect) {
        const list = h.medOftList ?? [];
        if (list.length) {
            moSect.classList.remove('hidden');
            document.getElementById('pd-medoft-list').innerHTML =
                list.map(t => `<p class="text-sm text-gray-600 leading-relaxed">${t}</p>`).join('');
        }
    }

    // 비급여 가격 테이블
    document.getElementById('pd-price-loading')?.classList.add('hidden');
    const prices = h.prices ?? [];
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
            rows.push(`<tr><td colspan="2" class="pt-3 pb-1.5"><span style="${hStyle}">${group.name}</span>${badge}</td></tr>`);
        }
        group.items.forEach(p => {
            const segs = (p.npayKorNm ?? '').split('/');
            const disp = groups.length > 1 ? (segs.slice(1).join(' / ').trim() || segs[0]) : p.npayKorNm ?? '';
            rows.push(`<tr>
                <td class="py-2.5 pr-3 text-sm leading-snug" style="word-break:keep-all;color:${group.matched?'#111827':'#6B7280'};">${disp}</td>
                <td class="py-2.5 text-right text-sm whitespace-nowrap w-px" style="font-weight:${group.matched?'700':'500'};color:${group.matched?'#2563EB':'#9CA3AF'};">${formatPrice(p.curAmt)}</td>
            </tr>`);
        });
    });
    document.getElementById('pd-price-tbody').innerHTML = rows.join('');
    document.getElementById('pd-price-table').classList.remove('hidden');
};

const showHospitalInPanel = async (ykiho, dist, keyword, lat, lng) => {
    const pd = document.getElementById('panel-detail');
    if (!pd) return;

    pd.classList.add('open');
    document.getElementById('pd-backdrop')?.classList.add('open');
    clearHospitalHighlight();
    highlightMarker?.(ykiho);

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

    try {
        const data = await api.get('/api/hospitals/' + encodeURIComponent(ykiho));
        if (!data.success || !data.data) throw new Error('no data');
        const h = data.data;

        if (!cached) {
            _renderBasicInfo(h, dist);
            pdLoading.classList.add('hidden');
            pdContent.classList.remove('hidden');
            pd.scrollTop = 0;
        }
        await _renderFullDetails(h, kw);

    } catch {
        document.getElementById('pd-price-loading')?.classList.add('hidden');
        if (pdContent.classList.contains('hidden')) {
            pdLoading.classList.add('hidden');
            pdError.classList.remove('hidden');
        } else {
            document.getElementById('pd-price-empty')?.classList.remove('hidden');
        }
    }
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
        } else {
            await api.post('/api/favorites', { ykiho });
        }
    } catch {
        // 실패 시 원상복구
        updateFavBtn(btnEl, isFav);
    } finally {
        btnEl.disabled = false;
    }
};

const loadFavoriteStates = async () => {
    if (!isLoggedIn()) return;

    try {
        const data = await api.get('/api/favorites');
        if (!data.success) return;

        const favoriteYkihos = new Set((data.data ?? []).map(f => f.ykiho));

        document.querySelectorAll('.fav-btn').forEach(btn => {
            const ykiho = btn.dataset.ykiho;
            if (ykiho) {
                updateFavBtn(btn, favoriteYkihos.has(ykiho));
            }
        });
    } catch {
        // 즐겨찾기 상태 로드 실패는 무시 (UI에 영향 없음)
    }
};

// ── 병원 상세 ─────────────────────────

const fetchHospitalDetail = async (ykiho) => {
    showState('state-loading');

    if (!ykiho) {
        showState('state-error');
        return;
    }

    try {
        const data = await api.get('/api/hospitals/' + encodeURIComponent(ykiho));
        if (!data.success || !data.data) {
            showState('state-error');
            return;
        }

        const h = data.data;

        // ── 기본 정보 ──
        document.getElementById('hospital-name').textContent    = h.yadmNm ?? '';
        document.getElementById('hospital-type').textContent    = h.clCdNm ?? '';
        document.getElementById('hospital-address').textContent = h.addr ?? '';

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
        if (h.telNo) {
            phoneEl.textContent = h.telNo;
            phoneEl.href = 'tel:' + h.telNo;
        } else {
            phoneEl.textContent = '전화번호 정보 없음';
            phoneEl.removeAttribute('href');
        }

        // 길찾기
        const dirEl = document.getElementById('hospital-directions');
        if (dirEl) {
            dirEl.href = _buildNaverDirectionsUrl(h.yadmNm ?? '', h.addr);
        }

        // 홈페이지
        const urlEl = document.getElementById('hospital-url');
        if (urlEl && h.hospUrl) {
            urlEl.href = h.hospUrl.startsWith('http') ? h.hospUrl : 'https://' + h.hospUrl;
            urlEl.textContent = h.hospUrl;
            urlEl.closest('[data-field="hosp-url"]')?.classList.remove('hidden');
        }

        // ── 진료과목 ──
        const dgsbjtSection = document.getElementById('section-dgsbjt');
        if (dgsbjtSection) {
            const list = h.dgsbjtList ?? [];
            if (list.length) {
                dgsbjtSection.classList.remove('hidden');
                document.getElementById('dgsbjt-list').innerHTML =
                    list.map(d => `<span class="inline-block bg-blue-50 text-[#2563EB] text-xs font-medium px-2.5 py-1 rounded-full">${d}</span>`).join('');
            }
        }

        // ── 진료시간 ──
        const medOftSection = document.getElementById('section-medoft');
        if (medOftSection) {
            const list = h.medOftList ?? [];
            if (list.length) {
                medOftSection.classList.remove('hidden');
                document.getElementById('medoft-list').innerHTML =
                    list.map(t => `<p class="text-sm text-gray-600 leading-relaxed">${t}</p>`).join('');
            }
        }

        // ── 비급여 가격 테이블 ──
        const prices = h.prices ?? [];
        if (!prices.length) {
            document.getElementById('price-empty').classList.remove('hidden');
        } else {
            // 아이템 캐시가 준비된 상태일 때만 그룹 활용
            await fetchItemsCache();

            // 검색 키워드
            const searchKw = (new URLSearchParams(location.search).get('keyword') ?? '').toLowerCase().trim();

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
                                <span style="${headerStyle}">${group.name}</span>${matchBadge}
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
                                style="word-break:keep-all; color:${group.matched ? '#111827' : '#6B7280'};">
                                ${displayName}
                            </td>
                            <td class="py-2.5 text-right text-sm whitespace-nowrap w-px"
                                style="font-weight:${group.matched ? '700' : '500'};
                                       color:${group.matched ? '#2563EB' : '#9CA3AF'};">
                                ${formatPrice(p.curAmt)}
                            </td>
                        </tr>`);
                });
            });
            tbody.innerHTML = rows.join('');
            document.getElementById('price-table').classList.remove('hidden');
        }

        // 즐겨찾기 버튼 초기화
        const detailFavBtn = document.getElementById('detail-fav-btn');
        if (detailFavBtn) detailFavBtn.dataset.ykiho = ykiho;
        loadFavoriteStates();

        showState('state-content');
    } catch {
        showState('state-error');
    }
};
