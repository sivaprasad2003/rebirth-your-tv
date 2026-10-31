/**
 * RebirthYourTV — Interactive Web Application Engine
 * Author: Sivaprasad (sivaprasad2003)
 */

document.addEventListener("DOMContentLoaded", () => {
  initLiveClock();
  initPromptCopy();
  initTvModes();
  initGalleryFilter();
  initLightbox();
  initCompatibilitySearch();
  initFaqAccordion();
  initGitHubStats();
});

/* ==========================================================================
   1. Live Clock for Screensaver & TV Screen Mockup
   ========================================================================== */
function initLiveClock() {
  const clockElements = document.querySelectorAll(".live-clock-time");
  const dateElements = document.querySelectorAll(".live-clock-date");

  function update() {
    const now = new Date();
    const hours = String(now.getHours()).padStart(2, "0");
    const minutes = String(now.getMinutes()).padStart(2, "0");
    const timeString = `${hours}:${minutes}`;

    const days = ["SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"];
    const months = ["JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"];
    const dateString = `${days[now.getDay()]}, ${months[now.getMonth()]} ${now.getDate()}`;

    clockElements.forEach(el => el.textContent = timeString);
    dateElements.forEach(el => el.textContent = dateString);
  }

  update();
  setInterval(update, 1000);
}

/* ==========================================================================
   2. Prompt System & 1-Click Copy
   ========================================================================== */
const PROMPT_TEXTS = {
  universal: `# ==============================================================================
# RebirthYourTV — 1-Click Debloat & 60 FPS Launcher Prompt
# ==============================================================================
# Paste this into Claude Code, Gemini CLI, or AGY on your computer connected to
# the same Wi-Fi as your Android TV / Google TV.

I want to optimize and debloat my Android TV using RebirthYourTV, install the
60 FPS featherlight launcher, configure the ambient weather screensaver, and
remap my remote buttons.
My TV model is: <ENTER YOUR TV MODEL HERE>. Work on the real device, step by step,
and verify everything you do with screenshots pulled from the TV.

1. Connect:
   - Find the TV on my local network and connect over ADB on port 5555.
   - Tell me when "Allow USB debugging?" appears on my TV so I can accept it.

2. Back Up First:
   - Run tools/backup.ps1 (or tools/backup.sh) to snapshot all 170+ installed
     packages, system properties, and home launcher settings into backup/.
   - Verify that tools/restore.ps1 can restore everything if needed.

3. Debloat:
   - Disable vendor analytics, telemetry daemons, and ad recommendation feeds
     using "pm disable-user --user 0". Never uninstall system packages.
   - Core packages (systemui, packageinstaller, settings) are protected.

4. Install RebirthYourTV:
   - Sideload the 430 KB launcher APK (com.gathin.tvhome).
   - Set it as the default Leanback Home with "cmd package set-home-activity".
   - Configure the ambient clock & weather screensaver (10 min idle).
   - Verify 60 FPS locked fluid navigation with screenshots.`,

  powershell: `# RebirthYourTV — Windows PowerShell 1-Click Quick Run
# Make sure your TV has Developer Options & USB Debugging ON

# 1. Connect over Wi-Fi
adb connect <YOUR_TV_IP>:5555

# 2. Take complete system backup before any changes
powershell -ExecutionPolicy Bypass -File tools\\backup.ps1 -TvHost <YOUR_TV_IP>

# 3. Apply safe debloat list for your TV brand
powershell -ExecutionPolicy Bypass -File tools\\disable.ps1 -TvHost <YOUR_TV_IP> examples\\xiaomi-mi-tv-4a\\debloat.txt

# 4. Build and install 60 FPS Leanback Launcher
powershell -ExecutionPolicy Bypass -File launcher\\build-and-install.ps1 -TvHost <YOUR_TV_IP> -InputKeyService -HomeKeyService`,

  bash: `# RebirthYourTV — macOS / Linux 1-Click Terminal Quick Run
# Make sure your TV has Developer Options & USB Debugging ON

# 1. Connect over Wi-Fi
adb connect <YOUR_TV_IP>:5555

# 2. Take complete system backup
tools/backup.sh <YOUR_TV_IP>

# 3. Apply safe debloat list for your TV
TV_HOST=<YOUR_TV_IP> tools/disable.sh examples/xiaomi-mi-tv-4a/debloat.txt

# 4. Build and install 60 FPS Leanback Launcher
TV_HOST=<YOUR_TV_IP> launcher/build-and-install.sh`
};

