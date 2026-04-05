// ── Pagination ────────────────────────────────────────────────
// page is already 0-based (Spring convention) — pass directly
function goToPage(page) {
    document.getElementById('page-input').value = page;
    document.getElementById('page-form').submit();
}

// ── Status pills ──────────────────────────────────────────────
// Wire ALL pill groups with one delegated listener
document.querySelectorAll('.filter-status-row').forEach(function(row) {
    row.addEventListener('click', function(e) {
        var pill = e.target.closest('.status-pill');
        if (!pill) return;

        // deactivate siblings in this row only
        row.querySelectorAll('.status-pill').forEach(function(p) {
            p.classList.remove('active');
        });
        pill.classList.add('active');

        // sync the single hidden input
        document.getElementById('status-input').value = pill.dataset.value || 'any';

        // keep both pill groups in sync (mobile ↔ desktop)
        document.querySelectorAll('.filter-status-row').forEach(function(otherRow) {
            if (otherRow === row) return;
            otherRow.querySelectorAll('.status-pill').forEach(function(p) {
                p.classList.toggle('active', p.dataset.value === pill.dataset.value);
            });
        });
    });
});

// ── Reset filters ─────────────────────────────────────────────
function resetFilters() {
    // uncheck all checkboxes in filter form
    document.querySelectorAll('#filter-form input[type="checkbox"]').forEach(function(cb) {
        cb.checked = false;
    });

    // reset all pill groups to "Any"
    document.querySelectorAll('.filter-status-row').forEach(function(row) {
        row.querySelectorAll('.status-pill').forEach(function(p) {
            p.classList.toggle('active', p.dataset.value === 'any');
        });
    });

    // reset hidden status input
    document.getElementById('status-input').value = 'any';
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