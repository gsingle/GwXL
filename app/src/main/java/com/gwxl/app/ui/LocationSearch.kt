package com.gwxl.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.gwxl.app.data.SavedPlace
import com.gwxl.app.data.SearchUiState

@Composable
fun LocationSearch(
    search: SearchUiState,
    usingCurrentLocation: Boolean,
    onQueryChange: (String) -> Unit,
    onPlaceSelected: (SavedPlace) -> Unit,
    onUseCurrentLocation: () -> Unit,
    onRemoveRecent: (SavedPlace) -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focus.requestFocus() }

    val dim = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = dim)
            }
            TextField(
                value = search.query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focus),
                placeholder = { Text("Search city or town") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                trailingIcon = {
                    if (search.query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = dim)
                        }
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                shape = MaterialTheme.shapes.large
            )
            Spacer(Modifier.width(12.dp))
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                PlaceRow(
                    icon = Icons.Default.MyLocation,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "Current location",
                    subtitle = if (usingCurrentLocation) "In use" else "Use your device's location",
                    onClick = onUseCurrentLocation
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
            }

            if (search.query.trim().length < 2) {
                if (search.recents.isNotEmpty()) {
                    item { SectionLabel("Recent") }
                    items(search.recents, key = { "r-${it.latitude},${it.longitude}" }) { place ->
                        PlaceRow(
                            icon = Icons.Default.History,
                            iconTint = dim,
                            title = place.name,
                            subtitle = place.subtitle,
                            onClick = { onPlaceSelected(place) },
                            onRemove = { onRemoveRecent(place) }
                        )
                    }
                }
            } else {
                if (search.isSearching && search.results.isEmpty()) {
                    item {
                        Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.Center) {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    }
                }
                search.error?.let { msg ->
                    item {
                        Text(
                            text = msg,
                            modifier = Modifier.padding(20.dp),
                            color = dim,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                items(search.results, key = { "s-${it.latitude},${it.longitude},${it.name}" }) { place ->
                    PlaceRow(
                        icon = Icons.Default.Place,
                        iconTint = dim,
                        title = place.name,
                        subtitle = place.subtitle,
                        onClick = { onPlaceSelected(place) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
    )
}

@Composable
private fun PlaceRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    onRemove: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }
        if (onRemove != null) {
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
