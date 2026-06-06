// mp_token 쿠키가 HttpOnly이므로 JS에서 토큰을 직접 디코딩할 수 없다.
// 페이지 로드 시 /api/auth/me를 호출해 로그인 상태를 캐싱한다.
// 호출처는 가능하면 `await authReady` 후 isLoggedIn()/getMemberInfo()를 사용한다.

let memberState = null;

const authReady = (async () => {
    try {
        const res = await fetch('/api/auth/me', {
            credentials: 'same-origin',
            headers: { 'Accept': 'application/json' }
        });
        if (!res.ok) {
            memberState = null;
            return;
        }
        const json = await res.json();
        if (json?.success && json.data) {
            memberState = json.data;
        }
    } catch {
        memberState = null;
    }
})();

const isLoggedIn = () => memberState !== null && memberState.role === 'MEMBER';
const getMemberInfo = () => (memberState && memberState.role === 'MEMBER') ? memberState : null;

const handleLogout = async () => {
    try {
        await fetch('/api/auth/logout', {
            method: 'POST',
            credentials: 'same-origin'
        });
    } catch {
        // 서버 오류와 무관하게 클라이언트 상태 리셋
    }
    memberState = null;
    window.location.href = '/';
};

const openWithdrawDialog = () => {
    const menu = document.getElementById('profile-menu');
    if (menu) menu.classList.add('hidden');
    const dialog = document.getElementById('withdraw-dialog');
    if (dialog) dialog.classList.remove('hidden');
};

const closeWithdrawDialog = () => {
    const dialog = document.getElementById('withdraw-dialog');
    if (dialog) dialog.classList.add('hidden');
};

const confirmWithdraw = async () => {
    const btn = document.querySelector('#withdraw-card button:last-child');
    if (btn) { btn.disabled = true; btn.textContent = '처리 중...'; }
    try {
        const res = await fetch('/api/auth/me', {
            method: 'DELETE',
            credentials: 'same-origin'
        });
        if (!res.ok) throw new Error();
        memberState = null;
        window.location.href = '/';
    } catch {
        if (btn) { btn.disabled = false; btn.textContent = '탈퇴하기'; }
        alert('탈퇴 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.');
    }
};

// ── 페이지 로드 시 로그인 상태에 따라 네비게이션 전환 ────────────────────────

document.addEventListener('DOMContentLoaded', async () => {
    await authReady;

    const navGuest  = document.getElementById('nav-guest');
    const navMember = document.getElementById('nav-member');
    if (navGuest && navMember && isLoggedIn()) {
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
