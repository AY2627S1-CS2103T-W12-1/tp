/* Add keyboard and screen-reader support to MarkBind's generated navigation toggles. */
document.addEventListener('DOMContentLoaded', () => {
  const enhanced = new WeakSet();

  function updateToggle(control, label, expanded, controls) {
    control.setAttribute('role', 'button');
    control.setAttribute('tabindex', '0');
    control.setAttribute('aria-label', label);
    control.setAttribute('aria-expanded', String(expanded));
    if (controls) control.setAttribute('aria-controls', controls);
    if (enhanced.has(control)) return;
    enhanced.add(control);
    control.addEventListener('keydown', event => {
      if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        event.stopPropagation();
        control.click();
      }
    });
  }

  function updateNavigation() {
    for (const [selector, label, controls] of [
      ['.toggle-site-nav-button', 'Site map', 'site-nav'],
      ['.toggle-page-nav-button', 'On this page', 'page-nav'],
    ]) {
      for (const control of document.querySelectorAll(selector)) {
        updateToggle(control, label, control.parentElement.classList.contains('nav-menu-close-icon'), controls);
      }
    }
    for (const control of document.querySelectorAll('.site-nav-dropdown-btn-icon')) {
      const link = control.parentElement.parentElement.querySelector('a');
      updateToggle(control, `Expand or collapse ${link?.textContent.trim() || 'navigation section'}`,
        control.classList.contains('site-nav-rotate-icon'));
    }
  }

  // MarkBind moves navigation into mobile portals after hydration and at responsive breakpoints.
  // Observe those moves and its open/closed classes without depending on private Vue state.
  new MutationObserver(updateNavigation).observe(document.body, {
    subtree: true,
    childList: true,
    attributes: true,
    attributeFilter: ['class'],
  });
  updateNavigation();
});
