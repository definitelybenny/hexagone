# Difficulty System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a 6-tier difficulty selection system to Random Mode with progressive unlocking, persistent stats, and a dedicated selection screen.

**Architecture:** New `DifficultyTier` enum drives the entire system. `LevelGenerator` accepts a tier and auto-scales board size from stack count. `ProgressManager` persists per-tier stats and unlock state. A new `DifficultySelectScreen` displays tiers as vertical cards. Navigation routes are parameterized by tier name.

**Tech Stack:** Kotlin, Jetpack Compose, Material3, SharedPreferences, Coroutines

**Spec:** `docs/superpowers/specs/2026-03-19-difficulty-system-design.md`

---

## File Structure

| File | Action | Responsibility |
|------|--------|----------------|
| `model/HexModels.kt` | Modify | Add `DifficultyTier` enum and `TierStats` data class |
| `game/LevelGenerator.kt` | Modify | Tier-based generation, auto-scale board, remove walls/height, background solver |
| `game/ProgressManager.kt` | Modify | Random mode stats persistence, tier unlock checks, haptic reset fix |
| `ui/screens/DifficultySelectScreen.kt` | Create | Difficulty selection UI with vertical card list |
| `ui/screens/Navigation.kt` | Modify | Add DifficultySelect route, parameterize RandomGame by tier |
| `ui/screens/LevelCompleteDialog.kt` | Modify | Add "Change Difficulty" button for random mode |
| `test/.../game/DifficultyTierTest.kt` | Create | Unit tests for tier enum logic |
| `test/.../game/LevelGeneratorTest.kt` | Create | Unit tests for tier-based generation |
| `test/.../game/ProgressManagerTest.kt` | Create | Unit tests for random stats persistence (requires Android context — see note) |

> **Note on ProgressManager tests:** `ProgressManager` depends on Android `Context` for `SharedPreferences`. Unit tests for it would require Robolectric or an instrumented test. Since the project doesn't currently use Robolectric, ProgressManager will be tested manually. The other unit tests (DifficultyTier, LevelGenerator) are pure Kotlin and work fine with JUnit.

All paths below are relative to `app/src/main/java/dev/definitelybenny/hexflipper/`.

---

### Task 1: Add DifficultyTier Enum and TierStats Data Class

**Files:**
- Modify: `model/HexModels.kt`
- Create: `app/src/test/java/dev/definitelybenny/hexflipper/game/DifficultyTierTest.kt`

- [ ] **Step 1: Write failing tests for DifficultyTier**

Create `app/src/test/java/dev/definitelybenny/hexflipper/game/DifficultyTierTest.kt`:

```kotlin
package dev.definitelybenny.hexflipper.game

import dev.definitelybenny.hexflipper.model.DifficultyTier
import org.junit.Assert.*
import org.junit.Test

class DifficultyTierTest {

    @Test
    fun `EASY has no prerequisite`() {
        assertNull(DifficultyTier.EASY.prerequisiteTier)
    }

    @Test
    fun `each tier except EASY has a prerequisite`() {
        for (tier in DifficultyTier.entries) {
            if (tier == DifficultyTier.EASY) continue
            assertNotNull("${tier.name} should have a prerequisite", tier.prerequisiteTier)
        }
    }

    @Test
    fun `prerequisite chain is EASY - MEDIUM - HARD - EXPERT - MASTER - INSANE`() {
        assertEquals(DifficultyTier.EASY, DifficultyTier.MEDIUM.prerequisiteTier)
        assertEquals(DifficultyTier.MEDIUM, DifficultyTier.HARD.prerequisiteTier)
        assertEquals(DifficultyTier.HARD, DifficultyTier.EXPERT.prerequisiteTier)
        assertEquals(DifficultyTier.EXPERT, DifficultyTier.MASTER.prerequisiteTier)
        assertEquals(DifficultyTier.MASTER, DifficultyTier.INSANE.prerequisiteTier)
    }

    @Test
    fun `stack ranges do not overlap and cover 3 to 30`() {
        val allValues = DifficultyTier.entries.flatMap { it.stackRange.toList() }
        assertEquals("no duplicates", allValues.size, allValues.toSet().size)
        assertEquals(3, allValues.min())
        assertEquals(30, allValues.max())
    }

    @Test
    fun `EASY unlock requirement is 0`() {
        assertEquals(0, DifficultyTier.EASY.unlockRequirement)
    }

    @Test
    fun `all other tiers require 5 solves`() {
        for (tier in DifficultyTier.entries) {
            if (tier == DifficultyTier.EASY) continue
            assertEquals(5, tier.unlockRequirement)
        }
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "dev.definitelybenny.hexflipper.game.DifficultyTierTest" --info`
Expected: Compilation error — `DifficultyTier` doesn't exist yet.

