# Random Mode Difficulty System

## Overview

Add a difficulty selection system to Random Mode with 6 tiers (Easy through Insane), a dedicated selection screen, progressive unlocking, and persistent per-tier stats.

Currently, Random Mode hardcodes `difficulty = 5` in `Navigation.kt`. This spec replaces that with a full difficulty selection flow.

## Difficulty Tiers

| Tier   | Stack Range | Board Size   | Unlock Requirement       |
|--------|-------------|--------------|--------------------------|
| Easy   | 3-5         | Auto-scaled  | Available from start     |
| Medium | 6-8         | Auto-scaled  | Solve 5 Easy puzzles     |
| Hard   | 9-12        | Auto-scaled  | Solve 5 Medium puzzles   |
| Expert | 13-16       | Auto-scaled  | Solve 5 Hard puzzles     |
| Master | 17-22       | Auto-scaled  | Solve 5 Expert puzzles   |
| Insane | 23-30       | Auto-scaled  | Solve 5 Master puzzles   |

Each time a puzzle is generated, a random stack count within the tier's range is chosen. Board size is derived automatically from the stack count to ensure enough room.

## Components

### 1. DifficultyTier Enum

**File:** `model/HexModels.kt`

New enum added to the model layer:

```kotlin
enum class DifficultyTier(
    val label: String,
    val stackRange: IntRange,
    val unlockRequirement: Int  // puzzles solved at previous tier, 0 = unlocked
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
```

### 2. LevelGenerator Updates

**File:** `game/LevelGenerator.kt`

**Changes:**
- Add `generate(tier: DifficultyTier)` overload that picks a random stack count from `tier.stackRange`
- Remove the `stackCount()` and `maxHeight()` helper functions — height is always 1, stack count comes from the tier
- Remove wall generation logic (walls are not used in this game)
- Auto-scale board size from stack count (see formula below)
- Remove the old `generate(difficulty: Int)` overload (no longer needed)

**Board size formula:** `boardSize = stackCount + max(6, stackCount * 2 / 3)` — ensures enough empty cells for movement. For small puzzles (3-5 stacks) this gives 9-11 total cells (~50-55% empty). For large puzzles (30 stacks) this gives 50 total cells (~40% empty). The formula may need tuning during implementation based on actual generation results.

**Background solver for large puzzles:**
For tiers with many stacks (Master, Insane), the BFS solver is launched on a background coroutine (`Dispatchers.Default`) when the puzzle is generated. The solver result (par) is delivered via a `Deferred<Int?>`. The game does not block on this — the player starts solving immediately.

When the player completes the puzzle:
- If the solver has finished: use the computed par for star calculation
- If the solver has not finished: use an estimated par of `stackCount` (the theoretical minimum — every stack must be removed in one move each). This is deliberately the most lenient possible par when no BFS result is available.

The solver coroutine must be scoped to the composable's lifecycle (e.g., `rememberCoroutineScope`) so it is automatically cancelled when the player navigates away (back, Change Difficulty, Menu). This prevents leaked coroutines.

**Function signature:** `generate(tier: DifficultyTier, random: Random = Random)` — preserves the `random` parameter for testability and seeded generation.

### 3. ProgressManager Updates

**File:** `game/ProgressManager.kt`

**New persistent data (SharedPreferences):**
- `random_solved_{TIER}` → `Int` — number of puzzles solved at each tier
- `random_best_moves_{TIER}` → `Int` — best (lowest) move count at each tier
- `random_total_stars_{TIER}` → `Int` — cumulative stars earned at each tier

**New methods:**
- `getRandomSolved(tier: DifficultyTier): Int`
- `getRandomBestMoves(tier: DifficultyTier): Int?`
- `getRandomTotalStars(tier: DifficultyTier): Int`
- `saveRandomResult(tier: DifficultyTier, moves: Int, stars: Int)`
- `isTierUnlocked(tier: DifficultyTier): Boolean` — checks if `getRandomSolved(prerequisiteTier) >= unlockRequirement`

**New StateFlows:**
- `randomStats: StateFlow<Map<DifficultyTier, TierStats>>` — reactive stats for the selection screen

**Data class:**
```kotlin
data class TierStats(
    val solved: Int = 0,
    val bestMoves: Int? = null,
    val totalStars: Int = 0
)
```

**Reset behavior:** `resetProgress()` also clears random mode stats. Additionally, preserve the haptic preference (currently only sound and music are preserved during reset).

