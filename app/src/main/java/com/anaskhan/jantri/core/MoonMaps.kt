package com.anaskhan.jantri.core

import android.content.Context
import org.json.JSONObject
import java.time.LocalDate

/**
 * The bundled visibility maps.
 *
 * They are drawn at build time (Odeh criterion, four bands, one image per evening) and
 * shipped inside the APK, so the app keeps its promise of no network calls. The
 * manifest is what the app reads: filenames carry a date, and guessing a filename for
 * a year nobody has published yet is exactly the bug this avoids.
 */
object MoonMaps {

    /** One evening's map, with the share of the globe in each band. */
    data class Frame(
        val evening: LocalDate,
        val file: String,
        val shares: Map<String, Double>,
    )

    private var manifest: JSONObject? = null
    private var zones: JSONObject? = null

    private fun load(context: Context) {
        if (manifest != null) return
        manifest = runCatching {
            JSONObject(context.assets.open("moon/manifest.json").bufferedReader().use { it.readText() })
        }.getOrNull()
        zones = runCatching {
            JSONObject(context.assets.open("moon/zones.json").bufferedReader().use { it.readText() })
        }.getOrNull()
    }

    /** True when the build shipped maps at all. */
    fun available(context: Context): Boolean {
        load(context)
        return (manifest?.length() ?: 0) > 0
    }

    /** Hijri years that have maps, ascending. */
    fun years(context: Context): List<Int> {
        load(context)
        val m = manifest ?: return emptyList()
        return m.keys().asSequence().mapNotNull { it.toIntOrNull() }.sorted().toList()
    }

    /**
     * The maps for one month of one Hijri year, in evening order. [code] is the three
     * letter month code the generator used: rmd, shw, zhj.
     */
    fun frames(context: Context, hijriYear: Int, code: String): List<Frame> {
        load(context)
        val year = manifest?.optJSONObject(hijriYear.toString()) ?: return emptyList()
        val arr = year.optJSONArray(code) ?: return emptyList()
        val out = ArrayList<Frame>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val file = o.optString("file")
            val evening = runCatching { LocalDate.parse(o.optString("evening")) }.getOrNull() ?: continue
            if (file.isBlank()) continue
            out.add(Frame(evening, file, sharesFor(context, file)))
        }
        return out
    }

    private fun sharesFor(context: Context, file: String): Map<String, Double> {
        load(context)
        val key = file.substringBeforeLast('.')
        val o = zones?.optJSONObject(key) ?: return emptyMap()
        val out = HashMap<String, Double>(4)
        for (band in o.keys()) out[band] = o.optDouble(band, 0.0)
        return out
    }
}
