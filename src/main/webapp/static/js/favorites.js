
const loadFavorites = async () => {
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
};

const renderFavoriteCard = (f) => {
    const lat = f.lat ?? 0;
    const lng = f.lng ?? 0;
    const ykihoEsc = f.ykiho.replace(/'/g, "\\'");

    return `
        <div onclick="showHospitalInPanel('${ykihoEsc}', 0, '', ${lat}, ${lng})"
             data-ykiho="${f.ykiho}"
             class="hospital-card bg-white rounded-2xl p-4 cursor-pointer hover:opacity-90 transition-all"
             style="box-shadow: 0 2px 10px rgba(0,0,0,0.09);">
            <div class="flex items-start justify-between gap-3">
                <div class="flex-1 min-w-0">
                    <p class="font-semibold text-gray-900 text-sm truncate">${escapeHtml(f.hospitalName)}</p>
                    <p class="text-xs text-gray-400 mt-0.5">${escapeHtml(f.clCdNm || '')}</p>
                    <p class="text-xs text-gray-400 mt-1 truncate">${escapeHtml(f.address || '')}</p>
                    ${f.telNo ? `<p class="text-xs text-[#2563EB] mt-1">${escapeHtml(f.telNo)}</p>` : ''}
                </div>
                <button onclick="handleFavoritesRemove('${ykihoEsc}', event)"
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

// 즐겨찾기 해제 공통 처리 (카드 버튼 & 상세 패널 별 버튼 공용)
const handleFavoritesRemove = async (ykiho, event) => {
    event.stopPropagation();
    if (!confirm('즐겨찾기에서 삭제하시겠습니까?')) return;

    try {
        await api.delete(`/api/favorites/${encodeURIComponent(ykiho)}`);

        // 상세 패널이 해당 병원으로 열려 있으면 닫기
        const pdFavBtn = document.getElementById('pd-fav-btn');
        if (pdFavBtn?.dataset.ykiho === ykiho) {
            showHospitalList();
        }

        // 지도 마커 제거
        removeMarkerByYkiho?.(ykiho);

        // 카드 제거 (fade-out 애니메이션)
        const card = document.querySelector(`.hospital-card[data-ykiho="${ykiho}"]`);
        if (card) {
            card.style.transition = 'opacity 0.2s';
            card.style.opacity = '0';
            setTimeout(() => {
                card.remove();

                const remaining = document.querySelectorAll('#favorites-list > div').length;
                const listEl = document.getElementById('favorites-list');
                const countEl = document.getElementById('favorite-count');

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

const escapeHtml = (str) => {
    if (!str) return '';
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
};

// 페이지 로드
document.addEventListener('DOMContentLoaded', loadFavorites);