### 4. Difficulty Selection Screen

**File:** `ui/screens/DifficultySelectScreen.kt` (new)

**Layout:** Vertical card list (full-width cards stacked vertically), consistent with existing level-select styling.

**Each card shows:**
- Tier name and color accent
- Stack range description (e.g., "3-5 stacks")
- Stats: puzzles solved, best moves, stars earned
- Locked tiers: dimmed at ~40% opacity, lock icon, unlock requirement text (e.g., "Solve 5 Medium puzzles")

**Composable signature:**
```kotlin
@Composable
fun DifficultySelectScreen(
    tierStats: Map<DifficultyTier, TierStats>,
    onTierClick: (DifficultyTier) -> Unit,
    onBackClick: () -> Unit
)
```

Locked tiers are not clickable. Unlocked tiers navigate to the game screen with the selected tier.

**Tier colors** (for card accents):
- Easy: Green
- Medium: Yellow/Amber
- Hard: Orange
- Expert: Red
- Master: Deep Purple
- Insane: Hot Pink / Magenta

### 5. Navigation Updates

**File:** `ui/screens/Navigation.kt`

**Changes:**
- Add `Screen.DifficultySelect` route
- Change `onRandomClick` to navigate to `DifficultySelect` instead of `RandomGame`
- Change `Screen.RandomGame` route to accept a tier parameter: `random_game/{tier}`
- `RandomGame` composable reads the tier argument, calls `LevelGenerator.generate(tier)`
- Store the current level in a `mutableStateOf` so "Replay" can reload the same puzzle and "Next Puzzle" can overwrite it with a newly generated level

**Completion dialog changes:**
- "Replay" button → reloads the exact same puzzle
- "Next Puzzle" button → generates a new puzzle at the same tier
- "Change Difficulty" button → navigates back to `DifficultySelect`
- "Menu" button → navigates back to `MainMenu` (unchanged)

### 6. LevelCompleteDialog Updates

**File:** `ui/screens/LevelCompleteDialog.kt`

**Changes:**
- Add `onChangeDifficulty` callback parameter (nullable, only shown in random mode)
- Rename `onNextLevel` to `onNext` for generality — in campaign it advances to next level, in random it generates a new puzzle
- In random mode, show "Replay", "Next Puzzle", "Change Difficulty", and "Menu" buttons
- `onReplay` reloads the exact same puzzle in random mode
- Campaign mode behavior unchanged

## Data Flow

```
MainMenu
  → DifficultySelectScreen (reads tierStats from ProgressManager)
    → RandomGame(tier) (generates puzzle + launches background solver)
      → Player solves puzzle
      → LevelCompleteDialog (uses solver par if ready, else estimates)
        → "Replay" → reload same puzzle
        → "Next Puzzle" → generate new puzzle at same tier
        → "Change Difficulty" → pop back to DifficultySelectScreen
        → "Menu" → pop back to MainMenu
```

## File Change Summary

| File | Change Type |
|------|-------------|
| `model/HexModels.kt` | Add `DifficultyTier` enum + `TierStats` data class |
| `game/LevelGenerator.kt` | Add tier-based generation, auto-scale board, background solver, remove walls/height/old overload |
| `game/ProgressManager.kt` | Add random mode stats persistence + StateFlows, fix haptic reset bug |
| `ui/screens/DifficultySelectScreen.kt` | New file — difficulty selection UI |
| `ui/screens/Navigation.kt` | Add DifficultySelect route, parameterize RandomGame, store current level for replay |
| `ui/screens/LevelCompleteDialog.kt` | Add "Change Difficulty" button, update Replay behavior for random mode |

## Edge Cases

- **Background solver timeout:** If the BFS solver exceeds 500K visited states, it returns null. In that case, fall back to estimated par (`stackCount + 2`). The 500K limit may be increased for background execution since it doesn't block the UI.
- **Fallback level:** If generation fails after MAX_BOARD_ATTEMPTS, the existing 2-stack fallback is used for any tier.
- **Progress reset:** Clearing progress resets all random mode stats and re-locks all tiers except Easy.
- **CellType.WALL:** Remains in the model (campaign levels may use it) but random generation no longer creates walls.
- **Board size tuning:** The auto-scale formula should be validated during implementation. If puzzles feel too tight or too spacious, adjust the padding ratio.
