// === Thymeleaf-injected variables ===
/*<![CDATA[*/
const chapterId           = /*[[${chapter.id}]]*/ 0;
const isLoggedIn          = /*[[${isLoggedIn}]]*/ false;
const paragraphTexts      = /*[[${paragraphs}]]*/ [];
const savedFontSize       = /*[[${readingSettings != null && readingSettings.fontSize != null ? readingSettings.fontSize.name() : 'MEDIUM'}]]*/ 'MEDIUM';
const prevChapterUrl      = /*[[${prevChapter != null ? '/novels/chapter/' + prevChapter.id : null}]]*/ null;
const nextChapterUrl      = /*[[${nextChapter != null ? '/novels/chapter/' + nextChapter.id : null}]]*/ null;
const savedParagraphIndex = /*[[${lastParagraphIndex != null ? lastParagraphIndex : 0}]]*/ 0;
/*]]>*/

// === LocalStorage settings (applied instantly on page load) ===
(function() {
    var sizeMap = { 'SMALL': 16, 'MEDIUM': 20, 'LARGE': 24, 'EXTRA_LARGE': 28 };

    // Theme: localStorage overrides server setting
    var lsTheme = localStorage.getItem('reader-theme');
    if (lsTheme) {
        document.body.classList.remove('theme-light', 'theme-sepia', 'theme-dark');
        document.body.classList.add('theme-' + lsTheme);
        document.querySelectorAll('.theme-btn').forEach(function(b) { b.classList.remove('active'); b.innerHTML = ''; });
        var activeBtn = document.querySelector('.theme-btn[data-theme="' + lsTheme + '"]');
        if (activeBtn) {
            activeBtn.classList.add('active');
            activeBtn.innerHTML = '<svg width="17" height="13" viewBox="0 0 17 13" fill="none"><path d="M1.5 7L6 11.5L15.5 1.5" stroke="#8b5cf6" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>';
        }
    }

    // Font family: localStorage overrides server setting
    var lsFont = localStorage.getItem('reader-font');
    if (lsFont) {
        if (lsFont === 'sans-serif') {
            document.body.classList.add('font-sans-serif');
        } else {
            document.body.classList.remove('font-sans-serif');
        }
        document.querySelectorAll('.font-toggle-btn').forEach(function(b) { b.classList.remove('active'); });
        var activeFont = document.querySelector('.font-toggle-btn[data-font="' + lsFont + '"]');
        if (activeFont) activeFont.classList.add('active');
    }

    // Font size: localStorage overrides server setting
    var lsSize = localStorage.getItem('reader-size');
    var px = lsSize ? parseInt(lsSize) : (sizeMap[savedFontSize] || 20);
    var content = document.getElementById('chapterContent');
    if (content) content.style.fontSize = px + 'px';
    var slider = document.querySelector('.size-slider');
    if (slider) slider.value = px;
})();

// === CSRF helper ===
var csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
var csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

function apiPost(url, data) {
    var headers = { 'Content-Type': 'application/json' };
    if (csrfHeader && csrfToken) {
        headers[csrfHeader] = csrfToken;
    }
    return fetch(url, {
        method: 'POST',
        headers: headers,
        body: JSON.stringify(data),
        credentials: 'same-origin'
    }).then(function(r) { return r.json(); });
}

// === Sidebar functions ===
var activeParagraphIndex = null;

function openSidebar(id) {
    closeSidebars();
    document.getElementById(id).classList.add('open');
    document.getElementById('sidebarOverlay').classList.add('active');
}

function closeSidebars() {
    document.querySelectorAll('.chapter-sidebar').forEach(function(s) { s.classList.remove('open'); });
    document.getElementById('sidebarOverlay').classList.remove('active');
}

// === Dropdown functions ===
function toggleChapterDropdown(name) {
    var allDropdowns = document.querySelectorAll('.chapter-dropdown');
    var target = document.getElementById('dropdown-' + name);
    if (!target) {
        allDropdowns.forEach(function(d) { d.classList.remove('active'); });
        return;
    }
    var isOpen = target.classList.contains('active');
    allDropdowns.forEach(function(d) { d.classList.remove('active'); });
    if (!isOpen) target.classList.add('active');
}

