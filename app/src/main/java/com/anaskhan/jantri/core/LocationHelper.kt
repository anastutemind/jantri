package com.anaskhan.jantri.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat

/** One-shot location fix using the platform LocationManager (no Play Services needed). */
object LocationHelper {

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun lastKnown(context: Context): Location? {
        if (!hasPermission(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        return providers.mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
            .maxByOrNull { it.time }
    }

    /** Requests a single fresh fix and calls back on the main thread; null when unavailable. */
    fun requestFix(context: Context, onResult: (Location?) -> Unit) {
        if (!hasPermission(context)) {
            onResult(null)
            return
        }
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        lastKnown(context)?.let {
            onResult(it)
            return
        }
        if (lm == null) {
            onResult(null)
            return
        }
        val handler = Handler(Looper.getMainLooper())
        var delivered = false
        val listener = object : android.location.LocationListener {
            override fun onLocationChanged(location: Location) {
                if (delivered) return
                delivered = true
                handler.post { onResult(location) }
            }

            @Deprecated("Required by the platform interface on API < 30")
            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
        }
        runCatching {
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, listener, Looper.getMainLooper())
        }
        runCatching {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, listener, Looper.getMainLooper())
        }
        handler.postDelayed({
            if (!delivered) {
                delivered = true
                runCatching { lm.removeUpdates(listener) }
                onResult(null)
            } else {
                runCatching { lm.removeUpdates(listener) }
            }
        }, 20_000)
    }
}
