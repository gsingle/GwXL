package com.gwxl.app.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Remembers the picked location (null = use device location) and recent picks
 * between launches. Plain SharedPreferences: tiny data, instant reads.
 */
class LocationStore(context: Context) {

    private val prefs = context.getSharedPreferences("gwxl_locations", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun selected(): SavedPlace? = prefs.getString(KEY_SELECTED, null)?.let {
        runCatching { json.decodeFromString<SavedPlace>(it) }.getOrNull()
    }

    fun recents(): List<SavedPlace> = prefs.getString(KEY_RECENTS, null)?.let {
        runCatching { json.decodeFromString<List<SavedPlace>>(it) }.getOrNull()
    } ?: emptyList()

    /** Select a place and push it to the top of the recents list. */
    fun select(place: SavedPlace) {
        val updated = (listOf(place) + recents().filterNot { it.sameAs(place) }).take(MAX_RECENTS)
        prefs.edit()
            .putString(KEY_SELECTED, json.encodeToString(place))
            .putString(KEY_RECENTS, json.encodeToString(updated))
            .apply()
    }

    fun useCurrentLocation() {
        prefs.edit().remove(KEY_SELECTED).apply()
    }

    fun removeRecent(place: SavedPlace) {
        val updated = recents().filterNot { it.sameAs(place) }
        prefs.edit().putString(KEY_RECENTS, json.encodeToString(updated)).apply()
    }

    private companion object {
        const val KEY_SELECTED = "selected_place"
        const val KEY_RECENTS = "recent_places"
        const val MAX_RECENTS = 8
    }
}
