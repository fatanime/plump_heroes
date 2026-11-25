package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.i18n.localizedName
import com.example.i18n.tr
import com.example.model.*
import com.example.ui.components.AutoShrinkText
import com.example.ui.components.HeroCardItem
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun TowerScreen(
    profile: PlayerProfile,
    allHeroes: List<HeroCard>,
    artifacts: List<Artifact>,
    towerDeckHeroIds: List<String?>,
    floors: List<TowerFloorPreview>,
    roofFloor: TowerFloorPreview,
    isDeckValid: Boolean,
    isDeckEligibleForRule: Boolean,
    showTutorial: Boolean = false,
    onBackToHub: () -> Unit,
    onAssignHeroToSlot: (Int, String?) -> Unit,
    onInspectHero: (HeroCard) -> Unit,
    onStartFloor: (TowerFloorPreview) -> Unit,
    onCompleteTutorial: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showWeeklySchedule by remember { mutableStateOf(false) }
    var isEditingSquad by remember { mutableStateOf(false) }
    var pickerSlotIndex by remember { mutableStateOf<Int?>(null) }
    var towerTutorialStep by remember(showTutorial) { mutableIntStateOf(1) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            currentTime = System.currentTimeMillis()
        }
    }

    BackHandler(enabled = isEditingSquad) {
        if (pickerSlotIndex != null) {
            pickerSlotIndex = null
        } else {
            isEditingSquad = false
        }
    }

    val rule = remember(currentTime) { resolveTowerDayRule(currentTime) }
    val wednesdayElement = remember(currentTime) { resolveWednesdayTowerElement(currentTime) }
    val clearedFloorToday = remember(
        currentTime,
        profile.towerProgressDate,
        profile.towerClearedFloorToday,
        profile.maxSeenTimestamp
    ) { computeTowerClearedFloorToday(profile, currentTime) }
    val floorStarsToday = remember(
        currentTime,
        profile.towerProgressDate,
        profile.towerFloorStarsToday,
        profile.maxSeenTimestamp
    ) { computeTowerFloorStarsToday(profile, currentTime) }
    val isRoofUnlocked = remember(
        currentTime,
        profile.towerProgressDate,
        profile.towerClearedFloorToday,
        profile.towerFloorStarsToday,
        profile.maxSeenTimestamp
    ) { isTowerRoofUnlockedToday(profile, currentTime) }
    val isRoofCleared = remember(
        currentTime,
        profile.towerProgressDate,
        profile.towerRoofClearedToday,
        profile.maxSeenTimestamp
    ) { isTowerRoofClearedToday(profile, currentTime) }
    val roofStars = remember(
        currentTime,
        profile.towerProgressDate,
        profile.towerRoofStarsToday,
        profile.maxSeenTimestamp
    ) { computeTowerRoofStarsToday(profile, currentTime) }
    val remainingAttempts = remember(
        currentTime,
        profile.towerProgressDate,
        profile.towerAttemptsUsedToday,
        profile.maxSeenTimestamp
    ) { computeTowerRemainingAttempts(profile, currentTime) }
    val resetMs = remember(currentTime) { computeTowerResetRemainingMs(currentTime) }
    val h = resetMs / 3600_000L
    val m = (resetMs % 3600_000L) / 60_000L
    val s = (resetMs % 60_000L) / 1000L
    val resetFormatted = String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)

    Box(modifier = modifier.fillMaxSize()) {
        if (isEditingSquad) {
            TowerSquadBuilderContent(
                rule = rule,
                wednesdayElement = wednesdayElement,
                allHeroes = allHeroes,
                artifacts = artifacts,
                towerDeckHeroIds = towerDeckHeroIds,
                pickerSlotIndex = pickerSlotIndex,
                onOpenSlotPicker = { pickerSlotIndex = it },
                onClosePicker = { pickerSlotIndex = null },
                onAssignHeroToSlot = onAssignHeroToSlot,
                onInspectHero = onInspectHero,
                onDone = {
                    pickerSlotIndex = null
                    isEditingSquad = false
                }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(GameBgDark)
                    .padding(horizontal = 12.dp)
            ) {
                Spacer(modifier = Modifier.height(6.dp))

                // Back to Hub Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onBackToHub() }
                            .padding(vertical = 6.dp, horizontal = 4.dp)
                            .testTag("tower_back_to_hub")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = tr("В Хаб", "To Hub"),
                            tint = GameGoldLight,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tr("В Хаб", "To Hub"),
                            color = GameGoldLight,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (remainingAttempts > 0) Color(0xFF281D3E) else Color(0xFF4A1824),
                            border = BorderStroke(1.dp, if (remainingAttempts > 0) GameGold.copy(alpha = 0.7f) else Color(0xFFEF5350)),
                            modifier = Modifier.testTag("tower_attempts_badge")
                        ) {
                            Text(
                                text = "⚔️ $remainingAttempts/$TOWER_DAILY_ATTEMPT_LIMIT",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (remainingAttempts > 0) GameGoldLight else Color(0xFFFFCDD2),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF281D3E),
                            border = BorderStroke(1.dp, Color(0xFF5A4380))
                        ) {
                            Text(
                                text = "⏳ $resetFormatted",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE1BEE7),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = tr("Башня Испытаний", "Tower of Trials"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = GameGoldLight,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { isEditingSquad = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF39235B),
                            contentColor = GameGoldLight
                        ),
                        border = BorderStroke(1.dp, GameGold),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .heightIn(min = 38.dp)
                            .testTag("tower_squad_button")
                    ) {
                        Text(
                            text = tr("🛡️ Отряд", "🛡️ Squad"),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tr(
                        "Каждый день — новое правило и 5 этажей (плюс секретная Крыша за все 4★). Прогресс этажа сохраняется при поражении!",
                        "A new daily rule and 5 floors each day (plus the secret Roof for all 4★). Enemy damage stays saved on defeat!"
                    ),
                    fontSize = 11.sp,
                    color = Color(0xFFAFA2C6)
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Active Daily Rule Card
                    item(key = "daily_rule_card") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("tower_daily_rule_card"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = GameSurfaceDark),
                            border = BorderStroke(1.5.dp, Color(0xFFAB47BC))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF39235B),
                                            border = BorderStroke(1.2.dp, GameGold),
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(text = rule.iconSymbol, fontSize = 18.sp)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = tr("Режим дня • $clearedFloorToday/$TOWER_TOTAL_FLOORS", "Today's Rule • $clearedFloorToday/$TOWER_TOTAL_FLOORS"),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFCE93D8)
                                            )
                                            Text(
                                                text = if (rule == TowerDayRule.WEDNESDAY_ELEMENTAL_STORM) {
                                                    "${tr(rule.titleRu, rule.titleEn)}: ${wednesdayElement.symbol} ${wednesdayElement.localizedName()}"
                                                } else {
                                                    tr(rule.titleRu, rule.titleEn)
                                                },
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = GameGoldLight
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = tr(rule.descriptionRu, rule.descriptionEn),
                                    fontSize = 11.5.sp,
                                    color = Color(0xFFE8DEF8),
                                    lineHeight = 15.5.sp
                                )

                                // Toggle 7-day schedule
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showWeeklySchedule = !showWeeklySchedule }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = tr("Расписание Башни на неделю", "Weekly Tower Schedule"),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB5A7CE)
                                    )
                                    Icon(
                                        imageVector = if (showWeeklySchedule) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = GameGoldLight,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                AnimatedVisibility(visible = showWeeklySchedule) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF191129))
                                            .padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        val orderedWeek = listOf(
                                            TowerDayRule.MONDAY_STEEL,
                                            TowerDayRule.TUESDAY_ARCANE_VOLLEY,
                                            TowerDayRule.WEDNESDAY_ELEMENTAL_STORM,
                                            TowerDayRule.THURSDAY_MANA_SURGE,
                                            TowerDayRule.FRIDAY_COMMONERS_VALOR,
                                            TowerDayRule.SATURDAY_BOSS_SIEGE,
                                            TowerDayRule.SUNDAY_GOLDEN_ASSAULT
                                        )
                                        orderedWeek.forEach { itemRule ->
                                            val isToday = itemRule == rule
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${itemRule.iconSymbol} ${tr(itemRule.shortBadgeRu, itemRule.shortBadgeEn)}",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                    color = if (isToday) GameGoldLight else Color(0xFFAEA3C5),
                                                    modifier = Modifier.width(116.dp)
                                                )
                                                Text(
                                                    text = tr(itemRule.titleRu, itemRule.titleEn),
                                                    fontSize = 10.5.sp,
                                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isToday) Color.White else Color(0xFFD5CBE9),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (remainingAttempts <= 0 && (clearedFloorToday < TOWER_TOTAL_FLOORS || (isRoofUnlocked && !isRoofCleared))) {
                        item(key = "tower_no_attempts_warning") {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("tower_no_attempts_banner"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF3A1624)),
                                border = BorderStroke(1.2.dp, Color(0xFFEF5350))
                            ) {
                                Text(
                                    text = tr(
                                        "Попытки на сегодня исчерпаны (0/$TOWER_DAILY_ATTEMPT_LIMIT). Пройденные этажи ($clearedFloorToday/$TOWER_TOTAL_FLOORS) сохранены до конца суток — в полночь откроются 2 новые попытки с 1 этажа.",
                                        "No attempts left today (0/$TOWER_DAILY_ATTEMPT_LIMIT). Cleared floors ($clearedFloorToday/$TOWER_TOTAL_FLOORS) are saved until midnight — tomorrow brings 2 fresh attempts from Floor 1."
                                    ),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFCDD2),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }

                    // Eligibility warning if Tower squad violates strict rule or is incomplete
                    if (!isDeckValid || !isDeckEligibleForRule) {
                        item(key = "tower_squad_warning") {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF3A1624)),
                                border = BorderStroke(1.2.dp, Color(0xFFEF5350))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (!isDeckValid) {
                                            tr("Соберите 4 уникальные героини в отряд Башни!", "Assign 4 unique heroines to your Tower squad!")
                                        } else {
                                            tr(
                                                "Отряд не подходит под правило «${rule.titleRu}». Нажмите «Отряд», чтобы собрать колоду для Башни.",
                                                "Your squad does not match \"${rule.titleEn}\". Tap Squad to build a Tower-only deck."
                                            )
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFCDD2),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { isEditingSquad = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = GameGold, contentColor = Color.Black),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("tower_edit_squad_warning_button")
                                    ) {
                                        Text(tr("Отряд", "Squad"), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                        }
                    }

                    // Floors 1..5 (No bot lineup displayed; only title, scaling/wounded state, stars, and rewards)
                    items(floors, key = { it.floor }) { floorItem ->
                        val isCleared = floorItem.floor <= clearedFloorToday
                        val isNextPlayable = floorItem.floor == clearedFloorToday + 1
                        val isLocked = floorItem.floor > clearedFloorToday + 1
                        val earnedStars = floorStarsToday.getOrElse(floorItem.floor - 1) { 0 }
                        val savedHps = computeTowerDamagedFloorEnemyHps(profile, floorItem.floor, currentTime)
                        val isWounded = savedHps != null
                        val killedCount = savedHps?.count { it <= 0 } ?: 0

                        TowerFloorCard(
                            floorItem = floorItem,
                            isCleared = isCleared,
                            isNextPlayable = isNextPlayable,
                            isLocked = isLocked,
                            earnedStars = earnedStars,
                            isWounded = isWounded,
                            killedCount = killedCount,
                            canFight = isDeckValid && isDeckEligibleForRule && remainingAttempts > 0,
                            onStartFloor = { onStartFloor(floorItem) }
                        )
                    }

                    // Secret Floor 6: The Roof (unlocked only when all 5 floors are cleared with 4★)
                    item(key = "tower_floor_roof") {
                        val totalStarsEarned = floorStarsToday.sum()
                        val roofSavedHps = computeTowerDamagedFloorEnemyHps(profile, TOWER_ROOF_FLOOR, currentTime)
                        TowerRoofCard(
                            roofFloor = roofFloor,
                            isUnlocked = isRoofUnlocked,
                            isCleared = isRoofCleared,
                            earnedStars = roofStars,
                            totalStarsEarned = totalStarsEarned,
                            isWounded = roofSavedHps != null,
                            killedCount = roofSavedHps?.count { it <= 0 } ?: 0,
                            canFight = isRoofUnlocked && !isRoofCleared && isDeckValid && isDeckEligibleForRule && remainingAttempts > 0,
                            onStartRoof = { onStartFloor(roofFloor) }
                        )
                    }
                }
            }
        }

        if (showTutorial && !isEditingSquad) {
            com.example.ui.components.TowerTutorialOverlay(
                step = towerTutorialStep,
                onAdvanceStep = {
                    if (towerTutorialStep < 6) {
                        towerTutorialStep++
                    } else {
                        onCompleteTutorial()
                    }
                },
                onFinishTutorial = { onCompleteTutorial() }
            )
        }
    }
}

