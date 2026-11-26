package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.i18n.localizeHeroNameByIdOrRu
import com.example.i18n.localizedEffectDescription
import com.example.i18n.localizedName
import com.example.i18n.tr
import com.example.model.ChestOpeningResult
import com.example.ui.components.HeroPortraitImage
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ChestOpeningDialog(
    result: ChestOpeningResult,
    onDismiss: () -> Unit
) {
    val accentColor = Color(result.accentColorHex)
    var isRevealed by remember(result) { mutableStateOf(false) }

    val shakeOffset = remember(result) { Animatable(0f) }
    val shakeRotation = remember(result) { Animatable(0f) }
    val chestScale = remember(result) { Animatable(0.92f) }
    val burstAlpha = remember(result) { Animatable(0.35f) }

    val infiniteTransition = rememberInfiniteTransition(label = "chest_glow_spin")
    val rayAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ray_angle"
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    LaunchedEffect(result) {
        isRevealed = false
        // Rapid escalating shake + glow build-up
        chestScale.animateTo(1.04f, tween(260))
        val shakeSteps = listOf(-12f, 12f, -15f, 15f, -18f, 18f, -14f, 14f, -8f, 8f, 0f)
        for ((index, target) in shakeSteps.withIndex()) {
            val dur = if (index < 6) 65 else 50
            shakeOffset.animateTo(target, tween(dur, easing = LinearEasing))
            shakeRotation.animateTo(target * 0.45f, tween(dur / 2, easing = LinearEasing))
            burstAlpha.animateTo((0.4f + index * 0.055f).coerceAtMost(1f), tween(dur))
        }
        // Final pop before loot reveal
        chestScale.animateTo(1.22f, tween(140, easing = FastOutLinearInEasing))
        delay(80)
        isRevealed = true
    }

    Dialog(
        onDismissRequest = { if (isRevealed) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xD9090514))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (!isRevealed) {
                        isRevealed = true
                    }
                }
                .testTag("chest_opening_dialog"),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                color = GameSurfaceDark,
                border = BorderStroke(2.dp, accentColor),
                shadowElevation = 20.dp
            ) {
                AnimatedContent(
                    targetState = isRevealed,
                    transitionSpec = {
                        (fadeIn(tween(280)) + scaleIn(initialScale = 0.86f, animationSpec = tween(320))) togetherWith
                            fadeOut(tween(180))
                    },
                    label = "chest_stage_transition"
                ) { revealed ->
                    if (!revealed) {
                        // Stage 1: Shaking chest & pulsing light burst
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = result.chestTitle,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = accentColor
                            )

                            Box(
                                modifier = Modifier.size(210.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val radius = (size.minDimension / 2f) * glowPulse
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                accentColor.copy(alpha = 0.65f * burstAlpha.value),
                                                accentColor.copy(alpha = 0.20f * burstAlpha.value),
                                                Color.Transparent
                                            ),
                                            center = center,
                                            radius = radius
                                        ),
                                        radius = radius,
                                        center = center
                                    )
                                    for (i in 0 until 8) {
                                        val rad = Math.toRadians((rayAngle + i * 45f).toDouble())
                                        val endX = center.x + (cos(rad) * radius * 0.92f).toFloat()
                                        val endY = center.y + (sin(rad) * radius * 0.92f).toFloat()
                                        drawLine(
                                            color = accentColor.copy(alpha = 0.35f * burstAlpha.value),
                                            start = center,
                                            end = Offset(endX, endY),
                                            strokeWidth = 6f
                                        )
                                    }
                                }

                                Image(
                                    painter = painterResource(id = result.chestImageRes),
                                    contentDescription = result.chestTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(126.dp)
                                        .graphicsLayer {
                                            translationX = shakeOffset.value
                                            rotationZ = shakeRotation.value
                                            scaleX = chestScale.value
                                            scaleY = chestScale.value
                                        }
                                        .clip(RoundedCornerShape(18.dp))
                                        .border(2.dp, accentColor, RoundedCornerShape(18.dp))
                                )
                            }

                            Text(
                                text = tr("Открываем сокровище...", "Opening treasure..."),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = GameGoldLight
                            )
                            Text(
                                text = tr("Нажмите, чтобы ускорить", "Tap to speed up"),
                                fontSize = 11.sp,
                                color = Color(0xFF9F92B8)
                            )
                        }
                    } else {
                        // Stage 2: Loot reveal with hero portrait/shards and/or artifact
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp, vertical = 22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "✨ ${result.chestTitle} ✨",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                color = accentColor,
                                textAlign = TextAlign.Center
                            )

                            if (result.isSecretMystery || result.heroImageRes != null) {
                                Box(
                                    modifier = Modifier.size(144.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val center = Offset(size.width / 2f, size.height / 2f)
                                        val radius = (size.minDimension / 2f) * glowPulse
                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    accentColor.copy(alpha = 0.55f),
                                                    accentColor.copy(alpha = 0.14f),
                                                    Color.Transparent
                                                ),
                                                center = center,
                                                radius = radius
                                            ),
                                            radius = radius,
                                            center = center
                                        )
                                    }

                                    val borderCircleColor = if (result.isSecretMystery) Color(0xFF4DD0E1) else GameGold
                                    Box(
                                        modifier = Modifier
                                            .size(110.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF281D42))
                                            .border(2.5.dp, borderCircleColor, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (result.isSecretMystery) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier.testTag("chest_secret_mystery_art")
                                            ) {
                                                Text(text = "❓", fontSize = 38.sp)
                                                Text(text = "✨🔒", fontSize = 14.sp)
                                            }
                                        } else if (result.heroImageRes != null) {
                                            HeroPortraitImage(
                                                imageRes = result.heroImageRes,
                                                heroId = result.heroId ?: "",
                                                heroName = result.heroName ?: "",
                                                contentDescription = localizeHeroNameByIdOrRu(
                                                    result.heroId,
                                                    result.heroName ?: tr("Героиня", "Heroine")
                                                ),
                                                contentScale = ContentScale.Crop,
                                                alignment = Alignment.TopCenter,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (result.isSecretMystery) Color(0xFF102636) else Color(0xFF2D2047),
                                    border = BorderStroke(
                                        1.2.dp,
                                        if (result.isSecretMystery) Color(0xFF4DD0E1) else GameGold
                                    )
                                ) {
                                    if (result.isSecretMystery) {
                                        Text(
                                            text = tr(
                                                "+${result.shardAmount} 🧩 Частицы: секретный персонаж",
                                                "+${result.shardAmount} 🧩 Shards: secret character"
                                            ),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF80DEEA),
                                            modifier = Modifier
                                                .padding(horizontal = 14.dp, vertical = 7.dp)
                                                .testTag("chest_secret_shards_label")
                                        )
                                    } else if (result.shardAmount > 0) {
                                        val localizedHeroName = localizeHeroNameByIdOrRu(result.heroId, result.heroName ?: "")
                                        Text(
                                            text = tr(
                                                "+${result.shardAmount} 🧩 Осколки: $localizedHeroName",
                                                "+${result.shardAmount} 🧩 Shards: $localizedHeroName"
                                            ),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = GameGoldLight,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                        )
                                    } else {
                                        val localizedHeroName = localizeHeroNameByIdOrRu(result.heroId, result.heroName ?: "")
                                        Text(
                                            text = tr(
                                                "✨ Открыт персонаж: $localizedHeroName!",
                                                "✨ Unlocked: $localizedHeroName!"
                                            ),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF80DEEA),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }

                            result.dropArtifact?.let { art ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFF271B3E),
                                    border = BorderStroke(1.5.dp, accentColor)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "${art.iconSymbol} ${art.localizedName()}",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "${art.rarity.localizedName()} • ${art.slot.localizedName()}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = accentColor
                                        )
                                        Text(
                                            text = art.localizedEffectDescription(),
                                            fontSize = 11.sp,
                                            color = Color(0xFFDDD2F0),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            if (!result.isSecretMystery && result.heroImageRes == null && result.shardAmount <= 0 && result.dropArtifact == null) {
                                Text(
                                    text = result.summaryMessage,
                                    fontSize = 13.sp,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GameGold,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("chest_opening_collect_button")
                            ) {
                                Text(
                                    text = tr("ЗАБРАТЬ", "COLLECT"),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
