package com.example.model

data class CampaignStage(
    val id: Int,
    val chapterId: Int,
    val stageNumber: Int,
    val title: String,
    val recommendedPower: Int,
    val requiredPlayerLevel: Int = 1,
    val enemyHeroIds: List<String>,
    val enemyLevels: List<Int>,
    var stars: Int = 0,
    var isUnlocked: Boolean = false,
    val isBoss: Boolean = false,
    val rewardGold: Int,
    val rewardGems: Int,
    val rewardExp: Int,
    val rewardShardsHeroId: String? = null,
    val rewardArtifactId: String? = null,
    val totalEnemyAtk: Int = 0,
    val battleBgRes: Int? = null,
    var clearCount: Int = 0
)

data class CareerStagePayout(
    val gold: Int,
    val gems: Int,
    val exp: Int,
    val shards: Int,
    val isReducedTier: Boolean
)

fun calculateCareerStagePayout(stage: CampaignStage, baseShards: Int = 5): CareerStagePayout {
    return when {
        stage.clearCount > 10 -> CareerStagePayout(
            gold = (stage.rewardGold * 0.20f).toInt().coerceAtLeast(1),
            gems = (stage.rewardGems * 0.20f).toInt().coerceAtLeast(1),
            exp = (stage.rewardExp * 0.20f).toInt().coerceAtLeast(1),
            shards = kotlin.math.round(baseShards * 0.20f).toInt().coerceAtLeast(1),
            isReducedTier = true
        )
        stage.clearCount > 5 -> CareerStagePayout(
            gold = (stage.rewardGold * 0.35f).toInt().coerceAtLeast(1),
            gems = (stage.rewardGems * 0.35f).toInt().coerceAtLeast(1),
            exp = (stage.rewardExp * 0.35f).toInt().coerceAtLeast(1),
            shards = kotlin.math.round(baseShards * 0.35f).toInt().coerceAtLeast(1),
            isReducedTier = true
        )
        stage.stars > 0 -> CareerStagePayout(
            gold = (stage.rewardGold * 0.50f).toInt(),
            gems = stage.rewardGems,
            exp = stage.rewardExp,
            shards = baseShards,
            isReducedTier = false
        )
        else -> CareerStagePayout(
            gold = stage.rewardGold,
            gems = stage.rewardGems,
            exp = stage.rewardExp,
            shards = baseShards,
            isReducedTier = false
        )
    }
}

data class CampaignChapter(
    val id: Int,
    val name: String,
    val subtitle: String,
    val stages: List<CampaignStage>
)

data class ArenaBot(
    val name: String,
    val title: String,
    val rating: Int,
    val avatarRes: Int,
    val heroIds: List<String>,
    val heroLevels: List<Int>,
    val targetTotalAtk: Int = 0,
    val targetTotalHp: Int = 0,
    val isHardChallenge: Boolean = false,
    val rewardGold: Int? = null,
    val rewardGems: Int = 25,
    val rewardExp: Int = 60
)