@Composable
private fun TowerFloorCard(
    floorItem: TowerFloorPreview,
    isCleared: Boolean,
    isNextPlayable: Boolean,
    isLocked: Boolean,
    earnedStars: Int,
    isWounded: Boolean,
    killedCount: Int,
    canFight: Boolean,
    onStartFloor: () -> Unit
) {
    val scalingBadge = when (floorItem.floor) {
        1 -> "-30% ⚔️❤️"
        2 -> "-15% ⚔️❤️"
        3 -> tr("100% отряда", "100% Squad")
        4 -> "+15% ⚔️❤️"
        else -> "+30% ⚔️❤️"
    }
    val canImproveStars = isCleared && earnedStars < 4

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tower_floor_card_${floorItem.floor}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isNextPlayable -> Color(0xFF281E3E)
                isCleared -> Color(0xFF1A2622)
                else -> Color(0xFF1A1429)
            }
        ),
        border = BorderStroke(
            width = if (isNextPlayable) 1.5.dp else 1.dp,
            color = when {
                isNextPlayable -> GameGold
                isCleared -> Color(0xFF4CAF50)
                else -> Color(0xFF3E3159)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = when {
                    isCleared -> Color(0xFF2E7D32)
                    isNextPlayable -> GameGold
                    else -> Color(0xFF2D2242)
                },
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    when {
                        isCleared -> Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                        isLocked -> Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF8F82A8), modifier = Modifier.size(18.dp))
                        else -> Text(
                            text = "${floorItem.floor}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = tr(
                        "Этаж ${floorItem.floor} • ${floorItem.titleRu}",
                        "Floor ${floorItem.floor} • ${floorItem.titleEn}"
                    ),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isLocked) Color(0xFF8F82A8) else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF1B1329),
                        border = BorderStroke(0.8.dp, Color(0xFF4C3C6E))
                    ) {
                        Text(
                            text = scalingBadge,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCE93D8),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (isCleared || earnedStars > 0) {
                        Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                            repeat(4) { idx ->
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (idx < earnedStars) GameGold else Color(0xFF45375C),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }

                if (isWounded) {
                    Text(
                        text = if (killedCount > 0) {
                            tr("⚠️ Враги ослаблены ($killedCount/4 повержено)", "⚠️ Enemies weakened ($killedCount/4 defeated)")
                        } else {
                            tr("⚠️ Здоровье врагов сохранено", "⚠️ Enemy HP damage preserved")
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFB74D)
                    )
                }

                val rewardText = buildString {
                    append("+${floorItem.rewardGold} 🪙  +${floorItem.rewardGems} 💎  +${floorItem.rewardExp} XP")
                    if (floorItem.bonusShards > 0) {
                        append("  +${floorItem.bonusShards} 🧩")
                    }
                }
                Text(
                    text = rewardText,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = GameGoldLight
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            when {
                isNextPlayable -> {
                    Button(
                        onClick = onStartFloor,
                        enabled = canFight,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GameGold,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("tower_fight_floor_${floorItem.floor}")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(tr("Бой", "Fight"), fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
                canImproveStars -> {
                    OutlinedButton(
                        onClick = onStartFloor,
                        enabled = canFight,
                        border = BorderStroke(1.2.dp, GameGold),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GameGoldLight),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .heightIn(min = 44.dp)
                            .testTag("tower_fight_floor_${floorItem.floor}")
                    ) {
                        Text(tr("На 4★", "Get 4★"), fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                    }
                }
                isCleared -> {
                    Text(
                        text = tr("4★ Пройден", "4★ Cleared"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF81C784)
                    )
                }
            }
        }
    }
}

@Composable
private fun TowerRoofCard(
    roofFloor: TowerFloorPreview,
    isUnlocked: Boolean,
    isCleared: Boolean,
    earnedStars: Int,
    totalStarsEarned: Int,
    isWounded: Boolean,
    killedCount: Int,
    canFight: Boolean,
    onStartRoof: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tower_floor_card_${TOWER_ROOF_FLOOR}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCleared -> Color(0xFF1A2823)
                isUnlocked -> Color(0xFF321E48)
                else -> Color(0xFF161022)
            }
        ),
        border = BorderStroke(
            width = if (isUnlocked) 2.dp else 1.2.dp,
            color = when {
                isCleared -> Color(0xFF4CAF50)
                isUnlocked -> GameGold
                else -> Color(0xFF493A69)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = when {
                    isCleared -> Color(0xFF2E7D32)
                    isUnlocked -> GameGold
                    else -> Color(0xFF281D3D)
                },
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (!isUnlocked) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(20.dp))
                    } else if (isCleared) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                    } else {
                        Text(text = "👑", fontSize = 20.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = tr("Секретный этаж • Крыша", "Secret Floor • The Roof"),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isUnlocked) GameGoldLight else Color(0xFFB5A7CE)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF411B2E),
                        border = BorderStroke(0.8.dp, Color(0xFFFF7043))
                    ) {
                        Text(
                            text = tr("+50% ⚔️❤️ • Награда x2", "+50% ⚔️❤️ • 2x Reward"),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFFCC80),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (isCleared && earnedStars > 0) {
                        Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                            repeat(4) { idx ->
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (idx < earnedStars) GameGold else Color(0xFF45375C),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }

                if (!isUnlocked) {
                    Text(
                        text = tr(
                            "Откроется после прохождения этажей 1–5 на 4★ ($totalStarsEarned/20 ★)",
                            "Unlocks after clearing Floors 1–5 with all 4★ ($totalStarsEarned/20 ★)"
                        ),
                        fontSize = 10.sp,
                        color = Color(0xFFFFB74D),
                        fontWeight = FontWeight.SemiBold
                    )
                } else if (isWounded && !isCleared) {
                    Text(
                        text = if (killedCount > 0) {
                            tr("⚠️ Враги ослаблены ($killedCount/4 повержено)", "⚠️ Enemies weakened ($killedCount/4 defeated)")
                        } else {
                            tr("⚠️ Здоровье врагов сохранено", "⚠️ Enemy HP damage preserved")
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFB74D)
                    )
                }

                val rewardText = "+${roofFloor.rewardGold} 🪙  +${roofFloor.rewardGems} 💎  +${roofFloor.rewardExp} XP  +${roofFloor.bonusShards} 🧩"
                Text(
                    text = rewardText,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = GameGoldLight
                )
            }

            if (isUnlocked && !isCleared) {
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onStartRoof,
                    enabled = canFight,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GameGold,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("tower_fight_floor_$TOWER_ROOF_FLOOR")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(tr("Бой", "Fight"), fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            } else if (isCleared) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tr("Пройдена", "Cleared"),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF81C784)
                )
            }
        }
    }
}