function initPromptCopy() {
  const codeEl = document.getElementById("terminalCodeBlock");
  const copyBtns = document.querySelectorAll(".btn-copy-prompt");
  const tabs = document.querySelectorAll(".prompt-tab");
  const toast = document.getElementById("toastBanner");
  let activeTab = "universal";

  function renderPrompt(key) {
    if (!codeEl) return;
    activeTab = key;
    codeEl.textContent = PROMPT_TEXTS[key] || PROMPT_TEXTS.universal;

    tabs.forEach(tab => {
      tab.classList.toggle("active", tab.dataset.tab === key);
    });
  }

  tabs.forEach(tab => {
    tab.addEventListener("click", () => {
      renderPrompt(tab.dataset.tab);
    });
  });

  copyBtns.forEach(btn => {
    btn.addEventListener("click", () => {
      const textToCopy = PROMPT_TEXTS[activeTab] || PROMPT_TEXTS.universal;
      navigator.clipboard.writeText(textToCopy).then(() => {
        showToast("✓ Copied prompt to clipboard! Ready to paste.");
        
        // Button visual feedback
        const originalText = btn.innerHTML;
        btn.innerHTML = `<span>✓ Copied!</span>`;
        btn.classList.add("btn-copied");
        setTimeout(() => {
          btn.innerHTML = originalText;
          btn.classList.remove("btn-copied");
        }, 2200);
      }).catch(err => {
        showToast("Copied to clipboard!");
      });
    });
  });

  function showToast(msg) {
    if (!toast) return;
    toast.textContent = msg;
    toast.classList.add("show");
    setTimeout(() => {
      toast.classList.remove("show");
    }, 3000);
  }

  renderPrompt("universal");
}

/* ==========================================================================
   3. Interactive TV Mockup Screen Modes
   ========================================================================== */
function initTvModes() {
  const tabs = document.querySelectorAll(".mode-tab");
  const screens = {
    home: document.getElementById("screenHome"),
    screensaver: document.getElementById("screenScreensaver"),
    settings: document.getElementById("screenSettings"),
    grid: document.getElementById("screenGrid"),
    quick: document.getElementById("screenQuick")
  };

  tabs.forEach(tab => {
    tab.addEventListener("click", () => {
      const mode = tab.dataset.mode;
      tabs.forEach(t => t.classList.toggle("active", t === tab));

      Object.entries(screens).forEach(([key, el]) => {
        if (!el) return;
        if (key === mode) {
          el.style.display = "block";
          el.style.opacity = "0";
          setTimeout(() => el.style.opacity = "1", 20);
        } else {
          el.style.display = "none";
        }
      });
    });
  });
}

/* ==========================================================================
   Gallery Category Filter & Lightbox Zoom
   ========================================================================== */
function initGalleryFilter() {
  const filterChips = document.querySelectorAll(".filter-chip");
  const cards = document.querySelectorAll(".gallery-card");

  filterChips.forEach(chip => {
    chip.addEventListener("click", () => {
      filterChips.forEach(c => c.classList.remove("active"));
      chip.classList.add("active");
      const filter = chip.dataset.filter;

      cards.forEach(card => {
        if (filter === "all" || card.dataset.category === filter) {
          card.style.display = "block";
        } else {
          card.style.display = "none";
        }
      });
    });
  });
}

