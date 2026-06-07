let dailyChart, monthlyChart;

if (UI.guard()) {
    UI.renderLayout('sales.html');
    init();
}

async function init() {
    await loadProductOptions();
    document.getElementById('saleForm').addEventListener('submit', record);
    loadRecent();
    loadDaily();
}

async function loadProductOptions() {
    const data = await API.get('/products?size=200');
    document.getElementById('sProduct').innerHTML =
        data.content.map(p => `<option value="${p.id}">${p.name} (${p.sku}) — $${UI.money(p.unitPrice)}</option>`).join('');
}

async function record(e) {
    e.preventDefault();
    const body = {
        productId: parseInt(sProduct.value),
        quantity: parseInt(sQty.value),
        saleDate: sDate.value || null,
        invoiceNo: sInvoice.value.trim() || null
    };
    try {
        await API.post('/sales', body);
        UI.toast('Sale recorded');
        document.getElementById('saleForm').reset();
        loadRecent(); loadDaily();
    } catch (err) { UI.toast(err.message, 'error'); }
}

async function loadRecent() {
    const data = await API.get('/sales?size=10&sort=saleDate,desc');
    document.getElementById('salesTable').innerHTML = data.content.length ? data.content.map(s => `
        <tr><td>${s.saleDate}</td><td>${s.productName}</td>
        <td class="text-end">${s.quantity}</td><td class="text-end">$${UI.money(s.totalAmount)}</td>
        <td>${s.invoiceNo || '-'}</td></tr>`).join('') :
        '<tr><td colspan="5" class="text-center text-muted py-3">No sales yet</td></tr>';
}

async function loadDaily() {
    const data = await API.get('/sales/reports/daily');
    if (dailyChart) dailyChart.destroy();
    dailyChart = new Chart(document.getElementById('dailyChart'), {
        type: 'bar',
        data: { labels: data.map(d => d.date), datasets: [{ label: 'Revenue', data: data.map(d => d.totalAmount), backgroundColor: '#2a9d8f' }] },
        options: { plugins: { legend: { display: false } } }
    });
    document.getElementById('dailyTable').innerHTML = table(['Date', 'Qty', 'Revenue', 'Txns'],
        data.map(d => [d.date, d.totalQuantity, '$' + UI.money(d.totalAmount), d.transactionCount]));
}

async function loadMonthly() {
    const data = await API.get('/sales/reports/monthly');
    if (monthlyChart) monthlyChart.destroy();
    monthlyChart = new Chart(document.getElementById('monthlyChart'), {
        type: 'line',
        data: { labels: data.map(d => d.monthName), datasets: [{ label: 'Revenue', data: data.map(d => d.totalAmount), borderColor: '#457b9d', tension: .3 }] },
        options: { plugins: { legend: { display: false } } }
    });
    document.getElementById('monthlyTable').innerHTML = table(['Month', 'Qty', 'Revenue'],
        data.map(d => [d.monthName, d.totalQuantity, '$' + UI.money(d.totalAmount)]));
}

async function loadProductWise() {
    const data = await API.get('/sales/reports/product-wise');
    document.getElementById('productTable').innerHTML = table(['Product', 'SKU', 'Qty Sold', 'Revenue'],
        data.map(d => [d.productName, d.sku, d.totalQuantity, '$' + UI.money(d.totalAmount)]));
}

function table(headers, rows) {
    if (!rows.length) return '<p class="text-muted">No data for this period.</p>';
    return `<table class="table table-sm table-striped">
        <thead><tr>${headers.map(h => `<th>${h}</th>`).join('')}</tr></thead>
        <tbody>${rows.map(r => `<tr>${r.map(c => `<td>${c}</td>`).join('')}</tr>`).join('')}</tbody></table>`;
}
