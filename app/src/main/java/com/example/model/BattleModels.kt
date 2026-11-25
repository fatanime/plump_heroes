package com.example.model

data class BattleCard(
    val heroId: String,
    val name: String,
    val title: String,
    val heroType: HeroType,
    val rarity: CardRarity,
    val element: Element,
    val imageRes: Int,
    val slotIndex: Int, // 0: Front-Left, 1: Front-Right, 2: Back-Left, 3: Back-Right
    val isPlayer: Boolean,
    val level: Int = 1,
    var maxHp: Int,
    var currentHp: Int,
    var maxArmor: Int,
    var currentArmor: Int,
    var atk: Int,
    var skillEnergy: Int = 0,
    val skill: HeroSkill? = null,
    val attackPattern: AttackPattern = AttackPattern.SINGLE,
    var isAlive: Boolean = true,
    var hasActedThisRound: Boolean = false,
    var isTargetSelected: Boolean = false,
    var poisonRounds: Int = 0,
    var burnRounds: Int = 0,
    var burnPercent: Int = 10,
    var isWeakened: Boolean = false,
    var weakenedRounds: Int = 0,
    var isFrozen: Boolean = false,
    var freezeRounds: Int = 0,
    var isStunned: Boolean = false,
    var stunRounds: Int = 0,
    var isBlinded: Boolean = false,
    var blindRounds: Int = 0,
    var isCharmed: Boolean = false,
    var charmRounds: Int = 0,
    var isSlowed: Boolean = false,
    var slowRounds: Int = 0,
    var poisonAppliedRound: Int = 0,
    var burnAppliedRound: Int = 0,
    val availableSkills: List<HeroSkill> = emptyList()
) {
    val isFrontRow: Boolean get() = slotIndex == 0 || slotIndex == 1
    val isBackRow: Boolean get() = slotIndex == 2 || slotIndex == 3
    val hasMagicUnlocked: Boolean get() = level >= 3 && (skill != null || availableSkills.isNotEmpty())
    val hasSkipTurnEffect: Boolean get() = isFrozen || isBlinded || isCharmed || isStunned
}

data class FloatingCombatText(
    val id: Long,
    val slotIndex: Int,
    val isPlayer: Boolean,
    val text: String,
    val type: CombatTextType
)

enum class CombatTextType {
    DAMAGE,
    CRIT_DAMAGE,
    SHIELD_BLOCK,
    SHIELD_BOOST,
    HEAL,
    SKILL,
    POISON,
    BURN
}

data class BattleActionLog(
    val id: Long,
    val text: String,
    val isPlayerAction: Boolean
)

enum class BattlePhase {
    PLAYER_TURN,
    ENEMY_TURN,
    VICTORY,
    DEFEAT
}

data class AttackAnimation(
    val attackerSlot: Int,
    val isPlayerAttacker: Boolean,
    val targetSlot: Int,
    val targetSlots: List<Int> = listOf(targetSlot),
    val isSkill: Boolean = false,
    val id: Long = System.currentTimeMillis()
)

data class BattleState(
    val round: Int = 1,
    val phase: BattlePhase = BattlePhase.PLAYER_TURN,
    val playerCards: List<BattleCard> = emptyList(),
    val enemyCards: List<BattleCard> = emptyList(),
    val selectedPlayerSlot: Int? = null,
    val targetedEnemySlot: Int? = null,
    val playerTeamMana: Int = 0,
    val playerMaxMana: Int = 10,
    val enemyTeamMana: Int = 0,
    val enemyMaxMana: Int = 10,
    val isAutoBattle: Boolean = false,
    val battleSpeed: Float = 1f,
    val combatTexts: List<FloatingCombatText> = emptyList(),
    val actionLogs: List<BattleActionLog> = emptyList(),
    val enemyName: String = "Теневой Страж",
    val enemyTitle: String = "Глава клана",
    val isArena: Boolean = false,
    val campaignChapter: Int = 1, // 1: Emerald Hills / Castle, 2: Dark Forest, 3: Fire Citadel, 0: Arena
    val activeAttackAnimation: AttackAnimation? = null,
    val rewardGold: Int = 120,
    val rewardGems: Int = 15,
    val rewardExp: Int = 50,
    val rewardCard: HeroCard? = null,
    val rewardArtifact: Artifact? = null,
    val battleBgRes: Int = com.example.R.drawable.bg_brunhilde,
    val foundAmulet: DiscoveredAmulet? = null,
    val victoryEventMessage: String? = null,
    val showArenaFirstTurnModal: Boolean = false,
    val isPlayerFirstTurn: Boolean = true,
    val isProcessingRoundEffects: Boolean = false,
    val dotHitTargets: Set<Pair<Boolean, Int>> = emptySet(),
    val battleStartQuote: HeroBattleQuote? = null,
    val victoryQuote: HeroBattleQuote? = null,
    val playerSkillsUsedCount: Int = 0,
    val campaignStageId: Int? = null,
    val isColiseum: Boolean = false,
    val coliseumBattleNumber: Int = 0,
    val coliseumAccumulatedGold: Int = 0,
    val coliseumAccumulatedGems: Int = 0,
    val coliseumConsolationGold: Int = 0,
    val coliseumConsolationGems: Int = 0,
    val isTower: Boolean = false,
    val towerFloor: Int = 0,
    val towerRuleLabel: String? = null,
    val isBoss: Boolean = false
) {
    fun isBattleOver(): Boolean {
        val playerAllDead = playerCards.none { it.isAlive }
        val enemyAllDead = enemyCards.none { it.isAlive }
        return playerAllDead || enemyAllDead
    }
}
