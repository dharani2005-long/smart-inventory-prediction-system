let supplierModal, productsModal, currentPage = 0;
const canEdit = () => UI.hasRole('ADMIN', 'MANAGER');
const canDelete = () => UI.hasRole('ADMIN');

if (UI.guard()) {
    UI.renderLayout('suppliers.html');
    supplierModal = new bootstrap.Modal(document.getElementById('supplierModal'));
    productsModal = new bootstrap.Modal(document.getElementById('productsModal'));
    document.getElementById('addBtn').addEventListener('click', () => openModal());
    if (!canEdit()) document.getElementById('addBtn').style.display = 'none';
    document.getElementById('searchInput').addEventListener('keyup', (e) => { if (e.key === 'Enter') loadSuppliers(0); });
    document.getElementById('supplierForm').addEventListener('submit', save);
    loadSuppliers(0);
}

async function loadSuppliers(page) {
    currentPage = page;
    const kw = document.getElementById('searchInput').value.trim();
    let url = `/suppliers?page=${page}&size=10`;
    if (kw) url += `&keyword=${encodeURIComponent(kw)}`;
    try {
        const data = await API.get(url);
        renderTable(data.content);
        renderPager(data);
    } catch (e) { UI.toast(e.message, 'error'); }
}

function renderTable(items) {
    if (!items.length) {
        document.getElementById('supplierTable').innerHTML =
            '<tr><td colspan="6" class="text-center text-muted py-4">No suppliers found</td></tr>';
        return;
    }
    document.getElementById('supplierTable').innerHTML = items.map(s => `
        <tr>
            <td>${s.name}</td>
            <td>${s.contactPerson || '-'}</td>
            <td>${s.email || '-'}</td>
            <td>${s.phone || '-'}</td>
            <td><span class="badge ${s.active ? 'badge-ok' : 'bg-secondary'}">${s.active ? 'Active' : 'Inactive'}</span></td>
            <td class="text-end">
                <button class="btn btn-sm btn-outline-info" onclick="viewProducts(${s.id})"><i class="bi bi-box-seam"></i></button>
                ${canEdit() ? `<button class="btn btn-sm btn-outline-primary" onclick='editSupplier(${JSON.stringify(s)})'><i class="bi bi-pencil"></i></button>` : ''}
                ${canDelete() ? `<button class="btn btn-sm btn-outline-danger" onclick="deleteSupplier(${s.id})"><i class="bi bi-trash"></i></button>` : ''}
            </td>
        </tr>`).join('');
}

function renderPager(data) {
    const pager = document.getElementById('pager');
    if (data.totalPages <= 1) { pager.innerHTML = ''; return; }
    let html = '';
    for (let i = 0; i < data.totalPages; i++)
        html += `<li class="page-item ${i === data.page ? 'active' : ''}"><a class="page-link" href="#" onclick="loadSuppliers(${i});return false;">${i + 1}</a></li>`;
    pager.innerHTML = html;
}

function openModal(s) {
    document.getElementById('supplierForm').reset();
    document.getElementById('supplierId').value = s ? s.id : '';
    document.getElementById('modalTitle').textContent = s ? 'Edit Supplier' : 'Add Supplier';
    if (s) { sName.value = s.name; sContact.value = s.contactPerson || ''; sEmail.value = s.email || ''; sPhone.value = s.phone || ''; sAddress.value = s.address || ''; }
    supplierModal.show();
}
window.editSupplier = (s) => openModal(s);

async function save(e) {
    e.preventDefault();
    const id = document.getElementById('supplierId').value;
    const body = {
        name: sName.value.trim(), contactPerson: sContact.value.trim(),
        email: sEmail.value.trim() || null, phone: sPhone.value.trim(), address: sAddress.value.trim()
    };
    try {
        if (id) await API.put(`/suppliers/${id}`, body);
        else await API.post('/suppliers', body);
        supplierModal.hide();
        UI.toast('Supplier saved');
        loadSuppliers(currentPage);
    } catch (err) { UI.toast(err.message, 'error'); }
}

window.deleteSupplier = async (id) => {
    if (!confirm('Deactivate this supplier?')) return;
    try { await API.del(`/suppliers/${id}`); UI.toast('Supplier deactivated'); loadSuppliers(currentPage); }
    catch (e) { UI.toast(e.message, 'error'); }
};

window.viewProducts = async (id) => {
    try {
        const products = await API.get(`/suppliers/${id}/products`);
        document.getElementById('linkedProducts').innerHTML = products.length ? `
            <table class="table table-sm">
                <thead><tr><th>SKU</th><th>Name</th><th class="text-end">Stock</th></tr></thead>
                <tbody>${products.map(p => `<tr><td><code>${p.sku}</code></td><td>${p.name}</td><td class="text-end">${UI.num(p.currentStock)}</td></tr>`).join('')}</tbody>
            </table>` : '<p class="text-muted">No products linked to this supplier.</p>';
        productsModal.show();
    } catch (e) { UI.toast(e.message, 'error'); }
};
