package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.i18n.localizedName
import com.example.i18n.tr
import com.example.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * ViewModel dedicated to the 2x2 turn-based battle logic.
 * Manages turn-based phases, round progression, targeting validation,
 * damage/armor calculation, floating combat texts, and health tracking
 * for both Player and Enemy teams of Heroes.
 */
class BattleViewModel : ViewModel() {

    private val _battleState = MutableStateFlow<BattleState?>(null)
    val battleState: StateFlow<BattleState?> = _battleState.asStateFlow()

    private var enemyTurnJob: Job? = null
    private var autoBattleJob: Job? = null

    private var onBattleEndCallback: ((isVictory: Boolean, state: BattleState) -> Unit)? = null
    private var hapticTrigger: ((strong: Boolean) -> Unit)? = null
    private var hasReportedBattleEnd: Boolean = false
    internal var vasilisaCritRollOverride: Int? = null
    internal var witchWeakenRollOverride: Int? = null

    fun setHapticCallback(callback: (strong: Boolean) -> Unit) {
        this.hapticTrigger = callback
    }

    /**
     * Initializes a new 2x2 battle between the player team and enemy team.
     */
    fun startBattle(
        playerCards: List<BattleCard>,
        enemyCards: List<BattleCard>,
        enemyName: String,
        enemyTitle: String,
        isArena: Boolean,
        rewardGold: Int,
        rewardGems: Int,
        rewardExp: Int,
        rewardArtifact: Artifact? = null,
        campaignChapter: Int = 1,
        battleBgRes: Int = com.example.R.drawable.bg_brunhilde,
        foundAmulet: DiscoveredAmulet? = null,
        victoryEventMessage: String? = null,
        campaignStageId: Int? = null,
        initialPlayerMana: Int = 1,
        initialEnemyMana: Int = 1,
        isColiseum: Boolean = false,
        coliseumBattleNumber: Int = 0,
        coliseumAccumulatedGold: Int = 0,
        coliseumAccumulatedGems: Int = 0,
        isTower: Boolean = false,
        towerFloor: Int = 0,
        towerRuleLabel: String? = null,
        isBoss: Boolean = false,
        onBattleEnd: (isVictory: Boolean, state: BattleState) -> Unit
    ) {
        this.onBattleEndCallback = onBattleEnd
        this.hasReportedBattleEnd = false

        val isPlayerFirst = if (isArena && !isColiseum && !isTower) Random.nextBoolean() else true
        val startPhase = if (isPlayerFirst) BattlePhase.PLAYER_TURN else BattlePhase.ENEMY_TURN
        val startSpeaker = playerCards.filter { it.isAlive }.randomOrNull()
        val startQuote = startSpeaker?.let { pickHeroBattleQuote(it, QuoteContext.BATTLE_START) }
        val initialLogs = if (startQuote != null) {
            listOf(
                BattleActionLog(
                    id = System.currentTimeMillis(),
                    text = "💬 ${startQuote.heroName}: «${startQuote.text}»",
                    isPlayerAction = true
                )
            )
        } else emptyList()

        _battleState.value = BattleState(
            round = 1,
            phase = startPhase,
            playerCards = playerCards,
            enemyCards = enemyCards,
            selectedPlayerSlot = if (isPlayerFirst) playerCards.firstOrNull { it.isAlive && !it.hasSkipTurnEffect }?.slotIndex else null,
            enemyName = enemyName,
            enemyTitle = enemyTitle,
            isArena = isArena,
            campaignChapter = campaignChapter,
            rewardGold = rewardGold,
            rewardGems = rewardGems,
            rewardExp = rewardExp,
            rewardArtifact = rewardArtifact,
            battleBgRes = battleBgRes,
            playerTeamMana = initialPlayerMana,
            enemyTeamMana = initialEnemyMana,
            actionLogs = initialLogs,
            foundAmulet = foundAmulet,
            victoryEventMessage = victoryEventMessage,
            showArenaFirstTurnModal = isArena && !isColiseum && !isTower,
            isPlayerFirstTurn = isPlayerFirst,
            battleStartQuote = startQuote,
            campaignStageId = campaignStageId,
            isColiseum = isColiseum,
            coliseumBattleNumber = coliseumBattleNumber,
            coliseumAccumulatedGold = coliseumAccumulatedGold,
            coliseumAccumulatedGems = coliseumAccumulatedGems,
            isTower = isTower,
            towerFloor = towerFloor,
            towerRuleLabel = towerRuleLabel,
            isBoss = isBoss
        )
    }

    fun updateColiseumBattleOutcomeTotals(
        accumulatedGold: Int,
        accumulatedGems: Int,
        consolationGold: Int,
        consolationGems: Int
    ) {
        _battleState.update { current ->
            current?.copy(
                coliseumAccumulatedGold = accumulatedGold,
                coliseumAccumulatedGems = accumulatedGems,
                coliseumConsolationGold = consolationGold,
                coliseumConsolationGems = consolationGems
            )
        }
    }

    /**
     * Dismisses the Arena first-turn popup and launches the enemy turn if bot moves first.
     */
    fun dismissArenaFirstTurnModal() {
        val state = _battleState.value ?: return
        if (!state.showArenaFirstTurnModal) return
        _battleState.update { it?.copy(showArenaFirstTurnModal = false) }
        if (!state.isPlayerFirstTurn && state.phase == BattlePhase.ENEMY_TURN) {
            executeEnemyTurn()
        }
    }

    internal data class DotTickResult(
        val texts: List<FloatingCombatText>,
        val hitTargets: Set<Pair<Boolean, Int>>,
        val logs: List<BattleActionLog>
    )

    internal fun processTeamDotEffects(cards: List<BattleCard>, currentRound: Int, isPlayerTeam: Boolean): DotTickResult {
        val texts = mutableListOf<FloatingCombatText>()
        val hitTargets = mutableSetOf<Pair<Boolean, Int>>()
        val logs = mutableListOf<BattleActionLog>()

        for (card in cards) {
            if (!card.isAlive) continue
            var tookDot = false

            // Poison deals damage only starting from the next round after application
            if (card.poisonRounds > 0 && currentRound > card.poisonAppliedRound) {
                val poisonDmg = if (card.poisonFixedDamage > 0) card.poisonFixedDamage else (card.maxHp * 0.10).toInt().coerceAtLeast(1)
                card.currentHp = (card.currentHp - poisonDmg).coerceAtLeast(0)
                card.poisonRounds = (card.poisonRounds - 1).coerceAtLeast(0)
                if (card.poisonRounds == 0) {
                    card.poisonFixedDamage = 0
                    card.hasKadruPoison = false
                }
                tookDot = true
                texts.add(
                    FloatingCombatText(
                        id = System.nanoTime() + 40 + card.slotIndex,
                        slotIndex = card.slotIndex,
                        isPlayer = card.isPlayer,
                        text = "-$poisonDmg💜",
                        type = CombatTextType.POISON
                    )
                )
                logs.add(
                    BattleActionLog(
                        id = System.nanoTime() + 100 + card.slotIndex,
                        text = tr(
                            "${card.name} получает $poisonDmg ур. от яда 💜",
                            "${card.localizedName()} takes $poisonDmg poison dmg 💜"
                        ),
                        isPlayerAction = isPlayerTeam
                    )
                )
            }

            // Burn deals damage only starting from the next round after application
            if (card.burnRounds > 0 && currentRound > card.burnAppliedRound) {
                val pct = if (card.burnPercent > 0) card.burnPercent else 10
                val burnDmg = (card.maxHp * (pct / 100.0)).toInt().coerceAtLeast(1)
                card.currentHp = (card.currentHp - burnDmg).coerceAtLeast(0)
                card.burnRounds = (card.burnRounds - 1).coerceAtLeast(0)
                tookDot = true
                texts.add(
                    FloatingCombatText(
                        id = System.nanoTime() + 60 + card.slotIndex,
                        slotIndex = card.slotIndex,
                        isPlayer = card.isPlayer,
                        text = "-$burnDmg🧡",
                        type = CombatTextType.BURN
                    )
                )
                logs.add(
                    BattleActionLog(
                        id = System.nanoTime() + 200 + card.slotIndex,
                        text = tr(
                            "${card.name} получает $burnDmg ур. от ожога 🧡",
                            "${card.localizedName()} takes $burnDmg burn dmg 🧡"
                        ),
                        isPlayerAction = isPlayerTeam
                    )
                )
            }

            if (tookDot) {
                hitTargets.add(card.isPlayer to card.slotIndex)
            }

            if (card.currentHp <= 0) {
                card.isAlive = false
                logs.add(
                    BattleActionLog(
                        id = System.nanoTime() + 300 + card.slotIndex,
                        text = tr(
                            "💀 ${card.name} погибает от яда/огня!",
                            "💀 ${card.localizedName()} falls to poison/burn!"
                        ),
                        isPlayerAction = isPlayerTeam
                    )
                )
            }
        }

        return DotTickResult(texts, hitTargets, logs)
    }

    internal fun decrementSkipTurnStatuses(card: BattleCard) {
        if (card.isFrozen) {
            card.freezeRounds--
            if (card.freezeRounds <= 0) {
                card.freezeRounds = 0
                card.isFrozen = false
            }
        }
        if (card.isBlinded) {
            card.blindRounds--
            if (card.blindRounds <= 0) {
                card.blindRounds = 0
                card.isBlinded = false
            }
        }
        if (card.isCharmed) {
            card.charmRounds--
            if (card.charmRounds <= 0) {
                card.charmRounds = 0
                card.isCharmed = false
            }
        }
        if (card.isStunned) {
            card.stunRounds--
            if (card.stunRounds <= 0) {
                card.stunRounds = 0
                card.isStunned = false
            }
        }
    }

    private fun skipTurnMessage(card: BattleCard): String {
        val cName = card.localizedName()
        return when {
            card.isFrozen -> tr("$cName заморожен(а) и пропускает ход! 🥶", "$cName is frozen and skips turn! 🥶")
            card.isBlinded -> tr("$cName ослеплен(а) и пропускает ход! 😵💫", "$cName is blinded and skips turn! 😵💫")
            card.isCharmed -> tr("$cName под действием приворота и пропускает ход! 💋", "$cName is charmed and skips turn! 💋")
            else -> tr("$cName оглушен(а) и пропускает ход! 💫", "$cName is stunned and skips turn! 💫")
        }
    }

    internal fun skillRequiresAllyTarget(skillId: String): Boolean =
        skillId == "leya_skill_1" || skillId == "leya_skill_2"

