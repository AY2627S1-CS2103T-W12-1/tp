/**
 * Keep published release-note bookmarks working without changing their checksummed Markdown files.
 * Editable pages use explicit heading IDs; release-note aliases are configured in site.json instead.
 */
module.exports.postRender = (_context, frontmatter, content) => {
    for (const [currentId, legacyId] of Object.entries(frontmatter.legacyAnchors || {})) {
        if (!/^[a-z0-9-]+$/.test(currentId) || !/^[a-z0-9-]+$/.test(legacyId)) {
            throw new Error('Legacy heading IDs must contain only lowercase letters, digits, and hyphens.');
        }
        const heading = new RegExp(`(<h[1-6]\\b[^>]*\\bid="${currentId}"[^>]*>)`);
        if (!heading.test(content)) {
            throw new Error(`Cannot preserve a missing release-note heading: ${currentId}`);
        }
        content = content.replace(heading, `$1<span id="${legacyId}" class="legacy-anchor" aria-hidden="true"></span>`);
    }
    return content;
};
