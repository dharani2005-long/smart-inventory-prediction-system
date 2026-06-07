if (UI.guard()) {
    UI.renderLayout('dashboard.html');
    loadDashboard();
}

async function loadDashboard() {
    try {
        const d = await API.get('/dashboard');
        renderKpis(d);
        renderPrediction(d.predictionSummary);
        renderTrend(d.salesTrend);
        renderTop(d.topProducts);
    } catch (e) {
        UI.toast(e.message, 'error');
    }
}

function renderKpis(d) {
    const kpis = [
        { label: 'Total Products', value: UI.num(d.totalProducts), cls: 'bg-grad-1', icon: 'box-seam' },
        { label: 'Total Suppliers', value: UI.num(d.totalSuppliers), cls: 'bg-grad-2', icon: 'truck' },
        { label: 'Low Stock', value: UI.num(d.lowStockProducts), cls: 'bg-grad-5', icon: 'exclamation-triangle' },
        { label: 'Monthly Sales', value: '$' + UI.money(d.monthlySales), cls: 'bg-grad-3', icon: 'cart-check' },
        { label: 'Inventory Value', value: '$' + UI.money(d.inventoryValue), cls: 'bg-grad-4', icon: 'cash-stack' },
    ];
    document.getElementById('kpiRow').innerHTML = kpis.map(k => `
        <div class="col">
            <div class="card kpi-card ${k.cls}"><div class="card-body">
                <div class="d-flex justify-content-between align-items-center">
                    <div>
                        <div class="kpi-value">${k.value}</div>
                        <div class="kpi-label">${k.label}</div>
                    </div>
                    <i class="bi bi-${k.icon} fs-2 opacity-50"></i>
                </div>
            </div></div>
        </div>`).join('');
}

function renderPrediction(p) {
    if (!p) return;
    const items = [
        { label: 'Products Needing Reorder', value: p.productsNeedingReorder, color: 'text-danger' },
        { label: 'Predicted Low Stock', value: p.productsPredictedLowStock, color: 'text-warning' },
        { label: 'Avg. Confidence', value: p.averageConfidence.toFixed(1) + '%', color: 'text-success' },
    ];
    document.getElementById('predRow').innerHTML = items.map(i => `
        <div class="col">
            <div class="fs-3 fw-bold ${i.color}">${i.value}</div>
            <div class="text-muted small">${i.label}</div>
        </div>`).join('');
}

function renderTrend(trend) {
    new Chart(document.getElementById('trendChart'), {
        type: 'line',
        data: {
            labels: trend.map(t => t.label),
            datasets: [{
                label: 'Revenue',
                data: trend.map(t => t.value),
                borderColor: '#2a9d8f',
                backgroundColor: 'rgba(42,157,143,.15)',
                fill: true, tension: .3
            }]
        },
        options: { plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true } } }
    });
}

function renderTop(top) {
    new Chart(document.getElementById('topChart'), {
        type: 'bar',
        data: {
            labels: top.map(t => t.label),
            datasets: [{ label: 'Revenue', data: top.map(t => t.value), backgroundColor: '#457b9d' }]
        },
        options: { indexAxis: 'y', plugins: { legend: { display: false } }, scales: { x: { beginAtZero: true } } }
    });
}
