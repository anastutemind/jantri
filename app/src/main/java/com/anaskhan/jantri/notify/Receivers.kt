package com.anaskhan.jantri.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.anaskhan.jantri.core.PrayerComputer
import com.anaskhan.jantri.core.Ramadan
import com.anaskhan.jantri.core.Store
import java.time.LocalDate
import java.time.LocalTime

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val store = Store(context)
        val s = store.load()
        val kind = intent.getStringExtra(Scheduler.EXTRA_KIND) ?: return
        val minutes = intent.getIntExtra(Scheduler.EXTRA_MINUTES, 0)
        val zone = s.zone
        val today = LocalDate.now(zone)
        val times = PrayerComputer.compute(today, s)
        val sehri = PrayerComputer.sehriEnd(times, s)
        val iftar = PrayerComputer.iftar(times, s)
        val day = Ramadan.dayOf(today, Ramadan.monthOf(today, s).start)
        val dayPrefix = if (day != null) "Ramadan day $day · " else ""
        val time = LocalTime.now(zone)

        Notifications.ensureChannels(context)
        val id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        when (kind) {
            Scheduler.KIND_SEHRI_LEAD -> Notifications.post(
                context, id, Notifications.CH_SEHRI,
                "Sehri ends in $minutes min",
                dayPrefix + Notifications.sehriLeadBody(minutes, sehri, s),
            )
            Scheduler.KIND_SEHRI_END -> Notifications.post(
                context, id, Notifications.CH_SEHRI,
                "Sehri time is over",
                dayPrefix + "Fajr has begun at ${PrayerComputer.fmt(times.fajr, s.use24Hour)}. Stop eating and drinking.",
            )
            Scheduler.KIND_IFTAR_LEAD -> Notifications.post(
                context, id, Notifications.CH_IFTAR,
                "Iftar in $minutes min",
                dayPrefix + Notifications.iftarLeadBody(minutes, iftar, s),
            )
            Scheduler.KIND_IFTAR -> Notifications.post(
                context, id, Notifications.CH_IFTAR,
                "Iftar time",
                dayPrefix + "Maghrib at ${PrayerComputer.fmt(times.maghrib, s.use24Hour)}. " +
                    "Allahumma inni laka sumtu wa bika amantu wa 'alayka tawakkaltu wa 'ala rizqika aftartu.",
            )
        }
        // Roll the window forward so the next alerts stay armed.
        Scheduler.scheduleNext(context, s)
        @Suppress("UNUSED_VARIABLE") val t = time
    }
}

/** Re-arm everything after a reboot, an app update, or the clock/timezone changing. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            -> Scheduler.rescheduleFromSettings(context)
        }
    }
}