document.addEventListener('click', function(e) {
    if (!e.target.closest('.chapter-dropdown-wrapper')) {
        document.querySelectorAll('.chapter-dropdown').forEach(function(d) { d.classList.remove('active'); });
    }
});

// === Reply state ===
var replyToCommentId = null;
var replyIndicator = document.getElementById('replyIndicator');
var replyToName = document.getElementById('replyToName');
var replyToPreview = document.getElementById('replyToPreview');
var replyCancelBtn = document.getElementById('replyCancelBtn');

function setReplyTo(commentId, userName, commentText) {
    if (!replyIndicator) return;
    replyToCommentId = commentId;
    var preview = commentText.length > 60 ? commentText.substring(0, 57) + '...' : commentText;
    replyToName.textContent = userName;
    replyToPreview.textContent = preview;
    replyIndicator.style.display = '';
    var input = document.getElementById('commentInput');
    if (input) {
        input.placeholder = 'Reply to ' + userName + '...';
        input.focus();
    }
}

function clearReply() {
    replyToCommentId = null;
    if (replyIndicator) replyIndicator.style.display = 'none';
    if (replyToName) replyToName.textContent = '';
    if (replyToPreview) replyToPreview.textContent = '';
    var input = document.getElementById('commentInput');
    if (input) input.placeholder = 'Add a comment...';
}

if (replyCancelBtn) {
    replyCancelBtn.addEventListener('click', clearReply);
}

// === Comment action buttons (open sidebar for specific paragraph) ===
document.querySelectorAll('.comment-action-btn').forEach(function(btn) {
    btn.addEventListener('click', function() {
        activeParagraphIndex = parseInt(this.getAttribute('data-paragraph-index'));
        clearReply();
        loadParagraphComments(activeParagraphIndex);
        openSidebar('commentsSidebar');
    });
});

// === Mobile Paragraph Selection & Action Bar ===
var selectedParagraphIndex = null;

function showParagraphActionBar(index) {
    var actionBar = document.getElementById('paragraphActionBar');
    var allParagraphs = document.querySelectorAll('.chapter-paragraph');

    if (selectedParagraphIndex === index) {
        hideParagraphActionBar();
        return;
    }

    allParagraphs.forEach(function(p) { p.classList.remove('selected'); });
    if (index < allParagraphs.length) {
        allParagraphs[index].classList.add('selected');
    }

    selectedParagraphIndex = index;
    activeParagraphIndex = index;

    var countSpan = document.getElementById('commentCountBar');
    var commentCountBtn = document.querySelector('.comment-action-btn[data-paragraph-index="' + index + '"] .comment-count');
    if (commentCountBtn && commentCountBtn.textContent) {
        countSpan.textContent = commentCountBtn.textContent;
        countSpan.style.display = 'block';
    } else {
        countSpan.textContent = '';
        countSpan.style.display = 'none';
    }

    var bookmarkBtn = document.querySelector('.bookmark-action-btn[data-paragraph-index="' + index + '"]');
    var actionBarBookmarkBtn = document.getElementById('bookmarkActionBarBtn');
    if (bookmarkBtn && bookmarkBtn.classList.contains('bookmarked')) {
        actionBarBookmarkBtn.classList.add('bookmarked');
    } else {
        actionBarBookmarkBtn.classList.remove('bookmarked');
    }

    actionBar.style.display = 'flex';
}

function hideParagraphActionBar() {
    var actionBar = document.getElementById('paragraphActionBar');
    actionBar.style.display = 'none';
    var allParagraphs = document.querySelectorAll('.chapter-paragraph');
    allParagraphs.forEach(function(p) { p.classList.remove('selected'); });
    selectedParagraphIndex = null;
}

function setupMobileParagraphClickHandlers() {
    var isMobile = window.innerWidth <= 640;
    if (!isMobile) return;

    var paragraphs = document.querySelectorAll('.chapter-paragraph');
    paragraphs.forEach(function(p, index) {
        p.addEventListener('click', function(e) {
            e.stopPropagation();
            showParagraphActionBar(index);
        });
    });
}

