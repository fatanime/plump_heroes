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
import androidx.compose.material.icons.filled.Refresh
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
import com.example.i18n.GameLocaleState
import com.example.i18n.localizeBotTitle
import com.example.i18n.localizeHeroNameByIdOrRu
import com.example.i18n.localizedName
import com.example.i18n.tr
import com.example.model.ArenaBot
import com.example.model.HeroCard
import com.example.model.PlayerProfile
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun ArenaScreen(
    profile: PlayerProfile,
    arenaBots: List<ArenaBot>,
    allHeroes: List<HeroCard>,
    onRefreshBots: () -> Unit,
    onChallengeBot: (ArenaBot) -> Unit,
    onBackToHub: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            currentTime = System.currentTimeMillis()
        }
    }

    // 1 key per 2 hours = 7,200,000 ms (10 hours for 5 keys)
    val periodMs = 2 * 3600 * 1000L
    val timeToNextKeyMs = if (profile.arenaKeys < 5) {
        val elapsed = (currentTime - profile.lastArenaKeyRegenTimestamp).coerceAtLeast(0L)
        (periodMs - (elapsed % periodMs)).coerceAtLeast(0L)
    } else {
        0L
    }

    val timerHours = (timeToNextKeyMs / 3600000L)
    val timerMinutes = (timeToNextKeyMs % 3600000L) / 60000L
    val timerSeconds = (timeToNextKeyMs % 60000L) / 1000L
    val nextKeyTimerFormatted = String.format(Locale.getDefault(), "%02d:%02d:%02d", timerHours, timerMinutes, timerSeconds)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GameBgDark)
            .padding(horizontal = 12.dp)
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        if (onBackToHub != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onBackToHub() }
                    .padding(vertical = 4.dp, horizontal = 4.dp)
                    .testTag("arena_back_to_hub")
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

        // Arena Banner
        Text(
            text = tr("Арена Славы", "Arena of Glory"),
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            color = GameGoldLight
        )
        Text(
            text = tr(
                "Случайные противники вашего ранга. Испытайте свою колоду и завоюйте трофеи!",
                "Random challengers of your rank. Test your deck and win trophies!"
            ),
            fontSize = 11.sp,
            color = Color(0xFFAFA2C6)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Player Arena Standing Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = GameSurfaceDark),
            border = BorderStroke(1.5.dp, GameGold)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF6A1B9A),
                        border = BorderStroke(2.dp, GameGold),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🏆", fontSize = 20.sp)
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        val displayName = if (profile.name == "Хранительница" && GameLocaleState.isEnglish) {
                            "Guardian"
                        } else {
                            profile.name
                        }
                        Text(
                            text = displayName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = tr("Лига: Золотой Воитель", "League: Golden Warrior"),
                            fontSize = 11.sp,
                            color = GameGoldLight
                        )
                        Text(
                            text = tr("Трофеи: ${profile.trophies} 🏆", "Trophies: ${profile.trophies} 🏆"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFFD54F)
                        )
                    }
                }

                // Arena Keys & Regen Timer
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (profile.arenaKeys == 0) Color(0xFF3E1A24) else Color(0xFF261D3B),
                        border = BorderStroke(1.dp, if (profile.arenaKeys == 0) Color(0xFFEF5350) else Color(0xFF4C3E6B))
                    ) {
                        Text(
                            text = tr("🔑 Ключи: ${profile.arenaKeys}/5", "🔑 Keys: ${profile.arenaKeys}/5"),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (profile.arenaKeys == 0) Color(0xFFFFCDD2) else Color(0xFFE1BEE7),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    if (profile.arenaKeys < 5) {
                        Text(
                            text = tr("⏳ +1 через $nextKeyTimerFormatted", "⏳ +1 in $nextKeyTimerFormatted"),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD54F)
                        )
                    }
                    Text(
                        text = tr("+28 🏆 за победу", "+28 🏆 per win"),
                        fontSize = 9.sp,
                        color = Color(0xFF81C784)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Lockout Warning if 0 keys
        if (profile.arenaKeys <= 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF38121E)),
                border = BorderStroke(1.2.dp, Color(0xFFE53935)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔒", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = tr("Вход на Арену временно закрыт!", "Arena Entry Temporarily Locked!"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFFFF8A80)
                        )
                        Text(
                            text = tr(
                                "Закончились ключи. Первый ключ восстановится через $nextKeyTimerFormatted (1 ключ за 2 часа).",
                                "Out of keys. Your next key restores in $nextKeyTimerFormatted (1 key every 2 hours)."
                            ),
                            fontSize = 10.sp,
                            color = Color(0xFFEDE7F6)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Matchmaking Row & Refresh Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tr("Доступные соперники:", "Available Opponents:"),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            TextButton(
                onClick = onRefreshBots,
                modifier = Modifier.testTag("refresh_arena_bots")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = GameGold, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = tr("Обновить список", "Refresh List"), color = GameGold, fontSize = 11.sp)
            }
        }

        // List of Arena Opponents
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(arenaBots) { bot ->
                ArenaBotCard(
                    bot = bot,
                    allHeroes = allHeroes,
                    hasKeys = profile.arenaKeys > 0,
                    onChallenge = { onChallengeBot(bot) }
                )
            }
        }
    }
}

