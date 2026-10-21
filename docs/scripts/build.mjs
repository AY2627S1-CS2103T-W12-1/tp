import { execFileSync } from 'node:child_process';
import { rmSync } from 'node:fs';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';

const require = createRequire(import.meta.url);
const docsDirectory = fileURLToPath(new URL('../', import.meta.url));

// A clean output prevents a failed page render from being masked by an older HTML file.
rmSync(new URL('../_site/', import.meta.url), { recursive: true, force: true });
execFileSync(process.execPath, [require.resolve('markbind-cli'), 'build', '--baseUrl', '/tp'], {
    cwd: docsDirectory,
    stdio: 'inherit',
});
await import('./verify-site.mjs');
