package com.anaskhan.jantri.core

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Everything the user can change. Defaults are tuned for India: Karachi angles (18/18,
 * the convention Indian jantri sheets follow), Hanafi Asr, and a two minute sehri safety
 * margin that matches how most printed sheets round.
 */
data class Settings(
    val placeName: String = "Bhopal, Madhya Pradesh",
    val lat: Double = 23.2547,
    val lng: Double = 77.4029,
    val elevationMetres: Int = 0,
    val zoneId: String = "Asia/Kolkata",

    val methodId: String = "karachi",
    /** Custom overrides. Null means "use the method's value". */
    val customFajrAngle: Double? = null,
    val customIshaAngle: Double? = null,
    val asr: AsrJuristic = AsrJuristic.HANAFI,
    val highLatitudeRule: HighLatitudeRule = HighLatitudeRule.NONE,

    /** Per-prayer minute corrections, the same idea as "adjustment" in any salah app. */
    val adjFajr: Int = 0,
    val adjSunrise: Int = 0,
    val adjDhuhr: Int = 0,
    val adjAsr: Int = 0,
    val adjMaghrib: Int = 0,
    val adjIsha: Int = 0,

    /** Sehri stops this many minutes BEFORE Fajr (safety margin, like a printed sheet). */
    val sehriSafetyMin: Int = 2,
    /** Iftar is this many minutes AFTER sunset (ihtiyat). */
    val iftarSafetyMin: Int = 1,

    /** Explicit Ramadan day 1. Null = computed from the Umm al-Qura Hijri calendar. */
    val ramadanStartOverride: String? = null,

    /** Notifications */
    val notifySehriLeadMinutes: Int = 15,
    val notifySehriLeadEnabled: Boolean = true,
    val notifySehriEndEnabled: Boolean = true,
    val notifyIftarLeadMinutes: Int = 10,
    val notifyIftarLeadEnabled: Boolean = true,
    val notifyIftarEnabled: Boolean = true,
    val notifyAdhanEnabled: Boolean = false,

    val use24Hour: Boolean = false,
) {
    val method: Method get() = Methods.byId(methodId)
    val fajrAngle: Double get() = customFajrAngle ?: method.fajrAngle
    val ishaAngle: Double? get() = customIshaAngle ?: method.ishaAngle
    val zone: ZoneId get() = runCatching { ZoneId.of(zoneId) }.getOrDefault(ZoneId.of("Asia/Kolkata"))
}

/** Times for one calendar day, as minutes after local midnight. */
data class DayTimes(
    val date: LocalDate,
    val fajr: Int,
    val sunrise: Int,
    val dhuhr: Int,
    val asr: Int,
    val maghrib: Int,
    val isha: Int,
    val fajrAngle: Double,
    val resolvedFajr: Boolean = true,
    val resolvedIsha: Boolean = true,
)

object PrayerComputer {

    private fun fixHour(h: Double): Double = Solar.fixHour(h)

