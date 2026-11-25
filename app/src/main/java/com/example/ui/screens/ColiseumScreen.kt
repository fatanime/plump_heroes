package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.i18n.localizedName
import com.example.i18n.tr
import com.example.model.*
import com.example.ui.components.AutoShrinkText
import com.example.ui.components.ColiseumTutorialOverlay
import com.example.ui.components.HeroCardItem
import com.example.ui.components.HeroPortraitImage
import com.example.ui.theme.*
import kotlin.math.ceil

@Composable
fun ColiseumScreen(
    profile: PlayerProfile,
    heroes: List<HeroCard>,
    artifacts: List<Artifact>,
    coliseumDeckHeroIds: List<String?>,
    runState: ColiseumRunState?,
    battlePreview: ColiseumBattlePreview?,
    isPreviewVisible: Boolean = battlePreview != null,
    milestoneBonusOffer: ColiseumMilestoneBonusOffer?,
    showTutorial: Boolean = !profile.isColiseumTutorialCompleted,
    onBackToMenu: () -> Unit,
    onAssignHeroToSlot: (Int, String?) -> Unit,
    onInspectHero: (HeroCard) -> Unit,
    onStartColiseum: () -> Unit,
    onCancelBattle1Preview: () -> Unit,
    onDismissBattlePreview: () -> Unit = {},
    onReopenBattlePreview: () -> Unit = {},
    onLeaveColiseum: () -> Unit = onBackToMenu,
    onConfirmBattle: () -> Unit,
    onApplyMilestoneHeal: () -> Unit,
    onApplyMilestoneRevive: (Int) -> Unit,
    onApplyMilestoneMana: () -> Unit = {},
    onCompleteTutorial: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pickerSlotIndex by remember { mutableStateOf<Int?>(null) }
    var coliseumTutorialStep by remember(showTutorial) { mutableIntStateOf(1) }

    val uniqueFilledCount = coliseumDeckHeroIds.filterNotNull().distinct().size
    val isDeckComplete = coliseumDeckHeroIds.size == 4 &&
        coliseumDeckHeroIds.none { it == null } &&
        uniqueFilledCount == 4

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GameBgDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("coliseum_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Bar: Back arrow + single-line AutoShrinkText "Вернуться в меню"
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
                        .clickable {
                            if (runState != null) onLeaveColiseum() else onBackToMenu()
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("coliseum_back_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = tr("Вернуться в меню", "Return to Menu"),
                        tint = GameGoldLight,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    AutoShrinkText(
                        text = tr("Вернуться в меню", "Return to Menu"),
                        color = Color.White,
                        initialFontSizeSp = 15f,
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
                            text = tr("🏛️ КОЛИЗЕЙ", "🏛️ COLISEUM"),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = GameGoldLight,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            if (runState == null) {
                // Header Instructions
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = tr("Сбор колоды Колизея (2x2)", "Build Coliseum Deck (2x2)"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = GameGoldLight
                    )
                    Text(
                        text = tr(
                            "Нажмите на пустой слот, чтобы выбрать героиню (зажмите карту для замены). Потерянное здоровье переносится между боями!",
                            "Tap an empty slot to pick a heroine (long-press a card to swap). Remaining HP carries over between battles!"
                        ),
                        fontSize = 11.sp,
                        color = Color(0xFFAFA2C6),
                        lineHeight = 14.5.sp
                    )
                }

                // 2x2 Coliseum Deck Card matching RosterScreen DeckSlotItem size (190.dp)
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
                                hero = coliseumDeckHeroIds.getOrNull(0)?.let { id -> heroes.firstOrNull { it.id == id } },
                                artifacts = artifacts,
                                isWaitingAssignment = pickerSlotIndex == 0,
                                onClickSlot = { pickerSlotIndex = 0 },
                                onLongClickSlot = { pickerSlotIndex = 0 },
                                onInspect = onInspectHero,
                                modifier = Modifier.weight(1f)
                            )

                            DeckSlotItem(
                                slotIndex = 1,
                                label = tr("Передний Правый", "Front Right"),
                                hero = coliseumDeckHeroIds.getOrNull(1)?.let { id -> heroes.firstOrNull { it.id == id } },
                                artifacts = artifacts,
                                isWaitingAssignment = pickerSlotIndex == 1,
                                onClickSlot = { pickerSlotIndex = 1 },
                                onLongClickSlot = { pickerSlotIndex = 1 },
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
                                hero = coliseumDeckHeroIds.getOrNull(2)?.let { id -> heroes.firstOrNull { it.id == id } },
                                artifacts = artifacts,
                                isWaitingAssignment = pickerSlotIndex == 2,
                                onClickSlot = { pickerSlotIndex = 2 },
                                onLongClickSlot = { pickerSlotIndex = 2 },
                                onInspect = onInspectHero,
                                modifier = Modifier.weight(1f)
                            )

                            DeckSlotItem(
                                slotIndex = 3,
                                label = tr("Задний Правый", "Back Right"),
                                hero = coliseumDeckHeroIds.getOrNull(3)?.let { id -> heroes.firstOrNull { it.id == id } },
                                artifacts = artifacts,
                                isWaitingAssignment = pickerSlotIndex == 3,
                                onClickSlot = { pickerSlotIndex = 3 },
                                onLongClickSlot = { pickerSlotIndex = 3 },
                                onInspect = onInspectHero,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Selected Squad Stats Summary
                val selectedHeroes = coliseumDeckHeroIds.mapNotNull { id -> heroes.firstOrNull { it.id == id } }
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
                            color = StatAtkBlue,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = tr("❤️ Здоровье: $totalHp", "❤️ HP: $totalHp"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF81C784),
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = tr("🛡️ Броня: $totalArmor", "🛡️ Armor: $totalArmor"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatArmorSteel,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Start Coliseum Battle Button (Active only when all 4 unique slots are filled)
                Button(
                    onClick = onStartColiseum,
                    enabled = isDeckComplete,
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
                        .testTag("coliseum_start_battle_button")
                ) {
                    AutoShrinkText(
                        text = tr("⚔️ Начать битву", "⚔️ Start Battle"),
                        color = if (isDeckComplete) Color.Black else Color(0xFF8D81A3),
                        initialFontSizeSp = 15f,
                        minFontSizeSp = 11f,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            } else {
                // Active Coliseum Run Intermission View (shown cleanly behind preview or when preview is dismissed via X)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("coliseum_intermission_panel"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = GameSurfaceDark),
                    border = BorderStroke(1.5.dp, GameGold)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = tr("🏛️ Колизей • Битва №${runState.battleNumber}", "🏛️ Coliseum • Battle #${runState.battleNumber}"),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = GameGoldLight
                        )
                        Text(
                            text = tr(
                                "Накоплено: ${runState.accumulatedGold} 🪙 • ${runState.accumulatedGems} 💎",
                                "Accumulated: ${runState.accumulatedGold} 🪙 • ${runState.accumulatedGems} 💎"
                            ),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (runState.bonusStartMana > 0) {
                            Text(
                                text = tr(
                                    "🧪 Мана в начале боя: ${1 + runState.bonusStartMana} ⚡",
                                    "🧪 Starting Battle Mana: ${1 + runState.bonusStartMana} ⚡"
                                ),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF4DD0E1),
                                modifier = Modifier.testTag("coliseum_intermission_mana_badge")
                            )
                        }

                        Text(
                            text = tr("Здоровье команды сейчас:", "Current squad health:"),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD7CCE8),
                            modifier = Modifier.align(Alignment.Start)
                        )

                        runState.playerCards.sortedBy { it.slotIndex }.forEach { card ->
                            val rowLabel = if (card.slotIndex in 0..1) tr("1-й ряд", "Front") else tr("2-й ряд", "Back")
                            val displayName = localizeHeroNameByIdOrRu(card.heroId, card.name)
                            val hpFraction = if (card.isAlive && card.maxHp > 0) {
                                (card.currentHp.toFloat() / card.maxHp.toFloat()).coerceIn(0f, 1f)
                            } else 0f

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1A1228),
                                border = BorderStroke(1.dp, if (card.isAlive) Color(0xFF453664) else Color(0xFF6B2432)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("coliseum_squad_status_${card.slotIndex}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .border(
                                                1.5.dp,
                                                if (card.isAlive) Color(0xFF81C784) else Color(0xFFEF5350),
                                                CircleShape
                                            )
                                    ) {
                                        HeroPortraitImage(
                                            imageRes = card.imageRes,
                                            heroId = card.heroId,
                                            heroName = displayName,
                                            heroLevel = card.level,
                                            contentDescription = displayName,
                                            contentScale = ContentScale.Crop,
                                            alignment = Alignment.TopCenter,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "$displayName ($rowLabel)",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (card.isAlive) Color.White else Color(0xFFFF8A80)
                                            )
                                            Text(
                                                text = if (card.isAlive) {
                                                    tr("Жива • ❤️ ${card.currentHp}/${card.maxHp}", "Alive • ❤️ ${card.currentHp}/${card.maxHp}")
                                                } else {
                                                    tr("💀 Пала (0/${card.maxHp})", "💀 Fallen (0/${card.maxHp})")
                                                },
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (card.isAlive) Color(0xFF81C784) else Color(0xFFFF8A80)
                                            )
                                        }

                                        LinearProgressIndicator(
                                            progress = { hpFraction },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = if (card.isAlive) Color(0xFF66BB6A) else Color(0xFF783A44),
                                            trackColor = Color(0xFF2D2242)
                                        )
                                    }
                                }
                            }
                        }

                        battlePreview?.let { preview ->
                            Spacer(modifier = Modifier.height(6.dp))

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1F1533),
                                border = BorderStroke(1.dp, Color(0xFF513D75)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = tr(
                                        "Битва №${preview.battleNumber}. Противник ${preview.opponentName}. Приз: ${preview.rewardGold} монет и ${preview.rewardGems} алмазов",
                                        "Battle #${preview.battleNumber}. Opponent ${preview.opponentName}. Prize: ${preview.rewardGold} coins and ${preview.rewardGems} gems"
                                    ),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                        .testTag("coliseum_intermission_preview_text")
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onLeaveColiseum,
                                    border = BorderStroke(1.2.dp, Color(0xFFEF5350)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF8A80)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("coliseum_intermission_leave_button")
                                ) {
                                    AutoShrinkText(
                                        text = tr("Покинуть Колизей", "Leave Coliseum"),
                                        color = Color(0xFFFF8A80),
                                        initialFontSizeSp = 13f,
                                        minFontSizeSp = 9f,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Button(
                                    onClick = onConfirmBattle,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GameGold,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("coliseum_intermission_fight_button")
                                ) {
                                    AutoShrinkText(
                                        text = tr("⚔️ В бой!", "⚔️ Fight!"),
                                        color = Color.Black,
                                        initialFontSizeSp = 14f,
                                        minFontSizeSp = 10f,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Card Picker Modal Dialog for Coliseum Deck Slot
        pickerSlotIndex?.let { slot ->
            val otherSlotsIds = coliseumDeckHeroIds.withIndex()
                .filter { it.index != slot }
                .mapNotNull { it.value }
                .toSet()
            val currentSlotHeroId = coliseumDeckHeroIds.getOrNull(slot)
            val availableHeroes = heroes.filter { it.isUnlocked && it.id !in otherSlotsIds }

            Dialog(
                onDismissRequest = { pickerSlotIndex = null },
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
                        .testTag("coliseum_picker_dialog")
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
                                text = tr("Выбор в слот ${slot + 1}", "Pick for Slot ${slot + 1}"),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = GameGoldLight,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { pickerSlotIndex = null },
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
                                    pickerSlotIndex = null
                                },
                                border = BorderStroke(1.dp, Color(0xFFEF5350)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF8A80)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 42.dp)
                                    .testTag("coliseum_clear_slot_button")
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
                                        "No available unlocked heroines. Clear another slot to rearrange your deck."
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
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        HeroCardItem(
                                            hero = hero,
                                            allArtifacts = artifacts,
                                            isSelectedInDeck = currentSlotHeroId == hero.id,
                                            showUpgradeIndicator = false,
                                            onClick = {
                                                val isMeleeInBack = slot in 2..3 && hero.heroType == HeroType.MELEE
                                                onAssignHeroToSlot(slot, hero.id)
                                                if (!isMeleeInBack) {
                                                    pickerSlotIndex = null
                                                }
                                            },
                                            modifier = Modifier
                                                .widthIn(max = 152.dp)
                                                .fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Pre-Battle Announcement Modal
        if (isPreviewVisible) {
            battlePreview?.let { preview ->
                Dialog(onDismissRequest = {
                    if (preview.battleNumber == 1) onCancelBattle1Preview() else onDismissBattlePreview()
                }) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = GameSurfaceDark,
                        border = BorderStroke(1.8.dp, GameGold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 420.dp)
                            .testTag("coliseum_battle_preview_dialog")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = tr("🏛️ Колизей", "🏛️ Coliseum"),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GameGoldLight,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                                IconButton(
                                    onClick = {
                                        if (preview.battleNumber == 1) onCancelBattle1Preview() else onDismissBattlePreview()
                                    },
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .size(48.dp)
                                        .testTag("coliseum_close_preview_x")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = tr("Закрыть", "Close"),
                                        tint = Color(0xFFAEA3C5)
                                    )
                                }
                            }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1F1533),
                            border = BorderStroke(1.dp, Color(0xFF513D75)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = tr(
                                    "Битва №${preview.battleNumber}. Противник ${preview.opponentName}. Приз: ${preview.rewardGold} монет и ${preview.rewardGems} алмазов",
                                    "Battle #${preview.battleNumber}. Opponent ${preview.opponentName}. Prize: ${preview.rewardGold} coins and ${preview.rewardGems} gems"
                                ),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                                    .testTag("coliseum_preview_text")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (preview.battleNumber == 1) {
                                OutlinedButton(
                                    onClick = onCancelBattle1Preview,
                                    border = BorderStroke(1.dp, Color(0xFF6B5A8E)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCCC2DF)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("coliseum_cancel_preview_button")
                                ) {
                                    AutoShrinkText(
                                        text = tr("Отмена", "Cancel"),
                                        color = Color(0xFFCCC2DF),
                                        initialFontSizeSp = 13f,
                                        minFontSizeSp = 9.5f,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Button(
                                onClick = onConfirmBattle,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GameGold,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("coliseum_confirm_battle_button")
                            ) {
                                AutoShrinkText(
                                    text = tr("⚔️ В бой!", "⚔️ Fight!"),
                                    color = Color.Black,
                                    initialFontSizeSp = 14f,
                                    minFontSizeSp = 10f,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
            }
        }

        // Milestone Bonus Modal (After Battle 3, 6, 9, ...)
        milestoneBonusOffer?.let { offer ->
            var showReviveSelector by remember(offer) { mutableStateOf(false) }

            Dialog(onDismissRequest = { /* Mandatory bonus choice */ }) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = GameSurfaceDark,
                    border = BorderStroke(1.8.dp, GameGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                        .testTag("coliseum_milestone_dialog")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AutoShrinkText(
                            text = if (!showReviveSelector) {
                                tr("🎁 Забери приз!", "🎁 Claim Bonus!")
                            } else {
                                tr("Выберите героиню для воскрешения", "Select heroine to revive")
                            },
                            color = GameGoldLight,
                            initialFontSizeSp = 18f,
                            minFontSizeSp = 12f,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (!showReviveSelector) {
                            Text(
                                text = tr(
                                    "После ${offer.completedBattleNumber}-й победы выберите награду восстановления для отряда:",
                                    "After ${offer.completedBattleNumber} victories, choose one recovery bonus for your squad:"
                                ),
                                fontSize = 12.sp,
                                color = Color(0xFFDDD2F0),
                                textAlign = TextAlign.Center
                            )

                            // Option 1: Heal surviving cards (Full-width single-line AutoShrinkText)
                            Button(
                                onClick = onApplyMilestoneHeal,
                                enabled = offer.aliveCount > 0,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2E7D32),
                                    contentColor = Color.White,
                                    disabledContainerColor = Color(0xFF2B223E),
                                    disabledContentColor = Color(0xFF8D81A3)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 50.dp)
                                    .testTag("coliseum_milestone_heal_button")
                            ) {
                                AutoShrinkText(
                                    text = tr(
                                        "❤️ Восстановить живым +${offer.healPercent}% ОЗ",
                                        "❤️ Heal survivors +${offer.healPercent}% HP"
                                    ),
                                    color = if (offer.aliveCount > 0) Color.White else Color(0xFF8D81A3),
                                    initialFontSizeSp = 13.5f,
                                    minFontSizeSp = 9f,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Option 2: Revive 1 fallen card (Full-width single-line AutoShrinkText)
                            val canRevive = offer.deadCards.isNotEmpty()
                            Button(
                                onClick = {
                                    if (offer.deadCards.size == 1) {
                                        onApplyMilestoneRevive(offer.deadCards.first().slotIndex)
                                    } else if (offer.deadCards.size > 1) {
                                        showReviveSelector = true
                                    }
                                },
                                enabled = canRevive,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6A1B9A),
                                    contentColor = GameGoldLight,
                                    disabledContainerColor = Color(0xFF2B223E),
                                    disabledContentColor = Color(0xFF8D81A3)
                                ),
                                border = if (canRevive) BorderStroke(1.2.dp, GameGold) else null,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 50.dp)
                                    .testTag("coliseum_milestone_revive_button")
                            ) {
                                AutoShrinkText(
                                    text = tr(
                                        "✨ Воскресить павшую карту (${offer.revivePercent}% ОЗ)",
                                        "✨ Revive fallen card (${offer.revivePercent}% HP)"
                                    ),
                                    color = if (canRevive) GameGoldLight else Color(0xFF8D81A3),
                                    initialFontSizeSp = 13.5f,
                                    minFontSizeSp = 9f,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Option 3: Mana Vial (+1 starting Mana in Coliseum)
                            val nextStartMana = 1 + offer.currentBonusStartMana + 1
                            Button(
                                onClick = onApplyMilestoneMana,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0277BD),
                                    contentColor = Color.White
                                ),
                                border = BorderStroke(1.2.dp, Color(0xFF81D4FA)),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 50.dp)
                                    .testTag("coliseum_milestone_mana_button")
                            ) {
                                AutoShrinkText(
                                    text = tr(
                                        "🧪 Пузырек Маны (+1 к мане на старте, будет $nextStartMana)",
                                        "🧪 Mana Vial (+1 starting Mana, next: $nextStartMana)"
                                    ),
                                    color = Color.White,
                                    initialFontSizeSp = 13.5f,
                                    minFontSizeSp = 9f,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            // Dead Card Selector when >1 cards fell
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                offer.deadCards.forEach { deadCard ->
                                    val revivedHp = ceil(deadCard.maxHp * (offer.revivePercent / 100.0)).toInt()
                                        .coerceIn(1, deadCard.maxHp)
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF261A3B),
                                        border = BorderStroke(1.2.dp, GameGold),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { onApplyMilestoneRevive(deadCard.slotIndex) }
                                            .testTag("coliseum_revive_target_${deadCard.slotIndex}")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .border(1.5.dp, GameGold, CircleShape)
                                                ) {
                                                    HeroPortraitImage(
                                                        imageRes = deadCard.imageRes,
                                                        heroId = deadCard.heroId,
                                                        heroName = deadCard.name,
                                                        heroLevel = deadCard.level,
                                                        contentDescription = deadCard.name,
                                                        contentScale = ContentScale.Crop,
                                                        alignment = Alignment.TopCenter,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = deadCard.name,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                            Text(
                                                text = "❤️ $revivedHp/${deadCard.maxHp}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF81C784)
                                            )
                                        }
                                    }
                                }

                                OutlinedButton(
                                    onClick = { showReviveSelector = false },
                                    border = BorderStroke(1.dp, Color(0xFF6B5A8E)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 44.dp)
                                ) {
                                    Text(tr("Назад", "Back"), color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // First-visit Coliseum Tutorial (3 Fairy Frames, strictly once ever)
        if (showTutorial) {
            ColiseumTutorialOverlay(
                step = coliseumTutorialStep,
                onAdvanceStep = {
                    if (coliseumTutorialStep < 3) {
                        coliseumTutorialStep++
                    } else {
                        onCompleteTutorial()
                    }
                },
                onFinishTutorial = { onCompleteTutorial() }
            )
        }
    }
}