    internal fun hasCleansableNegativeEffect(card: BattleCard): Boolean {
        return card.isFrozen ||
            card.isStunned ||
            card.isBlinded ||
            card.isCharmed ||
            card.isWeakened ||
            card.isSlowed ||
            card.burnRounds > 0 ||
            (card.poisonRounds > 0 && !card.hasKadruPoison)
    }

    private fun findBestAutoAllyTarget(caster: BattleCard, allies: List<BattleCard>, skillId: String): BattleCard? {
        val otherAliveAllies = allies.filter { it.isAlive && it.slotIndex != caster.slotIndex }
        if (otherAliveAllies.isEmpty()) return null
        return when (skillId) {
            "leya_skill_1" -> otherAliveAllies.maxByOrNull { it.atk }
            "leya_skill_2" -> otherAliveAllies
                .filter { hasCleansableNegativeEffect(it) }
                .maxByOrNull { ally ->
                    val skipTurnWeight = if (ally.hasSkipTurnEffect) 100 else 0
                    val dotWeight = if (ally.burnRounds > 0 || (ally.poisonRounds > 0 && !ally.hasKadruPoison)) 30 else 0
                    val debuffWeight = if (ally.isWeakened || ally.isSlowed) 10 else 0
                    skipTurnWeight + dotWeight + debuffWeight + ally.atk
                }
            else -> null
        }
    }

    private fun selectAutoSkillForCard(card: BattleCard, allies: List<BattleCard>, teamMana: Int): HeroSkill? {
        if (!card.hasMagicUnlocked) return null
        val candidateSkills = if (card.availableSkills.isNotEmpty()) card.availableSkills else listOfNotNull(card.skill)
        return candidateSkills
            .filter { skill ->
                teamMana >= skill.energyCost &&
                    (!skillRequiresAllyTarget(skill.id) || findBestAutoAllyTarget(card, allies, skill.id) != null)
            }
            .maxByOrNull { it.energyCost }
    }

    /**
     * Selects an active player hero slot for acting this turn, or applies a pending ally-targeted skill.
     */
    fun selectPlayerSlot(slotIndex: Int) {
        val current = _battleState.value ?: return
        if (current.isProcessingRoundEffects || current.phase != BattlePhase.PLAYER_TURN) return

        val pendingSlot = current.pendingSkillSlot
        if (pendingSlot != null) {
            if (slotIndex == pendingSlot) {
                _battleState.update { it?.copy(pendingSkillSlot = null) }
                return
            }
            val allyTarget = current.playerCards.firstOrNull { it.slotIndex == slotIndex && it.isAlive }
            if (allyTarget != null) {
                executePlayerSkill(targetAllySlot = slotIndex)
            }
            return
        }

        val card = current.playerCards.firstOrNull { it.slotIndex == slotIndex }
        if (card != null && card.isAlive && !card.hasActedThisRound && !card.hasSkipTurnEffect) {
            _battleState.update { it?.copy(selectedPlayerSlot = slotIndex, pendingSkillSlot = null) }
            hapticTrigger?.invoke(false)
        }
    }

    /**
     * Validates if the selected attacker can target a specific enemy slot on the 2x2 grid.
     * In 2x2 board:
     * - Slots 0 & 1: Front Row
     * - Slots 2 & 3: Back Row
     * Melee must target Front Row if any Front Row heroes are alive.
     * Ranged and Hybrid can target any slot.
     */
    fun canCardTargetSlot(attacker: BattleCard, targetSlot: Int, enemyCards: List<BattleCard>): Boolean {
        val target = enemyCards.firstOrNull { it.slotIndex == targetSlot } ?: return false
        if (!target.isAlive) return false

        val frontRowHasAlive = enemyCards.any { it.isFrontRow && it.isAlive }
        val backRowHasAlive = enemyCards.any { it.isBackRow && it.isAlive }

        return when (attacker.heroType) {
            HeroType.MELEE -> {
                if (frontRowHasAlive) target.isFrontRow else true
            }
            HeroType.RANGED -> {
                // Ranged cards hit ONLY enemies in the second row. If second row is empty/dead, can hit front row.
                if (backRowHasAlive) target.isBackRow else true
            }
            HeroType.HYBRID -> true
        }
    }

    /**
     * Executes player card attack towards the chosen target slot with health & armor resolution.
     */
    fun executePlayerAttack(targetSlotIndex: Int) {
        val state = _battleState.value ?: return
        if (state.phase != BattlePhase.PLAYER_TURN || state.isProcessingRoundEffects) return
        val selectedSlot = state.selectedPlayerSlot ?: return
        val attacker = state.playerCards.firstOrNull {
            it.slotIndex == selectedSlot && it.isAlive && !it.hasActedThisRound && !it.hasSkipTurnEffect
        } ?: return

        if (!canCardTargetSlot(attacker, targetSlotIndex, state.enemyCards)) return

        _battleState.update { it?.copy(pendingSkillSlot = null) }
        performAttackWithPattern(attacker, targetSlotIndex, isPlayerAttacker = true)
    }

    /**
     * Executes player special skill when magic is unlocked (level >= 3) and team energy is sufficient.
     * Ally-targeted skills enter target-selection mode unless targetAllySlot or auto-battle provides a target.
     */
    fun executePlayerSkill(targetAllySlot: Int? = null) {
        val state = _battleState.value ?: return
        if (state.phase != BattlePhase.PLAYER_TURN || state.isProcessingRoundEffects) return
        val selectedSlot = state.selectedPlayerSlot ?: return
        val attacker = state.playerCards.firstOrNull {
            it.slotIndex == selectedSlot && it.isAlive && !it.hasActedThisRound && !it.hasSkipTurnEffect
        } ?: return

        val activeSkill = attacker.skill ?: return
        if (!attacker.hasMagicUnlocked) return
        if (state.playerTeamMana < activeSkill.energyCost) return

        if (skillRequiresAllyTarget(activeSkill.id)) {
            if (targetAllySlot != null) {
                val validAlly = state.playerCards.firstOrNull {
                    it.slotIndex == targetAllySlot && it.isAlive && it.slotIndex != attacker.slotIndex
                } ?: return
                _battleState.update { it?.copy(pendingSkillSlot = null) }
                performSkill(attacker, isPlayerAttacker = true, targetAllySlot = validAlly.slotIndex)
                return
            }
            if (state.isAutoBattle) {
                val chosenAlly = findBestAutoAllyTarget(attacker, state.playerCards, activeSkill.id) ?: return
                performSkill(attacker, isPlayerAttacker = true, targetAllySlot = chosenAlly.slotIndex)
                return
            }
            val hasOtherAliveAlly = state.playerCards.any { it.isAlive && it.slotIndex != attacker.slotIndex }
            if (!hasOtherAliveAlly) {
                _battleState.update {
                    it?.copy(
                        pendingSkillSlot = null,
                        actionLogs = it.actionLogs + BattleActionLog(
                            System.nanoTime(),
                            tr("Нет другой живой союзницы для применения навыка!", "No other alive ally to target with this skill!"),
                            true
                        )
                    )
                }
                return
            }
            _battleState.update {
                it?.copy(
                    pendingSkillSlot = if (it.pendingSkillSlot == attacker.slotIndex) null else attacker.slotIndex
                )
            }
            hapticTrigger?.invoke(false)
            return
        }

        _battleState.update { it?.copy(pendingSkillSlot = null) }
        performSkill(attacker, isPlayerAttacker = true)
    }

