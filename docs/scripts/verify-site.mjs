import assert from 'node:assert/strict';
import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const docsDirectory = fileURLToPath(new URL('../', import.meta.url));
const outputDirectory = path.join(docsDirectory, '_site');
const excludedDirectories = new Set(['node_modules', '_site', '_markbind', '_templates', 'scripts']);

function filesIn(directory, exclusions = new Set()) {
    return readdirSync(directory, { withFileTypes: true }).flatMap(entry => {
        const target = path.join(directory, entry.name);
        return entry.isDirectory()
            ? (exclusions.has(entry.name) ? [] : filesIn(target, exclusions))
            : [target];
    });
}

const sourcePages = filesIn(docsDirectory, excludedDirectories).filter(file => file.endsWith('.md'));
for (const source of sourcePages) {
    const relative = path.relative(docsDirectory, source).replace(/\.md$/, '.html');
    const output = path.join(outputDirectory, relative);
    assert.ok(existsSync(output), `Documentation page was not generated: ${relative}`);
    const html = readFileSync(output, 'utf8');
    assert.match(html, /<main\b[^>]*id="content-wrapper"/, `Missing shared layout: ${relative}`);
    assert.match(html, /<h1\b/, `Missing page content: ${relative}`);
}

const htmlPages = filesIn(outputDirectory).filter(file => file.endsWith('.html'));
const idsByPage = new Map();
for (const page of htmlPages) {
    const ids = [...readFileSync(page, 'utf8').matchAll(/\sid="([^"]+)"/g)].map(match => match[1]);
    assert.equal(new Set(ids).size, ids.length, `Duplicate heading or element IDs in ${page}`);
    idsByPage.set(page, new Set(ids));
}

let referencesChecked = 0;
for (const page of htmlPages) {
    const html = readFileSync(page, 'utf8');
    for (const match of html.matchAll(/\s(?:href|src)="([^"]+)"/g)) {
        const reference = match[1].replaceAll('&amp;', '&');
        if (/^(?:[a-z][a-z\d+.-]*:|\/\/)/i.test(reference)) {
            continue;
        }
        const url = new URL(reference, `https://preview.invalid/tp/${path.relative(outputDirectory, page)}`);
        assert.ok(url.pathname.startsWith('/tp/'), `Link escapes the website base path: ${reference}`);
        let target = path.join(outputDirectory, decodeURIComponent(url.pathname.slice(4)));
        assert.ok(existsSync(target), `Missing target ${reference} in ${path.relative(outputDirectory, page)}`);
        if (statSync(target).isDirectory()) {
            target = path.join(target, 'index.html');
            assert.ok(existsSync(target), `Missing directory index for ${reference}`);
        }
        if (url.hash && idsByPage.has(target)) {
            assert.ok(idsByPage.get(target).has(decodeURIComponent(url.hash.slice(1))),
                `Missing section ${reference} in ${path.relative(outputDirectory, page)}`);
        }
        referencesChecked += 1;
    }
}
console.log(`Verified ${sourcePages.length} documentation pages and ${referencesChecked} local references.`);