document.addEventListener('click', function(e) {
    if (e.target.closest('.paragraph-action-bar') ||
        e.target.closest('.bookmark-action-bar-btn') ||
        e.target.closest('.comment-action-bar-btn')) {
        return;
    }
    if (selectedParagraphIndex !== null && !e.target.closest('.chapter-paragraph')) {
        hideParagraphActionBar();
    }
});

var bookmarkActionBarBtn = document.getElementById('bookmarkActionBarBtn');
var commentActionBarBtn = document.getElementById('commentActionBarBtn');

if (bookmarkActionBarBtn) {
    bookmarkActionBarBtn.addEventListener('click', function(e) {
        e.stopPropagation();
        if (selectedParagraphIndex !== null) {
            var bookmarkBtn = document.querySelector('.bookmark-action-btn[data-paragraph-index="' + selectedParagraphIndex + '"]');
            if (bookmarkBtn) bookmarkBtn.click();
        }
    });
}

if (commentActionBarBtn) {
    commentActionBarBtn.addEventListener('click', function(e) {
        e.stopPropagation();
        if (selectedParagraphIndex !== null) {
            clearReply();
            loadParagraphComments(selectedParagraphIndex);
            openSidebar('commentsSidebar');
            hideParagraphActionBar();
        }
    });
}

setupMobileParagraphClickHandlers();

var sidebarOverlay = document.getElementById('sidebarOverlay');
if (sidebarOverlay) {
    sidebarOverlay.addEventListener('click', function() {
        setupMobileParagraphClickHandlers();
    });
}

// === Build comment HTML with reply button ===
function buildCommentHtml(c, isNested) {
    var sizeClass = isNested ? ' comment-avatar-sm' : '';
    var nameStyle = isNested ? ' style="font-size:12px;"' : '';
    var textStyle = isNested ? ' style="font-size:12px;line-height:16.5px;"' : '';
    var avatarContent = c.avatarUrl
        ? '<img src="' + escapeHtml(c.avatarUrl) + '" alt="avatar" style="width:100%;height:100%;object-fit:cover;border-radius:9999px;"/>'
        : escapeHtml(c.userInitial);
    var replyAction = '';
    if (isLoggedIn && !isNested) {
        replyAction = '<span class="comment-reply comment-reply-btn" data-comment-id="' + c.id +
            '" data-user-name="' + escapeHtml(c.userName) +
            '" data-comment-text="' + escapeHtml(c.content).replace(/"/g, '&quot;') + '">Reply</span>';
    }
    var timeAgo = c.timeAgo ? escapeHtml(c.timeAgo) : '';
    return '<div class="comment-item' + (isNested ? ' comment-item-nested' : '') + '">' +
        '<div class="comment-avatar' + sizeClass + '">' + avatarContent + '</div>' +
        '<div class="comment-content">' +
            '<div class="comment-header">' +
                '<span class="comment-name"' + nameStyle + '>' + escapeHtml(c.userName) + '</span>' +
                '<span class="comment-time">' + timeAgo + '</span>' +
            '</div>' +
            '<p class="comment-text"' + textStyle + '>' + escapeHtml(c.content) + '</p>' +
            '<div class="comment-actions">' + replyAction + '</div>' +
        '</div>' +
    '</div>';
}

// === Load comments for a specific paragraph ===
function loadParagraphComments(pIndex) {
    var commentsBody = document.getElementById('commentsBody');
    var quoteEl = document.getElementById('commentQuote');
    var quoteText = document.getElementById('commentQuoteText');

    if (pIndex < paragraphTexts.length) {
        var text = paragraphTexts[pIndex];
        if (text.length > 200) text = text.substring(0, 197) + '...';
        quoteText.textContent = '"' + text + '"';
        quoteEl.style.display = '';
    } else {
        quoteEl.style.display = 'none';
    }

    commentsBody.innerHTML = '<p style="padding: 20px; color: #9ca3af; text-align: center;">Loading comments...</p>';

    fetch('/api/chapter/' + chapterId + '/comments?paragraphIndex=' + pIndex, {
        credentials: 'same-origin'
    }).then(function(r) { return r.json(); })
    .then(function(comments) {
        commentsBody.innerHTML = '';
        if (comments.length === 0) {
            commentsBody.innerHTML = '<p class="comments-empty-msg" style="padding: 20px; color: #9ca3af; text-align: center;">No comments on this paragraph yet. Be the first!</p>';
            return;
        }
        comments.forEach(function(c) {
            commentsBody.insertAdjacentHTML('beforeend', buildCommentHtml(c, false));
            if (c.replies && c.replies.length > 0) {
                c.replies.forEach(function(r) {
                    commentsBody.insertAdjacentHTML('beforeend',
                        '<div class="comment-reply-thread">' + buildCommentHtml(r, true) + '</div>');
                });
            }
        });
        commentsBody.querySelectorAll('.comment-reply-btn').forEach(function(btn) {
            btn.addEventListener('click', function() {
                setReplyTo(
                    parseInt(this.getAttribute('data-comment-id')),
                    this.getAttribute('data-user-name'),
                    this.getAttribute('data-comment-text')
                );
            });
        });
    }).catch(function(err) {
        commentsBody.innerHTML = '<p style="padding: 20px; color: #e74c3c; text-align: center;">Failed to load comments.</p>';
        console.error('Failed to load comments:', err);
    });
}

// === Post comment ===
var commentInput = document.getElementById('commentInput');
var commentSendBtn = document.getElementById('commentSendBtn');

if (commentSendBtn && commentInput) {
    commentSendBtn.addEventListener('click', submitComment);
    commentInput.addEventListener('keydown', function(e) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            submitComment();
        }
    });
}

