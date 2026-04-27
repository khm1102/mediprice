/**
 * 공통 유틸 함수
 */

/**
 * 숫자를 한국 원화 형식으로 포맷
 * @param {number} amount
 * @returns {string} 예: 150,000원
 */
const formatPrice = (amount) => {
    if (amount == null) return '정보 없음';
    return amount.toLocaleString('ko-KR') + '원';
};

/**
 * 거리를 보기 좋게 포맷
 * @param {number} meters
 * @returns {string} 예: 1.2km, 500m
 */
const formatDistance = (meters) => {
    if (meters == null) return '';
    if (meters >= 1000) return (meters / 1000).toFixed(1) + 'km';
    return Math.round(meters) + 'm';
};

/**
 * 토스트 알림 표시
 * @param {string} message
 * @param {'info'|'error'} type
 */
const showToast = (message, type = 'info') => {
    const toast = document.createElement('div');
    const bgColor = type === 'error' ? 'bg-red-500' : 'bg-gray-800';
    toast.className = `fixed bottom-6 left-1/2 -translate-x-1/2 ${bgColor} text-white text-sm px-5 py-2.5 rounded-full shadow-lg z-50 transition-opacity`;
    toast.textContent = message;
    document.body.appendChild(toast);
    setTimeout(() => {
        toast.style.opacity = '0';
        setTimeout(() => toast.remove(), 300);
    }, 2500);
};
