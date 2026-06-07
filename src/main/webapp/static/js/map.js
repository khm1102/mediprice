
let naverMap = null;
let _markerClustering = null;
const markers = [];
let _pendingMarkers = [];

const _markerByYkiho = {};
const _markerDataByYkiho = {};
let _highlightedMarkerYkiho = null;

const initMap = (lat, lng) => {
    const mapEl = document.getElementById('map');
    if (!mapEl || typeof naver === 'undefined') return;

    document.getElementById('map-loading')?.remove();

    naverMap = new naver.maps.Map(mapEl, {
        center: new naver.maps.LatLng(lat, lng),
        zoom: 14,
        mapTypeControl: false,
        scaleControl: false,
        logoControl: true,
        mapDataControl: false,
    });

    // 지도 드래그 끝 → 검색 결과가 있으면 '이 지역에서 재검색' 버튼 표시
    naver.maps.Event.addListener(naverMap, 'dragend', () => {
        const keyword = document.getElementById('search-input')?.value?.trim();
        if (keyword) showReSearchBtn?.();
    });

    // 지도 준비 완료 후 대기 중인 마커 일괄 추가 후 클러스터링
    if (_pendingMarkers.length > 0) {
        _pendingMarkers.forEach(args => _addMarkerNow(...args));
        _pendingMarkers = [];
        initClustering();
    }
};

const clearMarkers = () => {
    if (_markerClustering) {
        _markerClustering.setMap(null);
        _markerClustering = null;
    }
    markers.forEach(m => m.setMap(null));
    markers.length = 0;
    _pendingMarkers = [];
    Object.keys(_markerByYkiho).forEach(k => delete _markerByYkiho[k]);
    Object.keys(_markerDataByYkiho).forEach(k => delete _markerDataByYkiho[k]);
    _highlightedMarkerYkiho = null;
};

const addMarker = (lat, lng, name, price, isCheapest, ykiho, onHighlight) => {
    if (!naverMap) {
        _pendingMarkers.push([lat, lng, name, price, isCheapest, ykiho, onHighlight]);
        return;
    }
    _addMarkerNow(lat, lng, name, price, isCheapest, ykiho, onHighlight);
};

const _addMarkerNow = (lat, lng, name, price, isCheapest, ykiho, onHighlight) => {
    const marker = new naver.maps.Marker({
        position: new naver.maps.LatLng(lat, lng),
        // map을 직접 설정하지 않음 — MarkerClustering이 관리
        icon: { content: buildPinHtml(name, price, isCheapest) },
        zIndex: isCheapest ? 10 : 1,
    });

    naver.maps.Event.addListener(marker, 'click', () => onHighlight?.());

    markers.push(marker);
    _markerByYkiho[ykiho] = marker;
    _markerDataByYkiho[ykiho] = { name, price };
};

/** 클러스터링 인스턴스 생성 */
const initClustering = () => {
    if (!naverMap || markers.length === 0) return;

    // MarkerClustering 라이브러리 로드 실패 시 마커 직접 지도에 추가
    if (typeof MarkerClustering === 'undefined') {
        markers.forEach(m => m.setMap(naverMap));
        return;
    }

    if (_markerClustering) {
        _markerClustering.setMap(null);
    }

    _markerClustering = new MarkerClustering({
        minClusterSize: 2,
        maxZoom: 14,        // 15이상 줌인 시 개별 마커 표시
        map: naverMap,
        markers: [...markers],
        disableClickZoom: false,
        gridSize: 160,
        icons: [_buildClusterIcon(38), _buildClusterIcon(46)],
        indexGenerator: [10, 50],
        stylingFunction: (clusterMarker, count) => {
            const el = clusterMarker.getElement();
            if (el) {
                const countEl = el.querySelector('.cluster-count');
                if (countEl) countEl.textContent = count;
            }
        },
    });
};

/** 클러스터 아이콘  */
const _buildClusterIcon = (size) => ({
    content: `<div style="
        width:${size}px; height:${size}px;
        background:#2563EB; border-radius:50%;
        display:flex; align-items:center; justify-content:center;
        color:#fff; font-weight:700; font-size:13px;
        box-shadow:0 2px 10px rgba(37,99,235,0.45);
        border:2px solid #fff;
        transform:translate(-50%,-50%);
        cursor:pointer;
    "><span class="cluster-count"></span></div>`,
    size: new naver.maps.Size(size, size),
    anchor: new naver.maps.Point(size / 2, size / 2),
});

/** 선택된 병원 마커 파란색 강조 */
const clearMarkerHighlight = () => {
    if (!_highlightedMarkerYkiho) return;
    const marker = _markerByYkiho[_highlightedMarkerYkiho];
    const data   = _markerDataByYkiho[_highlightedMarkerYkiho];
    if (marker && data) {
        marker.setIcon({ content: buildPinHtml(data.name, data.price, false) });
        marker.setZIndex(1);
    }
    _highlightedMarkerYkiho = null;
};

const highlightMarker = (ykiho) => {
    clearMarkerHighlight();
    const marker = _markerByYkiho[ykiho];
    const data   = _markerDataByYkiho[ykiho];
    if (!marker || !data) return;
    _highlightedMarkerYkiho = ykiho;
    marker.setIcon({ content: buildPinHtml(data.name, data.price, true) });
    marker.setZIndex(20);
};

/** 개별 마커 제거 (즐겨찾기 해제 시 사용) */
const removeMarkerByYkiho = (ykiho) => {
    if (_highlightedMarkerYkiho === ykiho) {
        _highlightedMarkerYkiho = null;
    }
    const marker = _markerByYkiho[ykiho];
    if (!marker) return;
    marker.setMap(null);
    const idx = markers.indexOf(marker);
    if (idx !== -1) markers.splice(idx, 1);
    delete _markerByYkiho[ykiho];
    delete _markerDataByYkiho[ykiho];

    // 클러스터링 재빌드 (마커가 남아 있는 경우)
    if (_markerClustering) {
        _markerClustering.setMap(null);
        _markerClustering = null;
    }
    if (markers.length > 0) initClustering();
};

