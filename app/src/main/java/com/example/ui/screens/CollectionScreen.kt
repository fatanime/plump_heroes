package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.i18n.tr
import com.example.model.*
import com.example.ui.theme.*

@Composable
fun CollectionScreen(
    profile: PlayerProfile,
    heroes: List<HeroCard>,
    chapters: List<CampaignChapter>,
    isCampaign100Percent: Boolean,
    onOpenBonusGallery: () -> Unit,
    onOpenWorldLore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allStages = remember(chapters) { chapters.flatMap { it.stages } }
    val totalStages = allStages.size.coerceAtLeast(24)
    val clearedStagesCount = remember(allStages) { allStages.count { it.stars > 0 } }
    val totalStars = remember(allStages) { allStages.sumOf { it.stars } }
    val fourStarStagesCount = remember(allStages) { allStages.count { it.stars >= 4 } }
    val maxStars = totalStages * 4
    val unlockedHeroesCount = remember(heroes) { heroes.count { it.isUnlocked } }
    val maxLevelHeroesCount = remember(heroes) { heroes.count { it.isUnlocked && it.level >= it.maxUpgradeLevel() } }
    val towerClearedToday = remember(profile) { computeTowerClearedFloorToday(profile) }
    val unlockedHeroGalleryKeys = remember(heroes) {
        heroes.filter { it.isUnlocked }.flatMap { hero ->
            getUnlockedHeroGalleryItems(hero.id, hero.level, hero.imageRes).map { it.first }
        }
    }
    val finaleKeys = remember(isCampaign100Percent) {
        if (isCampaign100Percent) {
            listOf("bonus_final_1", "bonus_final_2", "bonus_final_3", "bonus_final_4", "bonus_final_6")
        } else {
            emptyList()
        }
    }
    val allUnlockedGalleryKeys = remember(unlockedHeroGalleryKeys, finaleKeys) {
        unlockedHeroGalleryKeys + finaleKeys
    }
    val unseenGalleryCount = remember(allUnlockedGalleryKeys, profile.viewedGalleryImageKeys) {
        allUnlockedGalleryKeys.count { it !in profile.viewedGalleryImageKeys }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GameBgDark)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = tr("Трофеи", "Trophies"),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = GameGoldLight
            )
            Text(
                text = tr(
                    "Бонусная галерея, древние амулеты, история мира и достижения",
                    "Bonus gallery, ancient amulets, world lore & achievements"
                ),
                fontSize = 11.5.sp,
                color = Color(0xFFB5A7CE)
            )
        }

        // 1. Bonus Gallery Card (Always accessible; hero portraits unlock by level, finale unlocks at 100% Career)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { onOpenBonusGallery() }
                .testTag("collection_bonus_gallery_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = GameSurfaceDark),
            border = BorderStroke(
                width = if (unseenGalleryCount > 0 || isCampaign100Percent) 2.dp else 1.5.dp,
                color = if (unseenGalleryCount > 0 || isCampaign100Percent) GameGold else Color(0xFF6B5399)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
            ) {
                val context = LocalContext.current
                val blurredLockedPreviewBitmap: ImageBitmap? = remember(isCampaign100Percent) {
                    if (isCampaign100Percent) {
                        null
                    } else {
                        runCatching {
                            val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                            val src = BitmapFactory.decodeResource(context.resources, R.drawable.bonus_final_1, opts)
                            src?.let {
                                val tiny = Bitmap.createScaledBitmap(it, 18, 10, true)
                                val soft = Bitmap.createScaledBitmap(tiny, 180, 100, true)
                                if (tiny !== soft) tiny.recycle()
                                if (it !== soft) it.recycle()
                                soft.asImageBitmap()
                            }
                        }.getOrNull()
                    }
                }

                if (isCampaign100Percent) {
                    Image(
                        painter = painterResource(id = R.drawable.bonus_final_1),
                        contentDescription = tr("Бонусная галерея", "Bonus Gallery"),
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(0.9f)
                    )
                } else if (blurredLockedPreviewBitmap != null) {
                    Image(
                        bitmap = blurredLockedPreviewBitmap,
                        contentDescription = tr("Бонусная галерея", "Bonus Gallery"),
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(18.dp)
                            .alpha(0.45f)
                            .testTag("bonus_gallery_blurred_preview")
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.bonus_final_1),
                        contentDescription = tr("Бонусная галерея", "Bonus Gallery"),
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(20.dp)
                            .alpha(0.35f)
                            .testTag("bonus_gallery_blurred_preview")
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = if (isCampaign100Percent) {
                                    listOf(
                                        Color(0xEE130C22),
                                        Color(0xAA130C22),
                                        Color(0x44130C22)
                                    )
                                } else {
                                    listOf(
                                        Color(0xF2130C22),
                                        Color(0xD8130C22),
                                        Color(0xB8130C22)
                                    )
                                }
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isCampaign100Percent) GameGold else Color(0xFF281E3B),
                            border = BorderStroke(1.dp, if (isCampaign100Percent) GameGoldLight else Color(0xFF5A4380))
                        ) {
                            Text(
                                text = tr(
                                    "🖼️ Открыто: ${allUnlockedGalleryKeys.size}",
                                    "🖼️ Unlocked: ${allUnlockedGalleryKeys.size}"
                                ),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isCampaign100Percent) Color.Black else Color(0xFFE6DCFA),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        if (unseenGalleryCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = GameGold,
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f))
                            ) {
                                Text(
                                    text = tr("✨ Новых: $unseenGalleryCount", "✨ New: $unseenGalleryCount"),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr("Бонусная галерея", "Bonus Gallery"),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = if (isCampaign100Percent) {
                                    tr(
                                        "Портреты героинь и финальные иллюстрации за 100% Карьеры",
                                        "Heroine portraits & finale artworks for 100% Career"
                                    )
                                } else {
                                    tr(
                                        "Портреты героинь по уровням (финал — после 4★ всей Карьеры: $fourStarStagesCount/$totalStages)",
                                        "Heroine portraits by level (finale after 4★ Career: $fourStarStagesCount/$totalStages)"
                                    )
                                },
                                fontSize = 10.5.sp,
                                color = Color(0xFFD5CBE9),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onOpenBonusGallery,
                            colors = ButtonDefaults.buttonColors(containerColor = GameGold, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.heightIn(min = 44.dp)
                        ) {
                            Text(tr("Открыть", "Open"), fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 2. Ancient Amulets (4 Cardinal Amulets from Career)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = GameSurfaceDark),
            border = BorderStroke(1.dp, Color(0xFF453664))
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
                        text = tr("Древние Амулеты", "Ancient Amulets"),
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = GameGoldLight
                    )
                    Text(
                        text = "${profile.foundAmulets.size}/4",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = GameGold
                    )
                }

                val amuletsMeta = listOf(
                    Triple("amulet_south", tr("Амулет Юга", "South Amulet"), Pair(R.drawable.img_amulet_south, tr("Глава 1-6", "Stage 1-6"))),
                    Triple("amulet_east", tr("Амулет Востока", "East Amulet"), Pair(R.drawable.img_amulet_east, tr("Глава 2-6", "Stage 2-6"))),
                    Triple("amulet_north", tr("Амулет Севера", "North Amulet"), Pair(R.drawable.img_amulet_north, tr("Глава 3-6", "Stage 3-6"))),
                    Triple("amulet_west", tr("Амулет Запада", "West Amulet"), Pair(R.drawable.img_amulet_west, tr("Глава 4-4", "Stage 4-4")))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    amuletsMeta.forEach { (id, name, artAndHint) ->
                        val isFound = id in profile.foundAmulets
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isFound) Color(0xFF281D3F) else Color(0xFF171124),
                            border = BorderStroke(1.2.dp, if (isFound) GameGold else Color(0xFF33264D))
                        ) {
                            Column(
                                modifier = Modifier.padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF120C1E)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = artAndHint.first),
                                        contentDescription = name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .alpha(if (isFound) 1f else 0.18f)
                                    )
                                    if (!isFound) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = Color(0xFFAFA2C6),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = name,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFound) Color.White else Color(0xFF8F82A8),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isFound) tr("Найден", "Found") else artAndHint.second,
                                    fontSize = 8.5.sp,
                                    color = if (isFound) Color(0xFF81C784) else Color(0xFF7B6F94),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. World Lore Entry
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { onOpenWorldLore() }
                .testTag("collection_world_lore_card"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = GameSurfaceDark),
            border = BorderStroke(1.dp, Color(0xFF5A4380))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr("📜 История Аврезии", "📜 Lore of Avresia"),
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = GameGoldLight
                    )
                    Text(
                        text = tr(
                            "Вспомните пролог, разлом у вулкана Патрея и историю четырёх амулетов",
                            "Revisit the prologue, Mount Patreya rift, and the quest for the four amulets"
                        ),
                        fontSize = 11.sp,
                        color = Color(0xFFB5A7CE)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tr("Читать →", "Read →"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = GameGold
                )
            }
        }

        // 4. Hall of Feats (Player Progress Summary)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = GameSurfaceDark),
            border = BorderStroke(1.dp, Color(0xFF453664))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = tr("Зал Подвигов", "Hall of Feats"),
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = GameGoldLight
                )

                val stats = listOf(
                    tr("Карьера", "Career") to "$clearedStagesCount/$totalStages",
                    tr("Звёзды Карьеры", "Career Stars") to "$totalStars/$maxStars",
                    tr("Героини", "Heroines") to "$unlockedHeroesCount/${heroes.size}",
                    tr("Макс. уровень", "Max Level") to "$maxLevelHeroesCount/${heroes.size}",
                    tr("Победы / Поражения", "Wins / Losses") to "${profile.battlesWon} / ${profile.battlesLost}",
                    tr("Арена", "Arena") to "🏆 ${profile.trophies}",
                    tr("Колизей (побед)", "Coliseum Wins") to "${profile.coliseumWon}",
                    tr("Башня (сегодня / всего)", "Tower (Today / Total)") to "$towerClearedToday/$TOWER_TOTAL_FLOORS (${profile.towerTotalWins})"
                )

                stats.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pair.forEach { (label, value) ->
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1D1530),
                                border = BorderStroke(1.dp, Color(0xFF382A52))
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        color = Color(0xFFAFA2C6),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = value,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
