package com.gwxl.app.viewmodel

import android.app.Application
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.gwxl.app.data.WeatherRepository
import com.gwxl.app.data.WeatherUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WeatherRepository()
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    private val _state = MutableStateFlow(WeatherUiState())
    val state: StateFlow<WeatherUiState> = _state.asStateFlow()

    // Fallback location (rough center of US) if location fails
    private val fallbackLat = 39.8283
    private val fallbackLon = -98.5795

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val location = getCurrentLocation()
                val lat = location?.first ?: fallbackLat
                val lon = location?.second ?: fallbackLon
                val name = reverseGeocode(lat, lon) ?: "Current Location"

                val result = repository.fetchWeather(lat, lon)
                result.fold(
                    onSuccess = { response ->
                        _state.value = repository.mapToUi(response, name)
                    },
                    onFailure = { e ->
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to load weather"
                        )
                    }
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Something went wrong"
                )
            }
        }
    }

    private suspend fun getCurrentLocation(): Pair<Double, Double>? {
        return try {
            val cancellationToken = CancellationTokenSource()
            val location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cancellationToken.token
            ).await()
            location?.let { Pair(it.latitude, it.longitude) }
        } catch (e: SecurityException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun reverseGeocode(lat: Double, lon: Double): String? {
        return try {
            val geocoder = Geocoder(getApplication(), Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            addresses?.firstOrNull()?.let { addr ->
                addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: "Unknown"
            }
        } catch (e: Exception) {
            null
        }
    }
}
