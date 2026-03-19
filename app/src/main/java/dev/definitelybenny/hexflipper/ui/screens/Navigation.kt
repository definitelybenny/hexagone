package dev.definitelybenny.hexflipper.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import dev.definitelybenny.hexflipper.game.CampaignLevels
import dev.definitelybenny.hexflipper.game.GameViewModel
import dev.definitelybenny.hexflipper.game.LevelGenerator
import dev.definitelybenny.hexflipper.game.ProgressManager
import dev.definitelybenny.hexflipper.game.SoundManager
import dev.definitelybenny.hexflipper.model.DifficultyTier

/**
 * Route definitions for Hex Flipper navigation.
 */
sealed class Screen(val route: String) {
    data object MainMenu : Screen("main_menu")
    data object LevelSelect : Screen("level_select")
    data object Settings : Screen("settings")
    data object DifficultySelect : Screen("difficulty_select")

    data object RandomGame : Screen("random_game/{tier}") {
        fun createRoute(tier: DifficultyTier): String = "random_game/${tier.name}"
    }

    data object Game : Screen("game/{levelNumber}") {
        fun createRoute(levelNumber: Int): String = "game/$levelNumber"
    }
}

/**
 * Top-level navigation host for Hex Flipper.
 */
@Composable
fun HexFlipperNavHost(
    navController: NavHostController,
    progressManager: ProgressManager,
    soundManager: SoundManager
) {
    // Sync sound setting
    val soundEnabled by progressManager.soundEnabled.collectAsState()
    LaunchedEffect(soundEnabled) {
        soundManager.enabled = soundEnabled
    }

    NavHost(
        navController = navController,
        startDestination = Screen.MainMenu.route
    ) {
        composable(Screen.MainMenu.route) {
            MainMenuScreen(
                onCampaignClick = {
                    navController.navigate(Screen.LevelSelect.route)
                },
                onRandomClick = {
                    navController.navigate(Screen.DifficultySelect.route)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.LevelSelect.route) {
            val completedLevels by progressManager.completedLevels.collectAsState()

            LevelSelectScreen(
                totalLevels = CampaignLevels.levels.size,
                completedLevels = completedLevels,
                unlockedUpTo = progressManager.unlockedUpTo,
                onLevelClick = { levelNumber ->
                    navController.navigate(Screen.Game.createRoute(levelNumber))
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Game.route,
            arguments = listOf(
                navArgument("levelNumber") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val levelNumber = backStackEntry.arguments?.getInt("levelNumber") ?: 1
            val viewModel: GameViewModel = viewModel()
            val level = CampaignLevels.levels.getOrNull(levelNumber - 1)
                ?: CampaignLevels.levels.first()

            LaunchedEffect(levelNumber) {
                viewModel.loadLevel(level)
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
            val campaignView = LocalView.current

            LaunchedEffect(tutorialSeen) {
                if (!tutorialSeen) showTutorial = true
            }

            LaunchedEffect(animatingStack) {
                if (hapticEnabled && animatingStack?.type == dev.definitelybenny.hexflipper.ui.components.AnimationType.SHAKE) {
                    campaignView.performHapticFeedback(HapticFeedbackConstants.REJECT)
                }
            }

            GameScreen(
                boardState = boardState,
                animatingStack = animatingStack,
                moveCount = moveCount,
                canUndo = canUndo,
                onCellTapped = { cell ->
                    soundManager.playTap()
                    viewModel.onCellTapped(cell)
                },
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
                    progressManager.saveStars(levelNumber, stars)
                }

                LevelCompleteDialog(
                    stars = stars,
                    moveCount = moveCount,
                    par = level.par,
                    isRandomMode = false,
                    onNext = {
                        val nextLevel = levelNumber + 1
                        if (nextLevel <= CampaignLevels.levels.size) {
                            navController.popBackStack()
                            navController.navigate(Screen.Game.createRoute(nextLevel))
                        } else {
                            navController.popBackStack(Screen.LevelSelect.route, false)
                        }
                    },
                    onReplay = { viewModel.loadLevel(level) },
                    onMenu = { navController.popBackStack(Screen.MainMenu.route, false) }
                )
            }
        }

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
                onCellTapped = { cell ->
                    soundManager.playTap()
                    viewModel.onCellTapped(cell)
                },
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

        composable(Screen.Settings.route) {
            val soundEnabled by progressManager.soundEnabled.collectAsState()
            val musicEnabled by progressManager.musicEnabled.collectAsState()
            val hapticEnabled by progressManager.hapticEnabled.collectAsState()

            SettingsScreen(
                soundEnabled = soundEnabled,
                musicEnabled = musicEnabled,
                hapticEnabled = hapticEnabled,
                onSoundToggle = { progressManager.setSoundEnabled(it) },
                onMusicToggle = { progressManager.setMusicEnabled(it) },
                onHapticToggle = { progressManager.setHapticEnabled(it) },
                onResetProgress = { progressManager.resetProgress() },
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
