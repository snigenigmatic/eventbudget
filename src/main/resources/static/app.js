/* ══════════════════════════════════════════════════════════
   EventBudget — Single-Page Application
   ══════════════════════════════════════════════════════════ */

// ── State ──────────────────────────────────────────────────
var S = { token: null, user: null };

// ── DOM helpers ────────────────────────────────────────────
function $(id) { return document.getElementById(id); }

// ── Format helpers ─────────────────────────────────────────
function fmt(v) {
  return '\u20B9' + Number(v).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}
function fmtDate(s) { return s ? new Date(s).toLocaleDateString('en-IN') : '\u2014'; }
function fmtDateTime(s) { return s ? new Date(s).toLocaleString('en-IN') : '\u2014'; }

function statusPill(status) {
  var map = {
    APPROVED:        'bg-success',
    CLOSED:          'bg-secondary',
    PENDING_APPROVAL:'bg-warning text-dark',
    REJECTED:        'bg-danger',
    AUTO_APPROVED:   'bg-info text-dark',
    SINGLE_LEVEL:    'bg-info text-dark',
    MULTI_LEVEL:     'bg-primary',
  };
  var cls = map[status] || 'bg-secondary';
  return '<span class="badge ' + cls + '">' + status.replace(/_/g, ' ') + '</span>';
}

// ── Toast ──────────────────────────────────────────────────
function toast(msg, type) {
  type = type || 'success';
  var t = $('toast');
  t.textContent = msg;
  t.className = type + ' visible';
  clearTimeout(t._tid);
  t._tid = setTimeout(function () { t.className = ''; }, 3500);
}

// ── API helper ─────────────────────────────────────────────
async function api(method, path, body) {
  var headers = { 'Content-Type': 'application/json' };
  if (S.token) headers['Authorization'] = 'Bearer ' + S.token;
  var opts = { method: method, headers: headers };
  if (body !== undefined) opts.body = JSON.stringify(body);
  var res = await fetch(path, opts);
  if (res.status === 204) return null;
  if (res.status === 401 || res.status === 403) {
    clearSession();
    showAuth('login');
    throw new Error('Session expired. Please log in again.');
  }
  var text = await res.text();
  var data = text ? JSON.parse(text) : null;
  if (!res.ok) throw new Error((data && data.message) || 'Error ' + res.status);
  return data;
}

async function apiBlob(path) {
  var res = await fetch(path, { headers: { 'Authorization': 'Bearer ' + S.token } });
  if (!res.ok) throw new Error('Export failed (' + res.status + ')');
  var cd = res.headers.get('Content-Disposition') || '';
  var m  = cd.match(/filename="?([^"]+)"?/);
  return { blob: await res.blob(), filename: m ? m[1] : 'export' };
}

function downloadBlob(blob, filename) {
  var url = URL.createObjectURL(blob);
  var a = document.createElement('a');
  a.href = url; a.download = filename; a.click();
  URL.revokeObjectURL(url);
}

// ── Modal ──────────────────────────────────────────────────
function openModal(title, html, onSubmit) {
  $('modal-title').textContent = title;
  $('modal-form').innerHTML = html;
  $('modal-overlay').classList.add('open');
  $('modal-form').onsubmit = async function (e) {
    e.preventDefault();
    var btn = e.target.querySelector('button[type="submit"]');
    if (btn) { btn.disabled = true; btn.textContent = 'Saving…'; }
    try {
      await onSubmit(new FormData(e.target));
      closeModal();
    } catch (err) {
      toast(err.message, 'error');
      if (btn) { btn.disabled = false; btn.textContent = 'Submit'; }
    }
  };
}

function closeModal() {
  $('modal-overlay').classList.remove('open');
}

// ── Auth persistence ───────────────────────────────────────
function saveSession(data) {
  S.token = data.token;
  S.user  = { id: data.userId, name: data.name, email: data.email, role: data.role };
  localStorage.setItem('eb_token', data.token);
  localStorage.setItem('eb_user',  JSON.stringify(S.user));
}

function clearSession() {
  S.token = null; S.user = null;
  localStorage.removeItem('eb_token');
  localStorage.removeItem('eb_user');
}

function restoreSession() {
  var t = localStorage.getItem('eb_token');
  var u = localStorage.getItem('eb_user');
  if (t && u) { S.token = t; S.user = JSON.parse(u); return true; }
  return false;
}

// ── Auth forms ─────────────────────────────────────────────
async function doLogin(e) {
  e.preventDefault();
  var fd = new FormData(e.target);
  try {
    var data = await api('POST', '/api/auth/login', { email: fd.get('email'), password: fd.get('password') });
    saveSession(data);
    showApp();
    toast('Welcome, ' + S.user.name + '!');
  } catch (err) {
    toast(err.message, 'error');
  }
}

async function doRegister(e) {
  e.preventDefault();
  var fd = new FormData(e.target);
  if (fd.get('password') !== fd.get('confirm')) { toast('Passwords do not match', 'error'); return; }
  try {
    var data = await api('POST', '/api/auth/register', {
      name: fd.get('name'), email: fd.get('email'),
      department: fd.get('department'), password: fd.get('password'),
    });
    saveSession(data);
    showApp();
    toast('Welcome, ' + S.user.name + '!');
  } catch (err) {
    toast(err.message, 'error');
  }
}

function logout() {
  clearSession();
  showAuth('login');
  toast('Signed out', 'info');
}

