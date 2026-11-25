package com.example.model

enum class QuestType {
    WIN_ANY_BATTLES,
    WIN_CAREER_BATTLES,
    WIN_ARENA_BATTLES,
    WIN_ARENA_RANGED_ONLY,
    WIN_ARENA_TWO_MELEE,
    WIN_NO_LOSSES,
    EARN_CAREER_STARS,
    WIN_CAREER_BOSS,
    DEFEAT_ENEMIES,
    CAST_SKILLS,
    OPEN_CHESTS,
    UPGRADE_HERO,
    FORGE_COMBINE,
    SELL_ARTIFACT,
    WIN_WITH_HYBRID,
    WIN_WITH_ELEMENT,
    WIN_TWO_HYBRID,
    WIN_FOUR_RANGED
}

enum class QuestRewardType {
    GOLD,
    SHARDS
}

data class QuestTemplate(
    val id: String,
    val title: String,
    val description: String,
    val type: QuestType,
    val difficultyTier: Int, // 1..5 progressive difficulty & reward scaling
    val targetCount: Int,
    val rewardType: QuestRewardType,
    val rewardAmount: Int,
    val requiredElement: Element? = null
)

data class DailyQuestState(
    val id: String,
    val title: String,
    val description: String,
    val type: QuestType,
    val difficultyTier: Int,
    val targetCount: Int,
    val currentProgress: Int = 0,
    val rewardType: QuestRewardType,
    val rewardAmount: Int,
    val isRewardClaimed: Boolean = false,
    val requiredElement: Element? = null
) {
    val isCompleted: Boolean
        get() = currentProgress >= targetCount
}
