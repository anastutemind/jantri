# moongrid

Draws the crescent-visibility maps the app ships on its Moon screen: one image per
evening, four bands, a world map you can find your own country on.

The point is that these are **made here**, from arithmetic, not copied from a
moonsighting site. No third-party imagery enters the APK, no runtime network call is
needed, and the picture and the app's own on-phone verdict come from the same engine,
so they cannot contradict each other.

## Criterion

Odeh (2004), as published by the International Astronomical Center:

```
V = ARCV - (-0.1018 W^3 + 0.7319 W^2 - 6.3226 W + 7.1651)
```

`ARCV` is the arc of vision at sunset and `W` the crescent width in arcminutes.

| Band | V | Meaning | Colour |
|---|---|---|---|
| A | `V >= 5.65` | easily visible with the naked eye | green `#1E7A3C` |
| B | `V >= 2.00` | visible with optical aid, and possibly then by eye | magenta `#C2185B` |
| C | `V >= -0.96` | binoculars or telescope only | blue `#1565C0` |
| D | below | not visible even with optical aid | red `#C62828` |

Cells where the moon sets before the sun, or where the new moon has not happened yet,
are drawn red too. Everything else stays blank.

## Run

```bash
cd tools/moongrid
bun install
bun run gen.ts          # writes app/src/main/assets/moon/
MOON_OUT=/tmp/maps bun run gen.ts   # render somewhere else
```

Takes a few minutes: 36 maps, each one 361 x 121 grid points with a sunset search at
every point. Before drawing anything it checks three known geometries against fixed
values and aborts if any differs by more than 0.05 degrees, because a silently wrong
map is worse than no map.

## Output

- `app/src/main/assets/moon/{hijriYear}{month}_{YYYY-MM-DD}.webp`, 1200x600, WebP q82.
  Month codes: `rmd` Ramadan, `shw` Shawwal, `zhj` Dhul Hijjah.
- `manifest.json` maps year to month to the evenings rendered. The app reads this, so
  it never constructs a filename and never renders a year nobody generated.
- `zones.json` holds, per map, the percentage of the globe in each band. The app shows
  it as "easy from 10% of the globe, impossible from 49%".

## Editing

- **Years:** `YEARS` at the top of `gen.ts`.
- **Palette:** the `C` object, and keep `MoonScreen.kt`'s `BandA..BandD` in step.
- **Criterion:** swap the `V` expression and the band cuts in `evalCell`.

## Sources and licences

- Moon and sun positions: [Astronomy Engine](https://github.com/cosinekitty/astronomy) 2.1.19, MIT, Don Cross.
- Coastlines: Natural Earth 110m land polygons, public domain, committed here as
  `ne_110m_land.geojson`.
- The Odeh criterion is published science; the numbers it produces are ours.

Nothing in this folder is derived from moonsighting.com or any other site's images.
