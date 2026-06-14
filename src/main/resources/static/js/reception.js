let currentNic = null, selectedDeptId = null, isEmergency = false, currentSlipUrl = null;

async function apiFetch(url, opts = {}) {
    const res = await fetch(url, opts);
    if (res.status === 401 || res.status === 403) {
        window.location.href = '/login';
        throw new Error('Session expired');
    }
    return res;
}

// ── Load default department ────────────────────────────────────
apiFetch('/api/departments').then(r => r.json()).then(depts => {
    if (depts.length) {
        selectedDeptId = depts[0].id;
        document.getElementById('selectedDeptId').value = selectedDeptId;
        checkReady();
    }
}).catch(() => {});

// ── Emergency toggle ───────────────────────────────────────────
document.getElementById('emergencyRow').addEventListener('click', () => {
    isEmergency = !isEmergency;
    document.getElementById('emergencyRow').classList.toggle('active', isEmergency);
});

function checkReady() {
    document.getElementById('issueBtn').disabled = !currentNic;
}

// ── Set patient after lookup or registration ───────────────────
function setPatient(patient, nic) {
    currentNic = nic;
    const initials = (patient.fullName || nic).split(' ').map(w => w[0]).join('').substring(0, 2).toUpperCase();
    document.getElementById('patientAvatar').textContent = initials;
    document.getElementById('patientName').textContent   = patient.fullName || '—';
    document.getElementById('patientNic').textContent    = nic;
    document.getElementById('patientPill').classList.add('show');
    checkReady();
}

// ── NIC lookup ─────────────────────────────────────────────────
document.getElementById('lookupBtn').addEventListener('click', async () => {
    const nic = document.getElementById('lookupNic').value.trim();
    if (!nic) return;
    try {
        const res = await apiFetch(`/api/patients/${nic}`);
        if (!res.ok) throw new Error();
        setPatient(await res.json(), nic);
    } catch {
        currentNic = null;
        document.getElementById('patientPill').classList.remove('show');
        checkReady();
        showAlert('issueAlert', 'Patient not found. Please register first.', 'warning');
    }
});

// ── Register patient ───────────────────────────────────────────
document.getElementById('registerForm').addEventListener('submit', async e => {
    e.preventDefault();
    const data = Object.fromEntries(new FormData(e.target));
    try {
        const res = await apiFetch('/api/patients/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        if (!res.ok) { const err = await res.json(); throw new Error(err.message || err.error); }
        const p = await res.json();
        setPatient(p, data.nic);
        document.getElementById('lookupNic').value = data.nic;
        showAlert('registerAlert', `${p.fullName || data.fullName} registered successfully`, 'success');
        e.target.reset();
    } catch (err) {
        showAlert('registerAlert', err.message || 'Registration failed', 'danger');
    }
});

// ── Issue ticket ───────────────────────────────────────────────
document.getElementById('issueBtn').addEventListener('click', async () => {
    const btn = document.getElementById('issueBtn');
    btn.disabled    = true;
    btn.textContent = 'Issuing...';
    try {
        const res = await apiFetch('/api/queue/issue', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ nic: currentNic, departmentId: parseInt(selectedDeptId), emergency: isEmergency })
        });
        if (!res.ok) { const err = await res.json(); throw new Error(err.message); }
        const t = await res.json();
        showPrintModal(t);
        document.getElementById('emptyState').style.display = 'none';
        showAlert('issueAlert', 'Ticket ' + t.ticketNumber + ' issued successfully!', 'success');
    } catch (err) {
        showAlert('issueAlert', err.message || 'Failed to issue ticket', 'danger');
    } finally {
        btn.disabled    = false;
        btn.textContent = 'Issue Ticket';
    }
});

// ── Print modal ────────────────────────────────────────────────
function showPrintModal(t) {
    currentSlipUrl = `/api/tickets/${t.ticketNumber}/slip`;
    const emerg    = t.emergency || t.isEmergency;

    document.getElementById('printTicketNum').textContent = t.ticketNumber;
    document.getElementById('printPos').textContent       = '#' + t.queuePosition;
    document.getElementById('printWait').textContent      = t.estimatedWaitMinutes + ' min';
    document.getElementById('printStatus').textContent    = t.status.replace(/_/g, ' ');
    document.getElementById('printAhead').textContent     = t.queuePosition - 1;
    document.getElementById('printPatient').textContent   = t.patient?.fullName || '—';
    document.getElementById('printDept').textContent      = t.department?.name  || '—';
    document.getElementById('printTicketId').textContent  = t.ticketNumber;

    document.getElementById('printHeader').className = 'print-header' + (emerg ? ' emergency' : '');
    document.getElementById('printEmergBadge').classList.toggle('show', !!emerg);
    document.getElementById('btnPrint').className = 'btn btn-print' + (emerg ? ' emergency' : '');

    const qrImg = document.getElementById('printQr');
    qrImg.src           = `/api/tickets/${t.ticketNumber}/qr`;
    qrImg.style.display = 'block';

    document.getElementById('printModal').classList.add('show');
}

function closePrintModal() {
    document.getElementById('printModal').classList.remove('show');
    document.getElementById('patientPill').classList.remove('show');
    document.getElementById('lookupNic').value = '';
    currentNic  = null;
    isEmergency = false;
    document.getElementById('emergencyRow').classList.remove('active');
    document.getElementById('emptyState').style.display = 'flex';
    checkReady();
}

function printSlip() {
    if (currentSlipUrl) window.open(currentSlipUrl, '_blank');
}

// ── Alert helper ───────────────────────────────────────────────
function showAlert(id, msg, type) {
    const el    = document.getElementById(id);
    el.textContent = msg;
    el.className   = 'alert alert-' + type + ' show';
    setTimeout(() => el.classList.remove('show'), 5000);
}
