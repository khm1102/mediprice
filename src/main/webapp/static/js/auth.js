
const getToken = () => document.cookie
    .split('; ')
    .find(row => row.startsWith('mp_token='))
    ?.split('=')[1] ?? null;

const getTokenPayload = () => {
    const token = getToken();
    if (!token) return null;
    try {
        const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
        const json = decodeURIComponent(
            atob(base64).split('').map(c =>
                '%' + c.charCodeAt(0).toString(16).padStart(2, '0')
            ).join('')
        );
        return JSON.parse(json);
    } catch {
        return null;
    }
};

const isLoggedIn = () => {
    const payload = getTokenPayload();
    return payload !== null && payload.exp > Date.now() / 1000 && payload.role !== 'GUEST';
};

const getMemberInfo = () => {
    const payload = getTokenPayload();
    if (!payload || payload.role === 'GUEST') return null;
    return { email: payload.email, role: payload.role, name: payload.name ?? '' };
};

const handleLogout = async () => {
    try {
        await fetch('/api/auth/logout', { method: 'POST' });
    } catch {
        // 서버 오류와 무관하게 클라이언트 토큰 제거
    }
    document.cookie = 'mp_token=; Max-Age=0; path=/';
    window.location.href = '/';
};

// ── 페이지 로드 시 로그인 상태에 따라 네비게이션 전환 ────────────────────────

document.addEventListener('DOMContentLoaded', () => {
    const navGuest  = document.getElementById('nav-guest');
    const navMember = document.getElementById('nav-member');
    if (!navGuest || !navMember) return;

    if (isLoggedIn()) {
        navGuest.classList.add('hidden');
        navMember.classList.remove('hidden');

        // 드롭다운 정보 채우기
        const info = getMemberInfo();
        if (info) {
            const setText = (id, val) => { const el = document.getElementById(id); if (el) el.textContent = val; };
            setText('menu-name',  info.name  || '');
            setText('menu-email', info.email || '');
        }
    }

    // ── 프로필 버튼 클릭 핸들러 ──
    const profileBtn = document.getElementById('profile-btn');
    if (profileBtn) {
        profileBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            const menu = document.getElementById('profile-menu');
            if (menu) menu.classList.toggle('hidden');
        });
    }

    // ── 드롭다운 외부 클릭 시 닫기 ──
    document.addEventListener('click', (e) => {
        const btn  = document.getElementById('profile-btn');
        const menu = document.getElementById('profile-menu');
        if (!menu || menu.classList.contains('hidden')) return;
        if (!btn?.contains(e.target) && !menu.contains(e.target)) {
            menu.classList.add('hidden');
        }
    });
});
