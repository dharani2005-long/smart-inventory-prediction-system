// Lightweight API client with JWT handling.
const API = (() => {
    const BASE = '/api';
    const TOKEN_KEY = 'si_token';
    const USER_KEY = 'si_user';

    const getToken = () => localStorage.getItem(TOKEN_KEY);
    const setToken = (t) => localStorage.setItem(TOKEN_KEY, t);
    const getUser = () => JSON.parse(localStorage.getItem(USER_KEY) || 'null');
    const setUser = (u) => localStorage.setItem(USER_KEY, JSON.stringify(u));
    const clear = () => { localStorage.removeItem(TOKEN_KEY); localStorage.removeItem(USER_KEY); };

    async function request(path, { method = 'GET', body, auth = true } = {}) {
        const headers = { 'Content-Type': 'application/json' };
        if (auth && getToken()) headers['Authorization'] = 'Bearer ' + getToken();

        const res = await fetch(BASE + path, {
            method,
            headers,
            body: body ? JSON.stringify(body) : undefined
        });

        if (res.status === 401 && auth) {
            clear();
            if (!location.pathname.endsWith('login.html')) location.href = 'login.html';
            throw new Error('Unauthorized');
        }

        const text = await res.text();
        const data = text ? JSON.parse(text) : null;
        if (!res.ok) {
            const msg = (data && (data.message || data.error)) || ('Request failed (' + res.status + ')');
            throw new Error(msg);
        }
        return data;
    }

    // Authenticated binary download (Excel).
    async function download(path, filename) {
        const res = await fetch(BASE + path, {
            headers: getToken() ? { 'Authorization': 'Bearer ' + getToken() } : {}
        });
        if (!res.ok) throw new Error('Download failed (' + res.status + ')');
        const blob = await res.blob();
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url; a.download = filename;
        document.body.appendChild(a); a.click(); a.remove();
        URL.revokeObjectURL(url);
    }

    return {
        get: (p) => request(p),
        post: (p, body, opts) => request(p, { method: 'POST', body, ...(opts || {}) }),
        put: (p, body) => request(p, { method: 'PUT', body }),
        del: (p) => request(p, { method: 'DELETE' }),
        download,
        getToken, setToken, getUser, setUser, clear,
        isLoggedIn: () => !!getToken()
    };
})();
