package com.anaskhan.jantri.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.anaskhan.jantri.MainActivity
import com.anaskhan.jantri.R
import com.anaskhan.jantri.core.PrayerComputer
import com.anaskhan.jantri.core.Settings

object Notifications {

    const val CH_SEHRI = "sehri"
    const val CH_IFTAR = "iftar"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val sehri = NotificationChannel(
            CH_SEHRI, "Sehri alerts", NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Warnings before sehri ends and when Fajr begins"
            enableVibration(true)
            setShowBadge(true)
        }
        val iftar = NotificationChannel(
            CH_IFTAR, "Iftar alerts", NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Reminders before Maghrib and at iftar time"
            enableVibration(true)
            setShowBadge(true)
        }
        nm.createNotificationChannel(sehri)
        nm.createNotificationChannel(iftar)
    }

    fun post(context: Context, id: Int, channel: String, title: String, body: String) {
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_jantri)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(id, n) }
    }

    fun sehriLeadBody(minutesLeft: Int, sehri: Int, s: Settings): String =
        "Sehri ends in $minutesLeft min at ${PrayerComputer.fmt(sehri, s.use24Hour)}. Finish eating before Fajr."

    fun iftarLeadBody(minutesLeft: Int, iftar: Int, s: Settings): String =
        "Iftar in $minutesLeft min at ${PrayerComputer.fmt(iftar, s.use24Hour)}. Prepare to break the fast."
}
