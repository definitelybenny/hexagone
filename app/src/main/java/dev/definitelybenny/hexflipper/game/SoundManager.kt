package dev.definitelybenny.hexflipper.game

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import dev.definitelybenny.hexflipper.R

/**
 * Manages game sound effects using SoundPool for low-latency playback.
 */
class SoundManager(context: Context) {

    private val soundPool: SoundPool
    private val tapPopId: Int

    var enabled: Boolean = true

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(attrs)
            .build()

        tapPopId = soundPool.load(context, R.raw.tap_pop, 1)
    }

    fun playTap() {
        if (enabled) {
            soundPool.play(tapPopId, 1f, 1f, 1, 0, 1f)
        }
    }

    fun release() {
        soundPool.release()
    }
}
