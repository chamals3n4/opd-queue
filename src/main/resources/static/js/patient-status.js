const ticketNumber = document.getElementById('ticketNumber').textContent.trim();
let stompClient = null;

// ── Share URL ──────────────────────────────────────────────────
document.getElementById('shareUrl').textContent = window.location.href;

function copyLink() {
    navigator.clipboard.writeText(window.location.href).then(() => {
        const el = document.getElementById('shareCopied');
        el.classList.add('show');
        setTimeout(() => el.classList.remove('show'), 2000);
    });
}

// ── Board update ───────────────────────────────────────────────
function updateUI(data) {
    const status = data.status || 'WAITING';

    document.getElementById('deptName').textContent     = data.departmentName || '—';
    document.getElementById('queuePos').textContent     = data.queuePosition  != null ? '#' + data.queuePosition  : '—';
    document.getElementById('peopleAhead').textContent  = data.peopleAhead    != null ? data.peopleAhead : '—';
    document.getElementById('waitTime').textContent     = data.estimatedWaitMinutes != null ? data.estimatedWaitMinutes + ' minutes' : '—';

    const badge = document.getElementById('statusBadge');
    badge.className = 'status-badge status-' + status;
    badge.innerHTML = `<span class="bdot"></span>${status.replace(/_/g, ' ')}`;

    const called = status === 'CALLED' || status === 'IN_PROGRESS';
    document.getElementById('calledBanner').classList.toggle('show', called);
    const header = document.getElementById('cardHeader');
    header.classList.toggle('called', called);

    const completed = status === 'COMPLETED' || status === 'NO_SHOW' || status === 'CANCELLED';
    const ahead = data.peopleAhead || 0;
    const pos   = data.queuePosition || 1;
    const pct   = completed ? 100 : (pos > 0 ? Math.round(((pos - ahead - 1) / Math.max(1, pos)) * 100) : 0);

    document.getElementById('progFill').style.width = pct + '%';
    document.getElementById('progFill').classList.toggle('complete', completed);
    document.getElementById('progLabel').textContent = completed
        ? 'Completed'
        : (ahead > 0 ? ahead + ' patient(s) ahead of you' : 'You are next!');

    document.getElementById('updTime').textContent =
        'Updated ' + new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    document.getElementById('connectionLost').classList.remove('show');
}

// ── REST fetch ─────────────────────────────────────────────────
async function fetchStatus() {
    try {
        const res = await fetch(`/api/queue/status/${ticketNumber}`);
        if (res.ok) updateUI(await res.json());
    } catch {}
}

// ── WebSocket ──────────────────────────────────────────────────
function connectWebSocket() {
    const socket = new SockJS('/ws');
    stompClient  = Stomp.over(socket);
    stompClient.debug = null;
    stompClient.connect({}, () => {
        stompClient.subscribe(`/topic/ticket/${ticketNumber}`, msg => {
            updateUI(JSON.parse(msg.body));
        });
    }, () => {
        document.getElementById('connectionLost').classList.add('show');
        setTimeout(connectWebSocket, 5000);
    });
}

fetchStatus();
connectWebSocket();
setInterval(fetchStatus, 20000);
