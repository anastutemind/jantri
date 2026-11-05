package com.anaskhan.jantri.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anaskhan.jantri.core.PrayerComputer
import com.anaskhan.jantri.core.Ramadan
import com.anaskhan.jantri.core.Settings
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val rowDate = DateTimeFormatter.ofPattern("d MMM")

/** The printed sheet, rebuilt for the real sighting date and for this exact location. */
@Composable
fun TableScreen(settings: Settings, onOpenSettings: () -> Unit, todayOverride: LocalDate? = null) {
    val context = LocalContext.current
    val today = todayOverride ?: LocalDate.now(settings.zone)
    val month = Ramadan.monthOf(today, settings)
    val days = (0 until month.days).map { i ->
        val date = month.start.plusDays(i.toLong())
        Triple(i + 1, date, PrayerComputer.compute(date, settings))
    }
    val todayDay = Ramadan.dayOf(today, month.start)

    Column(Modifier.fillMaxWidth()) {
        SectionCard(
            title = "Ramadan ${month.year} AH",
            subtitle = "Day 1: ${month.start.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"))} · ${settings.placeName}",
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onOpenSettings) { Text("Change start date") }
                TextButton(onClick = {
                    val text = buildString {
                        appendLine("RAMADAN ${month.year} AH · ${settings.placeName}")
                        appendLine("Day 1: ${month.start}")
                        appendLine("Day | Date | Sehri | Iftar")
                        days.forEach { (d, date, t) ->
                            appendLine(
                                "$d | ${date.format(rowDate)} | " +
                                    PrayerComputer.fmt(PrayerComputer.sehriEnd(t, settings), settings.use24Hour) + " | " +
                                    PrayerComputer.fmt(PrayerComputer.iftar(t, settings), settings.use24Hour)
                            )
                        }
                        appendLine("Calculated on-device with Jantri (Fajr ${settings.fajrAngle}°, " +
                            "Isha ${settings.ishaAngle ?: 90}°). Moon sighting may shift the start by a day.")
                    }
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Ramadan timetable · ${settings.placeName}")
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(send, "Share timetable"))
                }) { Text("Share") }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            HeaderCell("DAY", 0.9f)
            HeaderCell("DATE", 1.2f)
            HeaderCell("SEHRI", 1f)
            HeaderCell("IFTAR", 1f)
        }
        Spacer(Modifier.height(4.dp))

        LazyColumn(Modifier.fillMaxWidth()) {
            items(days) { (dayNo, date, times) ->
                val isToday = todayDay == dayNo
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when {
                                isToday -> JantriGold.copy(alpha = 0.22f)
                                dayNo % 2 == 0 -> MaterialTheme.colorScheme.surface
                                else -> MaterialTheme.colorScheme.background
                            }
                        )
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(0.9f)) {
                        Text(
                            dayNo.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Box(Modifier.weight(1.2f)) {
                        Column {
                            Text(
                                date.format(rowDate),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        Text(
                            PrayerComputer.fmt(PrayerComputer.sehriEnd(times, settings), settings.use24Hour),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        Text(
                            PrayerComputer.fmt(PrayerComputer.iftar(times, settings), settings.use24Hour),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.HeaderCell(text: String, weight: Float) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.weight(weight),
    )
}