- [ ] **Step 3: Add DifficultyTier and TierStats to HexModels.kt**

Add at the end of `model/HexModels.kt`:

```kotlin
/** Difficulty tiers for Random Mode. */
enum class DifficultyTier(
    val label: String,
    val stackRange: IntRange,
    val unlockRequirement: Int
) {
    EASY("Easy", 3..5, 0),
    MEDIUM("Medium", 6..8, 5),
    HARD("Hard", 9..12, 5),
    EXPERT("Expert", 13..16, 5),
    MASTER("Master", 17..22, 5),
    INSANE("Insane", 23..30, 5);

    /** The tier that must be completed to unlock this one, or null if always unlocked. */
    val prerequisiteTier: DifficultyTier?
        get() = when (this) {
            EASY -> null
            MEDIUM -> EASY
            HARD -> MEDIUM
            EXPERT -> HARD
            MASTER -> EXPERT
            INSANE -> MASTER
        }
}

/** Stats for a single difficulty tier in Random Mode. */
data class TierStats(
    val solved: Int = 0,
    val bestMoves: Int? = null,
    val totalStars: Int = 0
)
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests "dev.definitelybenny.hexflipper.game.DifficultyTierTest" --info`
Expected: All 6 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/dev/definitelybenny/hexflipper/model/HexModels.kt \
       app/src/test/java/dev/definitelybenny/hexflipper/game/DifficultyTierTest.kt
git commit -m "feat: add DifficultyTier enum and TierStats data class"
```

---

### Task 2: Update LevelGenerator for Tier-Based Generation

**Files:**
- Modify: `game/LevelGenerator.kt`
- Create: `app/src/test/java/dev/definitelybenny/hexflipper/game/LevelGeneratorTest.kt`

- [ ] **Step 1: Write failing tests for tier-based generation**

Create `app/src/test/java/dev/definitelybenny/hexflipper/game/LevelGeneratorTest.kt`:

```kotlin
package dev.definitelybenny.hexflipper.game

