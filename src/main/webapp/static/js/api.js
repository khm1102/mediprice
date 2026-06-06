// mp_token이 HttpOnly 쿠키이므로 JS는 토큰 값을 읽지 못한다.
// 인증은 same-origin 요청에 쿠키가 자동 첨부되는 것에 의존하며 Authorization 헤더는 보내지 않는다.
//
// 두 번째 인자 { signal } 옵션으로 AbortController를 전달할 수 있다.
// 호출처는 빠른 키워드 변경/패널 전환 시 옛 요청을 abort해 race 조건을 차단한다.

const api = {
    async get(url, { signal } = {}) {
        const res = await fetch(url, {
            credentials: 'same-origin',
            headers: { 'Accept': 'application/json' },
            signal
        });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.json();
    },

    async post(url, data, { signal } = {}) {
        const res = await fetch(url, {
            method: 'POST',
            credentials: 'same-origin',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify(data),
            signal
        });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.json();
    },

    async delete(url, { signal } = {}) {
        const res = await fetch(url, {
            method: 'DELETE',
            credentials: 'same-origin',
            headers: { 'Accept': 'application/json' },
            signal
        });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.json();
    }
};