/** 지도 중심 이동 + 확대 */
const focusMapOnHospital = (lat, lng) => {
    if (!naverMap || !lat || !lng) return;
    naverMap.setCenter(new naver.maps.LatLng(lat, lng));
    naverMap.setZoom(16);
};

/**
 * 지도 컨테이너의 가시/크기 변화(예: 모바일 segmented control 'list' → 'map')에 대응해
 * Naver Map이 캔버스 사이즈를 다시 측정하게 한다.
 */
const refreshMapSize = () => {
    if (!naverMap || typeof naver === 'undefined') return;
    try {
        naver.maps.Event.trigger(naverMap, 'resize');
    } catch (e) {
        // SDK 미초기화 등 — 무시
    }
};

/** 핀 HTML — name은 외부(DB/HIRA) 데이터이므로 null-safe + escapeHtml 처리. */
const buildPinHtml = (name, price, isCheapest) => {
    const safeName = String(name ?? '');
    const shortName = safeName.length > 9 ? safeName.slice(0, 9) + '…' : safeName;
    const safeShortName = escapeHtml(shortName);
    const priceText = price != null ? escapeHtml(Number(price).toLocaleString('ko-KR') + '원') : null;
    const bg        = isCheapest ? '#2563EB' : '#fff';
    const color     = isCheapest ? '#fff'    : '#374151';
    const priceColor = isCheapest ? 'rgba(255,255,255,0.85)' : '#2563EB';
    const border    = isCheapest ? 'none'    : '1px solid #E5E7EB';
    const shadow    = isCheapest ? '0 3px 8px rgba(37,99,235,0.38)' : '0 2px 6px rgba(0,0,0,0.12)';
    const stemColor = isCheapest ? '#2563EB' : '#D1D5DB';

    return `<div style="transform:translate(-50%,-100%);display:flex;flex-direction:column;align-items:center;cursor:pointer;">
        <div style="background:${bg};border:${border};border-radius:8px;padding:3px 8px;box-shadow:${shadow};white-space:nowrap;text-align:center;">
            <div style="font-size:11px;font-weight:${isCheapest ? '700' : '500'};color:${color};">${safeShortName}</div>
            ${priceText ? `<div style="font-size:10px;font-weight:600;color:${priceColor};margin-top:1px;">${priceText}</div>` : ''}
        </div>
        <div style="width:2px;height:5px;background:${stemColor};"></div>
        <div style="width:5px;height:5px;border-radius:50%;background:${stemColor};"></div>
    </div>`;
};

/** 현재 지도 중심 좌표 반환 */
const getMapCenter = () => {
    if (!naverMap) return null;
    const c = naverMap.getCenter();
    return { lat: c.lat(), lng: c.lng() };
};

const recenterMap = () => {
    if (!navigator.geolocation) {
        showToast('위치 정보를 지원하지 않는 브라우저입니다', 'error');
        return;
    }
    navigator.geolocation.getCurrentPosition(
        pos => {
            if (!naverMap) return;
            naverMap.setCenter(new naver.maps.LatLng(pos.coords.latitude, pos.coords.longitude));
            naverMap.setZoom(14);
        },
        () => showToast('현재 위치를 가져올 수 없습니다', 'error')
    );
};

// 현재 위치 표시(파란 점 + 정확도 원)
let _meMarker = null;
let _meCircle = null;

const showCurrentLocation = (lat, lng, accuracyMeters) => {
    if (!naverMap || lat == null || lng == null) return;
    const pos = new naver.maps.LatLng(lat, lng);

    if (!_meMarker) {
        _meMarker = new naver.maps.Marker({
            position: pos,
            map: naverMap,
            zIndex: 50,
            icon: {
                content: `<div style="position:relative;width:18px;height:18px;transform:translate(-50%,-50%);">
                    <div style="position:absolute;inset:0;border-radius:50%;background:#2563EB;border:2.5px solid #fff;box-shadow:0 2px 8px rgba(37,99,235,0.45);"></div>
                </div>`,
                anchor: new naver.maps.Point(0, 0),
            },
        });
    } else {
        _meMarker.setPosition(pos);
    }

    const radius = Math.max(20, Math.min(accuracyMeters ?? 80, 300));
    if (!_meCircle) {
        _meCircle = new naver.maps.Circle({
            map: naverMap,
            center: pos,
            radius,
            fillColor: '#2563EB',
            fillOpacity: 0.08,
            strokeColor: '#2563EB',
            strokeOpacity: 0.35,
            strokeWeight: 1,
        });
    } else {
        _meCircle.setCenter(pos);
        _meCircle.setRadius(radius);
    }
};

const initMapWithCurrentLocation = () => {
    if (!navigator.geolocation) {
        initMap(37.5665, 126.9780);
        return;
    }
    navigator.geolocation.getCurrentPosition(
        pos => {
            initMap(pos.coords.latitude, pos.coords.longitude);
            // 현재 위치 마커는 지도 인스턴스가 만들어진 다음 프레임에 안전하게 추가.
            requestAnimationFrame(() =>
                showCurrentLocation(pos.coords.latitude, pos.coords.longitude, pos.coords.accuracy));
        },
        () => initMap(37.5665, 126.9780)
    );
};

document.addEventListener('DOMContentLoaded', () => {
    if (document.getElementById('map')) {
        initMapWithCurrentLocation();
    }
});
