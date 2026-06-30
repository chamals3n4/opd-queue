let opdDeptId = null;
let allPatients = [];

// ── Confirm dialog ─────────────────────────────────────────────
function showConfirm(title, message, onConfirm) {
    document.getElementById('confirmTitle').textContent = title;
    document.getElementById('confirmMessage').textContent = message;
    document.getElementById('confirmOkBtn').onclick = () => { closeConfirm(); onConfirm(); };
    document.getElementById('confirmModal').classList.add('open');
}
function closeConfirm() {
    document.getElementById('confirmModal').classList.remove('open');
}

async function apiFetch(url, opts = {}) {
    const res = await fetch(url, opts);
    if (res.status === 401 || res.status === 403) {
        window.location.href = '/login';
        throw new Error('Session expired');
    }
    return res;
}

// ── Tab switching ──────────────────────────────────────────────
document.querySelectorAll('.sidebar-item[data-tab]').forEach(item => {
    item.addEventListener('click', () => {
        document.querySelectorAll('.sidebar-item').forEach(i => i.classList.remove('active'));
        item.classList.add('active');
        document.querySelectorAll('.section').forEach(s => s.classList.remove('show'));
        document.getElementById('sec-' + item.dataset.tab).classList.add('show');
        switch (item.dataset.tab) {
            case 'overview':  loadOverview(); break;
            case 'queue':     loadQueue();    break;
            case 'doctors':   loadDoctors();  break;
            case 'staff':     loadStaff();    break;
            case 'patients':  loadPatients(); break;
        }
    });
});

function goToQueue() { document.querySelector('[data-tab="queue"]').click(); }

// ── Init ───────────────────────────────────────────────────────
async function init() {
    try {
        const depts = await fetch('/api/departments', { cache: 'no-store' }).then(r => r.json());
        if (depts.length) opdDeptId = depts[0].id;
    } catch {}
    await loadOverview();
    return opdDeptId;
}

// ── Overview ───────────────────────────────────────────────────
async function loadOverview() {
    await Promise.all([loadStats(), loadQueueSnapshot()]);
}

async function loadStats() {
    try {
        const s = await apiFetch('/api/admin/stats').then(r => r.json());
        const get = (...keys) => keys.reduce((sum, k) => sum + (s[k] || 0), 0);
        document.getElementById('ovWaiting').textContent   = get('WAITING', 'REGISTERED');
        document.getElementById('ovCalled').textContent    = get('CALLED', 'IN_PROGRESS');
        document.getElementById('ovCompleted').textContent = get('COMPLETED');
        document.getElementById('ovNoShow').textContent    = get('NO_SHOW');
    } catch {}
}

async function loadQueueSnapshot() {
    if (!opdDeptId) return;
    try {
        const board = await apiFetch(`/api/display/${opdDeptId}`).then(r => r.json());
        const serving = board.currentlyCalledTicket && board.currentlyCalledTicket !== '-' ? board.currentlyCalledTicket : '—';
        document.getElementById('ovServing').textContent = serving;
        const nexts = (board.nextTickets || []).filter(t => t !== board.currentlyCalledTicket);
        document.getElementById('ovNext').textContent = nexts.length ? nexts.slice(0, 3).join(', ') : 'None';
        document.getElementById('ovTotalWaiting').textContent = board.totalWaiting ?? '—';
    } catch {}
}

