// Cross-platform Angular launcher; keeps worker usage bounded on small machines.
import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';
const child = spawn(process.execPath, [fileURLToPath(new URL('../node_modules/@angular/cli/bin/ng.js', import.meta.url)), ...process.argv.slice(2)], {
  stdio: 'inherit',
  env: { ...process.env, NG_BUILD_MAX_WORKERS: process.env.NG_BUILD_MAX_WORKERS || '2' },
});
child.on('error', error => { console.error(error.message); process.exitCode = 1; });
child.on('exit', (code, signal) => { process.exitCode = code ?? (signal ? 1 : 0); });
for (const signal of ['SIGINT', 'SIGTERM']) process.on(signal, () => child.kill(signal));
