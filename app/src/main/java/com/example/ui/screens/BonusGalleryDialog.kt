package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.i18n.GameLocaleState
import com.example.i18n.localizedName
import com.example.i18n.tr
import com.example.model.HeroCard
import com.example.model.getUnlockedHeroGalleryItems
import com.example.ui.components.ZoomableGalleryContainer
import com.example.ui.theme.*

data class BonusGalleryEntry(
    val key: String,
    val imageRes: Int,
    val labelRu: String,
    val labelEn: String,
    val isHeroPortrait: Boolean
)

data class BonusGallerySection(
    val id: String,
    val titleRu: String,
    val titleEn: String,
    val subtitleRu: String,
    val subtitleEn: String,
    val isFinale: Boolean,
    val items: List<BonusGalleryEntry>
)

private val HERO_GALLERY_TIER_MIN_LEVELS = listOf(1, 3, 5, 8, 10, 12)

fun buildBonusGallerySections(
    heroes: List<HeroCard>,
    isCampaign100Percent: Boolean,
    isEnglish: Boolean = GameLocaleState.isEnglish
): List<BonusGallerySection> {
    val sections = mutableListOf<BonusGallerySection>()

    heroes.filter { it.isUnlocked }.forEach { hero ->
        val rawItems = getUnlockedHeroGalleryItems(hero.id, hero.level, hero.imageRes)
        val tierLevels = HERO_GALLERY_TIER_MIN_LEVELS.filter { it <= hero.level.coerceAtLeast(1) }
        val entries = rawItems.mapIndexed { idx, (key, resId) ->
            val tierLv = tierLevels.getOrNull(idx)
            val labelRu = when {
                key.endsWith("_0") || tierLv == 1 -> "Начальный облик"
                key.endsWith("_max") || tierLv == 12 -> "Макс. облик (12 ур.)"
                tierLv != null -> "Облик $tierLv ур."
                else -> "Облик"
            }
            val labelEn = when {
                key.endsWith("_0") || tierLv == 1 -> "Base Form"
                key.endsWith("_max") || tierLv == 12 -> "Max Form (Lv. 12)"
                tierLv != null -> "Lv. $tierLv Form"
                else -> "Portrait"
            }
            BonusGalleryEntry(
                key = key,
                imageRes = resId,
                labelRu = labelRu,
                labelEn = labelEn,
                isHeroPortrait = true
            )
        }
        sections += BonusGallerySection(
            id = "hero_${hero.id}",
            titleRu = hero.name,
            titleEn = hero.localizedName(),
            subtitleRu = "Ур. ${hero.level} • ${entries.size}/6",
            subtitleEn = "Lv. ${hero.level} • ${entries.size}/6",
            isFinale = false,
            items = entries
        )
    }

    if (isCampaign100Percent) {
        val finaleItems = listOf(
            BonusGalleryEntry("bonus_final_1", R.drawable.bonus_final_1, "Финал I", "Finale I", isHeroPortrait = false),
            BonusGalleryEntry("bonus_final_2", R.drawable.bonus_final_2, "Финал II", "Finale II", isHeroPortrait = false),
            BonusGalleryEntry("bonus_final_3", R.drawable.bonus_final_3, "Финал III", "Finale III", isHeroPortrait = false),
            BonusGalleryEntry("bonus_final_4", R.drawable.bonus_final_4, "Финал IV", "Finale IV", isHeroPortrait = false),
            BonusGalleryEntry(
                key = "bonus_final_6",
                imageRes = if (isEnglish) R.drawable.final_6_en else R.drawable.final_6,
                labelRu = "Финал V • Пир победительниц",
                labelEn = "Finale V • Feast of Champions",
                isHeroPortrait = false
            )
        )
        sections += BonusGallerySection(
            id = "career_finale",
            titleRu = "✨ Финал Карьеры (100%)",
            titleEn = "✨ Career Finale (100%)",
            subtitleRu = "Бонусные иллюстрации",
            subtitleEn = "Bonus Artworks",
            isFinale = true,
            items = finaleItems
        )
    }

    return sections
}