// ── Queue ──────────────────────────────────────────────────────
async function loadQueue() {
    if (!opdDeptId) { await init(); return; }
    try {
        const [allActive, waiting] = await Promise.all([
            apiFetch('/api/admin/tickets/active').then(r => r.json()),
            apiFetch(`/api/queue/department/${opdDeptId}`).then(r => r.json())
        ]);

        const called = allActive.find(t => t.status === 'CALLED' || t.status === 'IN_PROGRESS') || null;
        updateNowServing(called);

        document.getElementById('waitingBadge').textContent = `${waiting.length} waiting`;
        document.getElementById('callNextSub').textContent = waiting.length
            ? `${waiting.length} patient${waiting.length !== 1 ? 's' : ''} in queue`
            : 'Queue is empty';

        const tbody = document.getElementById('queueTableBody');
        if (!waiting.length) {
            tbody.innerHTML = '<tr><td colspan="6" class="empty-cell"><div class="empty-cell-title">Queue is empty</div><div style="color:#9ca3af;font-size:12px;margin-top:2px">No patients currently waiting</div></td></tr>';
            return;
        }
        tbody.innerHTML = waiting.map((t, i) => {
            const emerg = t.emergency || t.isEmergency;
            const actions = [
                (t.status === 'CALLED' || t.status === 'IN_PROGRESS') ? `<button class="btn btn-info" onclick="completeTicket('${t.ticketNumber}')">Complete</button>` : '',
                (t.status === 'CALLED' || t.status === 'WAITING')     ? `<button class="btn btn-warning" onclick="noShowTicket('${t.ticketNumber}')">No Show</button>` : '',
            ].filter(Boolean).join('');
            return `<tr>
                <td style="color:#9ca3af;font-size:12px">${i + 1}</td>
                <td>
                    <span class="ticket-num">${t.ticketNumber}</span>
                    ${emerg ? '<span class="emerg-tag" style="margin-left:6px">EMRG</span>' : ''}
                </td>
                <td>${t.patient?.fullName || '—'}</td>
                <td style="color:#6b7280">${t.estimatedWaitMinutes} min</td>
                <td><span class="pill pill-${t.status}">${t.status.replace(/_/g, ' ')}</span></td>
                <td><div class="action-group">${actions || '<span style="color:#d1d5db">—</span>'}</div></td>
            </tr>`;
        }).join('');
    } catch {
        document.getElementById('queueTableBody').innerHTML =
            '<tr><td colspan="6" class="empty-cell"><div class="empty-cell-title">Failed to load queue</div></td></tr>';
    }
}

function updateNowServing(ticket) {
    const nsTicket  = document.getElementById('nsTicket');
    const nsPatient = document.getElementById('nsPatient');
    const nsActions = document.getElementById('nsActions');
    if (ticket) {
        nsTicket.textContent  = ticket.ticketNumber;
        nsTicket.className    = 'ns-ticket';
        nsPatient.textContent = ticket.patient?.fullName || '—';
        nsActions.innerHTML   = `
            <button class="btn btn-primary" onclick="completeTicket('${ticket.ticketNumber}')">Mark Complete</button>
            <button class="btn btn-warning" onclick="noShowTicket('${ticket.ticketNumber}')">No Show</button>`;
    } else {
        nsTicket.textContent  = 'No patient called';
        nsTicket.className    = 'ns-ticket idle';
        nsPatient.textContent = '';
        nsActions.innerHTML   = '';
    }
}

// ── Queue operations ───────────────────────────────────────────
async function callNext() {
    if (!opdDeptId) return;
    try {
        const res = await apiFetch(`/api/queue/call-next/${opdDeptId}`, { method: 'POST' });
        if (!res.ok) { const e = await res.json(); throw new Error(e.message); }
        const t = await res.json();
        toast(`Called: ${t.ticketNumber}`, 'success');
        loadQueue(); loadStats(); loadQueueSnapshot();
    } catch (e) { toast(e.message || 'No patients waiting', 'danger'); }
}

async function completeTicket(tn) {
    try {
        await apiFetch(`/api/queue/complete/${tn}`, { method: 'POST' });
        toast(`Completed: ${tn}`, 'success');
        loadQueue(); loadStats(); loadQueueSnapshot();
    } catch { toast('Failed', 'danger'); }
}

async function noShowTicket(tn) {
    try {
        await apiFetch(`/api/queue/no-show/${tn}`, { method: 'POST' });
        toast(`No-show recorded: ${tn}`, 'success');
        loadQueue(); loadStats(); loadQueueSnapshot();
    } catch { toast('Failed', 'danger'); }
}

function confirmResetQueue() {
    showConfirm(
        'Reset OPD Queue',
        'All waiting tickets will be cancelled. This action cannot be undone.',
        async () => {
            try {
                await apiFetch(`/api/queue/reset/${opdDeptId}`, { method: 'POST' });
                toast('Queue reset', 'success');
                loadQueue(); loadOverview();
            } catch { toast('Failed', 'danger'); }
        }
    );
}

