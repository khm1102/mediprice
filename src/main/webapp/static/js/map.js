/**
 * 네이버맵 초기화 및 마커 관리
 */
let naverMap = null;
const markers = [];
let _pendingMarkers = []; // 지도 준비 전 addMarker 호출 큐


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

    // 지도 준비 완료 후 대기 중인 마커 일괄 추가
    _pendingMarkers.forEach(args => _addMarkerNow(...args));
    _pendingMarkers = [];
};

const clearMarkers = () => {
    markers.forEach(m => m.setMap(null));
    markers.length = 0;
    _pendingMarkers = [];
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
        map: naverMap,
        icon: { content: buildPinHtml(name, price, isCheapest) },
        zIndex: isCheapest ? 10 : 1,
    });

    naver.maps.Event.addListener(marker, 'click', () => {
        // 왼쪽 목록에서 해당 카드 강조 + 스크롤 (핀 자체 시각 변화 없음)
        onHighlight?.();
    });

    markers.push(marker);
};

/** 지도 중심 이동 + 확대 (병원 상세 열릴 때 호출) */
const focusMapOnHospital = (lat, lng) => {
    if (!naverMap || !lat || !lng) return;
    naverMap.setCenter(new naver.maps.LatLng(lat, lng));
    naverMap.setZoom(16);
};

/** 핀 HTML — 말풍선 + 기둥 + 점 구조 */
const buildPinHtml = (name, price, isCheapest) => {
    const priceLabel = price != null ? formatPrice(price) : '가격 미등록';
    const shortName = name.length > 7 ? name.slice(0, 7) + '…' : name;

    const bubbleColor = isCheapest ? '#2563EB' : '#fff';
    const priceColor  = isCheapest ? '#fff'    : '#111827';
    const nameColor   = isCheapest ? 'rgba(255,255,255,0.8)' : '#9CA3AF';
    const stemColor   = isCheapest ? '#2563EB' : '#D1D5DB';
    const dotColor    = isCheapest ? '#2563EB' : '#D1D5DB';
    const border      = isCheapest ? 'none'    : '1.5px solid #E5E7EB';
    const shadow      = isCheapest
        ? '0 4px 12px rgba(37,99,235,0.45)'
        : '0 2px 8px rgba(0,0,0,0.15)';

    const starIcon = isCheapest
        ? `<svg width="10" height="10" viewBox="0 0 20 20" fill="${priceColor}" style="flex-shrink:0;">
               <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z"/>
           </svg>`
        : '';

    return `
    <div style="
        transform: translate(-50%, -100%);
        display: flex; flex-direction: column; align-items: center;
        cursor: pointer;
    ">
        <div style="
            background: ${bubbleColor}; border: ${border};
            border-radius: 12px; padding: 6px 11px;
            box-shadow: ${shadow}; white-space: nowrap;
            display: flex; flex-direction: column; align-items: center; gap: 2px;
        ">
            <div style="display:flex; align-items:center; gap:3px;">
                ${starIcon}
                <span style="font-size:12px; font-weight:700; color:${priceColor};">${priceLabel}</span>
            </div>
            <span style="font-size:10px; color:${nameColor};">${shortName}</span>
        </div>
        <div style="width:2px; height:7px; background:${stemColor};"></div>
        <div style="width:7px; height:7px; border-radius:50%; background:${dotColor};"></div>
    </div>`;
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

const initMapWithCurrentLocation = () => {
    if (!navigator.geolocation) {
        initMap(37.5665, 126.9780);
        return;
    }
    navigator.geolocation.getCurrentPosition(
        pos => initMap(pos.coords.latitude, pos.coords.longitude),
        ()  => initMap(37.5665, 126.9780)
    );
};

document.addEventListener('DOMContentLoaded', () => {
    if (document.getElementById('map')) {
        initMapWithCurrentLocation();
    }
});
