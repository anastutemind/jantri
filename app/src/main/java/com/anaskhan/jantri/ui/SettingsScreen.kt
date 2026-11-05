package com.anaskhan.jantri.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.anaskhan.jantri.core.AsrJuristic
import com.anaskhan.jantri.core.Cities
import com.anaskhan.jantri.core.City
import com.anaskhan.jantri.core.HighLatitudeRule
import com.anaskhan.jantri.core.LocationHelper
import com.anaskhan.jantri.core.Methods
import com.anaskhan.jantri.core.Ramadan
import com.anaskhan.jantri.core.Settings
import com.anaskhan.jantri.notify.Notifications
import com.anaskhan.jantri.notify.Scheduler
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val prettyDate = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")

@Composable
fun SettingsScreen(
    settings: Settings,
    onChange: (Settings) -> Unit,
    onRequestNotifications: () -> Unit,
) {
    val context = LocalContext.current
    var showCities by remember { mutableStateOf(false) }
    var showMethods by remember { mutableStateOf(false) }
    var showAsr by remember { mutableStateOf(false) }
    var showHighLat by remember { mutableStateOf(false) }
    var editLatLng by remember { mutableStateOf(false) }

    val exactOk = Scheduler.canScheduleExact(context)

    Column(Modifier.fillMaxWidth()) {
        SectionCard(title = "Location") {
            SettingRow(
                title = settings.placeName,
                description = "Tap to pick a city",
                value = "Change",
                onClick = { showCities = true },
            )
            SettingRow(
                title = "Coordinates",
                description = "%.4f, %.4f".format(settings.lat, settings.lng),
                value = "Edit",
                onClick = { editLatLng = true },
            )
            TextButton(onClick = {
                LocationHelper.requestFix(context) { loc ->
                    if (loc != null) {
                        onChange(
                            settings.copy(
                                lat = loc.latitude,
                                lng = loc.longitude,
                                placeName = "My location (%.3f, %.3f)".format(loc.latitude, loc.longitude),
                                zoneId = "Asia/Kolkata",
                            )
                        )
                    }
                }
            }) { Text(if (LocationHelper.hasPermission(context)) "Use my current location" else "Use my current location (allow location first)") }
        }

        SectionCard(title = "Calculation method") {
            SettingRow(
                title = settings.method.label,
                description = "Fajr ${settings.fajrAngle}°" + (settings.ishaAngle?.let { " · Isha $it°" } ?: " · Isha ${settings.method.ishaMinutes ?: 90} min after maghrib"),
                value = "Change",
                onClick = { showMethods = true },
            )
            SettingRow(
                title = "Asr",
                value = if (settings.asr == AsrJuristic.HANAFI) "Hanafi" else "Shafi'i",
                onClick = { showAsr = true },
            )
            SettingRow(
                title = "High latitude rule",
                description = "Only matters far from the equator",
                value = settings.highLatitudeRule.label.substringBefore(" ("),
                onClick = { showHighLat = true },
            )
            TextButton(onClick = { showMethods = true }) { Text("Why the angles differ") }
        }

        SectionCard(
            title = "Match your mosque's sheet",
            subtitle = "Printed jantris add a safety margin. Shift sehri earlier and iftar later until the app agrees with the sheet on your wall.",
        ) {
            Stepper("Sehri safety margin", settings.sehriSafetyMin, { onChange(settings.copy(sehriSafetyMin = it)) }, -15..30)
            Stepper("Iftar safety margin", settings.iftarSafetyMin, { onChange(settings.copy(iftarSafetyMin = it)) }, -15..30)
            Spacer(Modifier.height(6.dp))
            Text("Fine adjustments per prayer", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Stepper("Fajr", settings.adjFajr, { onChange(settings.copy(adjFajr = it)) })
            Stepper("Sunrise", settings.adjSunrise, { onChange(settings.copy(adjSunrise = it)) })
            Stepper("Dhuhr", settings.adjDhuhr, { onChange(settings.copy(adjDhuhr = it)) })
            Stepper("Asr", settings.adjAsr, { onChange(settings.copy(adjAsr = it)) })
            Stepper("Maghrib", settings.adjMaghrib, { onChange(settings.copy(adjMaghrib = it)) })
            Stepper("Isha", settings.adjIsha, { onChange(settings.copy(adjIsha = it)) })
            ToggleRow("24 hour clock", null, settings.use24Hour) { onChange(settings.copy(use24Hour = it)) }
        }

        SectionCard(
            title = "Ramadan start",
            subtitle = "Sighting decides day 1. The calculated date can be a day out, so set what your masjid announced.",
        ) {
            val calculated = Ramadan.startFor(LocalDate.now(settings.zone))
            val current = settings.ramadanStartOverride?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: calculated
            val offset = java.time.temporal.ChronoUnit.DAYS.between(calculated, current).toInt()
            Text(current.format(prettyDate), style = MaterialTheme.typography.titleMedium)
            Stepper(
                "Shift from calculated date",
                offset,
                { newOffset ->
                    val newDate = calculated.plusDays(newOffset.toLong())
                    onChange(
                        settings.copy(
                            ramadanStartOverride = if (newOffset == 0) null else newDate.toString(),
                        )
                    )
                },
                range = -3..3,
                suffix = "day",
            )
            Text(
                "Calculated (Umm al-Qura): ${calculated.format(prettyDate)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard(title = "Alerts") {
            ToggleRow("Sehri warning", "Before sehri ends", settings.notifySehriLeadEnabled) {
                onChange(settings.copy(notifySehriLeadEnabled = it))
            }
            if (settings.notifySehriLeadEnabled) {
                Stepper("Warn me", settings.notifySehriLeadMinutes, { onChange(settings.copy(notifySehriLeadMinutes = it)) }, 5..120)
            }
            ToggleRow("Sehri ends", "At Fajr", settings.notifySehriEndEnabled) {
                onChange(settings.copy(notifySehriEndEnabled = it))
            }
            ToggleRow("Iftar warning", "Before maghrib", settings.notifyIftarLeadEnabled) {
                onChange(settings.copy(notifyIftarLeadEnabled = it))
            }
            if (settings.notifyIftarLeadEnabled) {
                Stepper("Warn me", settings.notifyIftarLeadMinutes, { onChange(settings.copy(notifyIftarLeadMinutes = it)) }, 5..120)
            }
            ToggleRow("Iftar time", "At maghrib", settings.notifyIftarEnabled) {
                onChange(settings.copy(notifyIftarEnabled = it))
            }
            Spacer(Modifier.height(6.dp))
            SettingRow(
                title = "Exact alarms",
                description = if (exactOk) "Allowed, alerts fire on time even in Doze" else "Not allowed yet, Android may delay alerts",
                value = if (exactOk) "On" else "Open",
                onClick = {
                    if (!exactOk && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val i = Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + context.packageName))
                        runCatching { context.startActivity(i) }
                    }
                },
            )
            SettingRow(
                title = "Notifications",
                description = "Android 13+ needs this allowed separately",
                value = "Open",
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) onRequestNotifications()
                    else {
                        val i = Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)
                        runCatching { context.startActivity(i) }
                    }
                },
            )
            TextButton(onClick = {
                Notifications.ensureChannels(context)
                Notifications.post(
                    context, 991, Notifications.CH_IFTAR,
                    "Test alert from Jantri",
                    "Alerts are working. This is how iftar and sehri reminders will look.",
                )
            }) { Text("Send a test alert") }
        }

        SectionCard(title = "About") {
            Text(
                "Jantri calculates sehri and iftar from the sun's position at your coordinates, the same way a printed timetable is prepared, and keeps the two controls that differ between mosques: the twilight angle and the safety margin. " +
                    "All calculation happens on the phone; nothing is sent anywhere.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Data: city coordinates from GeoNames (CC BY 4.0).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(20.dp))
    }

    if (showCities) {
        CityPickerDialog(
            onDismiss = { showCities = false },
            onPick = { city ->
                onChange(
                    settings.copy(
                        placeName = city.label,
                        lat = city.lat,
                        lng = city.lng,
                        elevationMetres = city.elevation,
                        zoneId = city.zone,
                    )
                )
                showCities = false
            },
        )
    }

    if (showMethods) {
        ChoiceDialog(
            title = "Calculation method",
            items = Methods.ALL.map { it.id to it.label },
            onDismiss = { showMethods = false },
            onPick = { id ->
                onChange(settings.copy(methodId = id, customFajrAngle = null, customIshaAngle = null))
                showMethods = false
            },
        )
    }

    if (showAsr) {
        ChoiceDialog(
            title = "Asr calculation",
            items = AsrJuristic.entries.map { it.name to it.label },
            onDismiss = { showAsr = false },
            onPick = { id ->
                onChange(settings.copy(asr = AsrJuristic.valueOf(id)))
                showAsr = false
            },
        )
    }

    if (showHighLat) {
        ChoiceDialog(
            title = "High latitude rule",
            items = HighLatitudeRule.entries.map { it.name to it.label },
            onDismiss = { showHighLat = false },
            onPick = { id ->
                onChange(settings.copy(highLatitudeRule = HighLatitudeRule.valueOf(id)))
                showHighLat = false
            },
        )
    }

    if (editLatLng) {
        CoordinatesDialog(
            lat = settings.lat,
            lng = settings.lng,
            onDismiss = { editLatLng = false },
            onSave = { lat, lng ->
                onChange(settings.copy(lat = lat, lng = lng, placeName = "Custom (%.3f, %.3f)".format(lat, lng)))
                editLatLng = false
            },
        )
    }
}