data class PlayerProfile(
    val name: String = "Хранительница",
    val level: Int = 1,
    val exp: Int = 0,
    val maxExp: Int = 100,
    val gold: Int = 2000,
    val gems: Int = 100,
    val arenaKeys: Int = 5,
    val trophies: Int = 100,
    val activeDeckHeroIds: List<String?> = listOf("brunhilde", "olga", "berta", "witch"),
    val isGoogleSynced: Boolean = false,
    val googleAccountEmail: String? = null,
    val lastSyncTimestamp: Long = 0L,
    val lastArenaKeyRegenTimestamp: Long = System.currentTimeMillis(),
    val lastDailyClaimDate: String? = null,
    val lastDailyClaimTimestamp: Long = 0L,
    val dailyStreak: Int = 0,
    val maxSeenTimestamp: Long = System.currentTimeMillis(),
    val isTutorialCompleted: Boolean = false,
    val isStoryRead: Boolean = false,
    val battlesWon: Int = 0,
    val battlesLost: Int = 0,
    val foundAmulets: List<String> = emptyList(),
    val isCampaign100AchievementShown: Boolean = false,
    val lastCelebratedLevel: Int = 1,
    val coliseumWon: Int = 0,
    val coliseumLost: Int = 0,
    val lastColiseumTimestamp: Long = 0L,
    val coliseumEntriesDate: String? = null,
    val coliseumEntriesUsedToday: Int = 0,
    val isColiseumTutorialCompleted: Boolean = false,
    val towerProgressDate: String? = null,
    val towerClearedFloorToday: Int = 0,
    val towerAttemptsUsedToday: Int = 0,
    val towerTotalWins: Int = 0,
    val isTowerTutorialCompleted: Boolean = false,
    val towerFloorStarsToday: List<Int> = emptyList(),
    val towerRoofClearedToday: Boolean = false,
    val towerRoofStarsToday: Int = 0,
    val towerDamagedFloor: Int = 0,
    val towerSavedEnemyHps: List<Int> = emptyList(),
    val towerDeckHeroIds: List<String?> = emptyList(),
    val viewedGalleryImageKeys: Set<String> = emptySet(),
    val isSoundMuted: Boolean = false,
    val skippedUpdateVersionCode: Int = 0
)

const val COLISEUM_DAILY_ENTRY_LIMIT = 2

fun formatColiseumCalendarDate(timestampMs: Long): String {
    return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(timestampMs))
}

fun computeColiseumRemainingEntries(
    profile: PlayerProfile,
    nowMs: Long = System.currentTimeMillis()
): Int {
    if (nowMs + 5 * 60_000L < profile.maxSeenTimestamp) {
        return 0
    }
    val todayStr = formatColiseumCalendarDate(nowMs)
    val usedToday = when {
        profile.coliseumEntriesDate == todayStr -> profile.coliseumEntriesUsedToday
        profile.coliseumEntriesDate == null &&
            profile.lastColiseumTimestamp > 0L &&
            formatColiseumCalendarDate(profile.lastColiseumTimestamp) == todayStr -> 1
        else -> 0
    }
    return (COLISEUM_DAILY_ENTRY_LIMIT - usedToday).coerceIn(0, COLISEUM_DAILY_ENTRY_LIMIT)
}

fun computeColiseumCooldownRemainingMs(
    profile: PlayerProfile,
    nowMs: Long = System.currentTimeMillis()
): Long {
    if (computeColiseumRemainingEntries(profile, nowMs) > 0) return 0L
    val refTime = maxOf(nowMs, profile.maxSeenTimestamp)
    val nextMidnight = java.util.Calendar.getInstance().apply {
        timeInMillis = refTime
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
        add(java.util.Calendar.DAY_OF_MONTH, 1)
    }.timeInMillis
    return (nextMidnight - nowMs).coerceAtLeast(1L)
}

data class ColiseumRunState(
    val battleNumber: Int = 1,
    val accumulatedGold: Int = 0,
    val accumulatedGems: Int = 0,
    val initialDeckAtk: Int = 0,
    val initialDeckHp: Int = 0,
    val avgDeckLevel: Int = 1,
    val playerCards: List<BattleCard> = emptyList(),
    val bonusStartMana: Int = 0
)

data class ColiseumBattlePreview(
    val battleNumber: Int,
    val opponentName: String,
    val opponentBot: ArenaBot,
    val rewardGold: Int,
    val rewardGems: Int
)

data class ColiseumMilestoneBonusOffer(
    val completedBattleNumber: Int,
    val healPercent: Int,
    val revivePercent: Int,
    val aliveCount: Int,
    val deadCards: List<BattleCard>,
    val currentBonusStartMana: Int = 0
)

data class LevelUpRewardPopup(
    val newLevel: Int,
    val goldReward: Int,
    val gemsReward: Int
)

data class DiscoveredAmulet(
    val id: String,
    val name: String,
    val imageRes: Int,
    val extraNote: String? = null
)

