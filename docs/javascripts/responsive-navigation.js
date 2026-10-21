/* Keep one page outline: a desktop sidebar or a collapsible menu on narrower screens. */
document.addEventListener('DOMContentLoaded', () => {
  const outline = document.getElementById('page-nav');
  if (!outline) return;
  const desktop = window.matchMedia('(min-width: 1300px)');
  const updateOutline = () => { outline.open = desktop.matches; };
  updateOutline();
  desktop.addEventListener('change', updateOutline);
});
