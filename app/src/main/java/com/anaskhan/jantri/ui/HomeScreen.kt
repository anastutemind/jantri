package com.anaskhan.jantri.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anaskhan.jantri.core.DayTimes
import com.anaskhan.jantri.core.PrayerComputer
import com.anaskhan.jantri.core.Ramadan
import com.anaskhan.jantri.core.Settings
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val dayName = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")

@Composable
fun HomeScreen(
    settings: Settings,
    onOpenSettings: () -> Unit,
    onOpenTable: () -> Unit,
    clock: () -> LocalDateTime = { LocalDateTime.now(settings.zone) },
    ticking: Boolean = true,
) {
    var now by remember { mutableStateOf(clock().atZone(settings.zone)) }
    LaunchedEffect(Unit) {
        while (ticking) {
            now = clock().atZone(settings.zone)
            delay(1000)
        }
    }
    val today = now.toLocalDate()
    val times = PrayerComputer.compute(today, settings)
    val month = Ramadan.monthOf(today, settings)
    val day = Ramadan.dayOf(today, month.start)
    val sehri = PrayerComputer.sehriEnd(times, settings)
    val iftar = PrayerComputer.iftar(times, settings)
    val zone = settings.zone

    val sehriAt = PrayerComputer.at(today, sehri, zone)
    val iftarAt = PrayerComputer.at(today, iftar, zone)
    val tomorrowSehriAt = PrayerComputer.at(today.plusDays(1), PrayerComputer.sehriEnd(PrayerComputer.compute(today.plusDays(1), settings), settings), zone)

    val nextLabel: String
    val nextAt: java.time.ZonedDateTime
    when {
        now.isBefore(sehriAt) -> { nextLabel = "Sehri ends in"; nextAt = sehriAt }
        now.isBefore(iftarAt) -> { nextLabel = "Iftar in"; nextAt = iftarAt }
        else -> { nextLabel = "Next sehri in"; nextAt = tomorrowSehriAt }
    }
    val totalSeconds = ChronoUnit.SECONDS.between(now, nextAt).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val countdown = if (hours > 0) "%dh %02dm".format(hours, minutes) else "%dm %02ds".format(minutes, seconds)

    val fastStart = sehriAt
    val fastEnd = iftarAt
    val fastProgress = if (now.isAfter(fastStart) && now.isBefore(fastEnd)) {
        val total = ChronoUnit.SECONDS.between(fastStart, fastEnd).toFloat().coerceAtLeast(1f)
        (ChronoUnit.SECONDS.between(fastStart, now).toFloat() / total).coerceIn(0f, 1f)
    } else if (now.isAfter(fastEnd)) 1f else 0f

    Column(Modifier.fillMaxWidth()) {
        HeroCard(
            settings = settings,
            hijri = Ramadan.hijriLabel(today),
            ramadanDay = day,
            sehri = PrayerComputer.fmt(sehri, settings.use24Hour),
            iftar = PrayerComputer.fmt(iftar, settings.use24Hour),
            nextLabel = nextLabel,
            countdown = countdown,
            progress = fastProgress,
            fastLabel = if (day != null) "Ramadan day $day" else "Ramadan ${month.year} AH",
        )

        Spacer(Modifier.height(14.dp))

        SectionCard(title = "Today at a glance") {
            Text(
                today.format(dayName),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(10.dp))
            PrayerLine("Sehri ends (Fajr)", sehri, settings, highlight = true)
            PrayerLine("Sunrise (Shuruq)", times.sunrise, settings)
            PrayerLine("Dhuhr", times.dhuhr, settings)
            PrayerLine("Asr", times.asr, settings)
            PrayerLine("Iftar (Maghrib)", iftar, settings, highlight = true)
            PrayerLine("Isha", times.isha, settings)
            Spacer(Modifier.height(6.dp))
            Text(
                "Fajr ${settings.fajrAngle.format1()}° / Isha ${
                    settings.ishaAngle?.let { "${it.format1()}°" } ?: "${settings.method.ishaMinutes ?: 90} min after maghrib"
                } · ${if (settings.asr == com.anaskhan.jantri.core.AsrJuristic.HANAFI) "Hanafi" else "Shafi'i"} Asr" +
                    (if (settings.sehriSafetyMin != 0 || settings.iftarSafetyMin != 0)
                        " · safety ${settings.sehriSafetyMin}m / +${settings.iftarSafetyMin}m" else ""),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (day == null) {
            val start = month.start
            SectionCard(title = "Before Ramadan") {
                Text(
                    "Day 1 of Ramadan ${month.year} AH falls on ${start.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"))} on the calculated calendar. " +
                        "If the crescent is sighted a day earlier or later, set the real date in Timing.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onOpenSettings) { Text("Set Ramadan start date") }
            }
        }

        SectionCard(title = "This month") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onOpenTable) { Text("Open the jantri table") }
                TextButton(onClick = onOpenSettings) { Text("Adjust timings") }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

private fun Double.format1(): String = if (this % 1.0 == 0.0) this.toInt().toString() else this.toString()

@Composable
private fun PrayerLine(label: String, minutes: Int, settings: Settings, highlight: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (highlight) JantriGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            color = if (highlight) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            PrayerComputer.fmt(minutes, settings.use24Hour),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun HeroCard(
    settings: Settings,
    hijri: String,
    ramadanDay: Int?,
    sehri: String,
    iftar: String,
    nextLabel: String,
    countdown: String,
    progress: Float,
    fastLabel: String,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(JantriGreen, JantriGreenMid))),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Chip(
                    text = fastLabel.uppercase(),
                    color = JantriGold.copy(alpha = 0.18f),
                    textColor = JantriGoldLight,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    settings.placeName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.75f),
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("SEHRI ENDS", style = MaterialTheme.typography.labelSmall, color = JantriGoldLight)
                    Spacer(Modifier.height(4.dp))
                    Text(sehri, fontSize = 30.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
                Box(Modifier.width(1.dp).height(52.dp).background(Color.White.copy(alpha = 0.2f)))
                Column(Modifier.weight(1f).padding(start = 16.dp)) {
                    Text("IFTAR", style = MaterialTheme.typography.labelSmall, color = JantriGoldLight)
                    Spacer(Modifier.height(4.dp))
                    Text(iftar, fontSize = 30.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
            Spacer(Modifier.height(18.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = JantriGoldLight,
                trackColor = Color.White.copy(alpha = 0.18f),
            )
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(nextLabel.uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                    Text(countdown, fontSize = 22.sp, fontWeight = FontWeight.Medium, color = Color.White)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(hijri, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
                    Text(
                        if (ramadanDay != null) "Day $ramadanDay of 30" else "Not in Ramadan",
                        style = MaterialTheme.typography.bodyMedium,
                        color = JantriGoldLight,
                    )
                }
            }
        }
    }
}
