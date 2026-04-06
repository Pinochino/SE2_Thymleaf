        function toggleSynopsis() {
            const text = document.getElementById('synopsisText');
            const btn = document.getElementById('readMoreBtn');
            text.classList.toggle('expanded');
            btn.classList.toggle('expanded');
            btn.childNodes[0].textContent = text.classList.contains('expanded') ? 'Show Less ' : 'Read More ';
        }

        // CSRF helpers
        var csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        var csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

        function apiPost(url, data) {
            var headers = {'Content-Type': 'application/json'};
            if (csrfHeader && csrfToken) { headers[csrfHeader] = csrfToken; }
            return fetch(url, { method: 'POST', headers: headers, body: JSON.stringify(data), credentials: 'same-origin' }).then(r => r.json());
        }

        function apiGet(url) {
            return fetch(url, { credentials: 'same-origin' }).then(r => r.json());
        }

        // ── Star Rating ──
        var userRating = 0;
        var rateStarsContainer = document.querySelector('.nd-rate-stars');
        var novelIdForRating = rateStarsContainer?.getAttribute('data-novel-id');

        function renderRateStars(score) {
            document.querySelectorAll('.nd-rate-star').forEach((s, i) => {
                const path = s.querySelector('path');
                if (i < score) {
                    path.setAttribute('fill', '#c9a46a');
                    path.setAttribute('stroke', 'none');
                } else {
                    path.setAttribute('fill', 'none');
                    path.setAttribute('stroke', '#94a3b8');
                }
            });
        }

        // Load user's existing rating
        if (novelIdForRating) {
            apiGet('/api/novels/' + novelIdForRating + '/rating/user').then(data => {
                userRating = data.score || 0;
                renderRateStars(userRating);
            });
        }

        document.querySelectorAll('.nd-rate-star').forEach((star, idx) => {
            star.addEventListener('mouseenter', () => renderRateStars(idx + 1));
            star.addEventListener('click', () => {
                var score = parseInt(star.getAttribute('data-score'));
                apiPost('/api/novels/' + novelIdForRating + '/rate', { score: score }).then(data => {
                    userRating = data.score;
                    renderRateStars(userRating);

                    // Update displayed average rating
                    var avgEl = document.querySelector('.nd-rating-big');
                    if (avgEl) avgEl.textContent = data.averageRating;

                    // Update rating count display
                    var countEl = document.querySelector('.nd-rating-total');
                    if (countEl) {
                        countEl.textContent = data.averageRating + ' (' + data.ratingCount + ' rating' + (data.ratingCount !== 1 ? 's' : '') + ')';
                    }

                    // Update stars in the rating box display
                    document.querySelectorAll('.nd-stars svg path').forEach((path, i) => {
                        if ((i + 1) <= Math.round(data.averageRating)) {
                            path.setAttribute('fill', '#c9a46a');
                            path.setAttribute('stroke', 'none');
                        } else {
                            path.setAttribute('fill', 'none');
                            path.setAttribute('stroke', '#94a3b8');
                        }
                    });
                });
            });
        });

        rateStarsContainer?.addEventListener('mouseleave', () => renderRateStars(userRating));

        // ── Favorite Toggle ──
        var favoriteBtn = document.getElementById('favoriteBtn');
        var favoriteIcon = document.getElementById('favoriteIcon');
        var favoriteBtnText = document.getElementById('favoriteBtnText');

        function setFavoriteUI(isFavorited) {
            if (isFavorited) {
                favoriteIcon?.setAttribute('fill', '#f87171');
                if (favoriteBtnText) favoriteBtnText.textContent = 'Saved';
            } else {
                favoriteIcon?.setAttribute('fill', 'none');
                if (favoriteBtnText) favoriteBtnText.textContent = 'Save';
            }
        }

        if (favoriteBtn) {
            var novelIdForFav = favoriteBtn.getAttribute('data-novel-id');
            // Load current status
            apiGet('/api/novels/' + novelIdForFav + '/favorite/status').then(data => setFavoriteUI(data.favorited));
            // Toggle on click
            favoriteBtn.addEventListener('click', () => {
                apiPost('/api/novels/' + novelIdForFav + '/favorite', {}).then(data => setFavoriteUI(data.favorited));
            });
        }

        document.addEventListener("DOMContentLoaded", () => {

            const COMMENTS_PER_PAGE = 5;

            // Lấy đúng selector - chỉ lấy comment con trực tiếp của container
            const container = document.getElementById("nd-comments-container");

            // Kiểm tra container tồn tại không
            if (!container) {
                console.warn("Không tìm thấy #nd-comments-container");
                return;
            }

            const allComments = Array.from(container.querySelectorAll(":scope > .nd-comment"));
            console.log("Tổng số comments:", allComments.length); // Debug

            let currentShown = COMMENTS_PER_PAGE;

            // ===== Init: ẩn comment từ index 5 trở đi =====
            function initComments() {
                allComments.forEach((comment, index) => {
                    if (index >= COMMENTS_PER_PAGE) {
                        comment.style.display = "none"; // dùng style thay vì class hidden
                    }
                });

                const btn = document.getElementById("load-more-btn");
                if (!btn) return;

                // Ẩn nút nếu tổng comment <= 5
                if (allComments.length <= COMMENTS_PER_PAGE) {
                    btn.closest(".nd-load-more-wrapper").style.display = "none";
                }
            }

            // ===== Load More =====
            async function loadMoreComments() {
                const btn = document.getElementById("load-more-btn");
                const spinner = document.getElementById("load-more-spinner");
                const btnText = document.getElementById("load-more-text");

                if (!btn || !spinner || !btnText) return;

                // Hiện loading
                btn.disabled = true;
                spinner.style.display = "inline-block";
                btnText.textContent = "Loading...";

                // Delay giả lập loading
                await new Promise(resolve => setTimeout(resolve, 800));

                // Show thêm comment với stagger
                const nextBatch = allComments.slice(currentShown, currentShown + COMMENTS_PER_PAGE);

                if (nextBatch.length === 0) {
                    btn.closest(".nd-load-more-wrapper").style.display = "none";
                    return;
                }

                nextBatch.forEach((comment, index) => {
                    setTimeout(() => {
                        comment.style.display = "flex"; // hoặc "block" tùy CSS của bạn
                        comment.style.animation = "commentFadeIn 0.3s ease forwards";
                    }, index * 120);
                });

                currentShown += COMMENTS_PER_PAGE;

                // Ẩn nút nếu đã hết
                if (currentShown >= allComments.length) {
                    setTimeout(() => {
                        btn.closest(".nd-load-more-wrapper").style.display = "none";
                    }, nextBatch.length * 120 + 100);
                } else {
                    btn.disabled = false;
                    spinner.style.display = "none";
                    btnText.textContent = "Load more comments";
                }
            }

            // ===== Reply buttons =====
            function initReplyButtons() {
                const replyBtns = document.querySelectorAll(".reply-comment-button");
                console.log("Tổng số reply buttons:", replyBtns.length); // Debug

                replyBtns.forEach((btn) => {
                    btn.addEventListener("click", () => {
                        const formId = btn.getAttribute("data-form-id");
                        const mention = btn.getAttribute("data-mention");
                        const targetForm = document.getElementById("reply-form-" + formId);

                        if (!targetForm) {
                            console.warn("Không tìm thấy form reply-form-" + formId);
                            return;
                        }

                        // Đóng tất cả form khác
                        document.querySelectorAll(".reply-comment-form").forEach((form) => {
                            if (form !== targetForm) {
                                form.style.display = "none";
                                const ta = form.querySelector("textarea");
                                if (ta) ta.value = "";
                            }
                        });

                        // Toggle form hiện tại
                        const isHidden = targetForm.style.display === "none" || targetForm.style.display === "";
                        targetForm.style.display = isHidden ? "flex" : "none";

                        if (isHidden) {
                            const textarea = targetForm.querySelector("textarea");
                            if (textarea) {
                                textarea.value = mention && mention.trim() !== "" ? "@" + mention + " " : "";
                                textarea.focus();
                                textarea.setSelectionRange(textarea.value.length, textarea.value.length);
                            }
                        }
                    });
                });
            }

            // ===== Gắn event Load More =====
            const loadMoreBtn = document.getElementById("load-more-btn");
            if (loadMoreBtn) {
                loadMoreBtn.addEventListener("click", loadMoreComments);
            }

            // ===== Chạy init =====
            initComments();
            initReplyButtons();

        });

