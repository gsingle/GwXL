package com.gwxl.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class WeatherRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun fetchWeather(lat: Double, lon: Double): Result<OpenMeteoResponse> = withContext(Dispatchers.IO) {
        try {
            val url = buildString {
                append("https://api.open-meteo.com/v1/forecast")
                append("?latitude=$lat")
                append("&longitude=$lon")
                append("&current=temperature_2m,apparent_temperature,weather_code,wind_speed_10m,precipitation,relative_humidity_2m")
                append("&hourly=temperature_2m,apparent_temperature,precipitation_probability,precipitation,weather_code,wind_speed_10m")
                append("&daily=temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max,weather_code,wind_speed_10m_max")
                append("&temperature_unit=fahrenheit")
                append("&wind_speed_unit=kn")
                append("&timezone=auto")
                append("&forecast_days=14")
            }

            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val parsed = json.decodeFromString<OpenMeteoResponse>(body)
                Result.success(parsed)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchPlaces(query: String): Result<List<GeoResult>> = withContext(Dispatchers.IO) {
        try {
            val url = "https://geocoding-api.open-meteo.com/v1/search".toHttpUrl().newBuilder()
                .addQueryParameter("name", query)
                .addQueryParameter("count", "10")
                .addQueryParameter("language", "en")
                .addQueryParameter("format", "json")
                .build()
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: return@withContext Result.success(emptyList())
                Result.success(json.decodeFromString<GeocodingResponse>(body).results ?: emptyList())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun mapToUi(response: OpenMeteoResponse, locationName: String): WeatherUiState {
        val current = response.current?.let {
            CurrentUi(
                temperature = it.temperature?.roundToInt() ?: 0,
                feelsLike = it.apparentTemperature?.roundToInt() ?: 0,
                summary = weatherCodeToText(it.weatherCode ?: 0),
                weatherCode = it.weatherCode ?: 0,
                windSpeed = it.windSpeed?.roundToInt() ?: 0,
                humidity = it.humidity ?: 0
            )
        }

        val hourlyList = mutableListOf<HourlyUi>()
        val times = response.hourly?.time ?: emptyList()
        val temps = response.hourly?.temperature ?: emptyList()
        val probs = response.hourly?.precipitationProbability ?: emptyList()
        val codes = response.hourly?.weatherCode ?: emptyList()
        val winds = response.hourly?.windSpeed ?: emptyList()

        // Open-Meteo returns local times for the forecast location (timezone=auto),
        // so compare against "now" in that location's UTC offset.
        val zone = response.utcOffsetSeconds?.let { ZoneOffset.ofTotalSeconds(it) } ?: ZoneId.systemDefault()
        val nowHour = LocalDateTime.now(zone).truncatedTo(ChronoUnit.HOURS)
        val hourFmt = DateTimeFormatter.ofPattern("ha", Locale.US)

        // Index of the current hour in the hourly arrays
        val startIndex = times.indexOfFirst { t ->
            val dt = try { LocalDateTime.parse(t) } catch (_: Exception) { null }
            dt != null && !dt.isBefore(nowHour)
        }.let { if (it < 0) 0 else it }

        for (i in startIndex until minOf(times.size, startIndex + 24)) {
            val dt = try {
                LocalDateTime.parse(times[i], DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            } catch (_: Exception) {
                continue
            }
            val isNow = i == startIndex
            hourlyList.add(
                HourlyUi(
                    time = if (isNow) "Now" else dt.format(hourFmt).lowercase(),
                    temperature = temps.getOrNull(i)?.roundToInt() ?: 0,
                    precipProbability = probs.getOrNull(i) ?: 0,
                    weatherCode = codes.getOrNull(i) ?: 0,
                    windSpeed = winds.getOrNull(i)?.roundToInt() ?: 0,
                    isNow = isNow
                )
            )
        }

        val nextHourSummary = buildNextHourSummary(probs.drop(startIndex))

        val dailyList = mutableListOf<DailyUi>()
        val dayTimes = response.daily?.time ?: emptyList()
        val highs = response.daily?.temperatureMax ?: emptyList()
        val lows = response.daily?.temperatureMin ?: emptyList()
        val dayProbs = response.daily?.precipitationProbabilityMax ?: emptyList()
        val dayCodes = response.daily?.weatherCode ?: emptyList()
        val dayWinds = response.daily?.windSpeedMax ?: emptyList()

        val dayFmt = DateTimeFormatter.ofPattern("EEE", Locale.US)
        val dateFmt = DateTimeFormatter.ofPattern("M/d", Locale.US) // e.g. 11/2, no leading zeros
        for (i in dayTimes.indices) {
            val dayStr = dayTimes.getOrNull(i) ?: continue
            val date = try {
                java.time.LocalDate.parse(dayStr)
            } catch (_: Exception) {
                continue
            }
            val name = if (i == 0) "Today" else date.format(dayFmt)
            dailyList.add(
                DailyUi(
                    dayName = name,
                    dateLabel = date.format(dateFmt),
                    high = highs.getOrNull(i)?.roundToInt() ?: 0,
                    low = lows.getOrNull(i)?.roundToInt() ?: 0,
                    precipProbability = dayProbs.getOrNull(i) ?: 0,
                    weatherCode = dayCodes.getOrNull(i) ?: 0,
                    summary = weatherCodeToText(dayCodes.getOrNull(i) ?: 0),
                    windMax = dayWinds.getOrNull(i)?.roundToInt() ?: 0
                )
            )
        }

        return WeatherUiState(
            isLoading = false,
            locationName = locationName,
            current = current,
            nextHourSummary = nextHourSummary,
            hourly = hourlyList,
            daily = dailyList,
            lastUpdated = LocalDateTime.now().format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))
        )
    }

    private fun buildNextHourSummary(probs: List<Int>): String {
        if (probs.isEmpty()) return "No precipitation data"
        val firstFew = probs.take(2) // this hour and the next
        val maxProb = firstFew.maxOrNull() ?: 0
        return when {
            maxProb < 10 -> "No rain expected in the next hour"
            maxProb < 40 -> "Slight chance of rain soon"
            maxProb < 70 -> "Rain possible in the next hour"
            else -> "Rain likely in the next hour"
        }
    }

    fun weatherCodeToText(code: Int): String = when (code) {
        0 -> "Clear"
        1, 2 -> "Partly cloudy"
        3 -> "Cloudy"
        45, 48 -> "Foggy"
        51, 53, 55 -> "Drizzle"
        61, 63, 65 -> "Rain"
        66, 67 -> "Freezing rain"
        71, 73, 75 -> "Snow"
        77 -> "Snow grains"
        80, 81, 82 -> "Showers"
        85, 86 -> "Snow showers"
        95 -> "Thunderstorm"
        96, 99 -> "Thunderstorm with hail"
        else -> "Unknown"
    }
}

