// Shared UI helpers: nav, auth guard, toasts, formatting.
const UI = (() => {

    const NAV = [
        { href: 'dashboard.html',   icon: 'speedometer2', label: 'Dashboard' },
        { href: 'products.html',    icon: 'box-seam',      label: 'Products' },
        { href: 'suppliers.html',   icon: 'truck',         label: 'Suppliers' },
        { href: 'stock.html',       icon: 'arrow-left-right', label: 'Stock' },
        { href: 'sales.html',       icon: 'cart-check',    label: 'Sales' },
        { href: 'predictions.html', icon: 'graph-up-arrow', label: 'Predictions' },
        { href: 'reports.html',     icon: 'file-earmark-spreadsheet', label: 'Reports' },
    ];

    function guard() {
        if (!API.isLoggedIn()) { location.href = 'login.html'; return false; }
        return true;
    }

    function hasRole(...roles) {
        const u = API.getUser();
        if (!u || !u.roles) return false;
        return roles.some(r => u.roles.includes(r) || u.roles.includes('ROLE_' + r));
    }

    function renderLayout(activeHref) {
        const user = API.getUser() || {};
        const links = NAV.map(n => `
            <a href="${n.href}" class="${n.href === activeHref ? 'active' : ''}">
                <i class="bi bi-${n.icon} me-2"></i>${n.label}
            </a>`).join('');

        const sidebar = document.createElement('div');
        sidebar.className = 'si-sidebar';
        sidebar.innerHTML = `
            <div class="brand"><i class="bi bi-boxes me-2"></i>Smart Inventory</div>
            ${links}
            <a href="#" id="logoutBtn" class="mt-3"><i class="bi bi-box-arrow-right me-2"></i>Logout</a>
        `;
        document.body.prepend(sidebar);
        document.getElementById('logoutBtn').addEventListener('click', (e) => {
            e.preventDefault(); API.clear(); location.href = 'login.html';
        });

        const badge = document.getElementById('userBadge');
        if (badge) badge.textContent = `${user.fullName || user.username || ''} (${(user.roles || []).join(', ').replace(/ROLE_/g, '')})`;
    }

    function toast(message, type = 'success') {
        let container = document.querySelector('.toast-container');
        if (!container) {
            container = document.createElement('div');
            container.className = 'toast-container position-fixed top-0 end-0 p-3';
            document.body.appendChild(container);
        }
        const el = document.createElement('div');
        el.className = `toast align-items-center text-bg-${type === 'error' ? 'danger' : type} border-0 show`;
        el.role = 'alert';
        el.innerHTML = `<div class="d-flex"><div class="toast-body">${message}</div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button></div>`;
        container.appendChild(el);
        setTimeout(() => el.remove(), 4000);
        el.querySelector('.btn-close').addEventListener('click', () => el.remove());
    }

    const money = (v) => (v == null ? '0.00' : Number(v).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 }));
    const num = (v) => (v == null ? '0' : Number(v).toLocaleString());

    return { guard, hasRole, renderLayout, toast, money, num };
})();
