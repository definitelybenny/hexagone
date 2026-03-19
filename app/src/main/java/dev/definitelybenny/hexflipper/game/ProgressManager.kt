package dev.definitelybenny.hexflipper.game

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages game progress persistence using SharedPreferences.
 * Tracks completed levels, star ratings, and settings.
 */
class ProgressManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("hex_flipper_prefs", Context.MODE_PRIVATE)

    private val _completedLevels = MutableStateFlow(loadCompletedLevels())
    val completedLevels: StateFlow<Map<Int, Int>> = _completedLevels.asStateFlow()

    private val _soundEnabled = MutableStateFlow(prefs.getBoolean(KEY_SOUND, true))
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _musicEnabled = MutableStateFlow(prefs.getBoolean(KEY_MUSIC, true))
    val musicEnabled: StateFlow<Boolean> = _musicEnabled.asStateFlow()

    private val _hapticEnabled = MutableStateFlow(prefs.getBoolean(KEY_HAPTIC, true))
    val hapticEnabled: StateFlow<Boolean> = _hapticEnabled.asStateFlow()

    fun setHapticEnabled(enabled: Boolean) {
        _hapticEnabled.value = enabled
        prefs.edit().putBoolean(KEY_HAPTIC, enabled).apply()
    }

    private val _tutorialSeen = MutableStateFlow(prefs.getBoolean(KEY_TUTORIAL_SEEN, false))
    val tutorialSeen: StateFlow<Boolean> = _tutorialSeen.asStateFlow()

    fun setTutorialSeen() {
        _tutorialSeen.value = true
        prefs.edit().putBoolean(KEY_TUTORIAL_SEEN, true).apply()
    }

    /** Highest unlocked level (1-based). Level N+1 unlocks when level N is completed. */
    val unlockedUpTo: Int
        get() {
            val completed = _completedLevels.value
            var highest = 1
            for (level in completed.keys) {
                if (level >= highest) highest = level + 1
            }
            return highest
        }

    fun saveStars(levelNumber: Int, stars: Int) {
        val current = _completedLevels.value.toMutableMap()
        val existing = current[levelNumber] ?: 0
        if (stars > existing) {
            current[levelNumber] = stars
            _completedLevels.value = current
            prefs.edit().putInt("$KEY_STARS_PREFIX$levelNumber", stars).apply()
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        _soundEnabled.value = enabled
        prefs.edit().putBoolean(KEY_SOUND, enabled).apply()
    }

    fun setMusicEnabled(enabled: Boolean) {
        _musicEnabled.value = enabled
        prefs.edit().putBoolean(KEY_MUSIC, enabled).apply()
    }

    fun resetProgress() {
        _completedLevels.value = emptyMap()
        prefs.edit().clear().putBoolean(KEY_SOUND, _soundEnabled.value)
            .putBoolean(KEY_MUSIC, _musicEnabled.value).apply()
    }

    private fun loadCompletedLevels(): Map<Int, Int> {
        val result = mutableMapOf<Int, Int>()
        for ((key, value) in prefs.all) {
            if (key.startsWith(KEY_STARS_PREFIX) && value is Int) {
                val levelNum = key.removePrefix(KEY_STARS_PREFIX).toIntOrNull()
                if (levelNum != null) {
                    result[levelNum] = value
                }
            }
        }
        return result
    }

    companion object {
        private const val KEY_STARS_PREFIX = "stars_"
        private const val KEY_SOUND = "sound_enabled"
        private const val KEY_MUSIC = "music_enabled"
        private const val KEY_HAPTIC = "haptic_enabled"
        private const val KEY_TUTORIAL_SEEN = "tutorial_seen"
    }
}
