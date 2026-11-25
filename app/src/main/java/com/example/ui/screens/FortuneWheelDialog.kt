package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.i18n.localizedLabel
import com.example.i18n.tr
import com.example.model.FortuneWheelOffer
import com.example.model.FortuneWheelResult
import com.example.model.FortuneWheelSector
import com.example.model.FortuneWheelSectorType
import com.example.ui.theme.GameGold
import com.example.ui.theme.GameGoldLight
import com.example.ui.theme.GameSurfaceDark
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun FortuneWheelDialog(
    offer: FortuneWheelOffer,
    result: FortuneWheelResult?,
    onSpinComplete: (FortuneWheelSector) -> Unit,
    onDismiss: () -> Unit
) {
    val sectors = offer.sectors
    val rotation = remember(offer.dayNumber) { Animatable(0f) }
    var isSpinning by remember(offer.dayNumber) { mutableStateOf(false) }
    var hasSpun by remember(offer.dayNumber) { mutableStateOf(result != null) }
    val scope = rememberCoroutineScope()

    val sliceColors = listOf(
        Color(0xFF7B1FA2),
        Color(0xFF1565C0),
        Color(0xFF37474F),
        Color(0xFF00838F),
        Color(0xFF2E7D32),
        Color(0xFFE65100),
        Color(0xFF6A1B9A),
        Color(0xFF4527A0),
        Color(0xFF0277BD),
        Color(0xFFAD1457)
    )

    Dialog(
        onDismissRequest = {
            if (!isSpinning) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !isSpinning,
            dismissOnClickOutside = !isSpinning && hasSpun
        )
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = GameSurfaceDark,
            border = BorderStroke(2.dp, GameGold),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("fortune_wheel_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = tr("🎡 Колесо Фортуны", "🎡 Wheel of Fortune"),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = GameGoldLight,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = tr(
                        "День ${offer.dayNumber} подряд! Испытайте удачу — крутить можно 1 раз в день.",
                        "Day ${offer.dayNumber} in a row! Try your luck — you can spin once a day."
                    ),
                    fontSize = 11.5.sp,
                    color = Color(0xFFD9CEE8),
                    textAlign = TextAlign.Center
                )

                // Spinning Wheel + Top Pointer
                Box(
                    modifier = Modifier
                        .size(270.dp)
                        .padding(top = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .size(248.dp)
                            .align(Alignment.Center)
                    ) {
                        val count = sectors.size.coerceAtLeast(1)
                        val sweep = 360f / count
                        val radius = size.minDimension / 2f
                        val center = Offset(size.width / 2f, size.height / 2f)

                        rotate(degrees = rotation.value, pivot = center) {
                            sectors.forEachIndexed { index, sector ->
                                val startAngle = -90f - (sweep / 2f) + index * sweep
                                val fillColor = when (sector.type) {
                                    FortuneWheelSectorType.EMPTY -> Color(0xFF2B223B)
                                    FortuneWheelSectorType.SECRET_SHARDS_3 -> Color(0xFF00ACC1)
                                    else -> sliceColors[index % sliceColors.size]
                                }
                                val borderColor = if (sector.type == FortuneWheelSectorType.SECRET_SHARDS_3) {
                                    Color(0xFF80DEEA)
                                } else {
                                    Color(0xCCFFD54F)
                                }

                                drawArc(
                                    color = fillColor,
                                    startAngle = startAngle,
                                    sweepAngle = sweep,
                                    useCenter = true,
                                    topLeft = Offset.Zero,
                                    size = Size(size.width, size.height)
                                )
                                drawArc(
                                    color = borderColor,
                                    startAngle = startAngle,
                                    sweepAngle = sweep,
                                    useCenter = true,
                                    topLeft = Offset.Zero,
                                    size = Size(size.width, size.height),
                                    style = Stroke(width = if (sector.type == FortuneWheelSectorType.SECRET_SHARDS_3) 4.5f else 2.5f)
                                )

                                // Label inside slice
                                val midAngleRad = Math.toRadians((startAngle + sweep / 2f).toDouble())
                                val textRadius = radius * 0.64f
                                val tx = (center.x + textRadius * cos(midAngleRad)).toFloat()
                                val ty = (center.y + textRadius * sin(midAngleRad)).toFloat()

                                drawContext.canvas.nativeCanvas.apply {
                                    save()
                                    translate(tx, ty)
                                    rotate((startAngle + sweep / 2f) + 90f)
                                    val emojiPaint = android.graphics.Paint().apply {
                                        color = android.graphics.Color.WHITE
                                        textSize = 28f
                                        textAlign = android.graphics.Paint.Align.CENTER
                                        isAntiAlias = true
                                    }
                                    val labelPaint = android.graphics.Paint().apply {
                                        color = android.graphics.Color.parseColor("#FFF8E1")
                                        textSize = 21f
                                        isFakeBoldText = true
                                        textAlign = android.graphics.Paint.Align.CENTER
                                        isAntiAlias = true
                                    }
                                    drawText(sector.emoji, 0f, -6f, emojiPaint)
                                    drawText(sector.localizedLabel(), 0f, 20f, labelPaint)
                                    restore()
                                }
                            }
                        }

                        // Outer golden rim
                        drawCircle(
                            color = GameGold,
                            radius = radius,
                            center = center,
                            style = Stroke(width = 7f)
                        )
                        // Center hub
                        drawCircle(
                            color = Color(0xFF1A102B),
                            radius = radius * 0.16f,
                            center = center
                        )
                        drawCircle(
                            color = GameGold,
                            radius = radius * 0.16f,
                            center = center,
                            style = Stroke(width = 5f)
                        )
                    }

                    // Top downward pointer arrow
                    Canvas(
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.TopCenter)
                    ) {
                        val path = Path().apply {
                            moveTo(size.width / 2f, size.height)
                            lineTo(0f, 0f)
                            lineTo(size.width, 0f)
                            close()
                        }
                        drawPath(path = path, color = GameGold)
                        drawPath(path = path, color = Color(0xFF2B1700), style = Stroke(width = 3f))
                    }
                }

                // Result Card after spin
                result?.let { res ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (res.isEmpty) Color(0xFF2A1D38) else Color(0xFF223328),
                        border = BorderStroke(1.5.dp, if (res.isEmpty) Color(0xFF7B679A) else GameGold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("fortune_wheel_result")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (res.isEmpty) tr("Увы, пустой сектор!", "Alas, empty sector!") else tr("🎉 Поздравляем!", "🎉 Congratulations!"),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (res.isEmpty) Color(0xFFCEBEE6) else GameGoldLight
                            )
                            Text(
                                text = res.description,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Action Button
                if (result == null) {
                    Button(
                        onClick = {
                            if (!isSpinning && !hasSpun && sectors.isNotEmpty()) {
                                isSpinning = true
                                hasSpun = true
                                val winIndex = Random.nextInt(sectors.size)
                                val winningSector = sectors[winIndex]
                                val slice = 360f / sectors.size
                                val offsetInSlice = Random.nextFloat() * (slice * 0.55f) - (slice * 0.275f)
                                val targetMod = ((360f - winIndex * slice + offsetInSlice) % 360f + 360f) % 360f
                                val targetAngle = 360f * 6 + targetMod
                                scope.launch {
                                    rotation.snapTo(0f)
                                    rotation.animateTo(
                                        targetValue = targetAngle,
                                        animationSpec = tween(
                                            durationMillis = 3200,
                                            easing = FastOutSlowInEasing
                                        )
                                    )
                                    isSpinning = false
                                    onSpinComplete(winningSector)
                                }
                            }
                        },
                        enabled = !isSpinning && !hasSpun,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GameGold,
                            contentColor = Color.Black,
                            disabledContainerColor = Color(0xFF4A3D63),
                            disabledContentColor = Color(0xFFA89BBF)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("spin_fortune_wheel_button")
                    ) {
                        Text(
                            text = if (isSpinning) tr("Колесо вращается...", "Spinning...") else tr("КРУТИТЬ КОЛЕСО", "SPIN WHEEL"),
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GameGold,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("close_fortune_wheel_button")
                    ) {
                        Text(
                            text = tr("ЗАБРАТЬ И ЗАКРЫТЬ", "COLLECT & CLOSE"),
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
