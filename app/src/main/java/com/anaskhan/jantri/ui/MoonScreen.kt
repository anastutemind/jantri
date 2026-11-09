package com.anaskhan.jantri.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anaskhan.jantri.core.Crescent
import com.anaskhan.jantri.core.MoonMaps
import com.anaskhan.jantri.core.Ramadan
import com.anaskhan.jantri.core.Settings
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter

private val dayName = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")
private val clockTime = DateTimeFormatter.ofPattern("h:mm a")

// The same four colours the bundled maps are drawn in.
private val BandA = Color(0xFF1E7A3C)
private val BandB = Color(0xFFC2185B)
private val BandC = Color(0xFF1565C0)
private val BandD = Color(0xFFC62828)
private val BandNone = Color(0xFF8A8A80)

internal fun bandColor(zone: Crescent.Zone): Color = when (zone) {
    Crescent.Zone.A -> BandA
    Crescent.Zone.B -> BandB
    Crescent.Zone.C -> BandC
    Crescent.Zone.D -> BandD
    Crescent.Zone.NONE -> BandNone
}

private data class Event(val month: Int, val code: String, val label: String, val dayInMonth: Int)

private val events = listOf(
    Event(9, "rmd", "Ramadan", 1),
    Event(10, "shw", "Eid al-Fitr", 1),
    Event(12, "zhj", "Eid al-Adha", 10),
)

