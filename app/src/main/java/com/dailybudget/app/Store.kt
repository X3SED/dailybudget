package com.dailybudget.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Currency
import java.util.Locale

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
                category = o.optString("cat", "").ifEmpty { null },
                excluded = o.optBoolean("excl", false),
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
                    .put("cat", e.category ?: "")
                    .put("excl", e.excluded)
            )
        }
        prefs.edit().putString(KEY_ENTRIES, array.toString()).apply()
    }

    fun loadSettings() = Settings(
        payday = prefs.getInt(KEY_PAYDAY, 1),
        reserveCents = prefs.getLong(KEY_RESERVE, 0),
        currency = prefs.getString(KEY_CURRENCY, null) ?: defaultCurrency(),
    )

    private fun defaultCurrency(): String =
        runCatching { Currency.getInstance(Locale.getDefault()).currencyCode }.getOrNull() ?: "EUR"

    fun saveSettings(settings: Settings) {
        prefs.edit()
            .putInt(KEY_PAYDAY, settings.payday)
            .putLong(KEY_RESERVE, settings.reserveCents)
            .putString(KEY_CURRENCY, settings.currency)
            .apply()
    }

    private companion object {
        const val KEY_ENTRIES = "entries"
        const val KEY_PAYDAY = "payday"
        const val KEY_RESERVE = "reserve"
        const val KEY_CURRENCY = "currency"
    }
}