    internal fun performAttackWithPattern(
        attacker: BattleCard,
        primaryTargetSlot: Int,
        isPlayerAttacker: Boolean,
        silentInstant: Boolean = false
    ) {
        val state = _battleState.value ?: return
        val targetList = if (isPlayerAttacker) state.enemyCards else state.playerCards
        val aliveTargetSlotsBefore = targetList.filter { it.isAlive }.map { it.slotIndex }.toSet()
        val primaryTarget = targetList.firstOrNull { it.slotIndex == primaryTargetSlot && it.isAlive } ?: return

        if (!silentInstant) {
            hapticTrigger?.invoke(true)
        }

        val targetsToHit = mutableListOf<BattleCard>()

        when (attacker.attackPattern) {
            AttackPattern.SINGLE -> {
                targetsToHit.add(primaryTarget)
            }
            AttackPattern.PIERCE_LINE, AttackPattern.PIERCE_25_75 -> {
                // Column 0: slots 0 and 2; Column 1: slots 1 and 3
                val col = if (primaryTargetSlot == 0 || primaryTargetSlot == 2) listOf(0, 2) else listOf(1, 3)
                col.forEach { slot ->
                    targetList.firstOrNull { it.slotIndex == slot && it.isAlive }?.let { targetsToHit.add(it) }
                }
            }
            AttackPattern.ALL_ENEMIES -> {
                targetsToHit.addAll(targetList.filter { it.isAlive })
            }
            AttackPattern.DIAGONAL_PAIR -> {
                // Diagonal pairs: 0 <-> 3, 1 <-> 2
                val diagonalSlot = when (primaryTargetSlot) {
                    0 -> 3
                    1 -> 2
                    2 -> 1
                    3 -> 0
                    else -> primaryTargetSlot
                }
                targetsToHit.add(primaryTarget)
                if (diagonalSlot != primaryTargetSlot) {
                    targetList.firstOrNull { it.slotIndex == diagonalSlot && it.isAlive }?.let { targetsToHit.add(it) }
                }
            }
            AttackPattern.POISON_MELEE -> {
                targetsToHit.add(primaryTarget)
            }
            AttackPattern.FRONT_50_BACK_25 -> {
                targetsToHit.addAll(targetList.filter { it.isAlive })
            }
        }

        val isSlowMiss = attacker.isSlowed && (Random.nextFloat() < 0.30f)
        if (attacker.isSlowed) {
            attacker.slowRounds--
            if (attacker.slowRounds <= 0) {
                attacker.isSlowed = false
            }
        }

        val newCombatTexts = mutableListOf<FloatingCombatText>()
        val isCrit = Random.nextInt(100) < 5
        var totalDamageDealt = 0
        var totalShieldAbsorbed = 0
        val killedTargets = mutableListOf<String>()

        if (isSlowMiss) {
            for (target in targetsToHit) {
                if (!target.isAlive) continue
                newCombatTexts.add(
                    FloatingCombatText(
                        id = System.nanoTime() + target.slotIndex,
                        slotIndex = target.slotIndex,
                        isPlayer = target.isPlayer,
                        text = tr("Промах! 0⚔️", "Miss! 0⚔️"),
                        type = CombatTextType.SHIELD_BLOCK
                    )
                )
            }
        } else {
            // Basic attacks NEVER apply poison (even for Vlasta); poison is strictly in skills.
            val aliveTargetCount = targetsToHit.count { it.isAlive }
            val patternMultiplier = when (attacker.attackPattern) {
                AttackPattern.PIERCE_LINE -> if (aliveTargetCount <= 1) 1.0 else 0.50
                AttackPattern.DIAGONAL_PAIR -> if (aliveTargetCount <= 1) 1.0 else 0.50
                AttackPattern.ALL_ENEMIES -> when {
                    aliveTargetCount <= 1 -> 1.0
                    aliveTargetCount == 2 -> 0.50
                    aliveTargetCount == 3 -> 0.40
                    else -> 0.35
                }
                else -> 1.0
            }
            val ignoresShield = attacker.attackPattern == AttackPattern.ALL_ENEMIES

            for (target in targetsToHit) {
                if (!target.isAlive) continue
                val boostedAtk = (attacker.atk * (1.0 + attacker.atkBoostPercent / 100.0)).toInt().coerceAtLeast(1)
                val atkStat = if (attacker.isWeakened) (boostedAtk * 0.50).toInt().coerceAtLeast(1) else boostedAtk
                val targetMultiplier = when (attacker.attackPattern) {
                    AttackPattern.FRONT_50_BACK_25 -> if (target.isFrontRow) 0.50 else 0.25
                    AttackPattern.PIERCE_25_75 -> when {
                        aliveTargetCount <= 1 -> 1.0
                        target.isFrontRow -> 0.25
                        else -> 0.75
                    }
                    else -> patternMultiplier
                }
                val rawDamage = (atkStat * targetMultiplier * if (isCrit) 1.5 else 1.0).toInt().coerceAtLeast(1)
                var remainingDmg = rawDamage

                // Health tracking: Armor/Shield absorbs damage first unless basic attack ignores shield
                if (!ignoresShield && target.currentArmor > 0) {
                    val blocked = target.currentArmor.coerceAtMost(remainingDmg)
                    target.currentArmor -= blocked
                    remainingDmg -= blocked
                    totalShieldAbsorbed += blocked
                    newCombatTexts.add(
                        FloatingCombatText(
                            id = System.nanoTime() + target.slotIndex,
                            slotIndex = target.slotIndex,
                            isPlayer = target.isPlayer,
                            text = "-$blocked🤍",
                            type = CombatTextType.SHIELD_BLOCK
                        )
                    )
                }

                // Health tracking: If damage exceeds shield, reduce HP and show damage text.
                if (remainingDmg > 0) {
                    target.currentHp = (target.currentHp - remainingDmg).coerceAtLeast(0)
                    totalDamageDealt += remainingDmg
                    newCombatTexts.add(
                        FloatingCombatText(
                            id = System.nanoTime() + 10 + target.slotIndex,
                            slotIndex = target.slotIndex,
                            isPlayer = target.isPlayer,
                            text = if (isCrit) "-$remainingDmg!" else "-$remainingDmg",
                            type = if (isCrit) CombatTextType.CRIT_DAMAGE else CombatTextType.DAMAGE
                        )
                    )
                }

                // Health tracking: Death status
                if (target.currentHp <= 0) {
                    target.isAlive = false
                    killedTargets.add(target.localizedName())
                } else if (attacker.heroId == "kadru" || attacker.attackPattern == AttackPattern.POISON_MELEE) {
                    target.poisonRounds = maxOf(target.poisonRounds, 1)
                    target.poisonAppliedRound = state.round
                    target.isHealingBlocked = true
                    target.hasKadruPoison = true
                    newCombatTexts.add(
                        FloatingCombatText(
                            id = System.nanoTime() + 45 + target.slotIndex,
                            slotIndex = target.slotIndex,
                            isPlayer = target.isPlayer,
                            text = "💜☠️",
                            type = CombatTextType.POISON
                        )
                    )
                }
            }
        }

        attacker.hasActedThisRound = true
        if (attacker.weakenedRounds > 0) {
            attacker.weakenedRounds--
            if (attacker.weakenedRounds <= 0) attacker.isWeakened = false
        }
        if (attacker.atkBoostRounds > 0) {
            attacker.atkBoostRounds--
            if (attacker.atkBoostRounds <= 0) {
                attacker.atkBoostRounds = 0
                attacker.atkBoostPercent = 0
            }
        }

        val patternDesc = when (attacker.attackPattern) {
            AttackPattern.PIERCE_LINE -> tr("пронзанием линии", "line pierce")
            AttackPattern.PIERCE_25_75 -> tr("пронзанием корнями", "root pierce")
            AttackPattern.ALL_ENEMIES -> tr("залпом по врагам", "multi-target blast")
            AttackPattern.DIAGONAL_PAIR -> tr("ударом по диагонали", "diagonal strike")
            AttackPattern.FRONT_50_BACK_25 -> tr("волной льда", "ice wave")
            else -> tr("атакой", "attack")
        }

        val attackerName = attacker.localizedName()
        val targetsSummary = targetsToHit.joinToString(", ") { it.localizedName() }
        val damageDesc = if (isSlowMiss) {
            tr("0 ур. (Промах из-за замедления!)", "0 dmg (Missed due to Slow!)")
        } else {
            tr("$totalDamageDealt ур.", "$totalDamageDealt dmg") +
                if (totalShieldAbsorbed > 0) tr(" (щит -$totalShieldAbsorbed)", " (shield -$totalShieldAbsorbed)") else ""
        }
        val killDesc = if (killedTargets.isNotEmpty()) {
            tr(" • 💀 Повержен: ${killedTargets.joinToString(", ")}", " • 💀 Defeated: ${killedTargets.joinToString(", ")}")
        } else ""

        val logText = if (isSlowMiss) {
            tr(
                "$attackerName: Промах по [$targetsSummary] из-за замедления! 💨",
                "$attackerName: Missed [$targetsSummary] due to Slow! 💨"
            )
        } else {
            tr(
                "$attackerName: $patternDesc по [$targetsSummary] на $damageDesc$killDesc",
                "$attackerName: $patternDesc hits [$targetsSummary] for $damageDesc$killDesc"
            )
        }
        val updatedLogs = state.actionLogs + BattleActionLog(System.currentTimeMillis(), logText, isPlayerAttacker)

        val nextPlayerSlot = if (isPlayerAttacker) {
            state.playerCards.firstOrNull { it.isAlive && !it.hasActedThisRound && !it.hasSkipTurnEffect }?.slotIndex
        } else state.selectedPlayerSlot

        val hitTargetSlots = targetsToHit.map { it.slotIndex }.filter { it in aliveTargetSlotsBefore }.distinct()
        val attackAnim = AttackAnimation(
            attackerSlot = attacker.slotIndex,
            isPlayerAttacker = isPlayerAttacker,
            targetSlot = primaryTargetSlot,
            targetSlots = hitTargetSlots,
            isSkill = false
        )

        val newPlayerMana = state.playerTeamMana
        val newEnemyMana = state.enemyTeamMana

        _battleState.update {
            it?.copy(
                activeAttackAnimation = if (silentInstant) null else attackAnim,
                combatTexts = newCombatTexts,
                actionLogs = updatedLogs,
                selectedPlayerSlot = nextPlayerSlot,
                pendingSkillSlot = null,
                playerTeamMana = newPlayerMana,
                enemyTeamMana = newEnemyMana
            )
        }

        if (!silentInstant) {
            viewModelScope.launch {
                delay((350 / (state.battleSpeed)).toLong())
                _battleState.update { it?.copy(activeAttackAnimation = null) }
            }

            checkBattleEndOrNextTurn()
        }
    }

