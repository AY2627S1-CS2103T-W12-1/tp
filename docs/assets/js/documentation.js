/* Progressive enhancements: the guides and their original contents work without JavaScript. */
(() => {
  'use strict';

  const content = document.querySelector('.post-content');
  const headings = content ? Array.from(content.querySelectorAll('h2[id], h3[id], h4[id], h5[id], h6[id]')) : [];
  const overview = document.getElementById('overview');
  const sectionTabs = document.getElementById('section-tabs');
  const outline = document.getElementById('page-outline');
  const outlineLinks = document.getElementById('page-outline-links');
  const sidebarLinks = Array.from(document.querySelectorAll('.sidebar-subnav a[href]'));

  function decodeHash(hash) {
    try {
      return decodeURIComponent(hash.replace(/^#/, ''));
    } catch (_) {
      return '';
    }
  }

  function pagePath(path) {
    return path.replace(/\/index\.html$/, '/').replace(/\/$/, '');
  }

  function localAnchor(link) {
    const url = new URL(link.href, window.location.href);
    return url.origin === window.location.origin
      && pagePath(url.pathname) === pagePath(window.location.pathname)
      ? decodeHash(url.hash) : '';
  }

  function headingLink(heading, className, label = heading.textContent.trim()) {
    const link = document.createElement('a');
    link.href = '#' + encodeURIComponent(heading.id);
    link.textContent = label;
    link.className = className;
    return link;
  }

  // Generate navigation from the actual headings, so guide edits cannot leave a stale outline.
  if (overview && headings.length && sectionTabs && outline && outlineLinks) {
    // Overview has a real anchor and participates in the same navigation as every section.
    outlineLinks.append(headingLink(overview, 'outline-link', 'Overview'));
    sectionTabs.append(headingLink(overview, 'section-tab', 'Overview'));
    headings.forEach((heading) => {
      let outlineClass = 'outline-link';
      if (heading.tagName !== 'H2') outlineClass += ' outline-sub';
      if (Number(heading.tagName.slice(1)) >= 4) outlineClass += ' outline-detail';
      outlineLinks.append(headingLink(heading, outlineClass));
      if (heading.tagName === 'H2') {
        const tab = headingLink(heading, 'section-tab');
        const shortLabel = sidebarLinks.find((link) => localAnchor(link) === heading.id);
        if (shortLabel) tab.textContent = shortLabel.textContent.trim();
        sectionTabs.append(tab);
      }
    });
    sectionTabs.hidden = !sectionTabs.children.length;
    outline.hidden = false;
    document.body.classList.add('enhanced-toc');

    const tabs = Array.from(sectionTabs.querySelectorAll('a'));
    const links = Array.from(outlineLinks.querySelectorAll('a'));
    let previousHeading = null;
    let scrollPending = false;

    function setCurrent(link, active) {
      link.classList.toggle('is-active', active);
      if (active) link.setAttribute('aria-current', 'location');
      else link.removeAttribute('aria-current');
    }

    function highlight(current) {
      if (!current || current === previousHeading) return;
      previousHeading = current;
      let section = current;
      for (let index = headings.indexOf(current); index >= 0; index -= 1) {
        if (headings[index].tagName === 'H2') {
          section = headings[index];
          break;
        }
      }
      links.forEach((link) => setCurrent(link, localAnchor(link) === current.id));
      tabs.forEach((link) => setCurrent(link, localAnchor(link) === section.id));
      sidebarLinks.forEach((link) => setCurrent(link, localAnchor(link) === section.id));
    }

    function updateFromScroll() {
      scrollPending = false;
      const header = document.querySelector('.docs-header');
      const threshold = (header ? header.getBoundingClientRect().height : 0)
        + 40;
      let current = overview;
      for (const heading of headings) {
        if (heading.getBoundingClientRect().top > threshold) break;
        current = heading;
      }
      // Short final sections cannot always reach the sticky navigation at the top.
      if (window.scrollY > 0
        && window.innerHeight + window.scrollY >= document.documentElement.scrollHeight - 2) {
        current = headings[headings.length - 1];
      }
      highlight(current);
    }

    function updateFromHash() {
      const target = document.getElementById(decodeHash(window.location.hash));
      let current = target === overview ? overview : null;
      if (target && content.contains(target)) {
        for (const heading of headings) {
          if (heading === target || (heading.compareDocumentPosition(target)
            & Node.DOCUMENT_POSITION_FOLLOWING)) current = heading;
          else break;
        }
      }
      if (current) highlight(current);
      else updateFromScroll();
    }

    window.addEventListener('scroll', () => {
      if (!scrollPending) {
        scrollPending = true;
        window.requestAnimationFrame(updateFromScroll);
      }
    }, { passive: true });
    window.addEventListener('resize', updateFromScroll);
    window.addEventListener('hashchange', updateFromHash);
    window.addEventListener('load', updateFromHash, { once: true });
    updateFromHash();
  }

  if (outline) {
    const compactOutline = window.matchMedia('(max-width: 1320px)');
    const setOutlineDefault = () => { outline.open = !compactOutline.matches; };
    setOutlineDefault();
    compactOutline.addEventListener('change', setOutlineDefault);
  }

  const menu = document.getElementById('site-menu');
  if (menu) {
    const mobile = window.matchMedia('(max-width: 860px)');
    // Apply a suitable default when crossing the breakpoint, leaving the summary user-controlled.
    const setMenuDefault = () => { menu.open = !mobile.matches; };
    setMenuDefault();
    mobile.addEventListener('change', setMenuDefault);
    menu.addEventListener('click', (event) => {
      if (mobile.matches && event.target.closest('a[href]')) menu.open = false;
    });
  }

  if (content) {
    content.querySelectorAll('table').forEach((table) => {
      if (table.parentElement.classList.contains('table-scroll')) return;
      const wrapper = document.createElement('div');
      wrapper.className = 'table-scroll';
      wrapper.tabIndex = 0;
      wrapper.setAttribute('role', 'region');
      let label = table.caption ? table.caption.textContent.trim() : '';
      if (!label) {
        for (const heading of headings) {
          if (heading.compareDocumentPosition(table) & Node.DOCUMENT_POSITION_FOLLOWING) {
            label = heading.textContent.trim();
          }
        }
      }
      wrapper.setAttribute('aria-label', label ? label + ' table' : 'Scrollable table');
      table.before(wrapper);
      wrapper.append(table);
    });
  }

  const form = document.getElementById('documentation-search');
  const input = document.getElementById('search-input');
  const panel = document.getElementById('search-results');
  const status = document.getElementById('search-status');
  const resultsList = document.getElementById('search-results-list');
  if (!form || !input || !panel || !status || !resultsList) return;

  let indexPromise = null;
  let searchVersion = 0;
  let searchTimer = null;
  form.hidden = false;
  input.setAttribute('aria-controls', panel.id);

  const normalise = (text) => text.replace(/\s+/g, ' ').trim();

  // The index contains rendered Markdown. Parse it as inert HTML and index each section separately.
  function buildIndex(pages) {
    const sections = [];
    for (const page of pages) {
      if (!page || typeof page.html !== 'string' || typeof page.url !== 'string') continue;
      const pageUrl = new URL(page.url, window.location.href);
      if (pageUrl.origin !== window.location.origin) continue;
      const parsed = new DOMParser().parseFromString(page.html, 'text/html');
      parsed.querySelectorAll('#markdown-toc, script, style, noscript').forEach((node) => node.remove());
      let section = { title: String(page.title || 'Documentation'), heading: '', url: pageUrl.href, text: '' };
      const saveSection = () => {
        section.text = normalise(section.text);
        if (section.text || section.heading) sections.push(section);
      };
      const walker = parsed.createTreeWalker(parsed.body, NodeFilter.SHOW_ELEMENT | NodeFilter.SHOW_TEXT);
      let node;
      while ((node = walker.nextNode())) {
        if (node.nodeType === Node.ELEMENT_NODE && /^H[2-6]$/.test(node.tagName) && node.id) {
          saveSection();
          const sectionUrl = new URL(pageUrl.href);
          sectionUrl.hash = node.id;
          section = {
            title: String(page.title || 'Documentation'),
            heading: normalise(node.textContent),
            url: sectionUrl.href,
            text: ''
          };
        } else if (node.nodeType === Node.TEXT_NODE) {
          section.text += ' ' + node.textContent;
        }
      }
      saveSection();
    }
    return sections;
  }

  function loadIndex() {
    if (!indexPromise) {
      indexPromise = fetch(form.dataset.indexUrl, { credentials: 'same-origin' })
        .then((response) => {
          if (!response.ok) throw new Error('Search index unavailable');
          return response.json();
        })
        .then((pages) => {
          if (!Array.isArray(pages)) throw new Error('Invalid search index');
          return buildIndex(pages);
        })
        .catch((error) => {
          indexPromise = null; // The next input or retry can recover from a temporary network failure.
          throw error;
        });
    }
    return indexPromise;
  }

  function hideResults() {
    window.clearTimeout(searchTimer);
    panel.hidden = true;
    searchVersion += 1;
  }

  function excerpt(text, terms) {
    const lower = text.toLocaleLowerCase();
    const positions = terms.map((term) => lower.indexOf(term)).filter((position) => position >= 0);
    const start = Math.max(0, (positions.length ? Math.min(...positions) : 0) - 45);
    const end = Math.min(text.length, start + 165);
    return (start ? '…' : '') + text.slice(start, end) + (end < text.length ? '…' : '');
  }

  async function search() {
    window.clearTimeout(searchTimer);
    const query = normalise(input.value);
    const version = ++searchVersion;
    resultsList.replaceChildren();
    if (!query) {
      panel.hidden = true;
      return;
    }
    panel.hidden = false;
    status.textContent = 'Searching the documentation…';
    try {
      const entries = await loadIndex();
      if (version !== searchVersion) return;
      const terms = query.toLocaleLowerCase().split(/\s+/);
      const matches = entries.map((entry) => {
        const title = entry.title.toLocaleLowerCase();
        const heading = entry.heading.toLocaleLowerCase();
        const body = entry.text.toLocaleLowerCase();
        let score = 0;
        for (const term of terms) {
          if (!title.includes(term) && !heading.includes(term) && !body.includes(term)) return null;
          score += (title.includes(term) ? 8 : 0) + (heading.includes(term) ? 12 : 0)
            + (body.includes(term) ? 1 : 0);
        }
        if (heading === query.toLocaleLowerCase()) score += 20;
        return { entry, score };
      }).filter(Boolean).sort((left, right) => right.score - left.score).slice(0, 8);
      status.textContent = matches.length
        ? 'Showing ' + matches.length + (matches.length === 1 ? ' result' : ' results') + ' for “' + query + '”.'
        : 'No results for “' + query + '”. Try a command or section name.';
      matches.forEach(({ entry }) => {
        const item = document.createElement('li');
        const link = document.createElement('a');
        link.href = entry.url;
        const label = document.createElement('span');
        label.className = 'search-result-heading';
        label.textContent = entry.title + (entry.heading ? ' · ' + entry.heading : '');
        const summary = document.createElement('span');
        summary.className = 'search-result-excerpt';
        summary.textContent = excerpt(entry.text, terms);
        link.append(label, summary);
        item.append(link);
        resultsList.append(item);
      });
      return resultsList.querySelector('a');
    } catch (_) {
      if (version !== searchVersion) return;
      status.textContent = 'Search could not load. Check your connection and try again.';
      const retryItem = document.createElement('li');
      const retry = document.createElement('button');
      retry.type = 'button';
      retry.textContent = 'Retry search';
      retry.addEventListener('click', search);
      retryItem.append(retry);
      resultsList.append(retryItem);
    }
  }

  input.addEventListener('focus', () => {
    if (input.value.trim()) search();
    else loadIndex().catch(() => {});
  });
  input.addEventListener('input', () => {
    searchVersion += 1;
    window.clearTimeout(searchTimer);
    // Never leave old clickable results beneath a new query while it is being debounced.
    resultsList.replaceChildren();
    if (!input.value.trim()) hideResults();
    else {
      status.textContent = 'Searching the documentation…';
      panel.hidden = false;
      searchTimer = window.setTimeout(search, 150);
    }
  });
  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    // A superseded query returns nothing; never navigate to a later query's results.
    const firstResult = await search();
    if (firstResult && firstResult.isConnected && !panel.hidden) firstResult.click();
  });
  resultsList.addEventListener('click', (event) => {
    if (event.target.closest('a')) hideResults();
  });
  document.addEventListener('click', (event) => {
    if (!form.contains(event.target)) hideResults();
  });
  document.addEventListener('keydown', (event) => {
    const target = event.target;
    const editing = target instanceof HTMLElement
      && (target.isContentEditable || /^(INPUT|TEXTAREA|SELECT)$/.test(target.tagName));
    if ((event.key === '/' && !editing && !event.ctrlKey && !event.metaKey && !event.altKey)
      || (event.key.toLocaleLowerCase() === 'k' && (event.ctrlKey || event.metaKey) && !event.altKey)) {
      event.preventDefault();
      input.focus();
    } else if (event.key === 'Escape' && !panel.hidden) {
      if (panel.contains(document.activeElement)) input.focus();
      hideResults();
    }
  });
})();
