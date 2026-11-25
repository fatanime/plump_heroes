package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.i18n.localizedDescription
import com.example.i18n.localizedTitle
import com.example.i18n.tr
import com.example.model.DailyQuestState
import com.example.model.QuestRewardType
import com.example.ui.theme.*

@Composable
fun DailyQuestsDialog(
    quests: List<DailyQuestState>,
    onClaimReward: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val completedCount = quests.count { it.isCompleted }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            val widthScale = (maxWidth.value / 360f).coerceIn(0.80f, 1.16f)
            val cardMaxWidth = 560.dp
            val scrollState = rememberScrollState()

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = cardMaxWidth)
                    .heightIn(max = (maxHeight * 0.90f))
                    .testTag("daily_quests_dialog"),
                shape = RoundedCornerShape(18.dp),
                color = GameSurfaceDark,
                border = BorderStroke(1.5.dp, GameGold),
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = (12 * widthScale).dp, vertical = (12 * widthScale).dp)
                ) {
                    // Header with Title + Close "X" button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 6.dp)
                        ) {
                            Text(
                                text = tr("📜 Ежедневные квесты", "📜 Daily Quests"),
                                fontSize = (17f * widthScale).sp,
                                fontWeight = FontWeight.Black,
                                color = GameGoldLight,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = tr(
                                    "Выполнено: $completedCount/${quests.size} • От простого к сложному",
                                    "Completed: $completedCount/${quests.size} • Easy to hard"
                                ),
                                fontSize = (10.5f * widthScale).sp,
                                color = Color(0xFFAFA2C6),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF2B2040),
                            border = BorderStroke(1.dp, Color(0xFF5A4482))
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("close_daily_quests_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = tr("Закрыть квесты", "Close quests"),
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Vertical list of 5 daily quests
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quests.forEach { quest ->
                            DailyQuestRowCard(
                                quest = quest,
                                widthScale = widthScale,
                                onClaim = { onClaimReward(quest.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyQuestRowCard(
    quest: DailyQuestState,
    widthScale: Float,
    onClaim: () -> Unit
) {
    val isClaimable = quest.isCompleted && !quest.isRewardClaimed
    val isClaimed = quest.isRewardClaimed

    val borderColor = when {
        isClaimable -> GameGold
        isClaimed -> Color(0xFF4CAF50).copy(alpha = 0.65f)
        else -> Color(0xFF4C376A)
    }
    val borderWidth = if (isClaimable) 2.dp else 1.dp
    val cardBgColor = when {
        isClaimable -> Color(0xFF2F2344)
        isClaimed -> Color(0xFF1D162C)
        else -> GameCardBg
    }

    // Proportional font shrinkage state so title stays 1 line & description stays max 2 lines
    var textScale by remember(quest.id, widthScale) { mutableFloatStateOf(1f) }
    val rewardEmoji = if (quest.rewardType == QuestRewardType.GOLD) "💰" else "🧩"
    val rewardLabel = "+${quest.rewardAmount} $rewardEmoji"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isClaimable) {
                    Modifier.clickable(onClick = onClaim)
                } else Modifier
            )
            .testTag("daily_quest_card_${quest.id}"),
        shape = RoundedCornerShape(12.dp),
        color = cardBgColor,
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = (10 * widthScale).dp, vertical = (9 * widthScale).dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((8 * widthScale).dp)
        ) {
            // Left: Title (Line 1) + Description with Progress (Max 2 lines)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = quest.localizedTitle(),
                    fontSize = (13.2f * widthScale * textScale).sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = when {
                        isClaimable -> GameGoldLight
                        isClaimed -> Color(0xFFA5D6A7)
                        else -> Color.White
                    },
                    maxLines = 1,
                    softWrap = false,
                    onTextLayout = { layoutResult ->
                        if (layoutResult.didOverflowWidth && textScale > 0.66f) {
                            textScale = (textScale - 0.06f).coerceAtLeast(0.66f)
                        }
                    }
                )

                val descText = "${quest.localizedDescription()} (${quest.currentProgress}/${quest.targetCount})"
                Text(
                    text = descText,
                    fontSize = (10.8f * widthScale * textScale).sp,
                    lineHeight = (13.4f * widthScale * textScale).sp,
                    color = if (isClaimed) Color(0xFF9C91B4) else Color(0xFFD5CBE8),
                    maxLines = 2,
                    onTextLayout = { layoutResult ->
                        if ((layoutResult.didOverflowHeight || layoutResult.lineCount > 2) && textScale > 0.66f) {
                            textScale = (textScale - 0.06f).coerceAtLeast(0.66f)
                        }
                    }
                )
            }

            // Right: Reward button + Status Checkbox right next to it
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((6 * widthScale).dp)
            ) {
                val buttonContainer = when {
                    isClaimable -> GameGold
                    isClaimed -> Color(0xFF283529)
                    else -> Color(0xFF2B2040)
                }
                val buttonContent = when {
                    isClaimable -> Color.Black
                    isClaimed -> Color(0xFF81C784)
                    else -> Color(0xFFC5B8DE)
                }
                val buttonBorder = when {
                    isClaimable -> BorderStroke(1.dp, Color(0xFFFFF59D))
                    isClaimed -> BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.5f))
                    else -> BorderStroke(1.dp, Color(0xFF4C376A))
                }

                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = buttonContainer,
                    border = buttonBorder,
                    modifier = Modifier
                        .defaultMinSize(minWidth = (66 * widthScale).dp, minHeight = 40.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .clickable(enabled = isClaimable, onClick = onClaim)
                        .testTag("claim_quest_${quest.id}")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = (8 * widthScale).dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = rewardLabel,
                            fontSize = (11.5f * widthScale * textScale).sp,
                            fontWeight = FontWeight.Black,
                            color = buttonContent,
                            maxLines = 1,
                            softWrap = false,
                            onTextLayout = { layoutResult ->
                                if (layoutResult.didOverflowWidth && textScale > 0.66f) {
                                    textScale = (textScale - 0.06f).coerceAtLeast(0.66f)
                                }
                            }
                        )
                    }
                }

                // Status checkbox right next to the reward button
                val statusDescription = when {
                    isClaimed -> tr("Награда получена", "Reward claimed")
                    isClaimable -> tr("Выполнено, заберите награду", "Completed, claim reward")
                    else -> tr("Не выполнено", "Not completed")
                }
                val checkBg = when {
                    isClaimed -> Color(0xFF2E7D32)
                    isClaimable -> Color(0xFF1976D2)
                    else -> Color(0xFF1A1229)
                }
                val checkBorder = when {
                    isClaimed -> Color(0xFF81C784)
                    isClaimable -> GameGold
                    else -> Color(0xFF8F82A8)
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size((28 * widthScale).dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(checkBg)
                        .border(1.6.dp, checkBorder, RoundedCornerShape(7.dp))
                        .semantics { contentDescription = statusDescription }
                        .testTag("quest_checkbox_${quest.id}")
                ) {
                    if (isClaimed || isClaimable) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size((18 * widthScale).dp)
                        )
                    }
                }
            }
        }
    }
}