    private fun performSkill(
        attacker: BattleCard,
        isPlayerAttacker: Boolean,
        skillOverride: HeroSkill? = null,
        silentInstant: Boolean = false,
        targetAllySlot: Int? = null
    ) {
        val state = _battleState.value ?: return
        val skill = skillOverride ?: attacker.skill ?: return
        val currentMana = if (isPlayerAttacker) state.playerTeamMana else state.enemyTeamMana
        if (currentMana < skill.energyCost) return

        if (!silentInstant) {
            hapticTrigger?.invoke(true)
        }
        attacker.hasActedThisRound = true
        if (attacker.weakenedRounds > 0) {
            attacker.weakenedRounds--
            if (attacker.weakenedRounds <= 0) attacker.isWeakened = false
        }
        val newPlayerMana = if (isPlayerAttacker) (state.playerTeamMana - skill.energyCost).coerceAtLeast(0) else state.playerTeamMana
        val newEnemyMana = if (!isPlayerAttacker) (state.enemyTeamMana - skill.energyCost).coerceAtLeast(0) else state.enemyTeamMana
        val dmgMultiplier = (if (attacker.isWeakened) 0.5f else 1.0f) * (1.0f + attacker.atkBoostPercent / 100f)

        val allies = if (isPlayerAttacker) state.playerCards.filter { it.isAlive } else state.enemyCards.filter { it.isAlive }
        val enemies = if (isPlayerAttacker) state.enemyCards.filter { it.isAlive } else state.playerCards.filter { it.isAlive }
        val aliveEnemySlotsBefore = enemies.map { it.slotIndex }.toSet()
        val newCombatTexts = mutableListOf<FloatingCombatText>()

        fun healBattleCard(target: BattleCard, healAmount: Int) {
            if (target.isHealingBlocked) {
                newCombatTexts.add(
                    FloatingCombatText(
                        id = System.nanoTime() + 25 + target.slotIndex,
                        slotIndex = target.slotIndex,
                        isPlayer = target.isPlayer,
                        text = "0💛🚫",
                        type = CombatTextType.SKILL
                    )
                )
                return
            }
            val before = target.currentHp
            target.currentHp = (target.currentHp + healAmount).coerceAtMost(target.maxHp)
            val actual = target.currentHp - before
            newCombatTexts.add(
                FloatingCombatText(
                    id = System.nanoTime() + target.slotIndex,
                    slotIndex = target.slotIndex,
                    isPlayer = target.isPlayer,
                    text = "+$actual❤️",
                    type = CombatTextType.HEAL
                )
            )
        }

        fun dealDamageTo(target: BattleCard, damage: Int, isCrit: Boolean = false, poisonDmg: Int = 0, burnDmg: Int = 0): Int {
            var remaining = damage
            if (target.currentArmor > 0) {
                val blocked = target.currentArmor.coerceAtMost(remaining)
                target.currentArmor -= blocked
                remaining -= blocked
                newCombatTexts.add(
                    FloatingCombatText(
                        id = System.nanoTime() + target.slotIndex,
                        slotIndex = target.slotIndex,
                        isPlayer = target.isPlayer,
                        text = "-$blocked🤍",
                        type = CombatTextType.SHIELD_BLOCK
                    )
                )
            }
            if (remaining > 0) {
                target.currentHp = (target.currentHp - remaining).coerceAtLeast(0)
                newCombatTexts.add(
                    FloatingCombatText(
                        id = System.nanoTime() + 10 + target.slotIndex,
                        slotIndex = target.slotIndex,
                        isPlayer = target.isPlayer,
                        text = if (isCrit) "-$remaining!" else "-$remaining",
                        type = if (isCrit) CombatTextType.CRIT_DAMAGE else CombatTextType.DAMAGE
                    )
                )
            }
            if (poisonDmg > 0) {
                target.currentHp = (target.currentHp - poisonDmg).coerceAtLeast(0)
                newCombatTexts.add(
                    FloatingCombatText(
                        id = System.nanoTime() + 20 + target.slotIndex,
                        slotIndex = target.slotIndex,
                        isPlayer = target.isPlayer,
                        text = "-$poisonDmg💜",
                        type = CombatTextType.POISON
                    )
                )
            }
            if (burnDmg > 0) {
                target.currentHp = (target.currentHp - burnDmg).coerceAtLeast(0)
                newCombatTexts.add(
                    FloatingCombatText(
                        id = System.nanoTime() + 30 + target.slotIndex,
                        slotIndex = target.slotIndex,
                        isPlayer = target.isPlayer,
                        text = "-$burnDmg🧡",
                        type = CombatTextType.BURN
                    )
                )
            }
            if (target.currentHp <= 0) target.isAlive = false
            return damage
        }

        when (skill.id) {
            "brun_skill_1" -> {
                // Наносит 100% урона врагу и 25% соседнему врагу на линии; если соседа нет — 100% + 25% врагу спереди
                val preferredColSlot = if (attacker.slotIndex % 2 == 0) 0 else 1
                val primary = enemies.firstOrNull { it.slotIndex == preferredColSlot }
                    ?: enemies.firstOrNull { it.slotIndex in 0..1 }
                    ?: enemies.firstOrNull()
                if (primary != null) {
                    val siblingSlot = when (primary.slotIndex) {
                        0 -> 1
                        1 -> 0
                        2 -> 3
                        3 -> 2
                        else -> -1
                    }
                    val sibling = enemies.firstOrNull { it.slotIndex == siblingSlot }
                    if (sibling != null) {
                        val fullDmg = (attacker.atk * 1.0 * dmgMultiplier).toInt().coerceAtLeast(1)
                        val splashDmg = (attacker.atk * 0.25 * dmgMultiplier).toInt().coerceAtLeast(1)
                        dealDamageTo(primary, fullDmg)
                        dealDamageTo(sibling, splashDmg)
                    } else {
                        val boostedDmg = (attacker.atk * 1.25 * dmgMultiplier).toInt().coerceAtLeast(1)
                        dealDamageTo(primary, boostedDmg, isCrit = true)
                    }
                }
            }
            "olga_skill_1" -> {
                // Плюс 10% от здоровья к броне и замедляет врага перед собой
                val armorBonus = (attacker.maxHp * 0.10).toInt().coerceAtLeast(1)
                attacker.currentArmor += armorBonus
                newCombatTexts.add(FloatingCombatText(System.nanoTime(), attacker.slotIndex, attacker.isPlayer, "+$armorBonus🤍", CombatTextType.SHIELD_BOOST))
                val frontEnemy = enemies.firstOrNull { it.slotIndex in 0..1 } ?: enemies.firstOrNull()
                if (frontEnemy != null) {
                    frontEnemy.isSlowed = true
                    frontEnemy.slowRounds = 1
                    newCombatTexts.add(FloatingCombatText(System.nanoTime() + frontEnemy.slotIndex, frontEnemy.slotIndex, frontEnemy.isPlayer, tr("🐢 Замедление!", "🐢 Slowed!"), CombatTextType.SKILL))
                }
            }
            "alisa_skill_1" -> {
                // Каждому союзнику, в том числе и себе 10% от их максимального здоровья в щит
                allies.forEach { ally ->
                    val shield = (ally.maxHp * 0.10).toInt().coerceAtLeast(1)
                    ally.currentArmor += shield
                    newCombatTexts.add(FloatingCombatText(System.nanoTime() + ally.slotIndex, ally.slotIndex, ally.isPlayer, "+$shield🤍", CombatTextType.SHIELD_BOOST))
                }
            }
            "alisa_skill_2" -> {
                // 150% урон одному врагу
                val target = enemies.firstOrNull { it.slotIndex in 0..1 } ?: enemies.firstOrNull()
                if (target != null) {
                    val dmg = (attacker.atk * 1.5).toInt().coerceAtLeast(1)
                    dealDamageTo(target, dmg, isCrit = true)
                }
            }
            "vlasta_skill_1" -> {
                // Отравляет цель на 2 раунда, снимая 20% от максимального здоровья каждый ход
                val target = enemies.firstOrNull { it.slotIndex in 0..1 } ?: enemies.firstOrNull()
                if (target != null) {
                    target.poisonRounds = 2
                    target.poisonAppliedRound = state.round
                    newCombatTexts.add(FloatingCombatText(System.nanoTime() + target.slotIndex, target.slotIndex, target.isPlayer, tr("💜 Отравление 2р", "💜 Poison 2t"), CombatTextType.SKILL))
                }
            }
            "vlasta_skill_2" -> {
                // Наносит 120% урона цели и отравляет цель на 2 раунда на 10% от максимального здоровья каждый ход
                val target = enemies.firstOrNull { it.slotIndex in 0..1 } ?: enemies.firstOrNull()
                if (target != null) {
                    val directDmg = (attacker.atk * 1.20).toInt().coerceAtLeast(1)
                    dealDamageTo(target, directDmg, isCrit = true)
                    if (target.isAlive) {
                        target.poisonRounds = 2
                        target.poisonAppliedRound = state.round
                        newCombatTexts.add(FloatingCombatText(System.nanoTime() + target.slotIndex + 20, target.slotIndex, target.isPlayer, tr("💜 Отравление 2р", "💜 Poison 2t"), CombatTextType.SKILL))
                    }
                }
            }
            "vlasta_skill_3" -> {
                // Наносит 200% атаки цели, а себя лечит на 50% от уровня урона
                val target = enemies.firstOrNull { it.slotIndex in 0..1 } ?: enemies.firstOrNull()
                if (target != null) {
                    val dmg = (attacker.atk * 2.0).toInt().coerceAtLeast(1)
                    dealDamageTo(target, dmg, isCrit = true)
                    val heal = (dmg * 0.50).toInt().coerceAtLeast(1)
                    healBattleCard(attacker, heal)
                }
            }
            "evanda_skill_1" -> {
                // Добавь щит позади стоящему в размере 20% от ее здоровья
                val backAlly = allies.firstOrNull { it.slotIndex == attacker.slotIndex + 2 }
                    ?: allies.firstOrNull { it.slotIndex in 2..3 }
                    ?: allies.firstOrNull()
                if (backAlly != null) {
                    val shield = (attacker.maxHp * 0.20).toInt().coerceAtLeast(1)
                    backAlly.currentArmor += shield
                    newCombatTexts.add(FloatingCombatText(System.nanoTime() + backAlly.slotIndex, backAlly.slotIndex, backAlly.isPlayer, "+$shield🤍", CombatTextType.SHIELD_BOOST))
                }
            }
            "evanda_skill_2" -> {
                // Добавь щит двум любым персонажам в отряде в размере 15% от их максимального здоровья
                val targets = allies.sortedBy { it.currentArmor }.take(2)
                targets.forEach { target ->
                    val shield = (target.maxHp * 0.15).toInt().coerceAtLeast(1)
                    target.currentArmor += shield
                    newCombatTexts.add(FloatingCombatText(System.nanoTime() + target.slotIndex, target.slotIndex, target.isPlayer, "+$shield🤍", CombatTextType.SHIELD_BOOST))
                }
            }
            "evanda_skill_3" -> {
                // Щит всем в отряде в размере 10% от их максимального здоровья
                allies.forEach { ally ->
                    val shield = (ally.maxHp * 0.10).toInt().coerceAtLeast(1)
                    ally.currentArmor += shield
                    newCombatTexts.add(FloatingCombatText(System.nanoTime() + ally.slotIndex, ally.slotIndex, ally.isPlayer, "+$shield🤍", CombatTextType.SHIELD_BOOST))
                }
            }
            "berta_skill_1" -> {
                // Нанести каждому врагу на второй линии по 50% от своего урона (или 100%, если остался один враг)
                val backEnemies = enemies.filter { it.slotIndex in 2..3 }
                val targets = if (backEnemies.isNotEmpty()) backEnemies else enemies
                val multiplierPct = if (targets.size == 1) 1.0 else 0.50
                targets.forEach { target ->
                    val dmg = (attacker.atk * multiplierPct * dmgMultiplier).toInt().coerceAtLeast(1)
                    dealDamageTo(target, dmg)
                }
            }
            "witch_skill_1" -> {
                // Наносит по 30% урона обоим врагам на второй линии и с вероятностью 50% ослабляет их атаку вдвое на один ход
                val backEnemies = enemies.filter { it.slotIndex in 2..3 }
                val targets = if (backEnemies.isNotEmpty()) backEnemies else enemies
                targets.forEach { target ->
                    val dmg = (attacker.atk * 0.30 * dmgMultiplier).toInt().coerceAtLeast(1)
                    dealDamageTo(target, dmg)
                    val weakenRoll = witchWeakenRollOverride ?: Random.nextInt(100)
                    if (target.isAlive && weakenRoll < 50) {
                        target.isWeakened = true
                        target.weakenedRounds = 1
                        newCombatTexts.add(FloatingCombatText(System.nanoTime() + target.slotIndex + 10, target.slotIndex, target.isPlayer, "🤕 -50%⚔️", CombatTextType.SKILL))
                    }
                }
            }
            "iva_skill_1" -> {
                // Исцеляет одного наиболее раненого союзника на 25% от его максимального здоровья
                val target = allies.minByOrNull { it.currentHp.toFloat() / it.maxHp } ?: allies.firstOrNull()
                if (target != null) {
                    val healAmount = (target.maxHp * 0.25).toInt().coerceAtLeast(1)
                    healBattleCard(target, healAmount)
                }
            }
            "iva_skill_2" -> {
                // Исцеляет двух любых союзников на 20% от их максимального здоровья
                val targets = allies.sortedBy { it.currentHp.toFloat() / it.maxHp }.take(2)
                targets.forEach { target ->
                    val healAmount = (target.maxHp * 0.20).toInt().coerceAtLeast(1)
                    healBattleCard(target, healAmount)
                }
            }
            "isolda_skill_1" -> {
                // Замораживает любого персонажа врага на один раунд (враг пропускает ход, 🥶)
                val target = enemies.firstOrNull { it.slotIndex in 0..1 } ?: enemies.firstOrNull()
                if (target != null) {
                    target.isFrozen = true
                    target.freezeRounds = 1
                    newCombatTexts.add(FloatingCombatText(System.nanoTime() + target.slotIndex, target.slotIndex, target.isPlayer, tr("🥶 Заморозка!", "🥶 Frozen!"), CombatTextType.SKILL))
                }
            }
            "isolda_skill_2" -> {
                // Наносит по 30% урона всем врагам и замедляет их на один раунд на 25%
                enemies.forEach { target ->
                    val dmg = (attacker.atk * 0.30 * dmgMultiplier).toInt().coerceAtLeast(1)
                    dealDamageTo(target, dmg)
                    if (target.isAlive) {
                        target.isSlowed = true
                        target.slowRounds = 1
                        newCombatTexts.add(FloatingCombatText(System.nanoTime() + target.slotIndex + 5, target.slotIndex, target.isPlayer, "🐢 -25%", CombatTextType.SKILL))
                    }
                }
            }
            "mina_skill_1" -> {
                // Ослепляет одного из врагов (враг пропускает следующий ход, 😵💫)
                val target = enemies.firstOrNull { it.slotIndex in 0..1 } ?: enemies.firstOrNull()
                if (target != null) {
                    target.isBlinded = true
                    target.blindRounds = 1
                    newCombatTexts.add(FloatingCombatText(System.nanoTime() + target.slotIndex, target.slotIndex, target.isPlayer, tr("😵💫 Ослеплен!", "😵💫 Blinded!"), CombatTextType.SKILL))
                }
            }
            "mina_skill_2" -> {
                // Нанести всем врагам 60% урона и ожог картам второй линии с уроном в 20% на 2 раунда
                enemies.forEach { target ->
                    val dmg = (attacker.atk * 0.60 * dmgMultiplier).toInt().coerceAtLeast(1)
                    dealDamageTo(target, dmg)
                    if (target.isAlive && target.slotIndex in 2..3) {
                        target.burnRounds = 2
                        target.burnPercent = 20
                        target.burnAppliedRound = state.round
                        newCombatTexts.add(FloatingCombatText(System.nanoTime() + target.slotIndex + 30, target.slotIndex, target.isPlayer, tr("🔥 Ожог 20%", "🔥 Burn 20%"), CombatTextType.SKILL))
                    }
                }
            }
            "dem_skill_1" -> {
                // 125% урон по обоим врагам на первой линии
                val frontEnemies = enemies.filter { it.slotIndex in 0..1 }
                val targets = if (frontEnemies.isNotEmpty()) frontEnemies else enemies.take(2)
                targets.forEach { target ->
                    val dmg = (attacker.atk * 1.25 * dmgMultiplier).toInt().coerceAtLeast(1)
                    dealDamageTo(target, dmg, isCrit = true)
                }
            }
            "dem_skill_2" -> {
                // Приворот: враг пропускает следующий ход (💋) и атакует соседнего союзника на 100% своей атаки
                val frontEnemies = enemies.filter { it.slotIndex in 0..1 }
                val targetEnemy = frontEnemies.firstOrNull() ?: enemies.firstOrNull()
                if (targetEnemy != null) {
                    targetEnemy.isCharmed = true
                    targetEnemy.charmRounds = 1
                    newCombatTexts.add(FloatingCombatText(System.nanoTime() + targetEnemy.slotIndex, targetEnemy.slotIndex, targetEnemy.isPlayer, tr("💋 Приворот!", "💋 Charmed!"), CombatTextType.SKILL))
                    val victimEnemy = frontEnemies.firstOrNull { it.slotIndex != targetEnemy.slotIndex }
                        ?: enemies.firstOrNull { it.slotIndex != targetEnemy.slotIndex }
                    if (victimEnemy != null) {
                        val dmg = (targetEnemy.atk * dmgMultiplier).toInt().coerceAtLeast(1)
                        dealDamageTo(victimEnemy, dmg, isCrit = true)
                    }
                }
            }
            "dem_skill_3" -> {
                // 100% урон по двум врагам на первой линии и 50% по двум врагам на второй линии
                enemies.forEach { target ->
                    val multiplier = if (target.slotIndex in 0..1) 1.0 else 0.50
                    val dmg = (attacker.atk * multiplier * dmgMultiplier).toInt().coerceAtLeast(1)
                    dealDamageTo(target, dmg, isCrit = true)
                }
            }
            "baker_skill_1" -> {
                // 120% урон любому персонажу на первой линии и поджигает его на 2 раунда на 10% от его максимального здоровья каждый раунд
                val target = enemies.firstOrNull { it.slotIndex in 0..1 } ?: enemies.firstOrNull()
                if (target != null) {
                    val directDmg = (attacker.atk * 1.20 * dmgMultiplier).toInt().coerceAtLeast(1)
                    dealDamageTo(target, directDmg, isCrit = true)
                    if (target.isAlive) {
                        target.burnRounds = 2
                        target.burnPercent = 10
                        target.burnAppliedRound = state.round
                        newCombatTexts.add(FloatingCombatText(System.nanoTime() + target.slotIndex + 30, target.slotIndex, target.isPlayer, tr("🔥 Ожог 10%", "🔥 Burn 10%"), CombatTextType.SKILL))
                    }
                }
            }
            "baker_skill_2" -> {
                // 50% урон каждому противнику на первой линии и оглушает одного из них на один раунд
                val frontEnemies = enemies.filter { it.slotIndex in 0..1 }
                val targets = if (frontEnemies.isNotEmpty()) frontEnemies else enemies.take(2)
                targets.forEachIndexed { idx, target ->
                    val dmg = (attacker.atk * 0.50 * dmgMultiplier).toInt().coerceAtLeast(1)
                    dealDamageTo(target, dmg)
                    if (idx == 0 && target.isAlive) {
                        target.isStunned = true
                        target.stunRounds = 1
                        newCombatTexts.add(FloatingCombatText(System.nanoTime() + target.slotIndex + 3, target.slotIndex, target.isPlayer, tr("💫 Оглушен!", "💫 Stunned!"), CombatTextType.SKILL))
                    }
                }
            }
            "baker_skill_3" -> {
                // 150% урона по всей первой линии и поджигает ее на 10% от ее максимального здоровья на 2 раунда каждый ход
                val frontEnemies = enemies.filter { it.slotIndex in 0..1 }
                val targets = if (frontEnemies.isNotEmpty()) frontEnemies else enemies
                targets.forEach { target ->
                    val dmg = (attacker.atk * 1.50 * dmgMultiplier).toInt().coerceAtLeast(1)
                    dealDamageTo(target, dmg, isCrit = true)
                    if (target.isAlive) {
                        target.burnRounds = 2
                        target.burnPercent = 10
                        target.burnAppliedRound = state.round
                        newCombatTexts.add(FloatingCombatText(System.nanoTime() + target.slotIndex + 30, target.slotIndex, target.isPlayer, tr("🔥 Ожог 10%", "🔥 Burn 10%"), CombatTextType.SKILL))
                    }
                }
            }
            "elios_skill_1" -> {
                // Замораживает двух врагов на 1 ход и наносит им урон в размере 50%
                val targets = (enemies.filter { !it.isFrozen } + enemies.filter { it.isFrozen }).take(2)
                targets.forEach { target ->
                    val dmg = (attacker.atk * 0.50 * dmgMultiplier).toInt().coerceAtLeast(1)
                    dealDamageTo(target, dmg)
                    if (target.isAlive) {
                        target.isFrozen = true
                        target.freezeRounds = 1
                        newCombatTexts.add(FloatingCombatText(System.nanoTime() + target.slotIndex + 5, target.slotIndex, target.isPlayer, tr("🥶 Заморозка!", "🥶 Frozen!"), CombatTextType.SKILL))
                    }
                }
            }
            "elios_skill_2" -> {
                // Два любых противника ослаблены на 50%
                val targets = (enemies.filter { !it.isWeakened }.shuffled() + enemies.filter { it.isWeakened }).take(2)
                targets.forEach { target ->
                    target.isWeakened = true
                    target.weakenedRounds = 1
                    newCombatTexts.add(FloatingCombatText(System.nanoTime() + target.slotIndex + 10, target.slotIndex, target.isPlayer, "🤕 -50%⚔️", CombatTextType.SKILL))
                }
            }
            "vasilisa_skill_1" -> {
                // Дополнительно снимает 25% от нанесенного урона с любого врага
                val primary = enemies.firstOrNull()
                if (primary != null) {
                    val primaryDmg = (attacker.atk * 1.0 * dmgMultiplier).toInt().coerceAtLeast(1)
                    dealDamageTo(primary, primaryDmg)
                    val secondary = enemies.filter { it.isAlive && it.slotIndex != primary.slotIndex }.randomOrNull()
                    if (secondary != null) {
                        val extraDmg = (primaryDmg * 0.25).toInt().coerceAtLeast(1)
                        dealDamageTo(secondary, extraDmg)
                    }
                }
            }
            "vasilisa_skill_2" -> {
                // С вероятностью 35% наносит критический урон, в ином случае обычный
                val target = enemies.firstOrNull()
                if (target != null) {
                    val critRoll = vasilisaCritRollOverride ?: Random.nextInt(100)
                    val isSkillCrit = critRoll < 35
                    val critMult = if (isSkillCrit) 1.5 else 1.0
                    val dmg = (attacker.atk * critMult * dmgMultiplier).toInt().coerceAtLeast(1)
                    dealDamageTo(target, dmg, isCrit = isSkillCrit)
                }
            }
            "vasilisa_skill_3" -> {
                // Наносит дополнительно 100% второму врагу по диагонали
                fun diagonalSlot(slot: Int): Int = when (slot) {
                    0 -> 3
                    1 -> 2
                    2 -> 1
                    3 -> 0
                    else -> -1
                }
                val primary = enemies.firstOrNull { candidate ->
                    val diag = diagonalSlot(candidate.slotIndex)
                    enemies.any { it.slotIndex == diag && it.isAlive }
                } ?: enemies.firstOrNull()
                if (primary != null) {
                    val dmg = (attacker.atk * 1.0 * dmgMultiplier).toInt().coerceAtLeast(1)
                    dealDamageTo(primary, dmg)
                    val diagSlot = diagonalSlot(primary.slotIndex)
                    val diagTarget = enemies.firstOrNull { it.slotIndex == diagSlot && it.isAlive }
                    if (diagTarget != null) {
                        dealDamageTo(diagTarget, dmg, isCrit = true)
                    }
                }
            }
            "kadru_skill_1" -> {
                // Ярость: Увеличивает силу атаки на 30% на следующий ход
                attacker.atkBoostPercent = 30
                attacker.atkBoostRounds = 1
                newCombatTexts.add(
                    FloatingCombatText(
                        id = System.nanoTime() + attacker.slotIndex,
                        slotIndex = attacker.slotIndex,
                        isPlayer = attacker.isPlayer,
                        text = "+30%⚔️",
                        type = CombatTextType.SKILL
                    )
                )
            }
            "kadru_skill_2" -> {
                // Ядовитая лужа: Поражает всех врагов ядом. Яд снимет 50% от урона карты на 1 сл ход.
                val poisonDmg = (attacker.atk * 0.50 * dmgMultiplier).toInt().coerceAtLeast(1)
                enemies.forEach { target ->
                    target.poisonRounds = 1
                    target.poisonAppliedRound = state.round
                    target.poisonFixedDamage = poisonDmg
                    target.isHealingBlocked = true
                    target.hasKadruPoison = true
                    newCombatTexts.add(
                        FloatingCombatText(
                            id = System.nanoTime() + target.slotIndex + 40,
                            slotIndex = target.slotIndex,
                            isPlayer = target.isPlayer,
                            text = tr("💜 Яд -$poisonDmg", "💜 Poison -$poisonDmg"),
                            type = CombatTextType.SKILL
                        )
                    )
                }
            }
            "leya_skill_1" -> {
                // Усиленный корень: Повышает атаку выбранного союзника (кроме себя) на 25% на следующий ход
                val targetAlly = targetAllySlot?.let { slot ->
                    allies.firstOrNull { it.slotIndex == slot && it.slotIndex != attacker.slotIndex }
                } ?: findBestAutoAllyTarget(attacker, allies, "leya_skill_1")
                if (targetAlly != null) {
                    targetAlly.atkBoostPercent = maxOf(targetAlly.atkBoostPercent, 25)
                    targetAlly.atkBoostRounds = maxOf(targetAlly.atkBoostRounds, 1)
                    newCombatTexts.add(
                        FloatingCombatText(
                            id = System.nanoTime() + targetAlly.slotIndex + 50,
                            slotIndex = targetAlly.slotIndex,
                            isPlayer = targetAlly.isPlayer,
                            text = "+25%⚔️",
                            type = CombatTextType.SKILL
                        )
                    )
                }
            }
            "leya_skill_2" -> {
                // Очищение: Снимает с выбранного союзника (кроме себя) негативные эффекты (эффект Кадру не снимается!)
                val targetAlly = targetAllySlot?.let { slot ->
                    allies.firstOrNull { it.slotIndex == slot && it.slotIndex != attacker.slotIndex }
                } ?: findBestAutoAllyTarget(attacker, allies, "leya_skill_2")
                    ?: allies.firstOrNull { it.slotIndex != attacker.slotIndex }
                if (targetAlly != null) {
                    val hadCleansable = hasCleansableNegativeEffect(targetAlly)
                    targetAlly.isFrozen = false
                    targetAlly.freezeRounds = 0
                    targetAlly.isStunned = false
                    targetAlly.stunRounds = 0
                    targetAlly.isBlinded = false
                    targetAlly.blindRounds = 0
                    targetAlly.isCharmed = false
                    targetAlly.charmRounds = 0
                    targetAlly.isWeakened = false
                    targetAlly.weakenedRounds = 0
                    targetAlly.isSlowed = false
                    targetAlly.slowRounds = 0
                    targetAlly.burnRounds = 0
                    targetAlly.burnPercent = 0
                    targetAlly.burnAppliedRound = 0
                    if (!targetAlly.hasKadruPoison) {
                        targetAlly.poisonRounds = 0
                        targetAlly.poisonFixedDamage = 0
                        targetAlly.poisonAppliedRound = 0
                    }
                    // targetAlly.isHealingBlocked and hasKadruPoison remain untouched!
                    newCombatTexts.add(
                        FloatingCombatText(
                            id = System.nanoTime() + targetAlly.slotIndex + 55,
                            slotIndex = targetAlly.slotIndex,
                            isPlayer = targetAlly.isPlayer,
                            text = when {
                                hadCleansable -> tr("🌿 Очищена!", "🌿 Purified!")
                                targetAlly.isHealingBlocked -> tr("💛 Не снять", "💛 Permanent")
                                else -> tr("🌿 Свет", "🌿 Cleansed")
                            },
                            type = CombatTextType.SKILL
                        )
                    )
                }
            }
            else -> {
                when (skill.skillType) {
                    SkillType.SUPPORT -> {
                        val healAmount = (attacker.atk * 0.9).toInt().coerceAtLeast(10)
                        allies.forEach { ally ->
                            healBattleCard(ally, healAmount)
                        }
                    }
                    SkillType.DEFENSIVE -> {
                        val shieldAmount = (attacker.atk * 0.7).toInt().coerceAtLeast(6)
                        allies.forEach { ally ->
                            if (ally.heroType == HeroType.MELEE) {
                                ally.currentArmor = (ally.currentArmor + shieldAmount).coerceAtMost(ally.maxArmor + 10)
                                newCombatTexts.add(
                                    FloatingCombatText(
                                        id = System.nanoTime() + ally.slotIndex,
                                        slotIndex = ally.slotIndex,
                                        isPlayer = ally.isPlayer,
                                        text = "+$shieldAmount🤍",
                                        type = CombatTextType.SHIELD_BOOST
                                    )
                                )
                            }
                        }
                    }
                    SkillType.OFFENSIVE -> {
                        val aoeDmg = (attacker.atk * 1.3 * dmgMultiplier).toInt().coerceAtLeast(1)
                        enemies.take(2).forEach { enemy ->
                            dealDamageTo(enemy, aoeDmg, isCrit = true)
                        }
                    }
                }
            }
        }

        if (skill.id != "kadru_skill_1" && attacker.atkBoostRounds > 0) {
            attacker.atkBoostRounds--
            if (attacker.atkBoostRounds <= 0) {
                attacker.atkBoostRounds = 0
                attacker.atkBoostPercent = 0
            }
        }

        val logText = tr(
            "${attacker.name} применяет способность [${skill.name}]!",
            "${attacker.localizedName()} casts [${skill.localizedName()}]!"
        )
        val updatedLogs = state.actionLogs + BattleActionLog(System.currentTimeMillis(), logText, isPlayerAttacker)

        val nextPlayerSlot = if (isPlayerAttacker) {
            state.playerCards.firstOrNull { it.isAlive && !it.hasActedThisRound && !it.hasSkipTurnEffect }?.slotIndex
        } else state.selectedPlayerSlot

        val hitSlots = newCombatTexts
            .filter { it.isPlayer != isPlayerAttacker && it.slotIndex in aliveEnemySlotsBefore }
            .map { it.slotIndex }
            .distinct()
        val skillAnim = AttackAnimation(
            attackerSlot = attacker.slotIndex,
            isPlayerAttacker = isPlayerAttacker,
            targetSlot = hitSlots.firstOrNull() ?: -1,
            targetSlots = hitSlots,
            isSkill = true
        )

        val updatedSkillsUsed = if (isPlayerAttacker) {
            state.playerSkillsUsedCount + 1
        } else {
            state.playerSkillsUsedCount
        }

        _battleState.update {
            it?.copy(
                activeAttackAnimation = if (silentInstant) null else skillAnim,
                combatTexts = newCombatTexts,
                actionLogs = updatedLogs,
                selectedPlayerSlot = nextPlayerSlot,
                pendingSkillSlot = null,
                playerTeamMana = newPlayerMana,
                enemyTeamMana = newEnemyMana,
                playerSkillsUsedCount = updatedSkillsUsed
            )
        }

        if (!silentInstant) {
            viewModelScope.launch {
                delay((450 / (state.battleSpeed)).toLong())
                _battleState.update { it?.copy(activeAttackAnimation = null) }
            }

            checkBattleEndOrNextTurn()
        }
    }