import dev.definitelybenny.hexflipper.model.DifficultyTier
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class LevelGeneratorTest {

    @Test
    fun `generate with EASY tier produces 3-5 stacks`() {
        repeat(10) {
            val level = LevelGenerator.generate(DifficultyTier.EASY, Random(it))
            val stackCount = level.stacks.size
            assertTrue(
                "EASY should have 3-5 stacks, got $stackCount",
                stackCount in 3..5
            )
        }
    }

    @Test
    fun `generate with INSANE tier produces 23-30 stacks`() {
        repeat(5) {
            val level = LevelGenerator.generate(DifficultyTier.INSANE, Random(it + 100))
            val stackCount = level.stacks.size
            assertTrue(
                "INSANE should have 23-30 stacks, got $stackCount",
                stackCount in 23..30
            )
        }
    }

    @Test
    fun `all stacks have height 1`() {
        for (tier in DifficultyTier.entries) {
            val level = LevelGenerator.generate(tier, Random(42))
            for ((_, stack) in level.stacks) {
                assertEquals("All stacks should have height 1", 1, stack.height)
            }
        }
    }

    @Test
    fun `no walls are generated`() {
        for (tier in DifficultyTier.entries) {
            val level = LevelGenerator.generate(tier, Random(42))
            for ((_, cellType) in level.cells) {
                assertEquals(
                    "No walls should be generated",
                    dev.definitelybenny.hexflipper.model.CellType.EMPTY,
                    cellType
                )
            }
        }
    }

    @Test
    fun `board has enough cells for stacks plus movement room`() {
        for (tier in DifficultyTier.entries) {
            val level = LevelGenerator.generate(tier, Random(42))
            val totalCells = level.cells.size + level.stacks.size
            val stackCount = level.stacks.size
            assertTrue(
                "Board should have at least stackCount + 5 cells, got $totalCells for $stackCount stacks",
                totalCells >= stackCount + 5
            )
        }
    }

    @Test
    fun `generated level has a par value`() {
        val level = LevelGenerator.generate(DifficultyTier.EASY, Random(42))
        assertTrue("Par should be positive", level.par > 0)
    }

    @Test
    fun `seeded random produces deterministic results`() {
        val level1 = LevelGenerator.generate(DifficultyTier.MEDIUM, Random(99))
        val level2 = LevelGenerator.generate(DifficultyTier.MEDIUM, Random(99))
        assertEquals(level1.stacks.size, level2.stacks.size)
        assertEquals(level1.cells.size, level2.cells.size)
        assertEquals(level1.par, level2.par)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "dev.definitelybenny.hexflipper.game.LevelGeneratorTest" --info`
Expected: Compilation error — `generate(DifficultyTier, Random)` doesn't exist.

- [ ] **Step 3: Rewrite LevelGenerator**

Replace the entire contents of `game/LevelGenerator.kt` with:

```kotlin
package dev.definitelybenny.hexflipper.game

import dev.definitelybenny.hexflipper.model.BoardState
import dev.definitelybenny.hexflipper.model.CellType
import dev.definitelybenny.hexflipper.model.DifficultyTier
import dev.definitelybenny.hexflipper.model.HexCell
import dev.definitelybenny.hexflipper.model.HexDirection
import dev.definitelybenny.hexflipper.model.HexStack
import dev.definitelybenny.hexflipper.model.Level
import kotlin.random.Random

/**
 * Procedural level generator for random / endless mode.
 * Generates solvable hex puzzles based on difficulty tier.
 */
object LevelGenerator {

    private const val MAX_BOARD_ATTEMPTS = 50
    private const val MAX_STACK_ATTEMPTS = 200

    /**
     * Generate a solvable level for the given difficulty tier.
     * Stack count is randomly chosen within the tier's range.
     * Board size auto-scales to fit the stacks with movement room.
     */
    fun generate(tier: DifficultyTier, random: Random = Random): Level {
        val numStacks = random.nextInt(tier.stackRange.first, tier.stackRange.last + 1)
        val boardSize = numStacks + maxOf(6, numStacks * 2 / 3)
        val directions = HexDirection.entries

        repeat(MAX_BOARD_ATTEMPTS) {
            val allCells = generateBoardShape(boardSize, random)
            val cellList = allCells.toList()
            if (cellList.size < numStacks + 2) return@repeat

            repeat(MAX_STACK_ATTEMPTS) {
                val shuffled = cellList.shuffled(random)
                val stackCells = shuffled.take(numStacks)
                val remaining = shuffled.drop(numStacks)

                val stacks = mutableMapOf<HexCell, HexStack>()
                for (cell in stackCells) {
                    val dir = directions[random.nextInt(directions.size)]
                    stacks[cell] = HexStack(
                        cell = cell,
                        color = dir.color,
                        direction = dir,
                        height = 1
                    )
                }

                val cells = mutableMapOf<HexCell, CellType>()
                for (cell in remaining) {
                    cells[cell] = CellType.EMPTY
                }

                val state = BoardState(cells, stacks)
                val par = solve(state)

                if (par != null && par > 1) {
                    return Level(number = 0, cells = cells, stacks = stacks, par = par)
                }
            }
        }

        return createFallbackLevel()
    }

    /**
     * Generate a level and solve in the background for large puzzles.
     * Returns a pair of (Level, Deferred<Int?>) where the deferred
     * provides the BFS par. For small puzzles, par is computed inline.
     */
    suspend fun generateWithBackgroundSolver(
        tier: DifficultyTier,
        random: Random = Random,
        scope: kotlinx.coroutines.CoroutineScope
    ): Pair<Level, kotlinx.coroutines.Deferred<Int>> {
        val numStacks = random.nextInt(tier.stackRange.first, tier.stackRange.last + 1)
        val boardSize = numStacks + maxOf(6, numStacks * 2 / 3)
        val directions = HexDirection.entries

        // For large puzzles, generate without solving inline
        if (numStacks >= 17) {
            repeat(MAX_BOARD_ATTEMPTS) {
                val allCells = generateBoardShape(boardSize, random)
                val cellList = allCells.toList()
                if (cellList.size < numStacks + 2) return@repeat

                repeat(MAX_STACK_ATTEMPTS) {
                    val shuffled = cellList.shuffled(random)
                    val stackCells = shuffled.take(numStacks)
                    val remaining = shuffled.drop(numStacks)

                    val stacks = mutableMapOf<HexCell, HexStack>()
                    for (cell in stackCells) {
                        val dir = directions[random.nextInt(directions.size)]
                        stacks[cell] = HexStack(cell = cell, color = dir.color, direction = dir, height = 1)
                    }

                    val cells = mutableMapOf<HexCell, CellType>()
                    for (cell in remaining) {
                        cells[cell] = CellType.EMPTY
                    }

                    val state = BoardState(cells, stacks)
                    // Quick check: at least one stack can move
                    if (stacks.keys.any { state.canMove(it) }) {
                        val level = Level(number = 0, cells = cells, stacks = stacks, par = numStacks)
                        val parDeferred = scope.async(kotlinx.coroutines.Dispatchers.Default) {
                            solve(state) ?: numStacks
                        }
                        return Pair(level, parDeferred)
                    }
                }
            }
            val fallback = createFallbackLevel()
            val deferred = scope.async { fallback.par }
            return Pair(fallback, deferred)
        }

        // For smaller puzzles, solve inline (existing behavior)
        val level = generate(tier, random)
        val deferred = scope.async { level.par }
        return Pair(level, deferred)
    }

    private fun generateBoardShape(size: Int, random: Random): Set<HexCell> {
        val cells = mutableSetOf(HexCell(0, 0))
        val frontier = mutableListOf<HexCell>()
        for (dir in HexDirection.entries) {
            frontier.add(HexCell(0, 0).neighbor(dir))
        }
        while (cells.size < size && frontier.isNotEmpty()) {
            val idx = random.nextInt(frontier.size)
            val cell = frontier[idx]
            frontier[idx] = frontier.last()
            frontier.removeAt(frontier.lastIndex)
            if (cell in cells) continue
            cells.add(cell)
            for (dir in HexDirection.entries) {
                val nb = cell.neighbor(dir)
                if (nb !in cells) frontier.add(nb)
            }
        }
        return cells
    }

    /**
     * BFS solver. Each move removes one stack. Returns min moves or null.
     */
    internal fun solve(initial: BoardState): Int? {
        if (initial.isComplete) return 0

        fun stateKey(state: BoardState): List<Long> =
            state.stacks.keys.map { cell ->
                ((cell.q.toLong() and 0xFFFF) shl 16) or (cell.r.toLong() and 0xFFFF)
            }.sorted()

        val visited = HashSet<List<Long>>(512)
        visited.add(stateKey(initial))

        var frontier = listOf(initial)
        var depth = 0

        while (frontier.isNotEmpty()) {
            val nextFrontier = mutableListOf<BoardState>()

            for (state in frontier) {
                for (cell in state.stacks.keys) {
                    if (!state.canMove(cell)) continue
                    val next = state.removeStack(cell)
                    if (next.isComplete) return depth + 1
                    val key = stateKey(next)
                    if (visited.add(key)) {
                        nextFrontier.add(next)
                    }
                }
            }

            depth++
            frontier = nextFrontier
            if (visited.size > 500_000) return null
        }

        return null
    }

    private fun createFallbackLevel(): Level {
        val c0 = HexCell(0, 0)
        val c1 = HexCell(1, 0)
        val c2 = HexCell(2, 0)
        return Level(
            number = 0,
            cells = mapOf(c2 to CellType.EMPTY),
            stacks = mapOf(
                c0 to HexStack(c0, HexDirection.DOWN_RIGHT.color, HexDirection.DOWN_RIGHT, 1),
                c1 to HexStack(c1, HexDirection.DOWN_RIGHT.color, HexDirection.DOWN_RIGHT, 1)
            ),
            par = 2
        )
    }
}
```

Note: The `generateWithBackgroundSolver` uses `kotlinx.coroutines.async` and `Dispatchers.Default`. Add import for `kotlinx.coroutines.async` and `kotlinx.coroutines.Dispatchers` at the top.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests "dev.definitelybenny.hexflipper.game.LevelGeneratorTest" --info`
Expected: All 7 tests PASS.

Note: The `INSANE` test may take longer due to large board generation. If it times out or fails due to fallback level (2 stacks), adjust the random seed or increase `MAX_BOARD_ATTEMPTS` for large puzzles.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/dev/definitelybenny/hexflipper/game/LevelGenerator.kt \
       app/src/test/java/dev/definitelybenny/hexflipper/game/LevelGeneratorTest.kt
git commit -m "feat: tier-based level generation with auto-scaled board size"
```

---

### Task 3: Update ProgressManager for Random Mode Stats

**Files:**
- Modify: `game/ProgressManager.kt`

- [ ] **Step 1: Add random mode stat methods and fix haptic reset**

Add these imports to the top of `game/ProgressManager.kt`:

```kotlin
import dev.definitelybenny.hexflipper.model.DifficultyTier
import dev.definitelybenny.hexflipper.model.TierStats
```

Add these new members to the `ProgressManager` class, before the `companion object`:

```kotlin
    private val _randomStats = MutableStateFlow(loadRandomStats())
    val randomStats: StateFlow<Map<DifficultyTier, TierStats>> = _randomStats.asStateFlow()

    fun getRandomSolved(tier: DifficultyTier): Int =
        prefs.getInt("${KEY_RANDOM_SOLVED}${tier.name}", 0)

    fun getRandomBestMoves(tier: DifficultyTier): Int? {
        val value = prefs.getInt("${KEY_RANDOM_BEST}${tier.name}", -1)
        return if (value >= 0) value else null
    }

    fun getRandomTotalStars(tier: DifficultyTier): Int =
        prefs.getInt("${KEY_RANDOM_STARS}${tier.name}", 0)

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
```

Add these constants to the `companion object`:

```kotlin
        private const val KEY_RANDOM_SOLVED = "random_solved_"
        private const val KEY_RANDOM_BEST = "random_best_"
        private const val KEY_RANDOM_STARS = "random_stars_"
```

- [ ] **Step 2: Fix resetProgress to preserve haptic setting and clear random stats**

Replace the existing `resetProgress()` method:

```kotlin
    fun resetProgress() {
        _completedLevels.value = emptyMap()
        prefs.edit().clear()
            .putBoolean(KEY_SOUND, _soundEnabled.value)
            .putBoolean(KEY_MUSIC, _musicEnabled.value)
            .putBoolean(KEY_HAPTIC, _hapticEnabled.value)
            .apply()
        _randomStats.value = loadRandomStats()
    }
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/dev/definitelybenny/hexflipper/game/ProgressManager.kt
git commit -m "feat: add random mode stats persistence and tier unlock logic"
```

---

### Task 4: Create DifficultySelectScreen

**Files:**
- Create: `ui/screens/DifficultySelectScreen.kt`

- [ ] **Step 1: Create the difficulty selection screen**

Create `ui/screens/DifficultySelectScreen.kt`:

```kotlin
package dev.definitelybenny.hexflipper.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.definitelybenny.hexflipper.model.DifficultyTier
import dev.definitelybenny.hexflipper.model.TierStats

private val BackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0F0C29),
        Color(0xFF302B63),
        Color(0xFF24243E)
    )
)

