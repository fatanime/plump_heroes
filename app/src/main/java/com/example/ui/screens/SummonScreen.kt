package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.i18n.tr
import com.example.model.PlayerProfile
import com.example.ui.theme.*

enum class ChestType {
    BASIC,      // Базовый сундук (500 gold)
    RARITY,     // Сундук редкостей (100 gems)
    ARTIFACTS   // Сундук с артефактами (1000 gold)
}

@Composable
fun SummonScreen(
    profile: PlayerProfile,
    onOpenChest: (ChestType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GameBgDark)
            .padding(horizontal = 14.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = tr("Сундуки с сокровищами", "Treasure Chests"),
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            color = GameGoldLight
        )
        Text(
            text = tr(
                "Открывайте сундуки с сокровищами, чтобы пополнять осколки карт героинь и добывать могущественные артефакты!",
                "Open treasure chests to collect heroine card shards and obtain powerful artifacts!"
            ),
            fontSize = 11.sp,
            color = Color(0xFFAFA2C6)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 1. Basic Chest
        SummonChestCard(
            title = tr("Базовый сундук", "Basic Chest"),
            subtitle = tr(
                "Небольшое количество осколков с возможностью получить базовый артефакт!",
                "A small amount of shards with a chance to get a Basic artifact!"
            ),
            imageRes = R.drawable.img_chest_basic,
            costText = "500 💰",
            canAfford = profile.gold >= 500,
            primaryColor = GameGold,
            onClick = { onOpenChest(ChestType.BASIC) },
            testTag = "open_basic_chest"
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Rarity Chest
        SummonChestCard(
            title = tr("Сундук редкостей", "Rarity Chest"),
            subtitle = tr(
                "Большое количество осколков и возможность получить редкий артефакт!",
                "A large amount of shards and a chance to get a Rare artifact!"
            ),
            imageRes = R.drawable.img_chest_rarity,
            costText = "100 💎",
            canAfford = profile.gems >= 100,
            primaryColor = Color(0xFFBA68C8),
            onClick = { onOpenChest(ChestType.RARITY) },
            testTag = "open_rarity_chest"
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Artifact Chest
        SummonChestCard(
            title = tr("Сундук с артефактами", "Artifact Chest"),
            subtitle = tr(
                "Гарантированный артефакт: Базовый, Редкий или Особый для экипировки героинь!",
                "Guaranteed artifact: Basic, Special, or Rare to equip your heroines!"
            ),
            imageRes = R.drawable.img_chest_artifact,
            costText = "1000 💰",
            canAfford = profile.gold >= 1000,
            primaryColor = Color(0xFF4CAF50),
            onClick = { onOpenChest(ChestType.ARTIFACTS) },
            testTag = "open_artifact_chest"
        )

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun SummonChestCard(
    title: String,
    subtitle: String,
    imageRes: Int,
    costText: String,
    canAfford: Boolean,
    primaryColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GameSurfaceDark),
        border = BorderStroke(1.5.dp, primaryColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Image(
                painter = painterResource(imageRes),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.2.dp, primaryColor.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            )

            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = primaryColor
            )

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color(0xFFDDD2F0),
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onClick,
                enabled = canAfford,
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = Color.Black,
                    disabledContainerColor = Color(0xFF2B213E),
                    disabledContentColor = Color.Gray
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(testTag)
            ) {
                Text("✨", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = tr("ОТКРЫТЬ ($costText)", "OPEN ($costText)"),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp
                )
            }
        }
    }
}
