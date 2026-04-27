/**
 * 병원 목록 및 상세 데이터 처리
 */

const showState = (id) => {
    ['state-loading', 'state-error', 'state-content'].forEach(s => {
        const el = document.getElementById(s);
        if (el) el.classList.toggle('hidden', s !== id);
    });
};

// ── 병원 목록 ────────────────────────────────────────────────────────────────

const renderHospitalCard = (hospital) => {
    return `
        <a href="/hospitals/${hospital.id}"
           class="block bg-white rounded-xl shadow-sm p-4 hover:shadow-md transition-shadow border border-gray-50">
            <div class="flex items-start justify-between">
                <div class="flex-1 min-w-0">
                    <p class="font-semibold text-gray-800 text-sm truncate">${hospital.name}</p>
                    <p class="text-xs text-gray-400 mt-0.5">${hospital.type ?? ''}</p>
                    <p class="text-xs text-gray-500 mt-1 truncate">${hospital.address ?? ''}</p>
                </div>
                <span class="text-xs text-blue-500 font-medium flex-shrink-0 ml-3">
                    ${formatDistance(hospital.distance)}
                </span>
            </div>
        </a>`;
};

const fetchHospitals = async (keyword) => {
    showState('state-loading');
    try {
        const params = new URLSearchParams();
        if (keyword) params.set('keyword', keyword);
        const data = await api.get('/api/hospitals?' + params.toString());

        if (!data.success || !data.data?.length) {
            showState('state-error');
            return;
        }

        const list = document.getElementById('hospital-list');
        list.innerHTML = data.data.map(renderHospitalCard).join('');
        showState(null); // 카드가 list에 직접 렌더링되므로 state div 숨김

        // 지도 마커
        clearMarkers?.();
        data.data.forEach(h => {
            if (h.lat && h.lng) {
                addMarker?.(h.lat, h.lng, h.name, () => {
                    window.location.href = '/hospitals/' + h.id;
                });
            }
        });
    } catch {
        showState('state-error');
    }
};

// ── 병원 상세 ────────────────────────────────────────────────────────────────

const fetchHospitalDetail = async (id) => {
    showState('state-loading');
    try {
        const data = await api.get('/api/hospitals/' + id);
        if (!data.success || !data.data) {
            showState('state-error');
            return;
        }

        const h = data.data;
        document.getElementById('hospital-name').textContent    = h.name ?? '';
        document.getElementById('hospital-type').textContent    = h.type ?? '';
        document.getElementById('hospital-address').textContent = h.address ?? '';
        document.getElementById('hospital-distance').textContent = formatDistance(h.distance);

        const phoneEl = document.getElementById('hospital-phone');
        if (h.phone) {
            phoneEl.textContent = h.phone;
            phoneEl.href = 'tel:' + h.phone;
        } else {
            phoneEl.textContent = '전화번호 정보 없음';
        }

        // 가격 테이블
        const prices = h.prices ?? [];
        if (!prices.length) {
            document.getElementById('price-empty').classList.remove('hidden');
        } else {
            const tbody = document.getElementById('price-tbody');
            tbody.innerHTML = prices.map(p => `
                <tr>
                    <td class="py-3">${p.itemName}</td>
                    <td class="py-3 text-right">${formatPrice(p.minPrice)}</td>
                    <td class="py-3 text-right">${formatPrice(p.maxPrice)}</td>
                </tr>`).join('');
            document.getElementById('price-table').classList.remove('hidden');
        }

        showState('state-content');
    } catch {
        showState('state-error');
    }
};
