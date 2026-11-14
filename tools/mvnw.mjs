// Runs the Maven wrapper of the project in the current directory, on any OS.
import { spawnSync } from 'node:child_process';
import { join } from 'node:path';

const isWindows = process.platform === 'win32';
const wrapper = join(process.cwd(), isWindows ? 'mvnw.cmd' : 'mvnw');
const { status } = spawnSync(isWindows ? `"${wrapper}"` : wrapper, process.argv.slice(2), {
  stdio: 'inherit',
  shell: isWindows,
});
process.exit(status ?? 1);
