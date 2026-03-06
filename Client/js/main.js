/* ------------------HOMEPAGE------------------------- */
(function () {
  const userIcon = document.getElementById("user-icon");
  const userDropdown = document.getElementById("user-dropdown");
  const dropdownContainer = document.getElementById("user-dropdown-container");
  const notificationBell = document.getElementById("notification-bell");
  const notificationDropdown = document.getElementById("notification-dropdown");
  const notificationContainer = document.getElementById(
    "notification-dropdown-container",
  );

  if (userIcon && userDropdown) {
    userIcon.addEventListener("click", function (e) {
      e.stopPropagation();
      if (
        notificationDropdown &&
        !notificationDropdown.classList.contains("hidden")
      ) {
        notificationDropdown.classList.add("hidden");
      }
      userDropdown.classList.toggle("hidden");
    });
  }

  if (notificationBell && notificationDropdown) {
    notificationBell.addEventListener("click", function (e) {
      e.stopPropagation();
      if (userDropdown && !userDropdown.classList.contains("hidden")) {
        userDropdown.classList.add("hidden");
      }
      notificationDropdown.classList.toggle("hidden");
    });
  }

  document.addEventListener("click", function (e) {
    if (dropdownContainer && userDropdown) {
      if (!dropdownContainer.contains(e.target)) {
        userDropdown.classList.add("hidden");
      }
    }
    if (notificationContainer && notificationDropdown) {
      if (!notificationContainer.contains(e.target)) {
        notificationDropdown.classList.add("hidden");
      }
    }
  });

  if (userDropdown) {
    userDropdown.addEventListener("click", function (e) {
      e.stopPropagation();
    });
  }
  if (notificationDropdown) {
    notificationDropdown.addEventListener("click", function (e) {
      e.stopPropagation();
    });
  }

  const markAllReadBtn = document.querySelector(".mark-all-read");
  if (markAllReadBtn) {
    markAllReadBtn.addEventListener("click", function (e) {
      e.stopPropagation();
      document
        .querySelectorAll(".notification-item.unread")
        .forEach(function (item) {
          item.classList.remove("unread");
        });
      const notificationBadge = document.getElementById("notification-badge");
      if (notificationBadge) notificationBadge.style.display = "none";
    });
  }
})();

/* ------------------SEARCHPAGE------------------------- */
(function () {
  const sortBtn = document.getElementById("sort-btn");
  const sortDd = document.getElementById("sort-dropdown");

  if (!sortBtn || !sortDd) return;

  window.toggleSort = function () {
    sortBtn.classList.toggle("open");
    sortDd.classList.toggle("open");
  };

  window.selectSort = function (val) {
    sortBtn.childNodes[0].textContent = val + " ";
    sortBtn.classList.remove("open");
    sortDd.classList.remove("open");
  };

  document.addEventListener("click", (e) => {
    if (!e.target.closest(".sort-wrap")) {
      sortBtn.classList.remove("open");
      sortDd.classList.remove("open");
    }
  });

  window.setStatus = function (el) {
    document
      .querySelectorAll(".status-pill")
      .forEach((p) => p.classList.remove("active"));
    el.classList.add("active");
  };

  window.toggleSave = function (btn) {
    const isSaved = btn.classList.contains("saved");
    if (isSaved) {
      btn.classList.remove("saved");
      btn.querySelector(".save-icon").setAttribute("fill", "none");
      btn.childNodes[btn.childNodes.length - 1].textContent = " Save";
    } else {
      btn.classList.add("saved");
      btn.querySelector(".save-icon").setAttribute("fill", "#f87171");
      btn.childNodes[btn.childNodes.length - 1].textContent = " Saved";
    }
  };

  window.setPage = function (el) {
    document
      .querySelectorAll(".page-btn:not(.nav-arrow)")
      .forEach((b) => b.classList.remove("active"));
    el.classList.add("active");
  };

  window.resetFilters = function () {
    document
      .querySelectorAll('.filter-item input[type="checkbox"]')
      .forEach((c) => (c.checked = false));
    document
      .querySelectorAll(".status-pill")
      .forEach((p) => p.classList.remove("active"));
    const first = document.querySelector(".status-pill");
    if (first) first.classList.add("active");
  };
})();

/* ------------------CONTRIBUTE TRANSLATION (simple tab bar, no panels)------------------------- */
(function () {
  // Only run on pages that have tab-buttons but NO data-tab panels
  // (i.e. the contribute translation page, not the user profile page)
  if (document.querySelector("[data-tab]")) return; // hand off to USERPROFILE block

  const menuBtn = document.getElementById("menu-btn");
  const mobileMenu = document.getElementById("mobile-menu");
  if (menuBtn && mobileMenu) {
    menuBtn.addEventListener("click", () =>
      mobileMenu.classList.toggle("hidden"),
    );
  }

  document.querySelectorAll('[class*="tab-"]').forEach((tab) => {
    tab.addEventListener("click", () => {
      document.querySelectorAll('[class*="tab-"]').forEach((t) => {
        t.className = t.className.replace("tab-active", "tab-inactive");
      });
      tab.className = tab.className.replace("tab-inactive", "tab-active");
    });
  });
})();

/* ------------------USERPROFILE------------------------- */
(function () {
  // Only run if this page has data-tab elements
  const tabs = document.querySelectorAll("[data-tab]");
  if (!tabs.length) return;

  // Mobile menu
  const menuBtn = document.getElementById("menu-btn");
  const mobileMenu = document.getElementById("mobile-menu");
  if (menuBtn && mobileMenu) {
    menuBtn.addEventListener("click", () =>
      mobileMenu.classList.toggle("hidden"),
    );
  }

  // Tab switching
  const panelIds = [
    "user-info",
    "favourited-novels",
    "translation-contributions",
  ];

  tabs.forEach((tab) => {
    tab.addEventListener("click", () => {
      const target = tab.dataset.tab;

      // Update tab button styles
      tabs.forEach((t) => {
        t.classList.remove("tab-active");
        t.classList.add("tab-inactive");
      });
      tab.classList.remove("tab-inactive");
      tab.classList.add("tab-active");

      // Show/hide panels
      panelIds.forEach((id) => {
        const el = document.getElementById("tab-" + id);
        if (!el) return;
        if (id === target) {
          el.classList.remove("hidden");
          // Re-trigger fade-in animation
          el.classList.remove("animate-fade-in");
          void el.offsetWidth;
          el.classList.add("animate-fade-in");
        } else {
          el.classList.add("hidden");
        }
      });
    });
  });
})();
