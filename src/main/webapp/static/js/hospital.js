/**
 * 병원 목록 및 상세 데이터 처리
 */

// ── 더미 데이터 (keyword === '테스트' 일 때 사용) ─────────────────────────────

const DUMMY_HOSPITALS = [
    {
        id: 101, name: '테스트1 정형외과의원', type: '정형외과',
        address: '근처 테헤란로 152', distance: 850, phone: '02-1234-5678',
        minPrice: 78000, maxPrice: 120000,
        prices: [
            { itemName: '도수치료 (30분)', minPrice: 78000, maxPrice: 120000 },
            { itemName: '체외충격파',      minPrice: 40000, maxPrice:  60000 },
        ],
    },
    {
        id: 102, name: '테스트2 재활의학과의원', type: '재활의학과',
        address: '근처 강남대로 311', distance: 1200, phone: '02-2345-6789',
        minPrice: 35000, maxPrice: 65000,
        prices: [
            { itemName: '도수치료 (30분)', minPrice: 35000, maxPrice:  65000 },
            { itemName: '근막이완술',      minPrice: 25000, maxPrice:  45000 },
        ],
    },
    {
        id: 103, name: '테스트3 통증의학과의원', type: '통증의학과',
        address: '근처 한강대로 92', distance: 1700, phone: '02-3456-7890',
        minPrice: 61000, maxPrice: 95000,
        prices: [
            { itemName: '도수치료 (30분)', minPrice: 61000, maxPrice:  95000 },
            { itemName: '프롤로치료',      minPrice: 50000, maxPrice:  80000 },
        ],
    },
    {
        id: 104, name: '테스트4 종합병원', type: '종합병원',
        address: '근처 올림픽로 300', distance: 2100, phone: '02-4567-8901',
        minPrice: 52000, maxPrice: 90000,
        prices: [
            { itemName: '도수치료 (30분)',  minPrice: 52000, maxPrice:  90000 },
            { itemName: '초음파 유도 주사', minPrice: 60000, maxPrice: 100000 },
        ],
    },
    {
        id: 105, name: '테스트5 한의원', type: '한의원',
        address: '근처 양화로 45', distance: 3400, phone: '02-5678-9012',
        minPrice: 28000, maxPrice: 50000,
        prices: [
            { itemName: '추나요법', minPrice: 28000, maxPrice: 50000 },
            { itemName: '침 치료', minPrice: 15000, maxPrice: 30000 },
        ],
    },
    {
        id: 106, name: '테스트6 가정의학과의원', type: '가정의학과',
        address: '근처 동소문로 77', distance: 4200, phone: '02-6789-0123',
        minPrice: 23000, maxPrice: 45000,
        prices: [
            { itemName: '도수치료 (20분)', minPrice: 23000, maxPrice: 45000 },
            { itemName: '체외충격파',      minPrice: 30000, maxPrice: 55000 },
        ],
    },
    {
        id: 107, name: '테스트7 신경외과의원', type: '신경외과',
        address: '근처 왕산로 14', distance: 2800, phone: '02-7890-1234',
        minPrice: 88000, maxPrice: 150000,
        prices: [
            { itemName: '도수치료 (45분)', minPrice: 88000, maxPrice: 150000 },
            { itemName: '신경차단술',      minPrice: 70000, maxPrice: 120000 },
        ],
    },
    {
        id: 108, name: '테스트8 스포츠의학과', type: '스포츠의학',
        address: '근처 능동로 209', distance: 3900, phone: '02-8901-2345',
        minPrice: 67000, maxPrice: 110000,
        prices: [
            { itemName: '도수치료 (30분)', minPrice: 67000, maxPrice: 110000 },
            { itemName: '운동치료',        minPrice: 30000, maxPrice:  50000 },
        ],
    },
    {
        id: 109, name: '테스트9 내과의원', type: '내과',
        address: '근처 여의대로 24', distance: 5100, phone: '02-9012-3456',
        minPrice: 42000, maxPrice: 75000,
        prices: [
            { itemName: '도수치료 (30분)', minPrice: 42000, maxPrice: 75000 },
        ],
    },
    {
        id: 110, name: '테스트10 재활의학과', type: '재활의학과',
        address: '근처 동일로 1321', distance: 6300, phone: '02-0123-4567',
        minPrice: 19000, maxPrice: 38000,
        prices: [
            { itemName: '도수치료 (20분)', minPrice: 19000, maxPrice:  38000 },
            { itemName: '근막이완술',      minPrice: 15000, maxPrice:  28000 },
        ],
    },
];


