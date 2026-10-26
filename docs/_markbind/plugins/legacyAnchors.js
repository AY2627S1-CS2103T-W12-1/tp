/**
 * Keep published release-note bookmarks working when release headings are reorganised.
 * Editable pages use explicit heading IDs; release-note aliases are configured in site.json instead.
 */
module.exports.postRender = (_context, frontmatter, content) => {
    for (const [currentId, aliases] of Object.entries(frontmatter.legacyAnchors || {})) {
        const legacyIds = Array.isArray(aliases) ? aliases : [aliases];
        if (!/^[a-z0-9-]+$/.test(currentId) || legacyIds.some(id => !/^[a-z0-9-]+$/.test(id))) {
            throw new Error('Legacy heading IDs must contain only lowercase letters, digits, and hyphens.');
        }
        const heading = new RegExp(`(<h[1-6]\\b[^>]*\\bid="${currentId}"[^>]*>)`);
        if (!heading.test(content)) {
            throw new Error(`Cannot preserve a missing release-note heading: ${currentId}`);
        }
        const anchors = legacyIds.map(id => `<span id="${id}" class="legacy-anchor" aria-hidden="true"></span>`);
        content = content.replace(heading, `$1${anchors.join('')}`);
    }
    return content;
};
