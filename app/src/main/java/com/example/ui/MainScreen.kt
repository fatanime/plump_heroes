package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.res.painterResource
import com.example.R
import kotlinx.coroutines.delay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.i18n.GameLocaleState
import com.example.i18n.WORLD_LORE_EN
import com.example.i18n.localizedName
import com.example.i18n.localizedTitle
import com.example.i18n.tr
import com.example.model.*
import com.example.ui.components.AutoShrinkText
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: GameViewModel) {
    val battleState by viewModel.battleState.collectAsStateWithLifecycle()
    val playerProfile by viewModel.playerProfile.collectAsStateWithLifecycle()
    val allHeroes by viewModel.allHeroes.collectAsStateWithLifecycle()
    val inventoryArtifacts by viewModel.inventoryArtifacts.collectAsStateWithLifecycle()
    val campaignChapters by viewModel.campaignChapters.collectAsStateWithLifecycle()
    val arenaBots by viewModel.arenaBots.collectAsStateWithLifecycle()
    val inspectedHero by viewModel.inspectedHero.collectAsStateWithLifecycle()
    val forgeSelectedArtifactIds by viewModel.forgeSelectedArtifactIds.collectAsStateWithLifecycle()
    val latestDropMessage by viewModel.latestDropMessage.collectAsStateWithLifecycle()
    val dailyReward by viewModel.dailyBonusReward.collectAsStateWithLifecycle()
    val fortuneWheelOffer by viewModel.fortuneWheelOffer.collectAsStateWithLifecycle()
    val fortuneWheelResult by viewModel.fortuneWheelResult.collectAsStateWithLifecycle()
    val selectedCampaignChapterId by viewModel.selectedCampaignChapterId.collectAsStateWithLifecycle()
    val isCampaign100Percent by viewModel.isCampaign100Percent.collectAsStateWithLifecycle()
    val showCampaign100AchievementDialog by viewModel.showCampaign100AchievementDialog.collectAsStateWithLifecycle()
    val dailyQuests by viewModel.dailyQuests.collectAsStateWithLifecycle()
    val chestOpeningResult by viewModel.chestOpeningResult.collectAsStateWithLifecycle()
    val secretHeroUnlockedPopup by viewModel.secretHeroUnlockedPopup.collectAsStateWithLifecycle()
    val levelUpRewardPopup by viewModel.levelUpRewardPopup.collectAsStateWithLifecycle()
    val isColiseumOpen by viewModel.isColiseumOpen.collectAsStateWithLifecycle()
    val coliseumDeckHeroIds by viewModel.coliseumDeckHeroIds.collectAsStateWithLifecycle()
    val towerDeckHeroIds by viewModel.towerDeckHeroIds.collectAsStateWithLifecycle()
    val coliseumRunState by viewModel.coliseumRunState.collectAsStateWithLifecycle()
    val coliseumBattlePreview by viewModel.coliseumBattlePreview.collectAsStateWithLifecycle()
    val isColiseumPreviewVisible by viewModel.isColiseumPreviewVisible.collectAsStateWithLifecycle()
    val showColiseumTutorial by viewModel.showColiseumTutorial.collectAsStateWithLifecycle()
    val coliseumMilestoneBonusOffer by viewModel.coliseumMilestoneBonusOffer.collectAsStateWithLifecycle()
    val unclaimedQuestsCount = remember(dailyQuests) {
        dailyQuests.count { it.isCompleted && !it.isRewardClaimed }
    }

    var selectedNavTab by remember { mutableIntStateOf(2) } // 0: Герои, 1: Кузница, 2: Хаб, 3: Сундуки, 4: Трофеи
    var activeHubSection by remember { mutableStateOf<HubBattleSection?>(null) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var isWorldLoreOpen by remember { mutableStateOf(false) }
    var showDeckInvalidDialog by remember { mutableStateOf(false) }
    var showBonusStageImage by remember { mutableStateOf(false) }
    var showDailyQuestsDialog by remember { mutableStateOf(false) }
    var showCurrencyExchangeDialog by remember { mutableStateOf(false) }

    val isTutorialActive = playerProfile.isStoryRead && !playerProfile.isTutorialCompleted
    var tutorialStep by remember { mutableIntStateOf(1) }

    LaunchedEffect(tutorialStep, isTutorialActive) {
        if (isTutorialActive) {
            val frame = com.example.ui.components.getTutorialFrame(tutorialStep, playerProfile.name)
            selectedNavTab = frame.targetTab
            activeHubSection = frame.targetHubSection
        }
    }

    LaunchedEffect(latestDropMessage) {
        if (latestDropMessage != null) {
            delay(10_000L)
            viewModel.dismissDropMessage()
        }
    }

    // Back handling
    BackHandler(enabled = true) {
        when {
            showBonusStageImage -> showBonusStageImage = false
            showCurrencyExchangeDialog -> showCurrencyExchangeDialog = false
            levelUpRewardPopup != null -> viewModel.dismissLevelUpRewardPopup()
            secretHeroUnlockedPopup != null -> viewModel.dismissSecretHeroUnlockedPopup()
            chestOpeningResult != null -> viewModel.dismissChestOpening()
            showDailyQuestsDialog -> showDailyQuestsDialog = false
            showCampaign100AchievementDialog -> viewModel.dismissCampaign100Achievement()
            isWorldLoreOpen -> isWorldLoreOpen = false
            !playerProfile.isStoryRead -> { /* Read lore first */ }
            isTutorialActive -> { /* Block back action while tutorial is active */ }
            isSettingsOpen -> isSettingsOpen = false
            inspectedHero != null -> viewModel.inspectHero(null)
            battleState != null -> viewModel.exitBattle()
            isColiseumOpen -> {
                when {
                    coliseumBattlePreview != null && isColiseumPreviewVisible -> {
                        if (coliseumBattlePreview?.battleNumber == 1) {
                            viewModel.cancelColiseumBattle1Preview()
                        } else {
                            viewModel.dismissColiseumBattlePreview()
                        }
                    }
                    coliseumMilestoneBonusOffer != null -> { /* Modal confirmation required */ }
                    coliseumRunState != null -> viewModel.claimColiseumPrizeAndExit()
                    else -> viewModel.closeColiseumToMenu()
                }
            }
            selectedNavTab == 2 && activeHubSection != null -> activeHubSection = null
            selectedNavTab != 2 -> {
                selectedNavTab = 2
                activeHubSection = null
            }
        }
    }

    // If in Settings, show full screen Settings
    if (isSettingsOpen) {
        SettingsScreen(
            profile = playerProfile,
            onUpdateName = { newName -> viewModel.updatePlayerName(newName) },
            onToggleGoogleSync = { enable, email -> viewModel.toggleGoogleSync(enable, email) },
            onToggleSoundMuted = { viewModel.toggleSoundMuted() },
            onExportJson = { viewModel.exportSaveToJson() },
            onImportJson = { json -> viewModel.importSaveFromJson(json) },
            onBack = { isSettingsOpen = false }
        )
        return
    }

    // If in battle, show full screen battle
    if (battleState != null) {
        BattleScreen(
            battleState = battleState!!,
            canCardTargetSlot = { attacker, slot, list -> viewModel.canCardTargetSlot(attacker, slot, list) },
            onSelectPlayerSlot = { slot -> viewModel.selectPlayerSlot(slot) },
            onAttackTarget = { targetSlot -> viewModel.executePlayerAttack(targetSlot) },
            onCastSkill = { viewModel.executePlayerSkill() },
            onToggleAuto = { viewModel.toggleAutoBattle() },
            onSetSpeed = { speed -> viewModel.setBattleSpeed(speed) },
            onExitBattle = { viewModel.exitBattle() },
            onSurrender = { viewModel.surrenderBattle() },
            onDismissFirstTurnModal = { viewModel.dismissArenaFirstTurnModal() },
            onClaimColiseumPrize = { viewModel.claimColiseumPrizeAndExit() },
            onContinueColiseum = { viewModel.continueColiseumAfterVictory() }
        )
        return
    }

    // If in Coliseum mode (setup, preview, or milestone), show full screen Coliseum
    if (isColiseumOpen) {
        Box(modifier = Modifier.fillMaxSize()) {
            ColiseumScreen(
                profile = playerProfile,
                heroes = allHeroes,
                artifacts = inventoryArtifacts,
                coliseumDeckHeroIds = coliseumDeckHeroIds,
                runState = coliseumRunState,
                battlePreview = coliseumBattlePreview,
                isPreviewVisible = isColiseumPreviewVisible,
                milestoneBonusOffer = coliseumMilestoneBonusOffer,
                showTutorial = showColiseumTutorial,
                onBackToMenu = { viewModel.closeColiseumToMenu() },
                onAssignHeroToSlot = { slot, heroId -> viewModel.assignHeroToColiseumSlot(slot, heroId) },
                onInspectHero = { hero -> viewModel.inspectHero(hero) },
                onStartColiseum = { viewModel.startColiseumRunAndPreviewFirstBattle() },
                onCancelBattle1Preview = { viewModel.cancelColiseumBattle1Preview() },
                onDismissBattlePreview = { viewModel.dismissColiseumBattlePreview() },
                onReopenBattlePreview = { viewModel.reopenColiseumBattlePreview() },
                onLeaveColiseum = { viewModel.claimColiseumPrizeAndExit() },
                onConfirmBattle = { viewModel.confirmStartPreviewedColiseumBattle() },
                onApplyMilestoneHeal = { viewModel.applyColiseumMilestoneHeal() },
                onApplyMilestoneRevive = { slot -> viewModel.applyColiseumMilestoneRevive(slot) },
                onApplyMilestoneMana = { viewModel.applyColiseumMilestoneMana() },
                onCompleteTutorial = { viewModel.completeColiseumTutorial() }
            )

            inspectedHero?.let { hero ->
                HeroDetailDialog(
                    hero = hero,
                    allArtifacts = inventoryArtifacts,
                    playerGold = playerProfile.gold,
                    allHeroes = allHeroes,
                    onDismiss = { viewModel.inspectHero(null) },
                    onUpgradeLevel = { id -> viewModel.upgradeHeroLevel(id) },
                    onSelectSkill = { heroId, skillId -> viewModel.selectActiveSkill(heroId, skillId) },
                    onEquipArtifact = { heroId, artifactId -> viewModel.equipArtifact(heroId, artifactId) },
                    onUnequipArtifact = { heroId, artifactId -> viewModel.unequipArtifact(heroId, artifactId) }
                )
            }

            latestDropMessage?.let { msg ->
                Surface(
                    color = Color(0xEE1E152F),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, GameGold),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                        .clickable { viewModel.dismissDropMessage() }
                        .testTag("drop_notification_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = msg,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "✕", color = Color(0xFFAEA3C5), fontSize = 13.sp)
                    }
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            // Global Header with Level, Name & Currencies
            Surface(
                color = GameSurfaceDark,
                border = BorderStroke(1.dp, color = Color(0xFF382B52)),
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Player Level, Name + Settings & Daily Quests button right next to gear
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .testTag("profile_settings_entry")
                                    .clickable(enabled = !isTutorialActive) { isSettingsOpen = true }
                                    .padding(2.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = GameGold,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${playerProfile.level}",
                                            style = TextStyle(
                                                fontSize = 13.sp,
                                                lineHeight = 13.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.Black,
                                                textAlign = TextAlign.Center,
                                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                                lineHeightStyle = LineHeightStyle(
                                                    alignment = LineHeightStyle.Alignment.Center,
                                                    trim = LineHeightStyle.Trim.Both
                                                )
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val displayName = if (playerProfile.name == "Хранительница" && GameLocaleState.isEnglish) {
                                            "Guardian"
                                        } else {
                                            playerProfile.name
                                        }
                                        Text(
                                            text = displayName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            color = GameGoldLight,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "⚙️",
                                            fontSize = 10.sp
                                        )
                                    }
                                    Text(
                                        text = "🏆 ${playerProfile.trophies} • 1.2 Release",
                                        fontSize = 9.sp,
                                        color = Color(0xFFFFCA28),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(5.dp))

                            // Daily Quests Button right next to the Settings gear
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .padding(end = if (unclaimedQuestsCount > 0) 5.dp else 0.dp)
                                    .clickable(enabled = !isTutorialActive) {
                                        viewModel.refreshDailyQuestsIfNeeded()
                                        showDailyQuestsDialog = true
                                    }
                                    .testTag("open_daily_quests_button")
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (unclaimedQuestsCount > 0) Color(0xFF3E2B14) else Color(0xFF281E38),
                                    border = BorderStroke(
                                        width = if (unclaimedQuestsCount > 0) 1.4.dp else 1.dp,
                                        color = if (unclaimedQuestsCount > 0) GameGold else Color(0xFF5A4380)
                                    ),
                                    modifier = Modifier.size(width = 36.dp, height = 32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "📜",
                                            style = TextStyle(
                                                fontSize = 14.sp,
                                                lineHeight = 14.sp,
                                                textAlign = TextAlign.Center,
                                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                                lineHeightStyle = LineHeightStyle(
                                                    alignment = LineHeightStyle.Alignment.Center,
                                                    trim = LineHeightStyle.Trim.Both
                                                )
                                            )
                                        )
                                    }
                                }
                                if (unclaimedQuestsCount > 0) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 5.dp, y = (-5).dp)
                                            .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                                            .background(GameGold, CircleShape)
                                            .border(1.5.dp, Color(0xFF140D22), CircleShape)
                                            .padding(horizontal = 3.dp)
                                    ) {
                                        Text(
                                            text = "$unclaimedQuestsCount",
                                            style = TextStyle(
                                                fontSize = 10.sp,
                                                lineHeight = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.Black,
                                                textAlign = TextAlign.Center,
                                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                                lineHeightStyle = LineHeightStyle(
                                                    alignment = LineHeightStyle.Alignment.Center,
                                                    trim = LineHeightStyle.Trim.Both
                                                )
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Currencies (Gold, Gems, Arena Keys)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Gold (clickable -> opens currency conversion popup)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF281E38),
                                border = BorderStroke(1.dp, Color(0xFF4C376A)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(enabled = !isTutorialActive) { showCurrencyExchangeDialog = true }
                                    .testTag("gold_currency_chip")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "🪙", fontSize = 10.5.sp)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "${playerProfile.gold}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.5.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            // Gems (clickable -> opens identical currency conversion popup)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF281E38),
                                border = BorderStroke(1.dp, Color(0xFF4C376A)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(enabled = !isTutorialActive) { showCurrencyExchangeDialog = true }
                                    .testTag("gems_currency_chip")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "💎", fontSize = 10.5.sp)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "${playerProfile.gems}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.5.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            // Arena Keys
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF281E38),
                                border = BorderStroke(1.dp, Color(0xFF4C376A))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "🔑", fontSize = 10.5.sp)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "${playerProfile.arenaKeys}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.5.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Player EXP Bar (Spacious & clearly visible text with ample padding)
                    val expFraction = (playerProfile.exp.toFloat() / playerProfile.maxExp.toFloat()).coerceIn(0f, 1f)
                    val expRemaining = (playerProfile.maxExp - playerProfile.exp).coerceAtLeast(0)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(22.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(Color(0xFF201733))
                            .border(1.dp, Color(0xFF5E4582), RoundedCornerShape(11.dp))
                    ) {
                        // Filled bar
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = expFraction)
                                .clip(RoundedCornerShape(11.dp))
                                .background(
                                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                                        listOf(Color(0xFF7B1FA2), Color(0xFFAB47BC), Color(0xFFFFD54F))
                                    )
                                )
                        )
                        // Text inside bar
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tr(
                                    "Опыт: ${playerProfile.exp}/${playerProfile.maxExp} XP",
                                    "EXP: ${playerProfile.exp}/${playerProfile.maxExp} XP"
                                ),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = tr(
                                    "До ${playerProfile.level + 1} ур.: $expRemaining XP",
                                    "To Lv. ${playerProfile.level + 1}: $expRemaining XP"
                                ),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = GameGoldLight,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        },
        bottomBar = {
            Surface(
                color = GameSurfaceDark,
                border = BorderStroke(1.dp, Color(0xFF382B52)),
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(100f)
            ) {
                NavigationBar(
                    containerColor = GameSurfaceDark,
                    tonalElevation = 8.dp,
                    windowInsets = NavigationBarDefaults.windowInsets
                ) {
                NavigationBarItem(
                    selected = selectedNavTab == 0,
                    onClick = {
                        if (!isTutorialActive) {
                            selectedNavTab = 0
                        }
                    },
                    icon = { Icon(Icons.Default.Person, contentDescription = tr("Герои", "Heroes")) },
                    label = { Text(tr("Герои", "Heroes"), fontSize = 10.sp, maxLines = 1, softWrap = false) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GameGold,
                        selectedTextColor = GameGold,
                        unselectedIconColor = Color(0xFF8F82A8),
                        unselectedTextColor = Color(0xFF8F82A8),
                        indicatorColor = Color(0xFF382A52)
                    ),
                    modifier = Modifier.testTag("nav_roster")
                )

                NavigationBarItem(
                    selected = selectedNavTab == 1,
                    onClick = {
                        if (!isTutorialActive) {
                            selectedNavTab = 1
                        }
                    },
                    icon = { Icon(Icons.Default.Build, contentDescription = tr("Кузница", "Forge")) },
                    label = { Text(tr("Кузница", "Forge"), fontSize = 10.sp, maxLines = 1, softWrap = false) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GameGold,
                        selectedTextColor = GameGold,
                        unselectedIconColor = Color(0xFF8F82A8),
                        unselectedTextColor = Color(0xFF8F82A8),
                        indicatorColor = Color(0xFF382A52)
                    ),
                    modifier = Modifier.testTag("nav_forge")
                )

                NavigationBarItem(
                    selected = selectedNavTab == 2,
                    onClick = {
                        if (!isTutorialActive) {
                            selectedNavTab = 2
                            activeHubSection = null
                        }
                    },
                    icon = { Icon(painterResource(R.drawable.ic_nav_shield), contentDescription = tr("Хаб", "Hub"), modifier = Modifier.size(24.dp)) },
                    label = { Text(tr("Хаб", "Hub"), fontSize = 10.sp, maxLines = 1, softWrap = false) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GameGold,
                        selectedTextColor = GameGold,
                        unselectedIconColor = Color(0xFF8F82A8),
                        unselectedTextColor = Color(0xFF8F82A8),
                        indicatorColor = Color(0xFF382A52)
                    ),
                    modifier = Modifier.testTag("nav_hub")
                )

                NavigationBarItem(
                    selected = selectedNavTab == 3,
                    onClick = {
                        if (!isTutorialActive) {
                            selectedNavTab = 3
                        }
                    },
                    icon = { Icon(painterResource(R.drawable.ic_nav_gift), contentDescription = tr("Сундуки", "Chests"), modifier = Modifier.size(24.dp)) },
                    label = { Text(tr("Сундуки", "Chests"), fontSize = 10.sp, maxLines = 1, softWrap = false) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GameGold,
                        selectedTextColor = GameGold,
                        unselectedIconColor = Color(0xFF8F82A8),
                        unselectedTextColor = Color(0xFF8F82A8),
                        indicatorColor = Color(0xFF382A52)
                    ),
                    modifier = Modifier.testTag("nav_summon")
                )

                NavigationBarItem(
                    selected = selectedNavTab == 4,
                    onClick = {
                        if (!isTutorialActive) {
                            selectedNavTab = 4
                        }
                    },
                    icon = { Icon(Icons.Default.Star, contentDescription = tr("Трофеи", "Trophies")) },
                    label = { Text(tr("Трофеи", "Trophies"), fontSize = 10.sp, maxLines = 1, softWrap = false) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GameGold,
                        selectedTextColor = GameGold,
                        unselectedIconColor = Color(0xFF8F82A8),
                        unselectedTextColor = Color(0xFF8F82A8),
                        indicatorColor = Color(0xFF382A52)
                    ),
                    modifier = Modifier.testTag("nav_collection")
                )
            }
        }
    }
) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedNavTab) {
                0 -> RosterScreen(
                    heroes = allHeroes,
                    activeDeckHeroIds = playerProfile.activeDeckHeroIds,
                    artifacts = inventoryArtifacts,
                    onSelectHeroForDetails = { hero -> viewModel.inspectHero(hero) },
                    onAssignHeroToSlot = { slot, heroId -> viewModel.assignHeroToDeckSlot(slot, heroId) },
                    onShowLockedMessage = { msg -> viewModel.showLockedHeroMessage(msg) },
                    onOpenSecretHero = { heroId -> viewModel.unlockSecretHero(heroId) }
                )
                1 -> ForgeScreen(
                    artifacts = inventoryArtifacts,
                    selectedArtifactIds = forgeSelectedArtifactIds,
                    equippedArtifactIds = allHeroes.flatMap { it.equippedArtifactIds }.toSet(),
                    playerGold = playerProfile.gold,
                    onToggleSelectArtifact = { id -> viewModel.toggleForgeArtifactSelection(id) },
                    onCombineArtifacts = { viewModel.combineArtifacts() },
                    onSellArtifact = { id, price -> viewModel.sellArtifact(id, price) }
                )
                2 -> {
                    when (activeHubSection) {
                        HubBattleSection.CAREER -> CampaignScreen(
                            chapters = campaignChapters,
                            playerLevel = playerProfile.level,
                            selectedChapterId = selectedCampaignChapterId,
                            onSelectChapter = { chapterId -> viewModel.selectCampaignChapter(chapterId) },
                            onStartBattle = { stage ->
                                if (viewModel.isDeckValid()) {
                                    viewModel.startCampaignBattle(stage)
                                } else {
                                    showDeckInvalidDialog = true
                                }
                            },
                            onBackToHub = { activeHubSection = null }
                        )
                        HubBattleSection.ARENA -> ArenaScreen(
                            profile = playerProfile,
                            arenaBots = arenaBots,
                            allHeroes = allHeroes,
                            onRefreshBots = { viewModel.refreshArenaBots() },
                            onChallengeBot = { bot ->
                                if (viewModel.isDeckValid()) {
                                    viewModel.startArenaBattle(bot)
                                } else {
                                    showDeckInvalidDialog = true
                                }
                            },
                            onBackToHub = { activeHubSection = null }
                        )
                        HubBattleSection.TOWER -> {
                            val towerRule = resolveTowerDayRule()
                            val effectiveTowerDeck = viewModel.getEffectiveTowerDeckIds()
                            val towerFloors = remember(
                                playerProfile.level,
                                effectiveTowerDeck,
                                allHeroes,
                                towerRule
                            ) {
                                viewModel.getTowerFloorsForToday()
                            }
                            val roofFloor = remember(
                                playerProfile.level,
                                effectiveTowerDeck,
                                allHeroes,
                                towerRule
                            ) {
                                viewModel.getTowerRoofFloorForToday()
                            }
                            val isTowerDeckValid = viewModel.isDeckValid(effectiveTowerDeck)
                            val isDeckEligible = remember(towerRule, allHeroes, effectiveTowerDeck) {
                                viewModel.isDeckEligibleForTowerRule(towerRule, allHeroes, effectiveTowerDeck)
                            }
                            TowerScreen(
                                profile = playerProfile,
                                allHeroes = allHeroes,
                                artifacts = inventoryArtifacts,
                                towerDeckHeroIds = effectiveTowerDeck,
                                floors = towerFloors,
                                roofFloor = roofFloor,
                                isDeckValid = isTowerDeckValid,
                                isDeckEligibleForRule = isDeckEligible,
                                showTutorial = !playerProfile.isTowerTutorialCompleted,
                                onBackToHub = { activeHubSection = null },
                                onAssignHeroToSlot = { slot, id -> viewModel.assignHeroToTowerSlot(slot, id) },
                                onInspectHero = { hero -> viewModel.inspectHero(hero) },
                                onStartFloor = { floorItem ->
                                    if (isTowerDeckValid) {
                                        viewModel.startTowerBattle(floorItem)
                                    } else {
                                        showDeckInvalidDialog = true
                                    }
                                },
                                onCompleteTutorial = { viewModel.completeTowerTutorial() }
                            )
                        }
                        null -> BattleHubScreen(
                            profile = playerProfile,
                            chapters = campaignChapters,
                            isCampaign100Percent = isCampaign100Percent,
                            isDeckValid = viewModel.isDeckValid(),
                            onOpenCareer = {
                                if (viewModel.isDeckValid()) {
                                    activeHubSection = HubBattleSection.CAREER
                                } else {
                                    showDeckInvalidDialog = true
                                }
                            },
                            onOpenArena = {
                                if (viewModel.isDeckValid()) {
                                    activeHubSection = HubBattleSection.ARENA
                                } else {
                                    showDeckInvalidDialog = true
                                }
                            },
                            onOpenColiseum = {
                                if (viewModel.isDeckValid()) {
                                    viewModel.openColiseumScreen()
                                } else {
                                    showDeckInvalidDialog = true
                                }
                            },
                            onOpenTower = {
                                if (!viewModel.isDeckValid()) {
                                    showDeckInvalidDialog = true
                                } else if (viewModel.openTowerScreen()) {
                                    activeHubSection = HubBattleSection.TOWER
                                }
                            },
                            onFixDeck = { selectedNavTab = 0 }
                        )
                    }
                }
                3 -> SummonScreen(
                    profile = playerProfile,
                    onOpenChest = { type -> viewModel.openSummonChest(type) }
                )
                4 -> CollectionScreen(
                    profile = playerProfile,
                    heroes = allHeroes,
                    chapters = campaignChapters,
                    isCampaign100Percent = isCampaign100Percent,
                    onOpenBonusGallery = { showBonusStageImage = true },
                    onOpenWorldLore = { isWorldLoreOpen = true }
                )
            }

            // Hero Details Inspector Modal
            inspectedHero?.let { hero ->
                HeroDetailDialog(
                    hero = hero,
                    allArtifacts = inventoryArtifacts,
                    playerGold = playerProfile.gold,
                    allHeroes = allHeroes,
                    onDismiss = { viewModel.inspectHero(null) },
                    onUpgradeLevel = { id -> viewModel.upgradeHeroLevel(id) },
                    onSelectSkill = { heroId, skillId -> viewModel.selectActiveSkill(heroId, skillId) },
                    onEquipArtifact = { heroId, artifactId -> viewModel.equipArtifact(heroId, artifactId) },
                    onUnequipArtifact = { heroId, artifactId -> viewModel.unequipArtifact(heroId, artifactId) }
                )
            }

            // Daily Login Bonus Modal Dialog (only displayed after tutorial is finished)
            if (!isTutorialActive) {
                dailyReward?.let { reward ->
                    AlertDialog(
                        onDismissRequest = { viewModel.dismissDailyBonus() },
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = tr("🎁 Ежедневный бонус", "🎁 Daily Bonus"),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
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
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF2B1D45),
                                    border = BorderStroke(1.5.dp, GameGold),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = tr("День ${reward.dayNumber} подряд! 🗓️", "Day ${reward.dayNumber} in a row! 🗓️"),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFFD54F)
                                        )
                                        Text(
                                            text = "+${reward.goldAmount} 💰",
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                        reward.artifact?.let { art ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFF1E1430),
                                                border = BorderStroke(1.dp, Color(0xFFAB47BC)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Text(
                                                        text = "✨ ${art.localizedName()} (${art.rarity.localizedName()})",
                                                        fontSize = 11.sp,
                                                        color = Color(0xFFE1BEE7),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                Text(
                                    text = tr(
                                        "Заходите каждый день подряд, чтобы получать всё более ценные награды и уникальные артефакты!",
                                        "Log in every day in a row to claim even richer rewards and unique artifacts!"
                                    ),
                                    fontSize = 11.sp,
                                    color = Color(0xFFAFA2C6),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = { viewModel.claimDailyBonus() },
                                colors = ButtonDefaults.buttonColors(containerColor = GameGold, contentColor = Color.Black),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("claim_daily_bonus_button")
                            ) {
                                Text(tr("ПОЛУЧИТЬ", "CLAIM"), fontWeight = FontWeight.Black, fontSize = 14.sp)
                            }
                        },
                        containerColor = GameSurfaceDark
                    )
                }

                fortuneWheelOffer?.let { wheelOffer ->
                    FortuneWheelDialog(
                        offer = wheelOffer,
                        result = fortuneWheelResult,
                        onSpinComplete = { sector -> viewModel.claimFortuneWheelReward(sector) },
                        onDismiss = { viewModel.dismissFortuneWheel() }
                    )
                }
            }

            // Deck Invalid Alert Dialog
            if (showDeckInvalidDialog) {
                AlertDialog(
                    onDismissRequest = { showDeckInvalidDialog = false },
                    title = {
                        Text(
                            text = tr("Боевая колода не собрана!", "Battle Deck Incomplete!"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color(0xFFFFD54F)
                        )
                    },
                    text = {
                        Text(
                            text = tr(
                                "В боевой колоде должно быть ровно 4 уникальные героини без пустых слотов и дубликатов. Перейдите во вкладку «Герои» и заполните все 4 слота боевой колоды перед битвой.",
                                "Your battle deck must contain 4 unique heroines with no empty slots or duplicates. Open the Heroes tab and fill all 4 slots before entering battle."
                            ),
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDeckInvalidDialog = false
                                selectedNavTab = 0
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GameGold, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(tr("Собрать", "Build Deck"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeckInvalidDialog = false }) {
                            Text(tr("Отмена", "Cancel"), color = Color(0xFFAFA2C6))
                        }
                    },
                    containerColor = GameCardBg,
                    shape = RoundedCornerShape(14.dp)
                )
            }

            // Drop/Unlock Notification Banner (auto-dismisses after 10 seconds)
            latestDropMessage?.let { msg ->
                Surface(
                    color = Color(0xEE1E152F),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, GameGold),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                        .clickable { viewModel.dismissDropMessage() }
                        .testTag("drop_notification_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = msg,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "✕", color = Color(0xFFAEA3C5), fontSize = 13.sp)
                    }
                }
            }

            // World Lore Modal before tutorial or from Settings
            if (!playerProfile.isStoryRead || isWorldLoreOpen) {
                WorldLoreDialog(
                    onDismiss = {
                        viewModel.markStoryRead()
                        isWorldLoreOpen = false
                    }
                )
            }

            // Tutorial Dialogue Overlay (Frames 1-24)
            if (isTutorialActive) {
                com.example.ui.components.TutorialOverlay(
                    step = tutorialStep,
                    playerName = playerProfile.name,
                    onAdvanceStep = {
                        val next = tutorialStep + 1
                        if (next <= 28) {
                            tutorialStep = next
                            val nextFrame = com.example.ui.components.getTutorialFrame(next, playerProfile.name)
                            selectedNavTab = nextFrame.targetTab
                            activeHubSection = nextFrame.targetHubSection
                        } else {
                            viewModel.completeTutorial()
                            selectedNavTab = 2
                            activeHubSection = null
                        }
                    },
                    onSkipToFinal = {
                        tutorialStep = 28
                        selectedNavTab = 2
                        activeHubSection = null
                    },
                    onStartFromFrame5 = {
                        tutorialStep = 5
                        selectedNavTab = 0
                        activeHubSection = null
                    },
                    onFinishTutorial = {
                        viewModel.completeTutorial()
                        selectedNavTab = 2
                        activeHubSection = null
                    }
                )
            }

            // 100% Career Completion Achievement Popup
            if (showCampaign100AchievementDialog && !isTutorialActive) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissCampaign100Achievement() },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = tr("🏆 Достижение!", "🏆 Achievement!"),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = GameGoldLight
                            )
                        }
                    },
                    text = {
                        Text(
                            text = tr(
                                "Вы прошли уровни на 100%! Вам доступен бонус в конце 4 главы!",
                                "You cleared all levels at 100%! A bonus awaits at the end of Chapter 4!"
                            ),
                            fontSize = 14.sp,
                            color = Color.White,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.dismissCampaign100Achievement() },
                            colors = ButtonDefaults.buttonColors(containerColor = GameGold, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dismiss_campaign_100_achievement")
                        ) {
                            Text(tr("Отлично", "Awesome"), fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                    },
                    containerColor = GameSurfaceDark,
                    shape = RoundedCornerShape(16.dp)
                )
            }

            // Daily Quests Popup Dialog
            if (showDailyQuestsDialog && !isTutorialActive) {
                DailyQuestsDialog(
                    quests = dailyQuests,
                    onClaimReward = { questId -> viewModel.claimDailyQuestReward(questId) },
                    onDismiss = { showDailyQuestsDialog = false }
                )
            }

            // Animated Chest Opening & Loot Reveal Dialog
            chestOpeningResult?.let { result ->
                ChestOpeningDialog(
                    result = result,
                    onDismiss = { viewModel.dismissChestOpening() }
                )
            }

            // Secret Character Unlocked Popup Dialog
            secretHeroUnlockedPopup?.let { unlockedSecret ->
                AlertDialog(
                    onDismissRequest = { viewModel.dismissSecretHeroUnlockedPopup() },
                    modifier = Modifier.testTag("secret_hero_unlocked_dialog"),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = tr("✨ Секретный персонаж!", "✨ Secret Character!"),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = Color(0xFF80DEEA),
                                textAlign = TextAlign.Center
                            )
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(126.dp)
                                    .height(176.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(2.dp, Color(0xFF4DD0E1), RoundedCornerShape(14.dp))
                            ) {
                                com.example.ui.components.HeroPortraitImage(
                                    imageRes = unlockedSecret.imageRes,
                                    heroId = unlockedSecret.id,
                                    heroName = unlockedSecret.localizedName(),
                                    heroLevel = unlockedSecret.level,
                                    contentDescription = unlockedSecret.localizedName(),
                                    contentScale = ContentScale.Crop,
                                    alignment = Alignment.TopCenter,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Text(
                                text = tr(
                                    "Вы открыли секретного персонажа!",
                                    "You unlocked the secret character!"
                                ),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = GameGoldLight,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "${unlockedSecret.localizedName()} — ${unlockedSecret.localizedTitle()}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF80DEEA),
                                textAlign = TextAlign.Center
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.dismissSecretHeroUnlockedPopup() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4DD0E1),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dismiss_secret_hero_unlocked")
                        ) {
                            Text(tr("Отлично!", "Awesome!"), fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                    },
                    containerColor = GameSurfaceDark,
                    shape = RoundedCornerShape(16.dp)
                )
            }

            // Level-Up Celebration & Proportional Reward Popup
            if (!isTutorialActive) {
                levelUpRewardPopup?.let { popup ->
                    AlertDialog(
                        onDismissRequest = { viewModel.dismissLevelUpRewardPopup() },
                        modifier = Modifier.testTag("level_up_reward_dialog"),
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = tr("⭐ Повышение уровня!", "⭐ Level Up!"),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = GameGoldLight,
                                    textAlign = TextAlign.Center
                                )
                            }
                        },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = tr(
                                        "Поздравляем!\nТеперь у вас\n${popup.newLevel} уровень!\nВаша награда:",
                                        "Congratulations!\nNow you have\nLevel ${popup.newLevel}!\nYour reward:"
                                    ),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 22.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("level_up_message_text")
                                )
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF2B1D45),
                                    border = BorderStroke(1.5.dp, GameGold),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = tr(
                                            "${popup.goldReward} монет 🪙 и ${popup.gemsReward} алмазов 💎",
                                            "${popup.goldReward} coins 🪙 and ${popup.gemsReward} gems 💎"
                                        ),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        color = GameGoldLight,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 10.dp)
                                            .testTag("level_up_reward_amount_text")
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = { viewModel.dismissLevelUpRewardPopup() },
                                colors = ButtonDefaults.buttonColors(containerColor = GameGold, contentColor = Color.Black),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dismiss_level_up_dialog")
                            ) {
                                Text(tr("Отлично!", "Awesome!"), fontWeight = FontWeight.Black, fontSize = 14.sp)
                            }
                        },
                        containerColor = GameSurfaceDark,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            // Currency Conversion Modal Dialog (Coins <-> Gems)
            if (showCurrencyExchangeDialog && !isTutorialActive) {
                CurrencyExchangeDialog(
                    gold = playerProfile.gold,
                    gems = playerProfile.gems,
                    onConvertGoldToGems = { viewModel.convertGoldToGems() },
                    onConvertGemsToGold = { viewModel.convertGemsToGold() },
                    onDismiss = { showCurrencyExchangeDialog = false }
                )
            }

            // 2-Column Bonus Gallery (heroine portraits by level + Career finale after 100%)
            if (showBonusStageImage) {
                BonusGalleryDialog(
                    heroes = allHeroes,
                    isCampaign100Percent = isCampaign100Percent,
                    viewedImageKeys = playerProfile.viewedGalleryImageKeys,
                    onMarkImageViewed = { key -> viewModel.markBonusGalleryImageViewed(key) },
                    onDismiss = { showBonusStageImage = false }
                )
            }
        }
    }
}

