#!/usr/bin/env node
/**
 * audit-copy.js — auditoría mecánica de copy para HTML y README.
 *
 * Uso:
 *   node audit-copy.js [ruta|directorio] [--all] [--links] [--strict] [--json]
 *
 * Sin ruta, audita el directorio actual: index.html y README.md si existen.
 * --all    audita todos los .html y .md del árbol (por defecto, solo los de la raíz).
 * --links  verifica enlaces relativos locales (no hace red).
 * --strict sale con código 1 si hay fallos.
 * --json   salida legible por máquina.
 *
 * No sustituye la lectura humana: reporta hechos, no criterio.
 */

const fs = require('fs');
const path = require('path');

const args = process.argv.slice(2);
const flags = new Set(args.filter((a) => a.startsWith('--')));
const targets = args.filter((a) => !a.startsWith('--'));
const checkLinks = flags.has('--links');
const scanAll = flags.has('--all');
const strict = flags.has('--strict');
const asJson = flags.has('--json');

const AI_TELLS = [
  { re: /\bno es\s+[^.,;]{1,50},?\s+(es|sino)\b/gi, msg: 'contraste "no es X, es Y"' },
  { re: /\bno solo\b[^.]{0,60}\bsino\b/gi, msg: 'contraste "no solo X, sino Y"' },
  { re: /\bsin\s+\w+,\s*sin\s+\w+/gi, msg: 'lista de negaciones "sin X, sin Y"' },
  { re: /¿(el|la)\s+(resultado|clave|secreto|mejor|diferencia)\?/gi, msg: 'pregunta retórica autrespondida' },
  { re: /\ben un mundo (donde|en el que)\b/gi, msg: 'apertura "en un mundo donde"' },
  { re: /\ben la era digital\b/gi, msg: 'apertura "en la era digital"' },
  { re: /\bdescubre (el poder|todo lo que)\b/gi, msg: 'frase de folleto "descubre el poder"' },
  { re: /\b(llevar?|lleva)\b[^.]{0,40}\bal siguiente nivel\b/gi, msg: 'frase de folleto "siguiente nivel"' },
  { re: /\bsoluci[oó]n innovadora\b/gi, msg: 'frase vacía "solución innovadora"' },
  { re: /\btransforma (tu|la) experiencia\b/gi, msg: 'frase vacía "transforma tu experiencia"' },
  { re: /\bsinergia\b/gi, msg: 'palabra inflada "sinergia"' },
  { re: /\bde clase mundial\b/gi, msg: 'afirmación sin prueba "de clase mundial"' },
  { re: /\bapasionad[oa]s? por\b/gi, msg: 'frase de folleto "apasionados por"' },
  { re: /\bgame[- ]?changer\b/gi, msg: 'buzzword EN "game-changer"' },
  { re: /\b(unlock|elevate|seamless|cutting[- ]edge|world[- ]class)\b/gi, msg: 'buzzword EN' },
  { re: /\b(muy|realmente|increíblemente|sumamente)\b/gi, msg: 'intensificador vacío' },
];

const WEAK_CTAS = [
  'enviar', 'más información', 'mas informacion', 'clic aquí', 'click aqui',
  'conoce más', 'conocer más', 'contáctanos', 'learn more', 'click here',
  'submit', 'get started', 'más info', 'ver más',
];

const SKIP_DIRS = ['node_modules', '.git', '.agents', '.claude', '03_build', '.deploy', '.netlify', 'build', '01_src', '.tmp_stickman', '.tmp_stickman_dir', 'video-agent'];

function walk(p) {
  const stat = fs.statSync(p);
  if (stat.isFile()) return [p];
  const out = [];
  for (const entry of fs.readdirSync(p, { withFileTypes: true })) {
    if (SKIP_DIRS.includes(entry.name)) continue;
    const full = path.join(p, entry.name);
    if (entry.isDirectory()) out.push(...walk(full));
    else if (/\.(html?|md)$/i.test(entry.name)) out.push(full);
  }
  return out;
}

function stripTags(html) {
  return html
    .replace(/<script[\s\S]*?<\/script>/gi, ' ')
    .replace(/<style[\s\S]*?<\/style>/gi, ' ')
    .replace(/<[^>]+>/g, ' ')
    .replace(/&nbsp;/g, ' ')
    .replace(/&amp;/g, '&')
    .replace(/\s+/g, ' ')
    .trim();
}

function textOf(fragment) {
  return stripTags(fragment).trim();
}

