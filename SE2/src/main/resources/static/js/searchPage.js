// ── Status pills ──────────────────────────────────────────────
var statusPills = document.getElementById('status-pills');
if (statusPills) {
    statusPills.addEventListener('click', function(e) {
        var pill = e.target.closest('button.status-pill');
        if (!pill) return;

        e.preventDefault();

        document.querySelectorAll('#status-pills .status-pill')
                .forEach(function(p) { p.classList.remove('active'); });
        pill.classList.add('active');

        var statusInput = document.getElementById('status-input');
        if (statusInput) statusInput.value = pill.dataset.value || 'any';
    });
}

// ── Pagination ────────────────────────────────────────────────
function goToPage(page) {
    document.getElementById('page-input').value = page;
    document.getElementById('page-form').submit();
}

// ── Save / favourite toggle ───────────────────────────────────
var csrfToken  = document.querySelector('meta[name="_csrf"]')?.content;
var csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

document.querySelectorAll('.btn-save').forEach(function(btn) {
    btn.addEventListener('click', function() { toggleSave(btn); });
});

async function toggleSave(btn) {
    var novelId = btn.dataset.novelId;
    if (!novelId) return;

    try {
        var headers = { 'Content-Type': 'application/json' };
        if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;

        var res = await fetch('/api/novels/' + novelId + '/favorite', {
            method: 'POST',
            headers: headers,
            body: '{}',
            credentials: 'same-origin'
        });

        if (!res.ok) throw new Error('failed');
        var data = await res.json();

        if (data.favorited) {
            btn.classList.add('saved');
            btn.querySelector('svg').setAttribute('fill', 'currentColor');
            btn.querySelector('span').textContent = 'Saved';
        } else {
            btn.classList.remove('saved');
            btn.querySelector('svg').setAttribute('fill', 'none');
            btn.querySelector('span').textContent = 'Save';
        }
    } catch (e) {
        console.error('toggleSave:', e);
    }
}