    fun compute(date: LocalDate, s: Settings): DayTimes {
        val zone = s.zone
        val offsetHours = zone.rules.getOffset(date.atTime(12, 0)).totalSeconds / 3600.0
        val lat = s.lat
        val lng = s.lng

        val jd = Solar.julian(date.year, date.monthValue, date.dayOfMonth)
        val riseSetAngle = 0.833 + Solar.elevationAngle(s.elevationMetres.toDouble())

        fun sunAngleTime(angle: Double, ccw: Boolean, guess: Double): Double? {
            val jdGuess = jd - lng / (15.0 * 24.0) + (guess - offsetHours) / 24.0
            val (decl, eqt) = Solar.sunPosition(jdGuess)
            val noon = fixHour(12.0 - eqt)
            val numerator = -sinDeg(angle) - sinDeg(decl) * sinDeg(lat)
            val denominator = cosDeg(decl) * cosDeg(lat)
            val x = numerator / denominator
            if (x < -1.0 || x > 1.0) return null
            val t = Math.toDegrees(kotlin.math.acos(x.toDouble())) / 15.0
            return noon + if (ccw) -t else t
        }

        fun asrAngle(guess: Double): Double {
            val jdGuess = jd - lng / (15.0 * 24.0) + (guess - offsetHours) / 24.0
            val (decl, _) = Solar.sunPosition(jdGuess)
            val angle = -Math.toDegrees(kotlin.math.atan(1.0 / (s.asr.factor + tanDeg(Math.abs(lat - decl)))))
            return angle
        }

        fun toLocal(hours: Double): Double = fixHour(hours + offsetHours - lng / 15.0)

        val (_, eqtNoon) = Solar.sunPosition(jd - lng / (15.0 * 24.0))
        val noonLocal = toLocal(fixHour(12.0 - eqtNoon))

        val sunriseRaw = sunAngleTime(riseSetAngle, true, 5.0)
        val maghribRaw = sunAngleTime(riseSetAngle, false, 18.0)
        var fajrRaw = sunAngleTime(s.fajrAngle, true, 5.0)
        var ishaRaw = s.ishaAngle?.let { sunAngleTime(it, false, 18.0) }
        val asrRaw = sunAngleTime(asrAngle(13.0), false, 13.0)

        // High latitude fallbacks, only relevant far from the equator.
        val midnight = toLocal(fixHour(12.0 + 12.0))
        if (sunriseRaw != null && maghribRaw != null) {
            val sunriseL = toLocal(sunriseRaw)
            val maghribL = toLocal(maghribRaw)
            val nightLength = fixHour(sunriseL - maghribL)
            if (s.highLatitudeRule != HighLatitudeRule.NONE) {
                val portion = when (s.highLatitudeRule) {
                    HighLatitudeRule.ONE_SEVENTH -> nightLength / 7.0
                    else -> nightLength / 2.0
                }
                if (fajrRaw == null) fajrRaw = sunriseL - portion
                if (ishaRaw == null && s.ishaAngle != null) ishaRaw = maghribL + portion
            }
            // Last resort so the app never shows a blank where a time belongs.
            if (fajrRaw == null) fajrRaw = sunriseL - Math.min(nightLength / 7.0, 1.5)
            if (ishaRaw == null && s.ishaAngle != null) ishaRaw = maghribL + Math.min(nightLength / 7.0, 1.5)
        }
        @Suppress("UNUSED_VARIABLE") val unusedMidnight = midnight

        val sunrise = toLocal(sunriseRaw ?: noonLocal)
        val maghrib = toLocal(maghribRaw ?: noonLocal)
        val fajr = toLocal(fajrRaw ?: sunrise)
        val asr = toLocal(asrRaw ?: noonLocal)
        val isha = when {
            s.ishaAngle != null -> toLocal(ishaRaw ?: maghrib)
            else -> maghrib + (s.method.ishaMinutes ?: 90) / 60.0
        }
        val dhuhr = noonLocal

        fun minutesOf(hours: Double, adj: Int): Int = Math.round(hours * 60.0).toInt() + adj

        return DayTimes(
            date = date,
            fajr = minutesOf(fajr, s.adjFajr),
            sunrise = minutesOf(sunrise, s.adjSunrise),
            dhuhr = minutesOf(dhuhr, s.adjDhuhr),
            asr = minutesOf(asr, s.adjAsr),
            maghrib = minutesOf(maghrib, s.adjMaghrib),
            isha = minutesOf(isha, s.adjIsha),
            fajrAngle = s.fajrAngle,
            resolvedFajr = fajrRaw != null,
            resolvedIsha = ishaRaw != null || s.ishaAngle == null,
        )
    }

    private fun sinDeg(x: Double) = kotlin.math.sin(Math.toRadians(x))
    private fun cosDeg(x: Double) = kotlin.math.cos(Math.toRadians(x))
    private fun tanDeg(x: Double) = kotlin.math.tan(Math.toRadians(x))

    /** Sehri ends at Fajr minus the safety margin. */
    fun sehriEnd(t: DayTimes, s: Settings): Int = t.fajr - s.sehriSafetyMin

    /** Iftar is Maghrib plus the precaution margin. */
    fun iftar(t: DayTimes, s: Settings): Int = t.maghrib + s.iftarSafetyMin

    fun at(date: LocalDate, minutesOfDay: Int, zone: ZoneId): ZonedDateTime =
        date.atStartOfDay(zone).plusMinutes(minutesOfDay.toLong())

    fun fmt(minutesOfDay: Int, use24h: Boolean): String {
        val m = ((minutesOfDay % 1440) + 1440) % 1440
        val h = m / 60
        val min = m % 60
        return if (use24h) {
            String.format("%02d:%02d", h, min)
        } else {
            val h12 = when {
                h == 0 -> 12
                h > 12 -> h - 12
                else -> h
            }
            String.format("%d:%02d %s", h12, min, if (h < 12) "AM" else "PM")
        }
    }
}
