/**
 * searchPage.js - Search page interactivity
 * Handles filter persistence, reset, status pills, save button feedback, pagination.
 */

document.addEventListener('DOMContentLoaded', function() {
    // Status value mappings: display -> backend
    const statusMap = {
        'any': 'any',
'ongoing': 'ONGOING',
'complete': 'COMPLETED',
'hiatus': 'HIATUS'
    };

    // Parse URL params for persistence
    const urlParams = new URLSearchParams(window.location.search);
    const statusStr = urlParams.get('statusStr') || 'any';
    const genres = urlParams.getAll('genres');
    const isTrending = urlParams.get('trending') === 'true';

    // Restore checkboxes
    document.querySelectorAll('#filter-form input[type=\"checkbox\"][name=\"genres\"]').forEach(cb => {
        if (genres.includes(cb.value)) {
            cb.checked = true;
        }
    });
    const trendingCb = document.querySelector('input[name=\"trending\"]');
    if (trendingCb && isTrending) {
        trendingCb.checked = true;
    }

    // Restore status pill
    const statusInput = document.getElementById('status-input');
    statusInput.value = statusStr;
    const activePill = Array.from(document.querySelectorAll('#status-pills .status-pill')).find(pill => {
        const mapped = statusMap[pill.dataset.value] || pill.dataset.value;
        return mapped.toLowerCase() === statusStr.toLowerCase();
    });
    if (activePill) {
        activePill.classList.add('active');
    } else {
        // Default to 'any'
        const anyPill = document.querySelector('.status-pill[data-value=\"any\"]');
        if (anyPill) anyPill.classList.add('active');
    }

    // Status pill click handlers
    document.querySelectorAll('#status-pills .status-pill').forEach(pill => {
        pill.addEventListener('click', function() {
            document.querySelectorAll('#status-pills .status-pill').forEach(p => p.classList.remove('active'));
            this.classList.add('active');
            statusInput.value = statusMap[this.dataset.value] || this.dataset.value;
        });
    });

    // Save button feedback (transparent red on click)
    document.querySelectorAll('.btn-save').forEach(btn => {
        btn.addEventListener('click', function(e) {
            e.stopPropagation();
            this.classList.toggle('saved');
            this.classList.add('btn-save-clicked');
            // Remove click effect after 300ms
            setTimeout(() => {
                this.classList.remove('btn-save-clicked');
            }, 300);
            // TODO: Real save API call with this.dataset.novelId if needed
        });
    });
});

// Reset filters
function resetFilters() {
    // Uncheck all checkboxes
    document.querySelectorAll('#filter-form input[type=\"checkbox\"]').forEach(cb => {
        cb.checked = false;
    });

    // Reset status to 'any'
    const statusInput = document.getElementById('status-input');
    statusInput.value = 'any';
    document.querySelectorAll('#status-pills .status-pill').forEach(pill => pill.classList.remove('active'));
    const anyPill = document.querySelector('.status-pill[data-value=\"any\"]');
    if (anyPill) anyPill.classList.add('active');
}

// Pagination
function goToPage(page) {
    document.getElementById('page-input').value = page;
    document.getElementById('page-form').submit();
}

// Save toggle (compatibility with inline onclick)
function toggleSave(btn) {
    btn.classList.toggle('saved');
    btn.classList.add('btn-save-clicked');
    setTimeout(() => {
        btn.classList.remove('btn-save-clicked');
    }, 300);
    // Note: toggleSave used in HTML onclick; event listeners above for dynamic cards
}
