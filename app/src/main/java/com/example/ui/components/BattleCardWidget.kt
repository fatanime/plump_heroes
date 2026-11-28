package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.localizedName
import com.example.i18n.tr
import com.example.model.AttackAnimation
import com.example.model.BattleCard
import com.example.model.CombatTextType
import com.example.model.FloatingCombatText
import com.example.model.HeroType
import com.example.ui.theme.*

@Composable
fun BattleCardWidget(
    card: BattleCard?,
    isSelectedAttacker: Boolean,
    isValidTarget: Boolean,
    onClick: () -> Unit,
    combatTexts: List<FloatingCombatText> = emptyList(),
    attackAnimation: AttackAnimation? = null,
    dotHitTargets: Set<Pair<Boolean, Int>> = emptySet(),
    teamMana: Int = 0,
    modifier: Modifier = Modifier
) {
    if (card == null) {
        // Empty slot placeholder (scaled down 15%: 88x112 dp)
        Box(
            modifier = modifier
                .size(width = 88.dp, height = 112.dp)
                .background(Color(0x331E152A), RoundedCornerShape(10.dp))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = tr("Пусто", "Empty"), color = Color(0x55FFFFFF), fontSize = 11.sp, maxLines = 1, softWrap = false)
        }
        return
    }

    val isAttacker = card.isAlive && attackAnimation != null && attackAnimation.attackerSlot == card.slotIndex && attackAnimation.isPlayerAttacker == card.isPlayer
    val isTargetHit = card.isAlive && attackAnimation != null && attackAnimation.targetSlots.contains(card.slotIndex) && attackAnimation.isPlayerAttacker != card.isPlayer
    val isDotHit = card.isAlive && dotHitTargets.contains(card.isPlayer to card.slotIndex)

    // Lunge animation (towards enemy)
    val lungeOffsetY by animateDpAsState(
        targetValue = if (isAttacker) (if (card.isPlayer) (-22).dp else 22.dp) else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "lunge_offset_y"
    )

    // Impact shake animation (multi-target & DOT damage shakes simultaneously)
    val shakeOffsetX by animateDpAsState(
        targetValue = if (isTargetHit || isDotHit) 5.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessHigh),
        label = "shake_offset_x"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "target_pulse"
    )

    val frameColor = getCardFrameColor(card.level)
    val borderStroke = when {
        isTargetHit || isDotHit -> BorderStroke(3.dp, Color(0xFFFF5252))
        isValidTarget && card.isPlayer -> BorderStroke(3.dp, Color(0xFF66BB6A))
        isSelectedAttacker -> BorderStroke(3.dp, GameGold)
        isValidTarget -> BorderStroke(3.dp, Color(0xFFFF1744))
        !card.isAlive -> BorderStroke(1.dp, Color(0x44444444))
        card.hasSkipTurnEffect -> BorderStroke(1.5.dp, Color(0x8890A4AE))
        card.hasActedThisRound -> BorderStroke(1.dp, Color(0x44666666))
        else -> BorderStroke(if (card.level >= 5) 2.2.dp else 1.5.dp, frameColor)
    }

    // Main Card Box (92.dp x 122.dp)
    Box(
        modifier = modifier
            .testTag(if (card.isPlayer) "player_slot_${card.slotIndex}" else "enemy_slot_${card.slotIndex}")
            .offset(x = shakeOffsetX, y = lungeOffsetY)
            .size(width = 92.dp, height = 122.dp)
            .scale(if (isValidTarget) pulseScale else if (isAttacker) 1.08f else 1f)
            .clickable(enabled = card.isAlive && (!card.isPlayer || !card.hasSkipTurnEffect || isValidTarget), onClick = onClick),
        contentAlignment = Alignment.TopCenter
    ) {
        // Main Card Body
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (!card.isAlive) 0.35f else if (card.hasSkipTurnEffect) 0.55f else if (card.hasActedThisRound) 0.65f else 1f),
            shape = RoundedCornerShape(10.dp),
            border = borderStroke,
            color = GameCardBg,
            shadowElevation = if (isSelectedAttacker || isAttacker) 10.dp else 3.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Hero Portrait
                HeroPortraitImage(
                    imageRes = card.imageRes,
                    heroId = card.heroId,
                    heroName = card.name,
                    heroLevel = card.level,
                    contentDescription = card.localizedName(),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(9.dp))
                )

                // Dark vignette on bottom
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xF00B0914)),
                                startY = 50f
                            )
                        )
                )

                // Top row: Type icon & Status icons (left), Skill Ready lightning (right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Type icon and attached status effects in top left
                    Column(horizontalAlignment = Alignment.Start) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = when (card.heroType) {
                                    HeroType.MELEE -> "⚔️"
                                    HeroType.RANGED -> "🏹"
                                    HeroType.HYBRID -> "☯"
                                },
                                fontSize = 11.sp
                            )
                            if (card.isAlive && card.isFrozen) {
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(text = "🥶", fontSize = 11.sp)
                            }
                            if (card.isAlive && card.isBlinded) {
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(text = "😵💫", fontSize = 10.sp)
                            }
                            if (card.isAlive && card.isCharmed) {
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(text = "💋", fontSize = 11.sp)
                            }
                            if (card.isAlive && card.burnRounds > 0) {
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(text = "🔥", fontSize = 11.sp)
                            }
                            if (card.isAlive && card.isStunned) {
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(text = "💫", fontSize = 11.sp)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (card.isAlive && card.isWeakened) {
                                Text(text = "🤕", fontSize = 10.sp)
                            }
                            if (card.isAlive && card.isHealingBlocked) {
                                if (card.isWeakened) Spacer(modifier = Modifier.width(2.dp))
                                Text(text = "💛", fontSize = 10.sp)
                            }
                            if (card.isAlive && card.atkBoostPercent > 0) {
                                if (card.isWeakened || card.isHealingBlocked) Spacer(modifier = Modifier.width(2.dp))
                                Text(text = "💢", fontSize = 10.sp)
                            }
                        }
                    }

                    // Top-right: Skill Ready lightning + Element badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (card.isAlive && card.skill != null && card.hasMagicUnlocked) {
                            val canCast = teamMana >= card.skill.energyCost
                            if (canCast) {
                                Text(
                                    text = "⚡",
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Text(
                            text = card.element.symbol,
                            fontSize = 11.sp
                        )
                    }
                }

                // Name Ribbon (Scaled proportionally with reduced neat spacing)
                HeroNameRibbon(
                    name = card.localizedName(),
                    frameColor = frameColor,
                    fontSize = 9.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 9.dp, start = 2.dp, end = 2.dp)
                )

                // Defeated X overlay
                if (!card.isAlive) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x99000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = tr("Повержен", "Defeated"),
                            tint = Color.Red,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }

        // Circular Pedestal Stats at Bottom (ATK, ARMOR, HP) - scaled to 19.dp (-5%)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 8.dp)
                .background(Color(0xEE161025), RoundedCornerShape(10.dp))
                .border(1.dp, Color(0xFF4A3D66), RoundedCornerShape(10.dp))
                .padding(horizontal = 3.5.dp, vertical = 1.5.dp),
            horizontalArrangement = Arrangement.spacedBy(3.5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ATK Circle
            val displayAtk = (card.atk * (1.0 + card.atkBoostPercent / 100.0)).toInt().coerceAtLeast(1)
            Surface(
                shape = CircleShape,
                color = Color(0xFF1E3A5F),
                border = BorderStroke(1.dp, if (card.atkBoostPercent > 0) Color(0xFFFFD54F) else StatAtkBlue),
                modifier = Modifier.size(19.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "$displayAtk",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = if (card.atkBoostPercent > 0) Color(0xFFFFD54F) else Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // ARMOR Circle (Shield)
            Surface(
                shape = CircleShape,
                color = Color(0xFF37474F),
                border = BorderStroke(1.dp, StatArmorSteel),
                modifier = Modifier.size(19.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "${card.currentArmor}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = if (card.currentArmor > 0) Color.White else Color(0xFF888888),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // HP Circle (permanently yellow when Kadru healing-block is active; else green poison / orange burn / red)
            val hpCircleColor = when {
                card.isHealingBlocked -> Color(0xFFF9A825)
                card.poisonRounds > 0 -> Color(0xFF2E7D32)
                card.burnRounds > 0 -> Color(0xFFE65100)
                else -> Color(0xFFB71C1C)
            }
            val hpBorderColor = when {
                card.isHealingBlocked -> Color(0xFFFFEE58)
                card.poisonRounds > 0 -> Color(0xFF66BB6A)
                card.burnRounds > 0 -> Color(0xFFFF9800)
                else -> StatHpRed
            }
            Surface(
                shape = CircleShape,
                color = hpCircleColor,
                border = BorderStroke(1.dp, hpBorderColor),
                modifier = Modifier.size(19.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "${card.currentHp}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = if (card.isHealingBlocked) Color.Black else Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // Target Indicator Marker on top
        if (isValidTarget) {
            Surface(
                shape = CircleShape,
                color = Color(0xE6FF1744),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-6).dp)
                    .size(18.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "🎯", fontSize = 10.sp)
                }
            }
        }

        // Floating combat texts (never show on dead cards!)
        val cardCombatTexts = if (!card.isAlive) emptyList() else combatTexts.filter { it.slotIndex == card.slotIndex && it.isPlayer == card.isPlayer }
        val hasShield = cardCombatTexts.any { it.type == CombatTextType.SHIELD_BLOCK || it.type == CombatTextType.SHIELD_BOOST }
        val hasDmgOrHeal = cardCombatTexts.any { it.type == CombatTextType.DAMAGE || it.type == CombatTextType.CRIT_DAMAGE || it.type == CombatTextType.HEAL }

        cardCombatTexts.forEach { combatText ->
            val isShield = combatText.type == CombatTextType.SHIELD_BLOCK || combatText.type == CombatTextType.SHIELD_BOOST
            val isDmgOrHeal = combatText.type == CombatTextType.DAMAGE || combatText.type == CombatTextType.CRIT_DAMAGE || combatText.type == CombatTextType.HEAL
            val isDoT = combatText.type == CombatTextType.POISON || combatText.type == CombatTextType.BURN

            val yOffset = when {
                isShield && hasDmgOrHeal -> 20.dp
                isShield -> 30.dp
                isDmgOrHeal && hasShield -> 38.dp
                isDmgOrHeal -> 30.dp
                isDoT -> 52.dp // Placed on a row below main damage
                else -> 30.dp
            }

            FloatingTextBadge(combatText = combatText, xOffset = 0.dp, yOffsetBase = yOffset)
        }
    }
}

@Composable
fun FloatingTextBadge(
    combatText: FloatingCombatText,
    xOffset: androidx.compose.ui.unit.Dp = 0.dp,
    yOffsetBase: androidx.compose.ui.unit.Dp = 30.dp
) {
    val floatY by animateFloatAsState(
        targetValue = yOffsetBase.value - 14f,
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "combat_text_y"
    )

    val textColor = when (combatText.type) {
        CombatTextType.CRIT_DAMAGE -> Color(0xFFFFD54F).copy(alpha = 0.85f) // Yellow font for crit
        CombatTextType.DAMAGE -> Color(0xFFFF3D00).copy(alpha = 0.85f)      // Red font for HP damage
        CombatTextType.SHIELD_BLOCK,
        CombatTextType.SHIELD_BOOST -> Color(0xFFECEFF1).copy(alpha = 0.85f) // White/Silver font for shield
        CombatTextType.HEAL -> Color(0xFF4CAF50).copy(alpha = 0.85f)        // Green font for HP heal
        CombatTextType.POISON -> Color(0xFFBA68C8).copy(alpha = 0.85f)      // Purple font for poison
        CombatTextType.BURN -> Color(0xFFFF9800).copy(alpha = 0.85f)        // Orange font for burn
        else -> Color.White.copy(alpha = 0.85f)
    }

    val isSmallSubline = combatText.type == CombatTextType.POISON || combatText.type == CombatTextType.BURN
    val isSkillBadge = combatText.type == CombatTextType.SKILL

    if (!isSkillBadge) {
        Box(
            modifier = Modifier
                .offset(x = xOffset, y = floatY.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = combatText.text,
                color = textColor,
                fontWeight = FontWeight.Black,
                fontSize = if (isSmallSubline) 9.sp else 16.sp,
                maxLines = 1,
                softWrap = false
            )
        }
    } else {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xF27B1FA2).copy(alpha = 0.75f),
            modifier = Modifier
                .offset(x = xOffset, y = floatY.dp)
        ) {
            Text(
                text = combatText.text,
                color = Color.White.copy(alpha = 0.85f),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 10.sp,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
            )
        }
    }
}

