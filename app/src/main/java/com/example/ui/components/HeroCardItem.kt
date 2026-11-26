package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.localizedName
import com.example.i18n.localizedTitle
import com.example.i18n.tr
import com.example.model.CardRarity
import com.example.model.HeroCard
import com.example.model.HeroType
import com.example.model.maxUpgradeLevel
import com.example.model.secretUnlockShardsTarget
import com.example.ui.theme.*

/**
 * Returns frame color based on card level:
 * - Level 1..4: Default Blue frame
 * - Level 5..9: Silver frame
 * - Level 10+: Golden frame
 */
fun getCardFrameColor(level: Int): Color {
    return when {
        level >= 10 -> Color(0xFFFFD54F) // Golden frame
        level >= 5 -> Color(0xFFE0E0E0)  // Silver frame
        else -> Color(0xFF29B6F6)        // Default Blue frame
    }
}

fun getRarityColor(rarity: CardRarity): Color {
    return when (rarity) {
        CardRarity.BASIC -> Color(0xFF90A4AE)
        CardRarity.SPECIAL -> Color(0xFF66BB6A)
        CardRarity.RARE -> Color(0xFFAB47BC)
        CardRarity.MYTHIC -> Color(0xFFFFD54F)
    }
}

@Composable
fun HeroCardItem(
    hero: HeroCard,
    allArtifacts: List<com.example.model.Artifact> = emptyList(),
    isSelectedInDeck: Boolean = false,
    showUpgradeIndicator: Boolean = true,
    onOpenSecretHero: () -> Unit = {},
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val frameColor = getCardFrameColor(hero.level)

    // Shimmer effect for Silver (level >= 5) and Gold (level >= 10)
    val infiniteTransition = rememberInfiniteTransition(label = "frame_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "frame_glow_alpha"
    )

    Card(
        modifier = modifier
            .testTag("hero_card_${hero.id}")
            .shadow(
                elevation = if (hero.level >= 5) 8.dp else 4.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = frameColor
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (hero.level >= 5) 2.5.dp else 1.5.dp,
            color = if (hero.level >= 5) frameColor.copy(alpha = glowAlpha) else frameColor
        ),
        colors = CardDefaults.cardColors(containerColor = GameCardBg)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Card Portrait / Mystery Art
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(topStart = 11.dp, topEnd = 11.dp))
                ) {
                    if (hero.isUnlocked) {
                        HeroPortraitImage(
                            imageRes = hero.imageRes,
                            heroId = hero.id,
                            heroName = hero.name,
                            heroLevel = hero.level,
                            contentDescription = hero.localizedName(),
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.TopCenter,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Gradient overlay for contrast
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color(0xCC0F0C1B)),
                                        startY = 60f
                                    )
                                )
                        )

                        // Hero Type Emoji in top-left & Element symbol in top-right
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = when (hero.heroType) {
                                    HeroType.MELEE -> "⚔️"
                                    HeroType.RANGED -> "🏹"
                                    HeroType.HYBRID -> "☯"
                                },
                                fontSize = 12.sp,
                                modifier = Modifier.align(Alignment.TopStart)
                            )

                            Text(
                                text = hero.element.symbol,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .testTag("hero_element_${hero.id}")
                            )
                        }
                    } else if (hero.isSecret) {
                        val unlockTarget = hero.secretUnlockShardsTarget()
                        val remainingShards = (unlockTarget - hero.shards).coerceAtLeast(0)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(Color(0xFF17364A), Color(0xFF0E0B1A))
                                    )
                                )
                                .padding(top = 18.dp, bottom = 18.dp, start = 6.dp, end = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (hero.shards >= unlockTarget) {
                                    Text(
                                        text = when (hero.id) {
                                            "vasilisa" -> "🏜️"
                                            "kadru" -> "🦎"
                                            else -> "❄️"
                                        },
                                        fontSize = 24.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Button(
                                        onClick = onOpenSecretHero,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF4DD0E1),
                                            contentColor = Color.Black
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier
                                            .defaultMinSize(minHeight = 28.dp)
                                            .testTag("open_secret_hero_${hero.id}")
                                    ) {
                                        Text(
                                            text = tr("Открыть", "Open"),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Black,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                } else {
                                    Text(text = "❓", fontSize = 26.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        color = Color(0xDD102636),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, Color(0xFF4DD0E1)),
                                        modifier = Modifier.testTag("secret_hero_remaining_shards_${hero.id}")
                                    ) {
                                        Text(
                                            text = tr("Осталось: $remainingShards 🧩", "Left: $remainingShards 🧩"),
                                            color = Color(0xFF80DEEA),
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Hidden Mystery Card (without revealing character identity)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(Color(0xFF2C1E4A), Color(0xFF100B1C))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(text = "❓", fontSize = 34.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = tr("Скрытая карта", "Hidden Card"),
                                    tint = Color(0xFFFFB74D),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Level & Star Badge + Upgrade-Ready indicator on the same bottom line
                    val canUpgradeLevel = showUpgradeIndicator &&
                        hero.isUnlocked &&
                        hero.level < hero.maxUpgradeLevel() &&
                        hero.shards >= hero.shardsNeeded

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = tr("Уровень", "Level"),
                                tint = frameColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = tr("Ур. ${hero.level}", "Lv. ${hero.level}"),
                                color = frameColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (canUpgradeLevel) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xD9102A18))
                                    .border(1.dp, Color(0xFF76FF03), CircleShape)
                                    .testTag("hero_upgrade_ready_${hero.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = tr("Доступно повышение уровня", "Level up available"),
                                    tint = Color(0xFF76FF03),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }

                // Card Footer with Name in Ribbon
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HeroNameRibbon(
                        name = if (hero.isUnlocked) hero.localizedName() else tr("Скрытая карта", "Hidden Card"),
                        frameColor = frameColor
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = if (hero.isUnlocked) hero.localizedTitle() else tr("Таинственная Героиня", "Mystery Heroine"),
                        color = Color(0xFF9E91B8),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    // 3 Stat Indicators: Total ATK, Total ARMOR, Total HP (with level + equipped artifacts!)
                    val totalAtk = hero.calculateTotalAtk(allArtifacts)
                    val totalArmor = hero.calculateTotalArmor(allArtifacts)
                    val totalHp = hero.calculateTotalHp(allArtifacts)

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF161124), RoundedCornerShape(6.dp))
                            .padding(vertical = 3.dp, horizontal = 3.dp)
                    ) {
                        val isCompactCard = maxWidth < 120.dp
                        val statIconSize = if (isCompactCard) 8.sp else 9.5.sp
                        val statTextSize = if (isCompactCard) 8.5.sp else 10.sp

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // ATK
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "⚔️", fontSize = statIconSize, maxLines = 1, softWrap = false)
                                Text(
                                    text = if (hero.isUnlocked) "$totalAtk" else "?",
                                    fontSize = statTextSize,
                                    fontWeight = FontWeight.Bold,
                                    color = StatAtkBlue,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            // ARMOR
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🛡️", fontSize = statIconSize, maxLines = 1, softWrap = false)
                                Text(
                                    text = if (hero.isUnlocked) "$totalArmor" else "?",
                                    fontSize = statTextSize,
                                    fontWeight = FontWeight.Bold,
                                    color = StatArmorSteel,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            // HP
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "❤️", fontSize = statIconSize, maxLines = 1, softWrap = false)
                                Text(
                                    text = if (hero.isUnlocked) "$totalHp" else "?",
                                    fontSize = statTextSize,
                                    fontWeight = FontWeight.Bold,
                                    color = StatHpRed,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            // "В КОЛОДЕ" or "ЗАКРЫТО" Ribbon at top
            if (isSelectedInDeck) {
                Surface(
                    color = Color(0xE62E7D32),
                    shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    Text(
                        text = tr("В КОЛОДЕ", "IN DECK"),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else if (!hero.isUnlocked) {
                Surface(
                    color = if (hero.isSecret) Color(0xE6006064) else Color(0xE64A148C),
                    shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    Text(
                        text = if (hero.isSecret) tr("СЕКРЕТ 🔒", "SECRET 🔒") else tr("ТАЙНА 🔒", "MYSTERY 🔒"),
                        color = if (hero.isSecret) Color(0xFF80DEEA) else Color(0xFFFFD54F),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HeroNameRibbon(
    name: String,
    frameColor: Color,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 11.sp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 1.dp)
            .shadow(3.dp, RoundedCornerShape(4.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF1E1233),
                        Color(0xFF38235C),
                        Color(0xFF4C2A78),
                        Color(0xFF38235C),
                        Color(0xFF1E1233)
                    )
                ),
                shape = RoundedCornerShape(4.dp)
            )
            .border(
                BorderStroke(1.dp, frameColor.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            color = Color.White,
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            letterSpacing = 0.3.sp
        )
    }
}