// ── Doctors ────────────────────────────────────────────────────
async function loadDoctors() {
    try {
        const docs = await apiFetch('/api/doctors').then(r => r.json());
        const tbody = document.getElementById('doctorTableBody');
        tbody.innerHTML = docs.length ? docs.map(d => `<tr>
            <td><strong>${d.fullName}</strong></td>
            <td style="color:#6b7280">${d.specialization}</td>
            <td style="color:#6b7280">${d.roomNumber}</td>
            <td><span class="pill ${d.available ? 'pill-avail' : 'pill-unavail'}">${d.available ? 'Available' : 'Unavailable'}</span></td>
            <td><div class="action-group">
                <button class="btn btn-secondary" onclick="editDoctor(${d.id})">Edit</button>
                <button class="btn ${d.available ? 'btn-warning' : 'btn-secondary'}" onclick="toggleDoctorAvail(${d.id},${!d.available})">${d.available ? 'Set Unavailable' : 'Set Available'}</button>
                <button class="btn btn-danger" onclick="deleteDoctor(${d.id})">Remove</button>
            </div></td>
        </tr>`).join('') : '<tr><td colspan="5" class="empty-cell"><div class="empty-cell-title">No doctors added</div></td></tr>';
    } catch {}
}

function openDoctorModal(doc) {
    document.getElementById('doctorEditId').value           = doc ? doc.id : '';
    document.getElementById('doctorName').value             = doc ? doc.fullName : '';
    document.getElementById('doctorSpecialization').value   = doc ? doc.specialization : '';
    document.getElementById('doctorRoom').value             = doc ? doc.roomNumber : '';
    document.getElementById('doctorModalTitle').textContent = doc ? 'Edit Doctor' : 'Add Doctor';
    document.getElementById('doctorModal').classList.add('open');
}
function closeDoctorModal() { document.getElementById('doctorModal').classList.remove('open'); }

async function editDoctor(id) {
    const docs = await apiFetch('/api/doctors').then(r => r.json()).catch(() => []);
    const d = docs.find(x => x.id === id);
    if (d) openDoctorModal(d);
}

async function saveDoctor() {
    const id             = document.getElementById('doctorEditId').value;
    const fullName       = document.getElementById('doctorName').value.trim();
    const specialization = document.getElementById('doctorSpecialization').value.trim();
    const roomNumber     = document.getElementById('doctorRoom').value.trim();
    if (!fullName || !specialization || !roomNumber) { toast('All fields are required', 'danger'); return; }
    try {
        const isEdit = !!id;
        const body   = { fullName, specialization, roomNumber };
        if (opdDeptId) body.departmentId = opdDeptId;
        const res = await apiFetch(isEdit ? `/api/doctors/${id}` : '/api/doctors', {
            method: isEdit ? 'PUT' : 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        if (!res.ok) throw new Error();
        closeDoctorModal();
        toast(isEdit ? 'Doctor updated' : 'Doctor added', 'success');
        loadDoctors();
    } catch { toast('Failed to save', 'danger'); }
}

async function toggleDoctorAvail(id, avail) {
    try {
        await apiFetch(`/api/doctors/${id}`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ isAvailable: avail }) });
        toast(avail ? 'Doctor is now available' : 'Doctor set as unavailable', 'success');
        loadDoctors();
    } catch { toast('Failed', 'danger'); }
}

function deleteDoctor(id) {
    showConfirm(
        'Remove Doctor',
        'This doctor will be permanently removed from the system.',
        async () => {
            try { await apiFetch(`/api/doctors/${id}`, { method: 'DELETE' }); toast('Doctor removed', 'success'); loadDoctors(); }
            catch { toast('Failed', 'danger'); }
        }
    );
}

// ── Staff ──────────────────────────────────────────────────────
async function loadStaff() {
    try {
        const staff = await apiFetch('/api/staff').then(r => r.json());
        const tbody = document.getElementById('staffTableBody');
        tbody.innerHTML = staff.length ? staff.map(s => `<tr>
            <td><strong>${s.fullName}</strong></td>
            <td style="color:#6b7280">${(s.role || '').replace(/_/g, ' ')}</td>
            <td style="color:#6b7280;font-variant-numeric:tabular-nums">${s.username}</td>
            <td><div class="action-group">
                <button class="btn btn-secondary" onclick="editStaff(${s.id})">Edit</button>
                <button class="btn btn-danger" onclick="deleteStaff(${s.id})">Remove</button>
            </div></td>
        </tr>`).join('') : '<tr><td colspan="4" class="empty-cell"><div class="empty-cell-title">No staff added</div></td></tr>';
    } catch {}
}

function openStaffModal(sta) {
    document.getElementById('staffEditId').value           = sta ? sta.id : '';
    document.getElementById('staffName').value             = sta ? sta.fullName : '';
    document.getElementById('staffRole').value             = sta ? sta.role : 'RECEPTIONIST';
    document.getElementById('staffUsername').value         = sta ? sta.username : '';
    document.getElementById('staffPassword').value         = '';
    document.getElementById('staffModalTitle').textContent = sta ? 'Edit Staff' : 'Add Staff';
    document.getElementById('staffModal').classList.add('open');
}
function closeStaffModal() { document.getElementById('staffModal').classList.remove('open'); }

