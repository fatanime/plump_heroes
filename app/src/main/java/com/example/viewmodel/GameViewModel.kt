package com.example.viewmodel

import android.app.Application
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GameDataRepository
import com.example.i18n.*
import com.example.model.*
import com.example.network.AppUpdateCheckOutcome
import com.example.network.AppUpdateChecker
import com.example.network.RemoteAppVersionInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GameDataRepository(application)
    private val vibrator = application.getSystemService(Vibrator::class.java)

    // Dedicated ViewModel for the 2x2 turn-based battle logic
    val battleViewModel = BattleViewModel()
    val battleState: StateFlow<BattleState?> = battleViewModel.battleState

    private val _playerProfile = MutableStateFlow(PlayerProfile())
    val playerProfile: StateFlow<PlayerProfile> = _playerProfile.asStateFlow()

    private val _allHeroes = MutableStateFlow<List<HeroCard>>(emptyList())
    val allHeroes: StateFlow<List<HeroCard>> = _allHeroes.asStateFlow()

    private val _inventoryArtifacts = MutableStateFlow<List<Artifact>>(emptyList())
    val inventoryArtifacts: StateFlow<List<Artifact>> = _inventoryArtifacts.asStateFlow()

    private val _campaignChapters = MutableStateFlow<List<CampaignChapter>>(emptyList())
    val campaignChapters: StateFlow<List<CampaignChapter>> = _campaignChapters.asStateFlow()

    private val _arenaBots = MutableStateFlow<List<ArenaBot>>(emptyList())
    val arenaBots: StateFlow<List<ArenaBot>> = _arenaBots.asStateFlow()

    private val _inspectedHero = MutableStateFlow<HeroCard?>(null)
    val inspectedHero: StateFlow<HeroCard?> = _inspectedHero.asStateFlow()

    private val _forgeSelectedArtifactIds = MutableStateFlow<List<String>>(emptyList())
    val forgeSelectedArtifactIds: StateFlow<List<String>> = _forgeSelectedArtifactIds.asStateFlow()

    private val _latestDropMessage = MutableStateFlow<String?>(null)
    val latestDropMessage: StateFlow<String?> = _latestDropMessage.asStateFlow()

    private val _dailyBonusReward = MutableStateFlow<DailyBonusReward?>(null)
    val dailyBonusReward: StateFlow<DailyBonusReward?> = _dailyBonusReward.asStateFlow()

    private val _fortuneWheelOffer = MutableStateFlow<FortuneWheelOffer?>(null)
    val fortuneWheelOffer: StateFlow<FortuneWheelOffer?> = _fortuneWheelOffer.asStateFlow()

    private val _fortuneWheelResult = MutableStateFlow<FortuneWheelResult?>(null)
    val fortuneWheelResult: StateFlow<FortuneWheelResult?> = _fortuneWheelResult.asStateFlow()

    private val _selectedCampaignChapterId = MutableStateFlow(1)
    val selectedCampaignChapterId: StateFlow<Int> = _selectedCampaignChapterId.asStateFlow()

    private val _isCampaign100Percent = MutableStateFlow(false)
    val isCampaign100Percent: StateFlow<Boolean> = _isCampaign100Percent.asStateFlow()

    private val _showCampaign100AchievementDialog = MutableStateFlow(false)
    val showCampaign100AchievementDialog: StateFlow<Boolean> = _showCampaign100AchievementDialog.asStateFlow()

    private val _dailyQuests = MutableStateFlow<List<DailyQuestState>>(emptyList())
    val dailyQuests: StateFlow<List<DailyQuestState>> = _dailyQuests.asStateFlow()

    private val _chestOpeningResult = MutableStateFlow<ChestOpeningResult?>(null)
    val chestOpeningResult: StateFlow<ChestOpeningResult?> = _chestOpeningResult.asStateFlow()

    private val _secretHeroUnlockedPopup = MutableStateFlow<HeroCard?>(null)
    val secretHeroUnlockedPopup: StateFlow<HeroCard?> = _secretHeroUnlockedPopup.asStateFlow()

    private val _levelUpRewardPopup = MutableStateFlow<LevelUpRewardPopup?>(null)
    val levelUpRewardPopup: StateFlow<LevelUpRewardPopup?> = _levelUpRewardPopup.asStateFlow()

    private val _isColiseumOpen = MutableStateFlow(false)
    val isColiseumOpen: StateFlow<Boolean> = _isColiseumOpen.asStateFlow()

    private val _coliseumDeckHeroIds = MutableStateFlow<List<String?>>(listOf(null, null, null, null))
    val coliseumDeckHeroIds: StateFlow<List<String?>> = _coliseumDeckHeroIds.asStateFlow()

    private val _coliseumRunState = MutableStateFlow<ColiseumRunState?>(null)
    val coliseumRunState: StateFlow<ColiseumRunState?> = _coliseumRunState.asStateFlow()

    private val _coliseumBattlePreview = MutableStateFlow<ColiseumBattlePreview?>(null)
    val coliseumBattlePreview: StateFlow<ColiseumBattlePreview?> = _coliseumBattlePreview.asStateFlow()

    private val _isColiseumPreviewVisible = MutableStateFlow(false)
    val isColiseumPreviewVisible: StateFlow<Boolean> = _isColiseumPreviewVisible.asStateFlow()

    private val _showColiseumTutorial = MutableStateFlow(false)
    val showColiseumTutorial: StateFlow<Boolean> = _showColiseumTutorial.asStateFlow()

    private val _coliseumMilestoneBonusOffer = MutableStateFlow<ColiseumMilestoneBonusOffer?>(null)
    val coliseumMilestoneBonusOffer: StateFlow<ColiseumMilestoneBonusOffer?> = _coliseumMilestoneBonusOffer.asStateFlow()

    private val _towerDeckHeroIds = MutableStateFlow<List<String?>>(listOf(null, null, null, null))
    val towerDeckHeroIds: StateFlow<List<String?>> = _towerDeckHeroIds.asStateFlow()

    private val _availableAppUpdate = MutableStateFlow<RemoteAppVersionInfo?>(null)
    val availableAppUpdate: StateFlow<RemoteAppVersionInfo?> = _availableAppUpdate.asStateFlow()

    private val _isCheckingForUpdate = MutableStateFlow(false)
    val isCheckingForUpdate: StateFlow<Boolean> = _isCheckingForUpdate.asStateFlow()

    private val _updateCheckStatusMessage = MutableStateFlow<String?>(null)
    val updateCheckStatusMessage: StateFlow<String?> = _updateCheckStatusMessage.asStateFlow()

    private var activeTowerSessionNowMs: Long? = null

    private fun resolveInitialTowerDeck(profile: PlayerProfile, heroes: List<HeroCard>): List<String?> {
        val unlockedIds = heroes.filter { it.isUnlocked }.map { it.id }.toSet()
        val savedTower = profile.towerDeckHeroIds
        val cleanedSaved = List(4) { idx ->
            savedTower.getOrNull(idx)?.takeIf { it in unlockedIds }
        }
        return if (cleanedSaved.any { it != null }) {
            cleanedSaved
        } else {
            List(4) { idx ->
                profile.activeDeckHeroIds.getOrNull(idx)?.takeIf { it in unlockedIds }
            }
        }
    }

    fun dismissSecretHeroUnlockedPopup() {
        _secretHeroUnlockedPopup.value = null
    }

    fun dismissLevelUpRewardPopup() {
        val popup = _levelUpRewardPopup.value
        _levelUpRewardPopup.value = null
        if (popup != null) {
            _playerProfile.update { p ->
                if (p.lastCelebratedLevel < popup.newLevel) {
                    p.copy(lastCelebratedLevel = popup.newLevel)
                } else p
            }
            repository.savePlayerProfile(_playerProfile.value)
        }
    }

    internal fun calculateLevelUpGoldReward(level: Int): Int {
        if (level < 2) return 0
        return 50 + (level - 2) * 40
    }

    internal fun calculateLevelUpGemsReward(level: Int): Int {
        if (level < 2) return 0
        return (level - 1) * 10
    }

    fun selectCampaignChapter(chapterId: Int) {
        _selectedCampaignChapterId.value = chapterId.coerceAtLeast(1)
    }

    fun isCampaignCompleted100Percent(chapters: List<CampaignChapter> = _campaignChapters.value): Boolean {
        val stages = chapters.flatMap { it.stages }
        return stages.isNotEmpty() && stages.all { it.stars >= 4 }
    }

    private fun checkCampaign100Completion() {
        val completed100 = isCampaignCompleted100Percent()
        _isCampaign100Percent.value = completed100
        if (completed100 && !_playerProfile.value.isCampaign100AchievementShown) {
            _showCampaign100AchievementDialog.value = true
        } else if (!completed100) {
            _showCampaign100AchievementDialog.value = false
        }
    }

    fun dismissCampaign100Achievement() {
        _showCampaign100AchievementDialog.value = false
        if (!_playerProfile.value.isCampaign100AchievementShown) {
            _playerProfile.update { it.copy(isCampaign100AchievementShown = true) }
            repository.savePlayerProfile(_playerProfile.value)
        }
    }

    companion object {
        const val COLISEUM_COOLDOWN_MS = 24L * 3600L * 1000L
        const val COLISEUM_DAILY_LIMIT = COLISEUM_DAILY_ENTRY_LIMIT
        const val COLISEUM_MIN_PLAYER_LEVEL = 5

        val ARENA_BATTLE_BACKGROUNDS = listOf(
            com.example.R.drawable.bg_brunhilde,
            com.example.R.drawable.bg_iva,
            com.example.R.drawable.bg_olga,
            com.example.R.drawable.bg_griselda,
            com.example.R.drawable.bg_lilith,
            com.example.R.drawable.bg_isolda,
            com.example.R.drawable.bg_alisa
        )
    }

    init {
        GameLocaleState.init(application)
        battleViewModel.setHapticCallback { strong -> triggerHaptic(strong) }
        val loadedProfile = repository.loadPlayerProfile()
        _playerProfile.value = loadedProfile
        com.example.audio.GameAudioManager.setMuted(loadedProfile.isSoundMuted)

        val initialHeroes = repository.getInitialHeroCards()
        val loadedHeroes = repository.loadHeroesState(initialHeroes)
        _allHeroes.value = loadedHeroes
        _towerDeckHeroIds.value = resolveInitialTowerDeck(loadedProfile, loadedHeroes)

        _inventoryArtifacts.value = repository.loadArtifacts()

        // Auto-unequip artifacts if hero level is below artifact requirement
        sanitizeHeroArtifacts()

        val savedStars = repository.loadCampaignStars()
        val savedClearCounts = repository.loadCampaignClearCounts()
        val initialChapters = repository.getCampaignChapters()
        val restoredChapters = initialChapters.map { chapter ->
            val updatedStages = chapter.stages.map { stage ->
                val stars = savedStars[stage.id] ?: 0
                val clearCount = savedClearCounts[stage.id] ?: if (stars > 0) 1 else 0
                stage.copy(stars = stars, clearCount = clearCount)
            }
            chapter.copy(stages = updatedStages)
        }
        _campaignChapters.value = restoredChapters

        updateCampaignStageLocks(loadedProfile.level)
        checkCampaign100Completion()
        checkAndUnlockHeroes(loadedProfile.level)
        checkAndRegenerateArenaKeys()
        refreshArenaBots()
        refreshDailyQuestsIfNeeded()
        checkDailyLoginBonus()
        checkForAppUpdates(manual = false)
    }

    fun checkAndRegenerateArenaKeys() {
        val profile = _playerProfile.value
        if (profile.arenaKeys >= 5) return

        val now = System.currentTimeMillis()
        val elapsedMs = now - profile.lastArenaKeyRegenTimestamp
        val periodMs = 2 * 3600 * 1000L // 2 hours per key (10 hours for 5 keys)
        val regenerated = (elapsedMs / periodMs).toInt()

        if (regenerated > 0) {
            val newKeys = (profile.arenaKeys + regenerated).coerceAtMost(5)
            val newRegenTime = if (newKeys == 5) now else profile.lastArenaKeyRegenTimestamp + (regenerated * periodMs)
            _playerProfile.update {
                it.copy(
                    arenaKeys = newKeys,
                    lastArenaKeyRegenTimestamp = newRegenTime
                )
            }
            repository.savePlayerProfile(_playerProfile.value)
        }
    }

    private fun triggerHaptic(strong: Boolean = false) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = if (strong) {
                    VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE)
                } else {
                    VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
                }
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(if (strong) 70 else 30)
            }
        } catch (_: Exception) {}
    }

    fun sanitizeHeroArtifacts() {
        var changed = false
        val currentArtifacts = _inventoryArtifacts.value
        val clampedArtifacts = currentArtifacts.map { it.withClampedHp() }
        if (clampedArtifacts != currentArtifacts) {
            _inventoryArtifacts.value = clampedArtifacts
            repository.saveArtifacts(clampedArtifacts)
        }
        val allArtifactsMap = _inventoryArtifacts.value.associateBy { it.id }
        _allHeroes.update { heroes ->
            heroes.map { hero ->
                val validEquipped = hero.equippedArtifactIds.filter { artId ->
                    val art = allArtifactsMap[artId]
                    if (art == null) {
                        changed = true
                        false
                    } else if (!canHeroEquipArtifact(hero.level, art.rarity)) {
                        changed = true
                        false
                    } else {
                        true
                    }
                }
                if (validEquipped.size != hero.equippedArtifactIds.size) {
                    hero.copy(equippedArtifactIds = validEquipped)
                } else {
                    hero
                }
            }
        }
        if (changed) {
            repository.saveHeroesState(_allHeroes.value)
            _inspectedHero.value?.let { inspected ->
                _inspectedHero.value = _allHeroes.value.firstOrNull { it.id == inspected.id }
            }
        }
    }

    fun inspectHero(hero: HeroCard?) {
        if (hero != null && !hero.isUnlocked) {
            if (hero.isSecret) {
                val targetShards = hero.secretUnlockShardsTarget()
                if (hero.shards >= targetShards) {
                    unlockSecretHero(hero.id)
                } else {
                    val left = (targetShards - hero.shards).coerceAtLeast(1)
                    showLockedHeroMessage(
                        tr(
                            "Секретный персонаж! Находите её частицы в Шкатулке редкостей (осталось: $left).",
                            "Secret character! Collect shards in the Mystery Casket ($left left)."
                        )
                    )
                }
                return
            }
            showLockedHeroMessage(
                tr(
                    "Эта карта пока скрыта! Проходите уровни Карьеры, чтобы открыть её.",
                    "This card is still locked! Clear Career stages to unlock her."
                )
            )
            return
        }
        sanitizeHeroArtifacts()
        _inspectedHero.value = hero?.let { h -> _allHeroes.value.firstOrNull { it.id == h.id } ?: h }
    }

    fun dismissDropMessage() {
        _latestDropMessage.value = null
    }

    private fun postDropMessage(text: String, append: Boolean = false) {
        val cleanNew = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        if (cleanNew.isEmpty()) return
        if (!append) {
            _latestDropMessage.value = cleanNew.distinct().joinToString("\n")
        } else {
            val prevLines = _latestDropMessage.value
                ?.split("\n")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?: emptyList()
            _latestDropMessage.value = (prevLines + cleanNew).distinct().joinToString("\n")
        }
    }

    fun showLockedHeroMessage(msg: String) {
        postDropMessage(msg)
    }

    fun convertGoldToGems() {
        val profile = _playerProfile.value
        if (profile.gold < 2500) {
            postDropMessage(
                tr(
                    "Недостаточно монет для обмена! Требуется 2500 монет.",
                    "Not enough coins to exchange! Requires 2500 coins."
                )
            )
            triggerHaptic(true)
            return
        }
        _playerProfile.update { it.copy(gold = it.gold - 2500, gems = it.gems + 100) }
        repository.savePlayerProfile(_playerProfile.value)
        triggerHaptic(true)
        postDropMessage(
            tr(
                "Обмен выполнен: -2500 🪙 → +100 💎!",
                "Exchange complete: -2500 🪙 → +100 💎!"
            )
        )
    }

    fun convertGemsToGold() {
        val profile = _playerProfile.value
        if (profile.gems < 100) {
            postDropMessage(
                tr(
                    "Недостаточно алмазов для обмена! Требуется 100 алмазов.",
                    "Not enough gems to exchange! Requires 100 gems."
                )
            )
            triggerHaptic(true)
            return
        }
        _playerProfile.update { it.copy(gems = it.gems - 100, gold = it.gold + 2500) }
        repository.savePlayerProfile(_playerProfile.value)
        triggerHaptic(true)
        postDropMessage(
            tr(
                "Обмен выполнен: -100 💎 → +2500 🪙!",
                "Exchange complete: -100 💎 → +2500 🪙!"
            )
        )
    }

    internal fun calculatePlayerDeckTotals(): Pair<Int, Int> {
        val deckIds = _playerProfile.value.activeDeckHeroIds
        val heroes = _allHeroes.value
        val artifacts = _inventoryArtifacts.value
        if (heroes.isEmpty()) return 0 to 0
        val deckHeroes = deckIds.mapIndexed { index, id ->
            id?.let { heroId -> heroes.firstOrNull { it.id == heroId } }
                ?: heroes.firstOrNull { it.isUnlocked }
                ?: heroes[index.coerceIn(0, heroes.lastIndex)]
        }
        val totalAtk = deckHeroes.sumOf { it.calculateTotalAtk(artifacts) }
        val totalHp = deckHeroes.sumOf { it.calculateTotalHp(artifacts) }
        return totalAtk to totalHp
    }

    internal fun allocateProportionalStats(rawValues: List<Int>, targetTotal: Int): IntArray {
        val count = rawValues.size
        if (count == 0) return IntArray(0)
        val safeTarget = targetTotal.coerceAtLeast(count)
        val rawSum = rawValues.sum().coerceAtLeast(1).toDouble()
        val allocated = IntArray(count)
        var accumulated = 0
        for (i in 0 until count - 1) {
            val value = ((rawValues[i].toDouble() / rawSum) * safeTarget).toInt().coerceAtLeast(1)
            allocated[i] = value
            accumulated += value
        }
        allocated[count - 1] = (safeTarget - accumulated).coerceAtLeast(1)
        return allocated
    }

    fun refreshArenaBots() {
        checkAndRegenerateArenaKeys()
        val profile = _playerProfile.value
        val (playerDeckAtk, playerDeckHp) = calculatePlayerDeckTotals()
        _arenaBots.value = repository.generateArenaOpponents(
            playerLevel = profile.level,
            playerTrophies = profile.trophies,
            playerDeckAtk = playerDeckAtk,
            playerDeckHp = playerDeckHp
        )
    }

    private fun updateCampaignStageLocks(playerLevel: Int) {
        _campaignChapters.update { chapters ->
            var prevChapterCleared = true
            chapters.map { chapter ->
                val updatedStages = chapter.stages.mapIndexed { idx, stage ->
                    val isFirstInChapter = idx == 0
                    val isFirstStageEver = chapter.id == 1 && isFirstInChapter
                    val prevStageCleared = if (isFirstInChapter) prevChapterCleared else chapter.stages[idx - 1].stars > 0
                    val unlocked = isFirstStageEver || prevStageCleared || stage.stars > 0
                    stage.copy(isUnlocked = unlocked)
                }
                prevChapterCleared = updatedStages.lastOrNull()?.let { it.stars > 0 } ?: false
                chapter.copy(stages = updatedStages)
            }
        }
    }

    fun markStoryRead() {
        _playerProfile.update { it.copy(isStoryRead = true) }
        repository.savePlayerProfile(_playerProfile.value)
    }

    private fun checkAndUnlockHeroes(newPlayerLevel: Int, clearedStageId: Int? = null) {
        val unlockedNow = mutableListOf<HeroCard>()
        val maxCareerStageCleared = _campaignChapters.value.flatMap { it.stages }.filter { it.stars > 0 }.maxOfOrNull { it.id } ?: 0
        val effectiveClearedStageId = maxOf(clearedStageId ?: 0, maxCareerStageCleared)

        _allHeroes.update { list ->
            list.map { hero ->
                if (!hero.isUnlocked && !hero.isSecret) {
                    val isUnlockedNow = if (hero.unlockCareerStageId != null) {
                        effectiveClearedStageId >= hero.unlockCareerStageId
                    } else {
                        newPlayerLevel >= hero.unlockPlayerLevel
                    }
                    if (isUnlockedNow) {
                        val updated = hero.copy(isUnlocked = true)
                        unlockedNow.add(updated)
                        updated
                    } else {
                        hero
                    }
                } else {
                    hero
                }
            }
        }
        if (unlockedNow.isNotEmpty()) {
            val names = unlockedNow.joinToString(", ") { "${it.localizedName()} (${it.heroType.localizedName()})" }
            val unlockText = tr(
                "🎉 Новая героиня открыта! Вы открыли: $names!",
                "🎉 New heroine unlocked! You unlocked: $names!"
            )
            postDropMessage(unlockText, append = true)
            repository.saveHeroesState(_allHeroes.value)
        }
    }

    private fun HeroCard.effectiveDropRarity(): CardRarity =
        if (isSecret && isUnlocked) CardRarity.RARE else rarity

    fun unlockSecretHero(heroId: String) {
        val hero = _allHeroes.value.firstOrNull { it.id == heroId } ?: return
        val unlockTarget = hero.secretUnlockShardsTarget()
        if (!hero.isSecret || hero.isUnlocked || hero.shards < unlockTarget) return

        triggerHaptic(true)
        val remainingShards = (hero.shards - unlockTarget).coerceAtLeast(0)
        var unlockedHero: HeroCard? = null
        _allHeroes.update { list ->
            list.map {
                if (it.id == heroId) {
                    val updated = it.copy(
                        isUnlocked = true,
                        shards = remainingShards,
                        shardsNeeded = 10
                    ).withUpdatedImageForLevel()
                    unlockedHero = updated
                    updated
                } else it
            }
        }
        repository.saveHeroesState(_allHeroes.value)
        val currentOffer = _fortuneWheelOffer.value
        if (currentOffer != null && _fortuneWheelResult.value == null) {
            _fortuneWheelOffer.value = currentOffer.copy(
                sectors = buildFortuneWheelSectors()
            )
        }
        _secretHeroUnlockedPopup.value = unlockedHero
    }

    fun assignHeroToDeckSlot(slotIndex: Int, heroId: String?) {
        if (slotIndex !in 0..3) return
        val hero = heroId?.let { id -> _allHeroes.value.firstOrNull { it.id == id } }
        if (hero != null && !hero.isUnlocked) {
            showLockedHeroMessage(
                tr(
                    "Нельзя взять в колоду скрытую карту! Проходите уровни Карьеры, чтобы открыть её.",
                    "Cannot add a locked card to the deck! Clear Career stages to unlock her."
                )
            )
            return
        }

        // Positioning Rule: Melee heroes CANNOT be placed in row 2 (slots 2 & 3)
        if (hero != null && slotIndex in 2..3 && hero.heroType == HeroType.MELEE) {
            showLockedHeroMessage(tr("Ближний бой только в 1 ряду!", "Melee heroes can only be placed in row 1!"))
            return
        }

        _playerProfile.update { current ->
            val updatedDeck = current.activeDeckHeroIds.toMutableList()
            while (updatedDeck.size < 4) updatedDeck.add(null)
            if (heroId != null) {
                for (i in 0 until updatedDeck.size) {
                    if (updatedDeck[i] == heroId) {
                        updatedDeck[i] = null
                    }
                }
            }
            updatedDeck[slotIndex] = heroId
            current.copy(activeDeckHeroIds = updatedDeck)
        }
        repository.savePlayerProfile(_playerProfile.value)
        refreshArenaBots()
        triggerHaptic(false)
    }

    fun upgradeHeroLevel(heroId: String) {
        val hero = _allHeroes.value.firstOrNull { it.id == heroId } ?: return
        val profile = _playerProfile.value
        val goldCost = hero.level * 200
        if (profile.gold < goldCost) return
        if (hero.shards < hero.shardsNeeded) return
        if (hero.level >= hero.maxUpgradeLevel()) return

        triggerHaptic(true)

        _playerProfile.update { it.copy(gold = it.gold - goldCost) }
        _allHeroes.update { list ->
            list.map {
                if (it.id == heroId) {
                    val newLvl = it.level + 1
                    val armorInc = if (it.heroType == HeroType.MELEE) 1 else 0
                    it.copy(
                        level = newLvl,
                        shards = if (newLvl >= it.maxUpgradeLevel()) 0 else it.shards - it.shardsNeeded,
                        shardsNeeded = (it.shardsNeeded * 1.5).toInt(),
                        baseAtk = it.baseAtk + it.atkIncrementPerLevel(),
                        baseArmor = it.baseArmor + armorInc,
                        baseHp = it.baseHp + 6
                    ).withUpdatedImageForLevel()
                } else it
            }
        }
        _inspectedHero.value = _allHeroes.value.firstOrNull { it.id == heroId }
        repository.savePlayerProfile(_playerProfile.value)
        repository.saveHeroesState(_allHeroes.value)
        refreshArenaBots()
        advanceDailyQuestProgress(QuestType.UPGRADE_HERO, 1)
        reconcileInfeasibleUncompletedQuests()
    }

    fun selectActiveSkill(heroId: String, skillId: String) {
        _allHeroes.update { list ->
            list.map {
                if (it.id == heroId) {
                    it.copy(activeSkillId = skillId)
                } else it
            }
        }
        _inspectedHero.value = _allHeroes.value.firstOrNull { it.id == heroId }
        repository.saveHeroesState(_allHeroes.value)
        triggerHaptic(false)
    }

    fun isDeckValid(deckIds: List<String?> = _playerProfile.value.activeDeckHeroIds): Boolean {
        return deckIds.size == 4 && deckIds.all { !it.isNullOrBlank() } && deckIds.filterNotNull().distinct().size == 4
    }

    fun equipArtifact(heroId: String, artifactId: String) {
        val artifact = _inventoryArtifacts.value.firstOrNull { it.id == artifactId } ?: return
        val hero = _allHeroes.value.firstOrNull { it.id == heroId } ?: return

        if (!canHeroEquipArtifact(hero.level, artifact.rarity)) {
            val reqLevel = getArtifactRequiredHeroLevel(artifact.rarity)
            postDropMessage(
                tr(
                    "Этот артефакт (${artifact.rarity.displayName}) можно надеть только с $reqLevel ур. героини!",
                    "This ${artifact.rarity.localizedName()} artifact can only be equipped from heroine Lv. $reqLevel!"
                )
            )
            triggerHaptic(true)
            return
        }

        _allHeroes.update { list ->
            list.map { h ->
                if (h.id == heroId) {
                    val currentEquipped = h.equippedArtifactIds.mapNotNull { id ->
                        _inventoryArtifacts.value.firstOrNull { it.id == id }
                    }
                    val filtered = currentEquipped.filter { it.slot != artifact.slot }.map { it.id }.toMutableList()
                    filtered.add(artifactId)
                    h.copy(equippedArtifactIds = filtered)
                } else {
                    h.copy(equippedArtifactIds = h.equippedArtifactIds.filter { it != artifactId })
                }
            }
        }
        _inspectedHero.value = _allHeroes.value.firstOrNull { it.id == heroId }
        repository.saveHeroesState(_allHeroes.value)
        refreshArenaBots()
        triggerHaptic(false)
    }

    fun unequipArtifact(heroId: String, artifactId: String) {
        _allHeroes.update { list ->
            list.map { hero ->
                if (hero.id == heroId) {
                    hero.copy(equippedArtifactIds = hero.equippedArtifactIds.filter { it != artifactId })
                } else hero
            }
        }
        _inspectedHero.value = _allHeroes.value.firstOrNull { it.id == heroId }
        repository.saveHeroesState(_allHeroes.value)
        refreshArenaBots()
        triggerHaptic(false)
    }

    fun toggleForgeArtifactSelection(artifactId: String) {
        val current = _forgeSelectedArtifactIds.value.toMutableList()
        if (current.contains(artifactId)) {
            current.remove(artifactId)
        } else {
            if (current.size < 3) {
                val candidate = _inventoryArtifacts.value.firstOrNull { it.id == artifactId }
                if (candidate != null) {
                    if (current.isEmpty()) {
                        current.add(artifactId)
                    } else {
                        val firstSelected = _inventoryArtifacts.value.firstOrNull { it.id == current.first() }
                        if (firstSelected?.rarity == candidate.rarity) {
                            current.add(artifactId)
                        }
                    }
                }
            }
        }
        _forgeSelectedArtifactIds.value = current
        triggerHaptic(false)
    }

    fun combineArtifacts() {
        val selectedIds = _forgeSelectedArtifactIds.value
        if (selectedIds.size != 3) return
        val selectedArtifacts = selectedIds.mapNotNull { id -> _inventoryArtifacts.value.firstOrNull { it.id == id } }
        if (selectedArtifacts.size != 3) return
        val currentRarity = selectedArtifacts.first().rarity
        val cost = when (currentRarity) {
            CardRarity.BASIC -> 100
            CardRarity.SPECIAL -> 200
            CardRarity.RARE -> 400
            else -> 0
        }
        if (_playerProfile.value.gold < cost) {
            postDropMessage(
                tr(
                    "Недостаточно монет для слияния! Требуется $cost монет.",
                    "Not enough coins to synthesize! Requires $cost coins."
                )
            )
            triggerHaptic(true)
            return
        }

        val nextRarity = when (currentRarity) {
            CardRarity.BASIC -> CardRarity.SPECIAL
            CardRarity.SPECIAL -> CardRarity.RARE
            CardRarity.RARE -> CardRarity.MYTHIC
            CardRarity.MYTHIC -> CardRarity.MYTHIC
        }

        val slotCounts = selectedArtifacts.groupingBy { it.slot }.eachCount()
        val slot = slotCounts.entries.firstOrNull { it.value >= 2 }?.key ?: ArtifactSlot.values().random()

        val legacyIds = setOf("art_1", "art_2", "art_3", "art_4", "art_5", "art_6", "art_7", "art_8", "art_9")
        val allCatalog = repository.getArtifactCatalog().values
        val slotPool = allCatalog
            .filter { it.id !in legacyIds && it.rarity == nextRarity && it.slot == slot }
            .ifEmpty { allCatalog.filter { it.id !in legacyIds && it.rarity == nextRarity } }
            .ifEmpty { allCatalog.filter { it.rarity == nextRarity } }
        val existingNames = _inventoryArtifacts.value.map { it.name }.toSet()
        val freshPool = slotPool.filter { it.name !in existingNames }.ifEmpty { slotPool }
        val template = freshPool.random()
        val newArtifact = template.copy(
            id = "forged_${template.id}_${System.currentTimeMillis()}_${Random.nextInt(1000)}"
        ).withClampedHp()

        triggerHaptic(true)

        _playerProfile.update { it.copy(gold = it.gold - cost) }
        _inventoryArtifacts.update { list ->
            list.filter { it.id !in selectedIds } + newArtifact
        }
        _allHeroes.update { list ->
            list.map { hero ->
                hero.copy(equippedArtifactIds = hero.equippedArtifactIds.filter { it !in selectedIds })
            }
        }
        _forgeSelectedArtifactIds.value = emptyList()
        postDropMessage(
            tr(
                "Синтез успешен (-$cost монет)! Создан артефакт: ${newArtifact.name} (${newArtifact.rarity.displayName})",
                "Synthesis complete (-$cost coins)! Forged: ${newArtifact.localizedName()} (${newArtifact.rarity.localizedName()})"
            )
        )
        repository.saveArtifacts(_inventoryArtifacts.value)
        repository.saveHeroesState(_allHeroes.value)
        repository.savePlayerProfile(_playerProfile.value)
        advanceDailyQuestProgress(QuestType.FORGE_COMBINE, 1)
        reconcileInfeasibleUncompletedQuests()
    }

    fun sellArtifact(artifactId: String, price: Int) {
        val art = _inventoryArtifacts.value.firstOrNull { it.id == artifactId } ?: return
        _inventoryArtifacts.update { list -> list.filter { it.id != artifactId } }
        _allHeroes.update { list ->
            list.map { hero ->
                if (hero.equippedArtifactIds.contains(artifactId)) {
                    hero.copy(equippedArtifactIds = hero.equippedArtifactIds.filter { it != artifactId })
                } else hero
            }
        }
        _forgeSelectedArtifactIds.update { it.filter { id -> id != artifactId } }
        _playerProfile.update { it.copy(gold = it.gold + price) }
        repository.saveArtifacts(_inventoryArtifacts.value)
        repository.saveHeroesState(_allHeroes.value)
        repository.savePlayerProfile(_playerProfile.value)
        postDropMessage(
            tr(
                "Вы продали [${art.name}] за $price монет!",
                "Sold [${art.localizedName()}] for $price coins!"
            )
        )
        triggerHaptic(false)
        advanceDailyQuestProgress(QuestType.SELL_ARTIFACT, 1)
        reconcileInfeasibleUncompletedQuests()
    }

    internal fun selectChestShardHero(
        eligibleHeroes: List<HeroCard>,
        targetRarity: CardRarity,
        preferOwnedRoll: Int = Random.nextInt(100)
    ): HeroCard? {
        if (eligibleHeroes.isEmpty()) return null
        val ownedEligible = eligibleHeroes.filter { it.isUnlocked }
        val lockedEligible = eligibleHeroes.filter { !it.isUnlocked }
        val pool = if (ownedEligible.isNotEmpty() && lockedEligible.isNotEmpty()) {
            if (preferOwnedRoll < 40) ownedEligible else lockedEligible
        } else {
            eligibleHeroes
        }
        val candidates = pool.filter { it.effectiveDropRarity() == targetRarity }.ifEmpty { pool }
        return candidates.randomOrNull()
    }

    fun dismissChestOpening() {
        _chestOpeningResult.value = null
    }

    fun openSummonChest(type: com.example.ui.screens.ChestType) {
        when (type) {
            com.example.ui.screens.ChestType.BASIC -> {
                val profile = _playerProfile.value
                val catalog = repository.getArtifactCatalog().values.toList()
                if (profile.gold < 500) return
                triggerHaptic(true)
                _playerProfile.update { it.copy(gold = it.gold - 500) }

                val roll = Random.nextInt(100)
                val targetRarity = when {
                    roll < 50 -> CardRarity.BASIC
                    roll < 75 -> CardRarity.SPECIAL
                    roll < 90 -> CardRarity.RARE
                    else -> CardRarity.MYTHIC
                }
                val eligibleHeroes = _allHeroes.value.filter { (!it.isSecret || it.isUnlocked) && it.level < it.maxUpgradeLevel() }
                val hero = selectChestShardHero(eligibleHeroes, targetRarity)
                val shardAmount = if (hero != null) {
                    resolveBasicChestShardRange(_allHeroes.value, hero.effectiveDropRarity()).random()
                } else 0

                if (hero != null) {
                    _allHeroes.update { list ->
                        list.map { if (it.id == hero.id) it.copy(shards = it.shards + shardAmount) else it }
                    }
                }

                // 10% chance to drop random basic artifact; if all heroes are maxed, guarantee Basic artifact (or coins fallback)
                var droppedArt: Artifact? = null
                var dropBonusText = ""
                val basicArtifacts = catalog.filter { it.rarity == CardRarity.BASIC }
                if (Random.nextInt(100) < 10 || hero == null) {
                    val art = basicArtifacts.randomOrNull()
                    if (art != null) {
                        val uniqueArt = art.copy(id = "${art.id}_${System.currentTimeMillis()}_${Random.nextInt(1000)}")
                        _inventoryArtifacts.update { it + uniqueArt }
                        repository.saveArtifacts(_inventoryArtifacts.value)
                        droppedArt = uniqueArt
                        dropBonusText = tr(
                            " + Базовый артефакт: ${uniqueArt.name}!",
                            " + Basic artifact: ${uniqueArt.localizedName()}!"
                        )
                    }
                }

                var fallbackGold = 0
                if (hero == null && droppedArt == null) {
                    fallbackGold = 300
                    _playerProfile.update { it.copy(gold = it.gold + fallbackGold) }
                }

                val heroNameLoc = hero?.localizedName()
                val summary = when {
                    hero != null -> tr(
                        "Осколки ${hero.name}: +$shardAmount 🧩$dropBonusText",
                        "$heroNameLoc shards: +$shardAmount 🧩$dropBonusText"
                    )
                    droppedArt != null -> tr(
                        "Получен артефакт: ${droppedArt.name} (${droppedArt.rarity.displayName})!",
                        "Received artifact: ${droppedArt.localizedName()} (${droppedArt.rarity.localizedName()})!"
                    )
                    else -> tr(
                        "Монеты: +$fallbackGold 💰",
                        "Coins: +$fallbackGold 💰"
                    )
                }
                _chestOpeningResult.value = ChestOpeningResult(
                    chestTitle = tr("Базовый сундук", "Basic Chest"),
                    accentColorHex = 0xFFFFD54FL,
                    chestImageRes = com.example.R.drawable.img_chest_basic,
                    heroId = hero?.id,
                    heroName = heroNameLoc,
                    heroImageRes = hero?.imageRes,
                    shardAmount = shardAmount,
                    dropArtifact = droppedArt,
                    summaryMessage = summary
                )
                repository.savePlayerProfile(_playerProfile.value)
                repository.saveHeroesState(_allHeroes.value)
                advanceDailyQuestProgress(QuestType.OPEN_CHESTS, 1)
            }
            com.example.ui.screens.ChestType.RARITY -> {
                openRarityChestWithSecretRoll()
            }
            com.example.ui.screens.ChestType.MYSTERY -> {
                openMysteryCasketWithRoll()
            }
            com.example.ui.screens.ChestType.ARTIFACTS -> {
                val profile = _playerProfile.value
                val catalog = repository.getArtifactCatalog().values.toList()
                if (profile.gold < 1000) return
                triggerHaptic(true)
                _playerProfile.update { it.copy(gold = it.gold - 1000) }

                // Only Special (60%) and Rare (40%) artifacts; Basic artifacts drop only in Basic Chest
                val roll = Random.nextInt(100)
                val targetRarity = if (roll < 60) CardRarity.SPECIAL else CardRarity.RARE
                val eligibleCatalog = catalog.filter {
                    it.rarity == CardRarity.SPECIAL || it.rarity == CardRarity.RARE
                }
                val pool = eligibleCatalog.filter { it.rarity == targetRarity }.ifEmpty { eligibleCatalog }
                val art = pool.random()
                val uniqueArt = art.copy(id = "${art.id}_${System.currentTimeMillis()}_${Random.nextInt(1000)}")
                _inventoryArtifacts.update { it + uniqueArt }
                repository.saveArtifacts(_inventoryArtifacts.value)

                _chestOpeningResult.value = ChestOpeningResult(
                    chestTitle = tr("Сундук с артефактами", "Artifact Chest"),
                    accentColorHex = 0xFF4CAF50L,
                    chestImageRes = com.example.R.drawable.img_chest_artifact,
                    dropArtifact = uniqueArt,
                    summaryMessage = tr(
                        "Получен артефакт: ${uniqueArt.name} (${uniqueArt.rarity.displayName})!",
                        "Received artifact: ${uniqueArt.localizedName()} (${uniqueArt.rarity.localizedName()})!"
                    )
                )
                repository.savePlayerProfile(_playerProfile.value)
                repository.saveHeroesState(_allHeroes.value)
                advanceDailyQuestProgress(QuestType.OPEN_CHESTS, 1)
            }
        }
    }

    internal fun resolveBasicChestShardRange(
        heroes: List<HeroCard> = _allHeroes.value,
        droppedRarity: CardRarity = CardRarity.BASIC
    ): IntRange {
        if (heroes.isEmpty()) return 3..7
        val total = heroes.size
        val over9Count = heroes.count { it.level > 9 }
        if (over9Count * 2 > total) {
            return if (droppedRarity == CardRarity.BASIC) 25..33 else 15..20
        }
        val ge5Count = heroes.count { it.level >= 5 }
        if (ge5Count * 2 > total) {
            return if (droppedRarity == CardRarity.BASIC) 10..15 else 8..10
        }
        return 3..7
    }

    internal fun resolveRarityChestShardRange(
        heroes: List<HeroCard> = _allHeroes.value,
        droppedRarity: CardRarity = CardRarity.RARE
    ): IntRange {
        if (heroes.isEmpty()) return 8..15
        val total = heroes.size
        val over9Count = heroes.count { it.level > 9 }
        if (over9Count * 2 > total) return 35..45
        val ge5Count = heroes.count { it.level >= 5 }
        if (ge5Count * 2 > total) return 20..25
        return 8..15
    }

    internal fun resolveRarityChestTargetRarity(roll: Int): CardRarity = when {
        roll < 11 -> CardRarity.BASIC
        roll < 53 -> CardRarity.SPECIAL
        roll < 84 -> CardRarity.RARE
        else -> CardRarity.MYTHIC
    }

    internal fun openRarityChestWithSecretRoll(
        secretDropRoll: Int? = null,
        fixedSecretShards: Int? = null,
        secretDropRollPermille: Int = Random.nextInt(1000),
        artifactRoll: Int = Random.nextInt(100)
    ) {
        val profile = _playerProfile.value
        val catalog = repository.getArtifactCatalog().values.toList()
        if (profile.gems < 100) return
        triggerHaptic(true)
        _playerProfile.update { it.copy(gems = it.gems - 100) }

        val targetRarity = resolveRarityChestTargetRarity(Random.nextInt(100))
        val eligibleHeroes = _allHeroes.value.filter { (!it.isSecret || it.isUnlocked) && it.level < it.maxUpgradeLevel() }
        val hero = selectChestShardHero(eligibleHeroes, targetRarity)
        val shardAmount = if (hero != null) {
            resolveRarityChestShardRange(_allHeroes.value, hero.effectiveDropRarity()).random()
        } else 0

        if (hero != null) {
            _allHeroes.update { list ->
                list.map { if (it.id == hero.id) it.copy(shards = it.shards + shardAmount) else it }
            }
        }

        // 5% chance to drop Special or Rare artifact (or guaranteed fallback if all regular heroes are maxed); never Basic
        var droppedArt: Artifact? = null
        var dropBonusText = ""
        val specialAndRareArtifacts = catalog.filter {
            it.rarity == CardRarity.SPECIAL || it.rarity == CardRarity.RARE
        }
        if (artifactRoll < 5 || hero == null) {
            val preferredRarity = if (Random.nextBoolean()) CardRarity.RARE else CardRarity.SPECIAL
            val pool = specialAndRareArtifacts
                .filter { it.rarity == preferredRarity }
                .ifEmpty { specialAndRareArtifacts }
            val art = pool.randomOrNull()
            if (art != null) {
                val uniqueArt = art.copy(id = "${art.id}_${System.currentTimeMillis()}_${Random.nextInt(1000)}")
                _inventoryArtifacts.update { it + uniqueArt }
                repository.saveArtifacts(_inventoryArtifacts.value)
                droppedArt = uniqueArt
                dropBonusText = tr(
                    " + ${uniqueArt.rarity.displayName} артефакт: ${uniqueArt.name}!",
                    " + ${uniqueArt.rarity.localizedName()} artifact: ${uniqueArt.localizedName()}!"
                )
            }
        }

        var fallbackGold = 0
        if (hero == null && droppedArt == null) {
            fallbackGold = 1500
            _playerProfile.update { it.copy(gold = it.gold + fallbackGold) }
        }

        val heroNameLoc = hero?.localizedName()
        val summary = when {
            hero != null -> tr(
                "Осколки ${hero.name}: +$shardAmount 🧩$dropBonusText",
                "$heroNameLoc shards: +$shardAmount 🧩$dropBonusText"
            )
            droppedArt != null -> tr(
                "Получен артефакт: ${droppedArt.name} (${droppedArt.rarity.displayName})!",
                "Received artifact: ${droppedArt.localizedName()} (${droppedArt.rarity.localizedName()})!"
            )
            else -> tr(
                "Монеты: +$fallbackGold 💰",
                "Coins: +$fallbackGold 💰"
            )
        }
        _chestOpeningResult.value = ChestOpeningResult(
            chestTitle = tr("Сундук редкостей", "Rarity Chest"),
            accentColorHex = 0xFFBA68C8L,
            chestImageRes = com.example.R.drawable.img_chest_rarity,
            heroId = hero?.id,
            heroName = heroNameLoc,
            heroImageRes = hero?.imageRes,
            shardAmount = shardAmount,
            dropArtifact = droppedArt,
            summaryMessage = summary,
            isSecretMystery = false
        )
        repository.savePlayerProfile(_playerProfile.value)
        repository.saveHeroesState(_allHeroes.value)
        advanceDailyQuestProgress(QuestType.OPEN_CHESTS, 1)
    }

    internal fun openMysteryCasketWithRoll(
        outcomeRollPermille: Int = Random.nextInt(1000),
        fixedSecretShards: Int? = null,
        targetSecretHeroId: String? = null
    ) {
        val profile = _playerProfile.value
        val lockedSecrets = _allHeroes.value.filter { it.isSecret && !it.isUnlocked }
        if (lockedSecrets.isEmpty() || profile.gems < 200) return

        triggerHaptic(true)
        _playerProfile.update { it.copy(gems = it.gems - 200) }

        val chosenHero = targetSecretHeroId
            ?.let { id -> lockedSecrets.firstOrNull { it.id == id } }
            ?: lockedSecrets.random()

        val permille = outcomeRollPermille.coerceIn(0, 999)
        if (permille < 5) {
            // 0.5%: instant unlock of a random locked secret heroine
            var unlockedHero: HeroCard? = null
            _allHeroes.update { list ->
                list.map {
                    if (it.id == chosenHero.id) {
                        val updated = it.copy(
                            isUnlocked = true,
                            shards = it.shards,
                            shardsNeeded = 10
                        ).withUpdatedImageForLevel()
                        unlockedHero = updated
                        updated
                    } else it
                }
            }
            val currentOffer = _fortuneWheelOffer.value
            if (currentOffer != null && _fortuneWheelResult.value == null) {
                _fortuneWheelOffer.value = currentOffer.copy(
                    sectors = buildFortuneWheelSectors()
                )
            }
            val resolvedHero = unlockedHero ?: chosenHero.copy(isUnlocked = true, shardsNeeded = 10).withUpdatedImageForLevel()
            val summary = tr(
                "Невероятная удача! Открыт секретный персонаж: ${resolvedHero.name}!",
                "Incredible luck! Unlocked secret character: ${resolvedHero.localizedName()}!"
            )
            postDropMessage(summary)
            _secretHeroUnlockedPopup.value = resolvedHero
            _chestOpeningResult.value = ChestOpeningResult(
                chestTitle = tr("Шкатулка редкостей", "Mystery Casket"),
                accentColorHex = 0xFF4DD0E1L,
                chestImageRes = com.example.R.drawable.img_chest_mystery,
                heroId = resolvedHero.id,
                heroName = resolvedHero.localizedName(),
                heroImageRes = resolvedHero.imageRes,
                shardAmount = 0,
                dropArtifact = null,
                summaryMessage = summary,
                isSecretMystery = false
            )
        } else {
            val shardAmount = fixedSecretShards ?: when {
                permille < 105 -> 15                  // 10% (5..104)
                permille < 305 -> Random.nextInt(9, 15) // 20% (105..304)
                permille < 605 -> Random.nextInt(5, 9)  // 30% (305..604)
                else -> Random.nextInt(2, 5)            // 39.5% (605..999)
            }
            _allHeroes.update { list ->
                list.map {
                    if (it.id == chosenHero.id) it.copy(shards = it.shards + shardAmount) else it
                }
            }
            val summary = tr(
                "Частицы: +$shardAmount 🧩 (секретный персонаж)",
                "Shards: +$shardAmount 🧩 (secret character)"
            )
            postDropMessage(summary)
            _chestOpeningResult.value = ChestOpeningResult(
                chestTitle = tr("Шкатулка редкостей", "Mystery Casket"),
                accentColorHex = 0xFF4DD0E1L,
                chestImageRes = com.example.R.drawable.img_chest_mystery,
                heroId = null,
                heroName = tr("секретный персонаж", "secret character"),
                heroImageRes = null,
                shardAmount = shardAmount,
                dropArtifact = null,
                summaryMessage = summary,
                isSecretMystery = true
            )
        }

        repository.savePlayerProfile(_playerProfile.value)
        repository.saveHeroesState(_allHeroes.value)
        advanceDailyQuestProgress(QuestType.OPEN_CHESTS, 1)
    }

    // ----------------------------------------------------
    // DAILY LOGIN BONUS & FORTUNE WHEEL (Calendar-Day Accurate)
    // ----------------------------------------------------

    private fun formatCalendarDate(timestampMs: Long): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(timestampMs))
    }

    internal fun calendarDaysBetween(previousDateStr: String?, todayDateStr: String): Int? {
        if (previousDateStr.isNullOrBlank()) return null
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply {
                isLenient = false
            }
            val prevDate = sdf.parse(previousDateStr) ?: return null
            val todayDate = sdf.parse(todayDateStr) ?: return null
            val prevCal = java.util.Calendar.getInstance().apply {
                time = prevDate
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val todayCal = java.util.Calendar.getInstance().apply {
                time = todayDate
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val diffMs = todayCal.timeInMillis - prevCal.timeInMillis
            kotlin.math.round(diffMs.toDouble() / (24.0 * 3600.0 * 1000.0)).toInt()
        } catch (_: Exception) {
            null
        }
    }

    internal fun buildFortuneWheelSectors(heroes: List<HeroCard> = _allHeroes.value): List<FortuneWheelSector> {
        val hasLockedSecretHero = heroes.any { it.isSecret && !it.isUnlocked }
        val shardSector1 = if (hasLockedSecretHero) {
            FortuneWheelSector(1, "3 тайны", "🕵️", FortuneWheelSectorType.SECRET_SHARDS_3)
        } else {
            FortuneWheelSector(1, "5 шт.", "🧩", FortuneWheelSectorType.SHARDS_5)
        }
        return listOf(
            FortuneWheelSector(0, "до 500", "💰", FortuneWheelSectorType.GOLD_UP_TO_500),
            shardSector1,
            FortuneWheelSector(2, "Пусто", "💨", FortuneWheelSectorType.EMPTY),
            FortuneWheelSector(3, "40", "💎", FortuneWheelSectorType.GEMS_40),
            FortuneWheelSector(4, "Базовый", "🛡️", FortuneWheelSectorType.ARTIFACT_BASIC),
            FortuneWheelSector(5, "до 1000", "💰", FortuneWheelSectorType.GOLD_UP_TO_1000),
            FortuneWheelSector(6, "10 шт.", "🧩", FortuneWheelSectorType.SHARDS_10),
            FortuneWheelSector(7, "Пусто", "❌", FortuneWheelSectorType.EMPTY),
            FortuneWheelSector(8, "100", "💎", FortuneWheelSectorType.GEMS_100),
            FortuneWheelSector(9, "Особый", "✨", FortuneWheelSectorType.ARTIFACT_SPECIAL)
        )
    }

    fun checkDailyLoginBonus(nowMs: Long = System.currentTimeMillis()) {
        val profile = _playerProfile.value

        // Anti-cheat with 5-minute tolerance against NTP/emulator clock jitter
        if (nowMs + 5 * 60_000L < profile.maxSeenTimestamp) {
            return
        }

        refreshDailyQuestsIfNeeded(nowMs)

        // Update maxSeenTimestamp to prevent future clock rewinding
        val updatedMaxSeen = maxOf(profile.maxSeenTimestamp, nowMs)
        if (updatedMaxSeen > profile.maxSeenTimestamp) {
            _playerProfile.update { it.copy(maxSeenTimestamp = updatedMaxSeen) }
            repository.savePlayerProfile(_playerProfile.value)
        }

        val todayStr = formatCalendarDate(nowMs)
        val effectiveLastDate = profile.lastDailyClaimDate?.takeIf { it.isNotBlank() }
            ?: if (profile.lastDailyClaimTimestamp > 0L) formatCalendarDate(profile.lastDailyClaimTimestamp) else null

        val daysDiff = calendarDaysBetween(effectiveLastDate, todayStr)
        if (daysDiff != null && daysDiff < 0) {
            // System date was wound back earlier than last claim date
            return
        }
        if (effectiveLastDate == todayStr || daysDiff == 0) {
            // Already claimed today
            return
        }

        // Consecutive calendar day -> advance streak; missed >= 1 day or first launch -> streak = 1
        val nextStreak = if (daysDiff == 1) {
            profile.dailyStreak.coerceAtLeast(0) + 1
        } else {
            1
        }

        if (nextStreak > 7) {
            // More than 7 consecutive days -> show Fortune Wheel instead of regular daily bonus
            _dailyBonusReward.value = null
            if (_fortuneWheelOffer.value?.dayNumber != nextStreak) {
                _fortuneWheelResult.value = null
                _fortuneWheelOffer.value = FortuneWheelOffer(
                    dayNumber = nextStreak,
                    sectors = buildFortuneWheelSectors()
                )
            } else if (_fortuneWheelResult.value == null) {
                _fortuneWheelOffer.value = _fortuneWheelOffer.value?.copy(
                    sectors = buildFortuneWheelSectors()
                )
            }
            return
        }

        if (_dailyBonusReward.value?.dayNumber == nextStreak) {
            return
        }

        // Reward schedule (Days 1..7):
        val catalog = repository.getArtifactCatalog().values.toList()
        val gold = when (nextStreak) {
            1 -> 100
            2 -> 200
            3 -> 300
            4 -> 400
            5 -> 500
            6 -> 600
            else -> 1000
        }
        val bonusArtifact = when (nextStreak) {
            2 -> catalog.filter { it.rarity == CardRarity.BASIC }.randomOrNull()
            4 -> catalog.filter { it.rarity == CardRarity.RARE }.randomOrNull()
            7 -> catalog.filter { it.rarity == CardRarity.SPECIAL }.randomOrNull()
            else -> null
        }

        _fortuneWheelOffer.value = null
        _fortuneWheelResult.value = null
        _dailyBonusReward.value = DailyBonusReward(
            dayNumber = nextStreak,
            goldAmount = gold,
            artifact = bonusArtifact
        )
    }

    fun claimDailyBonus(nowMs: Long = System.currentTimeMillis()) {
        val reward = _dailyBonusReward.value ?: return
        val todayStr = formatCalendarDate(nowMs)

        _playerProfile.update {
            it.copy(
                gold = it.gold + reward.goldAmount,
                lastDailyClaimDate = todayStr,
                lastDailyClaimTimestamp = nowMs,
                dailyStreak = reward.dayNumber,
                maxSeenTimestamp = maxOf(it.maxSeenTimestamp, nowMs)
            )
        }

        reward.artifact?.let { art ->
            val uniqueArt = art.copy(id = "${art.id}_daily_${System.currentTimeMillis()}")
            _inventoryArtifacts.update { it + uniqueArt }
            repository.saveArtifacts(_inventoryArtifacts.value)
        }

        repository.savePlayerProfile(_playerProfile.value)
        _dailyBonusReward.value = null
        triggerHaptic(true)
        postDropMessage(
            tr(
                "Ежедневный бонус получен: +${reward.goldAmount} 💰" +
                    (if (reward.artifact != null) " и артефакт: ${reward.artifact.name}!" else "!"),
                "Daily bonus claimed: +${reward.goldAmount} 💰" +
                    (if (reward.artifact != null) " and artifact: ${reward.artifact.localizedName()}!" else "!")
            )
        )
    }

    fun dismissDailyBonus() {
        _dailyBonusReward.value = null
    }

    fun claimFortuneWheelReward(sector: FortuneWheelSector, nowMs: Long = System.currentTimeMillis()) {
        val offer = _fortuneWheelOffer.value ?: return
        if (_fortuneWheelResult.value != null) return // Only 1 spin per day

        val todayStr = formatCalendarDate(nowMs)
        val catalog = repository.getArtifactCatalog().values.toList()
        var goldGain = 0
        var gemGain = 0
        var isEmpty = false
        val description: String

        when (sector.type) {
            FortuneWheelSectorType.GOLD_UP_TO_500 -> {
                goldGain = Random.nextInt(100, 501)
                description = tr("+$goldGain 💰 (золото)", "+$goldGain 💰 (coins)")
            }
            FortuneWheelSectorType.GOLD_UP_TO_1000 -> {
                goldGain = Random.nextInt(501, 1001)
                description = tr("+$goldGain 💰 (золото)", "+$goldGain 💰 (coins)")
            }
            FortuneWheelSectorType.SHARDS_5, FortuneWheelSectorType.SHARDS_10 -> {
                val amount = if (sector.type == FortuneWheelSectorType.SHARDS_10) 10 else 5
                val eligibleHeroes = _allHeroes.value.filter { (!it.isSecret || it.isUnlocked) && it.level < it.maxUpgradeLevel() }
                val targetRarity = CardRarity.values().random()
                val hero = selectChestShardHero(eligibleHeroes, targetRarity)
                if (hero != null) {
                    _allHeroes.update { list ->
                        list.map { if (it.id == hero.id) it.copy(shards = it.shards + amount) else it }
                    }
                    repository.saveHeroesState(_allHeroes.value)
                    description = tr(
                        "+$amount 🧩 осколков для ${hero.name}!",
                        "+$amount 🧩 shards for ${hero.localizedName()}!"
                    )
                } else {
                    goldGain = amount * 50
                    description = tr(
                        "+$goldGain 💰 (золото)",
                        "+$goldGain 💰 (coins)"
                    )
                }
            }
            FortuneWheelSectorType.SECRET_SHARDS_3 -> {
                val secretHero = _allHeroes.value
                    .filter { it.isSecret && !it.isUnlocked && it.level < it.maxUpgradeLevel() }
                    .randomOrNull()
                if (secretHero != null) {
                    _allHeroes.update { list ->
                        list.map { if (it.id == secretHero.id) it.copy(shards = it.shards + 3) else it }
                    }
                    repository.saveHeroesState(_allHeroes.value)
                    description = tr(
                        "+3 🧩 частицы секретного персонажа!",
                        "+3 🧩 secret character shards!"
                    )
                } else {
                    val eligibleHeroes = _allHeroes.value.filter {
                        (!it.isSecret || it.isUnlocked) && it.level < it.maxUpgradeLevel()
                    }
                    val fallbackHero = selectChestShardHero(eligibleHeroes, CardRarity.RARE)
                    if (fallbackHero != null) {
                        _allHeroes.update { list ->
                            list.map { if (it.id == fallbackHero.id) it.copy(shards = it.shards + 3) else it }
                        }
                        repository.saveHeroesState(_allHeroes.value)
                        description = tr(
                            "+3 🧩 осколка для ${fallbackHero.name}!",
                            "+3 🧩 shards for ${fallbackHero.localizedName()}!"
                        )
                    } else {
                        goldGain = 150
                        description = tr(
                            "+150 💰 (золото)",
                            "+150 💰 (coins)"
                        )
                    }
                }
            }
            FortuneWheelSectorType.GEMS_40 -> {
                gemGain = 40
                description = tr("+40 💎 (алмазы)", "+40 💎 (gems)")
            }
            FortuneWheelSectorType.GEMS_100 -> {
                gemGain = 100
                description = tr("+100 💎 (алмазы)", "+100 💎 (gems)")
            }
            FortuneWheelSectorType.ARTIFACT_BASIC -> {
                val art = catalog.filter { it.rarity == CardRarity.BASIC }.randomOrNull()
                if (art != null) {
                    val uniqueArt = art.copy(id = "${art.id}_wheel_${System.currentTimeMillis()}_${Random.nextInt(1000)}")
                    _inventoryArtifacts.update { it + uniqueArt }
                    repository.saveArtifacts(_inventoryArtifacts.value)
                    description = tr(
                        "Базовый артефакт: ${uniqueArt.name}!",
                        "Basic artifact: ${uniqueArt.localizedName()}!"
                    )
                } else {
                    goldGain = 250
                    description = "+250 💰"
                }
            }
            FortuneWheelSectorType.ARTIFACT_SPECIAL -> {
                val art = catalog.filter { it.rarity == CardRarity.SPECIAL }.randomOrNull()
                if (art != null) {
                    val uniqueArt = art.copy(id = "${art.id}_wheel_${System.currentTimeMillis()}_${Random.nextInt(1000)}")
                    _inventoryArtifacts.update { it + uniqueArt }
                    repository.saveArtifacts(_inventoryArtifacts.value)
                    description = tr(
                        "Особый артефакт: ${uniqueArt.name}!",
                        "Special artifact: ${uniqueArt.localizedName()}!"
                    )
                } else {
                    goldGain = 500
                    description = "+500 💰"
                }
            }
            FortuneWheelSectorType.EMPTY -> {
                isEmpty = true
                description = tr(
                    "В этот раз без приза. Попробуйте завтра!",
                    "No prize this time. Try again tomorrow!"
                )
            }
        }

        _playerProfile.update {
            it.copy(
                gold = it.gold + goldGain,
                gems = it.gems + gemGain,
                lastDailyClaimDate = todayStr,
                lastDailyClaimTimestamp = nowMs,
                dailyStreak = offer.dayNumber,
                maxSeenTimestamp = maxOf(it.maxSeenTimestamp, nowMs)
            )
        }
        repository.savePlayerProfile(_playerProfile.value)
        _fortuneWheelResult.value = FortuneWheelResult(
            sector = sector,
            description = description,
            isEmpty = isEmpty
        )
        triggerHaptic(!isEmpty)
    }

    fun dismissFortuneWheel() {
        val res = _fortuneWheelResult.value
        _fortuneWheelOffer.value = null
        _fortuneWheelResult.value = null
        if (res != null && !res.isEmpty) {
            postDropMessage(
                tr(
                    "Колесо Фортуны: ${res.description}",
                    "Fortune Wheel: ${res.description}"
                )
            )
        }
    }

    // ----------------------------------------------------
    // DAILY QUESTS SYSTEM (5 Progressive Quests / 36 Pool)
    // ----------------------------------------------------

    internal fun isQuestTemplateFeasible(
        template: QuestTemplate,
        heroes: List<HeroCard> = _allHeroes.value,
        chapters: List<CampaignChapter> = _campaignChapters.value,
        artifacts: List<Artifact> = _inventoryArtifacts.value
    ): Boolean {
        if (template.rewardType == QuestRewardType.SHARDS &&
            heroes.none { (!it.isSecret || it.isUnlocked) && it.level < it.maxUpgradeLevel() }
        ) {
            return false
        }
        val unlocked = heroes.filter { it.isUnlocked }
        return when (template.type) {
            QuestType.WIN_ANY_BATTLES,
            QuestType.WIN_CAREER_BATTLES,
            QuestType.WIN_ARENA_BATTLES,
            QuestType.WIN_NO_LOSSES,
            QuestType.DEFEAT_ENEMIES,
            QuestType.OPEN_CHESTS -> true
            QuestType.WIN_ARENA_RANGED_ONLY -> unlocked.count { it.heroType == HeroType.RANGED } >= 4
            QuestType.WIN_FOUR_RANGED -> unlocked.count { it.heroType == HeroType.RANGED } >= 4
            QuestType.WIN_ARENA_TWO_MELEE -> false
            QuestType.WIN_TWO_HYBRID -> unlocked.count { it.heroType == HeroType.HYBRID } >= 2
            QuestType.WIN_WITH_HYBRID -> unlocked.any { it.heroType == HeroType.HYBRID }
            QuestType.WIN_WITH_ELEMENT -> template.requiredElement?.let { el -> unlocked.any { it.element == el } } ?: false
            QuestType.EARN_CAREER_STARS -> {
                val remainingStars = chapters.flatMap { it.stages }.sumOf { (4 - it.stars).coerceAtLeast(0) }
                remainingStars >= template.targetCount
            }
            QuestType.WIN_CAREER_BOSS -> chapters.flatMap { it.stages }.any { it.isBoss && it.isUnlocked }
            QuestType.CAST_SKILLS -> unlocked.any { it.level >= 3 }
            QuestType.UPGRADE_HERO -> unlocked.any { it.level < it.maxUpgradeLevel() }
            QuestType.FORGE_COMBINE -> artifacts.filter { it.rarity != CardRarity.MYTHIC }.groupBy { it.rarity }.any { it.value.size >= 3 }
            QuestType.SELL_ARTIFACT -> artifacts.size >= 2
        }
    }

    private fun templateToDailyState(t: QuestTemplate): DailyQuestState = DailyQuestState(
        id = t.id,
        title = t.title,
        description = t.description,
        type = t.type,
        difficultyTier = t.difficultyTier,
        targetCount = t.targetCount,
        currentProgress = 0,
        rewardType = t.rewardType,
        rewardAmount = t.rewardAmount,
        isRewardClaimed = false,
        requiredElement = t.requiredElement
    )

    private fun generateDailyQuestsForDay(prevQuestIds: Set<String>): List<DailyQuestState> {
        val catalog = repository.getDailyQuestCatalog()
        val heroes = _allHeroes.value
        val chapters = _campaignChapters.value
        val artifacts = _inventoryArtifacts.value

        return (1..5).mapNotNull { tier ->
            val feasible = catalog.filter {
                it.difficultyTier == tier && isQuestTemplateFeasible(it, heroes, chapters, artifacts)
            }
            val pool = feasible.ifEmpty {
                catalog.filter {
                    it.difficultyTier == tier && it.type in setOf(
                        QuestType.WIN_ANY_BATTLES,
                        QuestType.WIN_CAREER_BATTLES,
                        QuestType.WIN_ARENA_BATTLES,
                        QuestType.WIN_NO_LOSSES,
                        QuestType.DEFEAT_ENEMIES
                    )
                }
            }.ifEmpty {
                catalog.filter { it.difficultyTier == tier }
            }
            val alternating = pool.filter { it.id !in prevQuestIds }
            val chosen = (alternating.ifEmpty { pool }).randomOrNull()
            chosen?.let { templateToDailyState(it) }
        }.sortedBy { it.difficultyTier }
    }

    fun refreshDailyQuestsIfNeeded(nowMs: Long = System.currentTimeMillis()) {
        val todayStr = formatCalendarDate(nowMs)
        val savedDate = repository.loadDailyQuestDate()
        val savedQuests = repository.loadDailyQuests()

        val daysDiff = calendarDaysBetween(savedDate, todayStr)
        if (daysDiff != null && daysDiff < 0 && savedQuests.isNotEmpty()) {
            // Clock rewound backwards before saved daily quest date -> keep existing quests
            if (_dailyQuests.value.isEmpty()) {
                _dailyQuests.value = savedQuests
            }
            return
        }

        if (savedDate != todayStr || savedQuests.size != 5) {
            val prevIds = if (savedDate != null && savedDate != todayStr) {
                val yesterdayIds = savedQuests.map { it.id }.toSet()
                    .ifEmpty { _dailyQuests.value.map { it.id }.toSet() }
                if (yesterdayIds.isNotEmpty()) {
                    repository.savePreviousDailyQuestIds(yesterdayIds)
                    yesterdayIds
                } else {
                    repository.loadPreviousDailyQuestIds()
                }
            } else {
                repository.loadPreviousDailyQuestIds()
            }

            val freshQuests = generateDailyQuestsForDay(prevIds)
            _dailyQuests.value = freshQuests
            repository.saveDailyQuests(freshQuests)
            repository.saveDailyQuestDate(todayStr)
        } else {
            _dailyQuests.value = savedQuests
            reconcileInfeasibleUncompletedQuests()
        }
    }

    private fun reconcileInfeasibleUncompletedQuests() {
        val current = _dailyQuests.value
        if (current.isEmpty()) return
        val catalogById = repository.getDailyQuestCatalog().associateBy { it.id }
        val catalog = repository.getDailyQuestCatalog()
        val heroes = _allHeroes.value
        val chapters = _campaignChapters.value
        val artifacts = _inventoryArtifacts.value
        val prevIds = repository.loadPreviousDailyQuestIds()

        var changed = false
        val usedIds = current.map { it.id }.toMutableSet()
        val updated = current.map { quest ->
            if (quest.isCompleted) {
                quest
            } else {
                val tmpl = catalogById[quest.id]
                val feasible = tmpl != null && isQuestTemplateFeasible(tmpl, heroes, chapters, artifacts)
                if (feasible) {
                    quest
                } else {
                    val candidates = catalog.filter {
                        it.difficultyTier == quest.difficultyTier &&
                            it.id !in usedIds &&
                            isQuestTemplateFeasible(it, heroes, chapters, artifacts)
                    }
                    val preferred = candidates.filter { it.id !in prevIds }
                    val replacement = (preferred.ifEmpty { candidates }).randomOrNull()
                    if (replacement != null) {
                        usedIds.add(replacement.id)
                        changed = true
                        templateToDailyState(replacement)
                    } else {
                        quest
                    }
                }
            }
        }
        if (changed) {
            _dailyQuests.value = updated
            repository.saveDailyQuests(updated)
        }
    }

    private fun advanceDailyQuestProgress(
        type: QuestType,
        delta: Int = 1,
        requiredElement: Element? = null
    ) {
        if (delta <= 0) return
        var changed = false
        val updated = _dailyQuests.value.map { quest ->
            val elementMatches = quest.type != QuestType.WIN_WITH_ELEMENT || quest.requiredElement == requiredElement
            if (quest.type == type && elementMatches && !quest.isCompleted) {
                changed = true
                val newProg = (quest.currentProgress + delta).coerceAtMost(quest.targetCount)
                quest.copy(currentProgress = newProg)
            } else {
                quest
            }
        }
        if (changed) {
            _dailyQuests.value = updated
            repository.saveDailyQuests(updated)
        }
    }

    fun claimDailyQuestReward(questId: String) {
        val quest = _dailyQuests.value.firstOrNull { it.id == questId } ?: return
        if (!quest.isCompleted || quest.isRewardClaimed) return

        triggerHaptic(true)

        when (quest.rewardType) {
            QuestRewardType.GOLD -> {
                _playerProfile.update { it.copy(gold = it.gold + quest.rewardAmount) }
                repository.savePlayerProfile(_playerProfile.value)
                postDropMessage(
                    tr(
                        "Квест «${quest.title}»: +${quest.rewardAmount} 💰!",
                        "Quest \"${quest.localizedTitle()}\": +${quest.rewardAmount} 💰!"
                    )
                )
            }
            QuestRewardType.SHARDS -> {
                val eligibleHeroes = _allHeroes.value.filter { (!it.isSecret || it.isUnlocked) && it.level < it.maxUpgradeLevel() }
                val hero = selectChestShardHero(eligibleHeroes, CardRarity.values().random())
                if (hero != null) {
                    _allHeroes.update { list ->
                        list.map { if (it.id == hero.id) it.copy(shards = it.shards + quest.rewardAmount) else it }
                    }
                    repository.saveHeroesState(_allHeroes.value)
                    postDropMessage(
                        tr(
                            "Квест «${quest.title}»: +${quest.rewardAmount} 🧩 осколков для ${hero.name}!",
                            "Quest \"${quest.localizedTitle()}\": +${quest.rewardAmount} 🧩 shards for ${hero.localizedName()}!"
                        )
                    )
                } else {
                    val goldFallback = quest.rewardAmount * 40
                    _playerProfile.update { it.copy(gold = it.gold + goldFallback) }
                    repository.savePlayerProfile(_playerProfile.value)
                    postDropMessage(
                        tr(
                            "Квест «${quest.title}»: +$goldFallback 💰!",
                            "Quest \"${quest.localizedTitle()}\": +$goldFallback 💰!"
                        )
                    )
                }
            }
        }

        _dailyQuests.update { list ->
            list.map { if (it.id == questId) it.copy(isRewardClaimed = true) else it }
        }
        repository.saveDailyQuests(_dailyQuests.value)
    }

    // ----------------------------------------------------
    // SETTINGS, GOOGLE SYNC & JSON SAVE EXPORT / IMPORT
    // ----------------------------------------------------

    fun updatePlayerName(newName: String) {
        val trimmed = newName.trim().take(14)
        if (trimmed.isNotBlank()) {
            _playerProfile.update { it.copy(name = trimmed) }
            repository.savePlayerProfile(_playerProfile.value)
            postDropMessage(
                tr(
                    "Имя профиля обновлено: $trimmed",
                    "Profile name updated: $trimmed"
                )
            )
            triggerHaptic(false)
        }
    }

    fun completeTutorial() {
        _playerProfile.update { it.copy(isTutorialCompleted = true) }
        repository.savePlayerProfile(_playerProfile.value)
        checkDailyLoginBonus()
    }

    fun setPlayerNicknameAndStartTutorial(nick: String) {
        val trimmed = nick.trim().take(14).ifBlank { tr("Хранительница", "Guardian") }
        _playerProfile.update { it.copy(name = trimmed) }
        repository.savePlayerProfile(_playerProfile.value)
    }

    fun toggleGoogleSync(enable: Boolean, email: String? = null) {
        val syncEmail = if (enable) (email ?: "player@gmail.com") else null
        _playerProfile.update {
            it.copy(
                isGoogleSynced = enable,
                googleAccountEmail = syncEmail,
                lastSyncTimestamp = if (enable) System.currentTimeMillis() else 0L
            )
        }
        repository.savePlayerProfile(_playerProfile.value)
        postDropMessage(
            if (enable) {
                tr("Google аккаунт ($syncEmail) успешно синхронизирован! ☁️", "Google account ($syncEmail) synced successfully! ☁️")
            } else {
                tr("Синхронизация с Google отключена.", "Google sync disabled.")
            }
        )
        triggerHaptic(true)
    }

    fun exportSaveToJson(): String {
        val allStages = _campaignChapters.value.flatMap { it.stages }
        val starsMap = allStages.filter { it.stars > 0 }.associate { it.id to it.stars }
        val clearCountsMap = allStages.filter { it.clearCount > 0 }.associate { it.id to it.clearCount }
        return repository.exportSaveToJson(_playerProfile.value, _allHeroes.value, _inventoryArtifacts.value, starsMap, clearCountsMap)
    }

    fun importSaveFromJson(jsonString: String): Boolean {
        val defaultHeroes = repository.getInitialHeroCards()
        val result = repository.importSaveFromJson(jsonString, defaultHeroes)
        if (result != null) {
            val (profile, heroes, artifacts, importedStars) = result
            val importedClearCounts = result.campaignClearCounts
            _playerProfile.value = profile
            com.example.audio.GameAudioManager.setMuted(profile.isSoundMuted)
            _allHeroes.value = heroes
            _towerDeckHeroIds.value = resolveInitialTowerDeck(profile, heroes)
            _inventoryArtifacts.value = artifacts.map { it.withClampedHp() }
            _campaignChapters.update { chapters ->
                chapters.map { chapter ->
                    val updatedStages = chapter.stages.map { stage ->
                        val stars = importedStars[stage.id] ?: 0
                        val clearCount = importedClearCounts[stage.id] ?: if (stars > 0) 1 else 0
                        stage.copy(stars = stars, clearCount = clearCount)
                    }
                    chapter.copy(stages = updatedStages)
                }
            }
            sanitizeHeroArtifacts()
            updateCampaignStageLocks(profile.level)
            checkCampaign100Completion()
            checkAndUnlockHeroes(profile.level)
            refreshDailyQuestsIfNeeded()
            refreshArenaBots()
            postDropMessage(
                tr(
                    "Сохранение успешно загружено! Данные восстановлены.",
                    "Save file loaded successfully! Progress restored."
                )
            )
            triggerHaptic(true)
            return true
        } else {
            postDropMessage(
                tr(
                    "Ошибка чтения файла сохранения. Проверьте формат JSON.",
                    "Error reading save file. Check JSON format."
                )
            )
            return false
        }
    }

    // ----------------------------------------------------
    // BATTLE SYSTEM (Delegated to BattleViewModel)
    // ----------------------------------------------------

    internal fun resolveEffectiveCampaignEnemyHeroIds(
        stage: CampaignStage,
        heroes: List<HeroCard> = _allHeroes.value,
        replaceBackSlot: Int = listOf(2, 3).random()
    ): List<String> {
        if (stage.id != 203) return stage.enemyHeroIds
        val isVasilisaUnlocked = heroes.any { it.id == "vasilisa" && it.isUnlocked }
        if (!isVasilisaUnlocked) return stage.enemyHeroIds
        val targetSlot = replaceBackSlot.coerceIn(2, 3)
        if (targetSlot !in stage.enemyHeroIds.indices) return stage.enemyHeroIds
        return stage.enemyHeroIds.toMutableList().apply {
            this[targetSlot] = "vasilisa"
        }
    }

    private data class CampaignBattleSetup(
        val effectiveStage: CampaignStage,
        val playerBattleCards: List<BattleCard>,
        val enemyBattleCards: List<BattleCard>,
        val actualGold: Int,
        val rewardGems: Int,
        val rewardExp: Int,
        val rewardArtifact: Artifact?,
        val chapterIndex: Int,
        val campaignBg: Int,
        val foundAmulet: DiscoveredAmulet?,
        val victoryEventMessage: String?
    )

    private fun prepareCampaignBattleSetup(stage: CampaignStage): CampaignBattleSetup {
        val effectiveStage = stage.copy(
            enemyHeroIds = resolveEffectiveCampaignEnemyHeroIds(stage, _allHeroes.value)
        )
        val playerDeckIds = _playerProfile.value.activeDeckHeroIds
        val playerDeckHeroes = playerDeckIds.mapIndexed { index, id ->
            val hero = id?.let { heroId -> _allHeroes.value.firstOrNull { it.id == heroId } }
            hero ?: _allHeroes.value.firstOrNull { it.isUnlocked } ?: _allHeroes.value[index.coerceIn(0, _allHeroes.value.lastIndex)]
        }

        val playerBattleCards = playerDeckHeroes.mapIndexed { slotIndex, hero ->
            val totalAtk = hero.calculateTotalAtk(_inventoryArtifacts.value)
            val totalArmor = hero.calculateTotalArmor(_inventoryArtifacts.value)
            val totalHp = hero.calculateTotalHp(_inventoryArtifacts.value)
            BattleCard(
                heroId = hero.id,
                name = hero.name,
                title = hero.title,
                heroType = hero.heroType,
                rarity = hero.rarity,
                element = hero.element,
                imageRes = hero.imageRes,
                slotIndex = slotIndex,
                isPlayer = true,
                level = hero.level,
                maxHp = totalHp,
                currentHp = totalHp,
                maxArmor = totalArmor,
                currentArmor = totalArmor,
                atk = totalAtk,
                attackPattern = hero.attackPattern,
                skill = if (hero.level >= 3) hero.activeSkill else null
            )
        }

        val baseTemplates = repository.getInitialHeroCards()
        val targetTotalAtk = GameDataRepository.getStageTargetAtk(effectiveStage.id)
        val rawEnemyAtks = effectiveStage.enemyHeroIds.mapIndexed { slotIndex, heroId ->
            val baseHero = baseTemplates.firstOrNull { it.id == heroId } ?: baseTemplates.first()
            val enemyLevel = effectiveStage.enemyLevels.getOrElse(slotIndex) { 1 }
            val levelMult = 1.0 + (enemyLevel - 1) * 0.16
            (baseHero.baseAtk * levelMult).coerceAtLeast(1.0)
        }
        val sumRawEnemyAtk = rawEnemyAtks.sum()
        val allocatedAtks = IntArray(rawEnemyAtks.size)
        var accumulatedAtk = 0
        for (i in 0 until rawEnemyAtks.size - 1) {
            val allocated = ((rawEnemyAtks[i] / sumRawEnemyAtk) * targetTotalAtk).toInt().coerceAtLeast(1)
            allocatedAtks[i] = allocated
            accumulatedAtk += allocated
        }
        allocatedAtks[rawEnemyAtks.size - 1] = (targetTotalAtk - accumulatedAtk).coerceAtLeast(1)

        val careerPortraitTier = resolveEnemyPortraitTierForCareerStage(effectiveStage.chapterId, effectiveStage.stageNumber)
        val stageEnemyHps = calculateCareerStageEnemyHps(effectiveStage, baseTemplates)

        val enemyBattleCards = effectiveStage.enemyHeroIds.mapIndexed { slotIndex, heroId ->
            val baseHero = baseTemplates.firstOrNull { it.id == heroId } ?: baseTemplates.first()
            val enemyLevel = effectiveStage.enemyLevels.getOrElse(slotIndex) { 1 }
            val stageIndex = ((effectiveStage.chapterId - 1) * 6 + effectiveStage.stageNumber).coerceAtLeast(1)
            val stageProgressionMult = 1.0 + (stageIndex - 1) * 0.04
            val levelMult = (1.0 + (enemyLevel - 1) * 0.16) * (if (effectiveStage.isBoss) 1.25 else 1.0) * 1.25 * stageProgressionMult
            val hp = stageEnemyHps.getOrElse(slotIndex) { (baseHero.baseHp * levelMult).toInt() }
            val armor = if (baseHero.heroType == HeroType.MELEE) (baseHero.baseArmor * levelMult).toInt() else 0
            val atk = allocatedAtks.getOrElse(slotIndex) { (baseHero.baseAtk * levelMult).toInt() }

            val unlockedSkills = baseHero.copy(level = enemyLevel).getUnlockedSkills()
            val activeSkill = if (enemyLevel >= 3) (unlockedSkills.lastOrNull() ?: baseHero.activeSkill) else null

            BattleCard(
                heroId = "enemy_${effectiveStage.id}_$slotIndex",
                name = baseHero.localizedName(),
                title = tr("Ур. $enemyLevel ${baseHero.title}", "Lv. $enemyLevel ${baseHero.localizedTitle()}"),
                heroType = baseHero.heroType,
                rarity = baseHero.rarity,
                element = baseHero.element,
                imageRes = resolveHeroImageForEnemyTier(baseHero.id, careerPortraitTier, baseHero.imageRes),
                slotIndex = slotIndex,
                isPlayer = false,
                level = enemyLevel,
                maxHp = hp,
                currentHp = hp,
                maxArmor = armor,
                currentArmor = armor,
                atk = atk,
                attackPattern = baseHero.attackPattern,
                skill = activeSkill,
                availableSkills = unlockedSkills
            )
        }

        val isRepeat = stage.stars > 0
        val stagePayout = calculateCareerStagePayout(stage)
        val actualGold = stagePayout.gold
        val rewardArtifact = if (isRepeat) null else when (stage.id) {
            101 -> repository.getArtifactCatalog()["art_base_1"]
            104 -> repository.getArtifactCatalog()["art_base_4"]
            201 -> repository.getArtifactCatalog()["art_spec_7"]
            204 -> repository.getArtifactCatalog()["art_spec_1"]
            302 -> {
                val pool = repository.getArtifactCatalog().values.filter { it.rarity == CardRarity.RARE || it.rarity == CardRarity.SPECIAL }
                pool.randomOrNull() ?: repository.getArtifactCatalog()["art_rare_6"]
            }
            402 -> {
                val pool = repository.getArtifactCatalog().values.filter { it.rarity == CardRarity.MYTHIC || it.rarity == CardRarity.RARE }
                pool.randomOrNull() ?: repository.getArtifactCatalog()["art_mythic_11"]
            }
            else -> null
        }

        val chapterIndex = when {
            stage.id >= 400 -> 4
            stage.id >= 300 -> 3
            stage.id >= 200 -> 2
            else -> 1
        }
        _selectedCampaignChapterId.value = chapterIndex

        val campaignBg = stage.battleBgRes ?: com.example.R.drawable.bg_brunhilde

        val profile = _playerProfile.value
        val hasAmuletAlready = when (stage.id) {
            106 -> profile.foundAmulets.contains("amulet_south")
            206 -> profile.foundAmulets.contains("amulet_east")
            306 -> profile.foundAmulets.contains("amulet_north")
            404 -> profile.foundAmulets.contains("amulet_west")
            else -> true
        }

        val foundAmulet = if (!isRepeat && !hasAmuletAlready) {
            when (stage.id) {
                106 -> DiscoveredAmulet(
                    "amulet_south",
                    localizeAmuletName("amulet_south", "амулет Юга"),
                    com.example.R.drawable.img_amulet_south
                )
                206 -> DiscoveredAmulet(
                    "amulet_east",
                    localizeAmuletName("amulet_east", "амулет Востока"),
                    com.example.R.drawable.img_amulet_east
                )
                306 -> DiscoveredAmulet(
                    "amulet_north",
                    localizeAmuletName("amulet_north", "амулет Севера"),
                    com.example.R.drawable.img_amulet_north,
                    extraNote = localizeAmuletNote("amulet_north", "Остался последний!")
                )
                404 -> DiscoveredAmulet(
                    "amulet_west",
                    localizeAmuletName("amulet_west", "амулет Запада"),
                    com.example.R.drawable.img_amulet_west,
                    extraNote = localizeAmuletNote("amulet_west", "Все Амулеты собраны!")
                )
                else -> null
            }
        } else null
        val victoryEventMessage = if (stage.id == 406 && !isRepeat) {
            tr("Вы победили зло и вернули покой в Аврезию!", "You defeated evil and restored peace to Avresia!")
        } else null

        return CampaignBattleSetup(
            effectiveStage = effectiveStage,
            playerBattleCards = playerBattleCards,
            enemyBattleCards = enemyBattleCards,
            actualGold = actualGold,
            rewardGems = stagePayout.gems,
            rewardExp = stagePayout.exp,
            rewardArtifact = rewardArtifact,
            chapterIndex = chapterIndex,
            campaignBg = campaignBg,
            foundAmulet = foundAmulet,
            victoryEventMessage = victoryEventMessage
        )
    }

    fun getCareerAutoBattleCooldownRemainingMs(nowMs: Long = System.currentTimeMillis()): Long {
        return computeCareerAutoBattleCooldownRemainingMs(_playerProfile.value, nowMs)
    }

    fun startCampaignBattle(stage: CampaignStage) {
        val setup = prepareCampaignBattleSetup(stage)
        battleViewModel.startBattle(
            playerCards = setup.playerBattleCards,
            enemyCards = setup.enemyBattleCards,
            enemyName = stage.localizedTitle(),
            enemyTitle = tr(
                "Карьера (Ур. сложности ${stage.recommendedPower})",
                "Career (Power ${stage.recommendedPower})"
            ),
            isArena = false,
            rewardGold = setup.actualGold,
            rewardGems = setup.rewardGems,
            rewardExp = setup.rewardExp,
            rewardArtifact = setup.rewardArtifact,
            campaignChapter = setup.chapterIndex,
            battleBgRes = setup.campaignBg,
            foundAmulet = setup.foundAmulet,
            victoryEventMessage = setup.victoryEventMessage,
            campaignStageId = stage.id,
            isBoss = setup.effectiveStage.isBoss,
            onBattleEnd = { isVictory, state -> handleBattleFinished(isVictory, state) }
        )
    }

    fun startCampaignAutoBattle(stage: CampaignStage, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (stage.clearCount <= 10 || !isDeckValid()) return false
        if (getCareerAutoBattleCooldownRemainingMs(nowMs) > 0L) return false

        _playerProfile.update {
            it.copy(
                lastCareerAutoBattleTimestamp = nowMs,
                maxSeenTimestamp = maxOf(it.maxSeenTimestamp, nowMs)
            )
        }
        repository.savePlayerProfile(_playerProfile.value)

        val setup = prepareCampaignBattleSetup(stage)
        battleViewModel.runInstantAutoBattle(
            playerCards = setup.playerBattleCards,
            enemyCards = setup.enemyBattleCards,
            enemyName = stage.localizedTitle(),
            enemyTitle = tr(
                "Автобой Карьеры (Ур. сложности ${stage.recommendedPower})",
                "Career Auto-Battle (Power ${stage.recommendedPower})"
            ),
            rewardGold = setup.actualGold,
            rewardGems = setup.rewardGems,
            rewardExp = setup.rewardExp,
            rewardArtifact = setup.rewardArtifact,
            campaignChapter = setup.chapterIndex,
            battleBgRes = setup.campaignBg,
            foundAmulet = setup.foundAmulet,
            victoryEventMessage = setup.victoryEventMessage,
            campaignStageId = stage.id,
            isBoss = setup.effectiveStage.isBoss,
            onBattleEnd = { isVictory, state -> handleBattleFinished(isVictory, state) }
        )
        return true
    }

    fun startArenaBattle(bot: ArenaBot) {
        val profile = _playerProfile.value
        if (profile.arenaKeys <= 0) {
            postDropMessage(
                tr(
                    "Недостаточно ключей Арены! Ключи восстанавливаются со временем.",
                    "Not enough Arena keys! Keys regenerate over time."
                )
            )
            triggerHaptic(true)
            return
        }

        // Deduct 1 key upon battle start (win or loss)
        _playerProfile.update { p ->
            val newKeys = (p.arenaKeys - 1).coerceAtLeast(0)
            val newRegen = if (p.arenaKeys == 5 && newKeys < 5) System.currentTimeMillis() else p.lastArenaKeyRegenTimestamp
            p.copy(arenaKeys = newKeys, lastArenaKeyRegenTimestamp = newRegen)
        }
        repository.savePlayerProfile(_playerProfile.value)

        val playerDeckIds = _playerProfile.value.activeDeckHeroIds
        val playerDeckHeroes = playerDeckIds.mapIndexed { index, id ->
            val hero = id?.let { heroId -> _allHeroes.value.firstOrNull { it.id == heroId } }
            hero ?: _allHeroes.value.firstOrNull { it.isUnlocked } ?: _allHeroes.value[index.coerceIn(0, _allHeroes.value.lastIndex)]
        }

        val playerBattleCards = playerDeckHeroes.mapIndexed { slotIndex, hero ->
            val totalAtk = hero.calculateTotalAtk(_inventoryArtifacts.value)
            val totalArmor = hero.calculateTotalArmor(_inventoryArtifacts.value)
            val totalHp = hero.calculateTotalHp(_inventoryArtifacts.value)
            BattleCard(
                heroId = hero.id,
                name = hero.name,
                title = hero.title,
                heroType = hero.heroType,
                rarity = hero.rarity,
                element = hero.element,
                imageRes = hero.imageRes,
                slotIndex = slotIndex,
                isPlayer = true,
                level = hero.level,
                maxHp = totalHp,
                currentHp = totalHp,
                maxArmor = totalArmor,
                currentArmor = totalArmor,
                atk = totalAtk,
                attackPattern = hero.attackPattern,
                skill = if (hero.level >= 3) hero.activeSkill else null
            )
        }

        val baseTemplates = repository.getInitialHeroCards()
        val rawEnemyHps = bot.heroIds.mapIndexed { slotIndex, heroId ->
            val baseHero = baseTemplates.firstOrNull { it.id == heroId } ?: baseTemplates.first()
            val enemyLevel = bot.heroLevels.getOrElse(slotIndex) { 1 }
            val levelMult = 1.0 + (enemyLevel - 1) * 0.16
            (baseHero.baseHp * levelMult).toInt().coerceAtLeast(1)
        }
        val rawEnemyAtks = bot.heroIds.mapIndexed { slotIndex, heroId ->
            val baseHero = baseTemplates.firstOrNull { it.id == heroId } ?: baseTemplates.first()
            val enemyLevel = bot.heroLevels.getOrElse(slotIndex) { 1 }
            val levelMult = 1.0 + (enemyLevel - 1) * 0.16
            (baseHero.baseAtk * levelMult).toInt().coerceAtLeast(1)
        }
        val scaledHps = if (bot.targetTotalHp > 0) {
            allocateProportionalStats(rawEnemyHps, bot.targetTotalHp)
        } else {
            rawEnemyHps.toIntArray()
        }
        val scaledAtks = if (bot.targetTotalAtk > 0) {
            allocateProportionalStats(rawEnemyAtks, bot.targetTotalAtk)
        } else {
            rawEnemyAtks.toIntArray()
        }
        val arenaTotalHp = scaledHps.sum()
        val chaptersForTier = _campaignChapters.value.ifEmpty { repository.getCampaignChapters() }
        val arenaPortraitTier = resolveArenaPortraitTierByChapterHp(arenaTotalHp, chaptersForTier, baseTemplates)

        val enemyBattleCards = bot.heroIds.mapIndexed { slotIndex, heroId ->
            val baseHero = baseTemplates.firstOrNull { it.id == heroId } ?: baseTemplates.first()
            val enemyLevel = bot.heroLevels.getOrElse(slotIndex) { 1 }
            val levelMult = 1.0 + (enemyLevel - 1) * 0.16
            val hp = scaledHps[slotIndex]
            val armor = if (baseHero.heroType == HeroType.MELEE) (baseHero.baseArmor * levelMult).toInt() else 0
            val atk = scaledAtks[slotIndex]

            BattleCard(
                heroId = "bot_${bot.name}_$slotIndex",
                name = baseHero.localizedName(),
                title = tr("Ур. $enemyLevel ${baseHero.name}", "Lv. $enemyLevel ${baseHero.localizedName()}"),
                heroType = baseHero.heroType,
                rarity = baseHero.rarity,
                element = baseHero.element,
                imageRes = resolveHeroImageForEnemyTier(baseHero.id, arenaPortraitTier, baseHero.imageRes),
                slotIndex = slotIndex,
                isPlayer = false,
                level = enemyLevel,
                maxHp = hp,
                currentHp = hp,
                maxArmor = armor,
                currentArmor = armor,
                atk = atk,
                attackPattern = baseHero.attackPattern,
                skill = if (enemyLevel >= 3) baseHero.activeSkill else null
            )
        }

        val arenaBg = ARENA_BATTLE_BACKGROUNDS.random()
        val actualRewardGold = bot.rewardGold ?: ((350 + (bot.rating / 3)) * 0.5f).toInt()

        battleViewModel.startBattle(
            playerCards = playerBattleCards,
            enemyCards = enemyBattleCards,
            enemyName = localizeHeroNameByIdOrRu(null, bot.name),
            enemyTitle = "${localizeBotTitle(bot.title)} (${bot.rating} 🏆)",
            isArena = true,
            rewardGold = actualRewardGold,
            rewardGems = bot.rewardGems,
            rewardExp = bot.rewardExp,
            rewardArtifact = null,
            campaignChapter = 0,
            battleBgRes = arenaBg,
            onBattleEnd = { isVictory, state -> handleBattleFinished(isVictory, state) }
        )
    }

    private fun handleBattleFinished(isVictory: Boolean, state: BattleState) {
        if (state.isColiseum) {
            handleColiseumBattleFinished(isVictory, state)
            return
        }

        if (!isVictory) {
            val defeatedCount = state.enemyCards.count { !it.isAlive }
            if (defeatedCount > 0) {
                advanceDailyQuestProgress(QuestType.DEFEAT_ENEMIES, defeatedCount)
            }
            if (state.isTower) {
                val nowMs = activeTowerSessionNowMs ?: maxOf(System.currentTimeMillis(), _playerProfile.value.maxSeenTimestamp)
                activeTowerSessionNowMs = null
                val todayStr = formatColiseumCalendarDate(nowMs)
                val savedEnemyHps = (0..3).map { slot ->
                    val card = state.enemyCards.firstOrNull { it.slotIndex == slot }
                    if (card != null && card.isAlive && card.currentHp > 0) card.currentHp else 0
                }
                _playerProfile.update { p ->
                    val prevCleared = if (p.towerProgressDate == todayStr) p.towerClearedFloorToday else 0
                    val prevStars = if (p.towerProgressDate == todayStr) p.towerFloorStarsToday else emptyList()
                    val prevRoofCleared = if (p.towerProgressDate == todayStr) p.towerRoofClearedToday else false
                    val prevRoofStars = if (p.towerProgressDate == todayStr) p.towerRoofStarsToday else 0
                    val prevUsed = if (p.towerProgressDate == todayStr) p.towerAttemptsUsedToday else 0
                    p.copy(
                        battlesLost = p.battlesLost + 1,
                        towerProgressDate = todayStr,
                        towerClearedFloorToday = prevCleared,
                        towerFloorStarsToday = prevStars,
                        towerRoofClearedToday = prevRoofCleared,
                        towerRoofStarsToday = prevRoofStars,
                        towerAttemptsUsedToday = prevUsed,
                        towerDamagedFloor = state.towerFloor,
                        towerSavedEnemyHps = savedEnemyHps,
                        maxSeenTimestamp = maxOf(p.maxSeenTimestamp, nowMs)
                    )
                }
                repository.savePlayerProfile(_playerProfile.value)
                val attemptsLeft = computeTowerRemainingAttempts(_playerProfile.value, nowMs)
                postDropMessage(
                    tr(
                        "Поражение в Башне! Урон врагов сохранён. Осталось попыток: $attemptsLeft/$TOWER_DAILY_ATTEMPT_LIMIT.",
                        "Tower defeat! Enemy damage saved. Attempts remaining: $attemptsLeft/$TOWER_DAILY_ATTEMPT_LIMIT."
                    )
                )
                triggerHaptic(true)
            } else {
                _playerProfile.update { it.copy(battlesLost = it.battlesLost + 1) }
                repository.savePlayerProfile(_playerProfile.value)
                if (state.isArena) {
                    val currentKeys = _playerProfile.value.arenaKeys
                    postDropMessage(
                        tr(
                            "Поражение на Арене! Ключ потрачен (Осталось: $currentKeys/5).",
                            "Arena defeat! Key spent (Remaining: $currentKeys/5)."
                        )
                    )
                    refreshArenaBots()
                    triggerHaptic(true)
                }
            }
            return
        }

        val oldLevel = _playerProfile.value.level
        val oldLastCelebrated = _playerProfile.value.lastCelebratedLevel
        var newLvl = oldLevel
        var levelUpGoldBonus = 0
        var levelUpGemsBonus = 0

        _playerProfile.update { profile ->
            var tempExp = profile.exp + state.rewardExp
            var tempMaxExp = profile.maxExp
            var tempLevel = profile.level
            while (tempExp >= tempMaxExp) {
                tempExp -= tempMaxExp
                tempLevel += 1
                tempMaxExp = (tempMaxExp * 1.4).toInt()
                levelUpGoldBonus += calculateLevelUpGoldReward(tempLevel)
                levelUpGemsBonus += calculateLevelUpGemsReward(tempLevel)
            }
            newLvl = tempLevel
            val newTrophies = if (state.isArena) profile.trophies + 28 else profile.trophies

            val newAmulets = profile.foundAmulets.toMutableList()
            state.foundAmulet?.let { amulet ->
                if (!newAmulets.contains(amulet.id)) {
                    newAmulets.add(amulet.id)
                }
            }

            profile.copy(
                gold = profile.gold + state.rewardGold + levelUpGoldBonus,
                gems = profile.gems + state.rewardGems + levelUpGemsBonus,
                exp = tempExp,
                maxExp = tempMaxExp,
                level = tempLevel,
                trophies = newTrophies,
                battlesWon = profile.battlesWon + 1,
                foundAmulets = newAmulets
            )
        }

        if (newLvl > oldLevel && newLvl > oldLastCelebrated) {
            _levelUpRewardPopup.value = LevelUpRewardPopup(
                newLevel = newLvl,
                goldReward = levelUpGoldBonus,
                gemsReward = levelUpGemsBonus
            )
        }

        state.rewardArtifact?.let { art ->
            val uniqueArt = art.copy(id = "${art.id}_${System.currentTimeMillis()}_${Random.nextInt(1000)}")
            _inventoryArtifacts.update { it + uniqueArt }
            repository.saveArtifacts(_inventoryArtifacts.value)
            postDropMessage(
                tr(
                    "Добыт редкий артефакт в карьере: ${uniqueArt.name}!",
                    "Earned artifact in Career: ${uniqueArt.localizedName()}!"
                )
            )
        }

        var clearedStageId: Int? = null
        var clearedStagePayout: CareerStagePayout? = null
        var earnedNewStars = 0
        var clearedBossStage = false
        if (state.isTower) {
            val nowMs = activeTowerSessionNowMs ?: maxOf(System.currentTimeMillis(), _playerProfile.value.maxSeenTimestamp)
            activeTowerSessionNowMs = null
            val todayStr = formatColiseumCalendarDate(nowMs)
            val survivedHeroes = state.playerCards.count { it.isAlive }
            val starsEarned = survivedHeroes.coerceIn(1, 4)
            val prevCleared = computeTowerClearedFloorToday(_playerProfile.value, nowMs)
            val prevStarsList = computeTowerFloorStarsToday(_playerProfile.value, nowMs).toMutableList()
            val prevRoofCleared = isTowerRoofClearedToday(_playerProfile.value, nowMs)
            val prevRoofStars = computeTowerRoofStarsToday(_playerProfile.value, nowMs)

            if (state.towerFloor == TOWER_ROOF_FLOOR) {
                _playerProfile.update { p ->
                    val refundedUsed = if (p.towerProgressDate == todayStr) {
                        (p.towerAttemptsUsedToday - 1).coerceAtLeast(0)
                    } else 0
                    p.copy(
                        towerProgressDate = todayStr,
                        towerClearedFloorToday = maxOf(prevCleared, TOWER_TOTAL_FLOORS),
                        towerFloorStarsToday = prevStarsList,
                        towerRoofClearedToday = true,
                        towerRoofStarsToday = maxOf(prevRoofStars, starsEarned),
                        towerDamagedFloor = 0,
                        towerSavedEnemyHps = emptyList(),
                        towerAttemptsUsedToday = refundedUsed,
                        towerTotalWins = p.towerTotalWins + 1,
                        maxSeenTimestamp = maxOf(p.maxSeenTimestamp, nowMs)
                    )
                }
                if (!prevRoofCleared) {
                    val eligibleHeroes = _allHeroes.value.filter { (!it.isSecret || it.isUnlocked) && it.level < it.maxUpgradeLevel() }
                    val rewardedHero = selectChestShardHero(eligibleHeroes, CardRarity.RARE)
                        ?: eligibleHeroes.randomOrNull()
                    if (rewardedHero != null) {
                        val bonusShards = 20
                        _allHeroes.update { list ->
                            list.map { if (it.id == rewardedHero.id) it.copy(shards = it.shards + bonusShards) else it }
                        }
                        postDropMessage(
                            tr(
                                "👑 Крыша Башни покорена! +$bonusShards осколков ${rewardedHero.name}!",
                                "👑 Tower Roof conquered! +$bonusShards shards for ${rewardedHero.localizedName()}!"
                            ),
                            append = true
                        )
                    }
                }
            } else {
                val floorIdx = (state.towerFloor - 1).coerceIn(0, TOWER_TOTAL_FLOORS - 1)
                val newCleared = maxOf(prevCleared, state.towerFloor).coerceIn(1, TOWER_TOTAL_FLOORS)
                prevStarsList[floorIdx] = maxOf(prevStarsList[floorIdx], starsEarned)
                _playerProfile.update { p ->
                    val refundedUsed = if (p.towerProgressDate == todayStr) {
                        (p.towerAttemptsUsedToday - 1).coerceAtLeast(0)
                    } else 0
                    p.copy(
                        towerProgressDate = todayStr,
                        towerClearedFloorToday = newCleared,
                        towerFloorStarsToday = prevStarsList,
                        towerRoofClearedToday = prevRoofCleared,
                        towerRoofStarsToday = prevRoofStars,
                        towerDamagedFloor = 0,
                        towerSavedEnemyHps = emptyList(),
                        towerAttemptsUsedToday = refundedUsed,
                        towerTotalWins = p.towerTotalWins + 1,
                        maxSeenTimestamp = maxOf(p.maxSeenTimestamp, nowMs)
                    )
                }
                if (state.towerFloor >= TOWER_TOTAL_FLOORS && newCleared > prevCleared) {
                    val eligibleHeroes = _allHeroes.value.filter { (!it.isSecret || it.isUnlocked) && it.level < it.maxUpgradeLevel() }
                    val rewardedHero = selectChestShardHero(eligibleHeroes, CardRarity.RARE)
                        ?: eligibleHeroes.randomOrNull()
                    if (rewardedHero != null) {
                        val bonusShards = 10
                        _allHeroes.update { list ->
                            list.map { if (it.id == rewardedHero.id) it.copy(shards = it.shards + bonusShards) else it }
                        }
                        postDropMessage(
                            tr(
                                "🏰 Вершина Башни покорена! +$bonusShards осколков ${rewardedHero.name}!",
                                "🏰 Tower Summit conquered! +$bonusShards shards for ${rewardedHero.localizedName()}!"
                            ),
                            append = true
                        )
                    }
                }
            }
        } else if (!state.isArena) {
            val survivedHeroes = state.playerCards.count { it.isAlive }
            val starsEarned = survivedHeroes.coerceIn(1, 4)

            _campaignChapters.update { chapters ->
                chapters.map { chapter ->
                    val stages = chapter.stages.map { stage ->
                        if ((state.campaignStageId != null && stage.id == state.campaignStageId) || stage.title == state.enemyName) {
                            clearedStageId = stage.id
                            clearedStagePayout = calculateCareerStagePayout(stage)
                            if (stage.isBoss) clearedBossStage = true
                            val previousStars = stage.stars
                            earnedNewStars = (starsEarned - previousStars).coerceAtLeast(0)
                            val isImprovedRun = previousStars in 1..3 && starsEarned > previousStars
                            if (isImprovedRun) {
                                if (stage.clearCount <= 5) {
                                    // Replayed with more stars within normal repeat window -> award full initial gold
                                    val bonusGold = stage.rewardGold - (stage.rewardGold * 0.55f).toInt()
                                    _playerProfile.update { p -> p.copy(gold = p.gold + bonusGold) }
                                }
                                stage.rewardArtifactId?.let { artId ->
                                    val art = repository.getArtifactCatalog()[artId]
                                    if (art != null) {
                                        val uniqueArt = art.copy(id = "${art.id}_${System.currentTimeMillis()}_${Random.nextInt(1000)}")
                                        _inventoryArtifacts.update { it + uniqueArt }
                                        repository.saveArtifacts(_inventoryArtifacts.value)
                                        postDropMessage(
                                            tr(
                                                "Улучшение до $starsEarned звёзд! Получен артефакт: ${uniqueArt.name} и полное золото!",
                                                "Improved to $starsEarned stars! Received artifact: ${uniqueArt.localizedName()} and full coins!"
                                            ),
                                            append = true
                                        )
                                    }
                                }
                            }
                            val newStars = maxOf(previousStars, starsEarned)
                            stage.copy(stars = newStars, clearCount = stage.clearCount + 1)
                        } else stage
                    }
                    chapter.copy(stages = stages)
                }
            }
            val allStagesAfter = _campaignChapters.value.flatMap { it.stages }
            val starsMap = allStagesAfter.associate { it.id to it.stars }
            repository.saveCampaignStars(starsMap)
            val clearCountsMap = allStagesAfter.filter { it.clearCount > 0 }.associate { it.id to it.clearCount }
            repository.saveCampaignClearCounts(clearCountsMap)

            // Stages 1 and 4 in each chapter award shards of a hero of corresponding rarity upon victory
            val shardRarity = when (clearedStageId) {
                101, 104 -> CardRarity.BASIC
                201, 204 -> CardRarity.RARE
                301, 304 -> CardRarity.SPECIAL
                else -> null
            }
            if (shardRarity != null) {
                val awardedShards = clearedStagePayout?.shards ?: 5
                val matchingHeroes = _allHeroes.value.filter {
                    (!it.isSecret || it.isUnlocked) && it.effectiveDropRarity() == shardRarity && it.level < it.maxUpgradeLevel()
                }
                val rewardedHero = matchingHeroes.randomOrNull()
                if (rewardedHero != null) {
                    _allHeroes.update { list ->
                        list.map { if (it.id == rewardedHero.id) it.copy(shards = it.shards + awardedShards) else it }
                    }
                    val shardText = tr(
                        "🎉 Награда за победу: +$awardedShards осколков ${rewardedHero.name}!",
                        "🎉 Victory reward: +$awardedShards shards for ${rewardedHero.localizedName()}!"
                    )
                    postDropMessage(shardText, append = true)
                }
            }

            val storyAmuletMessage = when (clearedStageId) {
                106 -> if (state.foundAmulet != null) tr("🏆 Вы получили амулет Юга!", "🏆 You obtained the Amulet of the South!") else null
                206 -> if (state.foundAmulet != null) tr("🏆 Вы получили амулет Востока!", "🏆 You obtained the Amulet of the East!") else null
                306 -> if (state.foundAmulet != null) tr("🏆 Вы получили амулет Севера! Остался последний!", "🏆 You obtained the Amulet of the North! One left!") else null
                404 -> if (state.foundAmulet != null) tr("✨ Все Амулеты собраны!", "✨ All Amulets collected!") else null
                406 -> if (!state.victoryEventMessage.isNullOrBlank()) tr("👑 Вы победили зло и вернули покой в Аврезию!", "👑 You defeated evil and restored peace to Avresia!") else null
                else -> null
            }
            if (storyAmuletMessage != null) {
                postDropMessage(storyAmuletMessage, append = true)
            }
        } else {
            refreshArenaBots()
        }

        updateCampaignStageLocks(newLvl)
        checkCampaign100Completion()
        checkAndUnlockHeroes(newLvl, clearedStageId)
        repository.savePlayerProfile(_playerProfile.value)
        repository.saveHeroesState(_allHeroes.value)

        // Update daily quests progress after victory & unlocks/stars are committed
        advanceDailyQuestProgress(QuestType.WIN_ANY_BATTLES, 1)
        if (state.isArena) {
            advanceDailyQuestProgress(QuestType.WIN_ARENA_BATTLES, 1)
            if (state.playerCards.isNotEmpty() && state.playerCards.all { it.heroType == HeroType.RANGED }) {
                advanceDailyQuestProgress(QuestType.WIN_ARENA_RANGED_ONLY, 1)
            }
        } else if (!state.isTower) {
            advanceDailyQuestProgress(QuestType.WIN_CAREER_BATTLES, 1)
            if (clearedBossStage) {
                advanceDailyQuestProgress(QuestType.WIN_CAREER_BOSS, 1)
            }
            if (earnedNewStars > 0) {
                advanceDailyQuestProgress(QuestType.EARN_CAREER_STARS, earnedNewStars)
            }
        }
        val defeatedEnemies = state.enemyCards.count { !it.isAlive }.coerceAtLeast(state.enemyCards.size)
        advanceDailyQuestProgress(QuestType.DEFEAT_ENEMIES, defeatedEnemies)
        if (state.playerSkillsUsedCount > 0) {
            advanceDailyQuestProgress(QuestType.CAST_SKILLS, state.playerSkillsUsedCount)
        }
        if (state.playerCards.isNotEmpty() && state.playerCards.all { it.isAlive }) {
            advanceDailyQuestProgress(QuestType.WIN_NO_LOSSES, 1)
        }
        if (state.playerCards.any { it.heroType == HeroType.HYBRID }) {
            advanceDailyQuestProgress(QuestType.WIN_WITH_HYBRID, 1)
        }
        if (state.playerCards.count { it.heroType == HeroType.HYBRID } >= 2) {
            advanceDailyQuestProgress(QuestType.WIN_TWO_HYBRID, 1)
        }
        if (state.playerCards.count { it.heroType == HeroType.RANGED } >= 4) {
            advanceDailyQuestProgress(QuestType.WIN_FOUR_RANGED, 1)
        }
        state.playerCards.map { it.element }.distinct().forEach { elem ->
            advanceDailyQuestProgress(QuestType.WIN_WITH_ELEMENT, 1, requiredElement = elem)
        }
        reconcileInfeasibleUncompletedQuests()
    }

    // ----------------------------------------------------
    // COLISEUM (КОЛИЗЕЙ) ENDLESS ARENA MODE
    // ----------------------------------------------------

    fun getColiseumRemainingEntries(nowMs: Long = System.currentTimeMillis()): Int {
        return computeColiseumRemainingEntries(_playerProfile.value, nowMs)
    }

    fun getColiseumCooldownRemainingMs(nowMs: Long = System.currentTimeMillis()): Long {
        return computeColiseumCooldownRemainingMs(_playerProfile.value, nowMs)
    }

    fun openColiseumScreen(nowMs: Long = System.currentTimeMillis()) {
        val profile = _playerProfile.value
        if (profile.level < COLISEUM_MIN_PLAYER_LEVEL) {
            postDropMessage(
                tr(
                    "Доступ в Колизей только с 5 уровня профиля!",
                    "Coliseum unlocks at Profile Level 5!"
                )
            )
            triggerHaptic(true)
            return
        }
        if (getColiseumRemainingEntries(nowMs) <= 0) {
            postDropMessage(
                tr(
                    "В Колизей можно зайти только 2 раза в сутки!",
                    "You can enter the Coliseum only 2 times per day!"
                )
            )
            triggerHaptic(true)
            return
        }
        val shouldShowTutorial = !profile.isColiseumTutorialCompleted
        val updatedMaxSeen = maxOf(profile.maxSeenTimestamp, nowMs)
        if (updatedMaxSeen > profile.maxSeenTimestamp || shouldShowTutorial) {
            _playerProfile.update {
                it.copy(
                    maxSeenTimestamp = updatedMaxSeen,
                    isColiseumTutorialCompleted = true
                )
            }
            repository.savePlayerProfile(_playerProfile.value)
        }
        _coliseumDeckHeroIds.value = listOf(null, null, null, null)
        _coliseumRunState.value = null
        _coliseumBattlePreview.value = null
        _isColiseumPreviewVisible.value = false
        _coliseumMilestoneBonusOffer.value = null
        _showColiseumTutorial.value = shouldShowTutorial
        _isColiseumOpen.value = true
    }

    fun closeColiseumToMenu(nowMs: Long = System.currentTimeMillis()) {
        val hadActiveRun = _coliseumRunState.value != null
        if (hadActiveRun) {
            _playerProfile.update {
                it.copy(
                    lastColiseumTimestamp = if (it.lastColiseumTimestamp <= 0L) nowMs else it.lastColiseumTimestamp,
                    maxSeenTimestamp = maxOf(it.maxSeenTimestamp, nowMs)
                )
            }
            repository.savePlayerProfile(_playerProfile.value)
        }
        _coliseumRunState.value = null
        _coliseumBattlePreview.value = null
        _isColiseumPreviewVisible.value = false
        _showColiseumTutorial.value = false
        _coliseumMilestoneBonusOffer.value = null
        _coliseumDeckHeroIds.value = listOf(null, null, null, null)
        _isColiseumOpen.value = false
    }

    fun completeColiseumTutorial() {
        _showColiseumTutorial.value = false
        if (!_playerProfile.value.isColiseumTutorialCompleted) {
            _playerProfile.update { it.copy(isColiseumTutorialCompleted = true) }
            repository.savePlayerProfile(_playerProfile.value)
        }
    }

    fun assignHeroToColiseumSlot(slotIndex: Int, heroId: String?) {
        if (slotIndex !in 0..3) return
        if (_coliseumRunState.value != null) return

        val hero = heroId?.let { id -> _allHeroes.value.firstOrNull { it.id == id } }
        if (hero != null && !hero.isUnlocked) {
            showLockedHeroMessage(
                tr(
                    "Нельзя взять в колоду скрытую карту!",
                    "Cannot add a locked card to the Coliseum deck!"
                )
            )
            return
        }
        if (hero != null && slotIndex in 2..3 && hero.heroType == HeroType.MELEE) {
            showLockedHeroMessage(
                tr(
                    "Ближний бой только в 1 ряду!",
                    "Melee heroes can only be placed in row 1!"
                )
            )
            return
        }
        val currentDeck = _coliseumDeckHeroIds.value.toMutableList()
        while (currentDeck.size < 4) currentDeck.add(null)
        if (heroId != null && currentDeck.withIndex().any { it.index != slotIndex && it.value == heroId }) {
            showLockedHeroMessage(
                tr(
                    "Нельзя ставить дубликаты в колоду Колизея!",
                    "Duplicates are not allowed in the Coliseum deck!"
                )
            )
            return
        }
        currentDeck[slotIndex] = heroId
        _coliseumDeckHeroIds.value = currentDeck
        triggerHaptic(false)
    }

    fun startColiseumRunAndPreviewFirstBattle() {
        val deckIds = _coliseumDeckHeroIds.value
        if (!isDeckValid(deckIds)) {
            showLockedHeroMessage(
                tr(
                    "Заполните все 4 слота колоды Колизея уникальными героинями!",
                    "Fill all 4 Coliseum deck slots with unique heroines!"
                )
            )
            return
        }
        val deckHeroes = deckIds.mapNotNull { id -> _allHeroes.value.firstOrNull { it.id == id } }
        if (deckHeroes.size != 4) return

        val playerCards = deckHeroes.mapIndexed { slotIndex, hero ->
            val totalAtk = hero.calculateTotalAtk(_inventoryArtifacts.value)
            val totalArmor = hero.calculateTotalArmor(_inventoryArtifacts.value)
            val totalHp = hero.calculateTotalHp(_inventoryArtifacts.value)
            BattleCard(
                heroId = hero.id,
                name = hero.name,
                title = hero.title,
                heroType = hero.heroType,
                rarity = hero.rarity,
                element = hero.element,
                imageRes = hero.imageRes,
                slotIndex = slotIndex,
                isPlayer = true,
                level = hero.level,
                maxHp = totalHp,
                currentHp = totalHp,
                maxArmor = totalArmor,
                currentArmor = totalArmor,
                atk = totalAtk,
                attackPattern = hero.attackPattern,
                skill = if (hero.level >= 3) hero.activeSkill else null
            )
        }
        val initialDeckAtk = playerCards.sumOf { it.atk }
        val initialDeckHp = playerCards.sumOf { it.maxHp }
        val avgDeckLevel = (playerCards.sumOf { it.level } / 4.0).toInt().coerceAtLeast(1)

        _coliseumRunState.value = ColiseumRunState(
            battleNumber = 1,
            accumulatedGold = 0,
            accumulatedGems = 0,
            initialDeckAtk = initialDeckAtk,
            initialDeckHp = initialDeckHp,
            avgDeckLevel = avgDeckLevel,
            playerCards = playerCards
        )
        prepareColiseumBattlePreview(
            battleNumber = 1,
            initialDeckAtk = initialDeckAtk,
            initialDeckHp = initialDeckHp,
            avgDeckLevel = avgDeckLevel
        )
    }

    fun cancelColiseumBattle1Preview() {
        if (_coliseumBattlePreview.value?.battleNumber == 1) {
            _isColiseumPreviewVisible.value = false
            _coliseumBattlePreview.value = null
            _coliseumRunState.value = null
        }
    }

    fun dismissColiseumBattlePreview() {
        val preview = _coliseumBattlePreview.value ?: return
        if (preview.battleNumber == 1) {
            cancelColiseumBattle1Preview()
        } else {
            _isColiseumPreviewVisible.value = false
        }
    }

    fun reopenColiseumBattlePreview() {
        if (_coliseumBattlePreview.value != null) {
            _isColiseumPreviewVisible.value = true
        }
    }

    private fun prepareColiseumBattlePreview(
        battleNumber: Int,
        initialDeckAtk: Int,
        initialDeckHp: Int,
        avgDeckLevel: Int
    ) {
        val unlockedSecretHeroIds = _allHeroes.value
            .filter { it.isSecret && it.isUnlocked }
            .map { it.id }
            .toSet()
        val bot = repository.generateColiseumOpponent(
            battleNumber = battleNumber,
            playerDeckAtk = initialDeckAtk,
            playerDeckHp = initialDeckHp,
            avgHeroLevel = avgDeckLevel,
            unlockedSecretHeroIds = unlockedSecretHeroIds
        )
        val localizedOpponentName = localizeHeroNameByIdOrRu(null, bot.name)
        _coliseumBattlePreview.value = ColiseumBattlePreview(
            battleNumber = battleNumber,
            opponentName = localizedOpponentName,
            opponentBot = bot,
            rewardGold = bot.rewardGold ?: repository.calculateColiseumRewards(battleNumber).first,
            rewardGems = bot.rewardGems
        )
        // All Coliseum waves (including Battle 1) open directly on the unified Transition Screen without a popup overlay.
        _isColiseumPreviewVisible.value = false
    }

    fun confirmStartPreviewedColiseumBattle(nowMs: Long = System.currentTimeMillis()) {
        val preview = _coliseumBattlePreview.value ?: return
        val run = _coliseumRunState.value ?: return
        _isColiseumPreviewVisible.value = false
        _coliseumBattlePreview.value = null

        if (preview.battleNumber == 1) {
            val todayStr = formatColiseumCalendarDate(nowMs)
            _playerProfile.update { p ->
                val existingTodayCount = when {
                    p.coliseumEntriesDate == todayStr -> p.coliseumEntriesUsedToday
                    p.coliseumEntriesDate == null &&
                        p.lastColiseumTimestamp > 0L &&
                        formatColiseumCalendarDate(p.lastColiseumTimestamp) == todayStr -> 1
                    else -> 0
                }
                p.copy(
                    lastColiseumTimestamp = nowMs,
                    coliseumEntriesDate = todayStr,
                    coliseumEntriesUsedToday = (existingTodayCount + 1).coerceAtMost(COLISEUM_DAILY_LIMIT),
                    maxSeenTimestamp = maxOf(p.maxSeenTimestamp, nowMs)
                )
            }
            repository.savePlayerProfile(_playerProfile.value)
        }

        val playerBattleCards = run.playerCards.map { card ->
            val alive = card.isAlive && card.currentHp > 0
            card.copy(
                currentHp = if (alive) card.currentHp.coerceIn(1, card.maxHp) else 0,
                currentArmor = if (alive) card.maxArmor else 0,
                skillEnergy = 0,
                isAlive = alive,
                hasActedThisRound = false,
                isTargetSelected = false,
                poisonRounds = 0,
                burnRounds = 0,
                isWeakened = false,
                weakenedRounds = 0,
                isFrozen = false,
                freezeRounds = 0,
                isStunned = false,
                stunRounds = 0,
                isBlinded = false,
                blindRounds = 0,
                isCharmed = false,
                charmRounds = 0,
                isSlowed = false,
                slowRounds = 0,
                poisonAppliedRound = 0,
                burnAppliedRound = 0,
                atkBoostPercent = 0,
                atkBoostRounds = 0,
                poisonFixedDamage = 0,
                isHealingBlocked = false
            )
        }

        val bot = preview.opponentBot
        val baseTemplates = repository.getInitialHeroCards()
        val rawEnemyHps = bot.heroIds.mapIndexed { slotIndex, heroId ->
            val baseHero = baseTemplates.firstOrNull { it.id == heroId } ?: baseTemplates.first()
            val enemyLevel = bot.heroLevels.getOrElse(slotIndex) { 1 }
            val levelMult = 1.0 + (enemyLevel - 1) * 0.16
            (baseHero.baseHp * levelMult).toInt().coerceAtLeast(1)
        }
        val rawEnemyAtks = bot.heroIds.mapIndexed { slotIndex, heroId ->
            val baseHero = baseTemplates.firstOrNull { it.id == heroId } ?: baseTemplates.first()
            val enemyLevel = bot.heroLevels.getOrElse(slotIndex) { 1 }
            val levelMult = 1.0 + (enemyLevel - 1) * 0.16
            (baseHero.baseAtk * levelMult).toInt().coerceAtLeast(1)
        }
        val scaledHps = if (bot.targetTotalHp > 0) allocateProportionalStats(rawEnemyHps, bot.targetTotalHp) else rawEnemyHps.toIntArray()
        val scaledAtks = if (bot.targetTotalAtk > 0) allocateProportionalStats(rawEnemyAtks, bot.targetTotalAtk) else rawEnemyAtks.toIntArray()

        val coliseumTotalHp = scaledHps.sum()
        val chaptersForTier = _campaignChapters.value.ifEmpty { repository.getCampaignChapters() }
        val portraitTier = resolveArenaPortraitTierByChapterHp(coliseumTotalHp, chaptersForTier, baseTemplates)

        val scaleMult = repository.calculateColiseumScalingMultiplier(preview.battleNumber)
        val enemyBattleCards = bot.heroIds.mapIndexed { slotIndex, heroId ->
            val baseHero = baseTemplates.firstOrNull { it.id == heroId } ?: baseTemplates.first()
            val enemyLevel = bot.heroLevels.getOrElse(slotIndex) { 1 }
            val levelMult = 1.0 + (enemyLevel - 1) * 0.16
            val hp = scaledHps[slotIndex]
            val armor = if (baseHero.heroType == HeroType.MELEE) {
                (baseHero.baseArmor * levelMult * scaleMult).toInt().coerceAtLeast(0)
            } else 0
            val atk = scaledAtks[slotIndex]
            val unlockedSkills = baseHero.copy(level = enemyLevel).getUnlockedSkills()
            val activeSkill = if (enemyLevel >= 3) (unlockedSkills.lastOrNull() ?: baseHero.activeSkill) else null

            BattleCard(
                heroId = "coliseum_${preview.battleNumber}_$slotIndex",
                name = baseHero.localizedName(),
                title = tr("Ур. $enemyLevel ${baseHero.name}", "Lv. $enemyLevel ${baseHero.localizedName()}"),
                heroType = baseHero.heroType,
                rarity = baseHero.rarity,
                element = baseHero.element,
                imageRes = resolveHeroImageForEnemyTier(baseHero.id, portraitTier, baseHero.imageRes),
                slotIndex = slotIndex,
                isPlayer = false,
                level = enemyLevel,
                maxHp = hp,
                currentHp = hp,
                maxArmor = armor,
                currentArmor = armor,
                atk = atk,
                attackPattern = baseHero.attackPattern,
                skill = activeSkill,
                availableSkills = unlockedSkills
            )
        }

        battleViewModel.startBattle(
            playerCards = playerBattleCards,
            enemyCards = enemyBattleCards,
            enemyName = preview.opponentName,
            enemyTitle = tr("Колизей • Битва №${preview.battleNumber}", "Coliseum • Battle #${preview.battleNumber}"),
            isArena = false,
            rewardGold = preview.rewardGold,
            rewardGems = preview.rewardGems,
            rewardExp = 0,
            rewardArtifact = null,
            campaignChapter = 3,
            battleBgRes = ARENA_BATTLE_BACKGROUNDS.random(),
            initialPlayerMana = (1 + run.bonusStartMana).coerceAtLeast(1),
            isColiseum = true,
            coliseumBattleNumber = preview.battleNumber,
            coliseumAccumulatedGold = run.accumulatedGold,
            coliseumAccumulatedGems = run.accumulatedGems,
            isBoss = preview.battleNumber % 5 == 0,
            onBattleEnd = { isVictory, endState -> handleBattleFinished(isVictory, endState) }
        )
    }

    private fun handleColiseumBattleFinished(isVictory: Boolean, state: BattleState) {
        val run = _coliseumRunState.value
        val defeatedEnemies = state.enemyCards.count { !it.isAlive }
        if (defeatedEnemies > 0) {
            advanceDailyQuestProgress(QuestType.DEFEAT_ENEMIES, defeatedEnemies)
        }

        if (!isVictory) {
            val baseGold = run?.accumulatedGold ?: state.coliseumAccumulatedGold
            val baseGems = run?.accumulatedGems ?: state.coliseumAccumulatedGems
            val potentialGold = baseGold + state.rewardGold
            val potentialGems = baseGems + state.rewardGems
            val consolationGold = kotlin.math.round(potentialGold * 0.20).toInt().coerceAtLeast(if (potentialGold > 0) 1 else 0)
            val consolationGems = kotlin.math.round(potentialGems * 0.20).toInt().coerceAtLeast(if (potentialGems > 0) 1 else 0)

            val nowMs = System.currentTimeMillis()
            _playerProfile.update { p ->
                p.copy(
                    gold = p.gold + consolationGold,
                    gems = p.gems + consolationGems,
                    coliseumLost = p.coliseumLost + 1,
                    battlesLost = p.battlesLost + 1,
                    lastColiseumTimestamp = if (p.lastColiseumTimestamp <= 0L) nowMs else p.lastColiseumTimestamp,
                    maxSeenTimestamp = maxOf(p.maxSeenTimestamp, nowMs)
                )
            }
            repository.savePlayerProfile(_playerProfile.value)

            battleViewModel.updateColiseumBattleOutcomeTotals(
                accumulatedGold = 0,
                accumulatedGems = 0,
                consolationGold = consolationGold,
                consolationGems = consolationGems
            )
            _coliseumRunState.value = null
            _coliseumBattlePreview.value = null
            _coliseumMilestoneBonusOffer.value = null
            _coliseumDeckHeroIds.value = listOf(null, null, null, null)
            triggerHaptic(true)
            postDropMessage(
                tr(
                    "Поражение в Колизее! Утешительный приз (20%): +$consolationGold 🪙 и +$consolationGems 💎",
                    "Coliseum defeat! Consolation prize (20%): +$consolationGold 🪙 and +$consolationGems 💎"
                )
            )
            return
        }

        // Victory in Coliseum battle N: persist HP & alive/dead state, accumulate coins & gems
        val updatedSnapshots = state.playerCards.sortedBy { it.slotIndex }.map { card ->
            val alive = card.isAlive && card.currentHp > 0
            card.copy(
                currentHp = if (alive) card.currentHp.coerceIn(1, card.maxHp) else 0,
                isAlive = alive,
                currentArmor = if (alive) card.maxArmor else 0,
                hasActedThisRound = false,
                poisonRounds = 0,
                burnRounds = 0,
                isWeakened = false,
                weakenedRounds = 0,
                isFrozen = false,
                freezeRounds = 0,
                isStunned = false,
                stunRounds = 0,
                isBlinded = false,
                blindRounds = 0,
                isCharmed = false,
                charmRounds = 0,
                isSlowed = false,
                slowRounds = 0,
                atkBoostPercent = 0,
                atkBoostRounds = 0,
                poisonFixedDamage = 0,
                isHealingBlocked = false
            )
        }
        val newAccGold = (run?.accumulatedGold ?: state.coliseumAccumulatedGold) + state.rewardGold
        val newAccGems = (run?.accumulatedGems ?: state.coliseumAccumulatedGems) + state.rewardGems

        _coliseumRunState.value = (run ?: ColiseumRunState(battleNumber = state.coliseumBattleNumber)).copy(
            accumulatedGold = newAccGold,
            accumulatedGems = newAccGems,
            playerCards = updatedSnapshots
        )

        _playerProfile.update { p ->
            p.copy(
                coliseumWon = p.coliseumWon + 1,
                battlesWon = p.battlesWon + 1
            )
        }
        repository.savePlayerProfile(_playerProfile.value)

        battleViewModel.updateColiseumBattleOutcomeTotals(
            accumulatedGold = newAccGold,
            accumulatedGems = newAccGems,
            consolationGold = 0,
            consolationGems = 0
        )

        // Update relevant daily quests
        advanceDailyQuestProgress(QuestType.WIN_ANY_BATTLES, 1)
        if (state.playerSkillsUsedCount > 0) {
            advanceDailyQuestProgress(QuestType.CAST_SKILLS, state.playerSkillsUsedCount)
        }
        if (state.playerCards.isNotEmpty() && state.playerCards.all { it.isAlive }) {
            advanceDailyQuestProgress(QuestType.WIN_NO_LOSSES, 1)
        }
        if (state.playerCards.any { it.heroType == HeroType.HYBRID }) {
            advanceDailyQuestProgress(QuestType.WIN_WITH_HYBRID, 1)
        }
        if (state.playerCards.count { it.heroType == HeroType.HYBRID } >= 2) {
            advanceDailyQuestProgress(QuestType.WIN_TWO_HYBRID, 1)
        }
        if (state.playerCards.count { it.heroType == HeroType.RANGED } >= 4) {
            advanceDailyQuestProgress(QuestType.WIN_FOUR_RANGED, 1)
        }
        state.playerCards.map { it.element }.distinct().forEach { elem ->
            advanceDailyQuestProgress(QuestType.WIN_WITH_ELEMENT, 1, requiredElement = elem)
        }
        reconcileInfeasibleUncompletedQuests()
    }

    fun claimColiseumPrizeAndExit(nowMs: Long = System.currentTimeMillis()) {
        val run = _coliseumRunState.value
        val goldToClaim = run?.accumulatedGold ?: battleState.value?.coliseumAccumulatedGold ?: 0
        val gemsToClaim = run?.accumulatedGems ?: battleState.value?.coliseumAccumulatedGems ?: 0

        val shouldPreserveUnusedEntry = run != null &&
            run.battleNumber == 1 &&
            goldToClaim == 0 &&
            gemsToClaim == 0 &&
            battleState.value == null
        if (!shouldPreserveUnusedEntry) {
            _playerProfile.update { p ->
                p.copy(
                    gold = p.gold + goldToClaim,
                    gems = p.gems + gemsToClaim,
                    lastColiseumTimestamp = if (p.lastColiseumTimestamp <= 0L) nowMs else p.lastColiseumTimestamp,
                    maxSeenTimestamp = maxOf(p.maxSeenTimestamp, nowMs)
                )
            }
            repository.savePlayerProfile(_playerProfile.value)
        }

        battleViewModel.exitBattle()
        _coliseumRunState.value = null
        _coliseumBattlePreview.value = null
        _isColiseumPreviewVisible.value = false
        _coliseumMilestoneBonusOffer.value = null
        _coliseumDeckHeroIds.value = listOf(null, null, null, null)
        _isColiseumOpen.value = false
        triggerHaptic(true)
        if (!shouldPreserveUnusedEntry) {
            postDropMessage(
                tr(
                    "🏆 Приз Колизея получен: +$goldToClaim 🪙 и +$gemsToClaim 💎!",
                    "🏆 Coliseum prize claimed: +$goldToClaim 🪙 and +$gemsToClaim 💎!"
                )
            )
        }
    }

    fun continueColiseumAfterVictory() {
        val run = _coliseumRunState.value ?: return
        val completedBattle = run.battleNumber
        battleViewModel.exitBattle()

        if (completedBattle >= 3 && completedBattle % 3 == 0) {
            val healPct = if (completedBattle == 3) 20 else 35
            val revivePct = if (completedBattle == 3) 35 else 50
            val aliveCount = run.playerCards.count { it.isAlive }
            val deadCards = run.playerCards.filter { !it.isAlive }
            _coliseumMilestoneBonusOffer.value = ColiseumMilestoneBonusOffer(
                completedBattleNumber = completedBattle,
                healPercent = healPct,
                revivePercent = revivePct,
                aliveCount = aliveCount,
                deadCards = deadCards,
                currentBonusStartMana = run.bonusStartMana
            )
        } else {
            val nextBattle = completedBattle + 1
            _coliseumRunState.value = run.copy(battleNumber = nextBattle)
            prepareColiseumBattlePreview(
                battleNumber = nextBattle,
                initialDeckAtk = run.initialDeckAtk,
                initialDeckHp = run.initialDeckHp,
                avgDeckLevel = run.avgDeckLevel
            )
        }
    }

    fun applyColiseumMilestoneHeal() {
        val offer = _coliseumMilestoneBonusOffer.value ?: return
        val run = _coliseumRunState.value ?: return

        val healedCards = run.playerCards.map { card ->
            if (card.isAlive && card.currentHp > 0) {
                val healAmount = kotlin.math.ceil(card.maxHp * (offer.healPercent / 100.0)).toInt().coerceAtLeast(1)
                card.copy(currentHp = (card.currentHp + healAmount).coerceAtMost(card.maxHp))
            } else {
                card
            }
        }
        val nextBattle = offer.completedBattleNumber + 1
        _coliseumRunState.value = run.copy(battleNumber = nextBattle, playerCards = healedCards)
        _coliseumMilestoneBonusOffer.value = null
        triggerHaptic(true)
        prepareColiseumBattlePreview(
            battleNumber = nextBattle,
            initialDeckAtk = run.initialDeckAtk,
            initialDeckHp = run.initialDeckHp,
            avgDeckLevel = run.avgDeckLevel
        )
    }

    fun applyColiseumMilestoneRevive(slotIndex: Int) {
        val offer = _coliseumMilestoneBonusOffer.value ?: return
        val run = _coliseumRunState.value ?: return
        val target = run.playerCards.firstOrNull { it.slotIndex == slotIndex && !it.isAlive } ?: return

        val revivedHp = kotlin.math.ceil(target.maxHp * (offer.revivePercent / 100.0)).toInt().coerceIn(1, target.maxHp)
        val updatedCards = run.playerCards.map { card ->
            if (card.slotIndex == slotIndex) {
                card.copy(
                    isAlive = true,
                    currentHp = revivedHp,
                    currentArmor = card.maxArmor
                )
            } else {
                card
            }
        }
        val nextBattle = offer.completedBattleNumber + 1
        _coliseumRunState.value = run.copy(battleNumber = nextBattle, playerCards = updatedCards)
        _coliseumMilestoneBonusOffer.value = null
        triggerHaptic(true)
        prepareColiseumBattlePreview(
            battleNumber = nextBattle,
            initialDeckAtk = run.initialDeckAtk,
            initialDeckHp = run.initialDeckHp,
            avgDeckLevel = run.avgDeckLevel
        )
    }

    fun applyColiseumMilestoneMana() {
        val offer = _coliseumMilestoneBonusOffer.value ?: return
        val run = _coliseumRunState.value ?: return

        val nextBonusMana = run.bonusStartMana + 1
        val nextBattle = offer.completedBattleNumber + 1
        _coliseumRunState.value = run.copy(
            battleNumber = nextBattle,
            bonusStartMana = nextBonusMana
        )
        _coliseumMilestoneBonusOffer.value = null
        triggerHaptic(true)
        prepareColiseumBattlePreview(
            battleNumber = nextBattle,
            initialDeckAtk = run.initialDeckAtk,
            initialDeckHp = run.initialDeckHp,
            avgDeckLevel = run.avgDeckLevel
        )
    }

    fun selectPlayerSlot(slotIndex: Int) = battleViewModel.selectPlayerSlot(slotIndex)
    fun canCardTargetSlot(attacker: BattleCard, targetSlot: Int, enemyCards: List<BattleCard>): Boolean =
        battleViewModel.canCardTargetSlot(attacker, targetSlot, enemyCards)
    fun executePlayerAttack(targetSlotIndex: Int) = battleViewModel.executePlayerAttack(targetSlotIndex)
    fun executePlayerSkill() = battleViewModel.executePlayerSkill()
    fun toggleAutoBattle() = battleViewModel.toggleAutoBattle()
    fun setBattleSpeed(speed: Float) = battleViewModel.setBattleSpeed(speed)
    fun dismissArenaFirstTurnModal() = battleViewModel.dismissArenaFirstTurnModal()
    fun exitBattle() {
        val currentState = battleState.value
        if (currentState?.isColiseum == true) {
            when (currentState.phase) {
                BattlePhase.VICTORY -> claimColiseumPrizeAndExit()
                BattlePhase.DEFEAT -> {
                    battleViewModel.exitBattle()
                    _coliseumRunState.value = null
                    _coliseumBattlePreview.value = null
                    _coliseumMilestoneBonusOffer.value = null
                    _coliseumDeckHeroIds.value = listOf(null, null, null, null)
                    _isColiseumOpen.value = false
                }
                else -> surrenderBattle()
            }
            return
        }
        val wasArena = currentState?.isArena == true
        if (currentState?.isTower == true &&
            currentState.phase != BattlePhase.VICTORY &&
            currentState.phase != BattlePhase.DEFEAT
        ) {
            surrenderBattle()
            return
        }
        battleViewModel.exitBattle()
        if (wasArena) {
            refreshArenaBots()
        }
    }

    fun surrenderBattle() {
        val wasArena = battleState.value?.isArena == true
        val wasColiseum = battleState.value?.isColiseum == true
        battleViewModel.surrenderBattle()
        if (wasColiseum) {
            _isColiseumOpen.value = false
        }
        if (wasArena) {
            refreshArenaBots()
        }
    }

    // ----------------------------------------------------
    // DAILY TRIAL TOWER (БАШНЯ ДНЯ)
    // ----------------------------------------------------

    fun setSoundMuted(muted: Boolean) {
        _playerProfile.update { it.copy(isSoundMuted = muted) }
        repository.savePlayerProfile(_playerProfile.value)
        com.example.audio.GameAudioManager.setMuted(muted)
        triggerHaptic(false)
    }

    fun toggleSoundMuted() {
        setSoundMuted(!_playerProfile.value.isSoundMuted)
    }

    fun checkForAppUpdates(
        manual: Boolean = false,
        installedVersionCode: Int = com.example.BuildConfig.VERSION_CODE,
        installedVersionName: String = com.example.BuildConfig.VERSION_NAME,
        httpFetcher: ((String) -> String?)? = null
    ) {
        if (_isCheckingForUpdate.value) return
        viewModelScope.launch {
            _isCheckingForUpdate.value = true
            if (manual) {
                _updateCheckStatusMessage.value = null
            }
            val skippedCode = _playerProfile.value.skippedUpdateVersionCode
            val outcome = AppUpdateChecker.checkRemoteVersion(
                installedVersionCode = installedVersionCode,
                installedVersionName = installedVersionName,
                skippedVersionCode = skippedCode,
                ignoreSkipped = manual,
                httpFetcher = httpFetcher
            )
            when (outcome) {
                is AppUpdateCheckOutcome.UpdateAvailable -> {
                    _availableAppUpdate.value = outcome.info
                    if (manual) {
                        _updateCheckStatusMessage.value = null
                    }
                }
                is AppUpdateCheckOutcome.UpToDate -> {
                    if (manual) {
                        _updateCheckStatusMessage.value = tr(
                            "У вас установлена актуальная версия (${outcome.currentVersionName})",
                            "You are on the latest version (${outcome.currentVersionName})"
                        )
                    }
                }
                AppUpdateCheckOutcome.NetworkError -> {
                    if (manual) {
                        _updateCheckStatusMessage.value = tr(
                            "Не удалось проверить обновления. Проверьте подключение к интернету.",
                            "Could not check for updates. Please check your internet connection."
                        )
                    }
                }
            }
            _isCheckingForUpdate.value = false
        }
    }

    fun dismissAvailableAppUpdate() {
        _availableAppUpdate.value = null
    }

    fun skipAvailableAppUpdate() {
        val update = _availableAppUpdate.value
        _availableAppUpdate.value = null
        if (update != null && update.versionCode > _playerProfile.value.skippedUpdateVersionCode) {
            _playerProfile.update { it.copy(skippedUpdateVersionCode = update.versionCode) }
            repository.savePlayerProfile(_playerProfile.value)
        }
    }

    fun clearUpdateCheckStatus() {
        _updateCheckStatusMessage.value = null
    }

    fun markBonusGalleryImageViewed(imageKey: String) {
        if (imageKey.isBlank()) return
        val current = _playerProfile.value.viewedGalleryImageKeys
        if (imageKey in current) return
        _playerProfile.update { p ->
            p.copy(viewedGalleryImageKeys = p.viewedGalleryImageKeys + imageKey)
        }
        repository.savePlayerProfile(_playerProfile.value)
    }

    fun getEffectiveTowerDeckIds(): List<String?> {
        val currentTower = _towerDeckHeroIds.value
        val activeDeck = _playerProfile.value.activeDeckHeroIds
        return if (currentTower.size == 4 && currentTower.any { it != null }) currentTower else activeDeck
    }

    fun assignHeroToTowerSlot(slotIndex: Int, heroId: String?) {
        if (slotIndex !in 0..3) return
        val hero = heroId?.let { id -> _allHeroes.value.firstOrNull { it.id == id } }
        if (hero != null && !hero.isUnlocked) {
            showLockedHeroMessage(
                tr(
                    "Нельзя взять в отряд скрытую карту!",
                    "Cannot add a locked card to the Tower squad!"
                )
            )
            return
        }
        if (hero != null && slotIndex in 2..3 && hero.heroType == HeroType.MELEE) {
            showLockedHeroMessage(
                tr(
                    "Ближний бой только в 1 ряду!",
                    "Melee heroes can only be placed in row 1!"
                )
            )
            return
        }
        val currentDeck = getEffectiveTowerDeckIds().toMutableList()
        while (currentDeck.size < 4) currentDeck.add(null)
        if (heroId != null && currentDeck.withIndex().any { it.index != slotIndex && it.value == heroId }) {
            showLockedHeroMessage(
                tr(
                    "Нельзя ставить дубликаты в отряд Башни!",
                    "Duplicates are not allowed in the Tower squad!"
                )
            )
            return
        }
        currentDeck[slotIndex] = heroId
        _towerDeckHeroIds.value = currentDeck
        _playerProfile.update { it.copy(towerDeckHeroIds = currentDeck) }
        repository.savePlayerProfile(_playerProfile.value)
        triggerHaptic(false)
    }

    fun getTowerRemainingAttempts(nowMs: Long = System.currentTimeMillis()): Int {
        return computeTowerRemainingAttempts(_playerProfile.value, nowMs)
    }

    fun openTowerScreen(nowMs: Long = System.currentTimeMillis()): Boolean {
        val profile = _playerProfile.value
        if (profile.level < TOWER_MIN_PLAYER_LEVEL) {
            postDropMessage(
                tr(
                    "Доступ в Башню испытаний только с $TOWER_MIN_PLAYER_LEVEL уровня профиля!",
                    "Tower of Trials unlocks at Profile Level $TOWER_MIN_PLAYER_LEVEL!"
                )
            )
            triggerHaptic(true)
            return false
        }
        if (_towerDeckHeroIds.value.all { it == null }) {
            _towerDeckHeroIds.value = resolveInitialTowerDeck(profile, _allHeroes.value)
        }
        return true
    }

    fun completeTowerTutorial() {
        if (!_playerProfile.value.isTowerTutorialCompleted) {
            _playerProfile.update { it.copy(isTowerTutorialCompleted = true) }
            repository.savePlayerProfile(_playerProfile.value)
        }
    }

    fun getTowerFloorsForToday(nowMs: Long = System.currentTimeMillis()): List<TowerFloorPreview> {
        val rule = resolveTowerDayRule(nowMs)
        val wednesdayElement = resolveWednesdayTowerElement(nowMs)
        val deckHeroes = getEffectiveTowerDeckIds().mapNotNull { id ->
            _allHeroes.value.firstOrNull { it.id == id }
        }
        val avgDeckLevel = if (deckHeroes.isNotEmpty()) {
            (deckHeroes.sumOf { it.level } / deckHeroes.size.toDouble()).toInt().coerceAtLeast(1)
        } else 1
        val unlockedSecrets = _allHeroes.value.filter { it.isSecret && it.isUnlocked }.map { it.id }.toSet()
        return repository.getTowerFloorsForDay(
            rule = rule,
            playerLevel = _playerProfile.value.level,
            avgDeckLevel = avgDeckLevel,
            unlockedSecretHeroIds = unlockedSecrets,
            wednesdayElement = wednesdayElement
        )
    }

    fun getTowerRoofFloorForToday(nowMs: Long = System.currentTimeMillis()): TowerFloorPreview {
        val rule = resolveTowerDayRule(nowMs)
        val wednesdayElement = resolveWednesdayTowerElement(nowMs)
        val deckHeroes = getEffectiveTowerDeckIds().mapNotNull { id ->
            _allHeroes.value.firstOrNull { it.id == id }
        }
        val avgDeckLevel = if (deckHeroes.isNotEmpty()) {
            (deckHeroes.sumOf { it.level } / deckHeroes.size.toDouble()).toInt().coerceAtLeast(1)
        } else 1
        val unlockedSecrets = _allHeroes.value.filter { it.isSecret && it.isUnlocked }.map { it.id }.toSet()
        return repository.getTowerRoofFloorForDay(
            rule = rule,
            playerLevel = _playerProfile.value.level,
            avgDeckLevel = avgDeckLevel,
            unlockedSecretHeroIds = unlockedSecrets,
            wednesdayElement = wednesdayElement
        )
    }

    fun isDeckEligibleForTowerRule(
        rule: TowerDayRule,
        heroes: List<HeroCard> = _allHeroes.value,
        deckIds: List<String?> = getEffectiveTowerDeckIds()
    ): Boolean {
        val unlocked = heroes.filter { it.isUnlocked }
        val deckHeroes = deckIds.mapNotNull { id -> unlocked.firstOrNull { it.id == id } }
        if (deckHeroes.size != 4 || deckHeroes.map { it.id }.distinct().size != 4) return false

        return when (rule) {
            TowerDayRule.MONDAY_STEEL -> {
                val eligibleUnlocked = unlocked.count { it.heroType == HeroType.MELEE || it.heroType == HeroType.HYBRID }
                if (eligibleUnlocked < 4) true else deckHeroes.all { it.heroType == HeroType.MELEE || it.heroType == HeroType.HYBRID }
            }
            TowerDayRule.FRIDAY_COMMONERS_VALOR -> {
                val eligibleUnlocked = unlocked.count { it.rarity == CardRarity.BASIC || it.rarity == CardRarity.SPECIAL }
                if (eligibleUnlocked < 4) true else deckHeroes.all { it.rarity == CardRarity.BASIC || it.rarity == CardRarity.SPECIAL }
            }
            else -> true
        }
    }

    fun startTowerBattle(floorPreview: TowerFloorPreview, nowMs: Long = System.currentTimeMillis()) {
        val profile = _playerProfile.value
        if (profile.level < TOWER_MIN_PLAYER_LEVEL) {
            postDropMessage(
                tr(
                    "Доступ в Башню испытаний только с $TOWER_MIN_PLAYER_LEVEL уровня профиля!",
                    "Tower of Trials unlocks at Profile Level $TOWER_MIN_PLAYER_LEVEL!"
                )
            )
            triggerHaptic(true)
            return
        }
        val rule = resolveTowerDayRule(nowMs)
        val clearedToday = computeTowerClearedFloorToday(profile, nowMs)
        val floorStarsToday = computeTowerFloorStarsToday(profile, nowMs)
        val isRoof = floorPreview.floor == TOWER_ROOF_FLOOR
        val isRoofUnlocked = isTowerRoofUnlockedToday(profile, nowMs)
        val isRoofCleared = isTowerRoofClearedToday(profile, nowMs)
        val remainingAttempts = computeTowerRemainingAttempts(profile, nowMs)

        if (isRoof) {
            if (!isRoofUnlocked) {
                postDropMessage(
                    tr(
                        "Крыша откроется после прохождения этажей 1–5 на все 4★!",
                        "The Roof unlocks after clearing Floors 1–5 with all 4★!"
                    )
                )
                triggerHaptic(true)
                return
            }
            if (isRoofCleared) {
                postDropMessage(
                    tr(
                        "Крыша Башни уже пройдена сегодня!",
                        "The Tower Roof is already cleared today!"
                    )
                )
                triggerHaptic(false)
                return
            }
        } else {
            if (floorPreview.floor > clearedToday + 1) {
                postDropMessage(
                    tr(
                        "Сначала пройдите этаж ${clearedToday + 1}!",
                        "Clear Floor ${clearedToday + 1} first!"
                    )
                )
                triggerHaptic(true)
                return
            }
            if (floorPreview.floor <= clearedToday && floorStarsToday.getOrElse(floorPreview.floor - 1) { 0 } >= 4) {
                postDropMessage(
                    tr(
                        "Этаж ${floorPreview.floor} уже пройден на все 4★!",
                        "Floor ${floorPreview.floor} is already cleared with all 4★!"
                    )
                )
                triggerHaptic(false)
                return
            }
        }

        val isStarImprovementReplay = !isRoof && floorPreview.floor <= clearedToday

        if (remainingAttempts <= 0) {
            postDropMessage(
                tr(
                    "Попытки в Башне на сегодня закончились ($TOWER_DAILY_ATTEMPT_LIMIT/$TOWER_DAILY_ATTEMPT_LIMIT)! Прогресс сохранён до завтра.",
                    "No Tower attempts remaining today ($TOWER_DAILY_ATTEMPT_LIMIT/$TOWER_DAILY_ATTEMPT_LIMIT)! Progress is saved until tomorrow."
                )
            )
            triggerHaptic(true)
            return
        }
        val effectiveDeckIds = getEffectiveTowerDeckIds()
        if (!isDeckValid(effectiveDeckIds) || !isDeckEligibleForTowerRule(rule, _allHeroes.value, effectiveDeckIds)) {
            postDropMessage(
                tr(
                    "Состав отряда не соответствует правилу дня (${rule.titleRu})! Нажмите «Отряд».",
                    "Your Tower squad does not match today's rule (${rule.titleEn})! Tap Squad."
                )
            )
            triggerHaptic(true)
            return
        }

        // Snapshot saved wounded/dead enemy state BEFORE mutating profile (keep progress from prior defeat on this floor today)
        val savedEnemyHps = computeTowerDamagedFloorEnemyHps(profile, floorPreview.floor, nowMs)
        activeTowerSessionNowMs = nowMs

        // Deduct 1 attempt upon entering battle; refunded on victory so failed/aborted runs count against the 2/day limit.
        val todayStr = formatColiseumCalendarDate(nowMs)
        _playerProfile.update { p ->
            val currentUsed = if (p.towerProgressDate == todayStr) p.towerAttemptsUsedToday else 0
            val currentCleared = if (p.towerProgressDate == todayStr) p.towerClearedFloorToday else 0
            val currentStars = if (p.towerProgressDate == todayStr) p.towerFloorStarsToday else emptyList()
            val currentRoofCleared = if (p.towerProgressDate == todayStr) p.towerRoofClearedToday else false
            val currentRoofStars = if (p.towerProgressDate == todayStr) p.towerRoofStarsToday else 0
            val currentDamagedFloor = if (p.towerProgressDate == todayStr) p.towerDamagedFloor else 0
            val currentSavedHps = if (p.towerProgressDate == todayStr) p.towerSavedEnemyHps else emptyList()
            p.copy(
                towerProgressDate = todayStr,
                towerClearedFloorToday = currentCleared,
                towerFloorStarsToday = currentStars,
                towerRoofClearedToday = currentRoofCleared,
                towerRoofStarsToday = currentRoofStars,
                towerDamagedFloor = currentDamagedFloor,
                towerSavedEnemyHps = currentSavedHps,
                towerAttemptsUsedToday = (currentUsed + 1).coerceAtMost(TOWER_DAILY_ATTEMPT_LIMIT),
                maxSeenTimestamp = maxOf(p.maxSeenTimestamp, nowMs)
            )
        }
        repository.savePlayerProfile(_playerProfile.value)

        val playerDeckHeroes = effectiveDeckIds.mapIndexed { index, id ->
            val hero = id?.let { heroId -> _allHeroes.value.firstOrNull { it.id == heroId } }
            hero ?: _allHeroes.value.firstOrNull { it.isUnlocked } ?: _allHeroes.value[index.coerceIn(0, _allHeroes.value.lastIndex)]
        }
        val wednesdayElement = resolveWednesdayTowerElement(nowMs)

        val basePlayerAtkList = playerDeckHeroes.map { it.calculateTotalAtk(_inventoryArtifacts.value) }
        val basePlayerHpList = playerDeckHeroes.map { it.calculateTotalHp(_inventoryArtifacts.value) }
        val basePlayerDeckAtk = basePlayerAtkList.sum().coerceAtLeast(40)
        val basePlayerDeckHp = basePlayerHpList.sum().coerceAtLeast(120)

        val playerBattleCards = playerDeckHeroes.mapIndexed { slotIndex, hero ->
            var totalAtk = basePlayerAtkList[slotIndex]
            var totalArmor = hero.calculateTotalArmor(_inventoryArtifacts.value)
            var totalHp = basePlayerHpList[slotIndex]

            when (rule) {
                TowerDayRule.MONDAY_STEEL -> {
                    if (hero.heroType == HeroType.MELEE || hero.heroType == HeroType.HYBRID) {
                        totalAtk = (totalAtk * 1.15).toInt().coerceAtLeast(1)
                        totalArmor = (totalArmor * 1.20).toInt()
                    }
                }
                TowerDayRule.TUESDAY_ARCANE_VOLLEY -> {
                    if (hero.heroType == HeroType.RANGED) {
                        totalAtk = (totalAtk * 1.35).toInt().coerceAtLeast(1)
                    }
                }
                TowerDayRule.WEDNESDAY_ELEMENTAL_STORM -> {
                    if (hero.element == wednesdayElement) {
                        totalAtk = (totalAtk * 1.35).toInt().coerceAtLeast(1)
                    }
                }
                TowerDayRule.FRIDAY_COMMONERS_VALOR -> {
                    if (hero.rarity == CardRarity.BASIC || hero.rarity == CardRarity.SPECIAL) {
                        totalAtk = (totalAtk * 1.25).toInt().coerceAtLeast(1)
                        totalHp = (totalHp * 1.25).toInt().coerceAtLeast(5)
                    }
                }
                else -> {}
            }

            BattleCard(
                heroId = hero.id,
                name = hero.name,
                title = hero.title,
                heroType = hero.heroType,
                rarity = hero.rarity,
                element = hero.element,
                imageRes = hero.imageRes,
                slotIndex = slotIndex,
                isPlayer = true,
                level = hero.level,
                maxHp = totalHp,
                currentHp = totalHp,
                maxArmor = totalArmor,
                currentArmor = totalArmor,
                atk = totalAtk,
                attackPattern = hero.attackPattern,
                skill = if (hero.level >= 3) hero.activeSkill else null
            )
        }

        val baseTemplates = repository.getInitialHeroCards()
        val scaleMult = calculateTowerScalingMultiplier(floorPreview.floor)
        val targetEnemyTotalAtk = kotlin.math.ceil(basePlayerDeckAtk * scaleMult).toInt().coerceAtLeast(4)
        val targetEnemyTotalHp = kotlin.math.ceil(basePlayerDeckHp * scaleMult).toInt().coerceAtLeast(4)

        val rawEnemyHps = floorPreview.enemyHeroIds.mapIndexed { slotIndex, heroId ->
            val baseHero = baseTemplates.firstOrNull { it.id == heroId } ?: baseTemplates.first()
            val enemyLevel = floorPreview.enemyLevels.getOrElse(slotIndex) { 1 }
            val levelMult = 1.0 + (enemyLevel - 1) * 0.16
            val fridayHpMult = if (rule == TowerDayRule.FRIDAY_COMMONERS_VALOR &&
                (baseHero.rarity == CardRarity.BASIC || baseHero.rarity == CardRarity.SPECIAL)
            ) 1.25 else 1.0
            (baseHero.baseHp * levelMult * fridayHpMult).toInt().coerceAtLeast(1)
        }
        val rawEnemyAtks = floorPreview.enemyHeroIds.mapIndexed { slotIndex, heroId ->
            val baseHero = baseTemplates.firstOrNull { it.id == heroId } ?: baseTemplates.first()
            val enemyLevel = floorPreview.enemyLevels.getOrElse(slotIndex) { 1 }
            val levelMult = 1.0 + (enemyLevel - 1) * 0.16
            val ruleAtkMult = when (rule) {
                TowerDayRule.MONDAY_STEEL -> if (baseHero.heroType == HeroType.MELEE || baseHero.heroType == HeroType.HYBRID) 1.15 else 1.0
                TowerDayRule.TUESDAY_ARCANE_VOLLEY -> if (baseHero.heroType == HeroType.RANGED) 1.35 else 1.0
                TowerDayRule.WEDNESDAY_ELEMENTAL_STORM -> if (baseHero.element == wednesdayElement) 1.35 else 1.0
                TowerDayRule.FRIDAY_COMMONERS_VALOR -> if (baseHero.rarity == CardRarity.BASIC || baseHero.rarity == CardRarity.SPECIAL) 1.25 else 1.0
                else -> 1.0
            }
            (baseHero.baseAtk * levelMult * ruleAtkMult).toInt().coerceAtLeast(1)
        }

        val scaledEnemyHps = allocateProportionalStats(rawEnemyHps, targetEnemyTotalHp)
        val scaledEnemyAtks = allocateProportionalStats(rawEnemyAtks, targetEnemyTotalAtk)
        val tuesdayArmorScale = if (rule == TowerDayRule.TUESDAY_ARCANE_VOLLEY) 1.35 else 1.0

        val enemyBattleCards = floorPreview.enemyHeroIds.mapIndexed { slotIndex, heroId ->
            val baseHero = baseTemplates.firstOrNull { it.id == heroId } ?: baseTemplates.first()
            val enemyLevel = floorPreview.enemyLevels.getOrElse(slotIndex) { 1 }
            val levelMult = 1.0 + (enemyLevel - 1) * 0.16
            val maxHp = scaledEnemyHps[slotIndex]
            val mondayArmorScale = if (rule == TowerDayRule.MONDAY_STEEL &&
                (baseHero.heroType == HeroType.MELEE || baseHero.heroType == HeroType.HYBRID)
            ) 1.20 else 1.0
            val maxArmor = if (baseHero.heroType == HeroType.MELEE) {
                (baseHero.baseArmor * levelMult * scaleMult * mondayArmorScale * tuesdayArmorScale).toInt().coerceAtLeast(0)
            } else 0
            val atk = scaledEnemyAtks[slotIndex]
            val unlockedSkills = baseHero.copy(level = enemyLevel).getUnlockedSkills()
            val activeSkill = if (enemyLevel >= 3) (unlockedSkills.lastOrNull() ?: baseHero.activeSkill) else null

            val savedHpForSlot = savedEnemyHps?.getOrNull(slotIndex)
            val isAliveNow = savedHpForSlot == null || savedHpForSlot > 0
            val startingHp = when {
                savedHpForSlot == null -> maxHp
                savedHpForSlot <= 0 -> 0
                else -> savedHpForSlot.coerceIn(1, maxHp)
            }
            val startingArmor = if (isAliveNow) maxArmor else 0

            BattleCard(
                heroId = "tower_${floorPreview.floor}_$slotIndex",
                name = baseHero.localizedName(),
                title = tr("Ур. $enemyLevel ${baseHero.name}", "Lv. $enemyLevel ${baseHero.localizedName()}"),
                heroType = baseHero.heroType,
                rarity = baseHero.rarity,
                element = baseHero.element,
                imageRes = resolveHeroImageForEnemyTier(
                    baseHero.id,
                    when {
                        floorPreview.floor >= 5 -> ENEMY_PORTRAIT_TIER_3
                        floorPreview.floor >= 4 -> ENEMY_PORTRAIT_TIER_2
                        floorPreview.floor >= 3 -> ENEMY_PORTRAIT_TIER_1
                        else -> ENEMY_PORTRAIT_TIER_STANDARD
                    },
                    baseHero.imageRes
                ),
                slotIndex = slotIndex,
                isPlayer = false,
                level = enemyLevel,
                maxHp = maxHp,
                currentHp = startingHp,
                maxArmor = maxArmor,
                currentArmor = startingArmor,
                atk = atk,
                attackPattern = baseHero.attackPattern,
                skill = activeSkill,
                availableSkills = unlockedSkills,
                isAlive = isAliveNow
            )
        }

        val initialMana = if (rule == TowerDayRule.THURSDAY_MANA_SURGE) 3 else 1
        val ruleLabel = tr(rule.titleRu, rule.titleEn)

        battleViewModel.startBattle(
            playerCards = playerBattleCards,
            enemyCards = enemyBattleCards,
            enemyName = tr(floorPreview.titleRu, floorPreview.titleEn),
            enemyTitle = "${rule.iconSymbol} $ruleLabel",
            isArena = false,
            rewardGold = if (isStarImprovementReplay) 0 else floorPreview.rewardGold,
            rewardGems = if (isStarImprovementReplay) 0 else floorPreview.rewardGems,
            rewardExp = if (isStarImprovementReplay) 0 else floorPreview.rewardExp,
            rewardArtifact = null,
            campaignChapter = 2,
            battleBgRes = ARENA_BATTLE_BACKGROUNDS.random(),
            initialPlayerMana = initialMana,
            initialEnemyMana = initialMana,
            isTower = true,
            towerFloor = floorPreview.floor,
            towerRuleLabel = ruleLabel,
            isBoss = floorPreview.isBoss,
            onBattleEnd = { isVictory, state -> handleBattleFinished(isVictory, state) }
        )
    }
}
