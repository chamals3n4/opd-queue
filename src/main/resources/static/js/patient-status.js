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
    const badge = document.getElementById('statusBadge');
    badge.className = 'status-badge status-' + status;
    badge.innerHTML = `<span class="bdot"></span>${status.replace(/_/g, ' ')}`;

    const called = status === 'CALLED' || status === 'IN_PROGRESS';
    document.getElementById('calledBanner').classList.toggle('show', called);
    const header = document.getElementById('cardHeader');
    header.classList.toggle('called', called);

    const completed = status === 'COMPLETED' || status === 'NO_SHOW' || status === 'CANCELLED';

    document.getElementById('progFill').style.width = completed ? '100%' : '0%';
    document.getElementById('progFill').classList.toggle('complete', completed);
    document.getElementById('progLabel').textContent = completed ? 'Completed' : 'In queue';

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
