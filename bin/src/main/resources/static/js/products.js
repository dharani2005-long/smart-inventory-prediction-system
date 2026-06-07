let productModal, currentPage = 0;
const canEdit = () => UI.hasRole('ADMIN', 'MANAGER');
const canDelete = () => UI.hasRole('ADMIN');

if (UI.guard()) {
    UI.renderLayout('products.html');
    productModal = new bootstrap.Modal(document.getElementById('productModal'));
    init();
}

async function init() {
    await loadDropdowns();
    document.getElementById('addBtn').addEventListener('click', () => openModal());
    if (!canEdit()) document.getElementById('addBtn').style.display = 'none';
    document.getElementById('searchInput').addEventListener('keyup', (e) => { if (e.key === 'Enter') loadProducts(0); });
    document.getElementById('productForm').addEventListener('submit', save);
    loadProducts(0);
}

async function loadDropdowns() {
    const cats = await API.get('/categories/all');
    const opts = cats.map(c => `<option value="${c.id}">${c.name}</option>`).join('');
    document.getElementById('categoryFilter').insertAdjacentHTML('beforeend', opts);
    document.getElementById('pCategory').insertAdjacentHTML('beforeend', opts);

    const sup = await API.get('/suppliers?size=100');
    const sopts = sup.content.map(s => `<option value="${s.id}">${s.name}</option>`).join('');
    document.getElementById('pSupplier').insertAdjacentHTML('beforeend', sopts);
}

async function loadProducts(page) {
    currentPage = page;
    const kw = document.getElementById('searchInput').value.trim();
    const cat = document.getElementById('categoryFilter').value;
    let url = `/products?page=${page}&size=10`;
    if (kw) url += `&keyword=${encodeURIComponent(kw)}`;
    if (cat) url += `&categoryId=${cat}`;

    try {
        const data = await API.get(url);
        renderTable(data.content);
        renderPager(data);
    } catch (e) { UI.toast(e.message, 'error'); }
}

function renderTable(items) {
    if (!items.length) {
        document.getElementById('productTable').innerHTML =
            '<tr><td colspan="8" class="text-center text-muted py-4">No products found</td></tr>';
        return;
    }
    document.getElementById('productTable').innerHTML = items.map(p => `
        <tr>
            <td><code>${p.sku}</code></td>
            <td>${p.name}</td>
            <td>${p.categoryName || '-'}</td>
            <td>${p.supplierName || '-'}</td>
            <td class="text-end">$${UI.money(p.unitPrice)}</td>
            <td class="text-end">${UI.num(p.currentStock)}</td>
            <td><span class="badge ${p.lowStock ? 'badge-low' : 'badge-ok'}">${p.lowStock ? 'Low' : 'OK'}</span></td>
            <td class="text-end">
                ${canEdit() ? `<button class="btn btn-sm btn-outline-primary" onclick='editProduct(${JSON.stringify(p)})'><i class="bi bi-pencil"></i></button>` : ''}
                ${canDelete() ? `<button class="btn btn-sm btn-outline-danger" onclick="deleteProduct(${p.id})"><i class="bi bi-trash"></i></button>` : ''}
            </td>
        </tr>`).join('');
}

function renderPager(data) {
    const pager = document.getElementById('pager');
    const buttons = [];
    buttons.push(`<li class="page-item ${data.first ? 'disabled' : ''}"><a class="page-link" href="#" onclick="loadProducts(${data.page - 1});return false;">«</a></li>`);
    for (let i = 0; i < data.totalPages; i++) {
        buttons.push(`<li class="page-item ${i === data.page ? 'active' : ''}"><a class="page-link" href="#" onclick="loadProducts(${i});return false;">${i + 1}</a></li>`);
    }
    buttons.push(`<li class="page-item ${data.last ? 'disabled' : ''}"><a class="page-link" href="#" onclick="loadProducts(${data.page + 1});return false;">»</a></li>`);
    pager.innerHTML = data.totalPages > 1 ? buttons.join('') : '';
}

function openModal(p) {
    document.getElementById('productForm').reset();
    document.getElementById('productId').value = p ? p.id : '';
    document.getElementById('modalTitle').textContent = p ? 'Edit Product' : 'Add Product';
    document.getElementById('pOpening').parentElement.style.display = p ? 'none' : 'block';
    if (p) {
        pName.value = p.name; pSku.value = p.sku; pBarcode.value = p.barcode || '';
        pDesc.value = p.description || ''; pPrice.value = p.unitPrice; pCost.value = p.costPrice || '';
        pReorder.value = p.reorderLevel;
        pCategory.value = p.categoryId || ''; pSupplier.value = p.supplierId || '';
    }
    productModal.show();
}
window.editProduct = (p) => openModal(p);

async function save(e) {
    e.preventDefault();
    const id = document.getElementById('productId').value;
    const body = {
        name: pName.value.trim(), sku: pSku.value.trim(),
        barcode: pBarcode.value.trim() || null, description: pDesc.value.trim(),
        unitPrice: parseFloat(pPrice.value), costPrice: pCost.value ? parseFloat(pCost.value) : null,
        reorderLevel: parseInt(pReorder.value),
        categoryId: pCategory.value || null, supplierId: pSupplier.value || null,
        openingStock: id ? null : parseInt(pOpening.value || '0')
    };
    try {
        if (id) await API.put(`/products/${id}`, body);
        else await API.post('/products', body);
        productModal.hide();
        UI.toast('Product saved');
        loadProducts(currentPage);
    } catch (err) { UI.toast(err.message, 'error'); }
}

window.deleteProduct = async (id) => {
    if (!confirm('Deactivate this product?')) return;
    try { await API.del(`/products/${id}`); UI.toast('Product deactivated'); loadProducts(currentPage); }
    catch (e) { UI.toast(e.message, 'error'); }
};
