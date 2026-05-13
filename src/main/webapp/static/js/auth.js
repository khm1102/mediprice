/**
 * 토큰 관리 및 로그인/로그아웃 처리
 */
const TOKEN_KEY = 'mp_token';

const getToken = () => document.cookie
    .split('; ')
    .find(row => row.startsWith('mp_token='))
    ?.split('=')[1] ?? null;

const isLoggedIn = () => {
    const token = getToken();
    if (!token) return false;
    try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        return payload.exp > Date.now() / 1000 && payload.role !== 'GUEST';
    } catch {
        return false;
    }
};

const handleLogout = async () => {
    try {
        await api.post('/api/auth/logout', {});
    } catch {
        // 서버 오류와 무관하게 클라이언트 토큰 제거
    }
    document.cookie = 'mp_token=; Max-Age=0; path=/';
    window.location.href = '/';
};

// 페이지 로드 시 로그인 상태에 따라 네비게이션 전환
document.addEventListener('DOMContentLoaded', () => {
    const navGuest = document.getElementById('nav-guest');
    const navMember = document.getElementById('nav-member');
    if (!navGuest || !navMember) return;

    if (isLoggedIn()) {
        navGuest.classList.add('hidden');
        navMember.classList.remove('hidden');
    }
});
