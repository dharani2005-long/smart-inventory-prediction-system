let forecasts = [];

if (UI.guard()) {
    UI.renderLayout('predictions.html');
    document.getElementById('runBtn').addEventListener('click', run);
    if (!UI.hasRole('ADMIN', 'MANAGER')) document.getElementById('runBtn').style.display = 'none';
    load();
}

async function load() {
    try {
        forecasts = await API.get('/predictions');
        render();
    } catch (e) { UI.toast(e.message, 'error'); }
}

async function run() {
    try {
        UI.toast('Running forecast...');
        forecasts = await API.post('/predictions/run');
        UI.toast('Forecast updated');
        render();
    } catch (e) { UI.toast(e.message, 'error'); }
}

function confidenceBar(pct) {
    const color = pct >= 70 ? 'bg-success' : pct >= 40 ? 'bg-warning' : 'bg-danger';
    return `<div class="progress" style="height:18px;min-width:90px">
        <div class="progress-bar ${color}" style="width:${pct}%">${pct.toFixed(0)}%</div></div>`;
}

function render() {
    const alertsOnly = document.getElementById('alertsOnly').checked;
    const rows = forecasts.filter(f => !alertsOnly || f.lowStock || f.recommendedReorderQty > 0);
    document.getElementById('forecastTable').innerHTML = rows.length ? rows.map(f => `
        <tr class="${f.lowStock ? 'table-danger' : ''}">
            <td><b>${f.productName}</b><br><small class="text-muted">${f.sku}</small></td>
            <td class="text-end">${f.avgDailyConsumption.toFixed(2)}</td>
            <td class="text-end">${UI.num(f.forecastDemand)}</td>
            <td class="text-end">${UI.num(f.currentStock)}</td>
            <td>${f.depletionExpectedOn || '<span class="text-muted">—</span>'}</td>
            <td class="text-end">${f.recommendedReorderQty > 0 ? `<span class="badge badge-low">${f.recommendedReorderQty}</span>` : '0'}</td>
            <td>${confidenceBar(f.confidencePercent)}</td>
            <td><small>${f.recommendation}</small></td>
        </tr>`).join('') :
        '<tr><td colspan="8" class="text-center text-muted py-4">No forecasts available</td></tr>';
}
