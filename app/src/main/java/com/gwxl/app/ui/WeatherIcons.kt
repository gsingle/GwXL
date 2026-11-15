package com.gwxl.app.ui

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

private val SunYellow = Color(0xFFF5C542)
private val CloudGray = Color(0xFFBDBDBD)
private val RainBlue = Color(0xFF5B9BE6)
private val BoltYellow = Color(0xFFF6E08C)
private val SnowWhite = Color(0xFFFFFFFF)
// Same as the app background; used so the cloud body occludes the sun or bolt.
private val Occlusion = Color(0xFF000000)
private const val StrokeWidth = 1.5f

private fun ImageVector.Builder.strokePath(
    color: Color,
    fill: Color = Color.Transparent,
    block: PathBuilder.() -> Unit
) {
    path(
        fill = SolidColor(fill),
        stroke = SolidColor(color),
        strokeLineWidth = StrokeWidth,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = block
    )
}

private fun PathBuilder.cloud(dy: Float = 0f) {
    fun y(v: Float) = v + dy
    moveTo(4.20f, y(16.20f))
    curveTo(2.35f, y(16.20f), 1.55f, y(14.70f), 1.55f, y(13.25f))
    curveTo(1.55f, y(11.60f), 2.85f, y(10.40f), 4.55f, y(10.45f))
    curveTo(5.05f, y(7.55f), 7.70f, y(5.35f), 11.20f, y(5.35f))
    curveTo(14.90f, y(5.35f), 17.45f, y(7.70f), 17.95f, y(10.70f))
    curveTo(20.15f, y(10.90f), 21.70f, y(12.60f), 21.70f, y(14.50f))
    curveTo(21.70f, y(16.20f), 20.15f, y(16.20f), 18.40f, y(16.20f))
    close()
}

private fun PathBuilder.sunCircle(cx: Float, cy: Float, r: Float) {
    moveTo(cx + r, cy)
    arcTo(r, r, 0f, true, true, cx - r, cy)
    arcTo(r, r, 0f, true, true, cx + r, cy)
    close()
}

private fun PathBuilder.sunRays(cx: Float, cy: Float, rIn: Float, rOut: Float, anglesDeg: IntArray) {
    for (a in anglesDeg) {
        val rad = Math.toRadians(a.toDouble())
        val cosA = cos(rad).toFloat()
        val sinA = sin(rad).toFloat()
        moveTo(cx + rIn * cosA, cy + rIn * sinA)
        lineTo(cx + rOut * cosA, cy + rOut * sinA)
    }
}

private fun PathBuilder.drop(cx: Float, cy: Float, s: Float = 1f) {
    moveTo(cx, cy)
    curveTo(cx + 0.08f * s, cy + 1.25f * s, cx + 1.15f * s, cy + 2.70f * s, cx + 1.15f * s, cy + 3.85f * s)
    arcTo(1.15f * s, 1.15f * s, 0f, true, true, cx - 1.15f * s, cy + 3.85f * s)
    curveTo(cx - 1.15f * s, cy + 2.70f * s, cx - 0.08f * s, cy + 1.25f * s, cx, cy)
    close()
}

private fun PathBuilder.flake(cx: Float, cy: Float, arm: Float = 1.80f) {
    // Three crossed lines at 60° — a simple 6-point snowflake.
    for (a in intArrayOf(0, 60, 120)) {
        val rad = Math.toRadians(a.toDouble())
        val dx = arm * cos(rad).toFloat()
        val dy = arm * sin(rad).toFloat()
        moveTo(cx - dx, cy - dy)
        lineTo(cx + dx, cy + dy)
    }
}