// ── Section visibility ─────────────────────────────────────
function showAuth(view) {
  view = view || 'login';
  $('auth-section').hidden = false;
  $('app-section').hidden  = true;
  $('login-view').hidden    = (view !== 'login');
  $('register-view').hidden = (view !== 'register');
}

function showApp() {
  $('auth-section').hidden = true;
  $('app-section').hidden  = false;
  $('user-name').textContent  = S.user.name;
  $('user-role').textContent  = S.user.role.replace(/_/g, ' ');
  buildSidebar();
  activateFirstTab();
}

// ── Sidebar ────────────────────────────────────────────────
var TABS = {
  ORGANIZER: [
    { id: 'dashboard', label: '<i class="bi bi-grid-1x2 me-2"></i>Dashboard'              },
    { id: 'events',    label: '<i class="bi bi-calendar-event me-2"></i>Events'           },
    { id: 'budgets',   label: '<i class="bi bi-wallet2 me-2"></i>Budgets'                },
    { id: 'claims',    label: '<i class="bi bi-receipt me-2"></i>Claims'                 },
    { id: 'notifs',    label: '<i class="bi bi-bell me-2"></i>Notifications'             },
  ],
  APPROVER: [
    { id: 'dashboard',    label: '<i class="bi bi-grid-1x2 me-2"></i>Dashboard'          },
    { id: 'pend-budgets', label: '<i class="bi bi-wallet2 me-2"></i>Pending Budgets'     },
    { id: 'pend-claims',  label: '<i class="bi bi-receipt me-2"></i>Pending Claims'      },
    { id: 'notifs',       label: '<i class="bi bi-bell me-2"></i>Notifications'          },
  ],
  FINANCE_ADMIN: [
    { id: 'dashboard', label: '<i class="bi bi-grid-1x2 me-2"></i>Dashboard'             },
    { id: 'workflows', label: '<i class="bi bi-gear me-2"></i>Workflow Config'            },
    { id: 'budgets',   label: '<i class="bi bi-wallet2 me-2"></i>All Budgets'            },
    { id: 'audit',     label: '<i class="bi bi-journal-text me-2"></i>Audit Log'         },
    { id: 'notifs',    label: '<i class="bi bi-bell me-2"></i>Notifications'             },
  ],
};

function buildSidebar() {
  var tabs = TABS[S.user.role] || [];
  var nav  = $('sidebar-nav');
  nav.innerHTML = tabs.map(function (t) {
    return '<button class="nav-side" data-tab="' + t.id + '">' + t.label + '</button>';
  }).join('');
  nav.addEventListener('click', function (e) {
    var btn = e.target.closest('[data-tab]');
    if (btn) activateTab(btn.dataset.tab);
  });
}

function activateFirstTab() {
  var tabs = TABS[S.user.role] || [];
  if (tabs.length) activateTab(tabs[0].id);
}

function activateTab(id) {
  document.querySelectorAll('.nav-side').forEach(function (b) { b.classList.remove('active'); });
  var btn = document.querySelector('[data-tab="' + id + '"]');
  if (btn) btn.classList.add('active');
  loadTab(id);
}

var TAB_HANDLERS = {
  ORGANIZER:    { dashboard: orgDashboard, events: orgEvents, budgets: orgBudgets, claims: orgClaims, notifs: orgNotifs },
  APPROVER:     { dashboard: aprDashboard, 'pend-budgets': aprPendingBudgets, 'pend-claims': aprPendingClaims, notifs: aprNotifs },
  FINANCE_ADMIN:{ dashboard: finDashboard, workflows: finWorkflows, budgets: finBudgets, audit: finAudit, notifs: finNotifs },
};

async function loadTab(id) {
  var fn = (TAB_HANDLERS[S.user.role] || {})[id];
  if (!fn) return;
  setContent('<div class="text-muted"><i class="bi bi-hourglass-split me-1"></i>Loading\u2026</div>');
  try { await fn(); }
  catch (err) { toast(err.message, 'error'); setContent('<div class="alert alert-danger"><i class="bi bi-exclamation-triangle me-1"></i>' + err.message + '</div>'); }
}

function setContent(html) { $('content').innerHTML = html; }

// ── Table builder ──────────────────────────────────────────
function statCard(icon, bgColor, textColor, value, label) {
  return '<div class="col-6 col-lg-3">' +
    '<div class="stat-card">' +
      '<div class="d-flex align-items-start justify-content-between">' +
        '<div>' +
          '<div class="stat-value">' + value + '</div>' +
          '<div class="stat-label">' + label + '</div>' +
        '</div>' +
        '<div class="stat-icon" style="background:' + bgColor + ';color:' + textColor + '">' +
          '<i class="bi ' + icon + '"></i>' +
        '</div>' +
      '</div>' +
    '</div>' +
  '</div>';
}

function buildTable(headers, rows) {
  if (!rows.length) return '<div class="text-center py-5 text-muted">' +
    '<i class="bi bi-inbox" style="font-size:2.5rem;opacity:.35;display:block"></i>' +
    '<p class="mt-2 mb-0 small fw-semibold">No records found</p></div>';
  return '<div class="table-responsive bg-white rounded-3 shadow-sm"><table class="table table-hover table-bordered eb-table mb-0">' +
    '<thead class="table-light"><tr>' + headers.map(function (h) { return '<th>' + h + '</th>'; }).join('') + '</tr></thead>' +
    '<tbody>' + rows.join('') + '</tbody>' +
    '</table></div>';
}


