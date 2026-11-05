# Jantri

Ramadan sehri and iftar times for India, calculated on the phone, with the two controls that make one timetable differ from the next mosque's: the twilight angle and the printed safety margin.

A *jantri* (also *jantari*) is the sheet masjids hand out one or two weeks before Ramadan, with a row per day carrying the sehri end time, the iftar time and the two supplications. It has to be printed and distributed before the month begins, which is where its two problems come from:

1. **It cannot know the sighting.** Day 1 is decided by the local crescent sighting, often one or two days after the arithmetic calendar says the month starts. A sheet printed in advance is off by that much unless someone reprints.
2. **It carries someone else's margins.** Each sheet is built for one city and one set of assumptions, so a town 40 km away, or a masjid that follows a different angle, sees times a few minutes different from its own wall sheet.

This app calculates the times from the sun's position at your exact coordinates, lets you shift them to match the sheet on your wall, and alerts you before sehri ends and at iftar.

## How the times are actually produced

Every printed timetable is the same astronomy, in four steps:

1. **Sun position.** For a given moment, two numbers are enough: the sun's declination δ (how far north or south of the celestial equator it stands) and the equation of time E (how far the real sun runs ahead of or behind the clock). Both come from the Julian day and standard low-precision solar formulae.
2. **Solar noon.** Local apparent noon is `12:00 − E − (longitude − timezone meridian)/15`.
3. **Hour angle.** For a required solar altitude `h`, the sun sits that far from noon at the hour angle
   `cos(H) = (sin h − sin φ sin δ) / (cos φ cos δ)`, where φ is latitude. Time from noon is `H/15` hours.
4. **Altitudes per prayer.**
   - Sunrise and sunset: `h = −0.833°` (34′ atmospheric refraction + 16′ solar semi-diameter).
   - Asr: the altitude at which a shadow equals the object (Shafi'i, `1×`) or twice it (Hanafi, `2×`).
   - **Fajr / sehri end and Isha: `h = −angle°`, where the angle is the twilight convention below.**
   - Maghrib / iftar: sunset.

The angle is the whole argument. It stands in for "the light in the sky when the true dawn line spreads", which is an observation, not a number, so different authorities picked different numbers from their own observations.

| Method | Fajr | Isha | Where it is used |
|---|---|---|---|
| **Karachi** (University of Islamic Sciences) | 18° | 18° | India, Pakistan, Bangladesh, Afghanistan, and most Indian jantri sheets |
| Muslim World League | 18° | 17° | Europe, Far East, parts of the US |
| ISNA | 15° | 15° | North America |
| Egyptian General Authority | 19.5° | 17.5° | Egypt, Syria, Lebanon, Malaysia |
| Umm al-Qura (Makkah) | 18.5° | 90 min after maghrib | Arabian peninsula |
| Shia Ithna Ashari (Jafari) | 16° | 14°, maghrib 4° | Shia communities |
| Tehran (Geophysics) | 17.7° | 14°, maghrib 4.5° | Iran |

Indian ulama settled on 18° for both, following Mufti Muhammad Shafi (Fatawa Uthmaani 1:390), which is why the country's sheets agree with each other to a minute or two and disagree with a 15° calculation by ten to fifteen minutes. In Bhopal on 1 March the difference between 18° and 15° sehri is about 14 minutes, as the fixture test asserts.

**Why timezone, not UTC, matters:** each calculation is done for the calendar date at the location, then converted to local clock time with the longitude correction, so a city far from its timezone meridian is handled correctly.

## Matching a printed sheet

Two margins sit on top of the astronomy, and both are exposed as settings:

- **Sehri safety margin** (default 2 min): sehri is declared over this many minutes *before* Fajr.
- **Iftar safety margin** (default 1 min): iftar comes this many minutes *after* sunset.
- **Per-prayer offsets**: ±60 min on each of Fajr, sunrise, Dhuhr, Asr, Maghrib, Isha.

The defaults are not guesses. They are the margins a real sheet uses: with 18°/18°, 2 min of sehri margin and 1 min of iftar margin, the engine reproduces the printed Bhopal sheet for Ramadan 1447 (19 Feb – 20 Mar 2026) on all 30 rows, both columns, within one minute. That sheet is a unit test in `JantriFixtureTest`.

## The sighting

Ramadan day 1 defaults to the Umm al-Qura date from the Hijri calendar shipped with Android (`java.time.chrono.HijrahDate`), which for 1447 is 18 February 2026, one day before India actually began. The Timing screen shifts day 1 by up to three days in either direction, so the month number, the date column and the alerts all follow what your masjid announced rather than what a printed sheet had to assume.

## Alerts

- Sehri warning N minutes before sehri ends (default 15).
- Sehri ends, at Fajr.
- Iftar warning N minutes before maghrib (default 10).
- Iftar, at maghrib, with the iftar dua.

Alarms are armed 45 days ahead with `AlarmManager.setExactAndAllowWhileIdle`, re-armed on app open, on boot, on package update, on a clock or timezone change, and after every alert that fires. The app asks for the "Alarms & reminders" permission (Android 12+) because without it the system is free to delay an alert past the moment it matters.

## Everything stays on the phone

No network calls, no accounts, no analytics. City coordinates come bundled from GeoNames (CC BY 4.0, 2 500 Indian places over 10 000 people); the sun position is arithmetic.

## Build

```bash
export JAVA_HOME=/path/to/jdk21 ANDROID_HOME=/path/to/android-sdk
./gradlew testDebugUnitTest assembleDebug    # tests + installable APK
./gradlew bundleRelease                      # Play upload bundle (needs keystore.properties)
```

`compileSdk`/`targetSdk` are 36 (Android 16), `minSdk` 26.

Signing keys never enter the repo: put `storeFile`, `storePassword`, `keyAlias`, `keyPassword` in a gitignored `keystore.properties`, or set `JANTRI_KEYSTORE_FILE`, `JANTRI_KEYSTORE_PASSWORD`, `JANTRI_KEY_ALIAS`, `JANTRI_KEY_PASSWORD`.

Store screenshots are real renders of the real composables on the Android runtime via Robolectric: `./gradlew testDebugUnitTest --tests '*ScreenshotTest*'` writes them to `build/screenshots`.

## Tests

- `JantriFixtureTest` — the astronomy against a printed 1447 Bhopal sheet (60 values, ≤1 min), prayer ordering, Hanafi vs Shafi'i Asr, 18° vs 15° sehri difference, exact offset arithmetic.
- `RamadanAndScheduleTest` — Umm al-Qura start dates, one-day sighting shift, month boundaries, alert planning order and de-duplication, alerts turned off.
