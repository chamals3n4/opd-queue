let stomp = null, sock = null, subscription = null, pollTimer = null, wsConnected = false;

// ── Footer URL (dynamic) ───────────────────────────────────────
document.getElementById('footerUrl').textContent = window.location.origin + '/status';

// ── Clock ──────────────────────────────────────────────────────
function tick() {
    document.getElementById('clock').textContent =
        new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false });
}
tick();
setInterval(tick, 1000);

// ── Board update ───────────────────────────────────────────────
function updateBoard(data) {
    document.getElementById('deptName').textContent     = data.departmentName || '—';
    document.getElementById('totalWaiting').textContent = data.totalWaiting   || 0;

    const el  = document.getElementById('currentTicket');
    const cur = data.currentlyCalledTicket;
    if (cur && cur !== '-' && cur !== 'Waiting...') {
        if (el.textContent !== cur) {
            el.textContent = cur;
            el.classList.remove('idle', 'flash');
            void el.offsetWidth;
            el.classList.add('flash');
        }
        el.classList.remove('idle');
        el.classList.toggle('emergency', !!data.currentEmergency);
    } else {
        el.textContent = 'Waiting...';
        el.classList.add('idle');
        el.classList.remove('emergency');
    }

    const emergencySet = new Set(data.emergencyNextTickets || []);
    const nextList = document.getElementById('nextTickets');
    const nexts    = (data.nextTickets || []).filter(t => t !== cur);
    const labels   = ['Next', '2nd', '3rd', '4th', '5th'];
    nextList.innerHTML = nexts.length
        ? nexts.slice(0, 5).map((t, i) =>
            `<div class="next-item"><span class="next-num${emergencySet.has(t) ? ' emerg' : ''}">${t}</span><span class="next-pos">${labels[i]}</span></div>`
          ).join('')
        : '<div class="next-empty">No patients waiting</div>';
}

function fetchBoard(deptId) {
    fetch(`/api/display/${deptId}`, { cache: 'no-store' }).then(r => r.json()).then(updateBoard).catch(() => {});
}

function startFallbackPoll(deptId) {
    if (pollTimer) return;
    pollTimer = setInterval(() => fetchBoard(deptId), 5000);
}

function stopFallbackPoll() {
    if (pollTimer) { clearInterval(pollTimer); pollTimer = null; }
}

// ── WebSocket + polling ────────────────────────────────────────
function connectBoard(deptId) {
    if (subscription) { try { subscription.unsubscribe(); } catch {} subscription = null; }
    const oldStomp = stomp;
    stomp = null;
    if (oldStomp) { try { oldStomp.disconnect(); } catch {} }

    fetchBoard(deptId);

    sock  = new SockJS('/ws');
    stomp = Stomp.over(sock);
    stomp.debug = null;
    stomp.connect({}, () => {
        wsConnected = true;
        stopFallbackPoll();
        subscription = stomp.subscribe(`/topic/queue/${deptId}`, msg => updateBoard(JSON.parse(msg.body)));
    }, () => {
        wsConnected = false;
        startFallbackPoll(deptId);
        setTimeout(() => connectBoard(deptId), 5000);
    });
}

connectBoard(DEPT_ID);
