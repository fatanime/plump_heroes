package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.localizedName
import com.example.i18n.tr
import com.example.model.Artifact
import com.example.model.ArtifactSlot
import com.example.model.CardRarity
import com.example.ui.components.ArtifactItemView
import com.example.ui.components.getRarityColor
import com.example.ui.theme.*

@Composable
fun ForgeScreen(
    artifacts: List<Artifact>,
    selectedArtifactIds: List<String>,
    equippedArtifactIds: Set<String> = emptySet(),
    playerGold: Int = 0,
    onToggleSelectArtifact: (String) -> Unit,
    onCombineArtifacts: () -> Unit,
    onSellArtifact: ((String, Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var slotFilter by remember { mutableStateOf<ArtifactSlot?>(null) }
    var artifactToSell by remember { mutableStateOf<Artifact?>(null) }

    // Only unequipped artifacts can be used in forge
    val availableArtifacts = artifacts.filter { it.id !in equippedArtifactIds }
    val filteredArtifacts = availableArtifacts.filter { slotFilter == null || it.slot == slotFilter }
    val selectedArtifacts = selectedArtifactIds.mapNotNull { id -> artifacts.firstOrNull { it.id == id } }

    val canCombine = selectedArtifacts.size == 3 &&
            selectedArtifacts.map { it.rarity }.distinct().size == 1

    val currentRarity = selectedArtifacts.firstOrNull()?.rarity
    val combineCost = when (currentRarity) {
        CardRarity.BASIC -> 100
        CardRarity.SPECIAL -> 200
        CardRarity.RARE -> 400
        else -> 0
    }
    val hasEnoughGold = playerGold >= combineCost
    val canCombineWithGold = canCombine && hasEnoughGold
    val targetRarity = when (currentRarity) {
        CardRarity.BASIC -> CardRarity.SPECIAL
        CardRarity.SPECIAL -> CardRarity.RARE
        CardRarity.RARE -> CardRarity.MYTHIC
        CardRarity.MYTHIC -> CardRarity.MYTHIC
        null -> null
    }

    val majoritySlot = selectedArtifacts
        .groupingBy { it.slot }
        .eachCount()
        .entries
        .firstOrNull { it.value >= 2 }
        ?.key

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GameBgDark)
            .padding(horizontal = 12.dp)
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = tr("Кузница Артефактов", "Artifact Forge"),
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            color = GameGoldLight
        )
        Text(
            text = tr(
                "Синтезируйте 3 артефакта одной редкости! Если 2 из 3 одного типа — результат сохранит этот тип.",
                "Synthesize 3 artifacts of the same rarity! If 2 of 3 share a type, the result keeps that type."
            ),
            fontSize = 11.sp,
            color = Color(0xFFAFA2C6)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // FORGE ANVIL CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = GameSurfaceDark),
            border = BorderStroke(1.5.dp, if (canCombine) GameGold else Color(0xFF4C3E6B))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (targetRarity != null) {
                        val slotSuffix = majoritySlot?.let { " • ${it.localizedName()}" } ?: ""
                        tr(
                            "Результат: 1x Артефакт [${targetRarity.localizedName()}$slotSuffix]",
                            "Result: 1x Artifact [${targetRarity.localizedName()}$slotSuffix]"
                        )
                    } else {
                        tr("Выберите 3 артефакта одной редкости", "Select 3 artifacts of the same rarity")
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (targetRarity != null) getRarityColor(targetRarity) else Color.White
                )

                // 3 Synthesis Slots
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    repeat(3) { slotIdx ->
                        val art = selectedArtifacts.getOrNull(slotIdx)
                        ForgeSlot(
                            artifact = art,
                            onRemove = { art?.let { onToggleSelectArtifact(it.id) } }
                        )
                    }
                }

                // Combine Button
                Button(
                    onClick = onCombineArtifacts,
                    enabled = canCombineWithGold,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GameGold,
                        contentColor = Color.Black,
                        disabledContainerColor = Color(0xFF2C2241),
                        disabledContentColor = Color(0xFF675B83)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("combine_artifacts_button")
                ) {
                    Text("✨", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when {
                            !canCombine -> tr(
                                "ВЫБЕРИТЕ 3 АРТЕФАКТА (${selectedArtifacts.size}/3)",
                                "SELECT 3 ARTIFACTS (${selectedArtifacts.size}/3)"
                            )
                            !hasEnoughGold -> tr(
                                "НЕДОСТАТОЧНО МОНЕТ ($combineCost 🪙)",
                                "NOT ENOUGH COINS ($combineCost 🪙)"
                            )
                            else -> tr(
                                "ВЫКОВАТЬ АРТЕФАКТ ($combineCost 🪙)",
                                "FORGE ARTIFACT ($combineCost 🪙)"
                            )
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = tr(
                "Продай лишние артефакты долгим нажатием на лишний артефакт.",
                "Long-press a spare artifact to sell it."
            ),
            fontSize = 11.sp,
            color = Color(0xFFFFD54F),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Filter Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterTabButton(label = tr("Все", "All"), isSelected = slotFilter == null, onClick = { slotFilter = null }, modifier = Modifier.weight(1f))
            FilterTabButton(label = tr("Оружие", "Weapons"), isSelected = slotFilter == ArtifactSlot.WEAPON, onClick = { slotFilter = ArtifactSlot.WEAPON }, modifier = Modifier.weight(1f))
            FilterTabButton(label = tr("Доспехи", "Armor"), isSelected = slotFilter == ArtifactSlot.ARMOR, onClick = { slotFilter = ArtifactSlot.ARMOR }, modifier = Modifier.weight(1f))
            FilterTabButton(label = tr("Реликвии", "Relics"), isSelected = slotFilter == ArtifactSlot.RELIC, onClick = { slotFilter = ArtifactSlot.RELIC }, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = tr("Ваш инвентарь (${filteredArtifacts.size}):", "Your inventory (${filteredArtifacts.size}):"),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Inventory list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(filteredArtifacts, key = { it.id }) { artifact ->
                val isSelected = selectedArtifactIds.contains(artifact.id)
                ArtifactItemView(
                    artifact = artifact,
                    isSelected = isSelected,
                    onClick = { onToggleSelectArtifact(artifact.id) },
                    onLongClick = { artifactToSell = artifact }
                )
            }
        }

        if (artifactToSell != null) {
            val art = artifactToSell!!
            val seed = kotlin.math.abs(art.id.hashCode())
            val price = when (art.rarity) {
                CardRarity.BASIC -> 150 + (seed % 101)
                CardRarity.SPECIAL -> 260 + (seed % 141)
                CardRarity.RARE -> 500 + (seed % 251)
                CardRarity.MYTHIC -> 800 + (seed % 201)
            }
            AlertDialog(
                onDismissRequest = { artifactToSell = null },
                title = {
                    Text(
                        text = tr("Продажа артефакта", "Sell Artifact"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = tr(
                                "Вы хотите продать [${art.localizedName()}]?",
                                "Do you want to sell [${art.localizedName()}]?"
                            ),
                            fontSize = 13.5.sp,
                            color = Color(0xFFDDD2F0)
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1B142D),
                            border = BorderStroke(1.dp, GameGold),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = tr("Цена продажи:", "Sell price:"), fontSize = 12.sp, color = Color(0xFFAFA2C6))
                                Text(text = "+$price 🪙", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GameGoldLight)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onSellArtifact?.invoke(art.id, price)
                            artifactToSell = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GameGold,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(tr("Продать", "Sell"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { artifactToSell = null },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFF5A497A)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(tr("Отмена", "Cancel"), fontSize = 13.sp)
                    }
                },
                containerColor = GameSurfaceDark,
                shape = RoundedCornerShape(14.dp)
            )
        }
    }
}

@Composable
fun ForgeSlot(
    artifact: Artifact?,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(64.dp)
            .clickable(enabled = artifact != null, onClick = onRemove),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B142D)),
        border = BorderStroke(
            1.5.dp,
            if (artifact != null) getRarityColor(artifact.rarity) else Color(0xFF453664)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (artifact != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = artifact.iconSymbol, fontSize = 20.sp)
                    Text(
                        text = artifact.rarity.localizedName().take(4),
                        fontSize = 8.sp,
                        color = getRarityColor(artifact.rarity),
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = tr("Пустой слот", "Empty slot"),
                    tint = Color(0xFF5A497A),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
