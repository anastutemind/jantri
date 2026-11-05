package com.anaskhan.jantri.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.anaskhan.jantri.core.PrayerComputer
import com.anaskhan.jantri.core.Ramadan
import com.anaskhan.jantri.core.Settings
import com.anaskhan.jantri.core.Store
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Arms the sehri and iftar alerts.
 *
 * Exact alarms are set one by one rather than relying on a background service, so the
 * alerts fire when the phone is idle and in Doze. A rolling window of days is armed each
 * time the app opens, after boot, and after every alert that fires.
 */
object Scheduler {

    private const val HORIZON_DAYS = 45

    const val EXTRA_KIND = "kind"
    const val EXTRA_MINUTES = "minutes"

    const val KIND_SEHRI_LEAD = "sehri_lead"
    const val KIND_SEHRI_END = "sehri_end"
    const val KIND_IFTAR_LEAD = "iftar_lead"
    const val KIND_IFTAR = "iftar"

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = context.getSystemService(AlarmManager::class.java) ?: return false
        return am.canScheduleExactAlarms()
    }

    /** One armed alert: when it fires and what it says. */
    data class Planned(val at: LocalDateTime, val kind: String, val minutes: Int, val requestCode: Int)

    /** Pure planning step: every alert that should fire in the window, earliest first. */
    fun plan(settings: Settings, today: LocalDate, now: LocalDateTime): List<Planned> {
        val zone = settings.zone
        val out = ArrayList<Planned>()
        for (i in 0 until HORIZON_DAYS) {
            val date = today.plusDays(i.toLong())
            val t = PrayerComputer.compute(date, settings)
            val sehri = PrayerComputer.sehriEnd(t, settings)
            val iftar = PrayerComputer.iftar(t, settings)
            val base = (date.toEpochDay() % 100000L).toInt() * 10

            fun add(minute: Int, kind: String, minutes: Int, slot: Int) {
                val at = PrayerComputer.at(date, minute, zone).toLocalDateTime()
                if (!at.isAfter(now)) return
                out.add(Planned(at, kind, minutes, base + slot))
            }
            if (settings.notifySehriLeadEnabled) add(sehri - settings.notifySehriLeadMinutes, KIND_SEHRI_LEAD, settings.notifySehriLeadMinutes, 1)
            if (settings.notifySehriEndEnabled) add(sehri, KIND_SEHRI_END, 0, 2)
            if (settings.notifyIftarLeadEnabled) add(iftar - settings.notifyIftarLeadMinutes, KIND_IFTAR_LEAD, settings.notifyIftarLeadMinutes, 3)
            if (settings.notifyIftarEnabled) add(iftar, KIND_IFTAR, 0, 4)
        }
        out.sortBy { it.at }
        return out
    }

    fun scheduleNext(context: Context, settings: Settings) {
        Notifications.ensureChannels(context)
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val now = LocalDateTime.now()
        val zone = settings.zone
        val today = LocalDate.now(zone)
        val items = plan(settings, today, now)
        // Cap what we arm per run; the window rolls forward on every open and every fire.
        var armed = 0
        for (p in items) {
            if (armed >= 240) break
            val millis = p.at.atZone(zone).toInstant().toEpochMilli()
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra(EXTRA_KIND, p.kind)
                putExtra(EXTRA_MINUTES, p.minutes)
            }
            val pi = PendingIntent.getBroadcast(
                context, p.requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                    am.setWindow(AlarmManager.RTC_WAKEUP, millis, 10 * 60 * 1000L, pi)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
                } else {
                    am.setExact(AlarmManager.RTC_WAKEUP, millis, pi)
                }
            }
            armed++
        }
    }

    fun rescheduleFromSettings(context: Context) {
        val s = Store(context).load()
        scheduleNext(context, s)
    }

    /** Day-of-Ramadan label for the notification body, when today is in the month. */
    fun dayLabel(settings: Settings): String {
        val zone = settings.zone
        val today = LocalDate.now(zone)
        val month = Ramadan.monthOf(today, settings)
        val day = Ramadan.dayOf(today, month.start)
        return if (day != null) "Ramadan day $day" else "Ramadan"
    }
}