// ══════════════════════════════════════════════════════════
// DASHBOARDS
// ══════════════════════════════════════════════════════════

async function orgDashboard() {
  var results = await Promise.all([
    api('GET', '/api/organizer/events'),
    api('GET', '/api/organizer/budgets'),
    api('GET', '/api/organizer/claims'),
  ]);
  var events  = results[0], budgets = results[1], claims = results[2];
  var approvedBudgets = budgets.filter(function(b) { return b.status === 'APPROVED'; }).length;
  var pendingBudgets  = budgets.filter(function(b) { return b.status === 'PENDING_APPROVAL'; }).length;
  var approvedClaims  = claims.filter(function(c) { return c.status === 'APPROVED' || c.status === 'AUTO_APPROVED'; }).length;
  var pendingClaims   = claims.filter(function(c) { return c.status === 'PENDING_APPROVAL'; }).length;
  var pendingAlerts = '';
  if (pendingBudgets) pendingAlerts += '<div class="d-flex align-items-center gap-2 py-2 border-bottom"><i class="bi bi-clock-history text-warning"></i><span class="small"><strong>' + pendingBudgets + ' budget(s)</strong> awaiting approval.</span></div>';
  if (pendingClaims)  pendingAlerts += '<div class="d-flex align-items-center gap-2 py-2"><i class="bi bi-clock-history text-warning"></i><span class="small"><strong>' + pendingClaims + ' claim(s)</strong> awaiting approval.</span></div>';
  if (!pendingAlerts) pendingAlerts  = '<p class="text-muted small mb-0 py-2"><i class="bi bi-check-circle text-success me-1"></i>No pending actions.</p>';
  setContent(
    '<div class="welcome-banner d-flex align-items-center justify-content-between">' +
      '<div>' +
        '<h5 class="fw-bold mb-1 text-white">Welcome back, ' + S.user.name + '</h5>' +
        '<p class="mb-0 text-white" style="opacity:.8;font-size:13px">Event Organizer &mdash; PES University</p>' +
      '</div>' +
      '<i class="bi bi-person-circle" style="font-size:2.5rem;opacity:.6;color:#fff"></i>' +
    '</div>' +
    '<div class="row g-3 mb-4">' +
      statCard('bi-calendar-event',  '#dbeafe', '#1e40af', events.length,   'Total Events') +
      statCard('bi-wallet2',         '#d1fae5', '#065f46', approvedBudgets, 'Approved Budgets') +
      statCard('bi-hourglass-split', '#fef3c7', '#92400e', pendingBudgets,  'Pending Approval') +
      statCard('bi-receipt-cutoff',  '#f0fdf4', '#166534', approvedClaims,  'Approved Claims') +
    '</div>' +
    '<div class="bg-white rounded-3 shadow-sm p-3 border border-light">' +
      '<div class="fw-semibold mb-2" style="font-size:11px;text-transform:uppercase;letter-spacing:.05em;color:#94a3b8">Pending Actions</div>' +
      pendingAlerts +
    '</div>'
  );
}

async function aprDashboard() {
  var results = await Promise.all([
    api('GET', '/api/approver/budgets/pending'),
    api('GET', '/api/approver/claims/pending'),
  ]);
  var budgets = results[0], claims = results[1];
  var total = budgets.length + claims.length;
  setContent(
    '<div class="welcome-banner d-flex align-items-center justify-content-between">' +
      '<div>' +
        '<h5 class="fw-bold mb-1 text-white">Welcome back, ' + S.user.name + '</h5>' +
        '<p class="mb-0 text-white" style="opacity:.8;font-size:13px">Approving Authority &mdash; PES University</p>' +
      '</div>' +
      '<i class="bi bi-shield-check" style="font-size:2.5rem;opacity:.6;color:#fff"></i>' +
    '</div>' +
    '<div class="row g-3 mb-4">' +
      statCard('bi-wallet2', '#fef3c7', '#92400e', budgets.length, 'Budgets to Review') +
      statCard('bi-receipt', '#fee2e2', '#991b1b', claims.length,  'Claims to Review')  +
    '</div>' +
    (total > 0
      ? '<div class="alert alert-warning d-flex align-items-center gap-2 small">' +
        '<i class="bi bi-exclamation-triangle-fill"></i>' +
        '<span><strong>' + total + ' item(s)</strong> require your review. Check the tabs above.</span></div>'
      : '<div class="alert alert-success d-flex align-items-center gap-2 small">' +
        '<i class="bi bi-check-circle-fill"></i><span>All caught up &mdash; no pending items.</span></div>'
    )
  );
}

async function finDashboard() {
  var results = await Promise.all([
    api('GET', '/api/finance/budgets'),
    api('GET', '/api/finance/audit-logs'),
  ]);
  var budgets = results[0], logs = results[1];
  var open    = budgets.filter(function(b) { return b.status === 'APPROVED'; }).length;
  var closed  = budgets.filter(function(b) { return b.status === 'CLOSED'; }).length;
  var pending = budgets.filter(function(b) { return b.status === 'PENDING_APPROVAL'; }).length;
  setContent(
    '<div class="welcome-banner d-flex align-items-center justify-content-between">' +
      '<div>' +
        '<h5 class="fw-bold mb-1 text-white">Welcome back, ' + S.user.name + '</h5>' +
        '<p class="mb-0 text-white" style="opacity:.8;font-size:13px">Finance Administrator &mdash; PES University</p>' +
      '</div>' +
      '<i class="bi bi-graph-up-arrow" style="font-size:2.5rem;opacity:.6;color:#fff"></i>' +
    '</div>' +
    '<div class="row g-3 mb-4">' +
      statCard('bi-wallet2',       '#dbeafe', '#1e40af', budgets.length, 'Total Budgets') +
      statCard('bi-check-circle',  '#d1fae5', '#065f46', open,           'Approved & Open') +
      statCard('bi-lock',          '#f1f5f9', '#475569', closed,         'Closed') +
      statCard('bi-journal-text',  '#f0fdf4', '#166534', logs.length,    'Audit Events') +
    '</div>'
  );
}