data class DailyBonusReward(
    val dayNumber: Int,
    val goldAmount: Int,
    val artifact: Artifact? = null
)

enum class FortuneWheelSectorType {
    GOLD_UP_TO_500,
    GOLD_UP_TO_1000,
    SHARDS_5,
    SHARDS_10,
    SECRET_SHARDS_3,
    GEMS_40,
    GEMS_100,
    ARTIFACT_BASIC,
    ARTIFACT_SPECIAL,
    EMPTY
}

data class FortuneWheelSector(
    val id: Int,
    val label: String,
    val emoji: String,
    val type: FortuneWheelSectorType
)

data class FortuneWheelOffer(
    val dayNumber: Int,
    val sectors: List<FortuneWheelSector>
)

data class FortuneWheelResult(
    val sector: FortuneWheelSector,
    val description: String,
    val isEmpty: Boolean
)

data class ChestOpeningResult(
    val chestTitle: String,
    val accentColorHex: Long,
    val chestImageRes: Int,
    val heroId: String? = null,
    val heroName: String? = null,
    val heroImageRes: Int? = null,
    val shardAmount: Int = 0,
    val dropArtifact: Artifact? = null,
    val summaryMessage: String,
    val isSecretMystery: Boolean = false
)

const val TOWER_TOTAL_FLOORS = 5
const val TOWER_ROOF_FLOOR = 6
const val TOWER_MIN_PLAYER_LEVEL = 7
const val TOWER_DAILY_ATTEMPT_LIMIT = 2

enum class TowerDayRule(
    val dayOfWeekCalendar: Int,
    val iconSymbol: String,
    val titleRu: String,
    val titleEn: String,
    val shortBadgeRu: String,
    val shortBadgeEn: String,
    val descriptionRu: String,
    val descriptionEn: String
) {
    MONDAY_STEEL(
        dayOfWeekCalendar = java.util.Calendar.MONDAY,
        iconSymbol = "⚔️",
        titleRu = "Испытание Стали",
        titleEn = "Trial of Steel",
        shortBadgeRu = "Пн • Сталь",
        shortBadgeEn = "Mon • Steel",
        descriptionRu = "Фокус на Ближний и Смешанный бой (+20% брони и +15% атаки передовой; без них штраф не накладывается, если подходящих карт меньше 4).",
        descriptionEn = "Melee & Hybrid focus (+20% Armor & +15% ATK for frontliners; strict roster rule applies once you own 4+ eligible heroines)."
    ),
    TUESDAY_ARCANE_VOLLEY(
        dayOfWeekCalendar = java.util.Calendar.TUESDAY,
        iconSymbol = "🏹",
        titleRu = "Град Стрел и Магии",
        titleEn = "Arcane & Arrow Volley",
        shortBadgeRu = "Вт • Залп",
        shortBadgeEn = "Tue • Volley",
        descriptionRu = "У врагов укреплена броня, а героини Дальнего боя наносят +35% урона.",
        descriptionEn = "Enemies have reinforced armor, while Ranged heroines deal +35% damage."
    ),
    WEDNESDAY_ELEMENTAL_STORM(
        dayOfWeekCalendar = java.util.Calendar.WEDNESDAY,
        iconSymbol = "🌪️",
        titleRu = "Стихийный шторм",
        titleEn = "Elemental Storm",
        shortBadgeRu = "Ср • Стихия",
        shortBadgeEn = "Wed • Element",
        descriptionRu = "Стихия недели усиливает атаку подходящих героинь на +35%.",
        descriptionEn = "This week's featured element empowers matching heroines with +35% ATK."
    ),
    THURSDAY_MANA_SURGE(
        dayOfWeekCalendar = java.util.Calendar.THURSDAY,
        iconSymbol = "💧",
        titleRu = "Магический всплеск",
        titleEn = "Mana Surge",
        shortBadgeRu = "Чт • Мана",
        shortBadgeEn = "Thu • Mana",
        descriptionRu = "В каждом бою Башни ваш отряд начинает схватку сразу с 3 Маны вместо 1!",
        descriptionEn = "Your squad starts every Tower battle with 3 Mana instead of 1!"
    ),
    FRIDAY_COMMONERS_VALOR(
        dayOfWeekCalendar = java.util.Calendar.FRIDAY,
        iconSymbol = "🛡️",
        titleRu = "Испытание Редкости",
        titleEn = "Valor of Classics",
        shortBadgeRu = "Пт • Редкость",
        shortBadgeEn = "Fri • Rarity",
        descriptionRu = "Базовые и Особые карты получают +25% к здоровью и атаке (строгий допуск при наличии 4+ карт).",
        descriptionEn = "Basic & Special heroines gain +25% HP & ATK (strict roster filter when you own 4+ eligible heroines)."
    ),
    SATURDAY_BOSS_SIEGE(
        dayOfWeekCalendar = java.util.Calendar.SATURDAY,
        iconSymbol = "👑",
        titleRu = "Осада Боссов",
        titleEn = "Boss Siege",
        shortBadgeRu = "Сб • Боссы",
        shortBadgeEn = "Sat • Bosses",
        descriptionRu = "Каждый этаж охраняет усиленный босс. Повышенная награда алмазами!",
        descriptionEn = "Every floor is guarded by an empowered boss. Bonus gem rewards!"
    ),
    SUNDAY_GOLDEN_ASSAULT(
        dayOfWeekCalendar = java.util.Calendar.SUNDAY,
        iconSymbol = "🪙",
        titleRu = "Золотой штурм",
        titleEn = "Golden Assault",
        shortBadgeRu = "Вс • Золото x2",
        shortBadgeEn = "Sun • 2x Gold",
        descriptionRu = "Все правила открыты! Двойная награда монетами на всех 5 этажах Башни.",
        descriptionEn = "No squad restrictions! Double coin rewards across all 5 Tower floors."
    );

    companion object {
        fun fromDayOfWeek(dayOfWeek: Int): TowerDayRule {
            return entries.firstOrNull { it.dayOfWeekCalendar == dayOfWeek } ?: SUNDAY_GOLDEN_ASSAULT
        }
    }
}

