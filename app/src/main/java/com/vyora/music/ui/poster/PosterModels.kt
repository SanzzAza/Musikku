package com.vyora.music.ui.poster

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.vyora.music.data.ytmusic.SongItem
import com.vyora.music.player.LyricsUiState
import com.vyora.music.ui.player.CoverPalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/**
 * Mode Studio Poster VYORA:
 * - [PHOTO_CARDS]: Kartu pemutar Liquid Glass melayang di atas foto sendiri (1, 3, atau 5 kartu + efek Subjek di Depan!)
 * - [SONG_POSTER]: Poster lagu editorial dengan sampul besar, strip 5 palet warna, dan kutipan opsional.
 * - [LYRIC_POSTER]: Poster kutipan lirik (pilih hingga 4 baris) dengan kustomisasi tipografi penuh.
 */
enum class PosterMode(val label: String) {
    PHOTO_CARDS("Kartu Foto"),
    SONG_POSTER("Poster Lagu"),
    LYRIC_POSTER("Poster Lirik"),
}

/** Gaya latar belakang poster/kartu. */
enum class PosterBackground(val label: String, val isLightPaper: Boolean = false, val isTransparent: Boolean = false) {
    LIQUID_GLASS("Liquid Glass"),
    EDITORIAL_PAPER("Editorial", isLightPaper = true),
    OBSIDIAN("Obsidian"),
    COVER_BLUR("Cover Blur"),
    TRANSPARENT("Transparan", isTransparent = true),
}

/** Rasio ekspor PNG resolusi tinggi. */
enum class PosterAspect(
    val label: String,
    val ratioText: String,
    val ratio: Float,
    val widthPx: Int,
    val heightPx: Int,
) {
    STORY_9_16("Story", "9:16", 9f / 16f, 1080, 1920),
    POSTER_4_5("Poster", "4:5", 4f / 5f, 1080, 1350),
    SQUARE_1_1("Square", "1:1", 1f, 1080, 1080),
}

/** Preset tata letak jumlah kartu pemutar Liquid Glass pada mode Kartu Foto. */
enum class CardCollagePreset(val label: String, val count: Int) {
    SINGLE("1 Kartu", 1),
    TRIO("3 Kartu", 3),
    FIVE_COLLAGE("5 Kartu", 5),
}

/** Posisi kilau bintang 4-ujung (`✦`) di sudut kartu kaca. */
enum class SparkleCorner { NONE, TOP_RIGHT, TOP_LEFT, BOTTOM_RIGHT }

/**
 * Satu kartu pemutar Liquid Glass yang melayang di kanvas Kartu Foto.
 * Koordinat [xFraction] dan [yFraction] adalah pusat kartu dalam rentang 0..1 terhadap kanvas.
 */
