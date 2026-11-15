package com.gwxl.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gwxl.app.data.DailyUi
import com.gwxl.app.data.SavedPlace
import com.gwxl.app.data.SearchUiState
import com.gwxl.app.data.WeatherRepository
import com.gwxl.app.data.WeatherUiState

@Composable
fun WeatherScreen(
    state: WeatherUiState,
    search: SearchUiState,
    onRefresh: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onPlaceSelected: (SavedPlace) -> Unit,
    onUseCurrentLocation: () -> Unit,
    onRemoveRecent: (SavedPlace) -> Unit,
    onSearchClosed: () -> Unit
) {
    val repo = remember { WeatherRepository() } // only for text/emoji helpers
    var searchOpen by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        if (searchOpen) {
            LocationSearch(
                search = search,
                usingCurrentLocation = state.usingCurrentLocation,
                onQueryChange = onSearchQueryChange,
                onPlaceSelected = { searchOpen = false; onPlaceSelected(it) },
                onUseCurrentLocation = { searchOpen = false; onUseCurrentLocation() },
                onRemoveRecent = onRemoveRecent,
                onClose = { searchOpen = false; onSearchClosed() }
            )
            return@Box
        }

        when {
            state.isLoading && state.current == null -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            state.error != null && state.current == null -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.error,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = MaterialTheme.colorScheme.primary)
                    }
                    Text(
                        text = "Change location",
                        modifier = Modifier
                            .clickable { searchOpen = true }
                            .padding(8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            else -> WeatherContent(
                state = state,
                repo = repo,
                onRefresh = onRefresh,
                onLocationClick = { searchOpen = true },
                onUseCurrentLocation = onUseCurrentLocation
            )
        }
    }
}

@Composable
private fun WeatherContent(
    state: WeatherUiState,
    repo: WeatherRepository,
    onRefresh: () -> Unit,
    onLocationClick: () -> Unit,
    onUseCurrentLocation: () -> Unit
) {
    val onBg = MaterialTheme.colorScheme.onBackground
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        // Header: tap the location name to search / change location
        item {
          BoxWithConstraints(Modifier.fillMaxWidth()) {
            // Keep the location name clear of the centered title.
            val locationMaxWidth = maxWidth / 2 - 34.dp
            Text(
                text = "GwXL",
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = onBg.copy(alpha = 0.35f)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp, top = 0.dp, bottom = 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .widthIn(max = locationMaxWidth)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onLocationClick)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.usingCurrentLocation) {
                        Icon(
                            Icons.Default.Navigation,
                            contentDescription = "Current location",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = state.locationName,
                        modifier = Modifier.weight(1f, fill = false),
                        style = MaterialTheme.typography.titleMedium,
                        color = onBg.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Change location",
                        tint = onBg.copy(alpha = 0.5f)
                    )
                }
                Spacer(Modifier.weight(1f))
                if (!state.usingCurrentLocation) {
                    IconButton(onClick = onUseCurrentLocation) {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = "Back to current location",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = onRefresh) {
                    if (state.isLoading) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = onBg.copy(alpha = 0.6f))
                    }
                }
            }
          }
        }

        // Current conditions
        item {
            state.current?.let { current ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 0.dp, bottom = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = repo.weatherCodeToEmoji(current.weatherCode), fontSize = 42.sp, lineHeight = 46.sp)
                    Text(
                        text = "${current.temperature}°",
                        fontSize = 88.sp,
                        lineHeight = 90.sp,
                        fontWeight = FontWeight.Light,
                        color = onBg,
                        letterSpacing = (-2).sp
                    )
                    Text(text = current.summary, style = MaterialTheme.typography.titleLarge, color = onBg)
                    Text(
                        text = "Feels like ${current.feelsLike}°",
                        style = MaterialTheme.typography.bodyLarge,
                        color = onBg.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Wind ${current.windSpeed} kt · Humidity ${current.humidity}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = onBg.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // Next hour summary
        item {
            Text(
                text = state.nextHourSummary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Hourly strip + temperature graph (scroll together)
        item {
            SectionHeader("Next 24 hours")
            HourlyStripWithGraph(hours = state.hourly, repo = repo)
        }

        // Daily list with range bars on a shared scale
        item {
            Spacer(Modifier.height(16.dp))
            SectionHeader("Next ${state.daily.size} days")
        }
        if (state.daily.isNotEmpty()) {
            val scaleMin = state.daily.minOf { it.low }
            val scaleMax = state.daily.maxOf { it.high }
            items(state.daily) { day ->
                DailyRow(day, repo, scaleMin, scaleMax)
            }
        }

        // Footer
        item {
            Spacer(Modifier.height(14.dp))
            Text(
                text = state.lastUpdated?.let { "Updated $it" } ?: "",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = onBg.copy(alpha = 0.4f)
            )
            Text(
                text = "Powered by Open-Meteo · GwXL",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = onBg.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        modifier = Modifier.padding(start = 20.dp, bottom = 6.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
    )
}

@Composable
private fun DailyRow(day: DailyUi, repo: WeatherRepository, scaleMin: Int, scaleMax: Int) {
    val onBg = MaterialTheme.colorScheme.onBackground
    val compact = MaterialTheme.typography.labelSmall.copy(lineHeight = 13.sp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Day name + date, e.g. "Mon 11/2"
        Row(modifier = Modifier.width(88.dp)) {
            Text(
                text = day.dayName,
                modifier = Modifier.alignByBaseline(),
                style = MaterialTheme.typography.bodyLarge,
                color = onBg,
                maxLines = 1
            )
            if (day.dateLabel.isNotEmpty()) {
                Spacer(Modifier.width(5.dp))
                Text(
                    text = day.dateLabel,
                    modifier = Modifier.alignByBaseline(),
                    style = MaterialTheme.typography.labelMedium,
                    color = onBg.copy(alpha = 0.45f),
                    maxLines = 1
                )
            }
        }
        Text(
            text = repo.weatherCodeToEmoji(day.weatherCode),
            fontSize = 20.sp,
            modifier = Modifier.width(30.dp)
        )
        // Fixed-width column: rain chance (if any) over max wind, so the low temp and
        // bar line up the same on every row whether or not there's rain.
        Column(modifier = Modifier.width(36.dp)) {
            if (day.precipProbability >= 10) {
                Text(
                    text = "${day.precipProbability}%",
                    style = compact,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }
            Text(
                text = "${day.windMax}kt",
                style = compact,
                fontSize = 10.sp,
                color = onBg.copy(alpha = 0.4f),
                maxLines = 1
            )
        }
        Text(
            text = "${day.low}°",
            modifier = Modifier.width(34.dp),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyLarge,
            color = onBg.copy(alpha = 0.55f),
            maxLines = 1
        )
        TempRangeBar(
            low = day.low,
            high = day.high,
            scaleMin = scaleMin,
            scaleMax = scaleMax,
            modifier = Modifier
                .weight(1f)
                .height(20.dp)
                .padding(horizontal = 8.dp)
        )
        Text(
            text = "${day.high}°",
            modifier = Modifier.width(34.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = onBg,
            maxLines = 1
        )
    }
}
