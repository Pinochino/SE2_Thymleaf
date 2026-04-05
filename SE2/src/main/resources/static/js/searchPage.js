// ── Status pills ──────────────────────────────────────────────
document.getElementById('status-pills')
        .addEventListener('click', function(e) {
    var pill = e.target.closest('button.status-pill');
    if (!pill) return;

    e.preventDefault();

    document.querySelectorAll('#status-pills .status-pill')
            .forEach(function(p) { p.classList.remove('active'); });
    pill.classList.add('active');

    document.getElementById('status-input').value = pill.dataset.value || 'any';
});


// ── Pagination ────────────────────────────────────────────────
function goToPage(page) {
    document.getElementById('page-input').value = page;
    document.getElementById('page-form').submit();
}

// ── Save / favourite toggle ───────────────────────────────────
var csrfToken  = document.querySelector('meta[name="_csrf"]')?.content;
var csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

async function toggleSave(btn) {
    var novelId = btn.dataset.novelId;
    var isSaved = btn.classList.contains('saved');

    try {
        var headers = { 'Content-Type': 'application/json' };
        if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;

        var res = await fetch('/favorites/toggle', {
            method: 'POST',
            headers: headers,
            body: JSON.stringify({ novelId: novelId, action: isSaved ? 'remove' : 'add' })
        });

        if (!res.ok) throw new Error('failed');

        btn.classList.toggle('saved');
        var nowSaved = btn.classList.contains('saved');
        btn.querySelector('svg').setAttribute('fill', nowSaved ? 'currentColor' : 'none');
        btn.querySelector('span').textContent = nowSaved ? 'Saved' : 'Save';

    } catch (e) {
        console.error('toggleSave:', e);
    }
}