    private fun checkBattleEndOrNextTurn() {
        val state = _battleState.value ?: return
        if (state.phase == BattlePhase.VICTORY || state.phase == BattlePhase.DEFEAT) return

        val playerDead = state.playerCards.none { it.isAlive }
        val enemyDead = state.enemyCards.none { it.isAlive }

        if (enemyDead) {
            enemyTurnJob?.cancel()
            autoBattleJob?.cancel()
            val victorySpeaker = state.playerCards.filter { it.isAlive }.randomOrNull()
                ?: state.playerCards.firstOrNull()
            val vQuote = victorySpeaker?.let { pickHeroBattleQuote(it, QuoteContext.VICTORY) }
            _battleState.update { current ->
                if (current == null) null
                else current.copy(
                    phase = BattlePhase.VICTORY,
                    dotHitTargets = emptySet(),
                    isProcessingRoundEffects = false,
                    victoryQuote = vQuote,
                    actionLogs = if (vQuote != null) {
                        current.actionLogs + BattleActionLog(
                            id = System.currentTimeMillis(),
                            text = "🏆 ${vQuote.heroName}: «${vQuote.text}»",
                            isPlayerAction = true
                        )
                    } else current.actionLogs
                )
            }
            if (!hasReportedBattleEnd) {
                hasReportedBattleEnd = true
                val endState = _battleState.value ?: state.copy(phase = BattlePhase.VICTORY, victoryQuote = vQuote)
                onBattleEndCallback?.invoke(true, endState)
            }
            return
        }
        if (playerDead) {
            enemyTurnJob?.cancel()
            autoBattleJob?.cancel()
            _battleState.update { it?.copy(phase = BattlePhase.DEFEAT, dotHitTargets = emptySet(), isProcessingRoundEffects = false) }
            if (!hasReportedBattleEnd) {
                hasReportedBattleEnd = true
                val endState = _battleState.value ?: state.copy(phase = BattlePhase.DEFEAT)
                onBattleEndCallback?.invoke(false, endState)
            }
            return
        }

        val playerCanAct = state.playerCards.any { it.isAlive && !it.hasActedThisRound && !it.hasSkipTurnEffect }
        if (!playerCanAct && state.phase == BattlePhase.PLAYER_TURN) {
            // Player's turn ends: decrement skip-turn counters on player cards that skipped this turn
            state.playerCards.forEach { card ->
                if (card.isAlive && card.hasSkipTurnEffect) {
                    decrementSkipTurnStatuses(card)
                }
            }
            _battleState.update { current ->
                if (current == null) null
                else if (!current.isPlayerFirstTurn) {
                    current.playerCards.forEach { it.hasActedThisRound = false }
                    current.enemyCards.forEach { it.hasActedThisRound = false }
                    current.copy(
                        round = current.round + 1,
                        phase = BattlePhase.ENEMY_TURN,
                        playerTeamMana = (current.playerTeamMana + 1).coerceAtMost(current.playerMaxMana),
                        enemyTeamMana = (current.enemyTeamMana + 1).coerceAtMost(current.enemyMaxMana),
                        selectedPlayerSlot = null
                    )
                } else {
                    current.copy(
                        phase = BattlePhase.ENEMY_TURN,
                        selectedPlayerSlot = null
                    )
                }
            }
            executeEnemyTurn()
        }
    }