@Composable
fun CurrencyExchangeDialog(
    gold: Int,
    gems: Int,
    onConvertGoldToGems: () -> Unit,
    onConvertGemsToGold: () -> Unit,
    onDismiss: () -> Unit
) {
    val canConvertGoldToGems = gold >= 2500
    val canConvertGemsToGold = gems >= 100

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = GameSurfaceDark,
            border = BorderStroke(1.5.dp, GameGold),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .testTag("currency_exchange_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    AutoShrinkText(
                        text = tr("Курс обмена", "Exchange Rate"),
                        color = GameGoldLight,
                        initialFontSizeSp = 18f,
                        minFontSizeSp = 12f,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 40.dp)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(48.dp)
                            .testTag("close_currency_exchange_dialog")
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
                    color = Color(0xFF221836),
                    border = BorderStroke(1.dp, Color(0xFF4C376A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AutoShrinkText(
                            text = tr("2500 монет на 100 алмазов.", "2500 coins for 100 gems."),
                            color = Color.White,
                            initialFontSizeSp = 13.5f,
                            minFontSizeSp = 9f,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        AutoShrinkText(
                            text = tr("100 алмазов на 2500 монет.", "100 gems for 2500 coins."),
                            color = Color.White,
                            initialFontSizeSp = 13.5f,
                            minFontSizeSp = 9f,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                AutoShrinkText(
                    text = tr("Заменить валюту на:", "Convert currency to:"),
                    color = GameGoldLight,
                    initialFontSizeSp = 14f,
                    minFontSizeSp = 10f,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                when {
                    canConvertGoldToGems && canConvertGemsToGold -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("exchange_both_buttons_row"),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = onConvertGoldToGems,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFBA68C8),
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("exchange_gold_to_gems_button")
                            ) {
                                AutoShrinkText(
                                    text = "100 💎",
                                    color = Color.Black,
                                    initialFontSizeSp = 14f,
                                    minFontSizeSp = 10f,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Button(
                                onClick = onConvertGemsToGold,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GameGold,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("exchange_gems_to_gold_button")
                            ) {
                                AutoShrinkText(
                                    text = "2500 💰",
                                    color = Color.Black,
                                    initialFontSizeSp = 14f,
                                    minFontSizeSp = 10f,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    canConvertGoldToGems -> {
                        Button(
                            onClick = onConvertGoldToGems,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFBA68C8),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("exchange_gold_to_gems_button")
                        ) {
                            AutoShrinkText(
                                text = "100 💎",
                                color = Color.Black,
                                initialFontSizeSp = 14f,
                                minFontSizeSp = 10f,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    canConvertGemsToGold -> {
                        Button(
                            onClick = onConvertGemsToGold,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GameGold,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("exchange_gems_to_gold_button")
                        ) {
                            AutoShrinkText(
                                text = "2500 💰",
                                color = Color.Black,
                                initialFontSizeSp = 14f,
                                minFontSizeSp = 10f,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    else -> {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF2B213E),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AutoShrinkText(
                                text = tr(
                                    "Нужно 2500 монет или 100 алмазов для обмена",
                                    "Need 2500 coins or 100 gems to exchange"
                                ),
                                color = Color(0xFFAFA2C6),
                                initialFontSizeSp = 12f,
                                minFontSizeSp = 8.5f,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorldLoreDialog(
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = tr("📜 История Аврезии", "📜 Lore of Avresia"),
                    color = GameGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1B1428))
                    .border(1.dp, Color(0xFF382A52), RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(end = 6.dp)
                ) {
                    Text(
                        text = tr(
                            "Извержение вулкана Патрея потрясло Аврезию: лавовые потоки обошли Храм Огня, но последовавшее землетрясение раскрыло зловещий разлом — из него хлынули демонические сущности. Среди тех, кто бежал от хаоса, — послушница Берта. В Безымянном лесу её нашла охотница Брунгильда и, узнав о беде, поспешила донести весть до Туверска.\n\n" +
                                "Царь Владимир II отверг рассказ как выдумку, но командующая Ольга поверила подруге Брунгильде и, объединившись с ней и Бертой, двинулась прочь от очага зла. У Болотного моря к ним присоединилась ведьмочка Моргана: она раскрыла новую угрозу — демоны научились принимать облик близких и сеять смятение.\n\n" +
                                "Теперь судьба Аврезии в руках четырёх девушек. Чтобы запечатать разлом, им предстоит собрать четыре древних амулета четырёх сторон света (Юга, Востока, Севера и Запада), одолеть одержимых владык и вернуть покой родным землям.",
                            WORLD_LORE_EN
                        ),
                        color = Color(0xFFE8DEF8),
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Start
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2E7D32),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("lore_continue_button")
            ) {
                Text(
                    text = tr("Закрыть", "Close"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        },
        containerColor = GameCardBg,
        shape = RoundedCornerShape(16.dp)
    )
}


