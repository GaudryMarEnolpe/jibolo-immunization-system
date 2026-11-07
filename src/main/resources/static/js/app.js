document.addEventListener('DOMContentLoaded', () => {
  // highlight current menu item
  document.querySelectorAll('.navbar-app .nav-link[href]').forEach(a => { if (a.pathname === location.pathname) a.classList.add('active'); });
  // show / hide password
  document.querySelectorAll('[data-toggle-pass]').forEach(b => b.addEventListener('click', () => {
    const i = document.querySelector(b.dataset.togglePass); i.type = i.type === 'password' ? 'text' : 'password';
    b.firstElementChild.className = i.type === 'password' ? 'bi bi-eye' : 'bi bi-eye-slash';
  }));
  // search bar + filter dropdown on every table.data-table
  document.querySelectorAll('table.data-table').forEach(t => {
    const body = t.tBodies[0]; if (!body) return;
    const tools = document.createElement('div'); tools.className = 'table-tools d-flex flex-wrap gap-2 p-3 no-print';
    const search = document.createElement('input'); search.type = 'search'; search.className = 'form-control'; search.placeholder = 'Search...'; search.style.maxWidth = '280px';
    tools.append(search);
    const col = t.dataset.filterCol; let sel = null;
    const val = r => { const c = r.cells[col]; return c ? (c.dataset.filter || c.textContent.trim()) : ''; };
    if (col !== undefined) {
      sel = document.createElement('select'); sel.className = 'form-select searchable'; sel.style.maxWidth = '230px';
      sel.append(new Option(t.dataset.filterLabel || 'All', ''));
      [...new Set([...body.rows].map(val).filter(Boolean))].sort().forEach(v => sel.append(new Option(v, v)));
      tools.append(sel);
    }
    const apply = () => {
      const q = search.value.toLowerCase(), f = sel ? sel.value : '';
      [...body.rows].forEach(r => { r.style.display = ((!q || r.textContent.toLowerCase().includes(q)) && (!f || val(r) === f)) ? '' : 'none'; });
    };
    search.addEventListener('input', apply); if (sel) sel.addEventListener('change', apply);
    (t.closest('.table-responsive') || t).before(tools);
  });
  // searchable dropdowns (type to find a child, vaccine or category)
  if (window.TomSelect) document.querySelectorAll('select.searchable').forEach(s => new TomSelect(s, { dropdownParent: 'body', allowEmptyOption: true, maxOptions: 1000 }));
});
