package com.anaskhan.jantri.core

/** How Asr is computed: shadow equal to the object (Shafi'i) or twice it (Hanafi). */
enum class AsrJuristic(val factor: Double, val label: String) {
    SHAFI(1.0, "Shafi'i, Maliki, Hanbali (shadow = 1x)"),
    HANAFI(2.0, "Hanafi (shadow = 2x)"),
}

/**
 * A twilight convention. Fajr and Isha are defined by how far the sun sits below the
 * horizon, and the schools/authorities disagree on that angle, which is exactly why two
 * mosques in one city can print timetables a few minutes apart.
 */
data class Method(
    val id: String,
    val label: String,
    val fajrAngle: Double,
    val ishaAngle: Double?,
    val ishaMinutes: Int? = null,
    val maghribAngle: Double? = null,
    val note: String = "",
)

object Methods {
    val ALL: List<Method> = listOf(
        Method(
            "karachi", "Karachi (India, Pakistan, Bangladesh)", 18.0, 18.0,
            note = "University of Islamic Sciences, Karachi. 18 degrees for both Fajr and Isha. " +
                "This is the convention most Indian jantri sheets are built on, and the one " +
                "endorsed by Mufti Muhammad Shafi and the ulama of Pakistan and India.",
        ),
        Method(
            "mwl", "Muslim World League", 18.0, 17.0,
            note = "Europe, Far East and parts of the US. Fajr 18, Isha 17 degrees.",
        ),
        Method(
            "isna", "ISNA (North America)", 15.0, 15.0,
            note = "Islamic Society of North America. 15 degrees both; gives a later Fajr " +
                "(shorter fast) and can be 10 to 15 minutes different from 18 degrees in India.",
        ),
        Method(
            "egypt", "Egyptian General Authority", 19.5, 17.5,
            note = "Egypt, Syria, Lebanon, Malaysia. Fajr 19.5 is the earliest of the common " +
                "conventions, so it gives the longest fast.",
        ),
        Method(
            "makkah", "Umm al-Qura, Makkah", 18.5, null, ishaMinutes = 90,
            note = "Arabian Peninsula. Fajr 18.5 degrees, Isha fixed at 90 minutes after " +
                "Maghrib (120 minutes in Ramadan).",
        ),
        Method(
            "jafari", "Shia Ithna Ashari (Jafari)", 16.0, 14.0, maghribAngle = 4.0,
            note = "Leva Institute, Qom. Isha 14 degrees, Maghrib 4 degrees after sunset.",
        ),
        Method(
            "tehran", "Tehran (Univ. of Geophysics)", 17.7, 14.0, maghribAngle = 4.5,
            note = "Iran and some Shia communities. Maghrib 4.5 degrees.",
        ),
    )

    fun byId(id: String): Method = ALL.firstOrNull { it.id == id } ?: ALL.first()
}

/** What to do when the sun never reaches the twilight angle (high latitudes). */
enum class HighLatitudeRule(val label: String) {
    NONE("None (leave blank)"),
    MIDDLE_OF_NIGHT("Middle of the night"),
    ONE_SEVENTH("One seventh of the night"),
}