@Composable
fun ArenaBotCard(
    bot: ArenaBot,
    allHeroes: List<HeroCard>,
    hasKeys: Boolean = true,
    onChallenge: () -> Unit
) {
    val rewardGoldDisplay = bot.rewardGold ?: ((350 + (bot.rating / 3)) * 0.5f).toInt()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("arena_bot_${bot.name}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (bot.isHardChallenge) Color(0xFF281629) else GameCardBg
        ),
        border = BorderStroke(
            width = if (bot.isHardChallenge) 2.dp else 1.dp,
            color = if (bot.isHardChallenge) Color(0xFFFF7043) else Color(0xFF493A68)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (bot.isHardChallenge) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF4A1812),
                    border = BorderStroke(1.dp, Color(0xFFFF7043))
                ) {
                    Text(
                        text = tr("🔥 БОСС АРЕНЫ (+42% ⚔️)", "🔥 ARENA BOSS (+42% ⚔️)"),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFFAB91),
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .testTag("arena_hard_bot_badge")
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = if (bot.isHardChallenge) Color(0xFF4E1B24) else Color(0xFF32234C),
                        border = BorderStroke(1.dp, if (bot.isHardChallenge) Color(0xFFFF7043) else GameGold),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = if (bot.isHardChallenge) "🔥" else "⚔️", fontSize = 16.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = localizeHeroNameByIdOrRu(null, bot.name),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = localizeBotTitle(bot.title),
                            fontSize = 10.sp,
                            color = Color(0xFFBCAFD4)
                        )
                    }
                }

                // Bot Rating
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF1E1630)
                ) {
                    Text(
                        text = "${bot.rating} 🏆",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GameGoldLight,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Reward preview row
            Text(
                text = tr(
                    "Награда: 🪙 $rewardGoldDisplay • 💎 ${bot.rewardGems} • ⭐ ${bot.rewardExp} XP",
                    "Reward: 🪙 $rewardGoldDisplay • 💎 ${bot.rewardGems} • ⭐ ${bot.rewardExp} XP"
                ),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (bot.isHardChallenge) Color(0xFFFFD54F) else Color(0xFFAFA2C6)
            )

            // Team Lineup Preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = tr("Отряд бота:", "Bot Squad:"), fontSize = 9.sp, color = Color.Gray)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        bot.heroIds.forEach { id ->
                            val hero = allHeroes.firstOrNull { it.id == id }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF2A203F),
                                border = BorderStroke(0.5.dp, Color(0xFF5A487D))
                            ) {
                                Text(
                                    text = hero?.localizedName() ?: tr("Герой", "Hero"),
                                    fontSize = 9.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Challenge Button
                Button(
                    onClick = onChallenge,
                    enabled = hasKeys,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (bot.isHardChallenge) Color(0xFFD84315) else Color(0xFFE91E63),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF382A45),
                        disabledContentColor = Color(0xFF8E82A1)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("challenge_bot_${bot.name}")
                ) {
                    Text(if (hasKeys) "⚔️" else "🔒", fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (hasKeys) tr("ВЫЗОВ", "FIGHT") else tr("НЕТ КЛЮЧЕЙ", "NO KEYS"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
