package com.example.ui.screens

import androidx.compose.animation.*
import kotlinx.coroutines.delay
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.localizeAmuletName
import com.example.i18n.localizeAmuletNote
import com.example.i18n.localizeHeroNameByIdOrRu
import com.example.i18n.localizedEffectDescription
import com.example.i18n.localizedName
import com.example.i18n.tr
import com.example.model.BattleCard
import com.example.model.BattlePhase
import com.example.model.BattleState
import com.example.model.HeroBattleQuote
import com.example.model.TOWER_ROOF_FLOOR
import com.example.model.TOWER_TOTAL_FLOORS
import com.example.ui.components.AutoShrinkText
import com.example.ui.components.BattleCardWidget
import com.example.ui.components.getCardFrameColor
import com.example.ui.components.getRarityColor
import com.example.ui.theme.*

@Composable
fun BattleScreen(
    battleState: BattleState,
    canCardTargetSlot: (BattleCard, Int, List<BattleCard>) -> Boolean,
    onSelectPlayerSlot: (Int) -> Unit,
    onAttackTarget: (Int) -> Unit,
    onCastSkill: () -> Unit,
    onToggleAuto: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    onExitBattle: () -> Unit,
    onSurrender: () -> Unit = onExitBattle,
    onDismissFirstTurnModal: () -> Unit = {},
    onClaimColiseumPrize: () -> Unit = onExitBattle,
    onContinueColiseum: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showSurrenderConfirmDialog by remember { mutableStateOf(false) }
    var dismissedStartQuote by remember(battleState.battleStartQuote) { mutableStateOf(false) }

    LaunchedEffect(battleState.battleStartQuote, battleState.showArenaFirstTurnModal) {
        if (battleState.battleStartQuote != null && !battleState.showArenaFirstTurnModal && !dismissedStartQuote) {
            delay(4200L)
            dismissedStartQuote = true
        }
    }

    val selectedAttacker = if (battleState.isProcessingRoundEffects) null else battleState.playerCards.firstOrNull {
        it.slotIndex == battleState.selectedPlayerSlot && it.isAlive && !it.hasActedThisRound && !it.hasSkipTurnEffect
    }
    val totalPlayerMana = battleState.playerTeamMana

    val backgroundColors = when {
        battleState.isArena -> listOf(Color(0xFF150F28), Color(0xFF1F143D), Color(0xFF0D091B)) // Astral Colosseum
        battleState.campaignChapter == 1 -> listOf(Color(0xFF0A1812), Color(0xFF11261B), Color(0xFF08120D)) // Emerald Hills / Castle
        battleState.campaignChapter == 2 -> listOf(Color(0xFF0E0B1C), Color(0xFF1A1233), Color(0xFF090614)) // Dark Mist Forest
        battleState.campaignChapter == 3 -> listOf(Color(0xFF200D0B), Color(0xFF331410), Color(0xFF130706)) // Fire Citadel
        else -> listOf(Color(0xFF140D26), Color(0xFF0C0816), Color(0xFF170E2B))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = backgroundColors))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Battle background image at full opacity 1.0f with light darkening scrim
        Image(
            painter = painterResource(battleState.battleBgRes),
            contentDescription = tr("Фон битвы", "Battle background"),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(1.0f)
        )

        // Light translucent scrim overlay (легкое затемнение)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x35000000))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TOP BAR: Enemy profile, Auto-battle & Speed toggles, Surrender
            BattleTopBar(
                enemyName = battleState.enemyName,
                enemyTitle = battleState.enemyTitle,
                isAutoBattle = battleState.isAutoBattle,
                battleSpeed = battleState.battleSpeed,
                onToggleAuto = onToggleAuto,
                onSetSpeed = onSetSpeed,
                onSurrender = { showSurrenderConfirmDialog = true }
            )

            // ENEMY 2x2 BOARD
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Enemy Back Row (Slots 2, 3)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val card2 = battleState.enemyCards.firstOrNull { it.slotIndex == 2 }
                    val card3 = battleState.enemyCards.firstOrNull { it.slotIndex == 3 }

                    val isTarget2 = selectedAttacker != null && card2 != null && card2.isAlive &&
                            canCardTargetSlot(selectedAttacker, 2, battleState.enemyCards)
                    val isTarget3 = selectedAttacker != null && card3 != null && card3.isAlive &&
                            canCardTargetSlot(selectedAttacker, 3, battleState.enemyCards)

                    BattleCardWidget(
                        card = card2,
                        isSelectedAttacker = false,
                        isValidTarget = isTarget2,
                        combatTexts = battleState.combatTexts,
                        attackAnimation = battleState.activeAttackAnimation,
                        dotHitTargets = battleState.dotHitTargets,
                        teamMana = battleState.enemyTeamMana,
                        onClick = { if (isTarget2) onAttackTarget(2) }
                    )

                    BattleCardWidget(
                        card = card3,
                        isSelectedAttacker = false,
                        isValidTarget = isTarget3,
                        combatTexts = battleState.combatTexts,
                        attackAnimation = battleState.activeAttackAnimation,
                        dotHitTargets = battleState.dotHitTargets,
                        teamMana = battleState.enemyTeamMana,
                        onClick = { if (isTarget3) onAttackTarget(3) }
                    )
                }

                // Enemy Front Row (Slots 0, 1)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val card0 = battleState.enemyCards.firstOrNull { it.slotIndex == 0 }
                    val card1 = battleState.enemyCards.firstOrNull { it.slotIndex == 1 }

                    val isTarget0 = selectedAttacker != null && card0 != null && card0.isAlive &&
                            canCardTargetSlot(selectedAttacker, 0, battleState.enemyCards)
                    val isTarget1 = selectedAttacker != null && card1 != null && card1.isAlive &&
                            canCardTargetSlot(selectedAttacker, 1, battleState.enemyCards)

                    BattleCardWidget(
                        card = card0,
                        isSelectedAttacker = false,
                        isValidTarget = isTarget0,
                        combatTexts = battleState.combatTexts,
                        attackAnimation = battleState.activeAttackAnimation,
                        dotHitTargets = battleState.dotHitTargets,
                        teamMana = battleState.enemyTeamMana,
                        onClick = { if (isTarget0) onAttackTarget(0) }
                    )

                    BattleCardWidget(
                        card = card1,
                        isSelectedAttacker = false,
                        isValidTarget = isTarget1,
                        combatTexts = battleState.combatTexts,
                        attackAnimation = battleState.activeAttackAnimation,
                        dotHitTargets = battleState.dotHitTargets,
                        teamMana = battleState.enemyTeamMana,
                        onClick = { if (isTarget1) onAttackTarget(1) }
                    )
                }
            }

            // CENTER DIVIDER & ROUND BANNER (Matching Image 1)
            CenterRoundDivider(
                round = battleState.round,
                phase = battleState.phase,
                selectedAttacker = selectedAttacker,
                totalPlayerMana = totalPlayerMana,
                onCastSkill = onCastSkill
            )

            // PLAYER 2x2 BOARD
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Player Front Row (Slots 0, 1)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val card0 = battleState.playerCards.firstOrNull { it.slotIndex == 0 }
                    val card1 = battleState.playerCards.firstOrNull { it.slotIndex == 1 }

                    BattleCardWidget(
                        card = card0,
                        isSelectedAttacker = battleState.selectedPlayerSlot == 0,
                        isValidTarget = false,
                        combatTexts = battleState.combatTexts,
                        attackAnimation = battleState.activeAttackAnimation,
                        dotHitTargets = battleState.dotHitTargets,
                        teamMana = battleState.playerTeamMana,
                        onClick = { onSelectPlayerSlot(0) }
                    )

                    BattleCardWidget(
                        card = card1,
                        isSelectedAttacker = battleState.selectedPlayerSlot == 1,
                        isValidTarget = false,
                        combatTexts = battleState.combatTexts,
                        attackAnimation = battleState.activeAttackAnimation,
                        dotHitTargets = battleState.dotHitTargets,
                        teamMana = battleState.playerTeamMana,
                        onClick = { onSelectPlayerSlot(1) }
                    )
                }

                // Player Back Row (Slots 2, 3)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val card2 = battleState.playerCards.firstOrNull { it.slotIndex == 2 }
                    val card3 = battleState.playerCards.firstOrNull { it.slotIndex == 3 }

                    BattleCardWidget(
                        card = card2,
                        isSelectedAttacker = battleState.selectedPlayerSlot == 2,
                        isValidTarget = false,
                        combatTexts = battleState.combatTexts,
                        attackAnimation = battleState.activeAttackAnimation,
                        dotHitTargets = battleState.dotHitTargets,
                        teamMana = battleState.playerTeamMana,
                        onClick = { onSelectPlayerSlot(2) }
                    )

                    BattleCardWidget(
                        card = card3,
                        isSelectedAttacker = battleState.selectedPlayerSlot == 3,
                        isValidTarget = false,
                        combatTexts = battleState.combatTexts,
                        attackAnimation = battleState.activeAttackAnimation,
                        dotHitTargets = battleState.dotHitTargets,
                        teamMana = battleState.playerTeamMana,
                        onClick = { onSelectPlayerSlot(3) }
                    )
                }
            }

            // BOTTOM COMBAT LOG
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .padding(bottom = 2.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xCC110B20),
                border = BorderStroke(1.dp, Color(0xFF3B2E58))
            ) {
                val latestLog = battleState.actionLogs.lastOrNull()?.text
                    ?: tr("Битва началась! Выберите карту и цель.", "Battle started! Select a card and target.")
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "📜 $latestLog",
                        color = Color(0xFFEDE4FF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // SURRENDER CONFIRMATION DIALOG
        if (showSurrenderConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showSurrenderConfirmDialog = false },
                title = {
                    Text(
                        text = tr("Выход из боя", "Leave Battle"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                },
                text = {
                    Text(
                        text = tr(
                            "Если уйдете сейчас, ваш отряд автоматически проиграет",
                            "If you leave now, your squad will automatically lose"
                        ),
                        fontSize = 13.sp,
                        color = Color(0xFFDDD2F0),
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showSurrenderConfirmDialog = false
                            onSurrender()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD32F2F),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = tr("Уйти", "Leave"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showSurrenderConfirmDialog = false },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFF5A497A)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = tr("Остаться", "Stay"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                },
                containerColor = GameSurfaceDark,
                shape = RoundedCornerShape(14.dp)
            )
        }

        // ARENA FIRST TURN MODAL
        if (battleState.showArenaFirstTurnModal) {
            ArenaFirstTurnDialog(
                enemyName = battleState.enemyName,
                isPlayerFirstTurn = battleState.isPlayerFirstTurn,
                onDismiss = onDismissFirstTurnModal
            )
        }

        // BATTLE START HEROINE QUOTE BANNER
        val startQuote = battleState.battleStartQuote
        AnimatedVisibility(
            visible = startQuote != null &&
                !dismissedStartQuote &&
                !battleState.showArenaFirstTurnModal &&
                battleState.phase != BattlePhase.VICTORY &&
                battleState.phase != BattlePhase.DEFEAT,
            enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) + slideInVertically { it / 2 },
            exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(300)) + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp, vertical = 44.dp)
        ) {
            if (startQuote != null) {
                HeroQuoteBanner(
                    quote = startQuote,
                    onClick = { dismissedStartQuote = true }
                )
            }
        }

        // VICTORY / DEFEAT MODAL OVERLAY
        if (battleState.phase == BattlePhase.VICTORY || battleState.phase == BattlePhase.DEFEAT) {
            BattleOutcomeOverlay(
                isVictory = battleState.phase == BattlePhase.VICTORY,
                state = battleState,
                onDismiss = onExitBattle,
                onClaimColiseumPrize = onClaimColiseumPrize,
                onContinueColiseum = onContinueColiseum
            )
        }
    }
}