    private fun executeEnemyTurn() {
        enemyTurnJob?.cancel()
        enemyTurnJob = viewModelScope.launch {
            val speedDelay = (900 / (_battleState.value?.battleSpeed ?: 1f)).toLong()
            delay(speedDelay)

            val initialState = _battleState.value ?: return@launch
            if (initialState.isBattleOver() || initialState.phase == BattlePhase.DEFEAT || initialState.phase == BattlePhase.VICTORY) return@launch

            // Pre-turn Poison & Burn damage animations for enemy cards
            val enemyDotResult = processTeamDotEffects(initialState.enemyCards, initialState.round, isPlayerTeam = false)
            if (enemyDotResult.texts.isNotEmpty() || enemyDotResult.hitTargets.isNotEmpty()) {
                _battleState.update {
                    it?.copy(
                        combatTexts = enemyDotResult.texts,
                        dotHitTargets = enemyDotResult.hitTargets,
                        actionLogs = it.actionLogs + enemyDotResult.logs
                    )
                }
                delay((650 / (_battleState.value?.battleSpeed ?: 1f)).toLong())
                _battleState.update { it?.copy(dotHitTargets = emptySet()) }
                if (_battleState.value?.enemyCards?.none { it.isAlive } == true) {
                    checkBattleEndOrNextTurn()
                    return@launch
                }
            }

            val state = _battleState.value ?: return@launch
            val aliveEnemies = state.enemyCards.filter { it.isAlive }

            for (enemy in aliveEnemies) {
                val currentState = _battleState.value ?: break
                if (currentState.isBattleOver() || currentState.phase == BattlePhase.DEFEAT || currentState.phase == BattlePhase.VICTORY) break
                if (currentState.playerCards.none { it.isAlive }) break
                if (!enemy.isAlive || enemy.hasActedThisRound) continue

                // Skip-turn check: Frozen / Blinded / Charmed / Stunned enemy cards skip this turn
                if (enemy.hasSkipTurnEffect) {
                    val logText = skipTurnMessage(enemy)
                    _battleState.update { it?.copy(actionLogs = it.actionLogs + BattleActionLog(System.currentTimeMillis(), logText, false)) }
                    delay((550 / (_battleState.value?.battleSpeed ?: 1f)).toLong())
                    enemy.hasActedThisRound = true
                    decrementSkipTurnStatuses(enemy)
                    _battleState.update { it?.copy() }
                    continue
                }

                val validTargets = currentState.playerCards.filter { playerCard ->
                    playerCard.isAlive && canCardTargetSlot(enemy, playerCard.slotIndex, currentState.playerCards)
                }

                if (validTargets.isNotEmpty()) {
                    val target = if (currentState.campaignChapter >= 3) {
                        // Начиная с 3 главы бот добивает сначала слабые карты игрока
                        validTargets.minByOrNull { it.currentHp + it.currentArmor } ?: validTargets.first()
                    } else {
                        validTargets.random()
                    }
                    val enemyAllies = currentState.enemyCards.filter { it.isAlive }
                    val affordableSkill = selectAutoSkillForCard(enemy, enemyAllies, currentState.enemyTeamMana)
                    if (affordableSkill != null) {
                        val autoAllyTarget = if (skillRequiresAllyTarget(affordableSkill.id)) {
                            findBestAutoAllyTarget(enemy, enemyAllies, affordableSkill.id)?.slotIndex
                        } else null
                        performSkill(enemy, isPlayerAttacker = false, skillOverride = affordableSkill, targetAllySlot = autoAllyTarget)
                    } else {
                        performAttackWithPattern(enemy, target.slotIndex, isPlayerAttacker = false)
                    }
                    delay(speedDelay)
                }
            }

            _battleState.update { current ->
                if (current == null) null
                else if (current.isBattleOver() || current.phase == BattlePhase.DEFEAT || current.phase == BattlePhase.VICTORY) current
                else if (current.isPlayerFirstTurn) {
                    current.playerCards.forEach { it.hasActedThisRound = false }
                    current.enemyCards.forEach { it.hasActedThisRound = false }
                    current.copy(
                        round = current.round + 1,
                        phase = BattlePhase.PLAYER_TURN,
                        isProcessingRoundEffects = true,
                        playerTeamMana = (current.playerTeamMana + 1).coerceAtMost(current.playerMaxMana),
                        enemyTeamMana = (current.enemyTeamMana + 1).coerceAtMost(current.enemyMaxMana),
                        selectedPlayerSlot = null
                    )
                } else {
                    current.playerCards.forEach { it.hasActedThisRound = false }
                    current.enemyCards.forEach { it.hasActedThisRound = false }
                    current.copy(
                        phase = BattlePhase.PLAYER_TURN,
                        isProcessingRoundEffects = true,
                        selectedPlayerSlot = null
                    )
                }
            }

            beginPlayerTurnWithRoundEffects()
        }
    }

