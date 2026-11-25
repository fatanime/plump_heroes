package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.localizedEffectDescription
import com.example.i18n.localizedName
import com.example.model.Artifact
import com.example.model.ArtifactSlot
import com.example.ui.theme.*

private fun cleanArtifactTitle(name: String): String {
    return name
        .replace("Базовая", "")
        .replace("Базовый", "")
        .replace("Базовое", "")
        .replace("Особая", "")
        .replace("Особый", "")
        .replace("Особое", "")
        .replace("Редкая", "")
        .replace("Редкий", "")
        .replace("Редкое", "")
        .replace("Мифическая", "")
        .replace("Мифический", "")
        .replace("Мифическое", "")
        .replace(Regex("\\s+"), " ")
        .trim()
}

fun cleanArtifactDescription(description: String): String {
    val trimmed = description.trim()
    val dotIndex = trimmed.indexOf(". ")
    if (dotIndex != -1) {
        val prefix = trimmed.substring(0, dotIndex)
        if (prefix.contains("к ") || prefix.contains("HP") || prefix.contains("атак") || prefix.contains("брон") || prefix.contains("здоров")) {
            return trimmed.substring(dotIndex + 2).trim()
        }
    }
    return trimmed
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArtifactItemView(
    artifact: Artifact,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val rarityColor = getRarityColor(artifact.rarity)

    Card(
        modifier = modifier
            .testTag("artifact_${artifact.id}")
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = if (isSelected) 2.5.dp else 1.dp,
            color = if (isSelected) GameGold else rarityColor
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF32284D) else GameCardBg
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon in rarity frame
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF140F22),
                border = BorderStroke(1.5.dp, rarityColor),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = artifact.iconSymbol, fontSize = 20.sp)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Name column: strictly word-wrapped and taking up to 40% width
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.38f)
                    .padding(end = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = cleanArtifactTitle(artifact.localizedName()),
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    softWrap = true,
                    lineHeight = 14.sp
                )
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = rarityColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = artifact.rarity.localizedName(),
                        fontSize = 8.5.sp,
                        color = rarityColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            // Stats & Description taking remaining space (Line 2: Stats, Line 3: Cleaned Lore Description)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Line 2: Stats badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (artifact.atkBonus != 0) {
                        Text(
                            text = (if (artifact.atkBonus > 0) "+${artifact.atkBonus}" else "${artifact.atkBonus}") + " ⚔️",
                            color = if (artifact.atkBonus > 0) StatAtkBlue else Color(0xFFFF8A80),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (artifact.armorBonus != 0) {
                        Text(
                            text = (if (artifact.armorBonus > 0) "+${artifact.armorBonus}" else "${artifact.armorBonus}") + " 🛡️",
                            color = if (artifact.armorBonus > 0) StatArmorSteel else Color(0xFFFF8A80),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (artifact.hpBonus != 0) {
                        Text(
                            text = (if (artifact.hpBonus > 0) "+${artifact.hpBonus}" else "${artifact.hpBonus}") + " ❤️",
                            color = if (artifact.hpBonus > 0) StatHpRed else Color(0xFFFF8A80),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Line 3: Cleaned short description (max 2 lines, without duplicate stats)
                val cleanedDesc = cleanArtifactDescription(artifact.localizedEffectDescription())
                if (cleanedDesc.isNotBlank()) {
                    Text(
                        text = cleanedDesc,
                        fontSize = 9.sp,
                        color = Color(0xFFC0B3DC),
                        lineHeight = 11.5.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Slot badge
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0x33FFFFFF),
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Text(
                    text = artifact.slot.localizedName(),
                    fontSize = 8.5.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }
    }
}