function submitComment() {
    var content = commentInput.value.trim();
    if (!content || activeParagraphIndex === null) return;

    commentInput.disabled = true;
    commentSendBtn.disabled = true;

    var payload = {
        paragraphIndex: activeParagraphIndex,
        content: content
    };
    if (replyToCommentId !== null) {
        payload.parentCommentId = replyToCommentId;
    }

    apiPost('/api/chapter/' + chapterId + '/comments', payload).then(function(data) {
        if (data.error) {
            alert(data.error);
            return;
        }
        loadParagraphComments(activeParagraphIndex);

        if (!replyToCommentId) {
            var countSpan = document.querySelector('.comment-action-btn[data-paragraph-index="' + activeParagraphIndex + '"] .comment-count');
            if (countSpan) {
                countSpan.textContent = parseInt(countSpan.textContent) + 1;
            } else {
                var btn = document.querySelector('.comment-action-btn[data-paragraph-index="' + activeParagraphIndex + '"]');
                if (btn) {
                    var span = document.createElement('span');
                    span.className = 'comment-count';
                    span.textContent = '1';
                    btn.appendChild(span);
                }
            }
        }

        commentInput.value = '';
        clearReply();
    }).catch(function(err) {
        console.error('Failed to post comment:', err);
    }).finally(function() {
        commentInput.disabled = false;
        commentSendBtn.disabled = false;
        commentInput.focus();
    });
}

// === Toggle bookmark ===
document.querySelectorAll('.bookmark-action-btn').forEach(function(btn) {
    btn.addEventListener('click', function() {
        if (!isLoggedIn) return;
        var pIndex = parseInt(this.getAttribute('data-paragraph-index'));
        var btnRef = this;

        apiPost('/api/chapter/' + chapterId + '/bookmarks', {
            paragraphIndex: pIndex
        }).then(function(data) {
            if (data.error) {
                alert(data.error);
                return;
            }
            if (data.bookmarked) {
                btnRef.classList.add('bookmarked');
                addBookmarkToDropdown(pIndex);
            } else {
                btnRef.classList.remove('bookmarked');
                removeBookmarkFromDropdown(pIndex);
            }
            if (selectedParagraphIndex === pIndex) {
                var actionBar = document.getElementById('paragraphActionBar');
                if (actionBar && actionBar.style.display === 'flex') {
                    var actionBarBookmarkBtn = document.getElementById('bookmarkActionBarBtn');
                    if (actionBarBookmarkBtn) {
                        if (data.bookmarked) {
                            actionBarBookmarkBtn.classList.add('bookmarked');
                        } else {
                            actionBarBookmarkBtn.classList.remove('bookmarked');
                        }
                    }
                }
            }
        }).catch(function(err) {
            console.error('Failed to toggle bookmark:', err);
        });
    });
});

