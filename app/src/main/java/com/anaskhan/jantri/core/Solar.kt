package com.anaskhan.jantri.core

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * Solar position and prayer-time mathematics.
 *
 * Follows the classic PrayTimes formulation: Julian-day to solar declination and
 * equation of time, then hour angles for a given solar altitude. Everything here is
 * pure arithmetic so it can be unit-tested off-device against printed timetables.
 *
 * All computed times are returned as fractional hours in the local civil time of the
 * supplied zone offset (hours east of UTC).
 */
object Solar {

    private const val DTR = Math.PI / 180.0
    private const val RTD = 180.0 / Math.PI

    private fun fix(a: Double, b: Double): Double = a - b * floor(a / b)
    fun fixAngle(a: Double): Double = fix(a, 360.0)
    fun fixHour(a: Double): Double = fix(a, 24.0)

    private fun dsin(a: Double) = sin(a * DTR)
    private fun dcos(a: Double) = cos(a * DTR)
    private fun dtan(a: Double) = tan(a * DTR)
    private fun darcsin(x: Double) = RTD * asin(x)
    private fun darccos(x: Double) = RTD * acos(x)
    private fun darctan2(y: Double, x: Double) = RTD * atan2(y, x)
    private fun darccot(x: Double) = RTD * atan(1.0 / x)

    /** Julian date for a calendar day (Gregorian), at 0h UT. */
    fun julian(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    /** Sun declination (degrees) and equation of time (hours) for a Julian date. */
    fun sunPosition(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g))
        val e = 23.439 - 0.00000036 * d
        val decl = darcsin(dsin(e) * dsin(l))
        var ra = darctan2(dcos(e) * dsin(l), dcos(l)) / 15.0
        ra = fixHour(ra)
        val eqt = q / 15.0 - ra
        return decl to eqt
    }

    /** Extra horizon depression (degrees) caused by observer elevation in metres. */
    fun elevationAngle(metres: Double): Double {
        if (metres <= 0.0) return 0.0
        return RTD * acos(6371000.0 / (6371000.0 + metres))
    }
}
