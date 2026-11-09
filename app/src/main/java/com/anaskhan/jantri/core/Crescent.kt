package com.anaskhan.jantri.core

import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Direction
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Refraction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.equator
import io.github.cosinekitty.astronomy.elongation
import io.github.cosinekitty.astronomy.geoMoon
import io.github.cosinekitty.astronomy.horizon
import io.github.cosinekitty.astronomy.searchMoonPhase
import io.github.cosinekitty.astronomy.searchRiseSet
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.chrono.HijrahDate
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow

/**
 * Crescent visibility for one place on one evening, scored with the Odeh (2004)
 * criterion.
 *
 * The criterion is a single number, V, measured in degrees:
 *
 *   V = ARCV - (-0.1018 W^3 + 0.7319 W^2 - 6.3226 W + 7.1651)
 *
 * where ARCV is the arc of vision (moon altitude minus sun altitude at sunset) and W
 * is the crescent width in arcminutes. V compares tonight's geometry against the
 * thinnest crescent that has actually been seen at that width, so the bands mean
 * something concrete:
 *
 *   A  V >= 5.65    easily visible with the naked eye
 *   B  2.00 .. 5.65 visible with optical aid, and maybe then with the eye
 *   C -0.96 .. 2.00 binoculars or a telescope only
 *   D  below -0.96  not visible even with optical aid
 *
 * These are the same four bands the bundled world maps are drawn in, so the map and
 * the verdict for a city can never disagree. That is the whole reason this lives in
 * the app rather than in a web call: the app is offline by design, and a phone that
 * can compute the sun can compute the moon.
 *
 * All astronomy comes from Astronomy Engine (MIT, Don Cross), which the generator
 * that drew the bundled maps uses too, so the picture and the number share a source.
 */
object Crescent {

    enum class Zone {
        A, B, C, D,
        /** No crescent above the horizon that evening: it set before the sun, or the new moon had not yet happened. */
        NONE;

        val letter: String get() = if (this == NONE) "-" else name

        /** Visible to an unaided eye, by this criterion. */
        val byEye: Boolean get() = this == A || this == B

        /** Locatable with binoculars or a telescope. */
        val withAid: Boolean get() = this == A || this == B || this == C
    }

    /** One evening, from one place. */
    data class Evening(
        val date: LocalDate,
        val sunset: ZonedDateTime?,
        val moonAltitude: Double,
        val sunAltitude: Double,
        val arcv: Double,
        val elongation: Double,
        val widthArcMin: Double,
        val v: Double,
        val zone: Zone,
        val moonSetsFirst: Boolean = false,
        val conjunctionAfterSunset: Boolean = false,
    ) {
        val crescentUp: Boolean get() = sunset != null && zone != Zone.NONE
    }

    /** A month's opening, as seen from one place. */
    data class Outlook(
        val hijriYear: Int,
        val hijriMonth: Int,
        val code: String,
        val title: String,
        val conjunction: Instant,
        val evenings: List<Evening>,
        /** First day of the month if the local sighting is accepted on the first evening it is possible. */
        val firstDay: LocalDate?,
        /** The evening whose sighting produces [firstDay]. */
        val sightingEvening: LocalDate?,
        /** First day if only an optical sighting is required. */
        val firstDayWithAid: LocalDate?,
    ) {
        /** True when the only way to start the month on [firstDay] was through optical aid. */
        val needsAid: Boolean
            get() = firstDay != null && evenings.firstOrNull { it.date == sightingEvening }?.zone == Zone.C
    }

    private const val J2000 = 2451545.0
    private const val UNIX_EPOCH_JD = 2440587.5
    private const val MOON_RADIUS_KM = 1737.4

    /** Astronomy Engine vectors are in astronomical units. */
    private const val AU_KM = 149_597_870.7

    fun timeOf(instant: Instant): Time =
        Time(UNIX_EPOCH_JD + instant.toEpochMilli() / 86_400_000.0 - J2000)

