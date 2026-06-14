let stomp = null, sock = null, subscription = null, pollTimer = null;

// ── Footer URL (dynamic) ───────────────────────────────────────
document.getElementById('footerUrl').textContent = window.location.origin + '/status';

// ── Clock ──────────────────────────────────────────────────────
function tick() {
    document.getElementById('clock').textContent =
        new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: false });
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
    } else {
        el.textContent = 'Waiting...';
        el.classList.add('idle');
    }

    const nextList = document.getElementById('nextTickets');
    const nexts    = (data.nextTickets || []).filter(t => t !== cur);
    const labels   = ['Next', '2nd', '3rd', '4th', '5th'];
    nextList.innerHTML = nexts.length
        ? nexts.slice(0, 5).map((t, i) =>
            `<div class="next-item"><span class="next-num">${t}</span><span class="next-pos">${labels[i]}</span></div>`
          ).join('')
        : '<div class="next-empty">No patients waiting</div>';
}

// ── WebSocket + polling ────────────────────────────────────────
function connectBoard(deptId) {
    if (pollTimer)    clearInterval(pollTimer);
    if (subscription) { try { subscription.unsubscribe(); } catch {} }
    if (stomp)        { try { stomp.disconnect();         } catch {} }

    fetch(`/api/display/${deptId}`).then(r => r.json()).then(updateBoard).catch(() => {});

    sock  = new SockJS('/ws');
    stomp = Stomp.over(sock);
    stomp.debug = null;
    stomp.connect({}, () => {
        subscription = stomp.subscribe(`/topic/queue/${deptId}`, msg => updateBoard(JSON.parse(msg.body)));
    }, () => {
        setTimeout(() => connectBoard(deptId), 5000);
    });

    pollTimer = setInterval(() => {
        fetch(`/api/display/${deptId}`).then(r => r.json()).then(updateBoard).catch(() => {});
    }, 30000);
}

connectBoard(DEPT_ID);