function initLightbox() {
  const modal = document.getElementById("lightboxModal");
  const modalImg = document.getElementById("lightboxImg");
  const modalCaption = document.getElementById("lightboxCaption");
  const closeBtn = document.getElementById("lightboxClose");

  if (!modal || !modalImg) return;

  const clickableImages = document.querySelectorAll(".gallery-card img, .tv-live-screenshot");
  clickableImages.forEach(img => {
    img.style.cursor = "zoom-in";
    img.addEventListener("click", () => {
      modalImg.src = img.src;
      modalCaption.textContent = img.alt || "Real TV Screenshot Preview";
      modal.classList.add("active");
    });
  });

  modal.addEventListener("click", () => {
    modal.classList.remove("active");
  });

  closeBtn?.addEventListener("click", (e) => {
    e.stopPropagation();
    modal.classList.remove("active");
  });
}

/* ==========================================================================
   4. TV Compatibility Search & Brand Filters
   ========================================================================== */
const TV_MODELS_DATA = [
  { brand: "Xiaomi", model: "Mi TV 4A / 4C / 4S / Horizon", os: "Android TV 9 / 11", notes: "PatchWall telemetry & recommendation rows removed. 60 FPS locked." },
  { brand: "Xiaomi", model: "Redmi Smart TV X Series (X50/X55/X65)", os: "Android TV 10 / 11", notes: "Full debloat, Mi Service ads disabled, ultra smooth." },
  { brand: "Xiaomi", model: "Xiaomi TV Box S (1st & 2nd Gen)", os: "Google TV 14 / Android 11", notes: "Casting & 4K HDR kept, stock launcher replaced." },
  { brand: "TCL", model: "TCL C728 / C635 / C745 / C845 (MediaTek)", os: "Google TV 14", notes: "TCL channel ads, telemetry, and background cleaners disabled." },
  { brand: "TCL", model: "TCL 55P71B / P725 / P635 (Android 11/12)", os: "Google TV 14", notes: "Full debloat, stock Home takeover, instant boot." },
  { brand: "TCL", model: "FFalcon 鹤6 PRO (MediaTek MT9653)", os: "Android TV 11", notes: "TCL boot ad, analytics, push feeds removed." },
  { brand: "Sony", model: "Sony BRAVIA XR A80L / A90K OLED", os: "Google TV 14", notes: "Google recommendations removed, BRAVIA picture engine kept intact." },
  { brand: "Sony", model: "Sony BRAVIA KD-55X9000H / X85J", os: "Android TV 10 / 11", notes: "Sony telemetry disabled, HDMI inputs & acoustic surface untouched." },
  { brand: "Sony", model: "Sony BRAVIA K-85XR70 (MediaTek)", os: "Google TV 14", notes: "ACR and Samba TV tracker safely disabled." },
  { brand: "OnePlus", model: "OnePlus TV Q1 / Q1 Pro / 55U1", os: "Android TV 9 / 10", notes: "OxygenPlay telemetry & ads removed, fast 60 FPS Home." },
  { brand: "OnePlus", model: "OnePlus TV Y1 / Y1S / Y1S Pro", os: "Android TV 11", notes: "1 GB RAM budget TV optimized, +320 MB free memory." },
  { brand: "Philips", model: "Philips 65PUG7406 / OLED 807 (TP Vision)", os: "Android TV 11", notes: "Ambilight preserved, Philips telemetry & bloat disabled." },
  { brand: "Hisense", model: "Hisense U7K / U8K / A6K Series", os: "Google TV 14", notes: "VIDAA/Google trackers removed, 100% remote key functionality." },
  { brand: "Hisense", model: "Hisense Fire TV (AFTCIBFBD1)", os: "Fire OS 8 / Android 11", notes: "Amazon telemetry & ACR debloated, Private DNS enabled." },
  { brand: "Amazon", model: "Fire TV Stick 4K / 4K Max (1st & 2nd Gen)", os: "Fire OS 7 / 8", notes: "Alexa hints, home promo feeders & telemetry safely disabled." },
  { brand: "Amazon", model: "Fire TV Omni / 4-Series Smart TV", os: "Fire OS 7.6+", notes: "Shopping, Luna, sponsored banner feeds disabled." },
  { brand: "Google", model: "Chromecast with Google TV (4K & HD)", os: "Google TV 14", notes: "Google ads removed, SetupWraith cast Wi-Fi safety handled." },
  { brand: "Google", model: "Google TV Streamer 4K (kirkwood)", os: "Google TV 14", notes: "Zero telemetry, lightweight launcher, instant response." },
  { brand: "Realme", model: "Realme Smart TV 32 / 43 / 4K", os: "Android TV 9 / 10", notes: "Budget ODM optimizations, 60 FPS Leanback UI." },
  { brand: "NVIDIA", model: "NVIDIA Shield TV / Shield Pro (2019)", os: "Android TV 11", notes: "AI Upscaling kept, Google banner ads removed." },
  { brand: "Anker", model: "Nebula Capsule / Cosmos Laser Projectors", os: "Android TV 9 / 14", notes: "Keystone & focus services kept, stock heavy launcher swapped." },
  { brand: "Samsung", model: "Samsung Tizen Smart TV", os: "Tizen OS (Non-Android)", notes: "Out of scope (No ADB shell on retail firmware). See SHOWCASE." }
];