    fun instantOf(time: Time): Instant = Instant.ofEpochMilli(
        ((time.ut + J2000 - UNIX_EPOCH_JD) * 86_400_000.0).toLong()
    )

    /** The Odeh V number for one geometry. Exposed so tests can check the bands directly. */
    fun vValue(arcv: Double, widthArcMin: Double): Double {
        val w = widthArcMin
        val minArcv = -0.1018 * w.pow(3) + 0.7319 * w.pow(2) - 6.3226 * w + 7.1651
        return arcv - minArcv
    }

    fun zoneOf(v: Double): Zone = when {
        v >= 5.65 -> Zone.A
        v >= 2.00 -> Zone.B
        v >= -0.96 -> Zone.C
        else -> Zone.D
    }

    /**
     * The evening of [date] at this place: sunset, the moon's position at that moment,
     * and the resulting band. [date] is the civil date of the evening, which is the day
     * that ends when the month begins.
     */
    fun evening(
        lat: Double,
        lng: Double,
        elevationMetres: Double,
        date: LocalDate,
        zone: ZoneId,
    ): Evening {
        val observer = Observer(lat, lng, elevationMetres)
        val dayStart = timeOf(date.atStartOfDay(zone).toInstant())
        val sunsetTime = searchRiseSet(Body.Sun, observer, Direction.Set, dayStart, 1.0)
            ?: return Evening(date, null, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Zone.NONE)

        val sunsetInstant = instantOf(sunsetTime)
        val moonsetTime = searchRiseSet(Body.Moon, observer, Direction.Set, dayStart, 1.0)
        val moonsetFirst = moonsetTime == null || instantOf(moonsetTime).isBefore(sunsetInstant)

        val sunEq = equator(Body.Sun, sunsetTime, observer, EquatorEpoch.OfDate, Aberration.Corrected)
        val sunHor = horizon(sunsetTime, observer, sunEq.ra, sunEq.dec, Refraction.Normal)
        val moonEq = equator(Body.Moon, sunsetTime, observer, EquatorEpoch.OfDate, Aberration.Corrected)
        val moonHor = horizon(sunsetTime, observer, moonEq.ra, moonEq.dec, Refraction.Normal)

        val moonAlt = moonHor.altitude
        val sunAlt = sunHor.altitude
        val arcv = moonAlt - sunAlt
        val elong = elongation(Body.Moon, sunsetTime).elongation

        val distanceKm = geoMoon(sunsetTime).length() * AU_KM
        val semiDiameterArcMin = Math.toDegrees(asin(MOON_RADIUS_KM / distanceKm)) * 60.0
        // Crescent width is the illuminated sliver across the moon's disc: semi-diameter
        // times (1 - cos(arc of light)). At conjunction the sliver is zero, at full moon
        // it is the whole diameter, which is how Yallop and Odeh both define it.
        val width = semiDiameterArcMin * (1.0 - cos(Math.toRadians(elong)))

        val conjunction = newestMoonBefore(sunsetInstant)
        val conjunctionAfterSunset = conjunction != null && conjunction.isAfter(sunsetInstant)

        val v = vValue(arcv, width)
        val zoneScore = when {
            moonsetFirst || conjunctionAfterSunset -> Zone.NONE
            else -> zoneOf(v)
        }

        return Evening(
            date = date,
            sunset = ZonedDateTime.ofInstant(sunsetInstant, zone),
            moonAltitude = moonAlt,
            sunAltitude = sunAlt,
            arcv = arcv,
            elongation = elong,
            widthArcMin = width,
            v = v,
            zone = zoneScore,
            moonSetsFirst = moonsetFirst,
            conjunctionAfterSunset = conjunctionAfterSunset,
        )
    }

    /** The most recent new moon at or before [instant], searched back three weeks. */
    private fun newestMoonBefore(instant: Instant): Instant? {
        var probe = instant
        var guard = 0
        while (guard++ < 4) {
            val from = timeOf(probe.minus(Duration.ofDays(30)))
            val found = searchMoonPhase(0.0, from, 30.0) ?: return null
            val at = instantOf(found)
            if (!at.isAfter(instant)) return at
            probe = probe.minus(Duration.ofDays(31))
        }
        return null
    }

