package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.MainScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.GameBgDark
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    private val gameViewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    when (event) {
                        androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                            com.example.audio.GameAudioManager.setAppForeground(true)
                            gameViewModel.checkDailyLoginBonus()
                        }
                        androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> {
                            com.example.audio.GameAudioManager.setAppForeground(false)
                        }
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            MyApplicationTheme {
                var isSplashActive by remember { mutableStateOf(true) }

                val playerProfile by gameViewModel.playerProfile.collectAsState()
                val battleState by gameViewModel.battleState.collectAsState()

                LaunchedEffect(playerProfile.isSoundMuted) {
                    com.example.audio.GameAudioManager.setMuted(playerProfile.isSoundMuted)
                }
                LaunchedEffect(battleState?.phase, battleState?.isBoss, battleState == null) {
                    com.example.audio.GameAudioManager.setMode(
                        com.example.audio.resolveDesiredAudioMode(battleState)
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = GameBgDark
                ) {
                    Crossfade(
                        targetState = isSplashActive,
                        animationSpec = tween(durationMillis = 400),
                        label = "splash_crossfade"
                    ) { showSplash ->
                        if (showSplash) {
                            SplashScreen(
                                isFirstLaunch = !playerProfile.isTutorialCompleted,
                                onSplashFinished = { chosenNick ->
                                    if (chosenNick != null) {
                                        gameViewModel.setPlayerNicknameAndStartTutorial(chosenNick)
                                    }
                                    isSplashActive = false
                                }
                            )
                        } else {
                            MainScreen(viewModel = gameViewModel)
                        }
                    }
                }
            }
        }
    }
}
