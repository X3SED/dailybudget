package com.dailybudget.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class Store(context: Context) {
    private val prefs = context.getSharedPreferences("budget", Context.MODE_PRIVATE)

    fun loadEntries(): List<Entry> {
        val raw = prefs.getString(KEY_ENTRIES, null) ?: return emptyList()
        val array = JSONArray(raw)
        return (0 until array.length()).mapNotNull { i ->
            val o = array.getJSONObject(i)
            val type = runCatching { EntryType.valueOf(o.getString("type")) }.getOrNull() ?: return@mapNotNull null
            Entry(
                id = o.getLong("id"),
                type = type,
                cents = o.getLong("cents"),
                timestamp = o.getLong("ts"),
                note = o.optString("note", ""),
            )
        }
    }

    fun saveEntries(entries: List<Entry>) {
        val array = JSONArray()
        entries.forEach { e ->
            array.put(
                JSONObject()
                    .put("id", e.id)
                    .put("type", e.type.name)
                    .put("cents", e.cents)
                    .put("ts", e.timestamp)
                    .put("note", e.note)
            )
        }
        prefs.edit().putString(KEY_ENTRIES, array.toString()).apply()
    }

    fun loadSettings() = Settings(
        payday = prefs.getInt(KEY_PAYDAY, 1),
        reserveCents = prefs.getLong(KEY_RESERVE, 0),
    )

    fun saveSettings(settings: Settings) {
        prefs.edit()
            .putInt(KEY_PAYDAY, settings.payday)
            .putLong(KEY_RESERVE, settings.reserveCents)
            .apply()
    }

    private companion object {
        const val KEY_ENTRIES = "entries"
        const val KEY_PAYDAY = "payday"
        const val KEY_RESERVE = "reserve"
    }
}
