package com.ah.filemanagerplus.core.audio

import com.mpatric.mp3agic.Mp3File
import javazoom.jl.player.Player
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream

class JvmAudioPlayer : AudioPlayer {

    @Volatile private var currentPlayer: Player? = null
    @Volatile private var currentThread: Thread? = null
    @Volatile private var currentPath: String? = null

    @Volatile private var totalDurationMs: Long = 0L
    @Volatile private var startOffsetMs: Long = 0L
    @Volatile private var startWallTimeMs: Long = 0L
    @Volatile private var bytesPerMs: Double = 0.0

    @Volatile private var paused = false
    @Volatile private var stopped = true
    @Volatile private var generation = 0

    private var onComplete: (() -> Unit)? = null

    override fun play(path: String): Boolean {
        val f = File(path)
        if (!f.exists() || !f.isFile) return false

        stopInternal()
        currentPath = path
        paused = false
        stopped = false

        val (durationMs, bitrateKbps) = readMp3Info(f)
        totalDurationMs = durationMs
        val br = bitrateKbps.coerceAtLeast(32)
        bytesPerMs = br * 125.0 / 1000.0

        startOffsetMs = 0L
        startWallTimeMs = System.currentTimeMillis()
        startPlayerAsync(f, 0L)
        return true
    }

    private fun readMp3Info(f: File): Pair<Long, Int> {
        return try {
            val mp3 = Mp3File(f)
            val ms = mp3.lengthInMilliseconds
            val br = if (mp3.bitrate > 0) mp3.bitrate else 128
            ms to br
        } catch (e: Exception) {
            val ms = (f.length() * 8L) / 128L
            ms to 128
        }
    }

    private fun startPlayerAsync(file: File, offsetMs: Long) {
        val myGen = ++generation
        val byteOffset = (offsetMs * bytesPerMs).toLong().coerceAtLeast(0)

        val t = Thread {
            try {
                val fis = FileInputStream(file)
                if (byteOffset > 0) {
                    try { fis.channel.position(byteOffset) } catch (e: Exception) {}
                }
                val stream = BufferedInputStream(fis, 65536)
                val player = Player(stream)

                if (myGen != generation || stopped) {
                    try { player.close() } catch (e: Exception) {}
                    return@Thread
                }

                currentPlayer = player
                player.play()

                if (myGen == generation && !paused && !stopped) {
                    onComplete?.invoke()
                }
            } catch (e: Exception) {
                // يتم تجاهله عند الإيقاف أو الانتقال
            } finally {
                if (myGen == generation) {
                    try { currentPlayer?.close() } catch (e: Exception) {}
                    currentPlayer = null
                }
            }
        }
        t.isDaemon = true
        t.name = "AHFM-Audio-$myGen"
        t.start()
        currentThread = t
    }

    override fun pause() {
        if (paused || stopped) return
        val pos = getPositionMs()
        paused = true
        generation++
        try { currentPlayer?.close() } catch (e: Exception) {}
        currentPlayer = null
        startOffsetMs = pos
    }

    override fun resume() {
        val path = currentPath ?: return
        if (!paused) return
        paused = false
        startWallTimeMs = System.currentTimeMillis()
        startPlayerAsync(File(path), startOffsetMs)
    }

    override fun stop() {
        stopInternal()
        currentPath = null
        startOffsetMs = 0L
        startWallTimeMs = 0L
        totalDurationMs = 0L
        bytesPerMs = 0.0
    }

    private fun stopInternal() {
        stopped = true
        paused = false
        generation++
        try { currentPlayer?.close() } catch (e: Exception) {}
        currentPlayer = null
        currentThread?.interrupt()
        currentThread = null
    }

    override fun seekTo(millis: Long) {
        val path = currentPath ?: return
        if (totalDurationMs <= 0) return

        val target = millis.coerceIn(0L, totalDurationMs)
        val wasPlaying = !paused && !stopped

        generation++
        try { currentPlayer?.close() } catch (e: Exception) {}
        currentPlayer = null

        startOffsetMs = target
        startWallTimeMs = System.currentTimeMillis()

        if (wasPlaying || paused) {
            paused = false
            stopped = false
            startPlayerAsync(File(path), target)
        }
    }

    override fun getPositionMs(): Long {
        if (totalDurationMs <= 0) return 0L
        val raw = if (paused || stopped || currentPlayer == null) {
            startOffsetMs
        } else {
            startOffsetMs + (System.currentTimeMillis() - startWallTimeMs)
        }
        return raw.coerceIn(0L, totalDurationMs)
    }

    override fun getDurationMs(): Long = totalDurationMs

    override fun isPlaying(): Boolean = currentPlayer != null && !paused && !stopped

    override fun setOnComplete(callback: () -> Unit) {
        onComplete = callback
    }

    override fun release() {
        stopInternal()
        onComplete = null
    }
}

actual fun createAudioPlayer(): AudioPlayer = JvmAudioPlayer()