    private suspend fun beginPlayerTurnWithRoundEffects() {
        val state = _battleState.value ?: return
        if (state.isBattleOver() || state.phase != BattlePhase.PLAYER_TURN) return

        // 1. Play DOT damage animation & apply HP reduction BEFORE player can pick cards to move
        val playerDotResult = processTeamDotEffects(state.playerCards, state.round, isPlayerTeam = true)
        if (playerDotResult.texts.isNotEmpty() || playerDotResult.hitTargets.isNotEmpty()) {
            _battleState.update {
                it?.copy(
                    combatTexts = playerDotResult.texts,
                    dotHitTargets = playerDotResult.hitTargets,
                    actionLogs = it.actionLogs + playerDotResult.logs
                )
            }
            delay((650 / (_battleState.value?.battleSpeed ?: 1f)).toLong())
            _battleState.update { it?.copy(dotHitTargets = emptySet()) }

            if (_battleState.value?.playerCards?.none { it.isAlive } == true) {
                checkBattleEndOrNextTurn()
                return
            }
        }

        // 2. Allow player to select active cards (cards with skip-turn effect remain inactive!)
        val updatedState = _battleState.value ?: return
        val firstActiveSlot = updatedState.playerCards.firstOrNull {
            it.isAlive && !it.hasActedThisRound && !it.hasSkipTurnEffect
        }?.slotIndex

        _battleState.update {
            it?.copy(
                isProcessingRoundEffects = false,
                selectedPlayerSlot = firstActiveSlot
            )
        }

        // 3. If ALL alive player cards have skip-turn effects, log and automatically end player's turn after brief pause
        if (firstActiveSlot == null && updatedState.playerCards.any { it.isAlive }) {
            val skipLogs = updatedState.playerCards.filter { it.isAlive && it.hasSkipTurnEffect }.map { card ->
                BattleActionLog(System.nanoTime() + card.slotIndex, skipTurnMessage(card), true)
            }
            _battleState.update { it?.copy(actionLogs = it.actionLogs + skipLogs) }
            delay((750 / (_battleState.value?.battleSpeed ?: 1f)).toLong())
            checkBattleEndOrNextTurn()
            return
        }

        if (_battleState.value?.isAutoBattle == true && _battleState.value?.phase == BattlePhase.PLAYER_TURN) {
            runAutoBattleStep()
        }
    }

    fun toggleAutoBattle() {
        val isNowAuto = !(_battleState.value?.isAutoBattle ?: false)
        _battleState.update { it?.copy(isAutoBattle = isNowAuto) }
        if (isNowAuto && _battleState.value?.phase == BattlePhase.PLAYER_TURN && _battleState.value?.isProcessingRoundEffects == false) {
            runAutoBattleStep()
        }
    }

    fun setBattleSpeed(speed: Float) {
        _battleState.update { it?.copy(battleSpeed = speed) }
    }

    private fun runAutoBattleStep() {
        autoBattleJob?.cancel()
        autoBattleJob = viewModelScope.launch {
            while (_battleState.value?.isAutoBattle == true && _battleState.value?.phase == BattlePhase.PLAYER_TURN) {
                val state = _battleState.value ?: break
                if (state.isBattleOver() || state.phase == BattlePhase.DEFEAT || state.phase == BattlePhase.VICTORY) break
                if (state.isProcessingRoundEffects) {
                    delay(100)
                    continue
                }

                val readyCard = state.playerCards.firstOrNull { it.isAlive && !it.hasActedThisRound && !it.hasSkipTurnEffect } ?: break
                _battleState.update { it?.copy(selectedPlayerSlot = readyCard.slotIndex) }

                val speedDelay = (600 / (state.battleSpeed)).toLong()
                delay(speedDelay)

                val autoSkill = selectAutoSkillForCard(readyCard, state.playerCards, state.playerTeamMana)
                if (autoSkill != null && readyCard.skill?.id == autoSkill.id) {
                    val autoAllyTarget = if (skillRequiresAllyTarget(autoSkill.id)) {
                        findBestAutoAllyTarget(readyCard, state.playerCards, autoSkill.id)?.slotIndex
                    } else null
                    executePlayerSkill(targetAllySlot = autoAllyTarget)
                } else {
                    val validTargets = state.enemyCards.filter { enemy ->
                        enemy.isAlive && canCardTargetSlot(readyCard, enemy.slotIndex, state.enemyCards)
                    }
                    if (validTargets.isNotEmpty()) {
                        executePlayerAttack(validTargets.random().slotIndex)
                    }
                }
                delay(speedDelay)
            }
        }
    }