async function editStaff(id) {
    const arr = await apiFetch('/api/staff').then(r => r.json()).catch(() => []);
    const s = arr.find(x => x.id === id);
    if (s) openStaffModal(s);
}

async function saveStaff() {
    const id       = document.getElementById('staffEditId').value;
    const fullName = document.getElementById('staffName').value.trim();
    const role     = document.getElementById('staffRole').value;
    const username = document.getElementById('staffUsername').value.trim();
    const password = document.getElementById('staffPassword').value;
    if (!fullName || !username || (!id && !password)) { toast('All fields required', 'danger'); return; }
    try {
        const isEdit = !!id;
        const body   = { fullName, role, username };
        if (opdDeptId) body.departmentId = opdDeptId;
        if (password) body.password = password;
        const res = await apiFetch(isEdit ? `/api/staff/${id}` : '/api/staff', {
            method: isEdit ? 'PUT' : 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        if (!res.ok) throw new Error();
        closeStaffModal();
        toast(isEdit ? 'Staff updated' : 'Staff added', 'success');
        loadStaff();
    } catch { toast('Failed to save', 'danger'); }
}

function deleteStaff(id) {
    showConfirm(
        'Remove Staff Member',
        'This staff member will be permanently removed from the system.',
        async () => {
            try { await apiFetch(`/api/staff/${id}`, { method: 'DELETE' }); toast('Staff removed', 'success'); loadStaff(); }
            catch { toast('Failed', 'danger'); }
        }
    );
}

// ── Patients ───────────────────────────────────────────────────
const PATIENTS_PER_PAGE = 10;
let currentPatientPage = 1;
let filteredPatients = [];

async function loadPatients() {
    try { allPatients = await apiFetch('/api/admin/patients').then(r => r.json()); renderPatients(allPatients); } catch {}
}

function filterPatients() {
    const q = document.getElementById('patientSearch').value.toLowerCase();
    currentPatientPage = 1;
    renderPatients(allPatients.filter(p => (p.nic || '').toLowerCase().includes(q) || (p.fullName || '').toLowerCase().includes(q)));
}

function patientPageChange(delta) {
    const totalPages = Math.ceil(filteredPatients.length / PATIENTS_PER_PAGE);
    currentPatientPage = Math.max(1, Math.min(currentPatientPage + delta, totalPages));
    renderPatientPage();
}

function goToPatientPage(page) {
    currentPatientPage = page;
    renderPatientPage();
}

function renderPatients(list) {
    filteredPatients = list;
    currentPatientPage = 1;
    renderPatientPage();
}

function renderPatientPage() {
    const list = filteredPatients;
    const totalPages = Math.max(1, Math.ceil(list.length / PATIENTS_PER_PAGE));
    const page = Math.min(currentPatientPage, totalPages);
    const start = (page - 1) * PATIENTS_PER_PAGE;
    const slice = list.slice(start, start + PATIENTS_PER_PAGE);

    const tbody = document.getElementById('patientTableBody');
    tbody.innerHTML = slice.length ? slice.map(p => `<tr>
        <td style="font-variant-numeric:tabular-nums;font-weight:600;color:#005d5d">${p.nic}</td>
        <td><strong>${p.fullName}</strong></td>
        <td style="color:#6b7280">${p.gender || '—'}</td>
        <td style="color:#6b7280">${p.contactNumber}</td>
        <td style="color:#9ca3af;font-size:12px">${p.createdAt ? new Date(p.createdAt).toLocaleDateString() : '—'}</td>
        <td><div class="action-group">
            <button class="btn btn-secondary" onclick="editPatient('${p.id}')">Edit</button>
            <button class="btn btn-danger" onclick="deletePatient('${p.id}', '${p.fullName.replace(/'/g, "\\'")}')">Remove</button>
        </div></td>
    </tr>`).join('') : '<tr><td colspan="6" class="empty-cell"><div class="empty-cell-title">No patients found</div></td></tr>';

    const bar = document.getElementById('patientPagination');
    if (list.length <= PATIENTS_PER_PAGE) { bar.style.display = 'none'; return; }
    bar.style.display = 'flex';

    const from = start + 1;
    const to   = Math.min(start + PATIENTS_PER_PAGE, list.length);
    document.getElementById('pgInfo').textContent = `${from}–${to} of ${list.length} items`;

    document.getElementById('pgPrev').disabled = page === 1;
    document.getElementById('pgNext').disabled = page === totalPages;

    // page number buttons — show window of 5 around current
    const pages  = [];
    const window = 2;
    for (let i = Math.max(1, page - window); i <= Math.min(totalPages, page + window); i++) pages.push(i);

    document.getElementById('pgPages').innerHTML = pages.map(p =>
        `<button class="pg-num${p === page ? ' active' : ''}" onclick="goToPatientPage(${p})">${p}</button>`
    ).join('');
}

function openPatientModal(patient) {
    const isEdit = !!patient;
    document.getElementById('patientEditId').value        = isEdit ? patient.id : '';
    document.getElementById('patientNic').value           = isEdit ? patient.nic : '';
    document.getElementById('patientNic').readOnly        = isEdit;
    document.getElementById('patientFullName').value      = isEdit ? patient.fullName : '';
    document.getElementById('patientDob').value           = isEdit ? (patient.dateOfBirth || '') : '';
    document.getElementById('patientGender').value        = isEdit ? (patient.gender || '') : '';
    document.getElementById('patientContact').value       = isEdit ? patient.contactNumber : '';
    document.getElementById('patientModalTitle').textContent = isEdit ? 'Edit Patient' : 'Register Patient';
    document.getElementById('patientSaveBtn').textContent    = isEdit ? 'Save Changes' : 'Register';
    document.getElementById('patientModal').classList.add('open');
    if (!isEdit) document.getElementById('patientNic').focus();
}
function closePatientModal() { document.getElementById('patientModal').classList.remove('open'); }

function editPatient(id) {
    const patient = allPatients.find(p => p.id === id);
    if (patient) openPatientModal(patient);
}

function deletePatient(id, name) {
    showConfirm(
        'Remove Patient',
        `"${name}" will be permanently removed from the system.`,
        async () => {
            try {
                const res = await apiFetch(`/api/patients/${id}`, { method: 'DELETE' });
                if (!res.ok) throw new Error();
                toast('Patient removed', 'success');
                loadPatients();
            } catch { toast('Failed to remove patient', 'danger'); }
        }
    );
}

async function savePatient() {
    const id            = document.getElementById('patientEditId').value;
    const isEdit        = !!id;
    const nic           = document.getElementById('patientNic').value.trim();
    const fullName      = document.getElementById('patientFullName').value.trim();
    const dateOfBirth   = document.getElementById('patientDob').value;
    const gender        = document.getElementById('patientGender').value;
    const contactNumber = document.getElementById('patientContact').value.trim();

    if (!nic || !fullName) { toast('NIC and full name are required', 'danger'); return; }

    try {
        let res;
        if (isEdit) {
            res = await apiFetch(`/api/patients/${id}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ fullName, dateOfBirth, gender, contactNumber })
            });
        } else {
            res = await apiFetch('/api/patients/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ nic, fullName, dateOfBirth, gender, contactNumber })
            });
        }
        if (!res.ok) { const err = await res.json(); throw new Error(err.message || err.error); }
        closePatientModal();
        toast(isEdit ? 'Patient updated' : 'Patient registered successfully', 'success');
        loadPatients();
    } catch (err) { toast(err.message || (isEdit ? 'Update failed' : 'Registration failed'), 'danger'); }
}

// ── Toast ──────────────────────────────────────────────────────
function toast(msg, type = 'success') {
    const el = document.getElementById('toast');
    el.textContent = msg;
    el.className   = `toast show toast-${type}`;
    setTimeout(() => el.classList.remove('show'), 4000);
}

// ── WebSocket (live queue updates) ─────────────────────────────
function connectAdminWs(deptId) {
    const sock  = new SockJS('/ws');
    const stomp = Stomp.over(sock);
    stomp.debug = null;
    stomp.connect({}, () => {
        stomp.subscribe(`/topic/queue/${deptId}`, () => {
            const activeTab = document.querySelector('.sidebar-item.active')?.dataset?.tab;
            if (activeTab === 'queue')    { loadQueue();    return; }
            if (activeTab === 'overview') { loadOverview(); return; }
            loadOverview();
        });
    }, () => {
        setTimeout(() => connectAdminWs(deptId), 5000);
    });
}

// ── Boot ───────────────────────────────────────────────────────
init().then(() => { if (opdDeptId) connectAdminWs(opdDeptId); });
setInterval(loadOverview, 30000);
