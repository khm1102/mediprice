/**
 * 네이버맵 초기화 및 마커 관리
 */
let naverMap = null;
const markers = [];

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
};

const clearMarkers = () => {
    markers.forEach(m => m.setMap(null));
    markers.length = 0;
};

const addMarker = (lat, lng, title, onClick) => {
    if (!naverMap) return;
    const marker = new naver.maps.Marker({
        position: new naver.maps.LatLng(lat, lng),
        map: naverMap,
        title,
    });
    if (onClick) naver.maps.Event.addListener(marker, 'click', onClick);
    markers.push(marker);
};

// 현재 위치로 지도 중심 이동
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

// 현재 위치 기준으로 지도 초기화
const initMapWithCurrentLocation = () => {
    if (!navigator.geolocation) {
        initMap(37.5665, 126.9780); // 서울 기본값
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
