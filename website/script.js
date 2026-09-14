// Mobile nav toggle
const navToggle = document.querySelector(".nav-toggle");
const mobileNav = document.querySelector(".mobile-nav");

if (navToggle && mobileNav) {
  navToggle.addEventListener("click", () => {
    mobileNav.classList.toggle("open");
  });

  mobileNav.querySelectorAll("a").forEach((link) => {
    link.addEventListener("click", () => mobileNav.classList.remove("open"));
  });
}

// Platform showcase tabs
const tabButtons = document.querySelectorAll(".tab-btn");
const tabPanels = document.querySelectorAll(".tab-panel");

tabButtons.forEach((btn) => {
  btn.addEventListener("click", () => {
    const target = btn.getAttribute("data-tab");

    tabButtons.forEach((b) => b.classList.remove("active"));
    tabPanels.forEach((p) => p.classList.remove("active"));

    btn.classList.add("active");
    document.getElementById(target).classList.add("active");
  });
});

// Simulated live BTC price ticker (cosmetic only, not a real feed)
(function priceTicker() {
  const valueEl = document.querySelector(".ticker .tk-value");
  const subEl = document.querySelector(".ticker .tk-sub");
  if (!valueEl) return;

  let price = 77300;

  function format(n) {
    return n.toLocaleString("en-US", { maximumFractionDigits: 0 });
  }

  function tick() {
    price += (Math.random() - 0.5) * 60;
    valueEl.textContent = "$" + format(price);
    const sats = Math.round(100000000 / price);
    subEl.textContent = "1 USD ≈ " + sats.toLocaleString("en-US") + " sats";
  }

  tick();
  setInterval(tick, 2200);
})();

// Footer year
const yearEl = document.getElementById("year");
if (yearEl) yearEl.textContent = new Date().getFullYear();
