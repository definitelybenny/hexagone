package dev.definitelybenny.hexflipper.game

import android.content.Context
import android.content.SharedPreferences
import dev.definitelybenny.hexflipper.model.DifficultyTier
import dev.definitelybenny.hexflipper.model.TierStats
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

    // ── Random mode stats ──────────────────────────────────

    private val _randomStats = MutableStateFlow(loadRandomStats())
    val randomStats: StateFlow<Map<DifficultyTier, TierStats>> = _randomStats.asStateFlow()

    fun isTierUnlocked(tier: DifficultyTier): Boolean {
        val prereq = tier.prerequisiteTier ?: return true
        return getRandomSolved(prereq) >= tier.unlockRequirement
    }

    fun saveRandomResult(tier: DifficultyTier, moves: Int, stars: Int) {
        val solved = getRandomSolved(tier) + 1
        val currentBest = getRandomBestMoves(tier)
        val best = if (currentBest == null) moves else minOf(currentBest, moves)
        val totalStars = getRandomTotalStars(tier) + stars

        prefs.edit()
            .putInt("${KEY_RANDOM_SOLVED}${tier.name}", solved)
            .putInt("${KEY_RANDOM_BEST}${tier.name}", best)
            .putInt("${KEY_RANDOM_STARS}${tier.name}", totalStars)
            .apply()

        _randomStats.value = loadRandomStats()
    }

    private fun getRandomSolved(tier: DifficultyTier): Int =
        prefs.getInt("${KEY_RANDOM_SOLVED}${tier.name}", 0)

    private fun getRandomBestMoves(tier: DifficultyTier): Int? {
        val value = prefs.getInt("${KEY_RANDOM_BEST}${tier.name}", -1)
        return if (value >= 0) value else null
    }

    private fun getRandomTotalStars(tier: DifficultyTier): Int =
        prefs.getInt("${KEY_RANDOM_STARS}${tier.name}", 0)

    private fun loadRandomStats(): Map<DifficultyTier, TierStats> {
        return DifficultyTier.entries.associateWith { tier ->
            TierStats(
                solved = prefs.getInt("${KEY_RANDOM_SOLVED}${tier.name}", 0),
                bestMoves = prefs.getInt("${KEY_RANDOM_BEST}${tier.name}", -1)
                    .let { if (it >= 0) it else null },
                totalStars = prefs.getInt("${KEY_RANDOM_STARS}${tier.name}", 0)
            )
        }
    }

    // ── Reset ─────────────────────────────────────────────

    fun resetProgress() {
        _completedLevels.value = emptyMap()
        prefs.edit().clear()
            .putBoolean(KEY_SOUND, _soundEnabled.value)
            .putBoolean(KEY_MUSIC, _musicEnabled.value)
            .putBoolean(KEY_HAPTIC, _hapticEnabled.value)
            .apply()
        _randomStats.value = loadRandomStats()
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
        private const val KEY_RANDOM_SOLVED = "random_solved_"
        private const val KEY_RANDOM_BEST = "random_best_"
        private const val KEY_RANDOM_STARS = "random_stars_"
    }
}