@Composable
fun MoonScreen(
    settings: Settings,
    today: LocalDate = LocalDate.now(settings.zone),
    onUseRamadanStart: (LocalDate) -> Unit = {},
) {
    val context = LocalContext.current
    val zone = settings.zone
    val hijriYear = Ramadan.hijriYearOf(today)
    var eventIndex by remember { mutableIntStateOf(0) }

    val outlooks = remember(hijriYear, settings.lat, settings.lng, settings.elevationMetres, settings.zoneId) {
        events.map { ev ->
            Crescent.outlook(
                hijriYear = hijriYear,
                hijriMonth = ev.month,
                lat = settings.lat,
                lng = settings.lng,
                elevationMetres = settings.elevationMetres.toDouble(),
                zone = zone,
                today = today,
            )
        }
    }

    val event = events[eventIndex]
    val outlook = outlooks[eventIndex]
    val firstOfMonth = outlook.firstDay
    val festivalDate = when (event.month) {
        12 -> firstOfMonth?.let { Crescent.eidAlAdha(it) }
        else -> firstOfMonth
    }
    val hijriLabel = "${event.dayInMonth} ${monthName(event.month)}"

    val frames = remember(hijriYear, event.code) { MoonMaps.frames(context, hijriYear, event.code) }
    val defaultIndex = remember(eventIndex, hijriYear, frames.size) {
        val sighting = outlook.sightingEvening
        val inFrames = frames.indexOfFirst { it.evening == sighting }
        if (inFrames >= 0) inFrames else {
            val inEvenings = outlook.evenings.indexOfFirst { it.date == sighting }
            if (inEvenings >= 0) inEvenings else 0
        }
    }
    var frameIndex by remember(eventIndex, hijriYear, defaultIndex) { mutableIntStateOf(defaultIndex) }
    val selectedEvening = frames.getOrNull(frameIndex)?.evening
        ?: outlook.evenings.getOrNull(frameIndex)?.date

    val verdict = remember(selectedEvening, settings.lat, settings.lng, settings.zoneId) {
        selectedEvening?.let {
            Crescent.evening(settings.lat, settings.lng, settings.elevationMetres.toDouble(), it, zone)
        } ?: outlook.evenings.firstOrNull()
    }

    Column(Modifier.fillMaxWidth()) {
        SectionCard(
            title = "Moon sighting",
            subtitle = "What the sky does over ${settings.placeName} on the evening before this month starts, and where the crescent can be seen at all.",
        ) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                events.forEachIndexed { index, ev ->
                    FilterChip(
                        selected = index == eventIndex,
                        onClick = { eventIndex = index },
                        label = { Text(ev.label) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "$hijriLabel $hijriYear AH",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                festivalDate?.format(dayName) ?: "No sighting evening at this latitude",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Chip("Expected", color = MaterialTheme.colorScheme.secondaryContainer, textColor = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.height(6.dp))
            Text(
                "Calculated from the crescent. Not an announcement: keep it as expected until the local committee speaks.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val ummAlQura = runCatching {
                LocalDate.from(HijrahDate.of(hijriYear, event.month, event.dayInMonth))
            }.getOrNull()
            if (ummAlQura != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Umm al-Qura calendar says ${ummAlQura.format(dayName)}" +
                        if (festivalDate != null && festivalDate != ummAlQura) " (a day apart)" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (frames.isNotEmpty()) {
            SectionCard(title = "Where it can be seen") {
                val frame = frames[frameIndex.coerceIn(0, frames.lastIndex)]
                val bitmap = remember(frame.file) {
                    runCatching {
                        context.assets.open("moon/${frame.file}").use { BitmapFactory.decodeStream(it) }
                    }.getOrNull()
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Crescent visibility for the evening of ${frame.evening}",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                    )
                } else {
                    Text("Map asset missing: ${frame.file}", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Evening of ${frame.evening.format(dayName)}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    if (frames.size > 1) {
                        TextButton(onClick = { frameIndex = (frameIndex - 1 + frames.size) % frames.size }) { Text("Prev") }
                        Text(
                            "${frameIndex + 1}/${frames.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = { frameIndex = (frameIndex + 1) % frames.size }) { Text("Next") }
                    }
                }
                val shares = frame.shares
                if (shares.isNotEmpty()) {
                    val a = shares["A"] ?: 0.0
                    val b = shares["B"] ?: 0.0
                    val c = shares["C"] ?: 0.0
                    val d = shares["D"] ?: 0.0
                    Text(
                        "By this criterion the crescent is easy to see from %.0f%% of the globe, marginal from %.0f%%, optical aid only from %.0f%%, and impossible from %.0f%%.".format(a, b, c, d),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(10.dp))
                BandRow(BandA, "A", "Easily visible with the naked eye")
                BandRow(BandB, "B", "Visible with optical aid, and possibly then by eye")
                BandRow(BandC, "C", "Binoculars or telescope only")
                BandRow(BandD, "D", "Not visible, even with optical aid")
                Spacer(Modifier.height(10.dp))
                Text(
                    "Odeh criterion, drawn from Astronomy Engine. No image from any moonsighting site is used.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        verdict?.let { v ->
            SectionCard(title = "From ${settings.placeName}") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bandColor(v.zone)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(v.zone.letter, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(verdictHeadline(v), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Evening of ${v.date.format(dayName)}" +
                                (v.sunset?.let { ", sunset ${it.format(clockTime)}" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Stat("Moon height at sunset", "%.1f°".format(v.moonAltitude))
                Stat("Arc of vision (ARCV)", "%.1f°".format(v.arcv))
                Stat("Distance from the sun", "%.1f°".format(v.elongation))
                Stat("Crescent width", "%.2f′".format(v.widthArcMin))
                Stat("Odeh V", "%.2f".format(v.v))
                Spacer(Modifier.height(8.dp))
                Text(
                    verdictAdvice(v),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (event.month == 9 && firstOfMonth != null) {
            val already = settings.ramadanStartOverride == firstOfMonth.toString()
            SectionCard(title = "Use this date") {
                Text(
                    if (already) "Ramadan already starts on ${firstOfMonth.format(dayName)} in the app."
                    else "Set ${firstOfMonth.format(dayName)} as Ramadan day 1, and every row, countdown and alert follows it until the announcement changes it.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onUseRamadanStart(firstOfMonth) },
                    enabled = !already,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (already) "Already day 1" else "Make this Ramadan day 1")
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun BandRow(color: Color, letter: String, text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        // Legend swatches stay at full strength: the map shows the same hue at 45% over
        // the basemap, and a washed chip would make four similar tints hard to tell apart
        // in a key this small.
        Box(Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).background(color))
        Spacer(Modifier.width(10.dp))
        Text(letter, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(18.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

private fun verdictHeadline(v: Crescent.Evening): String = when {
    v.moonSetsFirst -> "The moon sets before the sun"
    v.conjunctionAfterSunset -> "The new moon has not happened yet"
    else -> when (v.zone) {
        Crescent.Zone.A -> "Plainly visible"
        Crescent.Zone.B -> "Visible, but hard"
        Crescent.Zone.C -> "Binoculars or a telescope"
        Crescent.Zone.D -> "Not visible"
        Crescent.Zone.NONE -> "Nothing to see"
    }
}

private fun verdictAdvice(v: Crescent.Evening): String = when {
    v.moonSetsFirst -> "None of the month's first evenings can be seen from here, so the local sighting will land a day later than a place further west."
    v.conjunctionAfterSunset -> "This evening is still before the new moon, so no crescent exists yet."
    v.zone == Crescent.Zone.A -> "Anyone with a clear western horizon should find it without help."
    v.zone == Crescent.Zone.B -> "Look with binoculars first; with a very clear sky an experienced eye can find it."
    v.zone == Crescent.Zone.C -> "Only with binoculars or a telescope, and only with a clean horizon."
    else -> "No sighting is possible here this evening, even with optical aid."
}

private fun monthName(hijriMonth: Int): String = listOf(
    "Muharram", "Safar", "Rabi al-Awwal", "Rabi al-Thani", "Jumada al-Awwal",
    "Jumada al-Thani", "Rajab", "Shaban", "Ramadan", "Shawwal", "Dhul Qadah", "Dhul Hijjah",
).getOrElse(hijriMonth - 1) { "Month $hijriMonth" }