function auditHtml(file, html) {
  const findings = [];
  const add = (level, msg, detail) => findings.push({ level, msg, detail });

  const title = (html.match(/<title[^>]*>([\s\S]*?)<\/title>/i) || [])[1];
  if (!title) add('fail', 'Falta <title>');
  else {
    const len = textOf(title).length;
    if (len < 15) add('warn', 'Title muy corto', `${len} caracteres`);
    else if (len > 60) add('warn', 'Title largo (se corta en Google)', `${len} caracteres`);
    else add('pass', 'Title con longitud correcta', `${len} caracteres`);
  }

  const desc = (html.match(/<meta[^>]+name=["']description["'][^>]+content=["']([^"']*)["']/i) ||
    html.match(/<meta[^>]+content=["']([^"']*)["'][^>]+name=["']description["']/i) || [])[1];
  if (!desc) add('fail', 'Falta meta description');
  else {
    const len = desc.length;
    if (len < 70) add('warn', 'Meta description corta', `${len} caracteres`);
    else if (len > 160) add('warn', 'Meta description larga', `${len} caracteres`);
    else add('pass', 'Meta description con longitud correcta', `${len} caracteres`);
  }

  if (!/<html[^>]+lang=/i.test(html)) add('warn', 'Falta lang en <html>');
  if (!/<meta[^>]+name=["']viewport["']/i.test(html)) add('fail', 'Falta meta viewport');
  if (!/property=["']og:title["']/i.test(html)) add('warn', 'Falta og:title');
  if (!/property=["']og:description["']/i.test(html)) add('warn', 'Falta og:description');
  if (!/property=["']og:image["']/i.test(html)) add('warn', 'Falta og:image');
  if (!/rel=["'](icon|shortcut icon)["']/i.test(html)) add('warn', 'Falta favicon');

  const h1s = html.match(/<h1\b[^>]*>[\s\S]*?<\/h1>/gi) || [];
  if (h1s.length === 0) add('fail', 'No hay <h1>');
  else if (h1s.length > 1) add('fail', `Hay ${h1s.length} <h1>; debe haber uno`);
  else add('pass', 'Un solo <h1>', textOf(h1s[0]).slice(0, 70));

  const headings = [...html.matchAll(/<h([1-4])\b[^>]*>([\s\S]*?)<\/h\1>/gi)];
  let prev = 0;
  for (const h of headings) {
    const level = Number(h[1]);
    if (prev && level > prev + 1) add('warn', `Salto de encabezado h${prev} → h${level}`, textOf(h[2]).slice(0, 50));
    prev = level;
  }

  const imgs = html.match(/<img\b[^>]*>/gi) || [];
  const noAlt = imgs.filter((img) => !/\balt\s*=/i.test(img));
  if (noAlt.length) add('fail', `${noAlt.length} imagen(es) sin alt`, noAlt.slice(0, 3).join(' | ').slice(0, 160));
  else if (imgs.length) add('pass', 'Todas las imágenes tienen alt');

  const ctas = [...html.matchAll(/<(a|button)\b[^>]*>([\s\S]*?)<\/\1>/gi)]
    .map((m) => textOf(m[2]))
    .filter((t) => t && t.length < 60);
  const weak = ctas.filter((t) => WEAK_CTAS.includes(t.toLowerCase().replace(/\s+/g, ' ').trim()));
  if (weak.length) add('warn', 'CTAs débiles', [...new Set(weak)].join(', '));
  else if (ctas.length) add('pass', 'Sin CTAs débiles detectados');

  const body = html.replace(/<head[\s\S]*?<\/head>/i, ' ');
  const text = textOf(body);
  const words = text.split(/\s+/).filter(Boolean).length;
  add('info', 'Extensión', `${words} palabras, ~${Math.max(1, Math.round(words / 200))} min de lectura`);

  const shortTexts = [
    ...h1s, ...headings.map((h) => h[0]),
    ...ctas.map((c) => `<span>${c}</span>`),
  ].join(' ');
  if (/—/.test(shortTexts)) add('warn', 'Guion largo en titular o CTA');

  let tells = 0;
  for (const { re, msg } of AI_TELLS) {
    const matches = body.match(re);
    if (matches) {
      tells += matches.length;
      add('warn', `Tic de IA: ${msg}`, `${matches.length} ocurrencia(s)`);
    }
  }
  if (!tells) add('pass', 'Sin tics de IA detectados');

  const emails = text.match(/[\w.+-]+@[\w-]+\.[\w.]+/g) || [];
  if (emails.length) add('info', 'Correos en texto', [...new Set(emails)].join(', '));

  return findings;
}

function auditReadme(file, md) {
  const findings = [];
  const add = (level, msg, detail) => findings.push({ level, msg, detail });
  const lines = md.split(/\r?\n/);
  const headings = lines.filter((l) => /^#{1,6}\s+/.test(l));
  const sections = headings.filter((l) => /^##\s+/.test(l)).length;

  add('info', 'Encabezados', `${headings.length} (${sections} secciones de nivel 2)`);
  if (sections < 3) add('warn', 'Pocas secciones: un README completo suele tener 5-12');
  else if (sections > 14) add('warn', 'Muchas secciones: considera mover detalle a docs enlazados');

  const badgeLines = lines.slice(0, 25).filter((l) => /shields\.io|badge/i.test(l));
  const badges = badgeLines.reduce((n, l) => n + (l.match(/!\[/g) || []).length, 0);
  if (badges > 4) add('warn', 'Demasiados badges', `${badges}; máximo recomendado 4`);
  else if (badges) add('pass', 'Badges dentro del límite', `${badges}`);

  const fence = lines.findIndex((l) => /^```/.test(l));
  const wordsBefore = fence === -1
    ? lines.join(' ').split(/\s+/).filter(Boolean).length
    : lines.slice(0, fence).join(' ').split(/\s+/).filter(Boolean).length;
  if (fence === -1 && !/```/.test(md)) add('info', 'Sin bloque de código', 'propio de repos de contenido');
  else if (wordsBefore > 250) add('warn', 'Demasiado texto antes del primer comando', `${wordsBefore} palabras`);

  const statusRe = /\b(alpha|beta|experimental|activa|active|estable|stable|mantenida|archivada|deprecated|en pausa)\b/i;
  if (statusRe.test(md)) add('pass', 'Declara estado del proyecto');
  else add('warn', 'No declara estado (beta, activa, experimental…)');

  if (/\b(production[- ]ready|listo para producci[oó]n|battle[- ]tested)\b/i.test(md)) {
    const hasCi = /shields\.io\/github\/.*(actions|workflow)/i.test(md);
    add(hasCi ? 'info' : 'warn', 'Afirma estar listo para producción', hasCi ? 'hay badge de CI' : 'sin evidencia de CI');
  }

  const placeholders = md.match(/\bTODO:|\bTBD:|lorem ipsum|\[FALTA|\[NECESITO/g) || [];
  if (placeholders.length) add('fail', 'Placeholders sin resolver', `${placeholders.length}`);

  const install = /(npm i|npm install|yarn|pnpm|pip install|gradlew|docker run|git clone|descarga|download|play\.google\.com)/i.test(md);
  add(install ? 'pass' : 'info', install ? 'Incluye camino de instalación o descarga' : 'Sin instalación (revisa si aplica)');

  const firstPara = lines.find((l) => l.trim() && !l.startsWith('#') && !l.startsWith('![') && !l.startsWith('['));
  if (firstPara) {
    const len = firstPara.trim().split(/\s+/).length;
    if (len > 55) add('warn', 'Primer párrafo largo', `${len} palabras; busca 15-40`);
  }

  if (checkLinks) {
    const links = [...md.matchAll(/\[[^\]]*\]\(([^)]+)\)/g)].map((m) => m[1]);
    const local = links.filter((l) => !/^(https?:|mailto:|#|\/)/i.test(l));
    let missing = 0;
    for (const l of local) {
      const clean = decodeURIComponent(l.split('#')[0].split('?')[0]);
      if (!clean) continue;
      const resolved = path.resolve(path.dirname(file), clean);
      if (!fs.existsSync(resolved)) {
        missing++;
        add('fail', 'Enlace relativo roto', l);
      }
    }
    if (!missing && local.length) add('pass', 'Enlaces relativos resueltos', `${local.length} revisados`);
  }

  return findings;
}

function render(file, findings) {
  const icon = { fail: 'FAIL', warn: 'WARN', pass: 'PASS', info: 'INFO' };
  console.log(`\n== ${path.relative(process.cwd(), file) || file} ==`);
  for (const f of findings) {
    console.log(`[${icon[f.level]}] ${f.msg}${f.detail ? ` — ${f.detail}` : ''}`);
  }
  const fails = findings.filter((f) => f.level === 'fail').length;
  const warns = findings.filter((f) => f.level === 'warn').length;
  console.log(`Resumen: ${fails} fallo(s), ${warns} advertencia(s)`);
  return fails;
}

function main() {
  const roots = targets.length ? targets : [process.cwd()];
  const files = [];
  for (const root of roots) {
    if (!fs.existsSync(root)) {
      console.error(`No existe: ${root}`);
      continue;
    }
    const stat = fs.statSync(root);
    if (stat.isFile()) {
      files.push(root);
      continue;
    }
    if (scanAll) {
      files.push(...walk(root));
      continue;
    }
    for (const name of ['index.html', 'README.md']) {
      const candidate = path.join(root, name);
      if (fs.existsSync(candidate)) files.push(candidate);
      else if (name === 'README.md') {
        const lower = path.join(root, 'readme.md');
        if (fs.existsSync(lower)) files.push(lower);
      }
    }
  }

  if (!files.length) {
    console.error('No se encontraron index.html ni README.md. Pasa una ruta explícita.');
    process.exit(strict ? 1 : 0);
  }

  let totalFails = 0;
  const report = [];
  for (const file of files) {
    const content = fs.readFileSync(file, 'utf8');
    const findings = /\.html?$/i.test(file) ? auditHtml(file, content) : auditReadme(file, content);
    report.push({ file, findings });
    totalFails += asJson ? findings.filter((f) => f.level === 'fail').length : render(file, findings);
  }

  if (asJson) {
    console.log(JSON.stringify(report, null, 2));
  } else {
    console.log(`\nTotal: ${files.length} archivo(s), ${totalFails} fallo(s) bloqueante(s).`);
    console.log('Recuerda: esto es mecánico. La prueba del lector frío y la verificación de datos son humanas.');
  }

  if (strict && totalFails > 0) process.exit(1);
}

main();
