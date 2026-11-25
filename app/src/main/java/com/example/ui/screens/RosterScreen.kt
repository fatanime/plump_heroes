package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.localizedName
import com.example.i18n.tr
import com.example.model.Artifact
import com.example.model.CardRarity
import com.example.model.Element
import com.example.model.HERO_FILTER_ELEMENTS
import com.example.model.HeroCard
import com.example.model.HeroType
import com.example.model.secretUnlockShardsTarget
import com.example.ui.components.HeroCardItem
import com.example.ui.components.getCardFrameColor
import com.example.ui.theme.*

internal fun availableElementsForHeroType(
    heroes: List<HeroCard>,
    typeFilter: HeroType?
): Set<Element> {
    return heroes
        .filter { typeFilter == null || it.heroType == typeFilter }
        .map { it.element }
        .toSet()
}

internal fun filterRosterHeroes(
    heroes: List<HeroCard>,
    typeFilter: HeroType?,
    elementFilter: Element?,
    rarityFilter: CardRarity? = null
): List<HeroCard> {
    return heroes.filter { hero ->
        (typeFilter == null || hero.heroType == typeFilter) &&
            (elementFilter == null || hero.element == elementFilter) &&
            (rarityFilter == null || hero.rarity == rarityFilter)
    }
}

@Composable
fun RosterScreen(
    heroes: List<HeroCard>,
    activeDeckHeroIds: List<String?>,
    artifacts: List<Artifact> = emptyList(),
    onSelectHeroForDetails: (HeroCard) -> Unit,
    onAssignHeroToSlot: (Int, String?) -> Unit,
    onShowLockedMessage: (String) -> Unit = {},
    onOpenSecretHero: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTypeFilter by remember { mutableStateOf<HeroType?>(null) }
    var selectedElementFilter by remember { mutableStateOf<Element?>(null) }
    var selectedRarityFilter by remember { mutableStateOf<CardRarity?>(null) }
    var assigningSlotIndex by remember { mutableStateOf<Int?>(null) }

    val availableElements = remember(heroes, selectedTypeFilter) {
        availableElementsForHeroType(heroes, selectedTypeFilter)
    }
    val filteredHeroes = remember(heroes, selectedTypeFilter, selectedElementFilter, selectedRarityFilter) {
        filterRosterHeroes(heroes, selectedTypeFilter, selectedElementFilter, selectedRarityFilter)
    }

    fun selectTypeFilter(nextType: HeroType?) {
        selectedTypeFilter = nextType
        if (selectedElementFilter != null && selectedElementFilter !in availableElementsForHeroType(heroes, nextType)) {
            selectedElementFilter = null
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(start = 12.dp, top = 6.dp, end = 12.dp, bottom = 90.dp),
        modifier = modifier
            .fillMaxSize()
            .background(GameBgDark)
    ) {
        // Active Deck Header
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tr("Боевая колода (2x2)", "Battle Deck (2x2)"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = GameGoldLight,
                    maxLines = 1,
                    modifier = Modifier.weight(1f).padding(end = 6.dp)
                )

                if (assigningSlotIndex != null) {
                    Surface(
                        color = Color(0xFFD32F2F),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable { assigningSlotIndex = null }
                    ) {
                        Text(
                            text = tr("Отмена выбора ✕", "Cancel Swap ✕"),
                            fontSize = 10.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else {
                    Text(
                        text = tr("Зажмите карту для смены", "Long-press card to swap"),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GameGoldLight,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // 2x2 Battle Deck Cards (Vertical Format)
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = GameSurfaceDark),
                border = BorderStroke(1.dp, Color(0xFF3F335A))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Front Row label & 2 slots
                    Text(
                        text = tr("⚔️ Первый ряд (Ближний бой / Танки)", "⚔️ Front Row (Melee / Tanks)"),
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
                            hero = activeDeckHeroIds.getOrNull(0)?.let { id -> heroes.firstOrNull { it.id == id } },
                            artifacts = artifacts,
                            isWaitingAssignment = assigningSlotIndex == 0,
                            onClickSlot = {
                                assigningSlotIndex = if (assigningSlotIndex == 0) null else 0
                            },
                            onLongClickSlot = {
                                assigningSlotIndex = 0
                            },
                            onInspect = onSelectHeroForDetails,
                            modifier = Modifier.weight(1f)
                        )

                        DeckSlotItem(
                            slotIndex = 1,
                            label = tr("Передний Правый", "Front Right"),
                            hero = activeDeckHeroIds.getOrNull(1)?.let { id -> heroes.firstOrNull { it.id == id } },
                            artifacts = artifacts,
                            isWaitingAssignment = assigningSlotIndex == 1,
                            onClickSlot = {
                                assigningSlotIndex = if (assigningSlotIndex == 1) null else 1
                            },
                            onLongClickSlot = {
                                assigningSlotIndex = 1
                            },
                            onInspect = onSelectHeroForDetails,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Back Row label & 2 slots
                    Text(
                        text = tr("🏹 Задний ряд (Дальний бой / Магия)", "🏹 Back Row (Ranged / Magic)"),
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
                            hero = activeDeckHeroIds.getOrNull(2)?.let { id -> heroes.firstOrNull { it.id == id } },
                            artifacts = artifacts,
                            isWaitingAssignment = assigningSlotIndex == 2,
                            onClickSlot = {
                                assigningSlotIndex = if (assigningSlotIndex == 2) null else 2
                            },
                            onLongClickSlot = {
                                assigningSlotIndex = 2
                            },
                            onInspect = onSelectHeroForDetails,
                            modifier = Modifier.weight(1f)
                        )

                        DeckSlotItem(
                            slotIndex = 3,
                            label = tr("Задний Правый", "Back Right"),
                            hero = activeDeckHeroIds.getOrNull(3)?.let { id -> heroes.firstOrNull { it.id == id } },
                            artifacts = artifacts,
                            isWaitingAssignment = assigningSlotIndex == 3,
                            onClickSlot = {
                                assigningSlotIndex = if (assigningSlotIndex == 3) null else 3
                            },
                            onLongClickSlot = {
                                assigningSlotIndex = 3
                            },
                            onInspect = onSelectHeroForDetails,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Squad Stats Summary (Single adaptive line)
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
            val deckHeroes = activeDeckHeroIds.mapNotNull { id -> heroes.firstOrNull { it.id == id } }
            val totalDeckAtk = deckHeroes.sumOf { it.calculateTotalAtk(artifacts) }
            val totalDeckHp = deckHeroes.sumOf { it.calculateTotalHp(artifacts) }
            val totalDeckArmor = deckHeroes.sumOf { it.calculateTotalArmor(artifacts) }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1E1730),
                border = BorderStroke(1.dp, Color(0xFF453664))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "⚔️", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = tr("Атака: $totalDeckAtk", "ATK: $totalDeckAtk"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatAtkBlue,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "❤️", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = tr("Здоровье: $totalDeckHp", "HP: $totalDeckHp"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF81C784),
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🛡️", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = tr("Броня: $totalDeckArmor", "Armor: $totalDeckArmor"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatArmorSteel,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // Role Filter Buttons + 4 Element Filter Buttons
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterTabButton(
                        label = tr("Все (${heroes.size})", "All (${heroes.size})"),
                        isSelected = selectedTypeFilter == null,
                        onClick = { selectTypeFilter(null) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("roster_type_filter_all")
                    )
                    FilterTabButton(
                        label = tr("⚔️ Ближ.", "⚔️ Melee"),
                        isSelected = selectedTypeFilter == HeroType.MELEE,
                        onClick = {
                            val next = if (selectedTypeFilter == HeroType.MELEE) null else HeroType.MELEE
                            selectTypeFilter(next)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("roster_type_filter_melee")
                    )
                    FilterTabButton(
                        label = tr("🏹 Дальн.", "🏹 Ranged"),
                        isSelected = selectedTypeFilter == HeroType.RANGED,
                        onClick = {
                            val next = if (selectedTypeFilter == HeroType.RANGED) null else HeroType.RANGED
                            selectTypeFilter(next)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("roster_type_filter_ranged")
                    )
                    FilterTabButton(
                        label = tr("☯ Смеш.", "☯ Hybrid"),
                        isSelected = selectedTypeFilter == HeroType.HYBRID,
                        onClick = {
                            val next = if (selectedTypeFilter == HeroType.HYBRID) null else HeroType.HYBRID
                            selectTypeFilter(next)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("roster_type_filter_hybrid")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HERO_FILTER_ELEMENTS.forEach { element ->
                        val isElementEnabled = element in availableElements
                        val isElementSelected = selectedElementFilter == element && isElementEnabled
                        FilterTabButton(
                            label = "${element.symbol} ${element.localizedName()}",
                            isSelected = isElementSelected,
                            enabled = isElementEnabled,
                            onClick = {
                                selectedElementFilter = if (selectedElementFilter == element) null else element
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("roster_element_filter_${element.name.lowercase()}")
                        )
                    }
                }
            }
        }

        // Hero Collection Title
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
            Text(
                text = tr(
                    "Коллекция пышных героинь (${filteredHeroes.size}):",
                    "Plump Heroines Collection (${filteredHeroes.size}):"
                ),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )
        }

        // Hero Collection Grid Items
        items(filteredHeroes, key = { it.id }) { hero ->
            val isInDeck = activeDeckHeroIds.contains(hero.id)

            HeroCardItem(
                hero = hero,
                allArtifacts = artifacts,
                isSelectedInDeck = isInDeck,
                onOpenSecretHero = { onOpenSecretHero(hero.id) },
                onClick = {
                    if (!hero.isUnlocked) {
                        if (hero.isSecret) {
                            val unlockTarget = hero.secretUnlockShardsTarget()
                            if (hero.shards >= unlockTarget) {
                                onOpenSecretHero(hero.id)
                            } else {
                                val left = (unlockTarget - hero.shards).coerceAtLeast(0)
                                onShowLockedMessage(formatSecretHeroLockedMessage(left))
                            }
                        } else {
                            onShowLockedMessage(
                                tr(
                                    "Эта карта пока скрыта! Проходите уровни Карьеры, чтобы открыть её.",
                                    "This card is still locked! Clear Career stages to unlock her."
                                )
                            )
                        }
                    } else if (assigningSlotIndex != null) {
                        val slot = assigningSlotIndex!!
                        if (slot in 2..3 && hero.heroType == HeroType.MELEE) {
                            onShowLockedMessage(
                                tr(
                                    "Герои ближнего боя могут размещаться ТОЛЬКО в 1-м ряду (слоты 1 и 2)!",
                                    "Melee heroines can ONLY be placed in the Front Row (slots 1 and 2)!"
                                )
                            )
                        } else {
                            onAssignHeroToSlot(slot, hero.id)
                            assigningSlotIndex = null
                        }
                    } else {
                        onSelectHeroForDetails(hero)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DeckSlotItem(
    slotIndex: Int,
    label: String,
    hero: HeroCard?,
    artifacts: List<Artifact> = emptyList(),
    isWaitingAssignment: Boolean,
    onClickSlot: () -> Unit,
    onLongClickSlot: () -> Unit,
    onInspect: (HeroCard) -> Unit,
    modifier: Modifier = Modifier
) {
    val frameColor = hero?.let { getCardFrameColor(it.level) } ?: Color(0xFF4A3E66)

    Card(
        modifier = modifier
            .testTag("deck_slot_$slotIndex")
            .height(190.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = {
                    if (isWaitingAssignment) {
                        onClickSlot()
                    } else if (hero != null) {
                        onInspect(hero)
                    } else {
                        onClickSlot()
                    }
                },
                onLongClick = {
                    if (hero != null) {
                        onLongClickSlot()
                    } else {
                        onClickSlot()
                    }
                }
            ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (isWaitingAssignment) 2.5.dp else 1.5.dp,
            color = if (isWaitingAssignment) GameGold else frameColor
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isWaitingAssignment) Color(0xFF352650) else Color(0xFF1B1428)
        )
    ) {
        if (hero != null) {
            val totalAtk = hero.calculateTotalAtk(artifacts)
            val totalArmor = hero.calculateTotalArmor(artifacts)
            val totalHp = hero.calculateTotalHp(artifacts)

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Portrait Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(topStart = 11.dp, topEnd = 11.dp))
                ) {
                    val heroDisplayName = hero.localizedName()
                    com.example.ui.components.HeroPortraitImage(
                        imageRes = hero.imageRes,
                        heroId = hero.id,
                        heroName = heroDisplayName,
                        heroLevel = hero.level,
                        contentDescription = heroDisplayName,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Hero Type Icon in top-left & Element Icon in top-right
                    Text(
                        text = when (hero.heroType) {
                            HeroType.MELEE -> "⚔️"
                            HeroType.RANGED -> "🏹"
                            HeroType.HYBRID -> "☯"
                        },
                        fontSize = 11.sp,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(horizontal = 4.dp, vertical = 3.dp)
                    )

                    Text(
                        text = hero.element.symbol,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(horizontal = 4.dp, vertical = 3.dp)
                            .testTag("deck_slot_element_$slotIndex")
                    )

                    // Level Tag at bottom left
                    Surface(
                        shape = RoundedCornerShape(topEnd = 6.dp),
                        color = Color(0xEE000000),
                        modifier = Modifier.align(Alignment.BottomStart)
                    ) {
                        Text(
                            text = tr("Ур.${hero.level}", "Lv.${hero.level}"),
                            color = frameColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }

                    // Assignment mode Indicator Tag (Row badges removed)
                    if (isWaitingAssignment) {
                        Surface(
                            shape = RoundedCornerShape(topStart = 6.dp),
                            color = GameGold,
                            modifier = Modifier.align(Alignment.BottomEnd)
                        ) {
                            Text(
                                text = tr("Смена ✕", "Swap ✕"),
                                color = Color.Black,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    if (isWaitingAssignment) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x66FFD54F)),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xEE1E1333),
                                border = BorderStroke(1.dp, GameGold)
                            ) {
                                Text(
                                    text = tr("Выберите замену", "Choose replacement"),
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GameGoldLight,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Name Ribbon (Styled beautifully without wrapping!)
                com.example.ui.components.HeroNameRibbon(
                    name = hero.localizedName(),
                    frameColor = frameColor
                )

                // Stats Chips Bar (ATK, ARMOR, HP)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .background(Color(0xFF130E20), RoundedCornerShape(6.dp))
                        .padding(horizontal = 4.dp, vertical = 2.5.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "⚔️", fontSize = 8.sp)
                        Text(
                            text = "$totalAtk",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = StatAtkBlue,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🛡️", fontSize = 8.sp)
                        Text(
                            text = "$totalArmor",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = StatArmorSteel,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "❤️", fontSize = 8.sp)
                        Text(
                            text = "$totalHp",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = StatHpRed,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        } else {
            // Empty Slot
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = tr("Выбрать карту", "Select card"),
                        tint = if (isWaitingAssignment) GameGold else Color(0xFFB39DDB),
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isWaitingAssignment) {
                            tr("Выберите карту", "Select card")
                        } else {
                            tr("Слот ${slotIndex + 1}", "Slot ${slotIndex + 1}")
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isWaitingAssignment) GameGold else Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (slotIndex in 0..1) {
                            tr("1-й ряд (Ближ/Смеш)", "Front (Melee/Hybrid)")
                        } else {
                            tr("2-й ряд (Дальн/Смеш)", "Back (Ranged/Hybrid)")
                        },
                        fontSize = 8.5.sp,
                        color = Color(0xFFAFA2C6),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
fun FilterTabButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) GameGold else Color(0xFF261E3A),
            contentColor = if (isSelected) Color.Black else Color.White,
            disabledContainerColor = Color(0xFF171224),
            disabledContentColor = Color(0xFF5A4E75)
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
        modifier = modifier.height(34.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
    }
}

internal fun formatSecretHeroLockedMessage(remainingShards: Int): String {
    return tr(
        "Секретный персонаж! Соберите ещё $remainingShards 🧩 частиц из сундука редкостей.",
        "Secret character! Collect $remainingShards more 🧩 shards from the Rarity Chest."
    )
}

