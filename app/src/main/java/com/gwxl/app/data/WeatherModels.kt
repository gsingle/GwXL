package com.gwxl.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenMeteoResponse(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timezone: String? = null,
    @SerialName("utc_offset_seconds") val utcOffsetSeconds: Int? = null,
    val current: CurrentWeather? = null,
    val hourly: HourlyWeather? = null,
    val daily: DailyWeather? = null
)

@Serializable
data class CurrentWeather(
    val time: String? = null,
    @SerialName("temperature_2m") val temperature: Double? = null,
    @SerialName("apparent_temperature") val apparentTemperature: Double? = null,
    @SerialName("weather_code") val weatherCode: Int? = null,
    @SerialName("wind_speed_10m") val windSpeed: Double? = null,
    @SerialName("precipitation") val precipitation: Double? = null,
    @SerialName("relative_humidity_2m") val humidity: Int? = null
)

@Serializable
data class HourlyWeather(
    val time: List<String>? = null,
    @SerialName("temperature_2m") val temperature: List<Double>? = null,
    @SerialName("apparent_temperature") val apparentTemperature: List<Double>? = null,
    @SerialName("precipitation_probability") val precipitationProbability: List<Int>? = null,
    @SerialName("precipitation") val precipitation: List<Double>? = null,
    @SerialName("weather_code") val weatherCode: List<Int>? = null
)

@Serializable
data class DailyWeather(
    val time: List<String>? = null,
    @SerialName("temperature_2m_max") val temperatureMax: List<Double>? = null,
    @SerialName("temperature_2m_min") val temperatureMin: List<Double>? = null,
    @SerialName("precipitation_sum") val precipitationSum: List<Double>? = null,
    @SerialName("precipitation_probability_max") val precipitationProbabilityMax: List<Int>? = null,
    @SerialName("weather_code") val weatherCode: List<Int>? = null
)

// UI-friendly models
data class CurrentUi(
    val temperature: Int,
    val feelsLike: Int,
    val summary: String,
    val weatherCode: Int,
    val windSpeed: Int,
    val humidity: Int
)

data class HourlyUi(
    val time: String,
    val temperature: Int,
    val precipProbability: Int,
    val weatherCode: Int,
    val isNow: Boolean = false
)

data class DailyUi(
    val dayName: String,
    val high: Int,
    val low: Int,
    val precipProbability: Int,
    val weatherCode: Int,
    val summary: String
)

data class WeatherUiState(
    val isLoading: Boolean = true,
    val locationName: String = "Locating…",
    val current: CurrentUi? = null,
    val nextHourSummary: String = "",
    val hourly: List<HourlyUi> = emptyList(),
    val daily: List<DailyUi> = emptyList(),
    val error: String? = null,
    val lastUpdated: String? = null,
    val usingCurrentLocation: Boolean = true
)

/** State of the location search screen. */
data class SearchUiState(
    val query: String = "",
    val results: List<SavedPlace> = emptyList(),
    val recents: List<SavedPlace> = emptyList(),
    val isSearching: Boolean = false,
    val error: String? = null
)
