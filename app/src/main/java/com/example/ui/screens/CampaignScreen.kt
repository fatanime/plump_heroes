package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.localizedName
import com.example.i18n.localizedSubtitle
import com.example.i18n.localizedTitle
import com.example.i18n.tr
import com.example.model.CampaignChapter
import com.example.model.CampaignStage
import com.example.model.calculateCareerStagePayout
import com.example.ui.theme.*

@Composable
fun CampaignScreen(
    chapters: List<CampaignChapter>,
    playerLevel: Int,
    selectedChapterId: Int = 1,
    onSelectChapter: (Int) -> Unit = {},
    onStartBattle: (CampaignStage) -> Unit,
    onBackToHub: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val currentChapter = chapters.firstOrNull { it.id == selectedChapterId } ?: chapters.firstOrNull()
    val selectedTabIndex = chapters.indexOfFirst { it.id == currentChapter?.id }.coerceAtLeast(0)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GameBgDark)
            .padding(horizontal = 8.dp)
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        if (onBackToHub != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onBackToHub() }
                    .padding(vertical = 4.dp, horizontal = 4.dp)
                    .testTag("campaign_back_to_hub")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = tr("В Хаб", "To Hub"),
                    tint = GameGoldLight,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = tr("В Хаб", "To Hub"),
                    color = GameGoldLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Campaign Header
        Text(
            text = tr("Карьера: Битвы Героинь", "Career: Heroines' Battles"),
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = GameGoldLight
        )
        Text(
            text = tr(
                "Миссии открываются с получением опыта игрока. Только здесь добываются редкие артефакты!",
                "Missions unlock as you gain player EXP. Rare artifacts can only be earned here!"
            ),
            fontSize = 10.5.sp,
            color = Color(0xFFAFA2C6)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Chapter Selection Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = GameSurfaceDark,
            contentColor = GameGold,
            edgePadding = 0.dp
        ) {
            chapters.forEach { chapter ->
                Tab(
                    selected = chapter.id == (currentChapter?.id ?: 1),
                    onClick = { onSelectChapter(chapter.id) },
                    text = {
                        Text(
                            text = tr("Глава ${chapter.id}", "Chapter ${chapter.id}"),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        currentChapter?.let { chapter ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF201833)),
                border = BorderStroke(1.dp, Color(0xFF453664))
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Text(
                        text = chapter.localizedName(),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = chapter.localizedSubtitle(),
                        fontSize = 10.5.sp,
                        color = Color(0xFFC7B9E3),
                        maxLines = 1,
                        softWrap = false,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stages List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chapter.stages, key = { it.id }) { stage ->
                    CampaignStageItem(
                        stage = stage,
                        playerLevel = playerLevel,
                        onFight = { onStartBattle(stage) }
                    )
                }
            }
        }
    }
}

@Composable
fun CampaignStageItem(
    stage: CampaignStage,
    playerLevel: Int,
    onFight: () -> Unit
) {
    val meetsLevel = playerLevel >= stage.requiredPlayerLevel
    val isEffectivelyUnlocked = (stage.isUnlocked && meetsLevel) || stage.stars > 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("campaign_stage_${stage.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEffectivelyUnlocked) GameCardBg else Color(0xFF1B1626)
        ),
        border = BorderStroke(
            if (stage.isBoss) 2.dp else 1.dp,
            if (stage.isBoss) GameGold else if (isEffectivelyUnlocked) Color(0xFF4C3D6E) else Color(0xFF2A223B)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stage Number / Boss Badge
            Surface(
                shape = CircleShape,
                color = if (stage.isBoss) GameGold else if (isEffectivelyUnlocked) Color(0xFF3B2D5C) else Color(0xFF201A2C),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (!isEffectivelyUnlocked) {
                        Icon(Icons.Default.Lock, contentDescription = tr("Закрыто", "Locked"), tint = Color.Gray, modifier = Modifier.size(18.dp))
                    } else if (stage.isBoss) {
                        Text(text = "👑", fontSize = 20.sp)
                    } else {
                        Text(
                            text = "${stage.stageNumber}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                         )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Stage Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stage.localizedTitle(),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isEffectivelyUnlocked) Color.White else Color.Gray,
                        maxLines = 1,
                        softWrap = false,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                if (!meetsLevel) {
                    Text(
                        text = tr(
                            "🔒 Открывается на ${stage.requiredPlayerLevel} ур. опыта\n(ваш: $playerLevel)",
                            "🔒 Unlocks at EXP Lv. ${stage.requiredPlayerLevel}\n(yours: $playerLevel)"
                        ),
                        fontSize = 10.sp,
                        color = Color(0xFFFFB74D),
                        fontWeight = FontWeight.Bold
                    )
                } else if (isEffectivelyUnlocked) {
                    Text(
                        text = tr(
                            "Сила атаки отряда: ${stage.totalEnemyAtk} ⚔️",
                            "Squad Attack Power: ${stage.totalEnemyAtk} ⚔️"
                        ),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFB74D)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    // Stars earned (up to 4 stars)
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(4) { starIndex ->
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (starIndex < stage.stars) GameGold else Color(0xFF4C3D66),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                val isRepeat = stage.stars > 0
                val payout = calculateCareerStagePayout(stage)
                val displayGold = payout.gold
                val awardsShards = stage.id in listOf(101, 104, 201, 204, 301, 304)

                // Line 1: Gold & Gems currencies
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "+${displayGold}💰",
                        fontSize = 9.sp,
                        color = GameGoldLight,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(text = "+${payout.gems}💎", fontSize = 9.sp, color = Color(0xFF4FC3F7), maxLines = 1, softWrap = false)
                }

                // Line 2: Artifact & Shards badges on a separate line below so they fit without clipping
                if (awardsShards || (!isRepeat && stage.rewardArtifactId != null) || (stage.rewardShardsHeroId != null && !awardsShards)) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (awardsShards) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFAB47BC).copy(alpha = 0.25f),
                                border = BorderStroke(0.8.dp, Color(0xFFCE93D8))
                            ) {
                                Text(
                                    text = tr("+${payout.shards}🧩 Осколки", "+${payout.shards}🧩 Shards"),
                                    fontSize = 8.sp,
                                    color = Color(0xFFE1BEE7),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        if (!isRepeat && stage.rewardArtifactId != null) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = GameGold.copy(alpha = 0.2f),
                                border = BorderStroke(0.8.dp, GameGold)
                            ) {
                                Text(
                                    text = tr("🏆 Артефакт", "🏆 Artifact"),
                                    fontSize = 8.sp,
                                    color = GameGold,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        } else if (stage.rewardShardsHeroId != null && !awardsShards) {
                            Text(text = tr("Осколки 🧩", "Shards 🧩"), fontSize = 9.sp, color = Color(0xFFBA68C8), maxLines = 1, softWrap = false)
                        }
                    }
                }
            }

            // Start Fight Button
            Button(
                onClick = onFight,
                enabled = isEffectivelyUnlocked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (stage.isBoss) GameGold else Color(0xFF7E57C2),
                    contentColor = if (stage.isBoss) Color.Black else Color.White,
                    disabledContainerColor = Color(0xFF272033),
                    disabledContentColor = Color.DarkGray
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier
                    .testTag("fight_stage_${stage.id}")
                    .height(36.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text(text = tr("В БОЙ", "FIGHT"), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }
        }
    }
}
