// Crescent-visibility world maps for the app's Moon screen.
//
// A real map underneath, the four Odeh (2004) bands laid over it translucently: the
// geography stays readable, and where no sighting is possible you see the map itself
// rather than a solid block of colour.
//
// Run:  bun install && bun run gen.ts            # all maps
//       bun run gen.ts 1448rmd_2027-02-07        # one map, for drawing experiments
// Out:  app/src/main/assets/moon/*.webp, manifest.json, zones.json
//
// Criterion (Odeh 2004, published by the International Astronomical Center):
//   V = ARCV - (-0.1018 W^3 + 0.7319 W^2 - 6.3226 W + 7.1651)
//   A V >= 5.65   easily visible with the naked eye
//   B V >= 2.00   visible with optical aid, and possibly then by eye
//   C V >= -0.96  binoculars or telescope only
//   D below       not visible even with optical aid

import * as A from "astronomy-engine";
import sharp from "sharp";
import { readFileSync, writeFileSync, mkdirSync, existsSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const HERE = dirname(fileURLToPath(import.meta.url));
const REPO = join(HERE, "..", "..");

/** Where the maps land. Override with MOON_OUT to render into a scratch folder. */
const OUT = process.env.MOON_OUT ?? join(REPO, "app", "src", "main", "assets", "moon");

const IMG_W = 2000;
const IMG_H = 839; // Mercator between 60N and 60S keeps this aspect
const LON_STEP = 0.5;
const LAT_STEP = 0.5;
const LAT_MAX = 60;
const LAT_MIN = -60;
const COLS = Math.round(360 / LON_STEP) + 1; // 721
const ROWS = Math.round((LAT_MAX - LAT_MIN) / LAT_STEP) + 1; // 241

/** Hijri years and the three months the app shows. */
const YEARS = [1447, 1452];
const MONTHS: Record<string, number> = { rmd: 9, shw: 10, zhj: 12 };

// Band colours. The app's legend draws these at the same alpha, so the square beside
// the words matches the pixels it names.
const BAND = {
  A: [0x1e, 0x7a, 0x3c],
  B: [0xc2, 0x18, 0x5b],
  C: [0x15, 0x65, 0xc0],
  D: [0xc6, 0x28, 0x28],
} as const;
const BAND_ORDER = ["A", "B", "C", "D"] as const;
const BAND_ALPHA = Number(process.env.MOON_ALPHA ?? 0.45);

// Basemap: pale sea, warm land, thin borders, so the tint reads as a wash rather than
// a paint-over.
const MAP = {
  ocean: "#AECBE2",
  land: "#F7F2E2",
  edge: "#6F7C74",
  border: "#8E8878",
};

interface Cell {
  zone: number;
  moonAlt: number;
  sunAlt: number;
  arcv: number;
  elong: number;
  w: number;
  V: number;
}

function evalCell(lat: number, lon: number, dayStart: Date, conjunction: Date): Cell | null {
  const obs = new A.Observer(lat, lon, 0);
  const riseSet = A.SearchRiseSet(A.Body.Sun, obs, -1, dayStart, 2);
  if (!riseSet) return null;
  const sunset = riseSet.date;

  const sunEq = A.Equator(A.Body.Sun, sunset, obs, true, true);
  const sunHor = A.Horizon(sunset, obs, sunEq.ra, sunEq.dec, "normal");
  const moonEq = A.Equator(A.Body.Moon, sunset, obs, true, true);
  const moonHor = A.Horizon(sunset, obs, moonEq.ra, moonEq.dec, "normal");

  const moonAlt = moonHor.altitude;
  const sunAlt = sunHor.altitude;
  const arcv = moonAlt - sunAlt;
  const elong = A.AngleFromSun(A.Body.Moon, sunset);
  const distanceKm = A.Illumination(A.Body.Moon, sunset).geo_dist * 149597870.7;
  const semiDiameter = ((Math.asin(1737.4 / distanceKm) * 180) / Math.PI) * 60;
  // Crescent width is the illuminated sliver across the disc: semi-diameter times
  // (1 - cos(arc of light)). Zero at conjunction, the full diameter at full moon.
  const w = semiDiameter * (1 - Math.cos((elong * Math.PI) / 180));
  const V = arcv - (-0.1018 * w * w * w + 0.7319 * w * w - 6.3226 * w + 7.1651);

  let zone: number;
  if (sunset.getTime() < conjunction.getTime()) zone = 4; // the new moon has not happened yet
  else if (moonAlt < 0) zone = 4; // the moon was already below the horizon at sunset
  else if (V >= 5.65) zone = 1;
  else if (V >= 2.0) zone = 2;
  else if (V >= -0.96) zone = 3;
  else zone = 4;

  return { zone, moonAlt, sunAlt, arcv, elong, w, V };
}

/**
 * Three points checked against an independent implementation before anything is drawn.
 * A difference over 0.05 degrees aborts the run: a silently wrong map is worse than no map.
 */
function sanity(): void {
  const cases = [
    { lat: 17.384, lon: 78.4564, evening: "2027-02-07", exp: { moonAlt: 7.95, sunAlt: -0.218, arcv: 8.168, elong: 9.603, V: 2.29 } },
    { lat: 21.4225, lon: 39.8262, evening: "2027-02-07", exp: { moonAlt: 9.128, sunAlt: -0.218, arcv: 9.346, elong: 10.764, V: 3.79 } },
    { lat: 17.384, lon: 78.4564, evening: "2027-02-08", exp: { moonAlt: 19.088, sunAlt: -0.218, arcv: 19.306, elong: 20.842, V: 17.74 } },
  ];
  let ok = true;
  for (const c of cases) {
    const [y, m, d] = c.evening.split("-").map(Number);
    const r = evalCell(c.lat, c.lon, new Date(Date.UTC(y, m - 1, d)), new Date(0));
    if (!r) {
      console.error(`SANITY: no sunset for ${c.evening}`);
      ok = false;
      continue;
    }
    const got: Record<string, number> = { moonAlt: r.moonAlt, sunAlt: r.sunAlt, arcv: r.arcv, elong: r.elong, V: r.V };
    for (const [k, expected] of Object.entries(c.exp)) {
      if (Math.abs(got[k] - expected) > 0.05) {
        console.error(`SANITY MISMATCH ${c.evening} ${k}: got ${got[k].toFixed(3)} expected ${expected}`);
        ok = false;
      }
    }
    console.log(
      `SANITY ${c.evening} ${c.lat},${c.lon}: alt=${r.moonAlt.toFixed(3)} arcv=${r.arcv.toFixed(3)} ` +
        `elong=${r.elong.toFixed(3)} W=${r.w.toFixed(3)} V=${r.V.toFixed(2)} zone=${"ABCD"[r.zone - 1]}`,
    );
  }
  if (!ok) {
    console.error("SANITY FAILED, nothing written");
    process.exit(1);
  }
  console.log("SANITY OK");
}

function addDays(iso: string, n: number): string {
  const [y, m, d] = iso.split("-").map(Number);
  return new Date(Date.UTC(y, m - 1, d + n)).toISOString().slice(0, 10);
}

/** Anchors each Hijri month to its own conjunction, then takes the two evenings after. */
function buildJobs(): Array<{ hijri: number; code: string; evening: string; conjunction: Date }> {
  const jobs: Array<{ hijri: number; code: string; evening: string; conjunction: Date }> = [];
  const newMoons: Date[] = [];
  let t = new Date(Date.UTC(2026, 0, 1));
  for (let i = 0; i < 120; i++) {
    const found = A.SearchMoonPhase(0, t, 40);
    if (!found) break;
    newMoons.push(found.date);
    t = new Date(found.date.getTime() + 86400000);
  }
  const anchorIndex = newMoons.findIndex((d) => d.toISOString().slice(0, 10) === "2027-02-06");
  if (anchorIndex < 0) throw new Error("anchor conjunction 2027-02-06 not found");

  for (let hy = YEARS[0]; hy <= YEARS[1]; hy++) {
    for (const code of Object.keys(MONTHS)) {
      const index = anchorIndex + (hy - 1448) * 12 + (MONTHS[code] - 9);
      const conjunction = newMoons[index];
      if (!conjunction) throw new Error(`no conjunction for ${hy}${code} (index ${index})`);
      const conjunctionDate = conjunction.toISOString().slice(0, 10);
      for (const evening of [addDays(conjunctionDate, 1), addDays(conjunctionDate, 2)]) {
        jobs.push({ hijri: hy, code, evening, conjunction });
      }
      console.log(`${hy}${code}: conjunction ${conjunction.toISOString()} -> ${addDays(conjunctionDate, 1)}, ${addDays(conjunctionDate, 2)}`);
    }
  }
  return jobs;
}

// ---------- projection ----------
// Mercator between 60N and 60S: the view the printed crescent tables use, where the
// bands keep their familiar shape.

const mercator = (latDeg: number): number => {
  const r = (latDeg * Math.PI) / 180;
  return Math.log(Math.tan(Math.PI / 4 + r / 2));
};
const MERC_TOP = mercator(LAT_MAX);
const MERC_BOTTOM = mercator(LAT_MIN);

const projectX = (lon: number): number => ((lon + 180) / 360) * IMG_W;
const projectY = (lat: number): number => ((mercator(lat) - MERC_TOP) / (MERC_BOTTOM - MERC_TOP)) * IMG_H;

/** Country polygons of Natural Earth 1:50m (public domain), clamped to the view. */
function countryPaths(): string {
  const geo = JSON.parse(readFileSync(join(HERE, "ne_50m_admin_0_countries.geojson"), "utf8"));
  let path = "";
  const ring = (coords: number[][]) => {
    let previous: number[] | null = null;
    for (const [lon, rawLat] of coords) {
      const lat = Math.max(LAT_MIN, Math.min(LAT_MAX, rawLat));
      const x = projectX(lon).toFixed(1);
      const y = projectY(lat).toFixed(1);
      const wrapsDateline = previous !== null && Math.abs(lon - previous[0]) > 180;
      path += `${previous === null || wrapsDateline ? "M" : "L"}${x},${y}`;
      previous = [lon, rawLat];
    }
  };
  for (const feature of geo.features) {
    const g = feature.geometry;
    if (!g) continue;
    if (g.type === "Polygon") g.coordinates.forEach(ring);
    else if (g.type === "MultiPolygon") g.coordinates.forEach((polygon: number[][][]) => polygon.forEach(ring));
  }
  return path;
}

/** The actual map: pale sea, land fill, country borders. */
function basemapSvg(path: string): string {
  return (
    `<svg xmlns="http://www.w3.org/2000/svg" width="${IMG_W}" height="${IMG_H}" viewBox="0 0 ${IMG_W} ${IMG_H}">` +
    `<rect width="${IMG_W}" height="${IMG_H}" fill="${MAP.ocean}"/>` +
    `<path d="${path}" fill="${MAP.land}" stroke="${MAP.border}" stroke-width="0.9" stroke-linejoin="round"/>` +
    `</svg>`
  );
}

/** Coastlines and a light graticule above the tint, so the map stays legible through it. */
function detailSvg(path: string): string {
  let grid = "";
  for (let lon = -180; lon <= 180; lon += 30) {
    const x = projectX(lon).toFixed(1);
    grid += `M${x},0L${x},${IMG_H}`;
  }
  for (let lat = -60; lat <= 60; lat += 30) {
    const y = projectY(lat).toFixed(1);
    grid += `M0,${y}L${IMG_W},${y}`;
  }
  return (
    `<svg xmlns="http://www.w3.org/2000/svg" width="${IMG_W}" height="${IMG_H}" viewBox="0 0 ${IMG_W} ${IMG_H}">` +
    `<path d="${grid}" fill="none" stroke="${MAP.edge}" stroke-opacity="0.16" stroke-width="0.5"/>` +
    `<path d="${path}" fill="none" stroke="${MAP.edge}" stroke-opacity="0.75" stroke-width="0.8" stroke-linejoin="round"/>` +
    `</svg>`
  );
}

async function render(
  job: { hijri: number; code: string; evening: string; conjunction: Date },
  base: string,
  detail: string,
): Promise<{ file: string; evening: string; key: string; zones: Record<string, number> }> {
  const [y, m, d] = job.evening.split("-").map(Number);
  const dayStart = new Date(Date.UTC(y, m - 1, d));
  const grid = new Uint8Array(COLS * ROWS);
  const counts = [0, 0, 0, 0, 0];

  for (let r = 0; r < ROWS; r++) {
    const lat = LAT_MAX - r * LAT_STEP;
    for (let c = 0; c < COLS; c++) {
      const cell = evalCell(lat, -180 + c * LON_STEP, dayStart, job.conjunction);
      const zone = cell ? cell.zone : 0;
      grid[r * COLS + c] = zone;
      counts[zone]++;
    }
  }

  // Transparent overlay: only the bands carry colour, everything else stays map.
  const overlay = Buffer.alloc(IMG_W * IMG_H * 4);
  const alpha = Math.round(BAND_ALPHA * 255);
  for (let r = 0; r < ROWS; r++) {
    const latTop = LAT_MAX - r * LAT_STEP;
    const latBottom = latTop - LAT_STEP;
    const y0 = Math.max(0, Math.round(projectY(latTop)));
    const y1 = Math.min(IMG_H, Math.max(y0 + 1, Math.round(projectY(latBottom))));
    for (let c = 0; c < COLS; c++) {
      const zone = grid[r * COLS + c];
      if (zone === 0) continue;
      const [red, green, blue] = BAND[BAND_ORDER[zone - 1]];
      const x0 = Math.max(0, Math.round(projectX(-180 + c * LON_STEP)));
      const x1 = Math.min(IMG_W, Math.max(x0 + 1, Math.round(projectX(-180 + (c + 1) * LON_STEP))));
      for (let yy = y0; yy < y1; yy++) {
        for (let xx = x0; xx < x1; xx++) {
          const offset = (yy * IMG_W + xx) * 4;
          overlay[offset] = red;
          overlay[offset + 1] = green;
          overlay[offset + 2] = blue;
          overlay[offset + 3] = alpha;
        }
      }
    }
  }

  const file = `${job.hijri}${job.code}_${job.evening}.webp`;
  await sharp(Buffer.from(base))
    .composite([
      { input: overlay, raw: { width: IMG_W, height: IMG_H, channels: 4 } },
      { input: Buffer.from(detail) },
    ])
    .webp({ quality: 84 })
    .toFile(join(OUT, file));

  const total = COLS * ROWS;
  const share = (n: number) => Math.round((n / total) * 1000) / 10;
  const zones = { A: share(counts[1]), B: share(counts[2]), C: share(counts[3]), D: share(counts[4]) };
  console.log(`${file}: A=${zones.A}% B=${zones.B}% C=${zones.C}% D=${zones.D}% blank=${share(counts[0])}%`);
  return { file, evening: job.evening, key: `${job.hijri}${job.code}_${job.evening}`, zones };
}

async function main(): Promise<void> {
  sanity();
  if (!existsSync(join(HERE, "ne_50m_admin_0_countries.geojson"))) {
    throw new Error("ne_50m_admin_0_countries.geojson is missing from tools/moongrid");
  }
  mkdirSync(OUT, { recursive: true });
  const only = process.argv[2];
  const jobs = buildJobs().filter((j) => !only || `${j.hijri}${j.code}_${j.evening}` === only);
  if (jobs.length === 0) throw new Error(`no job matched ${only}`);
  const path = countryPaths();
  const base = basemapSvg(path);
  const detail = detailSvg(path);
  console.log(`basemap ${(base.length / 1024).toFixed(0)} KB, grid ${COLS}x${ROWS}, ${jobs.length} images`);

  const manifest: Record<string, Record<string, Array<{ evening: string; file: string }>>> = {};
  const zones: Record<string, Record<string, number>> = {};
  for (const job of jobs) {
    const result = await render(job, base, detail);
    (manifest[job.hijri] ??= {})[job.code] ??= [];
    manifest[job.hijri][job.code].push({ evening: result.evening, file: result.file });
    zones[result.key] = result.zones;
  }
  if (!only) {
    writeFileSync(join(OUT, "manifest.json"), JSON.stringify(manifest, null, 1) + "\n");
    writeFileSync(join(OUT, "zones.json"), JSON.stringify(zones, null, 1) + "\n");
  }
  console.log(`DONE: ${jobs.length} maps${only ? "" : `, ${Object.keys(zones).length} zone records`} in ${OUT}`);
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
