package com.anaskhan.jantri.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class City(val name: String, val state: String, val lat: Double, val lng: Double, val population: Int, val elevation: Int, val zone: String) {
    val label: String get() = if (state.isBlank() || state == name) name else "$name, $state"
}

/** Offline city list (GeoNames, places over 10k people), loaded once from assets. */
object Cities {

    private var cache: List<City>? = null

    fun all(context: Context): List<City> {
        cache?.let { return it }
        val text = context.assets.open("cities.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(text)
        val out = ArrayList<City>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                City(
                    name = o.getString("n"),
                    state = o.optString("s", ""),
                    lat = o.getDouble("lat"),
                    lng = o.getDouble("lng"),
                    population = o.optInt("p", 0),
                    elevation = o.optInt("e", 0),
                    zone = o.optString("z", "Asia/Kolkata"),
                )
            )
        }
        cache = out
        return out
    }

    fun search(context: Context, query: String, limit: Int = 40): List<City> {
        val q = query.trim().lowercase()
        val list = all(context)
        if (q.isEmpty()) return list.take(limit)
        val starts = ArrayList<City>()
        val contains = ArrayList<City>()
        for (c in list) {
            val n = c.name.lowercase()
            when {
                n.startsWith(q) -> starts.add(c)
                n.contains(q) || c.state.lowercase().startsWith(q) -> contains.add(c)
            }
            if (starts.size >= limit) break
        }
        return (starts + contains).take(limit)
    }

    fun byLabel(context: Context, label: String): City? = all(context).firstOrNull { it.label == label }
}

/** Simple JSON-backed preference store; no external dependency, survives app updates. */
class Store(context: Context) {

    private val prefs = context.getSharedPreferences("jantri", Context.MODE_PRIVATE)

    fun load(): Settings {
        val raw = prefs.getString(KEY, null) ?: return Settings()
        return runCatching { decode(JSONObject(raw)) }.getOrDefault(Settings())
    }

    fun save(s: Settings) {
        prefs.edit().putString(KEY, encode(s).toString()).apply()
    }

    private fun encode(s: Settings): JSONObject = JSONObject().apply {
        put("placeName", s.placeName)
        put("lat", s.lat)
        put("lng", s.lng)
        put("elevationMetres", s.elevationMetres)
        put("zoneId", s.zoneId)
        put("methodId", s.methodId)
        put("customFajrAngle", s.customFajrAngle ?: JSONObject.NULL)
        put("customIshaAngle", s.customIshaAngle ?: JSONObject.NULL)
        put("asr", s.asr.name)
        put("highLatitudeRule", s.highLatitudeRule.name)
        put("adjFajr", s.adjFajr)
        put("adjSunrise", s.adjSunrise)
        put("adjDhuhr", s.adjDhuhr)
        put("adjAsr", s.adjAsr)
        put("adjMaghrib", s.adjMaghrib)
        put("adjIsha", s.adjIsha)
        put("sehriSafetyMin", s.sehriSafetyMin)
        put("iftarSafetyMin", s.iftarSafetyMin)
        put("ramadanStartOverride", s.ramadanStartOverride ?: JSONObject.NULL)
        put("notifySehriLeadMinutes", s.notifySehriLeadMinutes)
        put("notifySehriLeadEnabled", s.notifySehriLeadEnabled)
        put("notifySehriEndEnabled", s.notifySehriEndEnabled)
        put("notifyIftarLeadMinutes", s.notifyIftarLeadMinutes)
        put("notifyIftarLeadEnabled", s.notifyIftarLeadEnabled)
        put("notifyIftarEnabled", s.notifyIftarEnabled)
        put("notifyAdhanEnabled", s.notifyAdhanEnabled)
        put("use24Hour", s.use24Hour)
    }

    private fun decode(o: JSONObject): Settings {
        val d = Settings()
        fun num(key: String, def: Int) = if (o.has(key)) o.getInt(key) else def
        fun flag(key: String, def: Boolean) = if (o.has(key)) o.getBoolean(key) else def
        fun str(key: String) = if (o.has(key) && !o.isNull(key)) o.getString(key) else null
        return Settings(
            placeName = str("placeName") ?: d.placeName,
            lat = if (o.has("lat")) o.getDouble("lat") else d.lat,
            lng = if (o.has("lng")) o.getDouble("lng") else d.lng,
            elevationMetres = num("elevationMetres", d.elevationMetres),
            zoneId = str("zoneId") ?: d.zoneId,
            methodId = str("methodId") ?: d.methodId,
            customFajrAngle = if (o.has("customFajrAngle") && !o.isNull("customFajrAngle")) o.getDouble("customFajrAngle") else null,
            customIshaAngle = if (o.has("customIshaAngle") && !o.isNull("customIshaAngle")) o.getDouble("customIshaAngle") else null,
            asr = runCatching { AsrJuristic.valueOf(str("asr") ?: d.asr.name) }.getOrDefault(d.asr),
            highLatitudeRule = runCatching { HighLatitudeRule.valueOf(str("highLatitudeRule") ?: d.highLatitudeRule.name) }.getOrDefault(d.highLatitudeRule),
            adjFajr = num("adjFajr", d.adjFajr),
            adjSunrise = num("adjSunrise", d.adjSunrise),
            adjDhuhr = num("adjDhuhr", d.adjDhuhr),
            adjAsr = num("adjAsr", d.adjAsr),
            adjMaghrib = num("adjMaghrib", d.adjMaghrib),
            adjIsha = num("adjIsha", d.adjIsha),
            sehriSafetyMin = num("sehriSafetyMin", d.sehriSafetyMin),
            iftarSafetyMin = num("iftarSafetyMin", d.iftarSafetyMin),
            ramadanStartOverride = str("ramadanStartOverride"),
            notifySehriLeadMinutes = num("notifySehriLeadMinutes", d.notifySehriLeadMinutes),
            notifySehriLeadEnabled = flag("notifySehriLeadEnabled", d.notifySehriLeadEnabled),
            notifySehriEndEnabled = flag("notifySehriEndEnabled", d.notifySehriEndEnabled),
            notifyIftarLeadMinutes = num("notifyIftarLeadMinutes", d.notifyIftarLeadMinutes),
            notifyIftarLeadEnabled = flag("notifyIftarLeadEnabled", d.notifyIftarLeadEnabled),
            notifyIftarEnabled = flag("notifyIftarEnabled", d.notifyIftarEnabled),
            notifyAdhanEnabled = flag("notifyAdhanEnabled", d.notifyAdhanEnabled),
            use24Hour = flag("use24Hour", d.use24Hour),
        )
    }

    companion object {
        private const val KEY = "settings_v1"
    }
}
