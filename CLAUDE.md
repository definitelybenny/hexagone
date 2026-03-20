# Hex Flipper

Android hex-grid puzzle game built with Kotlin and Jetpack Compose.

## Game Rules

- The board is a hex grid with colored tiles (stacks). Each tile has a directional arrow showing which way it will slide.
- Tap a tile to slide it off the board in its arrow direction. The tile flips end-over-end as it exits.
- A tile can only move if its entire path to the board edge is clear — other tiles block movement.
- Tapping a blocked tile counts as a move (the tile shakes to show it's blocked).
- Clear all tiles from the board to win.
- **Par system:** Each level has a par (minimum moves from BFS solver). Stars are awarded based on moves vs par:
  - 3 stars: moves <= par
  - 2 stars: moves <= par + 2
  - 1 star: more than par + 2
- Undo is available (decrements move count).
- Concurrent animations — multiple tiles can animate out simultaneously for fast play.
- 6 directional colors: Red (up), Orange (up-right), Yellow (down-right), Green (down), Blue (down-left), Purple (up-left).

## Architecture

Three-layer structure under `app/src/main/java/dev/definitelybenny/hexflipper/`:

- **model/** — Data classes and enums (HexModels.kt). Pure Kotlin, no Android dependencies.
- **game/** — Game logic, level generation, progress persistence. Depends on model.
- **ui/** — Jetpack Compose screens and components. Depends on game and model.

Navigation uses Jetpack Navigation Compose with a sealed `Screen` class in `Navigation.kt`.

## Key Files

| File | Responsibility |
|------|---------------|
| `model/HexModels.kt` | HexDirection, HexCell, HexStack, BoardState, Level, DifficultyTier, StarRating |
| `game/GameState.kt` | Core game logic — tap, undo, move counting, completion |
| `game/GameViewModel.kt` | Compose ViewModel — exposes StateFlows, manages concurrent animations |
| `game/LevelGenerator.kt` | Procedural puzzle generation with BFS solver for par calculation |
| `game/CampaignLevels.kt` | 30 hand-crafted campaign levels |
| `game/ProgressManager.kt` | SharedPreferences persistence — campaign stars, random mode stats, settings |
| `game/SoundManager.kt` | SoundPool-based sound effects |
| `ui/components/HexBoard.kt` | Canvas-based hex grid renderer with animation system |
| `ui/screens/Navigation.kt` | All routes and screen wiring |
| `ui/screens/GameScreen.kt` | Main gameplay UI |
| `ui/screens/DifficultySelectScreen.kt` | Random mode difficulty tier selection |
| `ui/screens/LevelSelectScreen.kt` | Campaign level grid |
| `ui/screens/LevelCompleteDialog.kt` | Victory dialog with stars and navigation options |
| `ui/screens/SettingsScreen.kt` | Sound, music, haptic toggles + reset progress |
| `ui/screens/MainMenuScreen.kt` | Campaign / Random Mode entry point |
| `MainActivity.kt` | Entry point, sets up Compose, hides nav bar for immersive mode |

## Build & Run

```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Run unit tests
./gradlew test

# Take a phone screenshot (requires adb)
bash screenshot.sh
```

Requires Android SDK with API 36. Min SDK is 26.

## Conventions

- Dark theme only (deep purple-blue gradient background `#0F0C29`)
- Hex grid uses axial coordinates (q, r) with pointy-top orientation
- Board generation biases toward center using inverse-square distance weighting
- All generated puzzles are validated as solvable via BFS before being presented
- Settings persisted via SharedPreferences through ProgressManager
- Sound respects the Sound toggle in Settings
