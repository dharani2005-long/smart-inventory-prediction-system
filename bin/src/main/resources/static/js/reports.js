if (UI.guard()) {
    UI.renderLayout('reports.html');
}

window.dl = async (path, filename) => {
    try { await API.download(path, filename); UI.toast('Download started'); }
    catch (e) { UI.toast(e.message, 'error'); }
};

window.downloadSales = async () => {
    const start = document.getElementById('salesStart').value;
    const end = document.getElementById('salesEnd').value;
    let path = '/reports/sales';
    const params = [];
    if (start) params.push('start=' + start);
    if (end) params.push('end=' + end);
    if (params.length) path += '?' + params.join('&');
    await window.dl(path, 'sales-report.xlsx');
};