const showState = (id) => {
    ['state-loading', 'state-empty', 'state-error', 'state-content'].forEach(s => {
        const el = document.getElementById(s);
        if (el) el.classList.toggle('hidden', s !== id);
    });
};

// ── 병원 목록 ────────────────────────────────────────────────────────────────

const renderHospitalCard = (hospital, isCheapest = false) => {
    const borderClass = isCheapest
        ? 'border-blue-400 ring-2 ring-blue-100'
        : 'border-gray-100';

    const cheapestBadge = isCheapest
        ? `<span class="text-xs bg-blue-600 text-white px-2 py-0.5 rounded-full font-medium ml-2">최저가</span>`
        : '';

    const priceText = hospital.minPrice != null
        ? `<p class="text-xs text-blue-600 font-medium mt-1">${formatPrice(hospital.minPrice)} ~</p>`
        : '';

    return `
        <a href="/hospitals/${hospital.id}"
           class="block bg-white rounded-xl shadow-sm p-4 hover:shadow-md transition-shadow border ${borderClass}">
            <div class="flex items-start justify-between">
                <div class="flex-1 min-w-0">
                    <div class="flex items-center">
                        <p class="font-semibold text-gray-800 text-sm truncate">${hospital.name}</p>
                        ${cheapestBadge}
                    </div>
                    <p class="text-xs text-gray-400 mt-0.5">${hospital.type ?? ''}</p>
                    <p class="text-xs text-gray-500 mt-1 truncate">${hospital.address ?? ''}</p>
                    ${priceText}
                </div>
                <span class="text-xs text-gray-400 font-medium flex-shrink-0 ml-3">
                    ${formatDistance(hospital.distance)}
                </span>
            </div>
        </a>`;
};

const fetchHospitals = async (keyword) => {
    showState('state-loading');

    // 더미 데이터 모드 (keyword === '테스트')
    if (keyword === '테스트') {
        // 최저가 기준 오름차순 정렬
        const sorted = [...DUMMY_HOSPITALS].sort((a, b) => a.minPrice - b.minPrice);

        const list = document.getElementById('hospital-list');
        list.innerHTML = sorted.map((h, i) => renderHospitalCard(h, i === 0)).join('');
        showState(null);

        return;
    }

    try {
        const params = new URLSearchParams();
        if (keyword) params.set('keyword', keyword);
        const data = await api.get('/api/hospitals?' + params.toString());

        if (!data.success) {
            showState('state-error');
            return;
        }
        if (!data.data?.length) {
            showState('state-empty');
            return;
        }

        const list = document.getElementById('hospital-list');
        list.innerHTML = data.data.map(h => renderHospitalCard(h)).join('');
        showState(null);

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
        document.getElementById('hospital-name').textContent     = h.name ?? '';
        document.getElementById('hospital-type').textContent     = h.type ?? '';
        document.getElementById('hospital-address').textContent  = h.address ?? '';
        document.getElementById('hospital-distance').textContent = formatDistance(h.distance);

        const phoneEl = document.getElementById('hospital-phone');
        if (h.phone) {
            phoneEl.textContent = h.phone;
            phoneEl.href = 'tel:' + h.phone;
        } else {
            phoneEl.textContent = '전화번호 정보 없음';
        }

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