@Composable
fun BonusGalleryDialog(
    heroes: List<HeroCard>,
    isCampaign100Percent: Boolean,
    viewedImageKeys: Set<String>,
    onMarkImageViewed: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val isEnglish = GameLocaleState.isEnglish
    val sections = remember(heroes, isCampaign100Percent, isEnglish) {
        buildBonusGallerySections(heroes, isCampaign100Percent, isEnglish)
    }
    val flattenedItems = remember(sections) {
        sections.flatMap { it.items }
    }
    var activeFullscreenIndex by remember { mutableStateOf<Int?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            color = GameBgDark,
            modifier = Modifier
                .fillMaxSize()
                .testTag("bonus_gallery_grid_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Header
                Surface(
                    color = GameSurfaceDark,
                    border = BorderStroke(1.dp, Color(0xFF3F305C)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr("🖼️ Бонусная галерея", "🖼️ Bonus Gallery"),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = GameGoldLight
                            )
                            val unseenCount = flattenedItems.count { it.key !in viewedImageKeys }
                            Text(
                                text = if (unseenCount > 0) {
                                    tr(
                                        "Открыто: ${flattenedItems.size} • Новых: $unseenCount (золотая рамка)",
                                        "Unlocked: ${flattenedItems.size} • New: $unseenCount (gold border)"
                                    )
                                } else {
                                    tr(
                                        "Открыто иллюстраций: ${flattenedItems.size}",
                                        "Unlocked artworks: ${flattenedItems.size}"
                                    )
                                },
                                fontSize = 11.sp,
                                color = Color(0xFFB5A7CE)
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("close_bonus_gallery_grid")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = tr("Закрыть", "Close"),
                                tint = Color.White
                            )
                        }
                    }
                }

                // 2-Column Grid with Heroine Section Dividers
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    sections.forEach { section ->
                        item(
                            key = "divider_${section.id}",
                            span = { GridItemSpan(2) }
                        ) {
                            BonusGallerySectionDivider(section = section)
                        }

                        items(
                            items = section.items,
                            key = { it.key }
                        ) { entry ->
                            val isUnseen = entry.key !in viewedImageKeys
                            BonusGalleryPreviewCard(
                                entry = entry,
                                isUnseen = isUnseen,
                                onClick = {
                                    onMarkImageViewed(entry.key)
                                    val idx = flattenedItems.indexOfFirst { it.key == entry.key }
                                    if (idx >= 0) {
                                        activeFullscreenIndex = idx
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Full-screen Zoomable Image Viewer
    val currentIdx = activeFullscreenIndex
    if (currentIdx != null && flattenedItems.isNotEmpty()) {
        val safeIndex = currentIdx.coerceIn(0, flattenedItems.lastIndex)
        val currentEntry = flattenedItems[safeIndex]

        Dialog(
            onDismissRequest = { activeFullscreenIndex = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.94f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { activeFullscreenIndex = null }
                    .testTag("bonus_image_overlay")
            ) {
                ZoomableGalleryContainer(
                    resetKey = currentEntry.key,
                    maxScale = 3f,
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(top = 60.dp, bottom = 76.dp, start = 48.dp, end = 48.dp)
                ) {
                    Image(
                        painter = painterResource(id = currentEntry.imageRes),
                        contentDescription = tr(currentEntry.labelRu, currentEntry.labelEn),
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("bonus_final_image")
                    )
                }

                // Previous Arrow
                Surface(
                    shape = CircleShape,
                    color = if (safeIndex > 0) Color(0xCC281D40) else Color(0x55181028),
                    border = BorderStroke(1.dp, if (safeIndex > 0) GameGold else Color(0x44FFFFFF)),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp)
                ) {
                    IconButton(
                        onClick = {
                            val nextIdx = if (safeIndex > 0) safeIndex - 1 else flattenedItems.lastIndex
                            onMarkImageViewed(flattenedItems[nextIdx].key)
                            activeFullscreenIndex = nextIdx
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("bonus_gallery_prev")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = tr("Предыдущее фото", "Previous image"),
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Next Arrow
                Surface(
                    shape = CircleShape,
                    color = if (safeIndex < flattenedItems.lastIndex) Color(0xCC281D40) else Color(0x55181028),
                    border = BorderStroke(1.dp, if (safeIndex < flattenedItems.lastIndex) GameGold else Color(0x44FFFFFF)),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp)
                ) {
                    IconButton(
                        onClick = {
                            val nextIdx = if (safeIndex < flattenedItems.lastIndex) safeIndex + 1 else 0
                            onMarkImageViewed(flattenedItems[nextIdx].key)
                            activeFullscreenIndex = nextIdx
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("bonus_gallery_next")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = tr("Следующее фото", "Next image"),
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Bottom Caption & Page Indicator
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xCC181028),
                    border = BorderStroke(1.dp, GameGold),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 18.dp)
                ) {
                    Text(
                        text = "${tr(currentEntry.labelRu, currentEntry.labelEn)} • ${safeIndex + 1} / ${flattenedItems.size}",
                        color = GameGoldLight,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 7.dp)
                            .testTag("bonus_gallery_indicator")
                    )
                }

                // Close Button
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
                        onClick = { activeFullscreenIndex = null },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_bonus_image")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = tr("Закрыть", "Close"),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BonusGallerySectionDivider(section: BonusGallerySection) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp)
            .testTag("gallery_section_${section.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(0.18f),
                thickness = 1.dp,
                color = if (section.isFinale) GameGold else Color(0xFF5E4788)
            )
            Text(
                text = tr(section.titleRu, section.titleEn),
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (section.isFinale) GameGold else GameGoldLight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = tr(section.subtitleRu, section.subtitleEn),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFAFA2C6),
                maxLines = 1
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                thickness = 1.dp,
                color = if (section.isFinale) GameGold else Color(0xFF5E4788)
            )
        }
    }
}

@Composable
private fun BonusGalleryPreviewCard(
    entry: BonusGalleryEntry,
    isUnseen: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clickable(onClick = onClick)
            .testTag("gallery_thumb_${entry.key}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF191128)),
        border = BorderStroke(
            width = if (isUnseen) 2.5.dp else 1.dp,
            color = if (isUnseen) GameGold else Color(0xFF453664)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = entry.imageRes),
                contentDescription = tr(entry.labelRu, entry.labelEn),
                contentScale = ContentScale.Crop,
                alignment = if (entry.isHeroPortrait) Alignment.TopCenter else Alignment.Center,
                modifier = Modifier.fillMaxSize()
            )

            // Bottom gradient scrim for caption legibility
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(54.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xDD0F0919))
                        )
                    )
            )

            if (isUnseen) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = GameGold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .testTag("gallery_unseen_badge_${entry.key}")
                ) {
                    Text(
                        text = tr("НОВ.", "NEW"),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = tr(entry.labelRu, entry.labelEn),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 8.dp, vertical = 7.dp)
            )
        }
    }
}
