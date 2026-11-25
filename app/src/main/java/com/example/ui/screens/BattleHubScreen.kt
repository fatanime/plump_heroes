package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.i18n.localizedName
import com.example.i18n.tr
import com.example.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Locale

enum class HubBattleSection {
    CAREER,
    ARENA,
    TOWER
}

@Composable
fun BattleHubScreen(
    profile: PlayerProfile,
    chapters: List<CampaignChapter>,
    isCampaign100Percent: Boolean,
    isDeckValid: Boolean,
    onOpenCareer: () -> Unit,
    onOpenArena: () -> Unit,
    onOpenColiseum: () -> Unit,
    onOpenTower: () -> Unit,
    onFixDeck: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            currentTime = System.currentTimeMillis()
        }
    }

    // Career live stats
    val totalStages = remember(chapters) { chapters.sumOf { it.stages.size }.coerceAtLeast(24) }
    val clearedStages = remember(chapters) { chapters.sumOf { ch -> ch.stages.count { it.stars > 0 } } }
    val totalStars = remember(chapters) { chapters.sumOf { ch -> ch.stages.sumOf { it.stars } } }
    val maxStars = totalStages * 4
    val latestUnlockedStage = remember(chapters, profile.level) {
        chapters.flatMap { it.stages }
            .lastOrNull { it.isUnlocked && profile.level >= it.requiredPlayerLevel }
            ?: chapters.firstOrNull()?.stages?.firstOrNull()
    }
    val careerTopBadge = if (isCampaign100Percent) {
        "★ 100% ($totalStars/$maxStars)"
    } else {
        val chNum = latestUnlockedStage?.chapterId ?: 1
        tr("Гл. $chNum • $clearedStages/$totalStages", "Ch. $chNum • $clearedStages/$totalStages")
    }
    val careerBottomChip = if (isCampaign100Percent) {
        tr("Все главы пройдены", "All chapters cleared")
    } else {
        latestUnlockedStage?.let {
            tr("Этап ${it.chapterId}-${it.stageNumber}", "Stage ${it.chapterId}-${it.stageNumber}")
        } ?: tr("Начать путь", "Start path")
    }

    // Arena live stats
    val arenaPeriodMs = 2 * 3600 * 1000L
    val timeToNextKeyMs = if (profile.arenaKeys < 5) {
        val elapsed = (currentTime - profile.lastArenaKeyRegenTimestamp).coerceAtLeast(0L)
        (arenaPeriodMs - (elapsed % arenaPeriodMs)).coerceAtLeast(0L)
    } else 0L
    val arenaTimerFormatted = formatHms(timeToNextKeyMs)
    val arenaTopBadge = "🔑 ${profile.arenaKeys}/5 • 🏆 ${profile.trophies}"
    val arenaBottomChip = if (profile.arenaKeys < 5) {
        tr("+1 ключ через $arenaTimerFormatted", "+1 key in $arenaTimerFormatted")
    } else {
        tr("Полный запас ключей", "Full keys ready")
    }

    // Coliseum live stats
    val isColiseumUnlocked = profile.level >= 5
    val coliseumEntries = remember(
        currentTime,
        profile.coliseumEntriesDate,
        profile.coliseumEntriesUsedToday,
        profile.lastColiseumTimestamp,
        profile.maxSeenTimestamp
    ) { computeColiseumRemainingEntries(profile, currentTime) }
    val coliseumCooldownMs = remember(
        currentTime,
        profile.coliseumEntriesDate,
        profile.coliseumEntriesUsedToday,
        profile.lastColiseumTimestamp,
        profile.maxSeenTimestamp
    ) { computeColiseumCooldownRemainingMs(profile, currentTime) }
    val coliseumTopBadge = when {
        !isColiseumUnlocked -> tr("🔒 Ур. 5", "🔒 Lv. 5")
        coliseumEntries > 0 -> tr("⚔️ $coliseumEntries/$COLISEUM_DAILY_ENTRY_LIMIT", "⚔️ $coliseumEntries/$COLISEUM_DAILY_ENTRY_LIMIT")
        else -> "⏳ ${formatHms(coliseumCooldownMs)}"
    }
    val coliseumBottomChip = when {
        !isColiseumUnlocked -> tr("Откроется на 5 ур.", "Unlocks at Lv. 5")
        coliseumEntries > 0 -> tr("Попыток сегодня: $coliseumEntries/$COLISEUM_DAILY_ENTRY_LIMIT", "Today: $coliseumEntries/$COLISEUM_DAILY_ENTRY_LIMIT")
        else -> tr("Сброс: ${formatHms(coliseumCooldownMs)}", "Reset: ${formatHms(coliseumCooldownMs)}")
    }

    // Daily Tower live stats
    val isTowerUnlocked = profile.level >= TOWER_MIN_PLAYER_LEVEL
    val towerRule = remember(currentTime) { resolveTowerDayRule(currentTime) }
    val wednesdayElement = remember(currentTime) { resolveWednesdayTowerElement(currentTime) }
    val towerClearedToday = remember(
        currentTime,
        profile.towerProgressDate,
        profile.towerClearedFloorToday,
        profile.maxSeenTimestamp
    ) { computeTowerClearedFloorToday(profile, currentTime) }
    val towerRemainingAttempts = remember(
        currentTime,
        profile.towerProgressDate,
        profile.towerAttemptsUsedToday,
        profile.maxSeenTimestamp
    ) { computeTowerRemainingAttempts(profile, currentTime) }
    val towerResetMs = remember(currentTime) { computeTowerResetRemainingMs(currentTime) }
    val towerTopBadge = when {
        !isTowerUnlocked -> tr("🔒 Ур. $TOWER_MIN_PLAYER_LEVEL", "🔒 Lv. $TOWER_MIN_PLAYER_LEVEL")
        towerClearedToday >= TOWER_TOTAL_FLOORS -> "${towerRule.iconSymbol} $TOWER_TOTAL_FLOORS/$TOWER_TOTAL_FLOORS ✓"
        else -> "${towerRule.iconSymbol} $towerClearedToday/$TOWER_TOTAL_FLOORS • ⚔️ $towerRemainingAttempts/$TOWER_DAILY_ATTEMPT_LIMIT"
    }
    val towerRuleSubtitle = if (towerRule == TowerDayRule.WEDNESDAY_ELEMENTAL_STORM) {
        "${tr(towerRule.titleRu, towerRule.titleEn)} (${wednesdayElement.symbol} ${wednesdayElement.localizedName()})"
    } else {
        tr(towerRule.titleRu, towerRule.titleEn)
    }
    val towerBottomChip = when {
        !isTowerUnlocked -> tr("Откроется на $TOWER_MIN_PLAYER_LEVEL ур.", "Unlocks at Lv. $TOWER_MIN_PLAYER_LEVEL")
        towerClearedToday >= TOWER_TOTAL_FLOORS -> tr("Пройдено • ${formatHms(towerResetMs)}", "Cleared • ${formatHms(towerResetMs)}")
        towerRemainingAttempts <= 0 -> tr("Попытки: 0/$TOWER_DAILY_ATTEMPT_LIMIT • ⏳ ${formatHms(towerResetMs)}", "Attempts: 0/$TOWER_DAILY_ATTEMPT_LIMIT • ⏳ ${formatHms(towerResetMs)}")
        else -> "${tr(towerRule.shortBadgeRu, towerRule.shortBadgeEn)} • ${formatHms(towerResetMs)}"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GameBgDark)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Citadel Hero Header
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = tr("Цитадель Аврезии", "Citadel of Avresia"),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = GameGoldLight
            )
            Text(
                text = tr(
                    "Выберите боевой режим для отряда и завоюйте награды",
                    "Choose a battleground for your squad and claim glory"
                ),
                fontSize = 11.5.sp,
                color = Color(0xFFB5A7CE)
            )
        }

        if (!isDeckValid) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF3A1624),
                border = BorderStroke(1.2.dp, Color(0xFFE57373)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onFixDeck() }
                    .testTag("hub_incomplete_deck_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = tr(
                            "⚠️ Соберите 4 героини в отряде для входа в бой",
                            "⚠️ Assign 4 heroines to your squad before fighting"
                        ),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFCDD2),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tr("Отряд →", "Squad →"),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = GameGoldLight
                    )
                }
            }
        }

        // 2x2 Grid of Large Fantasy Location Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BattleHubLocationCard(
                title = tr("Карьера", "Career"),
                subtitle = tr("Главы, звёзды и артефакты", "Chapters, stars & artifacts"),
                imageRes = R.drawable.img_hub_career,
                topBadge = careerTopBadge,
                topBadgeColor = if (isCampaign100Percent) GameGold else Color(0xFF2B4C3B),
                topBadgeTextColor = if (isCampaign100Percent) Color.Black else Color(0xFFA5D6A7),
                bottomChipText = careerBottomChip,
                accentColor = Color(0xFFFFA726),
                onClick = onOpenCareer,
                modifier = Modifier
                    .weight(1f)
                    .testTag("hub_card_career")
            )

            BattleHubLocationCard(
                title = tr("Арена", "Arena"),
                subtitle = tr("Дуэли с отрядами за трофеи", "Squad duels for trophies"),
                imageRes = R.drawable.img_hub_arena,
                topBadge = arenaTopBadge,
                topBadgeColor = if (profile.arenaKeys > 0) Color(0xFF2D2047) else Color(0xFF4A1824),
                topBadgeTextColor = if (profile.arenaKeys > 0) GameGoldLight else Color(0xFFFFCDD2),
                bottomChipText = arenaBottomChip,
                accentColor = GameGold,
                onClick = onOpenArena,
                modifier = Modifier
                    .weight(1f)
                    .testTag("hub_card_arena")
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BattleHubLocationCard(
                title = tr("Колизей", "Coliseum"),
                subtitle = tr("Бесконечные волны на выживание", "Endless survival wave gauntlet"),
                imageRes = R.drawable.img_hub_coliseum,
                topBadge = coliseumTopBadge,
                topBadgeColor = when {
                    !isColiseumUnlocked -> Color(0xFF282038)
                    coliseumEntries > 0 -> Color(0xFF4A1C2A)
                    else -> Color(0xFF282038)
                },
                topBadgeTextColor = when {
                    !isColiseumUnlocked -> Color(0xFFB5A7CE)
                    coliseumEntries > 0 -> Color(0xFFFFD54F)
                    else -> Color(0xFFCE93D8)
                },
                bottomChipText = coliseumBottomChip,
                accentColor = Color(0xFFEF5350),
                isLockedOverlay = !isColiseumUnlocked,
                onClick = onOpenColiseum,
                modifier = Modifier
                    .weight(1f)
                    .testTag("hub_card_coliseum")
            )

            BattleHubLocationCard(
                title = tr("Башня Дня", "Trial Tower"),
                subtitle = towerRuleSubtitle,
                imageRes = R.drawable.img_hub_tower,
                topBadge = towerTopBadge,
                topBadgeColor = when {
                    !isTowerUnlocked -> Color(0xFF282038)
                    towerClearedToday >= TOWER_TOTAL_FLOORS -> Color(0xFF1F4B38)
                    towerRemainingAttempts <= 0 -> Color(0xFF4A1824)
                    else -> Color(0xFF2F1F52)
                },
                topBadgeTextColor = when {
                    !isTowerUnlocked -> Color(0xFFB5A7CE)
                    towerClearedToday >= TOWER_TOTAL_FLOORS -> Color(0xFFA5D6A7)
                    towerRemainingAttempts <= 0 -> Color(0xFFFFCDD2)
                    else -> Color(0xFFE1BEE7)
                },
                bottomChipText = towerBottomChip,
                accentColor = Color(0xFFAB47BC),
                isLockedOverlay = !isTowerUnlocked,
                onClick = onOpenTower,
                modifier = Modifier
                    .weight(1f)
                    .testTag("hub_card_tower")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun BattleHubLocationCard(
    title: String,
    subtitle: String,
    imageRes: Int,
    topBadge: String,
    topBadgeColor: Color,
    topBadgeTextColor: Color,
    bottomChipText: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLockedOverlay: Boolean = false
) {
    Card(
        modifier = modifier
            .aspectRatio(0.75f)
            .heightIn(min = 215.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.72f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = GameCardBg)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Top vignette for badge legibility
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xCC0B0714),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Bottom dramatic scrim for title + status chip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.58f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xCC110B1E),
                                Color(0xF50E0919)
                            )
                        )
                    )
            )

            if (isLockedOverlay) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x55090612))
                )
            }

            // Live Top Status Badge
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = topBadgeColor.copy(alpha = 0.94f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.85f)),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = topBadge,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = topBadgeTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Bottom Info Block
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 10.5.sp,
                    lineHeight = 13.sp,
                    color = Color(0xFFD5CBE9),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xDD1C1330),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.65f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = bottomChipText,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = GameGoldLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

private fun formatHms(ms: Long): String {
    val safe = ms.coerceAtLeast(0L)
    val h = safe / 3600_000L
    val m = (safe % 3600_000L) / 60_000L
    val s = (safe % 60_000L) / 1000L
    return String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
}