function initCompatibilitySearch() {
  const searchInput = document.getElementById("tvSearchInput");
  const brandChips = document.querySelectorAll(".brand-chip");
  const grid = document.getElementById("modelsGrid");
  let activeBrand = "All";

  function render() {
    if (!grid) return;
    const query = (searchInput?.value || "").toLowerCase().trim();

    const filtered = TV_MODELS_DATA.filter(item => {
      const matchesBrand = activeBrand === "All" || 
        (activeBrand === "Xiaomi" && (item.brand === "Xiaomi" || item.brand === "Redmi")) ||
        (item.brand.toLowerCase().includes(activeBrand.toLowerCase()));

      const matchesSearch = query === "" || 
        item.brand.toLowerCase().includes(query) ||
        item.model.toLowerCase().includes(query) ||
        item.os.toLowerCase().includes(query) ||
        item.notes.toLowerCase().includes(query);

      return matchesBrand && matchesSearch;
    });

    if (filtered.length === 0) {
      grid.innerHTML = `
        <div style="grid-column: 1 / -1; text-align: center; padding: 40px; color: #94a3b8;">
          <p style="font-size: 1.1rem; margin-bottom: 8px;">No exact match found.</p>
          <p style="font-size: 0.88rem;">RebirthYourTV supports virtually all 716+ Android TV & Google TV devices (Android 8.0 through 14).</p>
        </div>
      `;
      return;
    }

    grid.innerHTML = filtered.map(item => `
      <div class="model-card">
        <div class="model-card-top">
          <span class="model-brand">${item.brand}</span>
          <span class="model-os-tag">${item.os}</span>
        </div>
        <div class="model-name">${item.model}</div>
        <div class="model-notes">${item.notes}</div>
      </div>
    `).join("");
  }

  searchInput?.addEventListener("input", render);

  brandChips.forEach(chip => {
    chip.addEventListener("click", () => {
      brandChips.forEach(c => c.classList.remove("active"));
      chip.classList.add("active");
      activeBrand = chip.dataset.brand;
      render();
    });
  });

  render();
}

/* ==========================================================================
   5. FAQ Accordion
   ========================================================================== */
function initFaqAccordion() {
  const items = document.querySelectorAll(".faq-item");
  items.forEach(item => {
    const question = item.querySelector(".faq-question");
    question?.addEventListener("click", () => {
      const isOpen = item.classList.contains("open");
      items.forEach(i => i.classList.remove("open"));
      if (!isOpen) {
        item.classList.add("open");
      }
    });
  });
}

/* ==========================================================================
   6. GitHub Stars Counter with Fallback
   ========================================================================== */
function initGitHubStats() {
  const starBadge = document.getElementById("githubStarCount");
  if (!starBadge) return;

  fetch("https://api.github.com/repos/sivaprasad2003/rebirth-your-tv")
    .then(res => res.json())
    .then(data => {
      if (data && typeof data.stargazers_count === "number") {
        starBadge.textContent = `★ ${data.stargazers_count}`;
      }
    })
    .catch(() => {
      // Fallback
      starBadge.textContent = "★ Star";
    });
}