// ══════════════════════════════════════════════════════════
// ORGANIZER
// ══════════════════════════════════════════════════════════

async function orgEvents() {
  var events = await api('GET', '/api/organizer/events');
  var rows = events.map(function (ev) {
    return '<tr>' +
      '<td>' + ev.eventId + '</td>' +
      '<td><strong>' + ev.name + '</strong></td>' +
      '<td>' + (ev.description || '\u2014') + '</td>' +
      '<td>' + fmtDate(ev.eventDate) + '</td>' +
      '<td>' + ev.venue + '</td>' +
      '<td>' + (ev.budgetId
        ? '<span class="pill pill-info">Budget #' + ev.budgetId + '</span>'
        : '<span class="pill pill-neutral">No budget</span>') + '</td>' +
      '</tr>';
  });
  setContent(
    '<div class="page-header"><h4 class="fw-bold mb-0">My Events</h4>' +
    '<button class="btn btn-primary" id="create-event-btn"><i class="bi bi-plus-lg me-1"></i>New Event</button></div>' +
    buildTable(['#', 'Name', 'Description', 'Date', 'Venue', 'Budget'], rows)
  );
  $('create-event-btn').addEventListener('click', showCreateEventForm);
}

function showCreateEventForm() {
  openModal('New Event',
    formGroup('Event Name', '<input name="name" type="text" required placeholder="Tech Symposium 2025">') +
    formGroup('Description', '<textarea name="description" rows="2" placeholder="Brief description"></textarea>') +
    formGroup('Event Date', '<input name="eventDate" type="date" required>') +
    formGroup('Venue', '<input name="venue" type="text" required placeholder="Seminar Hall A">') +
    formActions('Create Event'),
    async function (fd) {
      await api('POST', '/api/organizer/events', {
        name: fd.get('name'), description: fd.get('description'),
        eventDate: fd.get('eventDate'), venue: fd.get('venue'),
      });
      toast('Event created!');
      await orgEvents();
    }
  );
}

async function orgBudgets() {
  var budgets = await api('GET', '/api/organizer/budgets');
  var rows = budgets.map(function (b) {
    return '<tr>' +
      '<td>' + b.budgetId + '</td>' +
      '<td>' + b.eventName + '</td>' +
      '<td>' + fmt(b.totalAmount) + '</td>' +
      '<td>' + fmt(b.totalAllocated) + '</td>' +
      '<td>' + statusPill(b.status) + '</td>' +
      '<td>' + (b.approvedBy ? b.approvedBy.name : '\u2014') + '</td>' +
      '<td>' + fmtDateTime(b.createdAt) + '</td>' +
      '</tr>';
  });
  setContent(
    '<div class="page-header"><h4 class="fw-bold mb-0">My Budgets</h4>' +
    '<button class="btn btn-primary" id="create-budget-btn"><i class="bi bi-plus-lg me-1"></i>New Budget</button></div>' +
    buildTable(['#', 'Event', 'Total', 'Allocated', 'Status', 'Approved By', 'Created'], rows)
  );
  $('create-budget-btn').addEventListener('click', function () { showCreateBudgetForm().catch(function (err) { toast(err.message, 'error'); }); });
}

