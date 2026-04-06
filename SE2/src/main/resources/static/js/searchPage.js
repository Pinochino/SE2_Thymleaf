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