function addBookmarkToDropdown(pIndex) {
    var list = document.getElementById('bookmarksList');
    if (!list) return;
    var emptyMsg = list.querySelector('.bookmark-empty-msg');
    if (emptyMsg) emptyMsg.remove();

    var text = pIndex < paragraphTexts.length ? paragraphTexts[pIndex] : '';
    if (text.length > 120) text = text.substring(0, 117) + '...';

    var div = document.createElement('div');
    div.className = 'bookmark-item';
    div.setAttribute('data-paragraph-index', pIndex);
    div.style.cursor = 'pointer';
    div.innerHTML = '<p class="bookmark-text">' + escapeHtml(text) + '</p>';
    div.addEventListener('click', function() {
        var paragraphs = document.querySelectorAll('.paragraph-wrapper');
        if (paragraphs[pIndex]) {
            paragraphs[pIndex].scrollIntoView({ behavior: 'smooth', block: 'center' });
            toggleChapterDropdown('__close__');
        }
    });
    list.appendChild(div);
}

function removeBookmarkFromDropdown(pIndex) {
    var list = document.getElementById('bookmarksList');
    if (!list) return;
    var item = list.querySelector('.bookmark-item[data-paragraph-index="' + pIndex + '"]');
    if (item) {
        var divider = item.nextElementSibling;
        if (divider && divider.classList.contains('bookmark-divider')) divider.remove();
        item.remove();
    }
    if (!list.querySelector('.bookmark-item')) {
        list.innerHTML = '<p class="bookmark-empty-msg" style="padding: 12px; color: #9ca3af; font-size: 13px;">No bookmarks yet. Click the bookmark icon on any paragraph to save it.</p>';
    }
}

// === Theme buttons (with persist) ===
document.querySelectorAll('.theme-btn').forEach(function(btn) {
    btn.addEventListener('click', function() {
        document.querySelectorAll('.theme-btn').forEach(function(b) { b.classList.remove('active'); b.innerHTML = ''; });
        btn.classList.add('active');
        btn.innerHTML = '<svg width="17" height="13" viewBox="0 0 17 13" fill="none"><path d="M1.5 7L6 11.5L15.5 1.5" stroke="#8b5cf6" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>';
        var theme = btn.getAttribute('data-theme');
        document.body.classList.remove('theme-light', 'theme-sepia', 'theme-dark');
        document.body.classList.add('theme-' + theme);
        localStorage.setItem('reader-theme', theme);
        if (isLoggedIn) {
            apiPost('/api/chapter/settings', { theme: theme });
        }
    });
});

// === Font family toggle (with persist) ===
document.querySelectorAll('.font-toggle-btn').forEach(function(btn) {
    btn.addEventListener('click', function() {
        document.querySelectorAll('.font-toggle-btn').forEach(function(b) { b.classList.remove('active'); });
        btn.classList.add('active');
        var font = btn.getAttribute('data-font');
        if (font === 'sans-serif') {
            document.body.classList.add('font-sans-serif');
        } else {
            document.body.classList.remove('font-sans-serif');
        }
        localStorage.setItem('reader-font', font);
        if (isLoggedIn) {
            var fontFamily = font === 'sans-serif' ? 'SANS_SERIF' : 'SERIF';
            apiPost('/api/chapter/settings', { fontFamily: fontFamily });
        }
    });
});

// === Font size slider (with debounced persist) ===
var sizeSlider = document.querySelector('.size-slider');
var sizeDebounce = null;
if (sizeSlider) {
    sizeSlider.addEventListener('input', function() {
        var val = this.value;
        document.getElementById('chapterContent').style.fontSize = val + 'px';
        localStorage.setItem('reader-size', val);
        if (isLoggedIn) {
            clearTimeout(sizeDebounce);
            sizeDebounce = setTimeout(function() {
                var sizeEnum;
                if (val <= 16) sizeEnum = 'SMALL';
                else if (val <= 20) sizeEnum = 'MEDIUM';
                else if (val <= 24) sizeEnum = 'LARGE';
                else sizeEnum = 'EXTRA_LARGE';
                apiPost('/api/chapter/settings', { fontSize: sizeEnum });
            }, 500);
        }
    });
}

