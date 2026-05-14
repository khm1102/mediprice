/**
 * 병원 목록 및 상세 데이터 처리
 */

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

const fetchItemsCache = async () => {
    if (_itemsCache) return _itemsCache;
    const data = await api.get('/api/items');
    if (!data.success) return [];
    // NonPayItemGroupDto[] → Item[] { npayCd, npayKorNm } 평탄화
    _itemsCache = data.data.flatMap(group => group.items ?? []);
    return _itemsCache;
};

// keyword → npayCd 변환 (이름에 keyword가 포함되는 첫 번째 항목)
const resolveNpayCd = async (keyword) => {
    const items = await fetchItemsCache();
    const lowerKw = keyword.trim().toLowerCase();
    return items.find(item => item.npayKorNm.toLowerCase().includes(lowerKw))?.npayCd ?? null;
};

// ── 상태 표시 ─────────────────────────────────────────────────────────────────

const showState = (id) => {
    ['state-loading', 'state-empty', 'state-error', 'state-content'].forEach(s => {
        const el = document.getElementById(s);
        if (el) el.classList.toggle('hidden', s !== id);
    });
};

// ── 병원 목록 ─────────────────────────────────────────────────────────────────

const renderHospitalCard = (hospital, isCheapest = false) => {
    const shadowStyle = isCheapest
        ? 'box-shadow: 0 0 0 2px #2563EB, 0 4px 16px rgba(37,99,235,0.18);'
        : 'box-shadow: 0 2px 10px rgba(0,0,0,0.09);';

    const cheapestBanner = isCheapest ? `
        <div class="flex items-center gap-1.5 bg-[#2563EB] px-4 py-1.5 rounded-t-2xl">
            <svg class="w-3 h-3 text-white flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z"/>
            </svg>
            <p class="text-[11px] text-white font-semibold tracking-tight">이 지역에서 가장 저렴해요</p>
        </div>` : '';

    const priceText = hospital.curAmt != null
        ? isCheapest
            ? `<p class="text-sm text-[#2563EB] font-bold mt-2">${formatPrice(hospital.curAmt)}</p>`
            : `<p class="text-xs text-[#2563EB] font-semibold mt-1.5">${formatPrice(hospital.curAmt)}</p>`
        : '';

    const cardRadius = isCheapest ? 'rounded-b-2xl' : 'rounded-2xl';
    const detailUrl = `/hospitals/0?ykiho=${encodeURIComponent(hospital.ykiho)}&dist=${hospital.distance ?? ''}`;

    return `
        <a href="${detailUrl}"
           class="block hover:opacity-90 transition-all"
           style="${shadowStyle}; border-radius: 1rem;">
            ${cheapestBanner}
            <div class="bg-white ${cardRadius} p-4 min-h-[76px]">
                <div class="flex items-start justify-between gap-3">
                    <div class="flex-1 min-w-0">
                        <p class="font-semibold text-gray-900 text-sm truncate">${hospital.yadmNm ?? ''}</p>
                        <p class="text-xs text-gray-400 mt-0.5">${hospital.clCdNm ?? ''}</p>
                        <p class="text-xs text-gray-400 mt-1 truncate">${hospital.addr ?? ''}</p>
                        ${priceText}
                    </div>
                    <span class="text-xs text-gray-400 font-medium flex-shrink-0 mt-0.5">
                        ${formatDistance(hospital.distance)}
                    </span>
                </div>
            </div>
        </a>`;
};

const fetchHospitals = async (keyword) => {
    showState('state-loading');

    if (!keyword?.trim()) {
        showState('state-empty');
        return;
    }

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
        const params = new URLSearchParams({ lat, lng, npayCd, radius: 2000 });
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

        // 5. 카드 렌더링
        const list = document.getElementById('hospital-list');
        list.innerHTML = sorted.map((h, i) => renderHospitalCard(h, i === 0)).join('');
        showState(null);

        // 6. 지도 마커
        clearMarkers?.();
        sorted.forEach(h => {
            if (h.lat && h.lng) {
                addMarker?.(h.lat, h.lng, h.yadmNm, () => {
                    window.location.href = `/hospitals/0?ykiho=${encodeURIComponent(h.ykiho)}&dist=${h.distance ?? ''}`;
                });
            }
        });

    } catch {
        showState('state-error');
    }
};

// ── 병원 상세 ─────────────────────────────────────────────────────────────────

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

        // 거리: 목록 페이지에서 URL 쿼리 파라미터로 전달받음
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

        // 의사 수
        const drCountEl = document.getElementById('hospital-dr-count');
        if (drCountEl && h.drTotCnt != null) {
            drCountEl.textContent = h.drTotCnt + '명';
            drCountEl.closest('[data-field="dr-count"]')?.classList.remove('hidden');
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

        // ── 특수진단 ──
        const spclSection = document.getElementById('section-spcl');
        if (spclSection) {
            const list = h.spclDiagList ?? [];
            if (list.length) {
                spclSection.classList.remove('hidden');
                document.getElementById('spcl-list').innerHTML =
                    list.map(s => `<span class="inline-block bg-gray-100 text-gray-600 text-xs font-medium px-2.5 py-1 rounded-full">${s}</span>`).join('');
            }
        }

        // ── 교통/주차 ──
        const trnsprtSection = document.getElementById('section-trnsprt');
        if (trnsprtSection) {
            const t = h.trnsprtInfo;
            if (t && (t.parkYn || t.trafInfo || t.parkEtc)) {
                trnsprtSection.classList.remove('hidden');

                const parkEl = document.getElementById('trnsprt-park');
                if (parkEl && t.parkYn) {
                    const canPark = t.parkYn === 'Y';
                    let parkText = canPark ? '주차 가능' : '주차 불가';
                    if (canPark && t.parkQty) parkText += ` (${t.parkQty}대)`;
                    if (canPark && t.parkXpnsYn === 'Y') parkText += ' · 유료';
                    else if (canPark && t.parkXpnsYn === 'N') parkText += ' · 무료';
                    if (canPark && t.parkEtc) parkText += `\n${t.parkEtc}`;
                    parkEl.textContent = parkText;
                    parkEl.closest('[data-field="park"]')?.classList.remove('hidden');
                }

                const trafEl = document.getElementById('trnsprt-traf');
                if (trafEl && t.trafInfo) {
                    trafEl.textContent = t.trafInfo;
                    trafEl.closest('[data-field="traf"]')?.classList.remove('hidden');
                }
            }
        }

        // ── 비급여 가격 테이블 (PriceItem: npayCd, npayKorNm, curAmt) ──
        const prices = h.prices ?? [];
        if (!prices.length) {
            document.getElementById('price-empty').classList.remove('hidden');
        } else {
            const tbody = document.getElementById('price-tbody');
            tbody.innerHTML = prices.map(p => `
                <tr>
                    <td class="py-3 pr-4 text-gray-700 text-sm">${p.npayKorNm ?? ''}</td>
                    <td class="py-3 text-right font-semibold text-[#2563EB] text-sm">${formatPrice(p.curAmt)}</td>
                </tr>`).join('');
            document.getElementById('price-table').classList.remove('hidden');
        }

        showState('state-content');
    } catch {
        showState('state-error');
    }
};
