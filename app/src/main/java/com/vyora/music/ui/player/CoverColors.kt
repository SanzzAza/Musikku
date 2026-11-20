package com.vyora.music.ui.player

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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class CoverPalette(
    val ambient: Color,
    val accent: Color,
    val secondaryAccent: Color,
) {
    companion object {
        val Default = CoverPalette(
            ambient = Color(0xFF2A194D),
            accent = Color(0xFF9D6BFF),
            secondaryAccent = Color(0xFFC9A0FF),
        )
    }
}

/**
 * Sampel warna dominan dari sampul album di luar main thread.
 * Menghasilkan pasangan warna: [CoverPalette.ambient] (kilau latar) dan
 * [CoverPalette.accent] (warna tombol & indikator yang selalu terbaca di tema gelap).
 */
@Composable
fun rememberCoverPalette(url: String?, enabled: Boolean = true): CoverPalette {
    if (!enabled || url.isNullOrBlank()) return CoverPalette.Default
    val context = LocalContext.current
    val palette by produceState(initialValue = CoverPalette.Default, key1 = url, key2 = enabled) {
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false)
            .size(64, 64)
            .build()
        val result = context.imageLoader.execute(request) as? SuccessResult
        val bitmap = (result?.drawable as? BitmapDrawable)?.bitmap
        value = if (bitmap != null) {
            withContext(Dispatchers.Default) { CoverPaletteMath.extractFromBitmap(bitmap) }
        } else {
            CoverPalette.Default
        }
    }
    return palette
}

/** Kompatibilitas helper tunggal untuk tint latar. */
@Composable
fun rememberCoverTint(url: String?, enabled: Boolean = true): Color =
    rememberCoverPalette(url, enabled).ambient

/** Logika murni (bisa diuji unit test tanpa Android runtime) untuk mengekstrak & merapikan palet. */
internal object CoverPaletteMath {

    fun extractFromBitmap(bitmap: Bitmap): CoverPalette {
        var rSum = 0.0
        var gSum = 0.0
        var bSum = 0.0
        var totalWeight = 0.0
        var bestAccentScore = -1.0
        var bestR = 157
        var bestG = 107
        var bestB = 255

        val stepX = max(1, bitmap.width / 24)
        val stepY = max(1, bitmap.height / 24)
        for (y in 0 until bitmap.height step stepY) {
            for (x in 0 until bitmap.width step stepX) {
                val p = bitmap.getPixel(x, y)
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                val hi = max(r, max(g, b))
                val lo = min(r, min(g, b))
                val chroma = (hi - lo) / 255.0
                val luma = (hi + lo) / 510.0

                val w = if (hi < 26 || lo > 236) 0.15 else 0.8 + chroma * 2.4
                rSum += r * w
                gSum += g * w
                bSum += b * w
                totalWeight += w

                // Cari piksel paling hidup namun tidak terlalu gelap/putih untuk aksen
                val lumaFit = 1.0 - abs(luma - 0.54) * 1.4
                val accentScore = chroma * 2.2 + lumaFit.coerceAtLeast(0.0) * 0.5
                if (chroma > 0.16 && hi > 48 && lo < 225 && accentScore > bestAccentScore) {
                    bestAccentScore = accentScore
                    bestR = r
                    bestG = g
                    bestB = b
                }
            }
        }
        if (totalWeight <= 0.0) return CoverPalette.Default
        val avgR = (rSum / totalWeight).toInt().coerceIn(0, 255)
        val avgG = (gSum / totalWeight).toInt().coerceIn(0, 255)
        val avgB = (bSum / totalWeight).toInt().coerceIn(0, 255)
        return buildPalette(
            avgRgb = Triple(avgR, avgG, avgB),
            vibrantRgb = if (bestAccentScore > 0.35) Triple(bestR, bestG, bestB) else Triple(avgR, avgG, avgB),
        )
    }

    fun buildPalette(
        avgRgb: Triple<Int, Int, Int>,
        vibrantRgb: Triple<Int, Int, Int>,
    ): CoverPalette {
        val (aH, aS, aL) = rgbToHsl(avgRgb.first, avgRgb.second, avgRgb.third)
        val (vH, vS, vL) = rgbToHsl(vibrantRgb.first, vibrantRgb.second, vibrantRgb.third)

        // Jika sampul hitam-putih / sangat pucat, pertahankan aksen Royal Violet khas VYORA
        if (vS < 0.10f && aS < 0.10f) {
            return CoverPalette(
                ambient = Color(0xFF221B36),
                accent = CoverPalette.Default.accent,
                secondaryAccent = CoverPalette.Default.secondaryAccent,
            )
        }

        // Latar ambient: gelap kaya warna agar teks tetap kontras
        val ambientColor = hslToColor(
            h = aH,
            s = aS.coerceIn(0.24f, 0.58f),
            l = aL.coerceIn(0.16f, 0.28f),
        )
        // Aksen utama: cerah hangat & mudah dibaca di atas obsidian
        val accentColor = hslToColor(
            h = vH,
            s = vS.coerceIn(0.45f, 0.78f),
            l = vL.coerceIn(0.56f, 0.68f),
        )
        // Aksen sekunder: geser hue sedikit lebih hangat untuk gradasi kaca
        val secondaryHue = (vH + 22f) % 360f
        val secondaryColor = hslToColor(
            h = secondaryHue,
            s = vS.coerceIn(0.42f, 0.80f),
            l = (vL + 0.08f).coerceIn(0.62f, 0.76f),
        )
        return CoverPalette(ambientColor, accentColor, secondaryColor)
    }

    internal fun rgbToHsl(r: Int, g: Int, b: Int): Triple<Float, Float, Float> {
        val rf = r / 255f
        val gf = g / 255f
        val bf = b / 255f
        val hi = max(rf, max(gf, bf))
        val lo = min(rf, min(gf, bf))
        val l = (hi + lo) / 2f
        val d = hi - lo
        if (d < 1e-5f) return Triple(0f, 0f, l)
        val s = d / (1f - abs(2f * l - 1f))
        val hRaw = when (hi) {
            rf -> ((gf - bf) / d) % 6f
            gf -> (bf - rf) / d + 2f
            else -> (rf - gf) / d + 4f
        }
        val h = ((hRaw * 60f) + 360f) % 360f
        return Triple(h, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))
    }

    internal fun hslToColor(h: Float, s: Float, l: Float): Color {
        val c = (1f - abs(2f * l - 1f)) * s
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val m = l - c / 2f
        val (r1, g1, b1) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return Color(
            red = (r1 + m).coerceIn(0f, 1f),
            green = (g1 + m).coerceIn(0f, 1f),
            blue = (b1 + m).coerceIn(0f, 1f),
        )
    }
}
