package com.gwxl.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gwxl.app.data.HourlyUi
import com.gwxl.app.data.WeatherRepository

/** Cool-to-warm color for a temperature in °F (dark-mode friendly, Dark Sky-ish). */
fun tempColor(tempF: Float): Color {
    val stops = listOf(
        0f to Color(0xFF7E8CFF),   // very cold: periwinkle
        32f to Color(0xFF5EB8FF),  // freezing: blue
        50f to Color(0xFF4DD6C8),  // cool: teal
        65f to Color(0xFFB6E36A),  // mild: green-yellow
        78f to Color(0xFFFFC94D),  // warm: amber
        90f to Color(0xFFFF8A4C),  // hot: orange
        105f to Color(0xFFFF4F5E)  // very hot: red
    )
    if (tempF <= stops.first().first) return stops.first().second
    if (tempF >= stops.last().first) return stops.last().second
    for (i in 0 until stops.size - 1) {
        val (t0, c0) = stops[i]
        val (t1, c1) = stops[i + 1]
        if (tempF in t0..t1) return lerp(c0, c1, (tempF - t0) / (t1 - t0))
    }
    return stops.last().second
}

/**
 * Hourly strip: time + icon per hour, a temperature line graph with labeled points,
 * and precipitation chance, all in one horizontally scrolling row so they move together.
 */
@Composable
fun HourlyStripWithGraph(
    hours: List<HourlyUi>,
    repo: WeatherRepository,
    cellWidth: Dp = 56.dp
) {
    if (hours.isEmpty()) return
    val scroll = rememberScrollState()
    val dim = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
    Column(
        modifier = Modifier
            .horizontalScroll(scroll)
            .padding(horizontal = 12.dp)
    ) {
        Row {
            hours.forEach { h ->
                Column(Modifier.width(cellWidth), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = h.time,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (h.isNow) FontWeight.Bold else FontWeight.Normal,
                        color = if (h.isNow) MaterialTheme.colorScheme.onBackground else dim
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(text = repo.weatherCodeToEmoji(h.weatherCode), fontSize = 20.sp)
                }
            }
        }
        HourlyTempGraph(
            hours = hours,
            modifier = Modifier
                .width(cellWidth * hours.size)
                .height(78.dp)
        )
        // Rain chance (blue, only when >= 10%) and wind in knots (light gray), compact.
        val compact = MaterialTheme.typography.labelSmall.copy(lineHeight = 13.sp)
        Row {
            hours.forEach { h ->
                Column(Modifier.width(cellWidth), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (h.precipProbability >= 10) "${h.precipProbability}%" else " ",
                        style = compact,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${h.windSpeed}kt",
                        style = compact,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HourlyTempGraph(hours: List<HourlyUi>, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onBackground
    val labelStyle = remember(labelColor) {
        TextStyle(color = labelColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
    val bg = MaterialTheme.colorScheme.background

    Canvas(modifier = modifier) {
        val temps = hours.map { it.temperature.toFloat() }
        val minT = temps.min()
        val maxT = temps.max()
        val range = (maxT - minT).coerceAtLeast(6f) // keep flat days from looking dramatic
        val mid = (maxT + minT) / 2f
        val lo = mid - range / 2f

        val topPad = 24.dp.toPx()     // room for labels above points
        val bottomPad = 6.dp.toPx()
        val graphH = size.height - topPad - bottomPad
        val cell = size.width / hours.size

        val points = temps.mapIndexed { i, t ->
            Offset(cell * (i + 0.5f), topPad + (1f - (t - lo) / range) * graphH)
        }

        // Smooth path through the points
        val line = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                val p0 = points[i - 1]
                val p1 = points[i]
                val cx = (p0.x + p1.x) / 2f
                cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
            }
        }
        val lineBrush = if (points.size > 1) {
            Brush.horizontalGradient(
                colorStops = temps.mapIndexed { i, t ->
                    (points[i].x / size.width) to tempColor(t)
                }.toTypedArray(),
                startX = 0f,
                endX = size.width
            )
        } else Brush.linearGradient(listOf(tempColor(temps[0]), tempColor(temps[0])))

        // Soft fill under the line
        val fill = Path().apply {
            addPath(line)
            lineTo(points.last().x, size.height)
            lineTo(points.first().x, size.height)
            close()
        }
        drawPath(
            fill,
            brush = Brush.verticalGradient(
                listOf(tempColor(maxT).copy(alpha = 0.22f), Color.Transparent),
                startY = topPad,
                endY = size.height
            )
        )
        drawPath(line, brush = lineBrush, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))

        // Points + labels
        points.forEachIndexed { i, p ->
            val c = tempColor(temps[i])
            drawCircle(color = bg, radius = 4.5.dp.toPx(), center = p)
            drawCircle(color = c, radius = 3.dp.toPx(), center = p)
            val layout = measurer.measure("${hours[i].temperature}°", labelStyle)
            drawText(
                layout,
                topLeft = Offset(
                    p.x - layout.size.width / 2f,
                    p.y - layout.size.height - 6.dp.toPx()
                )
            )
        }
    }
}

/**
 * Dark Sky-style daily range bar: a rounded bar from [low] to [high], positioned on a
 * scale shared by all days ([scaleMin]..[scaleMax]), with a cool-to-warm gradient.
 */
@Composable
fun TempRangeBar(
    low: Int,
    high: Int,
    scaleMin: Int,
    scaleMax: Int,
    modifier: Modifier = Modifier
) {
    val track = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
    Canvas(modifier = modifier) {
        val span = (scaleMax - scaleMin).coerceAtLeast(1).toFloat()
        val barH = 6.dp.toPx()
        val y = (size.height - barH) / 2f
        val radius = CornerRadius(barH / 2f, barH / 2f)

        drawRoundRect(color = track, topLeft = Offset(0f, y), size = Size(size.width, barH), cornerRadius = radius)

        val startX = (low - scaleMin) / span * size.width
        val endX = ((high - scaleMin) / span * size.width).coerceAtLeast(startX + barH)
        drawRoundRect(
            brush = Brush.horizontalGradient(
                listOf(tempColor(low.toFloat()), tempColor(high.toFloat())),
                startX = startX,
                endX = endX
            ),
            topLeft = Offset(startX, y),
            size = Size(endX - startX, barH),
            cornerRadius = radius
        )
    }
}
