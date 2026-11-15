package com.gwxl.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response of https://geocoding-api.open-meteo.com/v1/search */
@Serializable
data class GeocodingResponse(
    val results: List<GeoResult>? = null
)

@Serializable
data class GeoResult(
    val id: Long? = null,
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val admin1: String? = null,
    val country: String? = null,
    @SerialName("country_code") val countryCode: String? = null
) {
    fun toPlace(): SavedPlace {
        val region = when {
            countryCode.equals("US", ignoreCase = true) && admin1 != null -> admin1
            else -> listOfNotNull(admin1, country).joinToString(", ").ifBlank { null }
        }
        return SavedPlace(name = name, region = region, latitude = latitude, longitude = longitude)
    }
}

/** A location the user picked from search. */
@Serializable
data class SavedPlace(
    val name: String,
    val region: String? = null,
    val latitude: Double,
    val longitude: Double
) {
    val subtitle: String get() = region ?: ""
    fun sameAs(other: SavedPlace): Boolean =
        kotlin.math.abs(latitude - other.latitude) < 0.01 &&
            kotlin.math.abs(longitude - other.longitude) < 0.01
}
