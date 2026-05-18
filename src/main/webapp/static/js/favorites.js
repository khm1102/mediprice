
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
        const countEl = document.getElementById('favorite-count');
        countEl.textContent = `총 ${favorites.length}개`;
        countEl.classList.remove('hidden');

        renderFavorites(favorites);
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
    const encodedYkiho = encodeURIComponent(f.ykiho);
    return `
        <div class="bg-white rounded-xl border border-gray-100 p-5 hover:shadow-sm transition-shadow"
             id="card-${CSS.escape(f.ykiho)}">
            <div class="flex items-start justify-between gap-4">
                <div class="flex-1 min-w-0">
                    <a href="/hospitals/${encodedYkiho}"
                       class="font-semibold text-gray-900 hover:text-[#2563EB] transition-colors text-base">
                        ${escapeHtml(f.hospitalName)}
                    </a>
                    <p class="text-sm text-gray-500 mt-0.5">${escapeHtml(f.clCdNm || '')}</p>
                    <p class="text-sm text-gray-400 mt-1 truncate">${escapeHtml(f.address || '')}</p>
                    ${f.telNo ? `
                        <a href="tel:${escapeHtml(f.telNo)}"
                           class="inline-flex items-center gap-1 text-sm text-[#2563EB] mt-1 hover:underline">
                            <svg xmlns="http://www.w3.org/2000/svg" class="w-3.5 h-3.5" fill="none"
                                 viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                                <path stroke-linecap="round" stroke-linejoin="round"
                                      d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0
                                      01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1
                                      1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716
                                      21 3 14.284 3 6V5z"/>
                            </svg>
                            ${escapeHtml(f.telNo)}
                        </a>` : ''}
                </div>
                <button onclick="handleRemoveFavorite('${f.ykiho.replace(/'/g, "\\'")}')"
                        class="flex-shrink-0 p-2 text-gray-400 hover:text-red-500 transition-colors rounded-lg hover:bg-red-50"
                        title="즐겨찾기 삭제">
                    <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
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

const handleRemoveFavorite = async (ykiho) => {
    if (!confirm('즐겨찾기에서 삭제하시겠습니까?')) return;

    try {
        await api.delete(`/api/favorites/${encodeURIComponent(ykiho)}`);

        // 카드 제거 (애니메이션)
        const card = document.getElementById(`card-${CSS.escape(ykiho)}`);
        if (card) {
            card.style.transition = 'opacity 0.2s';
            card.style.opacity = '0';
            setTimeout(() => {
                card.remove();

                // 남은 카드 수 업데이트
                const remaining = document.querySelectorAll('#favorites-list > div').length;
                if (remaining === 0) {
                    document.getElementById('favorites-list').classList.add('hidden');
                    document.getElementById('favorite-count').classList.add('hidden');
                    document.getElementById('empty-state').classList.remove('hidden');
                } else {
                    document.getElementById('favorite-count').textContent = `총 ${remaining}개`;
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

const toggleFavorite = async (ykiho, btnEl) => {
    if (!isLoggedIn()) {
        window.location.href = '/auth/login';
        return;
    }

    const isFav = btnEl.dataset.favorited === 'true';

    try {
        if (isFav) {
            await api.delete(`/api/favorites/${encodeURIComponent(ykiho)}`);
            btnEl.dataset.favorited = 'false';
            updateFavoriteBtn(btnEl, false);
        } else {
            await api.post('/api/favorites', { ykiho });
            btnEl.dataset.favorited = 'true';
            updateFavoriteBtn(btnEl, true);
        }
    } catch {
        showToast('즐겨찾기 처리 중 오류가 발생했습니다.', 'error');
    }
};

const updateFavoriteBtn = (btnEl, isFav) => {
    if (isFav) {
        btnEl.classList.add('text-yellow-400');
        btnEl.classList.remove('text-gray-300');
        btnEl.title = '즐겨찾기 해제';
    } else {
        btnEl.classList.remove('text-yellow-400');
        btnEl.classList.add('text-gray-300');
        btnEl.title = '즐겨찾기 추가';
    }
};

// 페이지 로드
document.addEventListener('DOMContentLoaded', loadFavorites);
