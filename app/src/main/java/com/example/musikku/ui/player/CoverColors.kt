package com.example.musikku.ui.player

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/** Sample small software bitmap off the main thread. Fall back safely for missing images. */
@Composable
fun rememberCoverTint(url: String?): Color {
    val context = LocalContext.current
    val fallback = Color(0xFF235047)
    val color by produceState(initialValue = fallback, key1 = url) {
        value = fallback
        if (url == null) return@produceState
        val request = ImageRequest.Builder(context).data(url).allowHardware(false).size(64, 64).build()
        val result = context.imageLoader.execute(request) as? SuccessResult ?: return@produceState
        val bitmap = (result.drawable as? BitmapDrawable)?.bitmap ?: return@produceState
        value = withContext(Dispatchers.Default) { sampleTint(bitmap) }
    }
    return color
}

private fun sampleTint(bitmap: Bitmap): Color {
    var red = 0.0; var green = 0.0; var blue = 0.0; var weight = 0.0
    val stepX = max(1, bitmap.width / 24)
    val stepY = max(1, bitmap.height / 24)
    for (y in 0 until bitmap.height step stepY) for (x in 0 until bitmap.width step stepX) {
        val p = bitmap.getPixel(x, y)
        val r = android.graphics.Color.red(p); val g = android.graphics.Color.green(p); val b = android.graphics.Color.blue(p)
        val hi = max(r, max(g, b)); val lo = min(r, min(g, b))
        val w = if (hi < 28 || lo > 235) 0.2 else 1.0 + (hi - lo) / 100.0
        red += r * w; green += g * w; blue += b * w; weight += w
    }
    if (weight == 0.0) return Color(0xFF235047)
    // Darken to keep foreground text legible and ensure neutral artwork has a tint.
    return Color((red / weight / 255 * .55).toFloat(), (green / weight / 255 * .55).toFloat(), (blue / weight / 255 * .55).toFloat())
}
