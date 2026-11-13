// Generates one evidence definition package per row of the "Evidence Request List <version>"
// sheet of an SCF workbook, under package/scf/<version>/<code>/.
//
//   node generate.mjs <path/to/scf-workbook.xlsx>
//
// The SCF version comes from the sheet name. Ids are uuidv5 of `scf:<version>:<CODE>`:
// stable across reruns, never shared between versions (SCF reuses codes for different evidence).
// Re-running rewrites index.yml but never an existing package.json, whose version publish owns.
import { createHash } from 'node:crypto';
import { copyFileSync, existsSync, mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import ExcelJS from 'exceljs';

const NIL_NAMESPACE = '00000000-0000-0000-0000-000000000000';
const DNS_NAMESPACE = '6ba7b810-9dad-11d1-80b4-00c04fd430c8';

function uuidv5(name, namespace) {
  const hash = createHash('sha1')
    .update(Buffer.from(namespace.replace(/-/g, ''), 'hex'))
    .update(name, 'utf8')
    .digest();
  hash[6] = (hash[6] & 0x0f) | 0x50;
  hash[8] = (hash[8] & 0x3f) | 0x80;
  const h = hash.subarray(0, 16).toString('hex');
  return `${h.slice(0, 8)}-${h.slice(8, 12)}-${h.slice(12, 16)}-${h.slice(16, 20)}-${h.slice(20)}`;
}
// RFC 4122 test vector, so a broken uuidv5 can never mint ids
if (uuidv5('www.example.com', DNS_NAMESPACE) !== '2ed6657d-e927-568b-95e1-2665a8aea6a2') {
  throw new Error('uuidv5 self-check failed');
}

const clean = (s) => String(s ?? '').replace(/\s+/g, ' ').trim();
// SCF content is CC BY-ND 4.0: reproduce it verbatim (line breaks, bullets, spacing), only trim the ends
const verbatim = (s) => String(s ?? '').trim();
// JSON strings are valid YAML double-quoted scalars, so no YAML library is needed
const q = (s) => JSON.stringify(s);

const workbookPath = process.argv[2];
if (!workbookPath) {
  console.error('usage: node generate.mjs <scf-workbook.xlsx>');
  process.exit(1);
}

const workbook = new ExcelJS.Workbook();
await workbook.xlsx.readFile(workbookPath);
const sheet = workbook.worksheets.find((ws) => /^Evidence Request List\s+\d/i.test(ws.name));
if (!sheet) throw new Error(`no "Evidence Request List <version>" sheet in ${workbookPath}`);
const version = sheet.name.match(/(\d+(?:\.\d+)+)\s*$/)?.[1];
if (!version) throw new Error(`cannot read the SCF version from sheet name "${sheet.name}"`);

// Header names drift between SCF releases ("Documentation Artifact" → "ERL Artifact"), so match loosely
const headers = sheet.getRow(1).values.map(clean);
const col = (re, label) => {
  const i = headers.findIndex((h) => re.test(h));
  if (i < 1) throw new Error(`no ${label} column in "${sheet.name}" (headers: ${headers.filter(Boolean).join(' | ')})`);
  return i;
};
const codeCol = col(/^ERL #$/i, 'ERL #');
const nameCol = col(/artifact$/i, 'artifact name');
const descCol = col(/artifact description$/i, 'artifact description');

const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..', '..');
const versionDir = join(root, 'package', 'scf', version);
const seen = new Map();
let created = 0;

sheet.eachRow((row, n) => {
  if (n === 1) return;
  const code = clean(row.getCell(codeCol).text).toUpperCase();
  if (!code) return;
  if (!/^E-[A-Z]{3}-\d+(\.\d+)?$/.test(code)) throw new Error(`row ${n}: unexpected ERL code "${code}"`);
  const name = verbatim(row.getCell(nameCol).text);
  const description = verbatim(row.getCell(descCol).text);
  if (!name) throw new Error(`row ${n} (${code}): empty artifact name`);

  // Package code segments can't hold dots: E-GOV-01.1 → e-gov-01-1
  const dir = code.toLowerCase().replace(/\./g, '-');
  if (seen.has(dir)) throw new Error(`row ${n}: ${code} and ${seen.get(dir)} both map to ${dir}`);
  seen.set(dir, code);

  const pkgDir = join(versionDir, dir);
  mkdirSync(pkgDir, { recursive: true });

  writeFileSync(join(pkgDir, 'index.yml'), [
    `id: ${uuidv5(`scf:${version}:${code}`, NIL_NAMESPACE)}`,
    `code: ${q(code)}`,
    `name: ${q(name)}`,
    ...(description ? [`description: ${q(description)}`] : []),
    'category: confirmation',
    // ERL rows are documents to upload; the dataloader would otherwise default confirmation to data
    'format: file',
    `domain: ${q(code.split('-')[1].toLowerCase())}`,
    'status: published',
    'tags: []',
    '',
  ].join('\n'));

  const pkgJson = join(pkgDir, 'package.json');
  if (!existsSync(pkgJson)) {
    created++;
    writeFileSync(pkgJson, JSON.stringify({
      name: `@zerobias-org/evidencedefinition-scf-${version}-${dir}`,
      version: '1.0.0',
      description: `SCF ${version} ${code}: ${name}`,
      author: 'team@zerobias.com',
      license: 'CC-BY-ND-4.0', // SCF content license; see NOTICE.md
      type: 'module',
      repository: {
        type: 'git',
        url: 'git@github.com:zerobias-org/evidencedefinition.git',
        directory: `package/scf/${version}/${dir}/`,
      },
      publishConfig: { registry: 'https://pkg.zerobias.org' },
      files: ['index.yml', 'npm-shrinkwrap.json'],
      dependencies: {},
      zerobias: {
        'dataloader-version': '1.0.0',
        'import-artifact': 'evidencedefinition',
        package: `scf.${version.replace(/\./g, '_')}.${dir}.evidencedefinition`,
      },
    }, null, 2) + '\n');
    copyFileSync(join(root, '.npmrc'), join(pkgDir, '.npmrc'));
    writeFileSync(join(pkgDir, 'build.gradle.kts'), 'plugins { id("zb.content") }\n');
  }
});

console.log(`SCF ${version}: ${seen.size} evidence definitions (${created} new packages) in ${versionDir}`);
