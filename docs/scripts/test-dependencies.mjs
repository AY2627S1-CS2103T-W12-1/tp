import assert from 'node:assert/strict';
import { mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import { createRequire } from 'node:module';
import { tmpdir } from 'node:os';
import path from 'node:path';
import test from 'node:test';
import { pathToFileURL } from 'node:url';

import cheerio from 'cheerio';
import httpAuth from 'http-auth';
import nunjucks from 'nunjucks';

const require = createRequire(import.meta.url);
const coreDirectory = path.dirname(require.resolve('@markbind/core'));
const coreModule = relative => import(pathToFileURL(path.join(coreDirectory, relative)).href);

test('legacy Cheerio selectors and style picking work with patched helpers', () => {
    const $ = cheerio.load('<box><p style="color: red; display: block">First</p><p>Second</p></box>');
    assert.equal($('box p:nth-child(2)').text(), 'Second');
    assert.deepEqual($('p').first().css(['color']), { color: 'red' });
    // The alias provides the maintained implementation rather than vulnerable lodash.pick.
    const { pick } = createRequire(require.resolve('cheerio'))('lodash.pick');
    const picked = pick({ value: 1 }, ['__proto__.polluted', 'value']);
    assert.deepEqual(picked, { value: 1 });
    assert.equal(Object.prototype.polluted, undefined);
});

test('MarkBind external CSV variables preserve headers and quoted fields', async () => {
    const { SetExternalExtension } = await coreModule('src/lib/nunjucks-extensions/set-external.js');
    const directory = mkdtempSync(path.join(tmpdir(), 'trackcall-csv-'));
    try {
        writeFileSync(path.join(directory, 'members.csv'), '\uFEFFname,role\n"Tan, Jane",secretary\n');
        const environment = new nunjucks.Environment(null, { autoescape: false });
        environment.addExtension('SetExternalExtension', new SetExternalExtension(directory, environment));
        const rendered = environment.renderString(
            '{% ext members="members.csv" %}{{ members[0].name }}:{{ members[0].role }}');
        assert.equal(rendered, 'Tan, Jane:secretary');
    } finally {
        rmSync(directory, { recursive: true, force: true });
    }
});

test('MarkBind markdown extensions retain custom tags, radio lists and math', async () => {
    const { markdownIt } = await coreModule('src/lib/markdown-it/index.js');
    const rendered = markdownIt.render('<box>**Notice**</box>\n\n- ( ) Choice\n\n$x^2$\n');
    assert.match(rendered, /<box>/);
    assert.match(rendered, /<strong>Notice<\/strong>/);
    assert.match(rendered, /type="radio"/);
    assert.match(rendered, /class="katex"/);
});

test('preview authentication generates fresh digest nonces with the new UUID API', () => {
    const auth = httpAuth.digest({ realm: 'preview' }, () => {});
    const first = auth.askNonce();
    const second = auth.askNonce();
    assert.match(first, /^[a-f\d]{32}$/);
    assert.notEqual(first, second);
});