@Composable
private fun TowerSquadBuilderContent(
    rule: TowerDayRule,
    wednesdayElement: Element,
    allHeroes: List<HeroCard>,
    artifacts: List<Artifact>,
    towerDeckHeroIds: List<String?>,
    pickerSlotIndex: Int?,
    onOpenSlotPicker: (Int) -> Unit,
    onClosePicker: () -> Unit,
    onAssignHeroToSlot: (Int, String?) -> Unit,
    onInspectHero: (HeroCard) -> Unit,
    onDone: () -> Unit
) {
    val unlockedHeroes = remember(allHeroes) { allHeroes.filter { it.isUnlocked } }

    fun isHeroEligibleForDayRule(hero: HeroCard): Boolean {
        return when (rule) {
            TowerDayRule.MONDAY_STEEL -> {
                val eligible = unlockedHeroes.count { it.heroType == HeroType.MELEE || it.heroType == HeroType.HYBRID }
                eligible < 4 || hero.heroType == HeroType.MELEE || hero.heroType == HeroType.HYBRID
            }
            TowerDayRule.FRIDAY_COMMONERS_VALOR -> {
                val eligible = unlockedHeroes.count { it.rarity == CardRarity.BASIC || it.rarity == CardRarity.SPECIAL }
                eligible < 4 || hero.rarity == CardRarity.BASIC || hero.rarity == CardRarity.SPECIAL
            }
            else -> true
        }
    }

    val uniqueCount = towerDeckHeroIds.filterNotNull().distinct().size
    val selectedHeroes = towerDeckHeroIds.mapNotNull { id -> allHeroes.firstOrNull { it.id == id && it.isUnlocked } }
    val isSquadValid = towerDeckHeroIds.size == 4 && uniqueCount == 4 && selectedHeroes.size == 4 &&
        selectedHeroes.all { isHeroEligibleForDayRule(it) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GameBgDark)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("tower_squad_builder"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = GameSurfaceDark,
            border = BorderStroke(1.dp, Color(0xFF453664)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable { onDone() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("tower_squad_back_button"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = tr("Назад к Башне", "Back to Tower"),
                    tint = GameGoldLight,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                AutoShrinkText(
                    text = tr("Назад к этажам Башни", "Back to Tower Floors"),
                    color = Color.White,
                    initialFontSizeSp = 14f,
                    minFontSizeSp = 10f,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF341E4E),
                    border = BorderStroke(1.dp, GameGold)
                ) {
                    Text(
                        text = "${rule.iconSymbol} ${tr(rule.shortBadgeRu, rule.shortBadgeEn)}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = GameGoldLight,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = tr("Отряд для Башни (2x2)", "Tower Squad Builder (2x2)"),
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = GameGoldLight
            )
            Text(
                text = tr(
                    "Этот отряд используется только в Башне и не меняет вашу основную боевую колоду. Нажмите на слот (или зажмите карту) для выбора.",
                    "This squad is used exclusively for the Tower without changing your main deck. Tap an empty slot or long-press a card to swap."
                ),
                fontSize = 11.sp,
                color = Color(0xFFAFA2C6),
                lineHeight = 14.5.sp
            )
        }

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF281A3E),
            border = BorderStroke(1.dp, Color(0xFFAB47BC)),
            modifier = Modifier.fillMaxWidth()
        ) {
            val ruleName = if (rule == TowerDayRule.WEDNESDAY_ELEMENTAL_STORM) {
                "${tr(rule.titleRu, rule.titleEn)} (${wednesdayElement.symbol} ${wednesdayElement.localizedName()})"
            } else {
                tr(rule.titleRu, rule.titleEn)
            }
            Text(
                text = "${rule.iconSymbol} $ruleName: ${tr(rule.descriptionRu, rule.descriptionEn)}",
                fontSize = 11.sp,
                color = Color(0xFFE8DEF8),
                modifier = Modifier.padding(10.dp)
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = GameSurfaceDark),
            border = BorderStroke(1.2.dp, Color(0xFF4B3870))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = tr("⚔️ Первый ряд (Ближний бой / Смешанный)", "⚔️ Front Row (Melee / Hybrid)"),
                    fontSize = 10.sp,
                    color = StatArmorSteel,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DeckSlotItem(
                        slotIndex = 0,
                        label = tr("Передний Левый", "Front Left"),
                        hero = towerDeckHeroIds.getOrNull(0)?.let { id -> allHeroes.firstOrNull { it.id == id } },
                        artifacts = artifacts,
                        isWaitingAssignment = pickerSlotIndex == 0,
                        onClickSlot = { onOpenSlotPicker(0) },
                        onLongClickSlot = { onOpenSlotPicker(0) },
                        onInspect = onInspectHero,
                        modifier = Modifier.weight(1f)
                    )
                    DeckSlotItem(
                        slotIndex = 1,
                        label = tr("Передний Правый", "Front Right"),
                        hero = towerDeckHeroIds.getOrNull(1)?.let { id -> allHeroes.firstOrNull { it.id == id } },
                        artifacts = artifacts,
                        isWaitingAssignment = pickerSlotIndex == 1,
                        onClickSlot = { onOpenSlotPicker(1) },
                        onLongClickSlot = { onOpenSlotPicker(1) },
                        onInspect = onInspectHero,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = tr("🏹 Задний ряд (Дальний бой / Смешанный)", "🏹 Back Row (Ranged / Hybrid)"),
                    fontSize = 10.sp,
                    color = StatAtkBlue,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DeckSlotItem(
                        slotIndex = 2,
                        label = tr("Задний Левый", "Back Left"),
                        hero = towerDeckHeroIds.getOrNull(2)?.let { id -> allHeroes.firstOrNull { it.id == id } },
                        artifacts = artifacts,
                        isWaitingAssignment = pickerSlotIndex == 2,
                        onClickSlot = { onOpenSlotPicker(2) },
                        onLongClickSlot = { onOpenSlotPicker(2) },
                        onInspect = onInspectHero,
                        modifier = Modifier.weight(1f)
                    )
                    DeckSlotItem(
                        slotIndex = 3,
                        label = tr("Задний Правый", "Back Right"),
                        hero = towerDeckHeroIds.getOrNull(3)?.let { id -> allHeroes.firstOrNull { it.id == id } },
                        artifacts = artifacts,
                        isWaitingAssignment = pickerSlotIndex == 3,
                        onClickSlot = { onOpenSlotPicker(3) },
                        onLongClickSlot = { onOpenSlotPicker(3) },
                        onInspect = onInspectHero,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Squad stats summary
        val totalAtk = selectedHeroes.sumOf { it.calculateTotalAtk(artifacts) }
        val totalHp = selectedHeroes.sumOf { it.calculateTotalHp(artifacts) }
        val totalArmor = selectedHeroes.sumOf { it.calculateTotalArmor(artifacts) }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF1E1730),
            border = BorderStroke(1.dp, Color(0xFF453664))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tr("⚔️ Атака: $totalAtk", "⚔️ ATK: $totalAtk"),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = StatAtkBlue
                )
                Text(
                    text = tr("❤️ Здоровье: $totalHp", "❤️ HP: $totalHp"),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF81C784)
                )
                Text(
                    text = tr("🛡️ Броня: $totalArmor", "🛡️ Armor: $totalArmor"),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = StatArmorSteel
                )
            }
        }

        Button(
            onClick = onDone,
            enabled = isSquadValid,
            colors = ButtonDefaults.buttonColors(
                containerColor = GameGold,
                contentColor = Color.Black,
                disabledContainerColor = Color(0xFF2B223E),
                disabledContentColor = Color(0xFF8D81A3)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
                .testTag("tower_squad_done_button")
        ) {
            AutoShrinkText(
                text = if (isSquadValid) tr("✓ Сохранить отряд Башни", "✓ Save Tower Squad") else tr("Заполните 4 подходящие карты", "Fill 4 eligible heroines"),
                color = if (isSquadValid) Color.Black else Color(0xFF8D81A3),
                initialFontSizeSp = 15f,
                minFontSizeSp = 10f,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }

    // 2-Column Picker Modal matching ColiseumScreen
    pickerSlotIndex?.let { slot ->
        val otherSlotsIds = towerDeckHeroIds.withIndex()
            .filter { it.index != slot }
            .mapNotNull { it.value }
            .toSet()
        val currentSlotHeroId = towerDeckHeroIds.getOrNull(slot)
        val availableHeroes = unlockedHeroes
            .filter { it.id !in otherSlotsIds }
            .sortedWith(
                compareByDescending<HeroCard> {
                    isHeroEligibleForDayRule(it) && (slot !in 2..3 || it.heroType != HeroType.MELEE)
                }
            )

        Dialog(
            onDismissRequest = onClosePicker,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = GameSurfaceDark,
                border = BorderStroke(1.5.dp, GameGold),
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .widthIn(max = 440.dp)
                    .heightIn(max = 580.dp)
                    .testTag("tower_picker_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tr("Выбор в слот Башни ${slot + 1}", "Pick for Tower Slot ${slot + 1}"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = GameGoldLight,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onClosePicker,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = tr("Закрыть", "Close"),
                                tint = Color(0xFFAFA2C6)
                            )
                        }
                    }

                    if (slot in 2..3) {
                        Text(
                            text = tr(
                                "🏹 Задний ряд: только Дальний или Смешанный бой",
                                "🏹 Back row: Ranged or Hybrid heroines only"
                            ),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StatAtkBlue
                        )
                    }

                    if (currentSlotHeroId != null) {
                        OutlinedButton(
                            onClick = {
                                onAssignHeroToSlot(slot, null)
                                onClosePicker()
                            },
                            border = BorderStroke(1.dp, Color(0xFFEF5350)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF8A80)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 42.dp)
                                .testTag("tower_clear_slot_button")
                        ) {
                            Text(
                                text = tr("Убрать карту из слота", "Clear Slot"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (availableHeroes.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tr(
                                    "Нет свободных открытых героинь. Очистите другой слот, чтобы переставить карту.",
                                    "No available unlocked heroines. Clear another slot to rearrange your squad."
                                ),
                                fontSize = 12.sp,
                                color = Color(0xFFAFA2C6),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                        ) {
                            items(availableHeroes, key = { it.id }) { hero ->
                                val isSlotCompatible = slot !in 2..3 || hero.heroType != HeroType.MELEE
                                val isRuleCompatible = isHeroEligibleForDayRule(hero)
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    HeroCardItem(
                                        hero = hero,
                                        allArtifacts = artifacts,
                                        isSelectedInDeck = currentSlotHeroId == hero.id,
                                        showUpgradeIndicator = false,
                                        onClick = {
                                            if (!isSlotCompatible) {
                                                onAssignHeroToSlot(slot, hero.id)
                                            } else if (isRuleCompatible) {
                                                onAssignHeroToSlot(slot, hero.id)
                                                onClosePicker()
                                            }
                                        },
                                        modifier = Modifier
                                            .widthIn(max = 152.dp)
                                            .fillMaxWidth()
                                    )
                                    if (!isRuleCompatible) {
                                        Text(
                                            text = tr("Не подходит по правилу", "Disallowed by rule"),
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFF8A80)
                                        )
                                    } else if (!isSlotCompatible) {
                                        Text(
                                            text = tr("Только 1-й ряд", "Front row only"),
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFFB74D)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
