package com.example.musikku.player

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.media3.common.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Lives with the playback service, not the player screen or Activity. */
object SleepTimer {
    private val handler = Handler(Looper.getMainLooper())
    private var player: Player? = null
    private var deadlineMs: Long? = null
    private val _remainingMs = MutableStateFlow<Long?>(null)
    val remainingMs: StateFlow<Long?> = _remainingMs.asStateFlow()

    private val tick = object : Runnable {
        override fun run() {
            val deadline = deadlineMs ?: return
            val remaining = (deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0)
            if (remaining == 0L) {
                player?.pause()
                deadlineMs = null
                _remainingMs.value = null
            } else {
                _remainingMs.value = remaining
                handler.postDelayed(this, minOf(remaining, 1_000L))
            }
        }
    }

    fun attach(player: Player) { this.player = player }

    fun start(minutes: Int) {
        if (player == null || minutes <= 0) return
        handler.removeCallbacks(tick)
        deadlineMs = SystemClock.elapsedRealtime() + minutes.toLong() * 60_000L
        tick.run()
    }

    fun cancel() {
        handler.removeCallbacks(tick)
        deadlineMs = null
        _remainingMs.value = null
    }

    fun detach() {
        cancel()
        player = null
    }
}