@Immutable
data class GlassCardSlot(
    val slotId: Int,
    val song: SongItem,
    val xFraction: Float,
    val yFraction: Float,
    val scale: Float = 0.54f,
    val rotationDeg: Float = 0f,
    val progressFraction: Float = 0.34f,
    val volumeFraction: Float = 0.68f,
    val isLiked: Boolean = false,
    val sparkleCorner: SparkleCorner = SparkleCorner.NONE,
) {
    /** Menghitung teks waktu berjalan & sisa waktu yang realistis untuk kartu ini. */
    fun formattedTimestamps(): Pair<String, String> {
        val totalSec = song.durationSec.takeIf { it > 10 } ?: deterministicDurationSec(song.id)
        val elapsedSec = (totalSec * progressFraction.coerceIn(0.08f, 0.88f)).toInt()
        val remainSec = (totalSec - elapsedSec).coerceAtLeast(1)
        return formatSec(elapsedSec) to "-${formatSec(remainSec)}"
    }

    companion object {
        private fun deterministicDurationSec(id: String): Int {
            val hash = abs(id.hashCode())
            return 175 + (hash % 125) // ~2:55 sampai 5:00
        }

        private fun formatSec(sec: Int): String = "%d:%02d".format(sec / 60, sec % 60)

        /**
         * Membangun daftar slot kartu sesuai preset [CardCollagePreset].
         * Preset 5 Kartu menata 5 kartu kaca miring simetris (2 atas, 1 tengah, 2 bawah)
         * dengan ruang tengah bawah lega untuk efek kedalaman subjek foto.
         */
        fun buildForPreset(
            preset: CardCollagePreset,
            songs: List<SongItem>,
            primarySong: SongItem,
            primaryProgress: Float,
            likedIds: Set<String>,
        ): List<GlassCardSlot> {
            val pool = (listOf(primarySong) + songs.filter { it.id != primarySong.id })
            fun songAt(idx: Int): SongItem = pool.getOrElse(idx) { primarySong }
            fun progAt(idx: Int, song: SongItem): Float =
                if (idx == 0 && primaryProgress > 0.02f) primaryProgress.coerceIn(0.05f, 0.92f)
                else 0.24f + ((abs(song.id.hashCode() + idx * 31) % 45) / 100f)

            return when (preset) {
                CardCollagePreset.SINGLE -> {
                    val s = songAt(0)
                    listOf(
                        GlassCardSlot(
                            slotId = 0,
                            song = s,
                            xFraction = 0.50f,
                            yFraction = 0.48f,
                            scale = 0.94f,
                            rotationDeg = 0f,
                            progressFraction = progAt(0, s),
                            isLiked = s.id in likedIds,
                            sparkleCorner = SparkleCorner.TOP_RIGHT,
                        )
                    )
                }

                CardCollagePreset.TRIO -> {
                    val s0 = songAt(0)
                    val s1 = songAt(1)
                    val s2 = songAt(2)
                    listOf(
                        GlassCardSlot(
                            slotId = 0,
                            song = s1,
                            xFraction = 0.24f,
                            yFraction = 0.26f,
                            scale = 0.56f,
                            rotationDeg = -5.5f,
                            progressFraction = progAt(1, s1),
                            isLiked = s1.id in likedIds,
                            sparkleCorner = SparkleCorner.TOP_RIGHT,
                        ),
                        GlassCardSlot(
                            slotId = 1,
                            song = s0,
                            xFraction = 0.54f,
                            yFraction = 0.45f,
                            scale = 0.62f,
                            rotationDeg = 1.5f,
                            progressFraction = progAt(0, s0),
                            isLiked = s0.id in likedIds,
                            sparkleCorner = SparkleCorner.NONE,
                        ),
                        GlassCardSlot(
                            slotId = 2,
                            song = s2,
                            xFraction = 0.77f,
                            yFraction = 0.63f,
                            scale = 0.54f,
                            rotationDeg = -4.0f,
                            progressFraction = progAt(2, s2),
                            isLiked = s2.id in likedIds,
                            sparkleCorner = SparkleCorner.TOP_RIGHT,
                        ),
                    )
                }

                CardCollagePreset.FIVE_COLLAGE -> {
                    // Kartu utama ditempatkan di tengah sebagai fokus (slot 2), 4 lagu lain mengelilinginya.
                    val centerSong = songAt(0)
                    val topLeft = songAt(1)
                    val topRight = songAt(2)
                    val botLeft = songAt(3)
                    val botRight = songAt(4)
                    listOf(
                        GlassCardSlot(
                            slotId = 0,
                            song = topLeft,
                            xFraction = 0.21f,
                            yFraction = 0.24f,
                            scale = 0.52f,
                            rotationDeg = -4.5f,
                            progressFraction = progAt(1, topLeft),
                            isLiked = topLeft.id in likedIds,
                            sparkleCorner = SparkleCorner.TOP_RIGHT,
                        ),
                        GlassCardSlot(
                            slotId = 1,
                            song = topRight,
                            xFraction = 0.80f,
                            yFraction = 0.21f,
                            scale = 0.52f,
                            rotationDeg = 5.0f,
                            progressFraction = progAt(2, topRight),
                            isLiked = topRight.id in likedIds,
                            sparkleCorner = SparkleCorner.BOTTOM_RIGHT,
                        ),
                        GlassCardSlot(
                            slotId = 2,
                            song = centerSong,
                            xFraction = 0.50f,
                            yFraction = 0.37f,
                            scale = 0.56f,
                            rotationDeg = 0f,
                            progressFraction = progAt(0, centerSong),
                            isLiked = centerSong.id in likedIds,
                            sparkleCorner = SparkleCorner.NONE,
                        ),
                        GlassCardSlot(
                            slotId = 3,
                            song = botLeft,
                            xFraction = 0.20f,
                            yFraction = 0.61f,
                            scale = 0.52f,
                            rotationDeg = 2.8f,
                            progressFraction = progAt(3, botLeft),
                            isLiked = botLeft.id in likedIds,
                            sparkleCorner = SparkleCorner.NONE,
                        ),
                        GlassCardSlot(
                            slotId = 4,
                            song = botRight,
                            xFraction = 0.81f,
                            yFraction = 0.59f,
                            scale = 0.52f,
                            rotationDeg = -3.8f,
                            progressFraction = progAt(4, botRight),
                            isLiked = botRight.id in likedIds,
                            sparkleCorner = SparkleCorner.TOP_RIGHT,
                        ),
                    )
                }
            }
        }
    }
}

/** Konfigurasi khusus mode Kartu Foto Liquid Glass. */
@Immutable
data class PhotoCardsConfig(
    val preset: CardCollagePreset = CardCollagePreset.SINGLE,
    val glassOpacity: Float = 0.60f,
    val showSparkles: Boolean = true,
    val showHeart: Boolean = true,
    val showWatermark: Boolean = true,
    val subjectInFront: Boolean = true,
)

