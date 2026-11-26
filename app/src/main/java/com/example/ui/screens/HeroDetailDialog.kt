package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.i18n.localizedDescription
import com.example.i18n.localizedLore
import com.example.i18n.localizedName
import com.example.i18n.localizedRarityLabel
import com.example.i18n.localizedTitle
import com.example.i18n.tr
import com.example.model.*
import com.example.ui.components.ZoomableGalleryContainer
import com.example.ui.components.getRarityColor
import com.example.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeroDetailDialog(
    hero: HeroCard,
    allArtifacts: List<Artifact>,
    playerGold: Int,
    allHeroes: List<HeroCard> = emptyList(),
    onDismiss: () -> Unit,
    onUpgradeLevel: (String) -> Unit,
    onSelectSkill: (String, String) -> Unit,
    onEquipArtifact: (String, String) -> Unit,
    onUnequipArtifact: (String, String) -> Unit
) {
    val rarityColor = if (hero.isSecret) Color(0xFF4DD0E1) else getRarityColor(hero.rarity)
    val frameColor = com.example.ui.components.getCardFrameColor(hero.level)
    var selectedTab by remember { mutableStateOf(0) } // 0: Характеристики, 1: Артефакты, 2: Способности
    var choosingArtifactForSlot by remember { mutableStateOf<ArtifactSlot?>(null) }
    var showFullScreenPortrait by remember(hero.id) { mutableStateOf(false) }
    val unlockedGalleryImages = remember(hero.id, hero.level, hero.imageRes) {
        getUnlockedHeroGalleryImages(hero.id, hero.level, hero.imageRes)
    }
    var galleryIndex by remember(hero.id, hero.level) {
        mutableIntStateOf((unlockedGalleryImages.size - 1).coerceAtLeast(0))
    }

    val totalAtk = hero.calculateTotalAtk(allArtifacts)
    val totalArmor = hero.calculateTotalArmor(allArtifacts)
    val totalHp = hero.calculateTotalHp(allArtifacts)

    val equippedOnOtherHeroes = allHeroes.filter { it.id != hero.id }.flatMap { it.equippedArtifactIds }.toSet()
    val equippedArtifacts = hero.equippedArtifactIds.mapNotNull { id -> allArtifacts.firstOrNull { it.id == id } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(20.dp)),
            color = GameBgDark,
            border = BorderStroke(2.dp, frameColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Bar with Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = hero.localizedName(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = rarityColor.copy(alpha = 0.25f),
                                border = BorderStroke(1.dp, rarityColor)
                            ) {
                                Text(
                                    text = hero.localizedRarityLabel(),
                                    color = rarityColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = hero.localizedTitle(),
                            fontSize = 12.sp,
                            color = Color(0xFFC9BDDF)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .testTag("close_hero_detail")
                            .size(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = tr("Закрыть", "Close"), tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Hero Visual & Quick Stats Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .background(GameSurfaceDark, RoundedCornerShape(14.dp))
                        .border(1.dp, Color(0xFF3F3359), RoundedCornerShape(14.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Portrait Box (Long press opens full-screen portrait / gallery)
                    Box(
                        modifier = Modifier
                            .width(115.dp)
                            .height(170.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.5.dp, rarityColor, RoundedCornerShape(10.dp))
                            .testTag("hero_detail_portrait")
                            .combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    galleryIndex = (unlockedGalleryImages.size - 1).coerceAtLeast(0)
                                    showFullScreenPortrait = true
                                }
                            )
                    ) {
                        com.example.ui.components.HeroPortraitImage(
                            imageRes = hero.imageRes,
                            heroId = hero.id,
                            heroName = hero.localizedName(),
                            heroLevel = hero.level,
                            contentDescription = hero.localizedName(),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Surface(
                            shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
                            color = Color(0xDD000000),
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            Text(
                                text = hero.heroType.localizedName(),
                                color = GameGoldLight,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Stat Breakdown & Upgrade preview
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .wrapContentHeight(),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        // Level
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = GameGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = tr("Уровень ${hero.level} / ${hero.maxUpgradeLevel()}", "Level ${hero.level} / ${hero.maxUpgradeLevel()}"),
                                color = GameGoldLight,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // ATK
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = tr("⚔️ Атака:", "⚔️ Attack:"), color = Color(0xFFDDD2F0), fontSize = 11.5.sp, maxLines = 1, softWrap = false)
                            Text(text = "$totalAtk", color = StatAtkBlue, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }

                        // ARMOR
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = tr("🛡️ Броня:", "🛡️ Armor:"), color = Color(0xFFDDD2F0), fontSize = 11.5.sp, maxLines = 1, softWrap = false)
                            Text(text = "$totalArmor", color = StatArmorSteel, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }

                        // HP
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = tr("❤️ Здоровье:", "❤️ Health:"), color = Color(0xFFDDD2F0), fontSize = 11.5.sp, maxLines = 1, softWrap = false)
                            Text(text = "$totalHp", color = StatHpRed, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }

                        if (hero.level < hero.maxUpgradeLevel()) {
                            Spacer(modifier = Modifier.height(4.dp))

                            // Shards Progress (With spacious padding)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF140D24), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = tr("Осколки:", "Shards:"), fontSize = 10.sp, color = Color(0xFFAFA2C6), maxLines = 1, softWrap = false)
                                    Text(text = "${hero.shards} / ${hero.shardsNeeded}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GameGold, maxLines = 1, softWrap = false)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { (hero.shards.toFloat() / hero.shardsNeeded).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = GameGold,
                                    trackColor = Color(0xFF2A203F)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Level Up Button
                val goldCost = hero.level * 200
                val canUpgrade = playerGold >= goldCost && hero.shards >= hero.shardsNeeded && hero.level < hero.maxUpgradeLevel()

                Button(
                    onClick = { onUpgradeLevel(hero.id) },
                    enabled = canUpgrade,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("upgrade_hero_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GameGold,
                        contentColor = Color.Black,
                        disabledContainerColor = Color(0xFF322A45),
                        disabledContentColor = Color(0xFF756A8C)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (hero.level >= hero.maxUpgradeLevel()) {
                            tr("МАКСИМАЛЬНЫЙ УРОВЕНЬ", "MAX LEVEL")
                        } else {
                            tr(
                                "Прокачать ур. ${hero.level + 1} ($goldCost 💰 + ${hero.shardsNeeded} 🧩)",
                                "Upgrade Lv. ${hero.level + 1} ($goldCost 💰 + ${hero.shardsNeeded} 🧩)"
                            )
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Navigation Tabs (Single-line without wrapping!)
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = GameSurfaceDark,
                    contentColor = GameGold
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = tr("Артефакты", "Artifacts"),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = tr("Способности", "Skills"),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Text(
                                text = tr("История", "Lore"),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tab Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(GameSurfaceDark, RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            // Artifacts Slots (Weapon, Armor, Relic)
                            ArtifactsTabContent(
                                hero = hero,
                                allArtifacts = allArtifacts,
                                equippedArtifacts = equippedArtifacts,
                                equippedOnOtherHeroes = equippedOnOtherHeroes,
                                choosingArtifactForSlot = choosingArtifactForSlot,
                                onChooseSlot = { slot -> choosingArtifactForSlot = slot },
                                onEquip = { artId ->
                                    onEquipArtifact(hero.id, artId)
                                    choosingArtifactForSlot = null
                                },
                                onUnequip = { artId -> onUnequipArtifact(hero.id, artId) }
                            )
                        }
                        1 -> {
                            // Ability / Skill Customization
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (hero.level < 3) {
                                    Surface(
                                        color = Color(0xFF381F26),
                                        border = BorderStroke(1.dp, Color(0xFFE57373)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = tr(
                                                "🔒 На 1-2 уровне у карт нет магии! Первая способность откроется после прокачки до 3 уровня.",
                                                "🔒 Cards have no magic at Level 1-2! The first skill unlocks once upgraded to Level 3."
                                            ),
                                            color = Color(0xFFFFCDD2),
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                } else {
                                    Text(
                                        text = tr(
                                            "Выберите активную способность для битвы:",
                                            "Select an active skill for battle:"
                                        ),
                                        fontSize = 11.sp,
                                        color = Color(0xFFC6BAE2)
                                    )
                                }

                                hero.availableSkills.forEach { skill ->
                                    val isUnlocked = hero.level >= skill.unlockLevel
                                    val isSelectedSkill = isUnlocked && skill.id == hero.activeSkillId
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("skill_${skill.id}")
                                            .clickable(enabled = isUnlocked) { onSelectSkill(hero.id, skill.id) },
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(
                                            if (isSelectedSkill) 2.dp else 1.dp,
                                            if (isSelectedSkill) GameGold else if (isUnlocked) Color(0xFF453860) else Color(0xFF2E2442)
                                        ),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelectedSkill) Color(0xFF33274F) else if (isUnlocked) GameCardBg else Color(0xFF191325)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = skill.localizedName(),
                                                        color = if (isSelectedSkill) GameGold else if (isUnlocked) Color.White else Color.Gray,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    if (isUnlocked) {
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = Color(0x33FFFFFF)
                                                        ) {
                                                            Text(
                                                                text = tr("Энергия: ${skill.energyCost} ⚡", "Energy: ${skill.energyCost} ⚡"),
                                                                fontSize = 9.sp,
                                                                color = GameGoldLight,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    } else {
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = Color(0xFF2A1C38)
                                                        ) {
                                                            Text(
                                                                text = tr("🔒 С ${skill.unlockLevel} уровня", "🔒 Unlocks at Lv. ${skill.unlockLevel}"),
                                                                fontSize = 9.sp,
                                                                color = Color(0xFFFFB74D),
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = skill.localizedDescription(),
                                                    fontSize = 11.sp,
                                                    color = if (isUnlocked) Color(0xFFCCC0E6) else Color.DarkGray
                                                )
                                            }

                                            if (isSelectedSkill) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = GameGold,
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = tr("Выбрано", "Selected"),
                                                            tint = Color.Black,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            // Lore / Bio
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = tr("О героине", "About Heroine"),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GameGoldLight
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = hero.localizedLore(),
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    color = Color(0xFFDDD2F0)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = tr("Тип атаки: ${hero.heroType.localizedName()}", "Attack Type: ${hero.heroType.localizedName()}"),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = hero.heroType.localizedDescription(),
                                    fontSize = 11.sp,
                                    color = Color(0xFFB5A8D1)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = tr(
                                        "Обычная атака: ${hero.attackPattern.localizedTitle()}",
                                        "Basic Attack: ${hero.attackPattern.localizedTitle()}"
                                    ),
                                    fontSize = 11.sp,
                                    color = Color(0xFFD1C4E9)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFullScreenPortrait && unlockedGalleryImages.isNotEmpty()) {
        val safeIndex = galleryIndex.coerceIn(0, unlockedGalleryImages.lastIndex)
        Dialog(
            onDismissRequest = { showFullScreenPortrait = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.90f))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { showFullScreenPortrait = false }
                    .testTag("fullscreen_portrait_overlay")
            ) {
                // Full adaptive pinch-zoomable portrait image (1.0x..2.0x) with top/bottom margins
                ZoomableGalleryContainer(
                    resetKey = unlockedGalleryImages[safeIndex],
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(top = 60.dp, bottom = 52.dp, start = 16.dp, end = 16.dp)
                ) {
                    com.example.ui.components.HeroPortraitImage(
                        imageRes = unlockedGalleryImages[safeIndex],
                        heroId = hero.id,
                        heroName = hero.localizedName(),
                        heroLevel = hero.level,
                        contentDescription = "${hero.localizedName()} (${safeIndex + 1}/${unlockedGalleryImages.size})",
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("fullscreen_portrait_image")
                    )
                }

                // Top-right close button with top and side margin
                Surface(
                    shape = CircleShape,
                    color = Color(0xCC181028),
                    border = BorderStroke(1.dp, Color(0x88FFFFFF)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 12.dp, end = 12.dp)
                ) {
                    IconButton(
                        onClick = { showFullScreenPortrait = false },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_fullscreen_portrait")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = tr("Закрыть", "Close"),
                            tint = Color.White
                        )
                    }
                }

                // Gallery arrows when multiple level-unlocked images exist (earlier images left, ascending order)
                if (unlockedGalleryImages.size > 1) {
                    val canGoLeft = safeIndex > 0
                    val canGoRight = safeIndex < unlockedGalleryImages.lastIndex

                    Surface(
                        shape = CircleShape,
                        color = if (canGoLeft) Color(0xCC1B1230) else Color(0x551B1230),
                        border = BorderStroke(1.dp, if (canGoLeft) GameGold else Color(0x44FFFFFF)),
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 10.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (canGoLeft) galleryIndex = safeIndex - 1
                            },
                            enabled = canGoLeft,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("portrait_gallery_prev")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = tr("Предыдущее изображение", "Previous image"),
                                tint = if (canGoLeft) Color.White else Color(0x66FFFFFF),
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = if (canGoRight) Color(0xCC1B1230) else Color(0x551B1230),
                        border = BorderStroke(1.dp, if (canGoRight) GameGold else Color(0x44FFFFFF)),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 10.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (canGoRight) galleryIndex = safeIndex + 1
                            },
                            enabled = canGoRight,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("portrait_gallery_next")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = tr("Следующее изображение", "Next image"),
                                tint = if (canGoRight) Color.White else Color(0x66FFFFFF),
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xCC181028),
                        border = BorderStroke(1.dp, Color(0x66FFD54F)),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = "${safeIndex + 1} / ${unlockedGalleryImages.size}",
                            color = GameGoldLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ArtifactsTabContent(
    hero: HeroCard,
    allArtifacts: List<Artifact>,
    equippedArtifacts: List<Artifact>,
    equippedOnOtherHeroes: Set<String> = emptySet(),
    choosingArtifactForSlot: ArtifactSlot?,
    onChooseSlot: (ArtifactSlot?) -> Unit,
    onEquip: (String) -> Unit,
    onUnequip: (String) -> Unit
) {
    if (choosingArtifactForSlot != null) {
        // Picker list for specific slot, excluding artifacts equipped on other heroes
        val slot = choosingArtifactForSlot
        val availableInSlot = allArtifacts.filter { it.slot == slot && it.id !in equippedOnOtherHeroes }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tr("Выберите ${slot.displayName}:", "Select ${slot.localizedName()}:"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = GameGoldLight
                )
                TextButton(onClick = { onChooseSlot(null) }) {
                    Text(tr("Назад", "Back"), color = GameGold)
                }
            }

            if (availableInSlot.isEmpty()) {
                Text(
                    text = tr(
                        "Нет доступных артефактов в этот слот. Открывайте ларцы или создавайте в Кузнице!",
                        "No artifacts available for this slot. Open chests or craft them in the Forge!"
                    ),
                    color = Color.Gray,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                availableInSlot.forEach { artifact ->
                    val isEquippedOnThisHero = hero.equippedArtifactIds.contains(artifact.id)
                    val minLevel = getArtifactRequiredHeroLevel(artifact.rarity)
                    val isLevelLocked = !canHeroEquipArtifact(hero.level, artifact.rarity)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable(enabled = !isLevelLocked) { onEquip(artifact.id) },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isLevelLocked) Color(0xFF53436B) else getRarityColor(artifact.rarity)),
                        colors = CardDefaults.cardColors(containerColor = if (isLevelLocked) Color(0xFF1B1428) else GameCardBg)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF140F22),
                                border = BorderStroke(1.dp, getRarityColor(artifact.rarity)),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = artifact.iconSymbol, fontSize = 18.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))

                            // Middle Column: Line 1 = Name, Line 2 = Stats, Description removed
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = cleanArtifactTitle(artifact.localizedName()),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLevelLocked) Color(0xFFC0B5D3) else Color.White,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (artifact.atkBonus != 0) {
                                        Text(
                                            text = (if (artifact.atkBonus > 0) "+${artifact.atkBonus}" else "${artifact.atkBonus}") + " ⚔️",
                                            color = if (artifact.atkBonus > 0) StatAtkBlue else Color(0xFFFF8A80),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (artifact.armorBonus != 0) {
                                        Text(
                                            text = (if (artifact.armorBonus > 0) "+${artifact.armorBonus}" else "${artifact.armorBonus}") + " 🛡️",
                                            color = if (artifact.armorBonus > 0) StatArmorSteel else Color(0xFFFF8A80),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (artifact.hpBonus != 0) {
                                        Text(
                                            text = (if (artifact.hpBonus > 0) "+${artifact.hpBonus}" else "${artifact.hpBonus}") + " ❤️",
                                            color = if (artifact.hpBonus > 0) StatHpRed else Color(0xFFFF8A80),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = { onEquip(artifact.id) },
                                enabled = !isLevelLocked,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isEquippedOnThisHero) Color(0xFF4CAF50) else if (isLevelLocked) Color(0xFF281D3B) else GameGold,
                                    contentColor = if (isLevelLocked) Color(0xFFFFD54F) else if (isEquippedOnThisHero) Color.White else Color.Black,
                                    disabledContainerColor = Color(0xFF281D3B),
                                    disabledContentColor = Color(0xFFFFD54F)
                                ),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isEquippedOnThisHero) {
                                        tr("Надет", "Equipped")
                                    } else if (isLevelLocked) {
                                        tr("🔒 $minLevel ур.", "🔒 Lv. $minLevel")
                                    } else {
                                        tr("Надеть", "Equip")
                                    },
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Display 3 artifact slots
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = tr("Снаряжение героини (3 слота):", "Heroine Gear (3 slots):"),
                fontSize = 12.sp,
                color = Color(0xFFDDD2F0),
                fontWeight = FontWeight.Bold
            )

            ArtifactSlot.values().forEach { slot ->
                val equipped = equippedArtifacts.firstOrNull { it.slot == slot }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChooseSlot(slot) },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        1.dp,
                        if (equipped != null) getRarityColor(equipped.rarity) else Color(0xFF4B3E68)
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (equipped != null) Color(0xFF281F3E) else Color(0xFF1E1730)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Slot Icon
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF120E1E),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = equipped?.iconSymbol ?: when (slot) {
                                        ArtifactSlot.WEAPON -> "⚔️"
                                        ArtifactSlot.ARMOR -> "🛡️"
                                        ArtifactSlot.RELIC -> "🔮"
                                    },
                                    fontSize = 18.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Slot middle Column: Line 1 = Name, Line 2 = Stats, Description removed
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            if (equipped != null) {
                                Text(
                                    text = cleanArtifactTitle(equipped.localizedName()),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (equipped.atkBonus != 0) {
                                        Text(
                                            text = (if (equipped.atkBonus > 0) "+${equipped.atkBonus}" else "${equipped.atkBonus}") + " ⚔️",
                                            color = if (equipped.atkBonus > 0) StatAtkBlue else Color(0xFFFF8A80),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (equipped.armorBonus != 0) {
                                        Text(
                                            text = (if (equipped.armorBonus > 0) "+${equipped.armorBonus}" else "${equipped.armorBonus}") + " 🛡️",
                                            color = if (equipped.armorBonus > 0) StatArmorSteel else Color(0xFFFF8A80),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (equipped.hpBonus != 0) {
                                        Text(
                                            text = (if (equipped.hpBonus > 0) "+${equipped.hpBonus}" else "${equipped.hpBonus}") + " ❤️",
                                            color = if (equipped.hpBonus > 0) StatHpRed else Color(0xFFFF8A80),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = slot.localizedName(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = tr("Слот пуст", "Empty Slot"),
                                    fontSize = 10.5.sp,
                                    color = Color(0xFFAAA0C0)
                                )
                            }
                        }

                        if (equipped != null) {
                            IconButton(onClick = { onUnequip(equipped.id) }) {
                                Icon(Icons.Default.Close, contentDescription = tr("Снять", "Unequip"), tint = Color.LightGray)
                            }
                        } else {
                            Text(text = tr("+ Надеть", "+ Equip"), color = GameGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun cleanArtifactTitle(title: String): String = title.trim()