@Composable
fun BattleTopBar(
    enemyName: String,
    enemyTitle: String,
    isAutoBattle: Boolean,
    battleSpeed: Float,
    onToggleAuto: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    onSurrender: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Row 1: Full-width title with exit ✕ at top right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF7B1FA2),
                    border = BorderStroke(1.2.dp, GameGold),
                    modifier = Modifier.size(26.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "👑", fontSize = 13.sp)
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = enemyName,
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Close / Surrender Button ✕
            IconButton(
                onClick = onSurrender,
                modifier = Modifier
                    .testTag("surrender_battle")
                    .size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = tr("Выход", "Exit"),
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Row 2: Difficulty level subtitle ("Карьера (Ур. сложности X)")
        Text(
            text = enemyTitle,
            color = Color(0xFFB5A7CE),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 32.dp)
        )

        // Row 3: Speed toggle (1x / 2x) and Auto-battle button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speed Button (1x / 2x)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (battleSpeed > 1f) GameGold else Color(0xFF261D3B),
                    border = BorderStroke(1.dp, GameGold),
                    modifier = Modifier.clickable {
                        onSetSpeed(if (battleSpeed == 1f) 2f else 1f)
                    }
                ) {
                    Text(
                        text = "${battleSpeed.toInt()}x",
                        color = if (battleSpeed > 1f) Color.Black else GameGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                // Auto-Battle Button
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isAutoBattle) Color(0xFF4CAF50) else Color(0xFF261D3B),
                    border = BorderStroke(1.dp, if (isAutoBattle) Color(0xFF81C784) else Color(0xFF4C3E6B)),
                    modifier = Modifier
                        .testTag("toggle_auto_battle")
                        .clickable(onClick = onToggleAuto)
                ) {
                    Text(
                        text = if (isAutoBattle) tr("АВТО: ВКЛ", "AUTO: ON") else tr("Автобой", "Auto"),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CenterRoundDivider(
    round: Int,
    phase: BattlePhase,
    selectedAttacker: BattleCard?,
    totalPlayerMana: Int,
    onCastSkill: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xDD1B142F),
        border = BorderStroke(1.dp, GameGold.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = tr("Раунд $round", "Round $round"),
                    color = GameGoldLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = when (phase) {
                        BattlePhase.PLAYER_TURN -> tr("Ваш ход", "Your Turn")
                        BattlePhase.ENEMY_TURN -> tr("Ход врага...", "Enemy Turn...")
                        else -> ""
                    },
                    color = if (phase == BattlePhase.PLAYER_TURN) Color(0xFF81C784) else Color(0xFFFFB74D),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Dedicated Mana Counter widget during battle: displays total accumulated team mana
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF101738),
                border = BorderStroke(1.dp, Color(0xFF42A5F5).copy(alpha = 0.8f)),
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .testTag("player_mana_counter")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = "⚡", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = tr("Мана: $totalPlayerMana", "Mana: $totalPlayerMana"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE3F2FD),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Cast Skill Button if active attacker has magic unlocked and enough energy
            if (selectedAttacker != null) {
                if (selectedAttacker.skill != null && selectedAttacker.hasMagicUnlocked) {
                    val hasEnergy = totalPlayerMana >= selectedAttacker.skill.energyCost
                    Button(
                        onClick = onCastSkill,
                        enabled = hasEnergy && phase == BattlePhase.PLAYER_TURN,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GameGold,
                            contentColor = Color.Black,
                            disabledContainerColor = Color(0xFF2D2344),
                            disabledContentColor = Color(0xFF6B5E86)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("cast_skill_button")
                    ) {
                        Text("⚡", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${selectedAttacker.skill.localizedName()} (${selectedAttacker.skill.energyCost}⚡)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF231B36),
                        border = BorderStroke(1.dp, Color(0xFF453664))
                    ) {
                        Text(
                            text = tr("🔒 Магия с 3 ур.", "🔒 Magic at Lv.3"),
                            color = Color(0xFFB5A7CE),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BattleOutcomeOverlay(
    isVictory: Boolean,
    state: BattleState,
    onDismiss: () -> Unit,
    onClaimColiseumPrize: () -> Unit = onDismiss,
    onContinueColiseum: () -> Unit = {}
) {
    var showAmuletPopup by remember(state.foundAmulet, isVictory) { mutableStateOf(isVictory && state.foundAmulet != null) }
    var showVictoryEventPopup by remember(state.victoryEventMessage, isVictory) { mutableStateOf(isVictory && state.victoryEventMessage != null) }

    if (showAmuletPopup && isVictory && state.foundAmulet != null) {
        val amulet = state.foundAmulet
        val amuletTitle = localizeAmuletName(amulet.id, amulet.name)
        val amuletNote = localizeAmuletNote(amulet.id, amulet.extraNote)
        AlertDialog(
            onDismissRequest = { showAmuletPopup = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr("Победа!", "Victory!"),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = GameGoldLight
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF2B1D45),
                        border = BorderStroke(1.5.dp, GameGold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Image(
                                painter = painterResource(id = amulet.imageRes),
                                contentDescription = amuletTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(2.dp, GameGold, RoundedCornerShape(12.dp))
                            )
                            Text(
                                text = tr(
                                    "Вы нашли $amuletTitle. Поздравляем!",
                                    "You found the $amuletTitle. Congratulations!"
                                ),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            if (!amuletNote.isNullOrBlank()) {
                                Text(
                                    text = amuletNote,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFFD54F),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAmuletPopup = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GameGold,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("close_amulet_dialog_button")
                ) {
                    Text(tr("Закрыть", "Close"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            },
            containerColor = Color(0xFF1D142E)
        )
    } else if (showVictoryEventPopup && state.victoryEventMessage != null) {
        AlertDialog(
            onDismissRequest = { showVictoryEventPopup = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr("Великая Победа!", "Grand Victory!"),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = GameGoldLight
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF2B1D45),
                        border = BorderStroke(1.5.dp, GameGold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(text = "👑", fontSize = 42.sp)
                            Text(
                                text = state.victoryEventMessage,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showVictoryEventPopup = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GameGold,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("close_final_victory_dialog_button")
                ) {
                    Text(tr("Закрыть", "Close"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            },
            containerColor = Color(0xFF1D142E)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE6000000))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 460.dp)
                .testTag(if (isVictory) "victory_dialog" else "defeat_dialog"),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(2.dp, if (isVictory) GameGold else Color(0xFFFF5252)),
            colors = CardDefaults.cardColors(containerColor = GameSurfaceDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header Banner
                Text(
                    text = if (isVictory) tr("ПОБЕДА!", "VICTORY!") else tr("ПОРАЖЕНИЕ", "DEFEAT"),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isVictory) GameGoldLight else Color(0xFFFF5252)
                )

                // Battle Type on separate line
                Text(
                    text = when {
                        state.isColiseum -> tr("Колизей • Битва №${state.coliseumBattleNumber}", "Coliseum • Battle #${state.coliseumBattleNumber}")
                        state.isTower -> if (state.towerFloor == TOWER_ROOF_FLOOR) {
                            tr("Башня • Крыша", "Trial Tower • The Roof")
                        } else {
                            tr("Башня Дня • Этап ${state.towerFloor}/$TOWER_TOTAL_FLOORS", "Trial Tower • Floor ${state.towerFloor}/$TOWER_TOTAL_FLOORS")
                        }
                        state.isArena -> tr("Арена", "Arena")
                        else -> tr("Карьера", "Career")
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = GameGoldLight
                )

                // Location / Enemy on separate line
                Text(
                    text = state.enemyName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                // Difficulty / Rating on separate line
                Text(
                    text = state.enemyTitle,
                    fontSize = 12.sp,
                    color = Color(0xFFB5A7CE),
                    textAlign = TextAlign.Center
                )

                if (isVictory) {
                    if (state.isColiseum) {
                        state.victoryQuote?.let { vQuote ->
                            HeroQuoteBanner(
                                quote = vQuote,
                                onClick = {},
                                modifier = Modifier.testTag("victory_hero_quote")
                            )
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF140D22),
                            border = BorderStroke(1.dp, Color(0xFF453664))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = tr("Награда за бой №${state.coliseumBattleNumber}:", "Battle #${state.coliseumBattleNumber} Reward:"),
                                    fontSize = 11.5.sp,
                                    color = Color(0xFFBCAFD4),
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Text(
                                        text = "+${state.rewardGold} 🪙",
                                        color = GameGold,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "+${state.rewardGems} 💎",
                                        color = Color(0xFF40C4FF),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp
                                    )
                                }

                                HorizontalDivider(color = Color(0xFF33274E))

                                Text(
                                    text = tr("Всего накоплено за забег:", "Total Accumulated Run Prize:"),
                                    fontSize = 12.sp,
                                    color = GameGoldLight,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Text(
                                        text = "${state.coliseumAccumulatedGold} 🪙",
                                        color = GameGold,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "${state.coliseumAccumulatedGems} 💎",
                                        color = Color(0xFF40C4FF),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }

                        Text(
                            text = tr("Раундов боя: ${state.round}", "Rounds: ${state.round}"),
                            fontSize = 10.5.sp,
                            color = Color(0xFFAFA2C6)
                        )

                        Button(
                            onClick = onContinueColiseum,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("coliseum_continue_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GameGold,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            AutoShrinkText(
                                text = tr("Продолжить", "Continue"),
                                color = Color.Black,
                                initialFontSizeSp = 14f,
                                minFontSizeSp = 10f,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        OutlinedButton(
                            onClick = onClaimColiseumPrize,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("coliseum_claim_exit_button"),
                            border = BorderStroke(1.5.dp, GameGold),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GameGoldLight),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            AutoShrinkText(
                                text = tr("Забрать приз и выйти", "Claim Prize & Exit"),
                                color = GameGoldLight,
                                initialFontSizeSp = 13.5f,
                                minFontSizeSp = 9.5f,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    } else {
                        val survivedHeroes = state.playerCards.count { it.isAlive }
                        val starsCount = survivedHeroes.coerceIn(1, 4)

                        // Stars Display
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(4) { starIndex ->
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (starIndex < starsCount) GameGold else Color(0xFF4C3D66),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Text(
                            text = tr("$starsCount из 4 звёзд", "$starsCount of 4 stars"),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = GameGoldLight,
                            textAlign = TextAlign.Center
                        )

                        state.victoryQuote?.let { vQuote ->
                            HeroQuoteBanner(
                                quote = vQuote,
                                onClick = {},
                                modifier = Modifier.testTag("victory_hero_quote")
                            )
                        }

                        // Calculated Victory Rewards Box
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF140D22),
                            border = BorderStroke(1.dp, Color(0xFF453664))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = tr("Награды за бой:", "Battle Rewards:"),
                                    fontSize = 11.sp,
                                    color = GameGoldLight,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    Text(
                                        text = "+${state.rewardGold} 💰",
                                        color = GameGold,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "+${state.rewardGems} 💎",
                                        color = Color(0xFF40C4FF),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = tr("+${state.rewardExp} Опыт ⭐", "+${state.rewardExp} EXP ⭐"),
                                        color = Color(0xFFB388FF),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp
                                    )
                                }

                                // Potential Artifact Drop
                                state.rewardArtifact?.let { art ->
                                    HorizontalDivider(color = Color(0xFF33274E), modifier = Modifier.padding(vertical = 2.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(text = tr("Трофей:", "Loot:"), fontSize = 10.5.sp, color = Color.White)
                                            Text(
                                                text = "${art.iconSymbol} ${art.localizedName()} (${art.rarity.localizedName()})",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = getRarityColor(art.rarity)
                                            )
                                        }
                                        val effectDesc = art.localizedEffectDescription()
                                        if (effectDesc.isNotBlank()) {
                                            Text(
                                                text = effectDesc,
                                                fontSize = 9.5.sp,
                                                color = Color(0xFFFFD54F)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Battle Statistics Summary
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Text(
                                text = tr("Раундов боя: ${state.round}", "Rounds: ${state.round}"),
                                fontSize = 10.sp,
                                color = Color(0xFFAFA2C6)
                            )
                            Text(
                                text = when {
                                    state.isTower -> tr("Башня Дня", "Trial Tower")
                                    state.isArena -> tr("Арена (+28 🏆)", "Arena (+28 🏆)")
                                    else -> tr("Карьера", "Career")
                                },
                                fontSize = 10.sp,
                                color = Color(0xFFAFA2C6)
                            )
                        }

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dismiss_outcome_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GameGold,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = tr("ЗАБРАТЬ НАГРАДУ", "CLAIM REWARD"),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    Text(
                        text = if (state.isColiseum) {
                            tr(
                                "Ваш отряд пал в Колизее! Вы получаете утешительный приз (20% от награды):",
                                "Your squad fell in the Coliseum! You receive a 20% consolation prize:"
                            )
                        } else {
                            tr(
                                "Ваш отряд пал в бою. Прокачайте уровень героинь, экипируйте артефакты в Кузнице или смените расстановку карт!",
                                "Your squad fell in battle. Level up your heroines, equip stronger artifacts in the Forge, or adjust your lineup!"
                            )
                        },
                        fontSize = 11.5.sp,
                        color = Color(0xFFCCC0E6),
                        textAlign = TextAlign.Center
                    )

                    if (state.isColiseum) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1A1024),
                            border = BorderStroke(1.dp, GameGold.copy(alpha = 0.7f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = tr("Утешительный приз (20%):", "Consolation Prize (20%):"),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GameGoldLight
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Text(
                                        text = "+${state.coliseumConsolationGold} 🪙",
                                        color = GameGold,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "+${state.coliseumConsolationGems} 💎",
                                        color = Color(0xFF40C4FF),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1F1218),
                        border = BorderStroke(1.dp, Color(0xFF5A2532))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Text(
                                text = tr("Раундов боя: ${state.round}", "Rounds: ${state.round}"),
                                fontSize = 10.sp,
                                color = Color(0xFFFF8A80)
                            )
                            Text(
                                text = tr(
                                    "Врагов осталось: ${state.enemyCards.count { it.isAlive }}",
                                    "Enemies left: ${state.enemyCards.count { it.isAlive }}"
                                ),
                                fontSize = 10.sp,
                                color = Color(0xFFFF8A80)
                            )
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dismiss_outcome_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF42355C),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = tr("ВЕРНУТЬСЯ", "RETURN"),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ArenaFirstTurnDialog(
    enemyName: String,
    isPlayerFirstTurn: Boolean,
    onDismiss: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(5000L)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth(0.88f)
                .padding(16.dp)
                .testTag("arena_first_turn_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = GameSurfaceDark),
            border = BorderStroke(2.dp, if (isPlayerFirstTurn) GameGold else Color(0xFFFF5252))
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Top-right close button 'X'
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(36.dp)
                        .testTag("close_arena_first_turn_modal")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = tr("Закрыть", "Close"),
                        tint = Color(0xFFAFA2C6),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isPlayerFirstTurn) Color(0xFF2E7D32) else Color(0xFFC62828),
                        border = BorderStroke(2.dp, if (isPlayerFirstTurn) Color(0xFF81C784) else Color(0xFFFF8A80)),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (isPlayerFirstTurn) "⚔️" else "🛡️",
                                fontSize = 26.sp
                            )
                        }
                    }

                    Text(
                        text = tr("Арена: Определение хода", "Arena: Turn Order"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB5A7CE),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = if (isPlayerFirstTurn) {
                            tr("Вы ходите первым!", "You go first!")
                        } else {
                            tr("Первый ход делает $enemyName", "$enemyName goes first")
                        },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isPlayerFirstTurn) GameGoldLight else Color(0xFFFF8A80),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Text(
                        text = tr("Окно закроется автоматически через 5 сек.", "Closes automatically in 5 sec."),
                        fontSize = 11.sp,
                        color = Color(0xFF8E82A8),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun HeroQuoteBanner(
    quote: HeroBattleQuote,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val frameColor = getCardFrameColor(quote.heroLevel)
    val localizedSpeakerName = localizeHeroNameByIdOrRu(quote.heroId, quote.heroName)
    val dialogAvatarRes = remember(quote.heroId, quote.imageRes, quote.heroLevel) {
        com.example.model.resolveHeroFaceDrawableRes(quote.heroId, quote.imageRes, quote.heroLevel)
    }
    val hasFacePreview = remember(quote.heroId, quote.heroLevel) {
        com.example.model.resolveHeroFaceDrawableRes(quote.heroId, 0, quote.heroLevel) != 0
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .testTag("hero_quote_banner")
            .clickable(onClick = onClick)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xF2180F2A),
            border = BorderStroke(1.5.dp, Color(0xFFFFD54F)),
            shadowElevation = 10.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 12.dp, top = 14.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF281D42))
                        .border(1.8.dp, frameColor, CircleShape)
                ) {
                    com.example.ui.components.HeroPortraitImage(
                        imageRes = dialogAvatarRes,
                        heroId = quote.heroId,
                        heroName = localizedSpeakerName,
                        heroLevel = quote.heroLevel,
                        contentDescription = localizedSpeakerName,
                        contentScale = if (hasFacePreview) ContentScale.Fit else ContentScale.Crop,
                        alignment = if (hasFacePreview) Alignment.BottomCenter else Alignment.TopCenter,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (hasFacePreview) Modifier.padding(top = 2.dp) else Modifier)
                    )
                }
                Text(
                    text = tr("«${quote.text}»", "\"${quote.text}\""),
                    color = Color(0xFFFFF9E6),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 15.5.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 74.dp, y = (-10).dp),
            shape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp, topEnd = 4.dp, bottomStart = 4.dp),
            color = Color(0xFF341F58),
            border = BorderStroke(1.2.dp, Color(0xFFFFD54F)),
            shadowElevation = 4.dp
        ) {
            Text(
                text = localizedSpeakerName,
                color = Color(0xFFFFE082),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
            )
        }
    }
}