/** Pilihan font tipografi pada Poster Lagu & Poster Lirik. */
enum class PosterFont(
    val label: String,
    val family: FontFamily,
    val italicByDefault: Boolean = false,
) {
    MODERN("Modern", FontFamily.SansSerif),
    EDITORIAL("Editorial", FontFamily.Serif),
    ITALIC_SERIF("Miring", FontFamily.Serif, italicByDefault = true),
    MONO("Studio", FontFamily.Monospace),
}

/** Pilihan ketebalan huruf. */
enum class PosterWeight(val label: String, val weight: FontWeight) {
    REGULAR("Tipis", FontWeight.Normal),
    MEDIUM("Sedang", FontWeight.SemiBold),
    BOLD("Tebal", FontWeight.ExtraBold),
}

/** Pilihan perataan teks. */
enum class PosterAlign(val label: String, val textAlign: TextAlign) {
    START("Kiri", TextAlign.Start),
    CENTER("Tengah", TextAlign.Center),
    END("Kanan", TextAlign.End),
}

/** Pilihan warna teks dengan kontras terjaga. */
enum class PosterTextTone(val label: String) {
    AUTO("Otomatis"),
    IVORY("Putih"),
    ACCENT("Aksen"),
    WARM_GOLD("Krim"),
    INK_DARK("Gelap"),
}

/** Konfigurasi keseluruhan Poster / Kartu Lagu yang bisa diubah real-time oleh pengguna. */
@Immutable
data class PosterConfig(
    val mode: PosterMode = PosterMode.PHOTO_CARDS,
    val background: PosterBackground = PosterBackground.LIQUID_GLASS,
    val aspect: PosterAspect = PosterAspect.STORY_9_16,
    val font: PosterFont = PosterFont.EDITORIAL,
    val weight: PosterWeight = PosterWeight.MEDIUM,
    val align: PosterAlign = PosterAlign.START,
    val textTone: PosterTextTone = PosterTextTone.AUTO,
    val showPaletteBar: Boolean = true,
    val showProgressBar: Boolean = true,
    val showWatermark: Boolean = true,
    val photoCards: PhotoCardsConfig = PhotoCardsConfig(),
) {
    fun resolvePrimaryTextColor(palette: CoverPalette): Color = when (textTone) {
        PosterTextTone.AUTO -> if (background.isLightPaper) Color(0xFF1A1613) else Color(0xFFF6F1EA)
        PosterTextTone.IVORY -> Color(0xFFF7F3EC)
        PosterTextTone.ACCENT -> if (background.isLightPaper) {
            if (palette.accent.luminance() > 0.45f) Color(0xFFB44C2A) else palette.accent
        } else {
            palette.accent
        }
        PosterTextTone.WARM_GOLD -> Color(0xFFF4D9B0)
        PosterTextTone.INK_DARK -> Color(0xFF161311)
    }

    fun resolveSecondaryTextColor(palette: CoverPalette): Color {
        val primary = resolvePrimaryTextColor(palette)
        return primary.copy(alpha = if (background.isLightPaper) 0.66f else 0.72f)
    }

    fun resolveFontStyle(): FontStyle =
        if (font.italicByDefault) FontStyle.Italic else FontStyle.Normal
}

/** Permintaan global untuk membuka Studio Poster dari layar mana pun. */
@Immutable
data class PosterRequest(
    val song: SongItem,
    val initialLyrics: LyricsUiState? = null,
    val positionMs: Long = 0L,
    val initialMode: PosterMode = PosterMode.PHOTO_CARDS,
)

/**
 * Event bus ringan (sepola dengan [com.vyora.music.ui.playlist.PlaylistPicker])
 * agar Studio Poster Lagu bisa dibuka dari layar Pemutar, Lirik, maupun menu ⋮ lagu mana pun.
 */
object SongPosterPicker {
    private val _request = MutableStateFlow<PosterRequest?>(null)
    val request: StateFlow<PosterRequest?> = _request.asStateFlow()

    fun open(
        song: SongItem,
        lyricsState: LyricsUiState? = null,
        positionMs: Long = 0L,
        initialMode: PosterMode = PosterMode.PHOTO_CARDS,
    ) {
        _request.value = PosterRequest(
            song = song,
            initialLyrics = lyricsState,
            positionMs = positionMs,
            initialMode = initialMode,
        )
    }

    fun dismiss() {
        _request.value = null
    }
}

/** Utilitas mengekstrak strip 5 swatch warna harmonis untuk elemen visual poster. */
object PosterPaletteSwatches {
    fun buildSwatches(palette: CoverPalette, isLightPaper: Boolean): List<Color> {
        val darkAnchor = if (isLightPaper) Color(0xFF1E1916) else Color(0xFF121015)
        val lightAnchor = if (isLightPaper) Color(0xFFEDE4D6) else Color(0xFFF3ECE2)
        return listOf(
            darkAnchor,
            palette.accent,
            palette.secondaryAccent,
            palette.ambient,
            lightAnchor,
        )
    }
}
