package com.gwxl.app.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.gwxl.app.data.LocationStore
import com.gwxl.app.data.SavedPlace
import com.gwxl.app.data.SearchUiState
import com.gwxl.app.data.WeatherRepository
import com.gwxl.app.data.WeatherUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WeatherRepository()
    private val locationStore = LocationStore(application)
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    private val _state = MutableStateFlow(WeatherUiState())
    val state: StateFlow<WeatherUiState> = _state.asStateFlow()

    // Fallback location (geographic center of the contiguous US) if location is unavailable
    private val fallbackLat = 39.8283
    private val fallbackLon = -98.5795

    private var refreshJob: Job? = null
    private var searchJob: Job? = null

    private val _search = MutableStateFlow(SearchUiState(recents = locationStore.recents()))
    val search: StateFlow<SearchUiState> = _search.asStateFlow()

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val picked = locationStore.selected()
                val lat: Double
                val lon: Double
                val name: String
                if (picked != null) {
                    lat = picked.latitude
                    lon = picked.longitude
                    name = picked.name
                } else {
                    val location = getCurrentLocation()
                    lat = location?.first ?: fallbackLat
                    lon = location?.second ?: fallbackLon
                    name = if (location == null) {
                        "Central US (location off)"
                    } else {
                        reverseGeocode(lat, lon) ?: "Current Location"
                    }
                }

                repository.fetchWeather(lat, lon).fold(
                    onSuccess = { response ->
                        _state.value = repository.mapToUi(response, name)
                            .copy(usingCurrentLocation = picked == null)
                    },
                    onFailure = { e ->
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to load weather"
                        )
                    }
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Something went wrong"
                )
            }
        }
    }

    // --- Location search ---

    fun onSearchQueryChange(query: String) {
        _search.value = _search.value.copy(query = query, error = null)
        searchJob?.cancel()
        if (query.trim().length < 2) {
            _search.value = _search.value.copy(results = emptyList(), isSearching = false)
            return
        }
        searchJob = viewModelScope.launch {
            delay(300) // debounce typing
            _search.value = _search.value.copy(isSearching = true)
            repository.searchPlaces(query.trim()).fold(
                onSuccess = { results ->
                    _search.value = _search.value.copy(
                        results = results.map { it.toPlace() },
                        isSearching = false,
                        error = if (results.isEmpty()) "No places found" else null
                    )
                },
                onFailure = {
                    _search.value = _search.value.copy(
                        isSearching = false,
                        error = "Search failed. Check your connection."
                    )
                }
            )
        }
    }

    fun selectPlace(place: SavedPlace) {
        locationStore.select(place)
        resetSearch()
        refresh()
    }

    fun useCurrentLocation() {
        locationStore.useCurrentLocation()
        resetSearch()
        refresh()
    }

    fun removeRecent(place: SavedPlace) {
        locationStore.removeRecent(place)
        _search.value = _search.value.copy(recents = locationStore.recents())
    }

    fun resetSearch() {
        searchJob?.cancel()
        _search.value = SearchUiState(recents = locationStore.recents())
    }

    @SuppressLint("MissingPermission") // SecurityException is caught below
    private suspend fun getCurrentLocation(): Pair<Double, Double>? {
        return try {
            val fresh = withTimeoutOrNull(8_000) {
                val cancellationToken = CancellationTokenSource()
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cancellationToken.token
                ).await()
            }
            val location = fresh ?: fusedLocationClient.lastLocation.await()
            location?.let { Pair(it.latitude, it.longitude) }
        } catch (e: SecurityException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun reverseGeocode(lat: Double, lon: Double): String? = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(getApplication(), Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            addresses?.firstOrNull()?.let { addr ->
                addr.locality ?: addr.subAdminArea ?: addr.adminArea
            }
        } catch (e: Exception) {
            null
        }
    }
}