    /** Next new moon after [instant]. */
    fun nextConjunction(instant: Instant): Instant? {
        val found = searchMoonPhase(0.0, timeOf(instant), 40.0) ?: return null
        return instantOf(found)
    }

    private val monthTitles = mapOf(
        9 to "Ramadan",
        10 to "Shawwal (Eid al-Fitr)",
        12 to "Dhul Hijjah (Eid al-Adha)",
    )
    private val monthCodes = mapOf(9 to "rmd", 10 to "shw", 12 to "zhj")

    /**
     * The opening of a Hijri month at this place, with the evenings a sighting is
     * possible. [hijriMonth] is 9, 10 or 12 in the app's use, but any month works.
     */
    fun outlook(
        hijriYear: Int,
        hijriMonth: Int,
        lat: Double,
        lng: Double,
        elevationMetres: Double,
        zone: ZoneId,
        today: LocalDate = LocalDate.now(zone),
    ): Outlook {
        val approximate = runCatching {
            LocalDate.from(HijrahDate.of(hijriYear, hijriMonth, 1))
        }.getOrDefault(today)
        // The calculated month start can be a day or two off the conjunction, so search
        // for the new moon in a window around it rather than trusting the calendar.
        val conjunction = nextConjunction(approximate.minusDays(4).atTime(12, 0).toInstant(java.time.ZoneOffset.UTC))
            ?: approximate.atTime(12, 0).toInstant(java.time.ZoneOffset.UTC)
        val conjDate = conjunction.atZone(zone).toLocalDate()

        val evenings = ArrayList<Evening>()
        var date = conjDate.minusDays(1)
        while (date.isBefore(conjDate.plusDays(6))) {
            val e = evening(lat, lng, elevationMetres, date, zone)
            if (e.sunset != null && !e.conjunctionAfterSunset) evenings.add(e)
            date = date.plusDays(1)
        }

        val byEye = evenings.firstOrNull { it.zone.byEye }
        val withAid = evenings.firstOrNull { it.zone.withAid }

        return Outlook(
            hijriYear = hijriYear,
            hijriMonth = hijriMonth,
            code = monthCodes[hijriMonth] ?: "%02d".format(hijriMonth),
            title = monthTitles[hijriMonth] ?: "Month $hijriMonth",
            conjunction = conjunction,
            evenings = evenings,
            firstDay = byEye?.date?.plusDays(1),
            sightingEvening = byEye?.date,
            firstDayWithAid = withAid?.date?.plusDays(1),
        )
    }

    /**
     * Eid al-Adha is 10 Dhul Hijjah, so it sits nine days after the first of Dhul
     * Hijjah, not ten: day one already counts as the first day of the month.
     */
    fun eidAlAdha(firstOfDhulHijjah: LocalDate): LocalDate = firstOfDhulHijjah.plusDays(9)

    /** Distance in degrees between two angles, for tests and diagnostics. */
    fun angleGap(a: Double, b: Double): Double = abs(a - b)

    /** Solar elevation used by the refraction correction, exposed for diagnostics. */
    fun refractionLift(altitude: Double): Double {
        if (altitude > 89.0) return 0.0
        val te = Math.tan(Math.toRadians(altitude))
        val arcSec = if (altitude > 15.0) 58.1 / te - 0.07 / te.pow(3) + 0.000086 / te.pow(5)
        else if (altitude > -0.575) 1735.0 + altitude * (-518.2 + altitude * (103.4 + altitude * (-12.79 + altitude * 0.711)))
        else -20.774 / te
        return arcSec / 3600.0
    }

    @Suppress("unused")
    private fun degrees(x: Double): Double = Math.toDegrees(acos(cos(Math.toRadians(x))))
}
