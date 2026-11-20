package com.vyora.music.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.widget.RemoteViews
import com.vyora.music.MainActivity
import com.vyora.music.R
import com.vyora.music.player.PlaybackService

/**
 * Home Screen Widget VYORA bergaya Liquid Glass Obsidian-Violet.
 * Menampilkan sampul lagu membulat, judul, artis, dan tombol kontrol langsung di layar utama HP.
 */
class VyoraMusicWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val state = loadLastState(context)
        val views = buildRemoteViews(
            context = context,
            title = state.first,
            artist = state.second,
            isPlaying = state.third,
            cover = null,
        )
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        if (action in setOf(ACTION_PLAY_PAUSE, ACTION_NEXT, ACTION_PREV)) {
            val svcIntent = Intent(context, PlaybackService::class.java).apply {
                this.action = action
            }
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(svcIntent)
                } else {
                    context.startService(svcIntent)
                }
            }
        }
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.vyora.music.widget.PLAY_PAUSE"
        const val ACTION_NEXT = "com.vyora.music.widget.NEXT"
        const val ACTION_PREV = "com.vyora.music.widget.PREV"
        private const val PREFS = "vyora_widget_state"

        fun pushUpdate(
            context: Context,
            title: String?,
            artist: String?,
            isPlaying: Boolean,
            coverBitmap: Bitmap? = null,
        ) {
            val appCtx = context.applicationContext
            val cleanTitle = title?.takeIf { it.isNotBlank() } ?: "Ketuk untuk memutar musik"
            val cleanArtist = artist?.takeIf { it.isNotBlank() } ?: "VYORA Music"
            saveLastState(appCtx, cleanTitle, cleanArtist, isPlaying)

            val manager = AppWidgetManager.getInstance(appCtx)
            val ids = manager.getAppWidgetIds(ComponentName(appCtx, VyoraMusicWidget::class.java))
            if (ids == null || ids.isEmpty()) return

            val roundedCover = coverBitmap?.let { roundCrop(it, 168, 32f) }
            val views = buildRemoteViews(appCtx, cleanTitle, cleanArtist, isPlaying, roundedCover)
            runCatching { manager.updateAppWidget(ids, views) }
        }

        private fun buildRemoteViews(
            context: Context,
            title: String,
            artist: String,
            isPlaying: Boolean,
            cover: Bitmap?,
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_vyora_player)
            views.setTextViewText(R.id.widget_title, title)
            views.setTextViewText(R.id.widget_artist, artist)
            views.setImageViewResource(
                R.id.widget_play_pause,
                if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
            )
            if (cover != null) {
                views.setImageViewBitmap(R.id.widget_cover, cover)
            } else {
                views.setImageViewResource(R.id.widget_cover, R.drawable.vyora_logo)
            }

            val openAppPi = PendingIntent.getActivity(
                context,
                100,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            views.setOnClickPendingIntent(R.id.widget_root, openAppPi)
            views.setOnClickPendingIntent(R.id.widget_cover, openAppPi)

            views.setOnClickPendingIntent(R.id.widget_prev, actionBroadcast(context, ACTION_PREV, 101))
            views.setOnClickPendingIntent(R.id.widget_play_pause, actionBroadcast(context, ACTION_PLAY_PAUSE, 102))
            views.setOnClickPendingIntent(R.id.widget_next, actionBroadcast(context, ACTION_NEXT, 103))
            return views
        }

        private fun actionBroadcast(context: Context, action: String, reqCode: Int): PendingIntent {
            val intent = Intent(context, VyoraMusicWidget::class.java).apply {
                this.action = action
            }
            return PendingIntent.getBroadcast(
                context,
                reqCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }

        private fun roundCrop(src: Bitmap, targetPx: Int, radiusPx: Float): Bitmap {
            val scaled = Bitmap.createScaledBitmap(src, targetPx, targetPx, true)
            val out = Bitmap.createBitmap(targetPx, targetPx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            }
            canvas.drawRoundRect(RectF(0f, 0f, targetPx.toFloat(), targetPx.toFloat()), radiusPx, radiusPx, paint)
            return out
        }

        private fun saveLastState(context: Context, title: String, artist: String, isPlaying: Boolean) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("title", title)
                .putString("artist", artist)
                .putBoolean("playing", isPlaying)
                .apply()
        }

        private fun loadLastState(context: Context): Triple<String, String, Boolean> {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            return Triple(
                p.getString("title", "Ketuk untuk memutar musik") ?: "Ketuk untuk memutar musik",
                p.getString("artist", "VYORA Music") ?: "VYORA Music",
                p.getBoolean("playing", false),
            )
        }
    }
}