fun resolveTowerDayRule(nowMs: Long = System.currentTimeMillis()): TowerDayRule {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = nowMs }
    return TowerDayRule.fromDayOfWeek(cal.get(java.util.Calendar.DAY_OF_WEEK))
}

fun resolveWednesdayTowerElement(nowMs: Long = System.currentTimeMillis()): Element {
    val weekIndex = (nowMs / (7L * 24L * 3600_000L)).toInt().coerceAtLeast(0)
    val activeElements = listOf(Element.FIRE, Element.WATER, Element.LIGHT, Element.DARK)
    return activeElements[weekIndex % activeElements.size]
}

fun calculateTowerScalingMultiplier(floor: Int): Double {
    return when (floor.coerceIn(1, TOWER_ROOF_FLOOR)) {
        1 -> 0.70 // -30%
        2 -> 0.85 // -15%
        3 -> 1.00 // Same as player's hero deck
        4 -> 1.15 // +15%
        5 -> 1.30 // +30%
        else -> 1.50 // Roof: +50%
    }
}

fun computeTowerClearedFloorToday(
    profile: PlayerProfile,
    nowMs: Long = System.currentTimeMillis()
): Int {
    if (nowMs + 5 * 60_000L < profile.maxSeenTimestamp) {
        return profile.towerClearedFloorToday.coerceIn(0, TOWER_TOTAL_FLOORS)
    }
    val todayStr = formatColiseumCalendarDate(nowMs)
    return if (profile.towerProgressDate == todayStr) {
        profile.towerClearedFloorToday.coerceIn(0, TOWER_TOTAL_FLOORS)
    } else {
        0
    }
}