    /**
     * Simulates an entire Career battle synchronously in the background using the player's actual deck
     * and stage enemies, then immediately publishes the VICTORY or DEFEAT outcome state.
     */
    fun runInstantAutoBattle(
        playerCards: List<BattleCard>,
        enemyCards: List<BattleCard>,
        enemyName: String,
        enemyTitle: String,
        rewardGold: Int,
        rewardGems: Int,
        rewardExp: Int,
        rewardArtifact: Artifact? = null,
        campaignChapter: Int = 1,
        battleBgRes: Int = com.example.R.drawable.bg_brunhilde,
        foundAmulet: DiscoveredAmulet? = null,
        victoryEventMessage: String? = null,
        campaignStageId: Int? = null,
        initialPlayerMana: Int = 1,
        initialEnemyMana: Int = 1,
        isBoss: Boolean = false,
        onBattleEnd: (isVictory: Boolean, state: BattleState) -> Unit
    ) {
        enemyTurnJob?.cancel()
        autoBattleJob?.cancel()
        this.onBattleEndCallback = onBattleEnd
        this.hasReportedBattleEnd = false

        _battleState.value = BattleState(
            round = 1,
            phase = BattlePhase.PLAYER_TURN,
            playerCards = playerCards,
            enemyCards = enemyCards,
            selectedPlayerSlot = null,
            enemyName = enemyName,
            enemyTitle = enemyTitle,
            isArena = false,
            campaignChapter = campaignChapter,
            rewardGold = rewardGold,
            rewardGems = rewardGems,
            rewardExp = rewardExp,
            rewardArtifact = rewardArtifact,
            battleBgRes = battleBgRes,
            playerTeamMana = initialPlayerMana,
            enemyTeamMana = initialEnemyMana,
            actionLogs = listOf(
                BattleActionLog(
                    id = System.currentTimeMillis(),
                    text = tr("⚡ Автобой Карьеры запущен!", "⚡ Career Auto-Battle started!"),
                    isPlayerAction = true
                )
            ),
            foundAmulet = foundAmulet,
            victoryEventMessage = victoryEventMessage,
            showArenaFirstTurnModal = false,
            isPlayerFirstTurn = true,
            campaignStageId = campaignStageId,
            isBoss = isBoss,
            isAutoBattle = true
        )

        val maxRounds = 100
        while (true) {
            val state = _battleState.value ?: break
            if (state.enemyCards.none { it.isAlive } || state.playerCards.none { it.isAlive }) break
            if (state.round > maxRounds) break

            // 1. Player Team Turn
            for (card in state.playerCards) {
                val cur = _battleState.value ?: break
                if (cur.enemyCards.none { it.isAlive } || cur.playerCards.none { it.isAlive }) break
                if (!card.isAlive || card.hasActedThisRound) continue

                if (card.hasSkipTurnEffect) {
                    val msg = skipTurnMessage(card)
                    _battleState.update { it?.copy(actionLogs = it.actionLogs + BattleActionLog(System.nanoTime(), msg, true)) }
                    card.hasActedThisRound = true
                    decrementSkipTurnStatuses(card)
                    continue
                }

                val affordableSkill = selectAutoSkillForCard(card, cur.playerCards, cur.playerTeamMana)
                if (affordableSkill != null) {
                    val autoAllyTarget = if (skillRequiresAllyTarget(affordableSkill.id)) {
                        findBestAutoAllyTarget(card, cur.playerCards, affordableSkill.id)?.slotIndex
                    } else null
                    performSkill(
                        card,
                        isPlayerAttacker = true,
                        skillOverride = affordableSkill,
                        silentInstant = true,
                        targetAllySlot = autoAllyTarget
                    )
                } else {
                    val validTargets = cur.enemyCards.filter { enemy ->
                        enemy.isAlive && canCardTargetSlot(card, enemy.slotIndex, cur.enemyCards)
                    }
                    val target = validTargets.minByOrNull { it.currentHp + it.currentArmor } ?: validTargets.firstOrNull()
                    if (target != null) {
                        performAttackWithPattern(card, target.slotIndex, isPlayerAttacker = true, silentInstant = true)
                    } else {
                        card.hasActedThisRound = true
                    }
                }
            }

            val afterPlayerTurn = _battleState.value ?: break
            if (afterPlayerTurn.enemyCards.none { it.isAlive } || afterPlayerTurn.playerCards.none { it.isAlive }) break

            // 2. Enemy Team Pre-Turn DOTs & Enemy Team Turn
            val enemyDot = processTeamDotEffects(afterPlayerTurn.enemyCards, afterPlayerTurn.round, isPlayerTeam = false)
            if (enemyDot.logs.isNotEmpty()) {
                _battleState.update { it?.copy(actionLogs = it.actionLogs + enemyDot.logs) }
            }
            if ((_battleState.value?.enemyCards?.none { it.isAlive }) == true) break

            val enemyTurnState = _battleState.value ?: break
            for (enemy in enemyTurnState.enemyCards) {
                val cur = _battleState.value ?: break
                if (cur.playerCards.none { it.isAlive } || cur.enemyCards.none { it.isAlive }) break
                if (!enemy.isAlive || enemy.hasActedThisRound) continue

                if (enemy.hasSkipTurnEffect) {
                    val msg = skipTurnMessage(enemy)
                    _battleState.update { it?.copy(actionLogs = it.actionLogs + BattleActionLog(System.nanoTime(), msg, false)) }
                    enemy.hasActedThisRound = true
                    decrementSkipTurnStatuses(enemy)
                    continue
                }

                val validTargets = cur.playerCards.filter { pc ->
                    pc.isAlive && canCardTargetSlot(enemy, pc.slotIndex, cur.playerCards)
                }
                if (validTargets.isNotEmpty()) {
                    val target = if (cur.campaignChapter >= 3) {
                        validTargets.minByOrNull { it.currentHp + it.currentArmor } ?: validTargets.first()
                    } else {
                        validTargets.random()
                    }
                    val enemyAllies = cur.enemyCards.filter { it.isAlive }
                    val affordableSkill = selectAutoSkillForCard(enemy, enemyAllies, cur.enemyTeamMana)
                    if (affordableSkill != null) {
                        val autoAllyTarget = if (skillRequiresAllyTarget(affordableSkill.id)) {
                            findBestAutoAllyTarget(enemy, enemyAllies, affordableSkill.id)?.slotIndex
                        } else null
                        performSkill(
                            enemy,
                            isPlayerAttacker = false,
                            skillOverride = affordableSkill,
                            silentInstant = true,
                            targetAllySlot = autoAllyTarget
                        )
                    } else {
                        performAttackWithPattern(enemy, target.slotIndex, isPlayerAttacker = false, silentInstant = true)
                    }
                } else {
                    enemy.hasActedThisRound = true
                }
            }

            val afterEnemyTurn = _battleState.value ?: break
            if (afterEnemyTurn.enemyCards.none { it.isAlive } || afterEnemyTurn.playerCards.none { it.isAlive }) break

            // 3. Advance Round & Apply Start-of-Round Player DOTs
            afterEnemyTurn.playerCards.forEach { it.hasActedThisRound = false }
            afterEnemyTurn.enemyCards.forEach { it.hasActedThisRound = false }
            val nextRound = afterEnemyTurn.round + 1
            _battleState.update { current ->
                current?.copy(
                    round = nextRound,
                    phase = BattlePhase.PLAYER_TURN,
                    playerTeamMana = (current.playerTeamMana + 1).coerceAtMost(current.playerMaxMana),
                    enemyTeamMana = (current.enemyTeamMana + 1).coerceAtMost(current.enemyMaxMana),
                    selectedPlayerSlot = null
                )
            }

            val nextRoundState = _battleState.value ?: break
            val playerDot = processTeamDotEffects(nextRoundState.playerCards, nextRound, isPlayerTeam = true)
            if (playerDot.logs.isNotEmpty()) {
                _battleState.update { it?.copy(actionLogs = it.actionLogs + playerDot.logs) }
            }
            if ((_battleState.value?.playerCards?.none { it.isAlive }) == true) break
        }

        val finalRaw = _battleState.value ?: return
        val isVictory = finalRaw.enemyCards.none { it.isAlive } && finalRaw.playerCards.any { it.isAlive }
        val vQuote = if (isVictory) {
            val speaker = finalRaw.playerCards.filter { it.isAlive }.randomOrNull()
                ?: finalRaw.playerCards.firstOrNull()
            speaker?.let { pickHeroBattleQuote(it, QuoteContext.VICTORY) }
        } else null

        _battleState.update { current ->
            current?.copy(
                phase = if (isVictory) BattlePhase.VICTORY else BattlePhase.DEFEAT,
                activeAttackAnimation = null,
                dotHitTargets = emptySet(),
                isProcessingRoundEffects = false,
                selectedPlayerSlot = null,
                victoryQuote = vQuote,
                actionLogs = if (vQuote != null) {
                    current.actionLogs + BattleActionLog(
                        id = System.currentTimeMillis(),
                        text = "🏆 ${vQuote.heroName}: «${vQuote.text}»",
                        isPlayerAction = true
                    )
                } else current.actionLogs
            )
        }

        if (!hasReportedBattleEnd) {
            hasReportedBattleEnd = true
            val resolvedState = _battleState.value ?: return
            onBattleEndCallback?.invoke(isVictory, resolvedState)
        }
    }

    fun surrenderBattle() {
        enemyTurnJob?.cancel()
        autoBattleJob?.cancel()
        val state = _battleState.value ?: return
        if (!hasReportedBattleEnd) {
            hasReportedBattleEnd = true
            onBattleEndCallback?.invoke(false, state)
        }
        _battleState.value = null
    }

    fun exitBattle() {
        enemyTurnJob?.cancel()
        autoBattleJob?.cancel()
        _battleState.value = null
    }
}
