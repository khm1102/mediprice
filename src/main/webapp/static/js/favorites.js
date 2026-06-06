
const loadFavorites = async () => {
    // mp_token이 HttpOnly라 JS는 토큰을 못 읽는다. authReady가 /api/auth/me 응답을 받기 전에 호출하면
    // 로그인 상태인데도 isLoggedIn()이 false로 떨어진다 → 반드시 대기.
    await authReady;
    if (!isLoggedIn()) {
        document.getElementById('loading').classList.add('hidden');
        document.getElementById('not-logged-in').classList.remove('hidden');
        return;
    }

    try {
        const data = await api.get('/api/favorites');
        document.getElementById('loading').classList.add('hidden');

        if (!data.success || !data.data || data.data.length === 0) {
            document.getElementById('empty-state').classList.remove('hidden');
            return;
        }

        const favorites = data.data;

        // 카운트 업데이트
        const countEl = document.getElementById('favorite-count');
        countEl.textContent = `총 ${favorites.length}개`;
        countEl.classList.remove('hidden');

        // 카드 렌더링
        renderFavorites(favorites);

        // _hospitalMap에 기본 정보 미리 채워두기
        favorites.forEach(f => {
            _hospitalMap[f.ykiho] = {
                yadmNm: f.hospitalName,
                addr:   f.address,
                clCdNm: f.clCdNm,
                lat:    f.lat ?? 0,
                lng:    f.lng ?? 0,
            };
        });

        // 지도 마커 추가
        addFavoriteMarkers(favorites);

        // 즐겨찾기 상태 로드
        loadFavoriteStates();

    } catch {
        document.getElementById('loading').classList.add('hidden');
        document.getElementById('empty-state').classList.remove('hidden');
    }
};

const renderFavorites = (favorites) => {
    const listEl = document.getElementById('favorites-list');
    listEl.innerHTML = favorites.map(f => renderFavoriteCard(f)).join('');
    listEl.classList.remove('hidden');
    // 데스크톱 hover → 마커 강조 위임 (hospital.js의 헬퍼 재사용).
    attachCardHoverHighlight?.(listEl);
};