@Composable
private fun ChoiceDialog(
    title: String,
    items: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.heightIn(max = 420.dp)) {
                LazyColumn {
                    items(items) { (id, label) ->
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(id) }
                                .padding(vertical = 12.dp),
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun CityPickerDialog(onDismiss: () -> Unit, onPick: (City) -> Unit) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val results = remember(query) { Cities.search(context, query, 60) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("City") },
        text = {
            Column(Modifier.heightIn(max = 460.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    label = { Text("Search a town or city") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn {
                    items(results) { city ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onPick(city) }
                                .padding(vertical = 10.dp),
                        ) {
                            Text(city.name, style = MaterialTheme.typography.bodyLarge)
                            Text(city.state, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun CoordinatesDialog(
    lat: Double,
    lng: Double,
    onDismiss: () -> Unit,
    onSave: (Double, Double) -> Unit,
) {
    var latText by remember { mutableStateOf(lat.toString()) }
    var lngText by remember { mutableStateOf(lng.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Coordinates") },
        text = {
            Column {
                OutlinedTextField(
                    value = latText,
                    onValueChange = { latText = it },
                    label = { Text("Latitude") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = lngText,
                    onValueChange = { lngText = it },
                    label = { Text("Longitude") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val la = latText.toDoubleOrNull()
                val lo = lngText.toDoubleOrNull()
                if (la != null && lo != null && la in -90.0..90.0 && lo in -180.0..180.0) onSave(la, lo)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
