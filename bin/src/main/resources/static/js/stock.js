let currentPage = 0;

if (UI.guard()) {
    UI.renderLayout('stock.html');
    init();
}

async function init() {
    await loadProductOptions();
    document.getElementById('stockForm').addEventListener('submit', record);
    loadTxns(0);
}

async function loadProductOptions() {
    const data = await API.get('/products?size=200');
    document.getElementById('tProduct').innerHTML =
        data.content.map(p => `<option value="${p.id}">${p.name} (${p.sku}) — stock ${p.currentStock}</option>`).join('');
}

async function record(e) {
    e.preventDefault();
    const body = {
        productId: parseInt(tProduct.value),
        type: tType.value,
        quantity: parseInt(tQty.value),
        referenceNo: tRef.value.trim() || null,
        note: tNote.value.trim() || null
    };
    try {
        await API.post('/stock-transactions', body);
        UI.toast('Transaction recorded');
        document.getElementById('stockForm').reset();
        await loadProductOptions();
        loadTxns(0);
    } catch (err) { UI.toast(err.message, 'error'); }
}

async function loadTxns(page) {
    currentPage = page;
    try {
        const data = await API.get(`/stock-transactions?page=${page}&size=12&sort=createdAt,desc`);
        const typeBadge = { STOCK_IN: 'badge-ok', RETURN: 'badge-ok', STOCK_OUT: 'badge-low', ADJUSTMENT: 'bg-secondary' };
        document.getElementById('txnTable').innerHTML = data.content.length ? data.content.map(t => `
            <tr>
                <td>${t.createdAt ? t.createdAt.substring(0, 10) : '-'}</td>
                <td>${t.productName}</td>
                <td><span class="badge ${typeBadge[t.type]}">${t.type}</span></td>
                <td class="text-end ${t.quantity < 0 ? 'text-danger' : 'text-success'}">${t.quantity}</td>
                <td class="text-end">${UI.num(t.balanceAfter)}</td>
                <td>${t.performedBy || '-'}</td>
            </tr>`).join('') : '<tr><td colspan="6" class="text-center text-muted py-3">No transactions</td></tr>';
        renderPager(data);
    } catch (e) { UI.toast(e.message, 'error'); }
}

function renderPager(data) {
    const pager = document.getElementById('pager');
    if (data.totalPages <= 1) { pager.innerHTML = ''; return; }
    let html = '';
    for (let i = 0; i < data.totalPages; i++)
        html += `<li class="page-item ${i === data.page ? 'active' : ''}"><a class="page-link" href="#" onclick="loadTxns(${i});return false;">${i + 1}</a></li>`;
    pager.innerHTML = html;
}