const renderFavoriteCard = (f) => {
    const lat = f.lat ?? 0;
    const lng = f.lng ?? 0;
    // inline onclick과 data-attribute 모두에 들어가는 ykiho는 두 컨텍스트에 안전해야 한다.
    // - JS 문자열 컨텍스트: 작은따옴표 escape
    // - HTML attribute 컨텍스트: escapeHtml로 따옴표/꺽쇠/앰퍼샌드 차단
    const ykihoJs = (f.ykiho ?? '').replace(/'/g, "\\'");
    const ykihoAttr = escapeHtml(f.ykiho ?? '');

    // 병원명 + 종별 한 줄
    const nameLine = (f.clCdNm || '').trim()
        ? `${escapeHtml(f.hospitalName)}<span class="text-xs text-gray-400 font-normal ml-1.5">· ${escapeHtml(f.clCdNm)}</span>`
        : escapeHtml(f.hospitalName);

    return `
        <div onclick="showHospitalInPanel('${ykihoJs}', 0, '', ${lat}, ${lng})"
             data-ykiho="${ykihoAttr}"
             class="hospital-card bg-white rounded-2xl p-4 cursor-pointer hover:opacity-95 transition-all"
             style="box-shadow: 0 2px 10px rgba(0,0,0,0.09);">
            <div class="flex items-start justify-between gap-3">
                <div class="flex-1 min-w-0">
                    <p class="font-semibold text-gray-900 text-sm truncate">${nameLine}</p>
                    <p class="text-xs text-gray-400 mt-1 truncate">${escapeHtml(f.address || '')}</p>
                    ${f.telNo ? `<p class="text-xs text-[#2563EB] mt-1">${escapeHtml(f.telNo)}</p>` : ''}
                </div>
                <button onclick="handleFavoritesRemove('${ykihoJs}', event)"
                        class="flex-shrink-0 p-1.5 text-yellow-400 hover:text-yellow-500 transition-colors rounded-lg hover:bg-yellow-50"
                        title="즐겨찾기 해제">
                    <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" fill="currentColor" viewBox="0 0 24 24">
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
    `;
};

// 즐겨찾기 목록 마커 지도에 추가
const addFavoriteMarkers = (favorites) => {
    clearMarkers?.();

    let hasLocation = false;
    favorites.forEach(f => {
        if (!f.lat || !f.lng) return;
        hasLocation = true;
        addMarker?.(f.lat, f.lng, f.hospitalName, null, false, f.ykiho, () => {
            showHospitalInPanel(f.ykiho, 0, '', f.lat, f.lng);
        });
    });

    initClustering?.();

    // 첫 번째 좌표가 있는 병원으로 지도 중심 이동
    if (hasLocation) {
        const first = favorites.find(f => f.lat && f.lng);
        if (first) focusMapOnHospital?.(first.lat, first.lng);
    }
};

// ── 즐겨찾기 삭제 다이얼로그 ─────────────────────────────────────────────────

let _pendingRemoveYkiho = null;

const handleFavoritesRemove = (ykiho, event) => {
    event.stopPropagation();
    _pendingRemoveYkiho = ykiho;

    // 병원명 표시 (_hospitalMap은 hospital.js에서 관리)
    const name = _hospitalMap?.[ykiho]?.yadmNm ?? '';
    const nameEl = document.getElementById('frd-name');
    if (nameEl) nameEl.textContent = name;

    const dialog = document.getElementById('fav-remove-dialog');
    if (dialog) {
        dialog.classList.add('open');
    }
};

const closeFavRemoveDialog = () => {
    _pendingRemoveYkiho = null;
    const dialog = document.getElementById('fav-remove-dialog');
    const card   = document.getElementById('frd-card');
    if (!dialog) return;

    // 카드 닫힘 애니메이션 후 숨기기
    if (card) {
        card.style.transition = 'transform 0.18s ease, opacity 0.18s ease';
        card.style.transform  = 'scale(0.88) translateY(8px)';
        card.style.opacity    = '0';
    }
    setTimeout(() => {
        dialog.classList.remove('open');
        if (card) {
            card.style.transition = '';
            card.style.transform  = '';
            card.style.opacity    = '';
        }
    }, 180);
};

const confirmFavRemove = async () => {
    const ykiho = _pendingRemoveYkiho;
    closeFavRemoveDialog();
    if (!ykiho) return;

    try {
        await api.delete(`/api/favorites/${encodeURIComponent(ykiho)}`);

        // 상세 패널이 해당 병원으로 열려 있으면 닫기
        const pdFavBtn = document.getElementById('pd-fav-btn');
        if (pdFavBtn?.dataset.ykiho === ykiho) {
            showHospitalList();
        }

        // 지도 마커 제거
        removeMarkerByYkiho?.(ykiho);

        // 카드 fade-out 후 제거
        const card = document.querySelector(`.hospital-card[data-ykiho="${ykiho}"]`);
        if (card) {
            card.style.transition = 'opacity 0.2s, transform 0.2s';
            card.style.opacity    = '0';
            card.style.transform  = 'scale(0.96)';
            setTimeout(() => {
                card.remove();

                const remaining = document.querySelectorAll('#favorites-list > div').length;
                const listEl    = document.getElementById('favorites-list');
                const countEl   = document.getElementById('favorite-count');

                if (remaining === 0) {
                    listEl.classList.add('hidden');
                    countEl.classList.add('hidden');
                    document.getElementById('empty-state').classList.remove('hidden');
                } else {
                    countEl.textContent = `총 ${remaining}개`;
                }
            }, 200);
        }
    } catch {
        alert('즐겨찾기 삭제 중 오류가 발생했습니다.');
    }
};

// escapeHtml은 common.js에서 정의 — 별도 정의 금지(공통화).

// 페이지 로드
document.addEventListener('DOMContentLoaded', loadFavorites);