private val StarGold = Color(0xFFFFD700)

private val tierGradients = mapOf(
    DifficultyTier.EASY to Brush.horizontalGradient(listOf(Color(0xFF43E97B), Color(0xFF38F9D7))),
    DifficultyTier.MEDIUM to Brush.horizontalGradient(listOf(Color(0xFFFDD663), Color(0xFFFBD786))),
    DifficultyTier.HARD to Brush.horizontalGradient(listOf(Color(0xFFFA8231), Color(0xFFF7B731))),
    DifficultyTier.EXPERT to Brush.horizontalGradient(listOf(Color(0xFFFF6B6B), Color(0xFFEE5A24))),
    DifficultyTier.MASTER to Brush.horizontalGradient(listOf(Color(0xFFA855F7), Color(0xFF7C3AED))),
    DifficultyTier.INSANE to Brush.horizontalGradient(listOf(Color(0xFFF093FB), Color(0xFFF5576C)))
)

private val tierTextColors = mapOf(
    DifficultyTier.EASY to Color(0xFF065F46),
    DifficultyTier.MEDIUM to Color(0xFF713F12),
    DifficultyTier.HARD to Color(0xFF7C2D12),
    DifficultyTier.EXPERT to Color.White,
    DifficultyTier.MASTER to Color.White,
    DifficultyTier.INSANE to Color.White
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DifficultySelectScreen(
    tierStats: Map<DifficultyTier, TierStats>,
    isTierUnlocked: (DifficultyTier) -> Boolean,
    onTierClick: (DifficultyTier) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Random Mode",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGradient)
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(DifficultyTier.entries) { tier ->
                    val unlocked = isTierUnlocked(tier)
                    val stats = tierStats[tier] ?: TierStats()

                    DifficultyCard(
                        tier = tier,
                        stats = stats,
                        unlocked = unlocked,
                        onClick = { if (unlocked) onTierClick(tier) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DifficultyCard(
    tier: DifficultyTier,
    stats: TierStats,
    unlocked: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val gradient = tierGradients[tier] ?: tierGradients[DifficultyTier.EASY]!!
    val textColor = tierTextColors[tier] ?: Color.White

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .alpha(if (unlocked) 1f else 0.4f)
            .background(gradient, shape)
            .clickable(enabled = unlocked, onClick = onClick)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (unlocked) tier.label else "${tier.label} \uD83D\uDD12",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${tier.stackRange.first}-${tier.stackRange.last} stacks",
                    fontSize = 14.sp,
                    color = textColor.copy(alpha = 0.7f)
                )
                if (!unlocked) {
                    val prereq = tier.prerequisiteTier
                    if (prereq != null) {
                        Text(
                            text = "Solve ${tier.unlockRequirement} ${prereq.label} puzzles",
                            fontSize = 12.sp,
                            color = textColor.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            if (unlocked && stats.solved > 0) {
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = StarGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${stats.totalStars}",
                            fontSize = 14.sp,
                            color = textColor
                        )
                    }
                    Text(
                        text = "${stats.solved} solved",
                        fontSize = 12.sp,
                        color = textColor.copy(alpha = 0.7f)
                    )
                    if (stats.bestMoves != null) {
                        Text(
                            text = "Best: ${stats.bestMoves} moves",
                            fontSize = 12.sp,
                            color = textColor.copy(alpha = 0.7f)
                        )
                    }
                }
            } else if (!unlocked) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = textColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/dev/definitelybenny/hexflipper/ui/screens/DifficultySelectScreen.kt
git commit -m "feat: add difficulty selection screen with tier cards"
```

---

### Task 5: Update LevelCompleteDialog

**Files:**
- Modify: `ui/screens/LevelCompleteDialog.kt`

- [ ] **Step 1: Add onChangeDifficulty parameter and update button layout**

Update the `LevelCompleteDialog` composable signature to add `onChangeDifficulty`:

```kotlin
@Composable
fun LevelCompleteDialog(
    stars: Int,
    moveCount: Int,
    par: Int,
    isRandomMode: Boolean,
    onNext: () -> Unit,
    onReplay: () -> Unit,
    onChangeDifficulty: (() -> Unit)? = null,
    onMenu: () -> Unit
)
```

Note: `onNextLevel` is renamed to `onNext`.

Update the button text for `onNext`:

```kotlin
Text(
    text = if (isRandomMode) "Next Puzzle" else "Next Level",
    ...
)
```

Add the "Change Difficulty" button between Replay and Menu (only in random mode). Insert after the Replay button's Spacer:

```kotlin
if (isRandomMode && onChangeDifficulty != null) {
    Spacer(modifier = Modifier.height(8.dp))

    OutlinedButton(
        onClick = onChangeDifficulty,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = "Change Difficulty",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: Compilation errors in Navigation.kt because `onNextLevel` was renamed to `onNext`. That's expected — we'll fix it in the next task.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/dev/definitelybenny/hexflipper/ui/screens/LevelCompleteDialog.kt
git commit -m "feat: add Change Difficulty button to level complete dialog"
```

---

### Task 6: Update Navigation

**Files:**
- Modify: `ui/screens/Navigation.kt`

- [ ] **Step 1: Add DifficultySelect route and update Screen sealed class**

Add to the `Screen` sealed class:

```kotlin
data object DifficultySelect : Screen("difficulty_select")
```

Update `RandomGame` to accept a tier parameter:

```kotlin
data object RandomGame : Screen("random_game/{tier}") {
    fun createRoute(tier: DifficultyTier): String = "random_game/${tier.name}"
}
```

Add this import at the top:

```kotlin
import dev.definitelybenny.hexflipper.model.DifficultyTier
```

- [ ] **Step 2: Update onRandomClick to navigate to DifficultySelect**

In the `MainMenu` composable block, change `onRandomClick`:

```kotlin
onRandomClick = {
    navController.navigate(Screen.DifficultySelect.route)
},
```

- [ ] **Step 3: Add DifficultySelect composable route**

Add this new composable block after the `MainMenu` composable and before the `LevelSelect` composable:

```kotlin
composable(Screen.DifficultySelect.route) {
    val randomStats by progressManager.randomStats.collectAsState()

    DifficultySelectScreen(
        tierStats = randomStats,
        isTierUnlocked = { tier -> progressManager.isTierUnlocked(tier) },
        onTierClick = { tier ->
            navController.navigate(Screen.RandomGame.createRoute(tier))
        },
        onBackClick = { navController.popBackStack() }
    )
}
```

- [ ] **Step 4: Rewrite the RandomGame composable route**

Replace the entire `composable(Screen.RandomGame.route)` block with:

```kotlin
composable(
    route = Screen.RandomGame.route,
    arguments = listOf(
        navArgument("tier") { type = NavType.StringType }
    )
) { backStackEntry ->
    val tierName = backStackEntry.arguments?.getString("tier") ?: DifficultyTier.EASY.name
    val tier = DifficultyTier.valueOf(tierName)
    val viewModel: GameViewModel = viewModel()

    var currentLevel by remember { mutableStateOf(LevelGenerator.generate(tier)) }

    LaunchedEffect(currentLevel) {
        viewModel.loadLevel(currentLevel)
    }

    val boardState by viewModel.boardState.collectAsState()
    val animatingStack by viewModel.animatingStack.collectAsState()
    val moveCount by viewModel.moveCount.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val isComplete by viewModel.isComplete.collectAsState()
    val starRating by viewModel.starRating.collectAsState()

    val tutorialSeen by progressManager.tutorialSeen.collectAsState()
    var showTutorial by remember { mutableStateOf(false) }
    val hapticEnabled by progressManager.hapticEnabled.collectAsState()
    val randomView = LocalView.current

    LaunchedEffect(tutorialSeen) {
        if (!tutorialSeen) showTutorial = true
    }

    LaunchedEffect(animatingStack) {
        if (hapticEnabled && animatingStack?.type == dev.definitelybenny.hexflipper.ui.components.AnimationType.SHAKE) {
            randomView.performHapticFeedback(HapticFeedbackConstants.REJECT)
        }
    }

    GameScreen(
        boardState = boardState,
        animatingStack = animatingStack,
        moveCount = moveCount,
        canUndo = canUndo,
        onCellTapped = viewModel::onCellTapped,
        onUndo = viewModel::onUndo,
        onHint = viewModel::onHint,
        onPause = { navController.popBackStack() },
        onAnimationFinished = viewModel::onAnimationComplete
    )

    if (showTutorial) {
        TutorialDialog(
            onDismiss = {
                showTutorial = false
                progressManager.setTutorialSeen()
            }
        )
    }

    if (isComplete) {
        val stars = starRating.stars
        LaunchedEffect(Unit) {
            progressManager.saveRandomResult(tier, moveCount, stars)
        }

        LevelCompleteDialog(
            stars = stars,
            moveCount = moveCount,
            par = currentLevel.par,
            isRandomMode = true,
            onNext = {
                currentLevel = LevelGenerator.generate(tier)
            },
            onReplay = {
                viewModel.loadLevel(currentLevel)
            },
            onChangeDifficulty = {
                navController.popBackStack(Screen.DifficultySelect.route, false)
            },
            onMenu = { navController.popBackStack(Screen.MainMenu.route, false) }
        )
    }
}
```

- [ ] **Step 5: Update the campaign Game composable's LevelCompleteDialog call**

In the `composable(Screen.Game.route)` block, rename `onNextLevel` to `onNext`:

```kotlin
LevelCompleteDialog(
    stars = stars,
    moveCount = moveCount,
    par = level.par,
    isRandomMode = false,
    onNext = {  // was onNextLevel
        ...
    },
    onReplay = { viewModel.loadLevel(level) },
    onMenu = { navController.popBackStack(Screen.MainMenu.route, false) }
)
```

- [ ] **Step 6: Add missing import for NavType**

Ensure this import exists (it should already be there for the campaign route):

```kotlin
import androidx.navigation.NavType
import androidx.navigation.navArgument
```

- [ ] **Step 7: Verify full build**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/dev/definitelybenny/hexflipper/ui/screens/Navigation.kt
git commit -m "feat: wire up difficulty selection flow with parameterized random mode"
```

---

### Task 7: Integration Testing and Polish

- [ ] **Step 1: Run all unit tests**

Run: `./gradlew test --info`
Expected: All tests PASS.

- [ ] **Step 2: Build the APK**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Manual testing checklist**

Test these scenarios on device/emulator:
- Main Menu → Random Mode → shows DifficultySelectScreen
- Only Easy is unlocked initially
- Tap Easy → generates puzzle with 3-5 stacks
- Complete puzzle → stars shown, "Next Puzzle", "Replay", "Change Difficulty", "Menu" buttons
- "Replay" → same puzzle reloads
- "Next Puzzle" → new puzzle at same difficulty
- "Change Difficulty" → back to difficulty select
- "Menu" → back to main menu
- After 5 Easy solves → Medium unlocks
- Stats persist after app restart
- Reset progress → all tiers re-lock except Easy, random stats cleared
- Campaign mode still works correctly (no regression)

- [ ] **Step 4: Final commit if any polish changes needed**

```bash
git add -A
git commit -m "fix: integration polish for difficulty system"
```