// === Reading progress bar ===
function updateProgressBar() {
    var scrollTop = window.scrollY;
    var docHeight = document.documentElement.scrollHeight - window.innerHeight;
    var progress = docHeight > 0 ? (scrollTop / docHeight) * 100 : 0;
    var fill = document.querySelector('.progress-fill');
    if (fill) {
        fill.style.width = progress + '%';
        fill.setAttribute('aria-valuenow', Math.round(progress));
    }
}
window.addEventListener('scroll', updateProgressBar);
updateProgressBar();

// === Save reading progress (debounced on scroll) ===
var progressDebounce = null;
window.addEventListener('scroll', function() {
    if (!isLoggedIn) return;
    clearTimeout(progressDebounce);
    progressDebounce = setTimeout(function() {
        var scrollPercent = Math.round((window.scrollY / Math.max(1, document.documentElement.scrollHeight - window.innerHeight)) * 100);
        apiPost('/api/chapter/' + chapterId + '/progress', {
            position: scrollPercent
        });
    }, 2000);
});

// === TOC search filter ===
var tocSearch = document.getElementById('tocSearchInput');
if (tocSearch) {
    tocSearch.addEventListener('input', function() {
        var query = this.value.toLowerCase();
        document.querySelectorAll('.toc-chapter').forEach(function(ch) {
            ch.style.display = ch.textContent.toLowerCase().includes(query) ? '' : 'none';
        });
    });
}

// === Bookmark click: scroll to paragraph ===
document.querySelectorAll('.bookmark-item[data-paragraph-index]').forEach(function(item) {
    item.style.cursor = 'pointer';
    item.addEventListener('click', function() {
        var idx = parseInt(this.getAttribute('data-paragraph-index'));
        var paragraphs = document.querySelectorAll('.paragraph-wrapper');
        if (paragraphs[idx]) {
            paragraphs[idx].scrollIntoView({ behavior: 'smooth', block: 'center' });
            toggleChapterDropdown('__close__');
        }
    });
});

// === Chapter navigation: swipe (mobile/tablet) + arrow keys (desktop) ===
function navigateChapter(url) {
    if (url) window.location.href = url;
}

// Arrow keys (desktop)
document.addEventListener('keydown', function(e) {
    if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return;
    if (e.key === 'ArrowLeft') {
        e.preventDefault();
        navigateChapter(prevChapterUrl);
    } else if (e.key === 'ArrowRight') {
        e.preventDefault();
        navigateChapter(nextChapterUrl);
    }
});

// Swipe detection (mobile/tablet)
(function() {
    var touchStartX = 0;
    var touchStartY = 0;
    var touchEndX = 0;
    var touchEndY = 0;
    var swiping = false;
    var swipeIndicator = document.getElementById('swipeIndicator');
    var swipeLabel = document.getElementById('swipeLabel');
    var minSwipe = 80;

    document.addEventListener('touchstart', function(e) {
        touchStartX = e.changedTouches[0].screenX;
        touchStartY = e.changedTouches[0].screenY;
        swiping = false;
    }, { passive: true });

    document.addEventListener('touchmove', function(e) {
        touchEndX = e.changedTouches[0].screenX;
        touchEndY = e.changedTouches[0].screenY;
        var dx = touchEndX - touchStartX;
        var dy = Math.abs(touchEndY - touchStartY);

        if (Math.abs(dx) > 40 && Math.abs(dx) > dy * 1.5) {
            swiping = true;
            if (swipeIndicator) {
                if (dx > 0 && prevChapterUrl) {
                    swipeIndicator.className = 'swipe-indicator swipe-indicator-left active';
                    swipeLabel.textContent = Math.abs(dx) > minSwipe ? 'Release for previous chapter' : 'Swipe for previous chapter';
                } else if (dx < 0 && nextChapterUrl) {
                    swipeIndicator.className = 'swipe-indicator swipe-indicator-right active';
                    swipeLabel.textContent = Math.abs(dx) > minSwipe ? 'Release for next chapter' : 'Swipe for next chapter';
                } else {
                    swipeIndicator.className = 'swipe-indicator';
                }
            }
        }
    }, { passive: true });

    document.addEventListener('touchend', function(e) {
        if (swipeIndicator) {
            swipeIndicator.className = 'swipe-indicator';
        }
        if (!swiping) return;
        var dx = touchEndX - touchStartX;
        var dy = Math.abs(touchEndY - touchStartY);
        if (Math.abs(dx) > minSwipe && Math.abs(dx) > dy * 1.5) {
            if (dx > 0) {
                navigateChapter(prevChapterUrl);
            } else {
                navigateChapter(nextChapterUrl);
            }
        }
    }, { passive: true });
})();