fun computeTowerFloorStarsToday(
    profile: PlayerProfile,
    nowMs: Long = System.currentTimeMillis()
): List<Int> {
    val activeToday = if (nowMs + 5 * 60_000L < profile.maxSeenTimestamp) {
        true
    } else {
        profile.towerProgressDate == formatColiseumCalendarDate(nowMs)
    }
    if (!activeToday) return List(TOWER_TOTAL_FLOORS) { 0 }
    return List(TOWER_TOTAL_FLOORS) { idx ->
        profile.towerFloorStarsToday.getOrElse(idx) { 0 }.coerceIn(0, 4)
    }
}

fun isTowerRoofUnlockedToday(
    profile: PlayerProfile,
    nowMs: Long = System.currentTimeMillis()
): Boolean {
    val cleared = computeTowerClearedFloorToday(profile, nowMs)
    if (cleared < TOWER_TOTAL_FLOORS) return false
    val stars = computeTowerFloorStarsToday(profile, nowMs)
    return stars.size >= TOWER_TOTAL_FLOORS && stars.take(TOWER_TOTAL_FLOORS).all { it >= 4 }
}

fun isTowerRoofClearedToday(
    profile: PlayerProfile,
    nowMs: Long = System.currentTimeMillis()
): Boolean {
    if (nowMs + 5 * 60_000L < profile.maxSeenTimestamp) {
        return profile.towerRoofClearedToday
    }
    val todayStr = formatColiseumCalendarDate(nowMs)
    return profile.towerProgressDate == todayStr && profile.towerRoofClearedToday
}

fun computeTowerRoofStarsToday(
    profile: PlayerProfile,
    nowMs: Long = System.currentTimeMillis()
): Int {
    val activeToday = if (nowMs + 5 * 60_000L < profile.maxSeenTimestamp) {
        true
    } else {
        profile.towerProgressDate == formatColiseumCalendarDate(nowMs)
    }
    return if (activeToday) profile.towerRoofStarsToday.coerceIn(0, 4) else 0
}

fun computeTowerDamagedFloorEnemyHps(
    profile: PlayerProfile,
    floor: Int,
    nowMs: Long = System.currentTimeMillis()
): List<Int>? {
    val activeToday = if (nowMs + 5 * 60_000L < profile.maxSeenTimestamp) {
        true
    } else {
        profile.towerProgressDate == formatColiseumCalendarDate(nowMs)
    }
    if (!activeToday) return null
    if (profile.towerDamagedFloor != floor || profile.towerSavedEnemyHps.size != 4) return null
    return profile.towerSavedEnemyHps
}

fun computeTowerRemainingAttempts(
    profile: PlayerProfile,
    nowMs: Long = System.currentTimeMillis()
): Int {
    if (nowMs + 5 * 60_000L < profile.maxSeenTimestamp) {
        return 0
    }
    val todayStr = formatColiseumCalendarDate(nowMs)
    val usedToday = if (profile.towerProgressDate == todayStr) {
        profile.towerAttemptsUsedToday
    } else {
        0
    }
    return (TOWER_DAILY_ATTEMPT_LIMIT - usedToday).coerceIn(0, TOWER_DAILY_ATTEMPT_LIMIT)
}

fun computeTowerResetRemainingMs(nowMs: Long = System.currentTimeMillis()): Long {
    val nextMidnight = java.util.Calendar.getInstance().apply {
        timeInMillis = nowMs
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
        add(java.util.Calendar.DAY_OF_MONTH, 1)
    }.timeInMillis
    return (nextMidnight - nowMs).coerceAtLeast(1L)
}

data class TowerFloorPreview(
    val floor: Int,
    val titleRu: String,
    val titleEn: String,
    val enemyHeroIds: List<String>,
    val enemyLevels: List<Int>,
    val rewardGold: Int,
    val rewardGems: Int,
    val rewardExp: Int,
    val bonusShards: Int = 0,
    val isBoss: Boolean = false
)

