package com.anaskhan.jantri

import com.anaskhan.jantri.core.Crescent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

/**
 * The crescent verdict, checked against numbers from an independent Astronomy Engine
 * run (the Node build of the same library), so the Kotlin path cannot drift from the
 * engine that drew the bundled maps.
 */
class CrescentTest {

    private val india = ZoneId.of("Asia/Kolkata")

    // Hyderabad, 7 February 2027: the evening before Ramadan 1448 begins in India.
    private val hyderabadLat = 17.384
    private val hyderabadLng = 78.4564

    // Al-Madinah, same evening.
    private val madinahLat = 21.4225
    private val madinahLng = 39.8262

    private fun near(expected: Double, actual: Double, tolerance: Double, what: String) {
        assertTrue("$what: expected $expected, was $actual", abs(expected - actual) <= tolerance)
    }

    @Test
    fun hyderabadEveningBeforeRamadan1448() {
        val e = Crescent.evening(hyderabadLat, hyderabadLng, 0.0, LocalDate.of(2027, 2, 7), india)
        assertNotNull("sunset must resolve", e.sunset)
        near(7.950, e.moonAltitude, 0.05, "moon altitude")
        near(-0.218, e.sunAltitude, 0.05, "sun altitude")
        near(8.168, e.arcv, 0.05, "ARCV")
        near(9.603, e.elongation, 0.05, "elongation")
        near(2.29, e.v, 0.10, "Odeh V")
        assertEquals(Crescent.Zone.B, e.zone)
    }

    @Test
    fun madinahSameEvening() {
        val e = Crescent.evening(madinahLat, madinahLng, 0.0, LocalDate.of(2027, 2, 7), india)
        near(9.128, e.moonAltitude, 0.05, "moon altitude")
        near(9.346, e.arcv, 0.05, "ARCV")
        near(10.764, e.elongation, 0.05, "elongation")
        near(3.79, e.v, 0.10, "Odeh V")
        assertEquals(Crescent.Zone.B, e.zone)
    }

    @Test
    fun hyderabadOneDayLaterIsPlainlyVisible() {
        val e = Crescent.evening(hyderabadLat, hyderabadLng, 0.0, LocalDate.of(2027, 2, 8), india)
        near(19.088, e.moonAltitude, 0.05, "moon altitude")
        near(20.842, e.elongation, 0.05, "elongation")
        assertEquals(Crescent.Zone.A, e.zone)
    }

    @Test
    fun bandBoundariesFollowOdeh() {
        assertEquals(Crescent.Zone.A, Crescent.zoneOf(5.65))
        assertEquals(Crescent.Zone.B, Crescent.zoneOf(5.64))
        assertEquals(Crescent.Zone.B, Crescent.zoneOf(2.00))
        assertEquals(Crescent.Zone.C, Crescent.zoneOf(1.99))
        assertEquals(Crescent.Zone.C, Crescent.zoneOf(-0.96))
        assertEquals(Crescent.Zone.D, Crescent.zoneOf(-0.97))
    }

    @Test
    fun vValueUsesTheOdehPolynomial() {
        // At exactly the minimum arc of vision for a 2 arcminute crescent V must be zero.
        val width = 2.0
        val minArcv = -0.1018 * width * width * width + 0.7319 * width * width - 6.3226 * width + 7.1651
        near(0.0, Crescent.vValue(minArcv, width), 1e-9, "V at the threshold")
    }

    @Test
    fun instantRoundTrip() {
        val instant = java.time.Instant.parse("2027-02-06T15:55:46Z")
        val back = Crescent.instantOf(Crescent.timeOf(instant))
        assertTrue(abs(back.epochSecond - instant.epochSecond) <= 1)
    }

    /**
     * The three dates the app must get right for 1448, from Hyderabad. These agree with
     * the moonsighting.com prose for that year (crescent visible everywhere on 8
     * February, visible across Asia on 9 March, visible in most of the world on 7 May),
     * which is the point: the app derives the same dates independently, offline.
     */
    @Test
    fun ramadan1448OpeningAndEidDates() {
        val ramadan = Crescent.outlook(1448, 9, hyderabadLat, hyderabadLng, 0.0, india)
        assertEquals(LocalDate.of(2027, 2, 8), ramadan.firstDay)
        assertEquals(LocalDate.of(2027, 2, 7), ramadan.sightingEvening)
        assertEquals(Crescent.Zone.B, ramadan.evenings.first { it.date == LocalDate.of(2027, 2, 7) }.zone)

        val shawwal = Crescent.outlook(1448, 10, hyderabadLat, hyderabadLng, 0.0, india)
        assertEquals(LocalDate.of(2027, 3, 10), shawwal.firstDay)
        assertEquals(Crescent.Zone.D, shawwal.evenings.first { it.date == LocalDate.of(2027, 3, 8) }.zone)

        val dhulHijjah = Crescent.outlook(1448, 12, hyderabadLat, hyderabadLng, 0.0, india)
        assertEquals(LocalDate.of(2027, 5, 8), dhulHijjah.firstDay)
        assertEquals(LocalDate.of(2027, 5, 17), Crescent.eidAlAdha(dhulHijjah.firstDay!!))
    }

    /**
     * The Umm al-Qura calendar built into java.time disagrees with the sky on both Eids
     * of 1448, a day early. The app must show both numbers rather than hide the gap.
     */
    @Test
    fun ummAlQuraIsADayEarlyForBothEidsIn1448() {
        assertEquals(LocalDate.of(2027, 3, 9), LocalDate.from(java.time.chrono.HijrahDate.of(1448, 10, 1)))
        assertEquals(LocalDate.of(2027, 5, 16), LocalDate.from(java.time.chrono.HijrahDate.of(1448, 12, 10)))
        assertEquals(LocalDate.of(2027, 2, 8), LocalDate.from(java.time.chrono.HijrahDate.of(1448, 9, 1)))
    }
}