async function showCreateBudgetForm() {
  var results = await Promise.all([
    api('GET', '/api/organizer/events'),
    api('GET', '/api/catalog/expense-categories'),
  ]);
  var events     = results[0];
  var categories = results[1];
  var eventsNoBudget = events.filter(function (e) { return !e.budgetId; });
  if (!eventsNoBudget.length) {
    toast('All events already have budgets. Create a new event first.', 'info'); return;
  }
  var eventOpts = eventsNoBudget.map(function (e) {
    return '<option value="' + e.eventId + '">' + e.name + ' (' + fmtDate(e.eventDate) + ')</option>';
  }).join('');
  var catOpts = categories.map(function (c) {
    return '<option value="' + c.categoryId + '">' + c.name + (c.maxAmount ? ' (max: ' + fmt(c.maxAmount) + ')' : '') + '</option>';
  }).join('');

  openModal('New Budget',
    formGroup('Event', '<select name="eventId" required>' + eventOpts + '</select>') +
    formGroup('Category Allocations <small style="font-weight:400;text-transform:none">\u2014 total will be computed from these</small>',
      '<div id="cat-rows">' +
        '<div class="cat-row">' +
          '<select name="catId" required>' + catOpts + '</select>' +
          '<input name="catAmt" type="number" min="1" step="0.01" required placeholder="Amount (\u20B9)">' +
          '<button type="button" class="btn btn-sm btn-outline-danger" onclick="this.parentElement.remove();updateBudgetTotal()"><i class="bi bi-trash"></i></button>' +
        '</div>' +
      '</div>' +
      '<button type="button" class="btn btn-sm btn-outline-secondary mt-2" id="add-cat-row"><i class="bi bi-plus-lg me-1"></i>Add Category</button>' +
      '<p style="margin-top:10px;font-size:13px">Total: <strong id="budget-total">\u20B90.00</strong></p>'
    ) +
    formActions('Submit for Approval'),
    async function (fd) {
      var catIds  = fd.getAll('catId');
      var catAmts = fd.getAll('catAmt');
      if (!catIds.length) throw new Error('Add at least one category allocation.');
      var total = 0;
      var cats = catIds.map(function (id, i) {
        var amt = Number(catAmts[i]);
        total += amt;
        return { expenseCategoryId: Number(id), allocatedAmount: amt };
      });
      await api('POST', '/api/organizer/budgets', {
        eventId: Number(fd.get('eventId')),
        totalAmount: total,
        categories: cats,
      });
      toast('Budget submitted for approval!');
      await orgBudgets();
    }
  );

  function addCatRow() {
    var row = document.createElement('div');
    row.className = 'cat-row';
    row.innerHTML =
      '<select name="catId" required>' + catOpts + '</select>' +
      '<input name="catAmt" type="number" min="1" step="0.01" required placeholder="Amount (\u20B9)">' +
      '<button type="button" class="btn btn-sm btn-outline-danger" onclick="this.parentElement.remove();updateBudgetTotal()"><i class="bi bi-trash"></i></button>';
    row.querySelector('input').addEventListener('input', updateBudgetTotal);
    $('cat-rows').appendChild(row);
  }

  function updateBudgetTotal() {
    var inputs = $('cat-rows').querySelectorAll('input[name="catAmt"]');
    var sum = 0;
    inputs.forEach(function (inp) { sum += Number(inp.value) || 0; });
    var el = $('budget-total');
    if (el) el.textContent = fmt(sum);
  }

  $('add-cat-row').addEventListener('click', addCatRow);
  // attach live total update to first row
  var firstInput = $('cat-rows').querySelector('input[name="catAmt"]');
  if (firstInput) firstInput.addEventListener('input', updateBudgetTotal);
}

async function orgClaims() {
  var claims = await api('GET', '/api/organizer/claims');
  var rows = claims.map(function (c) {
    return '<tr>' +
      '<td>' + c.claimId + '</td>' +
      '<td>' + c.expenseCategoryName + '</td>' +
      '<td>' + c.vendor + '</td>' +
      '<td>' + fmt(c.amount) + '</td>' +
      '<td>' + fmtDate(c.expenseDate) + '</td>' +
      '<td>' + statusPill(c.status) + '</td>' +
      '<td>' + (c.approvalLevel ? statusPill(c.approvalLevel) : '\u2014') + '</td>' +
      '</tr>';
  });
  setContent(
    '<div class="page-header"><h4 class="fw-bold mb-0">My Claims</h4>' +
    '<button class="btn btn-primary" id="submit-claim-btn"><i class="bi bi-plus-lg me-1"></i>Submit Claim</button></div>' +
    buildTable(['#', 'Category', 'Vendor', 'Amount', 'Date', 'Status', 'Approval Level'], rows)
  );
  $('submit-claim-btn').addEventListener('click', function () { showSubmitClaimForm().catch(function (err) { toast(err.message, 'error'); }); });
}

