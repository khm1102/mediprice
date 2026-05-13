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
const showToast = (() => {
    // keyframe 한 번만 주입
    if (!document.getElementById('toast-style')) {
        const style = document.createElement('style');
        style.id = 'toast-style';
        style.textContent = `
            @keyframes toastIn {
                from { opacity: 0; bottom: 1rem; }
                to   { opacity: 1; bottom: 2rem; }
            }
            @keyframes toastOut {
                from { opacity: 1; bottom: 2rem; }
                to   { opacity: 0; bottom: 1rem; }
            }
            .toast-in  { animation: toastIn  0.3s ease forwards; }
            .toast-out { animation: toastOut 0.35s ease forwards; }
        `;
        document.head.appendChild(style);
    }

    return (message, type = 'info') => {
        const bgColor = type === 'error' ? '#ef4444' : '#1f2937';
        const toast = document.createElement('div');
        toast.textContent = message;
        toast.classList.add('toast-in');
        Object.assign(toast.style, {
            position: 'fixed',
            left: '50%',
            transform: 'translateX(-50%)',
            background: bgColor,
            color: '#fff',
            fontSize: '0.875rem',
            padding: '0.625rem 1.25rem',
            borderRadius: '9999px',
            boxShadow: '0 4px 16px rgba(0,0,0,0.18)',
            zIndex: '9999',
            whiteSpace: 'nowrap',
        });
        document.body.appendChild(toast);

        setTimeout(() => {
            toast.classList.replace('toast-in', 'toast-out');
            setTimeout(() => toast.remove(), 380);
        }, 2500);
    };
})();