private fun vector(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply(block).build()

object WeatherIcons {
    val Clear: ImageVector = vector("Clear") {
        strokePath(SunYellow) {
            sunCircle(12f, 12f, 4.35f)
            sunRays(12f, 12f, 6.45f, 9.15f, intArrayOf(0, 45, 90, 135, 180, 225, 270, 315))
        }
    }

    val PartlyCloudy: ImageVector = vector("PartlyCloudy") {
        strokePath(SunYellow) {
            sunCircle(7.85f, 8.05f, 3.15f)
            sunRays(7.85f, 8.05f, 4.55f, 6.75f, intArrayOf(180, 210, 240, 270, 300, 330, 0))
        }
        strokePath(CloudGray, fill = Occlusion) { cloud(dy = 1.15f) }
    }

    val Cloudy: ImageVector = vector("Cloudy") {
        strokePath(CloudGray) { cloud(dy = 1.20f) }
    }

    val Rain: ImageVector = vector("Rain") {
        strokePath(CloudGray) { cloud() }
        strokePath(RainBlue) {
            drop(7.70f, 17.85f)
            drop(12.00f, 17.85f)
            drop(16.30f, 17.85f)
        }
    }

    val Drizzle: ImageVector = vector("Drizzle") {
        strokePath(CloudGray) { cloud() }
        strokePath(RainBlue) {
            drop(7.70f, 18.15f, 0.70f)
            drop(12.00f, 18.15f, 0.70f)
            drop(16.30f, 18.15f, 0.70f)
        }
    }

    val Showers: ImageVector = vector("Showers") {
        strokePath(CloudGray) { cloud() }
        strokePath(RainBlue) {
            drop(7.40f, 17.65f, 1.12f)
            drop(12.00f, 17.65f, 1.12f)
            drop(16.60f, 17.65f, 1.12f)
        }
    }

    val Thunderstorm: ImageVector = vector("Thunderstorm") {
        strokePath(CloudGray, fill = Occlusion) { cloud() }
        strokePath(BoltYellow) {
            moveTo(12.55f, 16.70f)
            lineTo(10.15f, 20.25f)
            lineTo(12.05f, 20.25f)
            lineTo(10.25f, 23.55f)
            lineTo(15.05f, 19.05f)
            lineTo(13.10f, 19.05f)
            close()
        }
    }

    val Snow: ImageVector = vector("Snow") {
        strokePath(CloudGray) { cloud() }
        strokePath(SnowWhite) {
            flake(7.00f, 19.95f)
            flake(12.00f, 19.95f)
            flake(17.00f, 19.95f)
        }
    }

    val Fog: ImageVector = vector("Fog") {
        strokePath(CloudGray) {
            cloud(dy = -0.35f)
            moveTo(6.0f, 18.25f); lineTo(18.0f, 18.25f)
            moveTo(7.4f, 20.35f); lineTo(16.6f, 20.35f)
            moveTo(6.6f, 22.45f); lineTo(17.4f, 22.45f)
        }
    }

    val FreezingRain: ImageVector = vector("FreezingRain") {
        strokePath(CloudGray) { cloud() }
        strokePath(RainBlue) {
            drop(7.00f, 17.85f, 0.92f)
            drop(17.00f, 17.85f, 0.92f)
        }
        strokePath(SnowWhite) { flake(12.00f, 19.95f, 1.70f) }
    }
}

fun weatherIconFor(code: Int): ImageVector = when (code) {
    0 -> WeatherIcons.Clear
    1, 2 -> WeatherIcons.PartlyCloudy
    3 -> WeatherIcons.Cloudy
    45, 48 -> WeatherIcons.Fog
    51, 53, 55 -> WeatherIcons.Drizzle
    56, 57, 66, 67 -> WeatherIcons.FreezingRain
    61, 63, 65 -> WeatherIcons.Rain
    71, 73, 75, 77, 85, 86 -> WeatherIcons.Snow
    80, 81, 82 -> WeatherIcons.Showers
    95, 96, 99 -> WeatherIcons.Thunderstorm
    else -> WeatherIcons.PartlyCloudy
}

@Composable
fun WeatherIcon(
    weatherCode: Int,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    Icon(
        imageVector = weatherIconFor(weatherCode),
        contentDescription = contentDescription,
        modifier = modifier,
        tint = Color.Unspecified
    )
}