// === Utility ===
function escapeHtml(str) {
    var div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

// === Auto-save Reading Progress by Paragraph Index ===
(function () {
    if (!isLoggedIn || !chapterId) return;

    const _csrfToken  = document.querySelector('meta[name="_csrf"]')?.content;
    const _csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    const paragraphs = Array.from(document.querySelectorAll('.chapter-paragraph'));
    if (paragraphs.length === 0) return;

    let saveTimer = null;
    let lastSavedIndex = -1;

    function getVisibleParagraphIndex() {
        const viewportMid = window.scrollY + window.innerHeight / 2;
        let closestIndex = 0;
        let closestDist = Infinity;

        paragraphs.forEach((p, idx) => {
            const rect = p.getBoundingClientRect();
            const absTop = rect.top + window.scrollY;
            const dist = Math.abs(absTop - viewportMid);
            if (dist < closestDist) {
                closestDist = dist;
                closestIndex = idx;
            }
        });
        return closestIndex;
    }

    function saveProgress(paragraphIndex) {
        if (paragraphIndex === lastSavedIndex) return;
        lastSavedIndex = paragraphIndex;

        const headers = { 'Content-Type': 'application/json' };
        if (_csrfHeader && _csrfToken) headers[_csrfHeader] = _csrfToken;

        fetch(`/api/chapter/${chapterId}/progress`, {
            method: 'POST',
            headers,
            body: JSON.stringify({ paragraphIndex }),
            credentials: 'same-origin',
            keepalive: true
        }).catch(() => {});
    }

    window.addEventListener('scroll', () => {
        clearTimeout(saveTimer);
        saveTimer = setTimeout(() => {
            const idx = getVisibleParagraphIndex();
            saveProgress(idx);
        }, 1500);
    });

    window.addEventListener('visibilitychange', () => {
        if (document.visibilityState === 'hidden') {
            const idx = getVisibleParagraphIndex();
            if (idx !== lastSavedIndex) {
                const headers = { 'Content-Type': 'application/json' };
                if (_csrfHeader && _csrfToken) headers[_csrfHeader] = _csrfToken;
                fetch(`/novels/chapter/${chapterId}/progress`, {
                    method: 'POST',
                    headers,
                    body: JSON.stringify({ paragraphIndex: idx }),
                    credentials: 'same-origin',
                    keepalive: true
                }).catch(() => {});
            }
        }
    });

    // Restore scroll position if user returns to a chapter in progress.
    if (savedParagraphIndex > 0 && paragraphs[savedParagraphIndex]) {
        setTimeout(() => {
            paragraphs[savedParagraphIndex].scrollIntoView({ behavior: 'smooth', block: 'center' });
        }, 300);
    }
})();

// === Content Protection ===
(function() {
    document.addEventListener('selectstart', function(e) {
        if (e.target.closest('.chapter-content')) e.preventDefault();
    });

    document.addEventListener('keydown', function(e) {
        if ((e.ctrlKey || e.metaKey) && ['c','p','s','u'].includes(e.key.toLowerCase())) {
            e.preventDefault();
        }
        if (e.key === 'F12') e.preventDefault();
        if ((e.ctrlKey || e.metaKey) && e.shiftKey && ['i','j','c'].includes(e.key.toLowerCase())) {
            e.preventDefault();
        }
    });

    document.addEventListener('contextmenu', function(e) {
        if (e.target.closest('.chapter-content')) e.preventDefault();
    });

    // Detect devtools open via debugger timing
    (function detectDevTools() {
        var threshold = 160;
        setInterval(function() {
            var start = performance.now();
            debugger;
            if (performance.now() - start > threshold) {
                document.body.innerHTML = '<div style="display:flex;align-items:center;justify-content:center;height:100vh;background:#0f1115;color:#f87171;font-family:sans-serif;font-size:18px;">Content protection: Developer tools detected.</div>';
            }
        }, 1000);
    })();
})();