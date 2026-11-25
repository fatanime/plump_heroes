package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.i18n.AppLanguage
import com.example.i18n.GameLocaleState
import com.example.i18n.tr
import com.example.ui.theme.GameGold
import com.example.ui.theme.GameGoldLight
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

@Composable
fun SplashScreen(
    isFirstLaunch: Boolean = false,
    onSplashFinished: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val showEnglishSplash = !GameLocaleState.hasChosenLanguage || GameLocaleState.language == AppLanguage.EN

    var progress by remember { mutableFloatStateOf(0f) }
    var screenAlpha by remember { mutableFloatStateOf(0f) }
    var showLanguageDialog by remember {
        mutableStateOf(!GameLocaleState.hasChosenLanguage)
    }
    var showNickDialog by remember { mutableStateOf(false) }
    var nickInput by remember { mutableStateOf("") }

    LaunchedEffect(isFirstLaunch) {
        val fadeInDuration = 400L
        val fadeStart = System.currentTimeMillis()
        while (true) {
            val elapsed = System.currentTimeMillis() - fadeStart
            if (elapsed >= fadeInDuration) {
                screenAlpha = 1f
                break
            }
            screenAlpha = (elapsed.toFloat() / fadeInDuration).coerceIn(0f, 1f)
            delay(16)
        }

        if (!GameLocaleState.hasChosenLanguage) {
            showLanguageDialog = true
            snapshotFlow { GameLocaleState.hasChosenLanguage }.first { it }
            showLanguageDialog = false
        }

        val totalDurationMs = if (isFirstLaunch) 2200L else 2400L
        val startTime = System.currentTimeMillis()
        val fadeOutDuration = 400L

        while (true) {
            val elapsed = System.currentTimeMillis() - startTime
            val fraction = (elapsed.toFloat() / totalDurationMs).coerceIn(0f, 1f)
            progress = fraction

            if (isFirstLaunch && fraction >= 1f) {
                screenAlpha = 1f
                break
            }

            screenAlpha = if (!isFirstLaunch && elapsed > (totalDurationMs - fadeOutDuration)) {
                ((totalDurationMs - elapsed).toFloat() / fadeOutDuration).coerceIn(0f, 1f)
            } else {
                1f
            }

            if (fraction >= 1f) {
                break
            }
            delay(16)
        }

        if (isFirstLaunch) {
            showNickDialog = true
        } else {
            delay(50)
            onSplashFinished(null)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF08070F))
            .graphicsLayer { alpha = screenAlpha }
            .testTag("splash_screen")
    ) {
        // Splash Artwork Background (always screen_en until a language is chosen)
        Image(
            painter = painterResource(if (showEnglishSplash) R.drawable.screen_en else R.drawable.screen),
            contentDescription = tr("Заставка Plump Heroes", "Plump Heroes splash"),
            contentScale = ContentScale.Fit,
            alignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(top = 4.dp, bottom = 36.dp)
                .testTag("splash_artwork_image")
        )

        // Subtle bottom scrim so progress bar, studio name, and version are crisp and legible
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.72f to Color.Transparent,
                        0.88f to Color(0xAA07040E),
                        1.0f to Color(0xEB07040E)
                    )
                )
        )

        // Progress Bar + Percentage above bottom footer line
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 52.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .fillMaxWidth(0.72f)
                    .height(9.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .border(1.2.dp, Color(0xFFFFD54F), RoundedCornerShape(5.dp))
                    .testTag("splash_progress_bar"),
                color = Color(0xFFFFD54F),
                trackColor = Color(0xFF1B0E33)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${(progress * 100).toInt()}%",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.testTag("splash_percentage_text")
            )
        }

        // Bottom Footer Row: Studio name centered, "v. 0.9.8 Pre Release" aligned right on the same line
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = tr("Пухляшникофф Games", "Puhlyashnikoff Games"),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE2D6F8),
                letterSpacing = 0.4.sp,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("splash_studio_text")
            )

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = "v. ${com.example.BuildConfig.VERSION_NAME}",
                    color = Color(0xFFFFE082),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.testTag("splash_version_text")
                )
            }
        }

        // First-launch Language Selection Popup (shown BEFORE nickname input; cannot be dismissed by outside tap)
        if (showLanguageDialog) {
            AlertDialog(
                onDismissRequest = { /* Player must explicitly select RU or EN */ },
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                ),
                containerColor = Color(0xFF1E1333),
                shape = RoundedCornerShape(16.dp),
                title = {
                    Text(
                        text = "Выберите язык / Choose language",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD54F),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("language_dialog_title")
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                GameLocaleState.setLanguage(context, AppLanguage.RU, markChosen = true)
                                showLanguageDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2B1F47),
                                contentColor = Color.White
                            ),
                            border = BorderStroke(1.5.dp, Color(0xFFFFD54F)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 50.dp)
                                .testTag("language_option_ru")
                        ) {
                            Text(
                                text = "🇷🇺 Русский",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                GameLocaleState.setLanguage(context, AppLanguage.EN, markChosen = true)
                                showLanguageDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2B1F47),
                                contentColor = Color.White
                            ),
                            border = BorderStroke(1.5.dp, Color(0xFFFFD54F)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 50.dp)
                                .testTag("language_option_en")
                        ) {
                            Text(
                                text = "🇬🇧English",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                confirmButton = {}
            )
        }

        // Nickname input popup dialog shown at 100% loading on first launch
        if (showNickDialog) {
            AlertDialog(
                onDismissRequest = { /* Must enter nickname */ },
                containerColor = Color(0xFF1E1333),
                shape = RoundedCornerShape(16.dp),
                title = {
                    Text(
                        text = tr("Ваш ник:", "Your Nickname:"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD54F),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = nickInput,
                            onValueChange = { input ->
                                if (input.length <= 14) {
                                    nickInput = input
                                }
                            },
                            singleLine = true,
                            maxLines = 1,
                            placeholder = {
                                Text(
                                    text = tr("Имя героини (до 14 симв.)", "Heroine name (up to 14 chars)"),
                                    fontSize = 12.sp,
                                    color = Color(0xFF8E81A8)
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFFD54F),
                                unfocusedBorderColor = Color(0xFF5E497A),
                                cursorColor = Color(0xFFFFD54F)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("nickname_input_field")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "${nickInput.length}/14",
                                fontSize = 10.sp,
                                color = if (nickInput.length == 14) Color(0xFFFFB74D) else Color(0xFFAFA2C6)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val finalNick = nickInput.trim().ifBlank { tr("Хранительница", "Guardian") }
                            showNickDialog = false
                            onSplashFinished(finalNick)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                        modifier = Modifier.testTag("nickname_submit_button")
                    ) {
                        Text(
                            text = tr("Далее", "Next"),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }
    }
}