async function showSubmitClaimForm() {
  var results = await Promise.all([
    api('GET', '/api/organizer/budgets'),
    api('GET', '/api/catalog/expense-categories'),
  ]);
  var budgets    = results[0];
  var expCats    = results[1];
  var approved   = budgets.filter(function (b) { return b.status === 'APPROVED'; });
  if (!approved.length) { toast('No approved budgets yet. Get a budget approved first.', 'info'); return; }

  // build a lookup of expenseCategoryId → mandatoryDocumentRequired
  var docRequired = {};
  expCats.forEach(function (c) { docRequired[c.categoryId] = c.mandatoryDocumentRequired; });

  var budgetOpts = approved.map(function (b) {
    return '<option value="' + b.budgetId + '" data-cats=\'' +
      JSON.stringify(b.categories).replace(/'/g, '&#39;') +
      '\'>' + b.eventName + ' (#' + b.budgetId + ')</option>';
  }).join('');

  var firstCats = approved[0].categories;
  var catOpts = buildCatOpts(firstCats);

  var today = new Date().toISOString().split('T')[0];

  openModal('Submit Expense Claim',
    formGroup('Budget', '<select name="budgetId" id="claim-budget-sel" required>' + budgetOpts + '</select>') +
    formGroup('Budget Category', '<select name="budgetCategoryId" id="claim-cat-sel" required>' + catOpts + '</select>') +
    formGroup('Vendor / Payee', '<input name="vendor" type="text" required placeholder="City Travels Ltd.">') +
    formGroup('Amount (\u20B9)', '<input name="amount" type="number" min="0.01" step="0.01" required placeholder="1500">') +
    formGroup('Description', '<textarea name="description" rows="2" required placeholder="Purpose of this expense"></textarea>') +
    formGroup('Expense Date <small>(cannot be future)</small>', '<input name="expenseDate" type="date" required max="' + today + '" value="' + today + '">') +
    '<div id="doc-section">' +
    formGroup('Supporting Document <small id="doc-note">(required for Venue, Food, Travel)</small>',
      '<div class="doc-row">' +
        '<input name="docFileName" type="text" placeholder="invoice.pdf">' +
        '<input name="docFileType" type="text" placeholder="application/pdf" value="application/pdf">' +
        '<input name="docUrl" type="url" placeholder="https://storage.example.com/inv.pdf">' +
      '</div>'
    ) +
    '</div>' +
    formActions('Submit Claim'),
    async function (fd) {
      var docs = [];
      var fn  = (fd.get('docFileName')  || '').trim();
      var ft  = (fd.get('docFileType')  || '').trim();
      var url = (fd.get('docUrl')       || '').trim();
      if (fn || ft || url) {
        if (!fn || !ft || !url) throw new Error('Please fill in all three document fields (name, type, URL) or leave all blank.');
        docs.push({ fileName: fn, fileType: ft, storageUrl: url });
      }
      await api('POST', '/api/organizer/claims', {
        budgetId: Number(fd.get('budgetId')),
        budgetCategoryId: Number(fd.get('budgetCategoryId')),
        vendor: fd.get('vendor'),
        amount: Number(fd.get('amount')),
        description: fd.get('description'),
        expenseDate: fd.get('expenseDate'),
        documents: docs,
      });
      toast('Claim submitted for approval!');
      await orgClaims();
    }
  );

  function buildCatOpts(cats) {
    return cats.map(function (c) {
      return '<option value="' + c.categoryId + '" data-exp-cat-id="' + c.expenseCategoryId + '">' +
        c.expenseCategoryName + ' (avail: ' + fmt(c.availableBalance) + ')</option>';
    }).join('');
  }

  $('claim-budget-sel').addEventListener('change', function (e) {
    var opt  = e.target.options[e.target.selectedIndex];
    var cats = JSON.parse(opt.dataset.cats || '[]');
    $('claim-cat-sel').innerHTML = buildCatOpts(cats);
    updateDocNote();
  });

  $('claim-cat-sel').addEventListener('change', updateDocNote);

  function updateDocNote() {
    var sel     = $('claim-cat-sel');
    var opt     = sel.options[sel.selectedIndex];
    var expCatId = opt ? Number(opt.dataset.expCatId) : null;
    var isReq   = expCatId && docRequired[expCatId];
    var note    = $('doc-note');
    if (note) note.textContent = isReq ? '(\u26a0 required for this category)' : '(optional)';
  }

  updateDocNote();
}

async function orgNotifs() {
  var notifs = await api('GET', '/api/organizer/notifications');
  var rows = notifs.map(function (n) {
    return '<tr class="' + (n.read ? '' : 'unread') + '">' +
      '<td><strong>' + n.subject + '</strong></td>' +
      '<td>' + n.message + '</td>' +
      '<td>' + fmtDateTime(n.sentAt) + '</td>' +
      '<td>' + (n.read
        ? '<span class="pill pill-neutral">Read</span>'
        : '<span class="pill pill-warning">New</span>') + '</td>' +
      '</tr>';
  });
  setContent('<div class="page-header"><h4 class="fw-bold mb-0"><i class="bi bi-bell me-2"></i>Notifications</h4></div>' +
    buildTable(['Subject', 'Message', 'Received', 'Status'], rows));
}

// ══════════════════════════════════════════════════════════
// APPROVER
// ══════════════════════════════════════════════════════════

async function aprPendingBudgets() {
  var budgets = await api('GET', '/api/approver/budgets/pending');
  var rows = budgets.map(function (b) {
    var catList = b.categories.map(function (c) {
      return c.expenseCategoryName + ': ' + fmt(c.allocatedAmount);
    }).join('<br>');
    return '<tr>' +
      '<td>' + b.budgetId + '</td>' +
      '<td><strong>' + b.eventName + '</strong></td>' +
      '<td>' + fmt(b.totalAmount) + '</td>' +
      '<td style="font-size:12px">' + catList + '</td>' +
      '<td>' + fmtDateTime(b.createdAt) + '</td>' +
      '<td>' +
        '<button class="btn btn-sm btn-success" onclick="reviewBudget(' + b.budgetId + ', true)"><i class="bi bi-check-lg"></i> Approve</button> ' +
        '<button class="btn btn-sm btn-danger"  onclick="reviewBudget(' + b.budgetId + ', false)"><i class="bi bi-x-lg"></i> Reject</button>' +
      '</td>' +
      '</tr>';
  });
  setContent('<div class="page-header"><h4 class="fw-bold mb-0">Pending Budgets</h4></div>' +
    buildTable(['#', 'Event', 'Total', 'Categories', 'Submitted', 'Action'], rows));
}

function reviewBudget(budgetId, approved) {
  openModal(
    (approved
      ? '<i class="bi bi-check-circle-fill text-success me-2"></i>Approve Budget'
      : '<i class="bi bi-x-circle-fill text-danger me-2"></i>Reject Budget') + ' #' + budgetId,
    formGroup(
      approved ? 'Comment <span class="fw-normal text-muted">(optional)</span>' : 'Rejection Reason <span class="text-danger">*</span>',
      '<textarea name="comment" rows="3"' + (approved ? '' : ' required') + ' placeholder="' +
        (approved ? 'Add an optional approval note\u2026' : 'State the reason for rejection\u2026') + '"></textarea>'
    ) +
    formActions(approved ? 'Confirm Approval' : 'Confirm Rejection'),
    async function (fd) {
      await api('POST', '/api/approver/budgets/' + budgetId + '/decision', {
        approved: approved, comment: (fd.get('comment') || '').trim()
      });
      toast('Budget ' + (approved ? 'approved' : 'rejected') + '!', approved ? 'success' : 'info');
      await aprPendingBudgets();
    }
  );
}

async function aprPendingClaims() {
  var claims = await api('GET', '/api/approver/claims/pending');
  var rows = claims.map(function (c) {
    return '<tr>' +
      '<td>' + c.claimId + '</td>' +
      '<td>' + c.expenseCategoryName + '</td>' +
      '<td>' + (c.submittedBy ? c.submittedBy.name : '\u2014') + '</td>' +
      '<td>' + c.vendor + '</td>' +
      '<td>' + fmt(c.amount) + '</td>' +
      '<td style="max-width:200px;font-size:12px">' + c.description + '</td>' +
      '<td>' + (c.approvalLevel ? statusPill(c.approvalLevel) : '\u2014') + '</td>' +
      '<td>' +
        '<button class="btn btn-sm btn-success" onclick="reviewClaim(' + c.claimId + ', \'APPROVED\')"><i class="bi bi-check-lg"></i> Approve</button> ' +
        '<button class="btn btn-sm btn-danger"  onclick="reviewClaim(' + c.claimId + ', \'REJECTED\')"><i class="bi bi-x-lg"></i> Reject</button>' +
      '</td>' +
      '</tr>';
  });
  setContent('<div class="page-header"><h4 class="fw-bold mb-0">Pending Claims</h4></div>' +
    buildTable(['#', 'Category', 'Submitted By', 'Vendor', 'Amount', 'Description', 'Level', 'Action'], rows));
}

function reviewClaim(claimId, decision) {
  var approved = decision === 'APPROVED';
  openModal(
    (approved
      ? '<i class="bi bi-check-circle-fill text-success me-2"></i>Approve Claim'
      : '<i class="bi bi-x-circle-fill text-danger me-2"></i>Reject Claim') + ' #' + claimId,
    formGroup(
      approved ? 'Comment <span class="fw-normal text-muted">(optional)</span>' : 'Rejection Reason <span class="text-danger">*</span>',
      '<textarea name="comment" rows="3"' + (approved ? '' : ' required') + ' placeholder="' +
        (approved ? 'Add an optional approval note\u2026' : 'State the reason for rejection\u2026') + '"></textarea>'
    ) +
    formActions(approved ? 'Confirm Approval' : 'Confirm Rejection'),
    async function (fd) {
      await api('POST', '/api/approver/claims/' + claimId + '/decision', {
        decision: decision, comment: (fd.get('comment') || '').trim()
      });
      toast('Claim ' + decision.toLowerCase() + '!', approved ? 'success' : 'info');
      await aprPendingClaims();
    }
  );
}

async function aprNotifs() {
  var notifs = await api('GET', '/api/approver/notifications');
  var rows = notifs.map(function (n) {
    return '<tr class="' + (n.read ? '' : 'unread') + '">' +
      '<td><strong>' + n.subject + '</strong></td>' +
      '<td>' + n.message + '</td>' +
      '<td>' + fmtDateTime(n.sentAt) + '</td>' +
      '<td>' + (n.read ? '<span class="pill pill-neutral">Read</span>' : '<span class="pill pill-warning">New</span>') + '</td>' +
      '</tr>';
  });
  setContent('<div class="page-header"><h4 class="fw-bold mb-0"><i class="bi bi-bell me-2"></i>Notifications</h4></div>' +
    buildTable(['Subject', 'Message', 'Received', 'Status'], rows));
}

// ══════════════════════════════════════════════════════════
// FINANCE ADMIN
// ══════════════════════════════════════════════════════════

async function finWorkflows() {
  var configs = await api('GET', '/api/finance/workflow-configs');
  var rows = configs.map(function (c) {
    return '<tr>' +
      '<td>' + c.configId + '</td>' +
      '<td><strong>' + c.name + '</strong></td>' +
      '<td>' + fmt(c.autoApproveLimit) + '</td>' +
      '<td>' + fmt(c.singleLevelLimit) + '</td>' +
      '<td>' + fmt(c.multiLevelThreshold) + '</td>' +
      '</tr>';
  });
  setContent(
    '<div class="page-header"><h4 class="fw-bold mb-0">Workflow Configurations</h4>' +
    '<button class="btn btn-primary" id="create-workflow-btn"><i class="bi bi-plus-lg me-1"></i>New Config</button></div>' +
    '<div class="alert alert-info alert-sm py-2 mb-3 small">Thresholds must satisfy: <strong>Auto-Approve \u2264 Single-Level \u2264 Multi-Level</strong></div>' +
    buildTable(['#', 'Name', 'Auto-Approve Limit', 'Single-Level Limit', 'Multi-Level Threshold'], rows)
  );
  $('create-workflow-btn').addEventListener('click', showCreateWorkflowForm);
}

function showCreateWorkflowForm() {
  openModal('New Workflow Config',
    formGroup('Configuration Name', '<input name="name" type="text" required placeholder="Standard 2025">') +
    formGroup('Auto-Approve Limit (\u20B9)',   '<input name="autoApproveLimit"   type="number" min="0" step="0.01" required placeholder="500">') +
    formGroup('Single-Level Limit (\u20B9)',   '<input name="singleLevelLimit"   type="number" min="0" step="0.01" required placeholder="5000">') +
    formGroup('Multi-Level Threshold (\u20B9)','<input name="multiLevelThreshold" type="number" min="0" step="0.01" required placeholder="20000">') +
    formActions('Save Config'),
    async function (fd) {
      await api('POST', '/api/finance/workflow-configs', {
        name: fd.get('name'),
        autoApproveLimit:    Number(fd.get('autoApproveLimit')),
        singleLevelLimit:    Number(fd.get('singleLevelLimit')),
        multiLevelThreshold: Number(fd.get('multiLevelThreshold')),
      });
      toast('Workflow config saved!');
      await finWorkflows();
    }
  );
}

async function finBudgets() {
  var budgets = await api('GET', '/api/finance/budgets');
  var rows = budgets.map(function (b) {
    var actions =
      (b.status === 'APPROVED'
        ? '<button class="btn btn-sm btn-warning" onclick="closeBudget(' + b.budgetId + ')"><i class="bi bi-lock"></i> Close</button> '
        : '') +
      '<button class="btn btn-sm btn-outline-secondary" onclick="exportBudget(' + b.budgetId + ',\'CSV\')"><i class="bi bi-filetype-csv"></i> CSV</button> ' +
      '<button class="btn btn-sm btn-outline-danger"    onclick="exportBudget(' + b.budgetId + ',\'PDF\')"><i class="bi bi-filetype-pdf"></i> PDF</button>';
    return '<tr>' +
      '<td>' + b.budgetId + '</td>' +
      '<td>' + b.eventName + '</td>' +
      '<td>' + fmt(b.totalAmount) + '</td>' +
      '<td>' + statusPill(b.status) + '</td>' +
      '<td>' + (b.approvedBy ? b.approvedBy.name : '\u2014') + '</td>' +
      '<td>' + actions + '</td>' +
      '</tr>';
  });
  setContent('<div class="page-header"><h4 class="fw-bold mb-0">All Budgets</h4></div>' +
    buildTable(['#', 'Event', 'Total', 'Status', 'Approved By', 'Actions'], rows));
}

async function closeBudget(budgetId) {
  if (!confirm('Close budget #' + budgetId + '?\nRequirements: event must have concluded and no pending claims.')) return;
  try {
    await api('POST', '/api/finance/budgets/' + budgetId + '/close');
    toast('Budget closed!');
    await finBudgets();
  } catch (err) { toast(err.message, 'error'); }
}

async function exportBudget(budgetId, format) {
  try {
    var result = await apiBlob('/api/finance/budgets/' + budgetId + '/export?format=' + format);
    downloadBlob(result.blob, result.filename);
    toast('Export downloaded!');
  } catch (err) { toast(err.message, 'error'); }
}

async function finAudit() {
  var logs = await api('GET', '/api/finance/audit-logs');
  var rows = logs.map(function (l) {
    return '<tr>' +
      '<td>' + l.logId + '</td>' +
      '<td>' + l.entityType + '</td>' +
      '<td>' + l.entityId + '</td>' +
      '<td><span class="pill pill-info">' + l.action + '</span></td>' +
      '<td style="max-width:220px;font-size:12px">' + (l.description || '\u2014') + '</td>' +
      '<td>' + (l.performedBy ? l.performedBy.name : '\u2014') + '</td>' +
      '<td>' + fmtDateTime(l.timestamp) + '</td>' +
      '</tr>';
  });
  setContent('<div class="page-header"><h4 class="fw-bold mb-0">Audit Log</h4></div>' +
    buildTable(['#', 'Entity', 'ID', 'Action', 'Description', 'Performed By', 'Timestamp'], rows));
}

async function finNotifs() {
  var notifs = await api('GET', '/api/finance/notifications');
  var rows = notifs.map(function (n) {
    return '<tr class="' + (n.read ? '' : 'unread') + '">' +
      '<td><strong>' + n.subject + '</strong></td>' +
      '<td>' + n.message + '</td>' +
      '<td>' + fmtDateTime(n.sentAt) + '</td>' +
      '<td>' + (n.read ? '<span class="pill pill-neutral">Read</span>' : '<span class="pill pill-warning">New</span>') + '</td>' +
      '</tr>';
  });
  setContent('<div class="page-header"><h4 class="fw-bold mb-0"><i class="bi bi-bell me-2"></i>Notifications</h4></div>' +
    buildTable(['Subject', 'Message', 'Received', 'Status'], rows));
}

// ── Form helpers ────────────────────────────────────────────
function formGroup(label, inputHtml) {
  return '<div class="mb-3"><label>' + label + '</label>' + inputHtml + '</div>';
}

function formActions(submitLabel) {
  return '<div class="d-flex justify-content-end gap-2 pt-3 mt-2 border-top">' +
    '<button type="button" class="btn btn-secondary" onclick="closeModal()">Cancel</button>' +
    '<button type="submit" class="btn btn-primary fw-semibold">' + submitLabel + '</button>' +
    '</div>';
}

// ── Init ────────────────────────────────────────────────────
function init() {
  if (restoreSession()) {
    showApp();
  } else {
    showAuth('login');
  }

  $('login-form').addEventListener('submit', doLogin);
  $('register-form').addEventListener('submit', doRegister);
  $('show-register').addEventListener('click', function (e) { e.preventDefault(); showAuth('register'); });
  $('show-login').addEventListener('click',    function (e) { e.preventDefault(); showAuth('login'); });
  $('logout-btn').addEventListener('click', logout);
  $('modal-close').addEventListener('click', closeModal);
  $('modal-overlay').addEventListener('click', function (e) { if (e.target === $('modal-overlay')) closeModal(); });
}

document.addEventListener('DOMContentLoaded', init);
