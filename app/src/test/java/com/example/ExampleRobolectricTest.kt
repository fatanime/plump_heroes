package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.data.GameDataRepository
import com.example.i18n.*
import com.example.model.*
import com.example.ui.components.getCardFrameColor
import com.example.viewmodel.BattleViewModel
import com.example.viewmodel.GameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Plump Heroes", appName)
    }

    @Test
    fun `verify frame color by card level`() {
        // Level 1: Default Blue frame
        assertEquals(Color(0xFF29B6F6), getCardFrameColor(1))
        assertEquals(Color(0xFF29B6F6), getCardFrameColor(4))

        // After 5 Level: Silver frame
        assertEquals(Color(0xFFE0E0E0), getCardFrameColor(5))
        assertEquals(Color(0xFFE0E0E0), getCardFrameColor(9))

        // After 10 Level: Golden frame
        assertEquals(Color(0xFFFFD54F), getCardFrameColor(10))
        assertEquals(Color(0xFFFFD54F), getCardFrameColor(15))
    }

    @Test
    fun `verify player level 1 has only 4 playable cards - 2 melee and 2 ranged`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = GameDataRepository(context)
        val heroes = repo.getInitialHeroCards()

        val unlockedHeroes = heroes.filter { it.isUnlocked }
        assertEquals("At level 1, player should have exactly 4 unlocked playable cards", 4, unlockedHeroes.size)

        val meleeUnlocked = unlockedHeroes.filter { it.heroType == HeroType.MELEE }
        val rangedUnlocked = unlockedHeroes.filter { it.heroType == HeroType.RANGED }
        assertEquals("Must have 2 melee heroes unlocked", 2, meleeUnlocked.size)
        assertEquals("Must have 2 ranged heroes unlocked", 2, rangedUnlocked.size)

        val lockedHeroes = heroes.filter { !it.isUnlocked }
        assertTrue("Remaining heroes must be locked", lockedHeroes.isNotEmpty())
        assertTrue("All locked heroes require level > 1 or career", lockedHeroes.all { it.unlockPlayerLevel > 1 })
    }

    @Test
    fun `verify battle viewmodel manages 2x2 turn-based state and health tracking`() {
        val battleVm = BattleViewModel()

        val playerCard = BattleCard(
            heroId = "hero_1",
            name = "Тестовая Валькирия",
            title = "Воин",
            heroType = HeroType.MELEE,
            rarity = CardRarity.BASIC,
            element = Element.LIGHT,
            imageRes = R.drawable.hero_brunhilde,
            slotIndex = 0,
            isPlayer = true,
            maxHp = 50,
            currentHp = 50,
            maxArmor = 6,
            currentArmor = 6,
            atk = 15,
            level = 1
        )

        val enemyCard = BattleCard(
            heroId = "enemy_1",
            name = "Вражеский Гоблин",
            title = "Гоблин",
            heroType = HeroType.MELEE,
            rarity = CardRarity.BASIC,
            element = Element.FIRE,
            imageRes = R.drawable.hero_berta,
            slotIndex = 0,
            isPlayer = false,
            maxHp = 40,
            currentHp = 40,
            maxArmor = 4,
            currentArmor = 4,
            atk = 10,
            level = 1
        )

        battleVm.startBattle(
            playerCards = listOf(playerCard),
            enemyCards = listOf(enemyCard),
            enemyName = "Тренировочный бой",
            enemyTitle = "Глава 1",
            isArena = false,
            rewardGold = 100,
            rewardGems = 10,
            rewardExp = 50,
            onBattleEnd = { _, _ -> }
        )

        val initialBattleState = battleVm.battleState.value
        assertTrue("Battle should be active", initialBattleState != null)
        assertEquals(BattlePhase.PLAYER_TURN, initialBattleState?.phase)
        assertEquals(50, initialBattleState?.playerCards?.first()?.currentHp)
        assertEquals(40, initialBattleState?.enemyCards?.first()?.currentHp)

        // Execute player attack on enemy slot 0
        battleVm.executePlayerAttack(0)

        val afterAttackState = battleVm.battleState.value
        val damagedEnemy = afterAttackState?.enemyCards?.first()
        assertTrue("Enemy armor must be reduced or broken", (damagedEnemy?.currentArmor ?: 0) < 4)
        assertTrue("Enemy HP must be reduced by attack damage", (damagedEnemy?.currentHp ?: 40) < 40)
    }

    @Test
    fun `verify persistent profile saving and active deck retention`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = GameDataRepository(context)

        val customProfile = PlayerProfile(
            name = "Королева Битв",
            level = 4,
            gold = 8888,
            activeDeckHeroIds = listOf("brunhilde", "olga", "berta", "witch")
        )

        repo.savePlayerProfile(customProfile)
        val loaded = repo.loadPlayerProfile()

        assertEquals("Королева Битв", loaded.name)
        assertEquals(4, loaded.level)
        assertEquals(8888, loaded.gold)
        assertEquals("brunhilde", loaded.activeDeckHeroIds[0])
        assertEquals("olga", loaded.activeDeckHeroIds[1])
        assertEquals("berta", loaded.activeDeckHeroIds[2])
        assertEquals("witch", loaded.activeDeckHeroIds[3])
    }

    @Test
    fun `verify json export and import of game save`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = GameDataRepository(context)

        val profile = PlayerProfile(name = "Тестовый Игрок", level = 2, gold = 5000)
        val heroes = repo.getInitialHeroCards()
        val artifacts = repo.getInitialArtifacts()
        val testStars = mapOf(101 to 3, 102 to 2)
        val testClears = mapOf(101 to 7, 102 to 2)

        val json = repo.exportSaveToJson(profile, heroes, artifacts, testStars, testClears)
        assertTrue("JSON should contain version", json.contains("\"version\": \"${com.example.BuildConfig.VERSION_NAME}\""))
        assertTrue("JSON should contain player name", json.contains("Тестовый Игрок"))
        assertTrue("JSON should contain campaignStars", json.contains("campaignStars"))
        assertTrue("JSON should contain campaignClearCounts", json.contains("campaignClearCounts"))

        val imported = repo.importSaveFromJson(json, heroes)
        assertTrue("Imported result must not be null", imported != null)
        assertEquals("Тестовый Игрок", imported?.first?.name)
        assertEquals(2, imported?.first?.level)
        assertEquals(false, imported?.first?.isTutorialCompleted)
        assertEquals(3, imported?.campaignStars?.get(101))
        assertEquals(2, imported?.campaignStars?.get(102))
        assertEquals(7, imported?.campaignClearCounts?.get(101))
        assertEquals(2, imported?.campaignClearCounts?.get(102))
    }

    @Test
    fun `verify artifact equipping level restrictions and trade-off catalog`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = GameDataRepository(context)
        val catalog = repo.getArtifactCatalog()

        // 12 base + 12 trade-off artifacts + special/rare/legacy catalog
        assertTrue(catalog.size >= 24)

        // Verify base artifact bonus reduction (e.g. basic knife atk = 3, hp = 5)
        val basicKnife = catalog["art_base_1"]
        assertEquals(4, basicKnife?.atkBonus)
        assertEquals(4, basicKnife?.hpBonus)

        // Verify new trade-off artifacts exist and have asymmetric trade-offs (-2 ATK, +2 Armor, +7 HP after halving/cap)
        val tradeArmor = catalog["art_trade_base_1"]
        assertTrue("Trade armor must exist", tradeArmor != null)
        assertEquals(-2, tradeArmor?.atkBonus)
        assertEquals(2, tradeArmor?.armorBonus)
        assertEquals(7, tradeArmor?.hpBonus)

        // Test level restrictions:
        // Level 1-2: Basic only
        assertTrue(canHeroEquipArtifact(heroLevel = 1, CardRarity.BASIC))
        assertTrue(canHeroEquipArtifact(heroLevel = 2, CardRarity.BASIC))
        assertFalse(canHeroEquipArtifact(heroLevel = 1, CardRarity.SPECIAL))
        assertFalse(canHeroEquipArtifact(heroLevel = 2, CardRarity.RARE))

        // Level 3-4: Basic and Special
        assertTrue(canHeroEquipArtifact(heroLevel = 3, CardRarity.BASIC))
        assertTrue(canHeroEquipArtifact(heroLevel = 3, CardRarity.SPECIAL))
        assertFalse(canHeroEquipArtifact(heroLevel = 3, CardRarity.RARE))
        assertFalse(canHeroEquipArtifact(heroLevel = 4, CardRarity.MYTHIC))

        // Level 5-6: Basic, Special, Rare (Mythic requires 7+)
        assertTrue(canHeroEquipArtifact(heroLevel = 5, CardRarity.BASIC))
        assertTrue(canHeroEquipArtifact(heroLevel = 5, CardRarity.SPECIAL))
        assertTrue(canHeroEquipArtifact(heroLevel = 5, CardRarity.RARE))
        assertFalse(canHeroEquipArtifact(heroLevel = 5, CardRarity.MYTHIC))
        assertFalse(canHeroEquipArtifact(heroLevel = 6, CardRarity.MYTHIC))

        // Level 7+: Mythic
        assertTrue(canHeroEquipArtifact(heroLevel = 7, CardRarity.MYTHIC))
    }

    @Test
    fun `verify world lore read flag is persisted and campaign has 4 chapters`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = GameDataRepository(context)

        val profile = PlayerProfile(name = "Легенда", isStoryRead = false)
        repo.savePlayerProfile(profile)
        assertFalse("Profile story should be unread initially", repo.loadPlayerProfile().isStoryRead)

        repo.savePlayerProfile(profile.copy(isStoryRead = true))
        assertTrue("Profile story should be marked read", repo.loadPlayerProfile().isStoryRead)

        val chapters = repo.getCampaignChapters()
        assertEquals(4, chapters.size)
        for (chapter in chapters) {
            assertEquals("Each chapter must have exactly 6 stages (5 regular + 1 boss)", 6, chapter.stages.size)
            val bossStage = chapter.stages.last()
            assertTrue("Stage 6 must be marked as boss", bossStage.isBoss)
            for (stage in chapter.stages) {
                assertTrue("Recommended difficulty scale must be <= 150", stage.recommendedPower <= 150)
                // Front row (slots 0-1) can have melee, but back row (slots 2-3) must NEVER have melee
                val enemyHeroes = stage.enemyHeroIds.map { id -> repo.getInitialHeroCards().first { it.id == id } }
                if (enemyHeroes.size > 2) {
                    val backRowHeroes = enemyHeroes.drop(2)
                    assertTrue("Melee heroes strictly prohibited from back row in campaign", backRowHeroes.none { it.heroType == HeroType.MELEE })
                }
            }
        }
    }

    @Test
    fun `verify arena bots have bot name matching one of their deck cards and no melee in back row`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = GameDataRepository(context)
        val bots = repo.generateArenaOpponents(playerLevel = 5, playerTrophies = 300)

        assertTrue("Should generate arena bots", bots.isNotEmpty())
        for (bot in bots) {
            val heroNames = bot.heroIds.map { id -> repo.getInitialHeroCards().first { it.id == id }.name }
            assertTrue("Bot name (${bot.name}) must be one of its 4 cards ($heroNames)", heroNames.contains(bot.name))

            val botHeroes = bot.heroIds.map { id -> repo.getInitialHeroCards().first { it.id == id } }
            val backRow = botHeroes.drop(2)
            assertTrue("Bot back row must not contain Melee", backRow.none { it.heroType == HeroType.MELEE })
        }
    }

    @Test
    fun `verify ranged cards target second row when second row enemies are alive`() {
        val battleVm = BattleViewModel()
        val rangedAttacker = BattleCard(
            heroId = "berta",
            name = "Берта",
            title = "Озорная Пиромантка",
            heroType = HeroType.RANGED,
            rarity = CardRarity.BASIC,
            element = Element.FIRE,
            imageRes = R.drawable.hero_berta,
            slotIndex = 2,
            isPlayer = true,
            maxHp = 34,
            currentHp = 34,
            maxArmor = 0,
            currentArmor = 0,
            atk = 16
        )

        val frontEnemy = BattleCard(
            heroId = "front_enemy",
            name = "Фронт Враг",
            title = "Воин",
            heroType = HeroType.MELEE,
            rarity = CardRarity.BASIC,
            element = Element.LIGHT,
            imageRes = R.drawable.hero_brunhilde,
            slotIndex = 0, // Front row
            isPlayer = false,
            maxHp = 40,
            currentHp = 40,
            maxArmor = 5,
            currentArmor = 5,
            atk = 10
        )

        val backEnemy = BattleCard(
            heroId = "back_enemy",
            name = "Тыл Враг",
            title = "Маг",
            heroType = HeroType.RANGED,
            rarity = CardRarity.BASIC,
            element = Element.DARK,
            imageRes = R.drawable.hero_witch,
            slotIndex = 2, // Back row
            isPlayer = false,
            maxHp = 30,
            currentHp = 30,
            maxArmor = 0,
            currentArmor = 0,
            atk = 12
        )

        val enemyCards = listOf(frontEnemy, backEnemy)

        // Ranged card cannot target front row when back row has alive enemies
        assertFalse(
            "Ranged attacker should NOT target front row when back row has alive enemy",
            battleVm.canCardTargetSlot(rangedAttacker, targetSlot = 0, enemyCards)
        )
        // Ranged card CAN target back row
        assertTrue(
            "Ranged attacker MUST target back row when back row is alive",
            battleVm.canCardTargetSlot(rangedAttacker, targetSlot = 2, enemyCards)
        )

        // Once back enemy is defeated, ranged card can target front row
        backEnemy.isAlive = false
        assertTrue(
            "Ranged attacker can target front row when back row is defeated",
            battleVm.canCardTargetSlot(rangedAttacker, targetSlot = 0, enemyCards)
        )
    }

    @Test
    fun `verify artifact rarity HP caps`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = GameDataRepository(context)
        val catalog = repo.getArtifactCatalog().values

        for (art in catalog) {
            when (art.rarity) {
                CardRarity.MYTHIC -> assertTrue("Mythic HP bonus <= 50, was ${art.hpBonus}", art.hpBonus <= 50)
                CardRarity.RARE -> assertTrue("Rare HP bonus <= 35, was ${art.hpBonus}", art.hpBonus <= 35)
                CardRarity.SPECIAL -> assertTrue("Special HP bonus <= 20, was ${art.hpBonus}", art.hpBonus <= 20)
                CardRarity.BASIC -> assertTrue("Basic HP bonus <= 10, was ${art.hpBonus}", art.hpBonus <= 10)
            }
        }
    }

    @Test
    fun `verify career attack power values match user specifications`() {
        val expected = mapOf(
            101 to 40, 102 to 43, 103 to 46, 104 to 49, 105 to 51, 106 to 60,
            201 to 54, 202 to 58, 203 to 61, 204 to 63, 205 to 65, 206 to 75,
            301 to 70, 302 to 74, 303 to 78, 304 to 82, 305 to 86, 306 to 100,
            401 to 95, 402 to 102, 403 to 109, 404 to 115, 405 to 122, 406 to 140
        )
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = GameDataRepository(context)
        val stages = repo.getCampaignChapters().flatMap { it.stages }

        for ((stageId, expectedAtk) in expected) {
            val stage = stages.firstOrNull { it.id == stageId }
            org.junit.Assert.assertNotNull("Stage $stageId should exist", stage)
            assertEquals("Stage $stageId totalEnemyAtk should be $expectedAtk", expectedAtk, stage?.totalEnemyAtk)
            assertEquals("Stage $stageId recommendedPower should be $expectedAtk", expectedAtk, stage?.recommendedPower)
        }
    }

    @Test
    fun `verify import save strictly resets unlocked stages if imported save has fewer completed stages`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = GameDataRepository(app)
        val vm = GameViewModel(app)

        val profile = repo.loadPlayerProfile()
        val heroes = repo.loadHeroesState(repo.getInitialHeroCards())
        val artifacts = repo.loadArtifacts()
        val emptyStarsSave = repo.exportSaveToJson(profile, heroes, artifacts, emptyMap())

        val imported = vm.importSaveFromJson(emptyStarsSave)
        assertTrue(imported)

        val stages = vm.campaignChapters.value.flatMap { it.stages }
        val s101 = stages.first { it.id == 101 }
        val s102 = stages.first { it.id == 102 }
        val s103 = stages.first { it.id == 103 }

        assertTrue("First stage 1-1 must be unlocked", s101.isUnlocked)
        assertFalse("Stage 1-2 must be locked when 1-1 has 0 stars", s102.isUnlocked)
        assertFalse("Stage 1-3 must be locked", s103.isUnlocked)
    }

    @Test
    fun `verify hero level up stats persist after saving and reloading app state`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = GameDataRepository(app)

        // Start clean with sufficient gold and shards for Brunhilde
        val defaultHeroes = repo.getInitialHeroCards()
        val buffedHeroes = defaultHeroes.map {
            if (it.id == "brunhilde") it.copy(shards = 20) else it
        }
        repo.savePlayerProfile(PlayerProfile(name = "Tester", gold = 2000))
        repo.saveHeroesState(buffedHeroes)

        val vm = GameViewModel(app)
        val beforeHero = vm.allHeroes.value.first { it.id == "brunhilde" }
        val beforeAtk = beforeHero.calculateTotalAtk(emptyList())
        val beforeArmor = beforeHero.calculateTotalArmor(emptyList())
        val beforeHp = beforeHero.calculateTotalHp(emptyList())

        vm.upgradeHeroLevel("brunhilde")
        val upgradedHero = vm.allHeroes.value.first { it.id == "brunhilde" }
        assertEquals(2, upgradedHero.level)
        val upgradedAtk = upgradedHero.calculateTotalAtk(emptyList())
        val upgradedArmor = upgradedHero.calculateTotalArmor(emptyList())
        val upgradedHp = upgradedHero.calculateTotalHp(emptyList())
        assertTrue("ATK should increase on level up", upgradedAtk > beforeAtk)
        assertTrue("Armor should increase on level up", upgradedArmor > beforeArmor)
        assertTrue("HP should increase on level up", upgradedHp > beforeHp)

        // Simulate app restart by creating a new GameViewModel reading from SharedPreferences
        val restartedVm = GameViewModel(app)
        val reloadedHero = restartedVm.allHeroes.value.first { it.id == "brunhilde" }
        assertEquals(2, reloadedHero.level)
        assertEquals("ATK must persist after restart", upgradedAtk, reloadedHero.calculateTotalAtk(emptyList()))
        assertEquals("Armor must persist after restart", upgradedArmor, reloadedHero.calculateTotalArmor(emptyList()))
        assertEquals("HP must persist after restart", upgradedHp, reloadedHero.calculateTotalHp(emptyList()))
    }

    @Test
    fun `verify max hero level is 12 and level 12 heroes receive no shards`() {
        for (rarity in CardRarity.values()) {
            assertEquals("Max level cap must be 12 for $rarity", 12, rarity.levelCap)
        }

        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = GameDataRepository(app)
        val maxedHeroes = repo.getInitialHeroCards().map {
            it.copy(level = 12, shards = 0).withUpdatedImageForLevel()
        }
        repo.savePlayerProfile(PlayerProfile(name = "Tester", gold = 50000, gems = 5000))
        repo.saveHeroesState(maxedHeroes)

        val vm = GameViewModel(app)
        vm.openSummonChest(com.example.ui.screens.ChestType.BASIC)
        vm.openSummonChest(com.example.ui.screens.ChestType.RARITY)

        assertTrue(
            "No hero at level 12 should ever receive shards from chests",
            vm.allHeroes.value.all { it.shards == 0 }
        )
    }

    @Test
    fun `verify Olga, Berta, Brunhilde and Morgana portraits change by level, unlock gallery and persist to save file`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = GameDataRepository(app)

        // Berta & Olga including level 12 _max
        assertEquals(R.drawable.hero_berta_0, resolveHeroImageForLevel("berta", 1, R.drawable.hero_berta))
        assertEquals(R.drawable.hero_berta_0, resolveHeroImageForLevel("berta", 2, R.drawable.hero_berta))
        assertEquals(R.drawable.hero_berta, resolveHeroImageForLevel("berta", 3, R.drawable.hero_berta))
        assertEquals(R.drawable.hero_berta, resolveHeroImageForLevel("berta", 4, R.drawable.hero_berta))
        assertEquals(R.drawable.hero_berta_1, resolveHeroImageForLevel("berta", 5, R.drawable.hero_berta))
        assertEquals(R.drawable.hero_berta_1, resolveHeroImageForLevel("berta", 7, R.drawable.hero_berta))
        assertEquals(R.drawable.hero_berta_2, resolveHeroImageForLevel("berta", 8, R.drawable.hero_berta))
        assertEquals(R.drawable.hero_berta_2, resolveHeroImageForLevel("berta", 9, R.drawable.hero_berta))
        assertEquals(R.drawable.hero_berta_3, resolveHeroImageForLevel("berta", 10, R.drawable.hero_berta))
        assertEquals(R.drawable.hero_berta_3, resolveHeroImageForLevel("berta", 11, R.drawable.hero_berta))
        assertEquals(R.drawable.hero_berta_max, resolveHeroImageForLevel("berta", 12, R.drawable.hero_berta))

        assertEquals(R.drawable.hero_olga_0, resolveHeroImageForLevel("olga", 1, R.drawable.hero_olga))
        assertEquals(R.drawable.hero_olga_0, resolveHeroImageForLevel("olga", 2, R.drawable.hero_olga))
        assertEquals(R.drawable.hero_olga, resolveHeroImageForLevel("olga", 3, R.drawable.hero_olga))
        assertEquals(R.drawable.hero_olga, resolveHeroImageForLevel("olga", 4, R.drawable.hero_olga))
        assertEquals(R.drawable.hero_olga_1, resolveHeroImageForLevel("olga", 5, R.drawable.hero_olga))
        assertEquals(R.drawable.hero_olga_1, resolveHeroImageForLevel("olga", 7, R.drawable.hero_olga))
        assertEquals(R.drawable.hero_olga_2, resolveHeroImageForLevel("olga", 8, R.drawable.hero_olga))
        assertEquals(R.drawable.hero_olga_2, resolveHeroImageForLevel("olga", 9, R.drawable.hero_olga))
        assertEquals(R.drawable.hero_olga_3, resolveHeroImageForLevel("olga", 10, R.drawable.hero_olga))
        assertEquals(R.drawable.hero_olga_3, resolveHeroImageForLevel("olga", 11, R.drawable.hero_olga))
        assertEquals(R.drawable.hero_olga_max, resolveHeroImageForLevel("olga", 12, R.drawable.hero_olga))

        // Brunhilde & Morgana (witch)
        assertEquals(R.drawable.hero_brunhilde_0, resolveHeroImageForLevel("brunhilde", 1, R.drawable.hero_brunhilde))
        assertEquals(R.drawable.hero_brunhilde_0, resolveHeroImageForLevel("brunhilde", 2, R.drawable.hero_brunhilde))
        assertEquals(R.drawable.hero_brunhilde, resolveHeroImageForLevel("brunhilde", 3, R.drawable.hero_brunhilde))
        assertEquals(R.drawable.hero_brunhilde, resolveHeroImageForLevel("brunhilde", 4, R.drawable.hero_brunhilde))
        assertEquals(R.drawable.hero_brunhilde_1, resolveHeroImageForLevel("brunhilde", 5, R.drawable.hero_brunhilde))
        assertEquals(R.drawable.hero_brunhilde_1, resolveHeroImageForLevel("brunhilde", 7, R.drawable.hero_brunhilde))
        assertEquals(R.drawable.hero_brunhilde_2, resolveHeroImageForLevel("brunhilde", 8, R.drawable.hero_brunhilde))
        assertEquals(R.drawable.hero_brunhilde_2, resolveHeroImageForLevel("brunhilde", 9, R.drawable.hero_brunhilde))
        assertEquals(R.drawable.hero_brunhilde_3, resolveHeroImageForLevel("brunhilde", 10, R.drawable.hero_brunhilde))
        assertEquals(R.drawable.hero_brunhilde_3, resolveHeroImageForLevel("brunhilde", 11, R.drawable.hero_brunhilde))
        assertEquals(R.drawable.hero_brunhilde_max, resolveHeroImageForLevel("brunhilde", 12, R.drawable.hero_brunhilde))

        assertEquals(R.drawable.hero_witch_0, resolveHeroImageForLevel("witch", 1, R.drawable.hero_witch))
        assertEquals(R.drawable.hero_witch_0, resolveHeroImageForLevel("witch", 2, R.drawable.hero_witch))
        assertEquals(R.drawable.hero_witch, resolveHeroImageForLevel("witch", 3, R.drawable.hero_witch))
        assertEquals(R.drawable.hero_witch, resolveHeroImageForLevel("witch", 4, R.drawable.hero_witch))
        assertEquals(R.drawable.hero_witch_1, resolveHeroImageForLevel("witch", 5, R.drawable.hero_witch))
        assertEquals(R.drawable.hero_witch_1, resolveHeroImageForLevel("witch", 7, R.drawable.hero_witch))
        assertEquals(R.drawable.hero_witch_2, resolveHeroImageForLevel("witch", 8, R.drawable.hero_witch))
        assertEquals(R.drawable.hero_witch_2, resolveHeroImageForLevel("witch", 9, R.drawable.hero_witch))
        assertEquals(R.drawable.hero_witch_3, resolveHeroImageForLevel("witch", 10, R.drawable.hero_witch))
        assertEquals(R.drawable.hero_witch_3, resolveHeroImageForLevel("witch", 11, R.drawable.hero_witch))
        assertEquals(R.drawable.hero_witch_max, resolveHeroImageForLevel("witch", 12, R.drawable.hero_witch))

        // Gallery helper exposes ONLY unlocked images in ascending order
        assertEquals(
            listOf(R.drawable.hero_brunhilde_0),
            getUnlockedHeroGalleryImages("brunhilde", 2, R.drawable.hero_brunhilde_0)
        )
        assertEquals(
            listOf(R.drawable.hero_brunhilde_0, R.drawable.hero_brunhilde, R.drawable.hero_brunhilde_1),
            getUnlockedHeroGalleryImages("brunhilde", 5, R.drawable.hero_brunhilde_1)
        )
        assertEquals(
            listOf(
                R.drawable.hero_witch_0,
                R.drawable.hero_witch,
                R.drawable.hero_witch_1,
                R.drawable.hero_witch_2,
                R.drawable.hero_witch_3,
                R.drawable.hero_witch_max
            ),
            getUnlockedHeroGalleryImages("witch", 12, R.drawable.hero_witch_max)
        )

        // Verify upgrade -> save -> export JSON -> import JSON restores exact portrait including level 12 _max
        val startHeroes = repo.getInitialHeroCards().map {
            when (it.id) {
                "berta" -> it.copy(level = 4, shards = 50).withUpdatedImageForLevel()
                "olga" -> it.copy(level = 11, shards = 50).withUpdatedImageForLevel()
                "brunhilde" -> it.copy(level = 4, shards = 50).withUpdatedImageForLevel()
                "witch" -> it.copy(level = 11, shards = 50).withUpdatedImageForLevel()
                else -> it
            }
        }
        repo.savePlayerProfile(PlayerProfile(name = "Saver", gold = 20000))
        repo.saveHeroesState(startHeroes)

        val vm = GameViewModel(app)
        assertEquals(R.drawable.hero_berta, vm.allHeroes.value.first { it.id == "berta" }.imageRes)
        assertEquals(R.drawable.hero_olga_3, vm.allHeroes.value.first { it.id == "olga" }.imageRes)
        assertEquals(R.drawable.hero_brunhilde, vm.allHeroes.value.first { it.id == "brunhilde" }.imageRes)
        assertEquals(R.drawable.hero_witch_3, vm.allHeroes.value.first { it.id == "witch" }.imageRes)

        vm.upgradeHeroLevel("berta")     // 4 -> 5 => hero_berta_1
        vm.upgradeHeroLevel("olga")      // 11 -> 12 => hero_olga_max
        vm.upgradeHeroLevel("brunhilde") // 4 -> 5 => hero_brunhilde_1
        vm.upgradeHeroLevel("witch")     // 11 -> 12 => hero_witch_max

        assertEquals(R.drawable.hero_berta_1, vm.allHeroes.value.first { it.id == "berta" }.imageRes)
        assertEquals(R.drawable.hero_olga_max, vm.allHeroes.value.first { it.id == "olga" }.imageRes)
        assertEquals(R.drawable.hero_brunhilde_1, vm.allHeroes.value.first { it.id == "brunhilde" }.imageRes)
        assertEquals(R.drawable.hero_witch_max, vm.allHeroes.value.first { it.id == "witch" }.imageRes)

        val json = vm.exportSaveToJson()
        assertTrue(json.contains("\"imageResName\": \"hero_berta_1\""))
        assertTrue(json.contains("\"imageResName\": \"hero_olga_max\""))
        assertTrue(json.contains("\"imageResName\": \"hero_brunhilde_1\""))
        assertTrue(json.contains("\"imageResName\": \"hero_witch_max\""))

        val reloaded = repo.importSaveFromJson(json, repo.getInitialHeroCards())
        assertEquals(R.drawable.hero_berta_1, reloaded?.heroes?.first { it.id == "berta" }?.imageRes)
        assertEquals(R.drawable.hero_olga_max, reloaded?.heroes?.first { it.id == "olga" }?.imageRes)
        assertEquals(R.drawable.hero_brunhilde_1, reloaded?.heroes?.first { it.id == "brunhilde" }?.imageRes)
        assertEquals(R.drawable.hero_witch_max, reloaded?.heroes?.first { it.id == "witch" }?.imageRes)
    }

    @Test
    fun `verify tutorial includes new fairy frames 10 and 11 and ends at step 28`() {
        val f9 = com.example.ui.components.getTutorialFrame(9, "Игрок")
        val f10 = com.example.ui.components.getTutorialFrame(10, "Игрок")
        val f11 = com.example.ui.components.getTutorialFrame(11, "Игрок")
        val f27 = com.example.ui.components.getTutorialFrame(27, "Игрок")
        val f28 = com.example.ui.components.getTutorialFrame(28, "Игрок")

        assertTrue(f9.textTemplate.contains("Находи артефакты"))
        assertEquals(com.example.ui.components.FairySpeaker.GREENPIE, f10.speaker)
        assertEquals("А еще, в нашем мире чем сильнее ты, тем полнее!", f10.textTemplate)
        assertEquals(com.example.ui.components.FairySpeaker.BLUESWEET, f11.speaker)
        assertEquals("Поэтому, повышая уровень девушки становятся толще!", f11.textTemplate)
        assertEquals(27, f27.step)
        assertEquals(28, f28.step)
        assertEquals(com.example.ui.components.FairySpeaker.BLUESWEET, f28.speaker)
        assertEquals(
            "Монет много, а алмазов ноль? Нажми на иконку алмазов или монет сверху и конвентируй валюту. Чао!",
            f28.textTemplate
        )
        val f28En = com.example.i18n.getTutorialFrameEn(28, "Player")
        assertTrue(f28En.textTemplate.contains("Got lots of coins but zero gems?"))

        // Coliseum 3-frame tutorial RU & EN
        val col1Ru = com.example.ui.components.getColiseumTutorialFrame(1)
        val col2Ru = com.example.ui.components.getColiseumTutorialFrame(2)
        val col3Ru = com.example.ui.components.getColiseumTutorialFrame(3)
        assertEquals(com.example.ui.components.FairySpeaker.GREENPIE, col1Ru.speaker)
        assertEquals("Это Колизей. Здесь арены идут друг за дружкой.", col1Ru.textTemplate)
        assertEquals(com.example.ui.components.FairySpeaker.BLUESWEET, col2Ru.speaker)
        assertEquals("Так что прокачай своих дамочек прежде чем сюда идти!", col2Ru.textTemplate)
        assertEquals("Чем дальше пройдешь - тем сложнее и больше приз!", col3Ru.textTemplate)

        val col1En = com.example.i18n.getColiseumTutorialFrameEn(1)
        val col2En = com.example.i18n.getColiseumTutorialFrameEn(2)
        val col3En = com.example.i18n.getColiseumTutorialFrameEn(3)
        val cyrillicRegex = Regex("[А-Яа-яЁё]")
        assertFalse(cyrillicRegex.containsMatchIn(col1En.textTemplate))
        assertFalse(cyrillicRegex.containsMatchIn(col2En.textTemplate))
        assertFalse(cyrillicRegex.containsMatchIn(col3En.textTemplate))
    }

    @Test
    fun `verify Brunhilde skill deals 100pct plus 25pct splash to neighbor or 125pct when alone`() {
        val brunSkill = HeroSkill("brun_skill_1", "Клинок справедливости", "", energyCost = 1, unlockLevel = 3)
        fun makeBrun() = BattleCard(
            heroId = "brunhilde", name = "Брунгильда", title = "", heroType = HeroType.MELEE,
            rarity = CardRarity.BASIC, element = Element.LIGHT, imageRes = R.drawable.hero_brunhilde,
            slotIndex = 0, isPlayer = true, level = 3, maxHp = 60, currentHp = 60,
            maxArmor = 0, currentArmor = 0, atk = 20, skill = brunSkill
        )
        fun makeEnemy(slot: Int) = BattleCard(
            heroId = "e$slot", name = "Враг$slot", title = "", heroType = HeroType.MELEE,
            rarity = CardRarity.BASIC, element = Element.DARK, imageRes = R.drawable.hero_olga,
            slotIndex = slot, isPlayer = false, level = 1, maxHp = 100, currentHp = 100,
            maxArmor = 0, currentArmor = 0, atk = 10
        )

        // Case 1: Front neighbor present (slot 0 and slot 1) -> 100% (20) to slot 0, 25% (5) to slot 1
        val vm1 = BattleViewModel()
        vm1.startBattle(listOf(makeBrun()), listOf(makeEnemy(0), makeEnemy(1)), "T", "T", false, 0, 0, 0) { _, _ -> }
        vm1.executePlayerSkill()
        assertEquals(80, vm1.battleState.value?.enemyCards?.first { it.slotIndex == 0 }?.currentHp)
        assertEquals(95, vm1.battleState.value?.enemyCards?.first { it.slotIndex == 1 }?.currentHp)

        // Case 2: No neighbor on line (only slot 0 alive) -> 100% + 25% = 125% (25) to slot 0
        val vm2 = BattleViewModel()
        vm2.startBattle(listOf(makeBrun()), listOf(makeEnemy(0)), "T", "T", false, 0, 0, 0) { _, _ -> }
        vm2.executePlayerSkill()
        assertEquals(75, vm2.battleState.value?.enemyCards?.first { it.slotIndex == 0 }?.currentHp)
    }

    @Test
    fun `verify Griselda, Mina, and Vlasta basic attacks follow exact damage and no-poison rules`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = GameDataRepository(context)
        val vlastaTemplate = repo.getInitialHeroCards().first { it.id == "vlasta" }
        assertEquals("Vlasta basic attack must not be POISON_MELEE", AttackPattern.SINGLE, vlastaTemplate.attackPattern)

        fun makeEnemy(slot: Int) = BattleCard(
            heroId = "e$slot", name = "E$slot", title = "", heroType = HeroType.HYBRID,
            rarity = CardRarity.BASIC, element = Element.DARK, imageRes = R.drawable.hero_olga,
            slotIndex = slot, isPlayer = false, level = 1, maxHp = 100, currentHp = 100,
            maxArmor = 0, currentArmor = 0, atk = 5
        )

        // Vlasta basic attack never applies poison
        val vlastaCard = BattleCard(
            heroId = "vlasta", name = "Власта", title = "", heroType = HeroType.MELEE,
            rarity = CardRarity.RARE, element = Element.FIRE, imageRes = R.drawable.hero_vlasta,
            slotIndex = 0, isPlayer = true, level = 1, maxHp = 50, currentHp = 50,
            maxArmor = 0, currentArmor = 0, atk = 20, attackPattern = vlastaTemplate.attackPattern
        )
        val vmVlasta = BattleViewModel()
        vmVlasta.startBattle(listOf(vlastaCard), listOf(makeEnemy(0)), "T", "T", false, 0, 0, 0) { _, _ -> }
        vmVlasta.executePlayerAttack(0)
        assertEquals(0, vmVlasta.battleState.value?.enemyCards?.first()?.poisonRounds)

        // Griselda DIAGONAL_PAIR: 2 enemies on diagonal (slots 0 & 3) -> 50% each (10, or 15 if 5% crit)
        val griseldaTwo = BattleCard(
            heroId = "baker", name = "Гризельда", title = "", heroType = HeroType.HYBRID,
            rarity = CardRarity.RARE, element = Element.FIRE, imageRes = R.drawable.hero_baker,
            slotIndex = 0, isPlayer = true, level = 1, maxHp = 50, currentHp = 50,
            maxArmor = 0, currentArmor = 0, atk = 20, attackPattern = AttackPattern.DIAGONAL_PAIR
        )
        val vmGris2 = BattleViewModel()
        vmGris2.startBattle(listOf(griseldaTwo), listOf(makeEnemy(0), makeEnemy(3)), "T", "T", false, 0, 0, 0) { _, _ -> }
        vmGris2.executePlayerAttack(0)
        val d0 = 100 - (vmGris2.battleState.value?.enemyCards?.first { it.slotIndex == 0 }?.currentHp ?: 100)
        val d3 = 100 - (vmGris2.battleState.value?.enemyCards?.first { it.slotIndex == 3 }?.currentHp ?: 100)
        assertTrue("Griselda 2-target diagonal damage should be 50% (10 or crit 15), was $d0", d0 == 10 || d0 == 15)
        assertEquals("Both diagonal targets take equal damage", d0, d3)

        // Griselda single diagonal enemy -> 100% (20, or crit 30)
        val griseldaOne = griseldaTwo.copy(hasActedThisRound = false)
        val vmGris1 = BattleViewModel()
        vmGris1.startBattle(listOf(griseldaOne), listOf(makeEnemy(0)), "T", "T", false, 0, 0, 0) { _, _ -> }
        vmGris1.executePlayerAttack(0)
        val dSingle = 100 - (vmGris1.battleState.value?.enemyCards?.first()?.currentHp ?: 100)
        assertTrue("Griselda 1-target damage should be 100% (20 or crit 30), was $dSingle", dSingle == 20 || dSingle == 30)

        // Mina ALL_ENEMIES: 4 -> 25% (25), 3 -> 33% (33), 2 -> 50% (50), 1 -> 100% (100)
        fun testMinaWithCount(count: Int, expectedNonCrit: Int, expectedCrit: Int) {
            val mina = BattleCard(
                heroId = "mi_and_na", name = "Ми и На", title = "", heroType = HeroType.HYBRID,
                rarity = CardRarity.SPECIAL, element = Element.FIRE, imageRes = R.drawable.hero_mi_and_na,
                slotIndex = 0, isPlayer = true, level = 1, maxHp = 50, currentHp = 50,
                maxArmor = 0, currentArmor = 0, atk = 100, attackPattern = AttackPattern.ALL_ENEMIES
            )
            val enemies = (0 until count).map { makeEnemy(it).copy(maxHp = 200, currentHp = 200) }
            val vmMina = BattleViewModel()
            vmMina.startBattle(listOf(mina), enemies, "T", "T", false, 0, 0, 0) { _, _ -> }
            vmMina.executePlayerAttack(0)
            val dmg = 200 - (vmMina.battleState.value?.enemyCards?.first()?.currentHp ?: 200)
            assertTrue("Mina with $count enemies should deal $expectedNonCrit (or crit $expectedCrit), got $dmg", dmg == expectedNonCrit || dmg == expectedCrit)
        }
        testMinaWithCount(4, 25, 37)
        testMinaWithCount(3, 33, 49)
        testMinaWithCount(2, 50, 75)
        testMinaWithCount(1, 100, 150)
    }

    @Test
    fun `verify Blind, Freeze, Charm skills, skip-turn inactivity, and DOT processing`() {
        fun makePlayer(slot: Int, id: String, skill: HeroSkill?) = BattleCard(
            heroId = id, name = id, title = "", heroType = HeroType.HYBRID,
            rarity = CardRarity.RARE, element = Element.WATER, imageRes = R.drawable.hero_isolda,
            slotIndex = slot, isPlayer = true, level = 5, maxHp = 100, currentHp = 100,
            maxArmor = 0, currentArmor = 0, atk = 20, skill = skill
        )
        fun makeEnemy(slot: Int) = BattleCard(
            heroId = "e$slot", name = "Enemy$slot", title = "", heroType = HeroType.MELEE,
            rarity = CardRarity.BASIC, element = Element.DARK, imageRes = R.drawable.hero_olga,
            slotIndex = slot, isPlayer = false, level = 1, maxHp = 100, currentHp = 100,
            maxArmor = 0, currentArmor = 0, atk = 10
        )

        // Test Mina blind, Isolda freeze, Lilith charm
        val minaSkill = HeroSkill("mina_skill_1", "Световая вспышка", "", energyCost = 1, unlockLevel = 3)
        val isoldaSkill = HeroSkill("isolda_skill_1", "Холодильник", "", energyCost = 1, unlockLevel = 3)
        val charmSkill = HeroSkill("dem_skill_2", "Приворот", "", energyCost = 1, unlockLevel = 5)

        val vmMina = BattleViewModel()
        vmMina.startBattle(listOf(makePlayer(0, "mi_and_na", minaSkill)), listOf(makeEnemy(0)), "T", "T", false, 0, 0, 0) { _, _ -> }
        vmMina.executePlayerSkill()
        val blindedEnemy = vmMina.battleState.value?.enemyCards?.first()!!
        assertTrue("Enemy must be blinded by mina_skill_1", blindedEnemy.isBlinded)
        assertTrue("Blinded enemy hasSkipTurnEffect must be true", blindedEnemy.hasSkipTurnEffect)

        val vmIsolda = BattleViewModel()
        vmIsolda.startBattle(listOf(makePlayer(0, "isolda", isoldaSkill)), listOf(makeEnemy(0)), "T", "T", false, 0, 0, 0) { _, _ -> }
        vmIsolda.executePlayerSkill()
        val frozenEnemy = vmIsolda.battleState.value?.enemyCards?.first()!!
        assertTrue("Enemy must be frozen by isolda_skill_1", frozenEnemy.isFrozen)
        assertTrue("Frozen enemy hasSkipTurnEffect must be true", frozenEnemy.hasSkipTurnEffect)

        val vmCharm = BattleViewModel()
        vmCharm.startBattle(listOf(makePlayer(0, "demoness", charmSkill)), listOf(makeEnemy(0), makeEnemy(1)), "T", "T", false, 0, 0, 0) { _, _ -> }
        vmCharm.executePlayerSkill()
        val charmedEnemy = vmCharm.battleState.value?.enemyCards?.first { it.slotIndex == 0 }!!
        val victimEnemy = vmCharm.battleState.value?.enemyCards?.first { it.slotIndex == 1 }!!
        assertTrue("Enemy must be charmed by dem_skill_2", charmedEnemy.isCharmed)
        assertTrue("Charmed enemy hasSkipTurnEffect must be true", charmedEnemy.hasSkipTurnEffect)
        assertEquals("Adjacent enemy takes charmed ally attack", 90, victimEnemy.currentHp)

        // Player card with skip-turn effect must NOT be selectable or active
        val frozenPlayerCard = makePlayer(0, "p0", null).apply { isFrozen = true; freezeRounds = 1 }
        val activePlayerCard = makePlayer(1, "p1", null)
        val vmSelect = BattleViewModel()
        vmSelect.startBattle(listOf(frozenPlayerCard, activePlayerCard), listOf(makeEnemy(0)), "T", "T", false, 0, 0, 0) { _, _ -> }
        assertEquals("Initial selected slot must skip frozen card and select slot 1", 1, vmSelect.battleState.value?.selectedPlayerSlot)
        vmSelect.selectPlayerSlot(0)
        assertEquals("Selecting frozen card must be rejected", 1, vmSelect.battleState.value?.selectedPlayerSlot)

        // After active card moves, 1-round freeze expires on end of player turn
        vmSelect.executePlayerAttack(0)
        assertFalse("1-round freeze clears when player turn finishes", frozenPlayerCard.isFrozen)

        // DOT processing: 1-round poison clears immediately on round 2 tick; 2-round burn stays at 1 round remaining
        val dotCard1 = makePlayer(0, "p_poison", null).apply { poisonRounds = 1; poisonAppliedRound = 1 }
        val dotCard2 = makePlayer(1, "p_burn", null).apply { burnRounds = 2; burnPercent = 20; burnAppliedRound = 1 }
        val res = vmSelect.processTeamDotEffects(listOf(dotCard1, dotCard2), currentRound = 2, isPlayerTeam = true)
        assertEquals(90, dotCard1.currentHp)
        assertEquals(0, dotCard1.poisonRounds)
        assertEquals(80, dotCard2.currentHp)
        assertEquals(1, dotCard2.burnRounds)
        assertEquals(setOf(true to 0, true to 1), res.hitTargets)
    }

    @Test
    fun `verify Iva and Alisa portraits change by level, unlock gallery and persist to json`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = GameDataRepository(app)

        assertEquals(R.drawable.hero_iva_0, resolveHeroImageForLevel("iva", 1, R.drawable.hero_iva))
        assertEquals(R.drawable.hero_iva_0, resolveHeroImageForLevel("iva", 2, R.drawable.hero_iva))
        assertEquals(R.drawable.hero_iva, resolveHeroImageForLevel("iva", 3, R.drawable.hero_iva))
        assertEquals(R.drawable.hero_iva, resolveHeroImageForLevel("iva", 4, R.drawable.hero_iva))
        assertEquals(R.drawable.hero_iva_1, resolveHeroImageForLevel("iva", 5, R.drawable.hero_iva))
        assertEquals(R.drawable.hero_iva_1, resolveHeroImageForLevel("iva", 7, R.drawable.hero_iva))
        assertEquals(R.drawable.hero_iva_2, resolveHeroImageForLevel("iva", 8, R.drawable.hero_iva))
        assertEquals(R.drawable.hero_iva_2, resolveHeroImageForLevel("iva", 9, R.drawable.hero_iva))
        assertEquals(R.drawable.hero_iva_3, resolveHeroImageForLevel("iva", 10, R.drawable.hero_iva))
        assertEquals(R.drawable.hero_iva_3, resolveHeroImageForLevel("iva", 11, R.drawable.hero_iva))
        assertEquals(R.drawable.hero_iva_max, resolveHeroImageForLevel("iva", 12, R.drawable.hero_iva))

        assertEquals(R.drawable.hero_alisa_0, resolveHeroImageForLevel("alisa", 1, R.drawable.hero_alisa))
        assertEquals(R.drawable.hero_alisa_0, resolveHeroImageForLevel("alisa", 2, R.drawable.hero_alisa))
        assertEquals(R.drawable.hero_alisa, resolveHeroImageForLevel("alisa", 3, R.drawable.hero_alisa))
        assertEquals(R.drawable.hero_alisa, resolveHeroImageForLevel("alisa", 4, R.drawable.hero_alisa))
        assertEquals(R.drawable.hero_alisa_1, resolveHeroImageForLevel("alisa", 5, R.drawable.hero_alisa))
        assertEquals(R.drawable.hero_alisa_1, resolveHeroImageForLevel("alisa", 7, R.drawable.hero_alisa))
        assertEquals(R.drawable.hero_alisa_2, resolveHeroImageForLevel("alisa", 8, R.drawable.hero_alisa))
        assertEquals(R.drawable.hero_alisa_2, resolveHeroImageForLevel("alisa", 9, R.drawable.hero_alisa))
        assertEquals(R.drawable.hero_alisa_3, resolveHeroImageForLevel("alisa", 10, R.drawable.hero_alisa))
        assertEquals(R.drawable.hero_alisa_3, resolveHeroImageForLevel("alisa", 11, R.drawable.hero_alisa))
        assertEquals(R.drawable.hero_alisa_max, resolveHeroImageForLevel("alisa", 12, R.drawable.hero_alisa))

        assertEquals(
            listOf(R.drawable.hero_iva_0, R.drawable.hero_iva, R.drawable.hero_iva_1),
            getUnlockedHeroGalleryImages("iva", 5, R.drawable.hero_iva_1)
        )
        assertEquals(
            listOf(
                R.drawable.hero_alisa_0,
                R.drawable.hero_alisa,
                R.drawable.hero_alisa_1,
                R.drawable.hero_alisa_2,
                R.drawable.hero_alisa_3,
                R.drawable.hero_alisa_max
            ),
            getUnlockedHeroGalleryImages("alisa", 12, R.drawable.hero_alisa_max)
        )

        val heroes = repo.getInitialHeroCards().map {
            when (it.id) {
                "iva" -> it.copy(level = 8, isUnlocked = true).withUpdatedImageForLevel()
                "alisa" -> it.copy(level = 12, isUnlocked = true).withUpdatedImageForLevel()
                else -> it
            }
        }
        val json = repo.exportSaveToJson(PlayerProfile(name = "IvaAlisaTester"), heroes, emptyList(), emptyMap())
        assertTrue(json.contains("\"imageResName\": \"hero_iva_2\""))
        assertTrue(json.contains("\"imageResName\": \"hero_alisa_max\""))

        val reloaded = repo.importSaveFromJson(json, repo.getInitialHeroCards())
        assertEquals(R.drawable.hero_iva_2, reloaded?.heroes?.first { it.id == "iva" }?.imageRes)
        assertEquals(R.drawable.hero_alisa_max, reloaded?.heroes?.first { it.id == "alisa" }?.imageRes)
    }

    @Test
    fun `verify Mi and Na portraits and Career plus Arena enemy portrait tiers never show max`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = GameDataRepository(app)

        // Mi and Na full 6-tier player portraits
        assertEquals(R.drawable.hero_mi_and_na_0, resolveHeroImageForLevel("mi_and_na", 1, R.drawable.hero_mi_and_na))
        assertEquals(R.drawable.hero_mi_and_na_0, resolveHeroImageForLevel("mi_and_na", 2, R.drawable.hero_mi_and_na))
        assertEquals(R.drawable.hero_mi_and_na, resolveHeroImageForLevel("mi_and_na", 3, R.drawable.hero_mi_and_na))
        assertEquals(R.drawable.hero_mi_and_na, resolveHeroImageForLevel("mi_and_na", 4, R.drawable.hero_mi_and_na))
        assertEquals(R.drawable.hero_mi_and_na_1, resolveHeroImageForLevel("mi_and_na", 5, R.drawable.hero_mi_and_na))
        assertEquals(R.drawable.hero_mi_and_na_1, resolveHeroImageForLevel("mi_and_na", 7, R.drawable.hero_mi_and_na))
        assertEquals(R.drawable.hero_mi_and_na_2, resolveHeroImageForLevel("mi_and_na", 8, R.drawable.hero_mi_and_na))
        assertEquals(R.drawable.hero_mi_and_na_2, resolveHeroImageForLevel("mi_and_na", 9, R.drawable.hero_mi_and_na))
        assertEquals(R.drawable.hero_mi_and_na_3, resolveHeroImageForLevel("mi_and_na", 10, R.drawable.hero_mi_and_na))
        assertEquals(R.drawable.hero_mi_and_na_3, resolveHeroImageForLevel("mi_and_na", 11, R.drawable.hero_mi_and_na))
        assertEquals(R.drawable.hero_mi_and_na_max, resolveHeroImageForLevel("mi_and_na", 12, R.drawable.hero_mi_and_na))
        assertEquals(
            listOf(
                R.drawable.hero_mi_and_na_0,
                R.drawable.hero_mi_and_na,
                R.drawable.hero_mi_and_na_1,
                R.drawable.hero_mi_and_na_2,
                R.drawable.hero_mi_and_na_3,
                R.drawable.hero_mi_and_na_max
            ),
            getUnlockedHeroGalleryImages("mi_and_na", 12, R.drawable.hero_mi_and_na_max)
        )

        // Vlasta player portraits
        assertEquals(R.drawable.hero_vlasta_0, resolveHeroImageForLevel("vlasta", 1, R.drawable.hero_vlasta))
        assertEquals(R.drawable.hero_vlasta_0, resolveHeroImageForLevel("vlasta", 2, R.drawable.hero_vlasta))
        assertEquals(R.drawable.hero_vlasta, resolveHeroImageForLevel("vlasta", 3, R.drawable.hero_vlasta))
        assertEquals(R.drawable.hero_vlasta, resolveHeroImageForLevel("vlasta", 4, R.drawable.hero_vlasta))
        assertEquals(R.drawable.hero_vlasta_1, resolveHeroImageForLevel("vlasta", 5, R.drawable.hero_vlasta))
        assertEquals(R.drawable.hero_vlasta_1, resolveHeroImageForLevel("vlasta", 7, R.drawable.hero_vlasta))
        assertEquals(R.drawable.hero_vlasta_2, resolveHeroImageForLevel("vlasta", 8, R.drawable.hero_vlasta))
        assertEquals(R.drawable.hero_vlasta_2, resolveHeroImageForLevel("vlasta", 9, R.drawable.hero_vlasta))
        assertEquals(R.drawable.hero_vlasta_3, resolveHeroImageForLevel("vlasta", 10, R.drawable.hero_vlasta))
        assertEquals(R.drawable.hero_vlasta_3, resolveHeroImageForLevel("vlasta", 11, R.drawable.hero_vlasta))
        assertEquals(R.drawable.hero_vlasta_max, resolveHeroImageForLevel("vlasta", 12, R.drawable.hero_vlasta))
        assertEquals(
            listOf(
                R.drawable.hero_vlasta_0,
                R.drawable.hero_vlasta,
                R.drawable.hero_vlasta_1,
                R.drawable.hero_vlasta_2,
                R.drawable.hero_vlasta_3,
                R.drawable.hero_vlasta_max
            ),
            getUnlockedHeroGalleryImages("vlasta", 12, R.drawable.hero_vlasta_max)
        )

        // Isolda & Baker (Griselda) full 6-tier portraits
        assertEquals(R.drawable.hero_isolda_0, resolveHeroImageForLevel("isolda", 1, R.drawable.hero_isolda))
        assertEquals(R.drawable.hero_isolda, resolveHeroImageForLevel("isolda", 4, R.drawable.hero_isolda))
        assertEquals(R.drawable.hero_isolda_1, resolveHeroImageForLevel("isolda", 6, R.drawable.hero_isolda))
        assertEquals(R.drawable.hero_isolda_2, resolveHeroImageForLevel("isolda", 9, R.drawable.hero_isolda))
        assertEquals(R.drawable.hero_isolda_3, resolveHeroImageForLevel("isolda", 11, R.drawable.hero_isolda))
        assertEquals(R.drawable.hero_isolda_max, resolveHeroImageForLevel("isolda", 12, R.drawable.hero_isolda))
        assertEquals(
            listOf(
                R.drawable.hero_isolda_0,
                R.drawable.hero_isolda,
                R.drawable.hero_isolda_1,
                R.drawable.hero_isolda_2,
                R.drawable.hero_isolda_3,
                R.drawable.hero_isolda_max
            ),
            getUnlockedHeroGalleryImages("isolda", 12, R.drawable.hero_isolda_max)
        )

        assertEquals(R.drawable.hero_baker_0, resolveHeroImageForLevel("baker", 2, R.drawable.hero_baker))
        assertEquals(R.drawable.hero_baker, resolveHeroImageForLevel("baker", 3, R.drawable.hero_baker))
        assertEquals(R.drawable.hero_baker_1, resolveHeroImageForLevel("baker", 7, R.drawable.hero_baker))
        assertEquals(R.drawable.hero_baker_2, resolveHeroImageForLevel("baker", 8, R.drawable.hero_baker))
        assertEquals(R.drawable.hero_baker_3, resolveHeroImageForLevel("baker", 10, R.drawable.hero_baker))
        assertEquals(R.drawable.hero_baker_max, resolveHeroImageForLevel("baker", 12, R.drawable.hero_baker))

        // Evanda full 6-tier portraits
        assertEquals(R.drawable.hero_evanda_0, resolveHeroImageForLevel("evanda", 1, R.drawable.hero_evanda))
        assertEquals(R.drawable.hero_evanda_0, resolveHeroImageForLevel("evanda", 2, R.drawable.hero_evanda))
        assertEquals(R.drawable.hero_evanda, resolveHeroImageForLevel("evanda", 3, R.drawable.hero_evanda))
        assertEquals(R.drawable.hero_evanda, resolveHeroImageForLevel("evanda", 4, R.drawable.hero_evanda))
        assertEquals(R.drawable.hero_evanda_1, resolveHeroImageForLevel("evanda", 5, R.drawable.hero_evanda))
        assertEquals(R.drawable.hero_evanda_1, resolveHeroImageForLevel("evanda", 7, R.drawable.hero_evanda))
        assertEquals(R.drawable.hero_evanda_2, resolveHeroImageForLevel("evanda", 8, R.drawable.hero_evanda))
        assertEquals(R.drawable.hero_evanda_2, resolveHeroImageForLevel("evanda", 9, R.drawable.hero_evanda))
        assertEquals(R.drawable.hero_evanda_3, resolveHeroImageForLevel("evanda", 10, R.drawable.hero_evanda))
        assertEquals(R.drawable.hero_evanda_3, resolveHeroImageForLevel("evanda", 11, R.drawable.hero_evanda))
        assertEquals(R.drawable.hero_evanda_max, resolveHeroImageForLevel("evanda", 12, R.drawable.hero_evanda))
        assertEquals(
            listOf(
                R.drawable.hero_evanda_0,
                R.drawable.hero_evanda,
                R.drawable.hero_evanda_1,
                R.drawable.hero_evanda_2,
                R.drawable.hero_evanda_3,
                R.drawable.hero_evanda_max
            ),
            getUnlockedHeroGalleryImages("evanda", 12, R.drawable.hero_evanda_max)
        )

        // Lilith (demoness) full 6-tier portraits
        assertEquals(R.drawable.hero_demoness_0, resolveHeroImageForLevel("demoness", 1, R.drawable.hero_demoness))
        assertEquals(R.drawable.hero_demoness_0, resolveHeroImageForLevel("demoness", 2, R.drawable.hero_demoness))
        assertEquals(R.drawable.hero_demoness, resolveHeroImageForLevel("demoness", 3, R.drawable.hero_demoness))
        assertEquals(R.drawable.hero_demoness, resolveHeroImageForLevel("demoness", 4, R.drawable.hero_demoness))
        assertEquals(R.drawable.hero_demoness_1, resolveHeroImageForLevel("demoness", 5, R.drawable.hero_demoness))
        assertEquals(R.drawable.hero_demoness_1, resolveHeroImageForLevel("demoness", 7, R.drawable.hero_demoness))
        assertEquals(R.drawable.hero_demoness_2, resolveHeroImageForLevel("demoness", 8, R.drawable.hero_demoness))
        assertEquals(R.drawable.hero_demoness_2, resolveHeroImageForLevel("demoness", 9, R.drawable.hero_demoness))
        assertEquals(R.drawable.hero_demoness_3, resolveHeroImageForLevel("demoness", 10, R.drawable.hero_demoness))
        assertEquals(R.drawable.hero_demoness_3, resolveHeroImageForLevel("demoness", 11, R.drawable.hero_demoness))
        assertEquals(R.drawable.hero_demoness_max, resolveHeroImageForLevel("demoness", 12, R.drawable.hero_demoness))
        assertEquals(
            listOf(
                R.drawable.hero_demoness_0,
                R.drawable.hero_demoness,
                R.drawable.hero_demoness_1,
                R.drawable.hero_demoness_2,
                R.drawable.hero_demoness_3,
                R.drawable.hero_demoness_max
            ),
            getUnlockedHeroGalleryImages("demoness", 12, R.drawable.hero_demoness_max)
        )

        // Helper functions used by HeroImageLoader / HeroPortraitImage
        assertEquals("isolda", inferHeroIdFromDrawableName("hero_isolda_max"))
        assertEquals("evanda", inferHeroIdFromDrawableName("hero_evanda_max"))
        assertEquals("demoness", inferHeroIdFromDrawableName("hero_demoness_max"))
        assertEquals("mi_and_na", inferHeroIdFromDrawableName("hero_mi_and_na_2"))
        assertEquals(R.drawable.hero_baker, resolveHeroFallbackDrawableRes("baker"))
        assertEquals(R.drawable.hero_evanda, resolveHeroFallbackDrawableRes("evanda"))
        assertEquals(R.drawable.hero_demoness, resolveHeroFallbackDrawableRes("demoness"))
        assertEquals(R.drawable.hero_vlasta, resolveHeroFallbackDrawableRes("", "Власта"))

        val heroes = repo.getInitialHeroCards().map {
            when (it.id) {
                "mi_and_na" -> it.copy(level = 12, isUnlocked = true).withUpdatedImageForLevel()
                "vlasta" -> it.copy(level = 8, isUnlocked = true).withUpdatedImageForLevel()
                "evanda" -> it.copy(level = 12, isUnlocked = true).withUpdatedImageForLevel()
                "demoness" -> it.copy(level = 12, isUnlocked = true).withUpdatedImageForLevel()
                else -> it
            }
        }
        val json = repo.exportSaveToJson(PlayerProfile(name = "MinaVlastaTester"), heroes, emptyList(), emptyMap())
        assertTrue(json.contains("\"imageResName\": \"hero_mi_and_na_max\""))
        assertTrue(json.contains("\"imageResName\": \"hero_vlasta_2\""))
        assertTrue(json.contains("\"imageResName\": \"hero_evanda_max\""))
        assertTrue(json.contains("\"imageResName\": \"hero_demoness_max\""))
        val reloaded = repo.importSaveFromJson(json, repo.getInitialHeroCards())
        assertEquals(R.drawable.hero_mi_and_na_max, reloaded?.heroes?.first { it.id == "mi_and_na" }?.imageRes)
        assertEquals(R.drawable.hero_vlasta_2, reloaded?.heroes?.first { it.id == "vlasta" }?.imageRes)
        assertEquals(R.drawable.hero_evanda_max, reloaded?.heroes?.first { it.id == "evanda" }?.imageRes)
        assertEquals(R.drawable.hero_demoness_max, reloaded?.heroes?.first { it.id == "demoness" }?.imageRes)

        // Career stage tier mapping: 1-1..1-5 -> 0; 1-6..2-5 -> std; 2-6..3-5 -> 1; 3-6..4-5 -> 2; 4-6 -> 3
        assertEquals(ENEMY_PORTRAIT_TIER_0, resolveEnemyPortraitTierForCareerStage(1, 1))
        assertEquals(ENEMY_PORTRAIT_TIER_0, resolveEnemyPortraitTierForCareerStage(1, 5))
        assertEquals(ENEMY_PORTRAIT_TIER_STANDARD, resolveEnemyPortraitTierForCareerStage(1, 6))
        assertEquals(ENEMY_PORTRAIT_TIER_STANDARD, resolveEnemyPortraitTierForCareerStage(2, 5))
        assertEquals(ENEMY_PORTRAIT_TIER_1, resolveEnemyPortraitTierForCareerStage(2, 6))
        assertEquals(ENEMY_PORTRAIT_TIER_1, resolveEnemyPortraitTierForCareerStage(3, 5))
        assertEquals(ENEMY_PORTRAIT_TIER_2, resolveEnemyPortraitTierForCareerStage(3, 6))
        assertEquals(ENEMY_PORTRAIT_TIER_2, resolveEnemyPortraitTierForCareerStage(4, 5))
        assertEquals(ENEMY_PORTRAIT_TIER_3, resolveEnemyPortraitTierForCareerStage(4, 6))

        // Verify startCampaignBattle applies stage tiers properly
        repo.savePlayerProfile(PlayerProfile(name = "CareerImgTester", arenaKeys = 5))
        val vm = GameViewModel(app)
        val chapters = vm.campaignChapters.value
        val stage15 = chapters[0].stages[4] // 1-5: brunhilde, olga, iva, berta -> tier 0
        vm.startCampaignBattle(stage15)
        assertEquals(R.drawable.hero_olga_0, vm.battleState.value?.enemyCards?.get(1)?.imageRes)
        assertEquals(R.drawable.hero_iva_0, vm.battleState.value?.enemyCards?.get(2)?.imageRes)

        val stage16 = chapters[0].stages[5] // 1-6: alisa, brunhilde, iva, witch -> standard
        vm.startCampaignBattle(stage16)
        assertEquals(R.drawable.hero_alisa, vm.battleState.value?.enemyCards?.get(0)?.imageRes)
        assertEquals(R.drawable.hero_brunhilde, vm.battleState.value?.enemyCards?.get(1)?.imageRes)
        assertNotNull(vm.battleState.value?.foundAmulet)

        val stage26 = chapters[1].stages[5] // 2-6: vlasta, olga, baker, mi_and_na -> tier 1
        vm.startCampaignBattle(stage26)
        assertEquals(R.drawable.hero_vlasta_1, vm.battleState.value?.enemyCards?.get(0)?.imageRes)
        assertEquals(R.drawable.hero_olga_1, vm.battleState.value?.enemyCards?.get(1)?.imageRes)

        val stage36 = chapters[2].stages[5] // 3-6: isolda, vlasta, mi_and_na, baker -> tier 2
        vm.startCampaignBattle(stage36)
        assertEquals(R.drawable.hero_isolda_2, vm.battleState.value?.enemyCards?.get(0)?.imageRes)
        assertEquals(R.drawable.hero_vlasta_2, vm.battleState.value?.enemyCards?.get(1)?.imageRes)
        assertEquals(R.drawable.hero_mi_and_na_2, vm.battleState.value?.enemyCards?.get(2)?.imageRes)

        val stage45 = chapters[3].stages[4] // 4-5: vlasta, brunhilde, berta, isolda -> tier 2
        vm.startCampaignBattle(stage45)
        assertEquals(R.drawable.hero_vlasta_2, vm.battleState.value?.enemyCards?.get(0)?.imageRes)
        assertEquals(R.drawable.hero_brunhilde_2, vm.battleState.value?.enemyCards?.get(1)?.imageRes)
        assertEquals(R.drawable.hero_berta_2, vm.battleState.value?.enemyCards?.get(2)?.imageRes)

        val stage46 = chapters[3].stages[5] // 4-6: evanda, vlasta, isolda, demoness -> tier 3
        vm.startCampaignBattle(stage46)
        assertEquals(R.drawable.hero_evanda_3, vm.battleState.value?.enemyCards?.get(0)?.imageRes)
        assertEquals(R.drawable.hero_vlasta_3, vm.battleState.value?.enemyCards?.get(1)?.imageRes)
        assertEquals(R.drawable.hero_isolda_3, vm.battleState.value?.enemyCards?.get(2)?.imageRes)
        assertEquals(R.drawable.hero_demoness_3, vm.battleState.value?.enemyCards?.get(3)?.imageRes)

        // Verify enemy portrait helper caps at _3 and never shows _max (including mi_and_na, evanda & demoness)
        assertEquals(R.drawable.hero_olga_3, resolveHeroImageForEnemyTier("olga", ENEMY_PORTRAIT_TIER_3, R.drawable.hero_olga))
        assertEquals(R.drawable.hero_olga_3, resolveHeroImageForEnemyTier("olga", 99, R.drawable.hero_olga))
        assertEquals(R.drawable.hero_evanda_3, resolveHeroImageForEnemyTier("evanda", 99, R.drawable.hero_evanda))
        assertEquals(R.drawable.hero_demoness_3, resolveHeroImageForEnemyTier("demoness", 99, R.drawable.hero_demoness))
        assertEquals(R.drawable.hero_mi_and_na_3, resolveHeroImageForEnemyTier("mi_and_na", ENEMY_PORTRAIT_TIER_3, R.drawable.hero_mi_and_na))

        // Verify Arena uses chapter HP thresholds and never shows _max even for high-level bots
        val lowBot = ArenaBot("Ольга", "Новичок", 100, R.drawable.app_logo, listOf("brunhilde", "olga", "berta", "witch"), listOf(1, 1, 1, 1))
        vm.startArenaBattle(lowBot)
        assertEquals(R.drawable.hero_olga_0, vm.battleState.value?.enemyCards?.get(1)?.imageRes)

        val midBot = ArenaBot("Ольга", "Средний", 300, R.drawable.app_logo, listOf("brunhilde", "olga", "berta", "iva"), listOf(9, 9, 9, 9))
        vm.startArenaBattle(midBot)
        assertEquals(R.drawable.hero_olga, vm.battleState.value?.enemyCards?.get(1)?.imageRes)

        val highBot = ArenaBot("Ольга", "Легенда", 900, R.drawable.app_logo, listOf("evanda", "olga", "mi_and_na", "vlasta"), listOf(30, 30, 30, 30))
        vm.startArenaBattle(highBot)
        assertEquals(R.drawable.hero_evanda_3, vm.battleState.value?.enemyCards?.get(0)?.imageRes)
        assertEquals(R.drawable.hero_olga_3, vm.battleState.value?.enemyCards?.get(1)?.imageRes)
        assertEquals(R.drawable.hero_mi_and_na_3, vm.battleState.value?.enemyCards?.get(2)?.imageRes)
        assertEquals(R.drawable.hero_vlasta_3, vm.battleState.value?.enemyCards?.get(3)?.imageRes)
    }

    @Test
    fun `replaying amulet stage never shows amulet popup or amulet drop message`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = GameDataRepository(app)

        // Stage 106 already cleared with 3 stars and amulet_south already found
        repo.savePlayerProfile(PlayerProfile(name = "AmuletRepeatTester", foundAmulets = listOf("amulet_south")))
        repo.saveCampaignStars(mapOf(106 to 3, 206 to 2))

        val vm = GameViewModel(app)
        val stage106 = vm.campaignChapters.value[0].stages.first { it.id == 106 }
        vm.startCampaignBattle(stage106)
        assertNull(vm.battleState.value?.foundAmulet)

        // Simulate clearing stage 106 again -> BattleViewModel invokes handleBattleFinished
        val enemies = vm.battleState.value?.enemyCards ?: emptyList()
        enemies.forEachIndexed { idx, card ->
            if (idx == 0) {
                card.currentArmor = 0
                card.currentHp = 1
            } else {
                card.currentArmor = 0
                card.currentHp = 0
                card.isAlive = false
            }
        }
        vm.dismissDropMessage()
        vm.battleViewModel.selectPlayerSlot(0)
        vm.battleViewModel.executePlayerAttack(0)
        assertEquals(BattlePhase.VICTORY, vm.battleState.value?.phase)
        val dropMsg = vm.latestDropMessage.value
        assertTrue("Repeat stage clear must not notify about finding amulet, got: $dropMsg", dropMsg == null || !dropMsg.contains("амулет", ignoreCase = true))

        // Even if stars were 0 on another save where foundAmulets already contains amulet_east, no amulet popup or message
        repo.savePlayerProfile(PlayerProfile(name = "AmuletOwnedTester", foundAmulets = listOf("amulet_east")))
        repo.saveCampaignStars(emptyMap())
        val vm2 = GameViewModel(app)
        val stage206 = vm2.campaignChapters.value[1].stages.first { it.id == 206 }
        vm2.startCampaignBattle(stage206)
        assertNull(vm2.battleState.value?.foundAmulet)
    }

    @Test
    fun `career selected chapter persists after exiting battle early and arena includes all four new backgrounds`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = GameViewModel(app)

        // Verify all 4 backgrounds are present in Arena pool
        assertTrue(GameViewModel.ARENA_BATTLE_BACKGROUNDS.contains(R.drawable.bg_iva))
        assertTrue(GameViewModel.ARENA_BATTLE_BACKGROUNDS.contains(R.drawable.bg_isolda))
        assertTrue(GameViewModel.ARENA_BATTLE_BACKGROUNDS.contains(R.drawable.bg_griselda))
        assertTrue(GameViewModel.ARENA_BATTLE_BACKGROUNDS.contains(R.drawable.bg_alisa))

        // Verify Career stages use expected backgrounds
        val chapters = vm.campaignChapters.value
        assertEquals(R.drawable.bg_iva, chapters[0].stages.first { it.id == 103 }.battleBgRes)
        assertEquals(R.drawable.bg_alisa, chapters[0].stages.first { it.id == 105 }.battleBgRes)
        assertEquals(R.drawable.bg_griselda, chapters[1].stages.first { it.id == 205 }.battleBgRes)
        assertEquals(R.drawable.bg_isolda, chapters[2].stages.first { it.id == 305 }.battleBgRes)

        // Start a Chapter 2 stage and exit battle early -> selectedCampaignChapterId remains 2
        vm.selectCampaignChapter(2)
        val stage202 = chapters[1].stages.first { it.id == 202 }
        vm.startCampaignBattle(stage202)
        assertEquals(2, vm.selectedCampaignChapterId.value)
        vm.exitBattle()
        assertNull(vm.battleState.value)
        assertEquals(2, vm.selectedCampaignChapterId.value)

        // Surrender on Chapter 3 stage and exit -> selectedCampaignChapterId remains 3
        val stage305 = chapters[2].stages.first { it.id == 305 }
        vm.startCampaignBattle(stage305)
        assertEquals(3, vm.selectedCampaignChapterId.value)
        vm.surrenderBattle()
        vm.exitBattle()
        assertNull(vm.battleState.value)
        assertEquals(3, vm.selectedCampaignChapterId.value)
    }

    @Test
    fun `chest shard selection gives 40 percent chance for owned hero cards`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = GameViewModel(app)
        val heroes = vm.allHeroes.value

        // Rolls 0..39 (40% probability) must select an already unlocked (owned) hero
        for (roll in listOf(0, 15, 39)) {
            val selected = vm.selectChestShardHero(heroes, CardRarity.RARE, preferOwnedRoll = roll)
            assertNotNull(selected)
            assertTrue("Roll $roll (< 40) must choose an owned hero", selected!!.isUnlocked)
        }

        // Rolls 40..99 (60% probability) must select a locked hero when locked eligible heroes exist
        for (roll in listOf(40, 65, 99)) {
            val selected = vm.selectChestShardHero(heroes, CardRarity.RARE, preferOwnedRoll = roll)
            assertNotNull(selected)
            assertFalse("Roll $roll (>= 40) must choose a locked hero", selected!!.isUnlocked)
        }
    }

    @Test
    fun `100 percent campaign completion triggers achievement popup once and unlocks bonus stage`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = GameDataRepository(app)

        // 23 stages at 4 stars + stage 406 at 3 stars -> NOT 100%
        val partialStars = mutableMapOf<Int, Int>()
        for (chapter in 1..4) {
            for (stage in 1..6) {
                partialStars[chapter * 100 + stage] = 4
            }
        }
        partialStars[406] = 3
        repo.savePlayerProfile(PlayerProfile(name = "Tester100"))
        repo.saveCampaignStars(partialStars)

        val vmPartial = GameViewModel(app)
        assertFalse(vmPartial.isCampaign100Percent.value)
        assertFalse(vmPartial.showCampaign100AchievementDialog.value)

        // All 24 stages at 4 stars -> 100% triggers achievement dialog and bonus unlock
        val fullStars = partialStars.toMutableMap().apply { put(406, 4) }
        repo.saveCampaignStars(fullStars)
        val vmFull = GameViewModel(app)
        assertTrue(vmFull.isCampaign100Percent.value)
        assertTrue(vmFull.showCampaign100AchievementDialog.value)

        // Dismissing persists isCampaign100AchievementShown so popup won't show again on reload
        vmFull.dismissCampaign100Achievement()
        assertFalse(vmFull.showCampaign100AchievementDialog.value)
        assertTrue(vmFull.playerProfile.value.isCampaign100AchievementShown)

        val vmReloaded = GameViewModel(app)
        assertTrue(vmReloaded.isCampaign100Percent.value)
        assertFalse(vmReloaded.showCampaign100AchievementDialog.value)

        // Export & import preserves achievement shown flag and 100% status
        val json = vmReloaded.exportSaveToJson()
        repo.savePlayerProfile(PlayerProfile(name = "Reset"))
        repo.saveCampaignStars(emptyMap())
        assertTrue(vmReloaded.importSaveFromJson(json))
        assertTrue(vmReloaded.isCampaign100Percent.value)
        assertTrue(vmReloaded.playerProfile.value.isCampaign100AchievementShown)
        assertFalse(vmReloaded.showCampaign100AchievementDialog.value)
    }

    @Test
    fun `evanda unlocks at career stage 4-4 and lilith unlocks at 4-6`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = GameDataRepository(app)
        val initialHeroes = repo.getInitialHeroCards()

        val evandaDef = initialHeroes.first { it.id == "evanda" }
        val lilithDef = initialHeroes.first { it.id == "demoness" }
        assertEquals(404, evandaDef.unlockCareerStageId)
        assertEquals(406, lilithDef.unlockCareerStageId)

        // Passing 4-4 unlocks Evanda immediately while Lilith stays locked
        repo.savePlayerProfile(PlayerProfile(name = "UnlockTester"))
        repo.saveHeroesState(initialHeroes)
        val starsAt404 = mapOf(404 to 1)
        repo.saveCampaignStars(starsAt404)
        val vm = GameViewModel(app)
        val evandaAfter404 = vm.allHeroes.value.first { it.id == "evanda" }
        val lilithAfter404 = vm.allHeroes.value.first { it.id == "demoness" }
        assertTrue("Evanda must be unlocked after 4-4", evandaAfter404.isUnlocked)
        assertFalse("Lilith must still be locked at 4-4", lilithAfter404.isUnlocked)

        // Passing 4-6 unlocks Lilith too
        repo.saveHeroesState(initialHeroes)
        repo.saveCampaignStars(mapOf(404 to 3, 406 to 2))
        val vmAfter406 = GameViewModel(app)
        assertTrue("Lilith must unlock after 4-6", vmAfter406.allHeroes.value.first { it.id == "demoness" }.isUnlocked)
    }

    @Test
    fun `lilith pierce line splits 50 percent damage on 2 targets and double fist deals 125 percent`() {
        fun makeDummy(slot: Int, isPlayer: Boolean, atk: Int = 10) = BattleCard(
            heroId = "dummy_$slot", name = "Target$slot", title = "", heroType = HeroType.MELEE,
            rarity = CardRarity.BASIC, element = Element.LIGHT, imageRes = R.drawable.hero_olga,
            slotIndex = slot, isPlayer = isPlayer, level = 1, maxHp = 200, currentHp = 200,
            maxArmor = 0, currentArmor = 0, atk = atk
        )

        // 1) Player Lilith PIERCE_LINE on 2 alive enemies in column 0 (slots 0 & 2) -> 50% (10, or 15 if 5% crit) each
        val playerLilith = BattleCard(
            heroId = "demoness", name = "Лилит", title = "", heroType = HeroType.HYBRID,
            rarity = CardRarity.RARE, element = Element.DARK, imageRes = R.drawable.hero_demoness,
            slotIndex = 0, isPlayer = true, level = 3, maxHp = 100, currentHp = 100,
            maxArmor = 0, currentArmor = 0, atk = 20, attackPattern = AttackPattern.PIERCE_LINE,
            skill = HeroSkill("dem_skill_1", "Двойной кулак", "", energyCost = 1, unlockLevel = 3)
        )
        val vmPlayer2 = BattleViewModel()
        vmPlayer2.startBattle(
            listOf(playerLilith),
            listOf(makeDummy(0, false), makeDummy(2, false)),
            "T", "T", false, 0, 0, 0
        ) { _, _ -> }
        vmPlayer2.executePlayerAttack(0)
        val e0Dmg = 200 - vmPlayer2.battleState.value!!.enemyCards.first { it.slotIndex == 0 }.currentHp
        val e2Dmg = 200 - vmPlayer2.battleState.value!!.enemyCards.first { it.slotIndex == 2 }.currentHp
        assertTrue("Lilith 2-target PIERCE_LINE should deal 50% (10 or crit 15), got $e0Dmg", e0Dmg == 10 || e0Dmg == 15)
        assertEquals("Both line targets should take equal 50% split damage", e0Dmg, e2Dmg)

        // 2) Player Lilith PIERCE_LINE on single target -> 100% (20, or 30 crit)
        val vmPlayer1 = BattleViewModel()
        vmPlayer1.startBattle(
            listOf(playerLilith.copy(hasActedThisRound = false)),
            listOf(makeDummy(0, false)),
            "T", "T", false, 0, 0, 0
        ) { _, _ -> }
        vmPlayer1.executePlayerAttack(0)
        val eSingleDmg = 200 - vmPlayer1.battleState.value!!.enemyCards.first().currentHp
        assertTrue("Lilith 1-target PIERCE_LINE should deal 100% (20 or crit 30), got $eSingleDmg", eSingleDmg == 20 || eSingleDmg == 30)

        // 3) Bot Lilith PIERCE_LINE uses the exact same 50% rule on 2 targets in column
        val botLilith = playerLilith.copy(isPlayer = false, hasActedThisRound = false)
        val vmBot = BattleViewModel()
        vmBot.startBattle(
            listOf(makeDummy(0, true), makeDummy(2, true)),
            listOf(botLilith),
            "T", "T", false, 0, 0, 0
        ) { _, _ -> }
        vmBot.performAttackWithPattern(botLilith, primaryTargetSlot = 0, isPlayerAttacker = false)
        val p0Dmg = 200 - vmBot.battleState.value!!.playerCards.first { it.slotIndex == 0 }.currentHp
        val p2Dmg = 200 - vmBot.battleState.value!!.playerCards.first { it.slotIndex == 2 }.currentHp
        assertTrue("Bot Lilith 2-target PIERCE_LINE should deal 50% (10 or crit 15), got $p0Dmg", p0Dmg == 10 || p0Dmg == 15)
        assertEquals("Bot Lilith should deal equal split damage to both targets", p0Dmg, p2Dmg)

        // 4) "Двойной кулак" (dem_skill_1) deals 125% ATK to both front-row enemies (slots 0 & 1 -> 25 dmg each for ATK 20)
        val vmSkill = BattleViewModel()
        vmSkill.startBattle(
            listOf(playerLilith.copy(hasActedThisRound = false)),
            listOf(makeDummy(0, false), makeDummy(1, false), makeDummy(2, false)),
            "T", "T", false, 0, 0, 0
        ) { _, _ -> }
        vmSkill.executePlayerSkill()
        val f0 = 200 - vmSkill.battleState.value!!.enemyCards.first { it.slotIndex == 0 }.currentHp
        val f1 = 200 - vmSkill.battleState.value!!.enemyCards.first { it.slotIndex == 1 }.currentHp
        val b2 = 200 - vmSkill.battleState.value!!.enemyCards.first { it.slotIndex == 2 }.currentHp
        assertEquals("dem_skill_1 must deal 125% ATK (25) to front slot 0", 25, f0)
        assertEquals("dem_skill_1 must deal 125% ATK (25) to front slot 1", 25, f1)
        assertEquals("dem_skill_1 must NOT hit back row", 0, b2)
    }

    @Test
    fun `daily bonus triggers next calendar morning and switches to Fortune Wheel after 7 consecutive days`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = GameDataRepository(app)

        fun localMillis(year: Int, monthZeroBased: Int, day: Int, hour: Int): Long {
            return java.util.Calendar.getInstance().apply {
                set(year, monthZeroBased, day, hour, 0, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
        }

        // Claimed Day 1 yesterday at 22:00, opening app today at 08:00 (only 10h later, same next calendar day!)
        val day1Evening = localMillis(2030, java.util.Calendar.MARCH, 10, 22)
        val day2Morning = localMillis(2030, java.util.Calendar.MARCH, 11, 8)
        val day2Night = localMillis(2030, java.util.Calendar.MARCH, 11, 23)
        repo.savePlayerProfile(
            PlayerProfile(
                name = "DailyTester",
                isTutorialCompleted = true,
                dailyStreak = 1,
                lastDailyClaimDate = "2030-03-10",
                lastDailyClaimTimestamp = day1Evening,
                maxSeenTimestamp = day1Evening
            )
        )
        val vm = GameViewModel(app)
        vm.dismissDailyBonus()
        vm.checkDailyLoginBonus(nowMs = day2Morning)
        assertEquals("Next calendar morning must grant Day 2 even if < 20h elapsed", 2, vm.dailyBonusReward.value?.dayNumber)
        assertNull(vm.fortuneWheelOffer.value)

        // Same day later -> no duplicate offer
        vm.claimDailyBonus(nowMs = day2Morning)
        vm.checkDailyLoginBonus(nowMs = day2Night)
        assertNull("Already claimed today -> no new daily dialog", vm.dailyBonusReward.value)

        // Streak 7 claimed yesterday -> Day 8 opens Fortune Wheel instead of regular daily bonus
        val day7Time = localMillis(2030, java.util.Calendar.MARCH, 16, 21)
        val day8Time = localMillis(2030, java.util.Calendar.MARCH, 17, 9)
        repo.savePlayerProfile(
            PlayerProfile(
                name = "WheelTester",
                isTutorialCompleted = true,
                gold = 100,
                gems = 20,
                dailyStreak = 7,
                lastDailyClaimDate = "2030-03-16",
                lastDailyClaimTimestamp = day7Time,
                maxSeenTimestamp = day7Time
            )
        )
        val vmWheel = GameViewModel(app)
        vmWheel.dismissDailyBonus()
        vmWheel.dismissFortuneWheel()
        vmWheel.checkDailyLoginBonus(nowMs = day8Time)
        val wheelOffer = vmWheel.fortuneWheelOffer.value
        assertNotNull("Day 8 consecutive login must open Fortune Wheel", wheelOffer)
        assertEquals(8, wheelOffer?.dayNumber)
        assertEquals(10, wheelOffer?.sectors?.size)
        assertEquals(2, wheelOffer?.sectors?.count { it.type == FortuneWheelSectorType.EMPTY })
        assertTrue(wheelOffer!!.sectors.any { it.type == FortuneWheelSectorType.GOLD_UP_TO_500 })
        assertTrue(wheelOffer.sectors.any { it.type == FortuneWheelSectorType.GOLD_UP_TO_1000 })
        assertTrue("While secret hero is locked, sector 1 offers SECRET_SHARDS_3", wheelOffer.sectors.any { it.type == FortuneWheelSectorType.SECRET_SHARDS_3 })
        assertFalse("Locked secret hero replaces SHARDS_5 with SECRET_SHARDS_3", wheelOffer.sectors.any { it.type == FortuneWheelSectorType.SHARDS_5 })
        assertTrue(wheelOffer.sectors.any { it.type == FortuneWheelSectorType.SHARDS_10 })
        assertTrue(wheelOffer.sectors.any { it.type == FortuneWheelSectorType.GEMS_40 })
        assertTrue(wheelOffer.sectors.any { it.type == FortuneWheelSectorType.GEMS_100 })
        assertTrue(wheelOffer.sectors.any { it.type == FortuneWheelSectorType.ARTIFACT_BASIC })
        assertTrue(wheelOffer.sectors.any { it.type == FortuneWheelSectorType.ARTIFACT_SPECIAL })

        // Verify SECRET_SHARDS_3 grants +3 anonymous secret shards and switches back to SHARDS_5 once unlocked
        val secretWheelOffer = FortuneWheelOffer(dayNumber = 8, sectors = vmWheel.buildFortuneWheelSectors())
        assertTrue(secretWheelOffer.sectors.any { it.type == FortuneWheelSectorType.SECRET_SHARDS_3 })
        val unlockedAll = repo.getInitialHeroCards().map { if (it.isSecret) it.copy(isUnlocked = true) else it }
        val revertedSectors = vmWheel.buildFortuneWheelSectors(unlockedAll)
        assertFalse(revertedSectors.any { it.type == FortuneWheelSectorType.SECRET_SHARDS_3 })
        assertTrue(revertedSectors.any { it.type == FortuneWheelSectorType.SHARDS_5 })

        // Spin once -> grants reward & persists streak=8; second spin on same day is ignored
        val gemsSector = wheelOffer.sectors.first { it.type == FortuneWheelSectorType.GEMS_100 }
        vmWheel.claimFortuneWheelReward(gemsSector, nowMs = day8Time)
        assertEquals(120, vmWheel.playerProfile.value.gems)
        assertEquals(8, vmWheel.playerProfile.value.dailyStreak)
        assertEquals("2030-03-17", vmWheel.playerProfile.value.lastDailyClaimDate)
        assertNotNull(vmWheel.fortuneWheelResult.value)

        val secondGemsSector = wheelOffer.sectors.first { it.type == FortuneWheelSectorType.GEMS_40 }
        vmWheel.claimFortuneWheelReward(secondGemsSector, nowMs = day8Time)
        assertEquals("Only 1 spin per day is permitted", 120, vmWheel.playerProfile.value.gems)

        // Skipping a day (March 17 -> March 19, diff = 2) resets streak to 1 and exits Fortune Wheel
        val missedDayMorning = localMillis(2030, java.util.Calendar.MARCH, 19, 10)
        vmWheel.dismissFortuneWheel()
        vmWheel.checkDailyLoginBonus(nowMs = missedDayMorning)
        assertNull("Missed a day -> Fortune Wheel must not open", vmWheel.fortuneWheelOffer.value)
        assertEquals("Missed a day -> resets back to Day 1", 1, vmWheel.dailyBonusReward.value?.dayNumber)
    }

    @Test
    fun `hero quotes omit all level mentions and have expanded phrases per hero`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = GameDataRepository(app)
        val heroes = repo.getInitialHeroCards()
        assertEquals(14, heroes.size)

        val tierSampleLevels = listOf(1, 3, 5, 8, 10, 12)
        val forbiddenLevelPattern = Regex("(уровн|ур\\.|\\blvl\\b)", RegexOption.IGNORE_CASE)

        for (hero in heroes.filter { !it.isSecret }) {
            var totalPhrases = 0
            for (lvl in tierSampleLevels) {
                val startPhrases = getHeroPhrasesForLevel(hero.id, lvl, QuoteContext.BATTLE_START)
                val victoryPhrases = getHeroPhrasesForLevel(hero.id, lvl, QuoteContext.VICTORY)
                assertTrue("${hero.id} tier level $lvl start phrases should be >= 3", startPhrases.size >= 3)
                assertTrue("${hero.id} tier level $lvl victory phrases should be >= 3", victoryPhrases.size >= 3)
                totalPhrases += startPhrases.size + victoryPhrases.size

                for (phrase in startPhrases + victoryPhrases) {
                    assertFalse(
                        "Phrase for ${hero.id} lvl $lvl must not mention level: '$phrase'",
                        forbiddenLevelPattern.containsMatchIn(phrase)
                    )
                }
            }
            assertTrue("Hero ${hero.id} should have at least 36 expanded phrases, got $totalPhrases", totalPhrases >= 36)
        }
        val secretElios = heroes.first { it.id == "elios" }
        assertTrue(secretElios.isSecret)
        for (lvl in tierSampleLevels) {
            assertTrue(getHeroPhrasesForLevel("elios", lvl, QuoteContext.BATTLE_START).isEmpty())
            assertTrue(getHeroPhrasesForLevel("elios", lvl, QuoteContext.VICTORY).isEmpty())
        }
    }

    @Test
    fun `dialogue quotes use dedicated face previews for all 12 heroes instead of shrunk full-size portraits`() {
        val expectedFaceDrawables = mapOf(
            "brunhilde" to R.drawable.hero_brunhilde_face,
            "olga" to R.drawable.hero_olga_face,
            "berta" to R.drawable.hero_berta_face,
            "witch" to R.drawable.hero_witch_face,
            "alisa" to R.drawable.hero_alice_face,
            "isolda" to R.drawable.hero_isolda_face,
            "evanda" to R.drawable.hero_evanda_face,
            "vlasta" to R.drawable.hero_vlasta_face,
            "iva" to R.drawable.hero_iva_face,
            "demoness" to R.drawable.hero_demoness_face,
            "baker" to R.drawable.hero_baker_face,
            "mi_and_na" to R.drawable.hero_mi_and_na_face
        )

        for ((heroId, expectedFaceRes) in expectedFaceDrawables) {
            val fullRes = resolveHeroFallbackDrawableRes(heroId)
            val card = BattleCard(
                heroId = heroId,
                name = heroId,
                title = "",
                heroType = HeroType.HYBRID,
                rarity = CardRarity.RARE,
                element = Element.LIGHT,
                imageRes = fullRes,
                slotIndex = 0,
                isPlayer = true,
                maxHp = 50,
                currentHp = 50,
                maxArmor = 0,
                currentArmor = 0,
                atk = 10,
                level = 1
            )
            assertEquals(expectedFaceRes, resolveHeroFaceDrawableRes(heroId, fullRes, heroLevel = 1))
            assertEquals(expectedFaceRes, pickHeroBattleQuote(card, QuoteContext.BATTLE_START)?.imageRes)
            assertEquals(expectedFaceRes, pickHeroBattleQuote(card, QuoteContext.VICTORY)?.imageRes)
        }

        // Level-upgraded dialogue face portraits (5+ and 8+ thresholds)
        val upgradedExpectations = listOf(
            Triple("evanda", 7, R.drawable.hero_evanda_face),
            Triple("evanda", 8, R.drawable.hero_evanda_face_2),
            Triple("demoness", 7, R.drawable.hero_demoness_face),
            Triple("demoness", 8, R.drawable.hero_demoness_face_2),
            Triple("vlasta", 7, R.drawable.hero_vlasta_face),
            Triple("vlasta", 8, R.drawable.hero_vlasta_face_2),
            Triple("mi_and_na", 7, R.drawable.hero_mi_and_na_face),
            Triple("mi_and_na", 8, R.drawable.hero_mi_and_na_face_2),
            Triple("alisa", 7, R.drawable.hero_alice_face),
            Triple("alisa", 8, R.drawable.hero_alice_face_2),
            Triple("isolda", 4, R.drawable.hero_isolda_face),
            Triple("isolda", 5, R.drawable.hero_isolda_face_1),
            Triple("isolda", 7, R.drawable.hero_isolda_face_1),
            Triple("isolda", 8, R.drawable.hero_isolda_face_2),
            Triple("isolda", 12, R.drawable.hero_isolda_face_2),
            Triple("baker", 4, R.drawable.hero_baker_face),
            Triple("baker", 5, R.drawable.hero_baker_face_2),
            Triple("baker", 10, R.drawable.hero_baker_face_2)
        )
        for ((heroId, lv, expectedFace) in upgradedExpectations) {
            val fullRes = resolveHeroFallbackDrawableRes(heroId)
            val card = BattleCard(
                heroId = heroId,
                name = heroId,
                title = "",
                heroType = HeroType.HYBRID,
                rarity = CardRarity.RARE,
                element = Element.LIGHT,
                imageRes = fullRes,
                slotIndex = 0,
                isPlayer = true,
                maxHp = 50,
                currentHp = 50,
                maxArmor = 0,
                currentArmor = 0,
                atk = 10,
                level = lv
            )
            assertEquals("hero=$heroId lv=$lv", expectedFace, resolveHeroFaceDrawableRes(heroId, fullRes, lv))
            assertEquals("quote start hero=$heroId lv=$lv", expectedFace, pickHeroBattleQuote(card, QuoteContext.BATTLE_START)?.imageRes)
            assertEquals("quote victory hero=$heroId lv=$lv", expectedFace, pickHeroBattleQuote(card, QuoteContext.VICTORY)?.imageRes)
        }

        // Unknown hero IDs safely fall back to their full portrait
        assertEquals(R.drawable.hero_brunhilde_1, resolveHeroFaceDrawableRes("unknown_hero", R.drawable.hero_brunhilde_1))
    }

    @Test
    fun `daily quests catalog has 36 quests, scales progressively across 5 tiers, and filters infeasible quests`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = GameDataRepository(app)
        val catalog = repo.getDailyQuestCatalog()

        assertEquals("Daily quest catalog must contain 36 quests", 36, catalog.size)
        for (tier in 1..5) {
            val expectedCount = if (tier == 4) 8 else 7
            assertEquals("Tier $tier should have $expectedCount quests", expectedCount, catalog.count { it.difficultyTier == tier })
        }

        // Player with all 96 Career stars earned, only starter 4 heroes unlocked at lvl 1, empty bag
        val starterHeroes = repo.getInitialHeroCards().map {
            it.copy(isUnlocked = it.id in listOf("brunhilde", "olga", "berta", "witch"), level = 1)
        }
        val allStarsMap = repo.getCampaignChapters().flatMap { it.stages }.associate { it.id to 4 }
        repo.savePlayerProfile(PlayerProfile(name = "QuestTester", gold = 2000, gems = 200))
        repo.saveHeroesState(starterHeroes)
        repo.saveCampaignStars(allStarsMap)
        repo.saveArtifacts(emptyList())
        repo.saveDailyQuestDate("")

        val vm = GameViewModel(app)
        val quests = vm.dailyQuests.value
        assertEquals("Daily quests list must contain 5 quests", 5, quests.size)
        assertEquals(listOf(1, 2, 3, 4, 5), quests.map { it.difficultyTier })

        // Verify rewards progress strictly in ascending difficulty order (for same reward types)
        val goldQuests = quests.filter { it.rewardType == QuestRewardType.GOLD }
        for (i in 1 until goldQuests.size) {
            assertTrue(
                "Higher tier gold quest must award >= lower tier gold quest",
                goldQuests[i].rewardAmount >= goldQuests[i - 1].rewardAmount
            )
        }
        val shardQuests = quests.filter { it.rewardType == QuestRewardType.SHARDS }
        for (i in 1 until shardQuests.size) {
            assertTrue(
                "Higher tier shard quest must award >= lower tier shard quest",
                shardQuests[i].rewardAmount >= shardQuests[i - 1].rewardAmount
            )
        }

        // Verify infeasible quests for a maxed-stars, lvl-1, empty-bag state are filtered out
        assertTrue(quests.none { it.type == QuestType.EARN_CAREER_STARS })
        assertTrue(quests.none { it.type == QuestType.CAST_SKILLS })
        assertTrue(quests.none { it.type == QuestType.FORGE_COMBINE })
        assertTrue(quests.none { it.type == QuestType.SELL_ARTIFACT })

        // Separate check: starter roster (0 campaign stars -> only 2 Ranged heroes unlocked) forbids WIN_ARENA_RANGED_ONLY
        repo.saveCampaignStars(emptyMap())
        repo.saveHeroesState(starterHeroes)
        repo.saveDailyQuestDate("")
        val starterVm = GameViewModel(app)
        val rangedTemplate = catalog.first { it.type == QuestType.WIN_ARENA_RANGED_ONLY }
        assertFalse(starterVm.isQuestTemplateFeasible(rangedTemplate))
        assertTrue(starterVm.dailyQuests.value.none { it.type == QuestType.WIN_ARENA_RANGED_ONLY })

        val fourRangedHeroes = repo.getInitialHeroCards().map {
            it.copy(isUnlocked = it.heroType == HeroType.RANGED || it.id in listOf("brunhilde", "olga"), level = 4)
        }
        assertTrue(starterVm.isQuestTemplateFeasible(rangedTemplate, heroes = fourRangedHeroes))
    }

    @Test
    fun `daily quests alternate across days, track chest progress, claim rewards, and emit ChestOpeningResult`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = GameDataRepository(app)

        repo.savePlayerProfile(PlayerProfile(name = "ChestQuest", gold = 5000, gems = 500, isTutorialCompleted = true))
        repo.saveHeroesState(repo.getInitialHeroCards())

        // Seed a guaranteed OPEN_CHESTS quest alongside 4 other tier quests for Day 1
        val cal = java.util.Calendar.getInstance().apply {
            set(2031, java.util.Calendar.MAY, 10, 12, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val day1Ms = cal.timeInMillis
        val day1Str = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(day1Ms))
        val day1Seed = listOf(
            DailyQuestState("q_t1_chest_1", "Искательница", "Открой 1 любой сундук", QuestType.OPEN_CHESTS, 1, 1, 0, QuestRewardType.SHARDS, 2),
            DailyQuestState("q_t2_any_2", "Двойной триумф", "Выиграй 2 боя", QuestType.WIN_ANY_BATTLES, 2, 2, 0, QuestRewardType.GOLD, 230),
            DailyQuestState("q_t3_arena_2", "Боец Арены", "Выиграй 2 боя на Арене", QuestType.WIN_ARENA_BATTLES, 3, 2, 0, QuestRewardType.GOLD, 360),
            DailyQuestState("q_t4_any_4", "Серия побед", "Выиграй 4 боя", QuestType.WIN_ANY_BATTLES, 4, 4, 0, QuestRewardType.GOLD, 540),
            DailyQuestState("q_t5_enemies_18", "Буря клинков", "Уничтожь 18 врагов", QuestType.DEFEAT_ENEMIES, 5, 18, 0, QuestRewardType.GOLD, 800)
        )
        repo.saveDailyQuestDate(day1Str)
        repo.saveDailyQuests(day1Seed)

        val vm = GameViewModel(app)
        vm.refreshDailyQuestsIfNeeded(day1Ms)
        assertEquals(5, vm.dailyQuests.value.size)

        // Open a chest -> increments OPEN_CHESTS quest to 1/1 AND emits ChestOpeningResult
        val totalShardsBefore = vm.allHeroes.value.sumOf { it.shards }
        vm.openSummonChest(com.example.ui.screens.ChestType.BASIC)
        val openingResult = vm.chestOpeningResult.value
        assertNotNull("Opening a chest must emit ChestOpeningResult for animation", openingResult)
        assertTrue((openingResult?.shardAmount ?: 0) > 0)
        assertTrue(vm.allHeroes.value.sumOf { it.shards } > totalShardsBefore)

        vm.dismissChestOpening()
        assertNull(vm.chestOpeningResult.value)

        val chestQuest = vm.dailyQuests.value.first { it.id == "q_t1_chest_1" }
        assertTrue("Chest quest should be completed after opening chest", chestQuest.isCompleted)
        assertFalse(chestQuest.isRewardClaimed)

        // Claim shard reward
        val shardsAfterChest = vm.allHeroes.value.sumOf { it.shards }
        vm.claimDailyQuestReward("q_t1_chest_1")
        assertTrue(vm.dailyQuests.value.first { it.id == "q_t1_chest_1" }.isRewardClaimed)
        assertEquals(shardsAfterChest + 2, vm.allHeroes.value.sumOf { it.shards })

        // Claiming a second time must be ignored
        vm.claimDailyQuestReward("q_t1_chest_1")
        assertEquals(shardsAfterChest + 2, vm.allHeroes.value.sumOf { it.shards })

        // Advance to Day 2 -> quests regenerate and alternate (do not repeat Day 1 IDs)
        val day2Ms = day1Ms + 24L * 3600L * 1000L
        vm.refreshDailyQuestsIfNeeded(day2Ms)
        val day2Quests = vm.dailyQuests.value
        assertEquals(5, day2Quests.size)
        val day1Ids = day1Seed.map { it.id }.toSet()
        assertTrue(
            "Day 2 quests should alternate away from Day 1 quests",
            day2Quests.none { it.id in day1Ids }
        )
    }

    @Test
    fun `battle tracks playerSkillsUsedCount when casting hero skills`() {
        val bvm = BattleViewModel()
        val skill = HeroSkill("olga_skill_1", "Боевой клич", "", energyCost = 1, unlockLevel = 3)
        val playerCards = listOf(
            BattleCard(
                heroId = "olga", name = "Ольга", title = "", heroType = HeroType.MELEE,
                rarity = CardRarity.BASIC, element = Element.FIRE, imageRes = R.drawable.hero_olga,
                slotIndex = 0, isPlayer = true, level = 3, maxHp = 60, currentHp = 60,
                maxArmor = 10, currentArmor = 10, atk = 20, skill = skill
            )
        )
        val enemyCards = listOf(
            BattleCard(
                heroId = "e0", name = "Враг", title = "", heroType = HeroType.MELEE,
                rarity = CardRarity.BASIC, element = Element.DARK, imageRes = R.drawable.hero_olga,
                slotIndex = 0, isPlayer = false, level = 1, maxHp = 120, currentHp = 120,
                maxArmor = 0, currentArmor = 0, atk = 5
            )
        )

        bvm.startBattle(
            playerCards = playerCards,
            enemyCards = enemyCards,
            enemyName = "Тест",
            enemyTitle = "Тест",
            isArena = false,
            rewardGold = 0,
            rewardGems = 0,
            rewardExp = 0,
            onBattleEnd = { _, _ -> }
        )
        bvm.executePlayerSkill()

        assertEquals(1, bvm.battleState.value?.playerSkillsUsedCount)
    }

    @Test
    fun `language state supports fresh install selection, switching, persistence, and existing-player migration`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val prefs = app.getSharedPreferences("plump_heroes_prefs", Context.MODE_PRIVATE)
        try {
            prefs.edit().clear().commit()
            GameLocaleState.resetForTest()
            GameLocaleState.init(app)
            assertFalse("Fresh install must require language selection before nickname", GameLocaleState.hasChosenLanguage)
            assertEquals(AppLanguage.RU, GameLocaleState.language)

            // User chooses English on first launch
            GameLocaleState.setLanguage(app, AppLanguage.EN, markChosen = true)
            assertTrue(GameLocaleState.hasChosenLanguage)
            assertTrue(GameLocaleState.isEnglish)
            GameLocaleState.resetForTest()
            GameLocaleState.init(app)
            assertTrue("Persisted language selection must survive app restart", GameLocaleState.hasChosenLanguage)
            assertEquals(AppLanguage.EN, GameLocaleState.language)

            // User switches to Russian in Settings
            GameLocaleState.setLanguage(app, AppLanguage.RU, markChosen = true)
            GameLocaleState.resetForTest()
            GameLocaleState.init(app)
            assertEquals(AppLanguage.RU, GameLocaleState.language)

            // Existing player upgrading (tutorial_completed or profile_name without language_selected)
            prefs.edit().clear().putBoolean("tutorial_completed", true).commit()
            GameLocaleState.resetForTest()
            GameLocaleState.init(app)
            assertTrue("Upgrading players must skip first-launch language modal", GameLocaleState.hasChosenLanguage)
            assertEquals(AppLanguage.RU, GameLocaleState.language)
        } finally {
            prefs.edit().clear().commit()
            GameLocaleState.resetForTest(AppLanguage.RU, chosen = false)
        }
    }

    @Test
    fun `english localization covers all heroes, skills, artifacts, chapters, stages, quests, tutorial frames, and hero quotes`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = GameDataRepository(app)
        val cyrillicRegex = Regex("[А-Яа-яЁё]")
        val forbiddenLevelPattern = Regex("(уровн|ур\\.|\\blvl\\b)", RegexOption.IGNORE_CASE)

        try {
            GameLocaleState.resetForTest(AppLanguage.EN, chosen = true)

            // 1. All 14 heroes & skills
            val heroes = repo.getInitialHeroCards()
            assertEquals(14, heroes.size)
            for (hero in heroes) {
                val enName = hero.localizedName()
                val enTitle = hero.localizedTitle()
                val enLore = hero.localizedLore()
                assertTrue(enName.isNotBlank())
                assertTrue(enTitle.isNotBlank())
                assertTrue(enLore.isNotBlank())
                assertFalse("Hero ${hero.id} EN name contains Cyrillic: $enName", cyrillicRegex.containsMatchIn(enName))
                assertFalse("Hero ${hero.id} EN title contains Cyrillic: $enTitle", cyrillicRegex.containsMatchIn(enTitle))
                assertFalse("Hero ${hero.id} EN lore contains Cyrillic: $enLore", cyrillicRegex.containsMatchIn(enLore))

                for (skill in hero.availableSkills) {
                    val sName = skill.localizedName()
                    val sDesc = skill.localizedDescription()
                    assertTrue(sName.isNotBlank())
                    assertTrue(sDesc.isNotBlank())
                    assertFalse("Skill ${skill.id} EN name contains Cyrillic: $sName", cyrillicRegex.containsMatchIn(sName))
                    assertFalse("Skill ${skill.id} EN desc contains Cyrillic: $sDesc", cyrillicRegex.containsMatchIn(sDesc))
                }
            }

            // 2. All catalog + starter artifacts
            val artifacts = repo.getArtifactCatalog().values + repo.getInitialArtifacts()
            for (art in artifacts) {
                val aName = art.localizedName()
                val aDesc = art.localizedEffectDescription()
                assertTrue(aName.isNotBlank())
                assertTrue(aDesc.isNotBlank())
                assertFalse("Artifact ${art.id} EN name contains Cyrillic: $aName", cyrillicRegex.containsMatchIn(aName))
                assertFalse("Artifact ${art.id} EN desc contains Cyrillic: $aDesc", cyrillicRegex.containsMatchIn(aDesc))
            }

            // 3. All 4 chapters and 24 stages
            val chapters = repo.getCampaignChapters()
            assertEquals(4, chapters.size)
            for (chapter in chapters) {
                assertFalse(cyrillicRegex.containsMatchIn(chapter.localizedName()))
                assertFalse(cyrillicRegex.containsMatchIn(chapter.localizedSubtitle()))
                for (stage in chapter.stages) {
                    assertFalse("Stage ${stage.id} EN title contains Cyrillic", cyrillicRegex.containsMatchIn(stage.localizedTitle()))
                }
            }

            // 4. All 36 daily quests
            val quests = repo.getDailyQuestCatalog()
            assertEquals(36, quests.size)
            for (quest in quests) {
                assertFalse("Quest ${quest.id} EN title contains Cyrillic", cyrillicRegex.containsMatchIn(quest.localizedTitle()))
                assertFalse("Quest ${quest.id} EN desc contains Cyrillic", cyrillicRegex.containsMatchIn(quest.localizedDescription()))
            }

            // 5. All 24 tutorial frames & world lore
            for (step in 1..24) {
                val frame = getTutorialFrameEn(step, "TestHero")
                assertTrue(frame.textTemplate.isNotBlank())
                assertFalse("Tutorial step $step EN text contains Cyrillic", cyrillicRegex.containsMatchIn(frame.textTemplate))
            }
            assertTrue(WORLD_LORE_EN.isNotBlank())
            assertFalse("WORLD_LORE_EN contains Cyrillic", cyrillicRegex.containsMatchIn(WORLD_LORE_EN))

            // 6. All standard hero quotes (12 non-secret heroes x 6 level tiers x 2 contexts); secret hero has none
            val tierSampleLevels = listOf(1, 3, 5, 8, 10, 12)
            for (hero in heroes) {
                if (hero.isSecret) {
                    for (lvl in tierSampleLevels) {
                        assertTrue(getHeroPhrasesForLevel(hero.id, lvl, QuoteContext.BATTLE_START).isEmpty())
                        assertTrue(getHeroPhrasesForLevel(hero.id, lvl, QuoteContext.VICTORY).isEmpty())
                    }
                    continue
                }
                for (lvl in tierSampleLevels) {
                    val phrases = getHeroPhrasesForLevel(hero.id, lvl, QuoteContext.BATTLE_START) +
                        getHeroPhrasesForLevel(hero.id, lvl, QuoteContext.VICTORY)
                    assertTrue(phrases.size >= 6)
                    for (phrase in phrases) {
                        assertFalse("EN phrase for ${hero.id} lvl $lvl has Cyrillic: $phrase", cyrillicRegex.containsMatchIn(phrase))
                        assertFalse("EN phrase for ${hero.id} lvl $lvl mentions level: $phrase", forbiddenLevelPattern.containsMatchIn(phrase))
                    }
                }
            }
        } finally {
            GameLocaleState.resetForTest(AppLanguage.RU, chosen = false)
        }
    }

    @Test
    fun `campaign victory in English mode matches stage via campaignStageId and awards stars`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = GameDataRepository(app)
        val prefs = app.getSharedPreferences("plump_heroes_prefs", Context.MODE_PRIVATE)
        try {
            val deckIds = listOf("brunhilde", "olga", "berta", "witch")
            val strongHeroes = repo.getInitialHeroCards().map { hero ->
                if (hero.id in deckIds) hero.copy(isUnlocked = true, level = 12) else hero
            }
            repo.savePlayerProfile(
                PlayerProfile(
                    name = "Guardian",
                    gold = 100,
                    isTutorialCompleted = true,
                    activeDeckHeroIds = deckIds
                )
            )
            repo.saveHeroesState(strongHeroes)
            repo.saveCampaignStars(emptyMap())
            GameLocaleState.setLanguage(app, AppLanguage.EN, markChosen = true)

            val gameVm = GameViewModel(app)
            val stage101 = gameVm.campaignChapters.value.first().stages.first()
            assertEquals(101, stage101.id)
            assertEquals("1-1 Morgana's Cottage", stage101.localizedTitle())

            gameVm.startCampaignBattle(stage101)
            val initialState = gameVm.battleState.value
            assertNotNull(initialState)
            assertEquals(101, initialState?.campaignStageId)
            assertEquals("1-1 Morgana's Cottage", initialState?.enemyName)

            // Execute player attacks until all enemies in stage 101 are defeated
            var safety = 0
            while (gameVm.battleState.value?.phase == BattlePhase.PLAYER_TURN && safety < 10) {
                safety++
                val s = gameVm.battleState.value!!
                val attacker = s.playerCards.firstOrNull { it.slotIndex == s.selectedPlayerSlot && it.isAlive && !it.hasActedThisRound }
                    ?: s.playerCards.firstOrNull { it.isAlive && !it.hasActedThisRound }
                    ?: break
                if (s.selectedPlayerSlot != attacker.slotIndex) {
                    gameVm.selectPlayerSlot(attacker.slotIndex)
                }
                val validTarget = s.enemyCards.firstOrNull {
                    it.isAlive && gameVm.canCardTargetSlot(attacker, it.slotIndex, s.enemyCards)
                } ?: break
                gameVm.executePlayerAttack(validTarget.slotIndex)
            }

            assertEquals(BattlePhase.VICTORY, gameVm.battleState.value?.phase)
            val updatedStage101 = gameVm.campaignChapters.value.first().stages.first { it.id == 101 }
            assertEquals(4, updatedStage101.stars)
        } finally {
            prefs.edit().clear().commit()
            GameLocaleState.resetForTest(AppLanguage.RU, chosen = false)
        }
    }

    @Test
    fun `secret hero Elios profile, RU-EN localization, portraits, and exclusions from Arena and Career`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = GameDataRepository(app)
        try {
            val heroes = repo.getInitialHeroCards()
            val elios = heroes.first { it.id == "elios" }

            assertTrue(elios.isSecret)
            assertFalse(elios.isUnlocked)
            assertEquals(SECRET_HERO_UNLOCK_SHARDS, elios.shardsNeeded)
            assertEquals(HeroType.HYBRID, elios.heroType)
            assertEquals(Element.WATER, elios.element)
            assertEquals(AttackPattern.FRONT_50_BACK_25, elios.attackPattern)

            // RU localization
            GameLocaleState.resetForTest(AppLanguage.RU, chosen = true)
            assertEquals("Эллиос", elios.localizedName())
            assertEquals("Маг-альбинос", elios.localizedTitle())
            assertEquals("Секретная", elios.localizedRarityLabel())
            assertEquals("Волна льда (атака по первой и второй линии)", elios.attackPattern.localizedTitle())
            assertEquals("Остужение", elios.availableSkills[0].localizedName())
            assertEquals("Гололед", elios.availableSkills[1].localizedName())

            // EN localization
            GameLocaleState.resetForTest(AppLanguage.EN, chosen = true)
            assertEquals("Ellios", elios.localizedName())
            assertEquals("Albino Mage", elios.localizedTitle())
            assertEquals("Secret", elios.localizedRarityLabel())
            assertEquals("Ice wave (hits front and back row)", elios.attackPattern.localizedTitle())
            assertEquals("Chilling", elios.availableSkills[0].localizedName())
            assertEquals("Black Ice", elios.availableSkills[1].localizedName())

            // 6 portrait tiers & unlocked gallery
            assertEquals(R.drawable.hero_elios_0, resolveHeroImageForLevel("elios", 1, R.drawable.hero_elios))
            assertEquals(R.drawable.hero_elios_0, resolveHeroImageForLevel("elios", 2, R.drawable.hero_elios))
            assertEquals(R.drawable.hero_elios, resolveHeroImageForLevel("elios", 3, R.drawable.hero_elios))
            assertEquals(R.drawable.hero_elios, resolveHeroImageForLevel("elios", 4, R.drawable.hero_elios))
            assertEquals(R.drawable.hero_elios_1, resolveHeroImageForLevel("elios", 5, R.drawable.hero_elios))
            assertEquals(R.drawable.hero_elios_1, resolveHeroImageForLevel("elios", 7, R.drawable.hero_elios))
            assertEquals(R.drawable.hero_elios_2, resolveHeroImageForLevel("elios", 8, R.drawable.hero_elios))
            assertEquals(R.drawable.hero_elios_2, resolveHeroImageForLevel("elios", 9, R.drawable.hero_elios))
            assertEquals(R.drawable.hero_elios_3, resolveHeroImageForLevel("elios", 10, R.drawable.hero_elios))
            assertEquals(R.drawable.hero_elios_3, resolveHeroImageForLevel("elios", 11, R.drawable.hero_elios))
            assertEquals(R.drawable.hero_elios_max, resolveHeroImageForLevel("elios", 12, R.drawable.hero_elios))
            assertEquals(6, getUnlockedHeroGalleryImages("elios", 12, R.drawable.hero_elios_max).size)

            // Never appears in Arena bots or Career enemy stages
            repeat(15) {
                val bots = repo.generateArenaOpponents(playerLevel = 12, playerTrophies = 1500)
                assertTrue("Elios must never appear in Arena bot squads", bots.none { "elios" in it.heroIds })
            }
            val allStages = repo.getCampaignChapters().flatMap { it.stages }
            assertTrue("Elios must never appear in Campaign stages", allStages.none { "elios" in it.enemyHeroIds })
        } finally {
            GameLocaleState.resetForTest(AppLanguage.RU, chosen = false)
        }
    }

    @Test
    fun `secret hero Elios drops only from Rarity Chest anonymously and unlocks via Open action at 50 shards`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = GameDataRepository(app)
        val prefs = app.getSharedPreferences("plump_heroes_prefs", Context.MODE_PRIVATE)
        try {
            val baseHeroes = repo.getInitialHeroCards()
            repo.savePlayerProfile(
                PlayerProfile(
                    name = "SecretTester",
                    level = 12,
                    gold = 500000,
                    gems = 10000,
                    isTutorialCompleted = true
                )
            )
            repo.saveHeroesState(baseHeroes)
            repo.saveDailyQuests(
                listOf(
                    DailyQuestState(
                        id = "q_secret_test",
                        title = "Тест",
                        description = "Тест",
                        type = QuestType.OPEN_CHESTS,
                        difficultyTier = 1,
                        targetCount = 1,
                        currentProgress = 1,
                        rewardType = QuestRewardType.SHARDS,
                        rewardAmount = 5
                    )
                )
            )

            val vm = GameViewModel(app)

            // 1. Basic chests & Daily Quests must NEVER drop locked secret shards or unlock Elios
            repeat(30) {
                vm.openSummonChest(com.example.ui.screens.ChestType.BASIC)
                vm.dismissChestOpening()
            }
            vm.claimDailyQuestReward("q_secret_test")
            val eliosAfterBasics = vm.allHeroes.value.first { it.id == "elios" }
            assertEquals("Basic chests and daily quests must give 0 secret shards while locked", 0, eliosAfterBasics.shards)
            assertFalse(eliosAfterBasics.isUnlocked)

            // 2. Rarity chest at permille >= 150 (85% case when both secrets locked) does NOT drop secret shards
            vm.openRarityChestWithSecretRoll(secretDropRollPermille = 150)
            assertEquals(0, vm.allHeroes.value.first { it.id == "elios" }.shards)
            assertFalse(vm.chestOpeningResult.value?.isSecretMystery ?: true)
            vm.dismissChestOpening()

            // 3. Rarity chest at permille < 75 (7.5% Elios case) drops 1..6 shards anonymously without naming Эллиос / Ellios
            vm.openRarityChestWithSecretRoll(secretDropRollPermille = 40, fixedSecretShards = 6)
            val result = vm.chestOpeningResult.value!!
            assertTrue(result.isSecretMystery)
            assertNull("Mystery drop must not leak portrait resource", result.heroImageRes)
            assertEquals(6, result.shardAmount)
            assertFalse("Chest result must not reveal name Эллиос", result.summaryMessage.contains("Эллиос"))
            assertFalse("Chest result must not reveal name Ellios", result.summaryMessage.contains("Ellios"))
            assertTrue(
                "Drop notification must mention секретный персонаж",
                vm.latestDropMessage.value?.contains("секретный персонаж", ignoreCase = true) == true
            )
            assertEquals(6, vm.allHeroes.value.first { it.id == "elios" }.shards)
            vm.dismissChestOpening()

            // 4. Below 50 shards: inspectHero does not open details and unlockSecretHero is no-op
            vm.unlockSecretHero("elios")
            assertFalse(vm.allHeroes.value.first { it.id == "elios" }.isUnlocked)
            assertNull(vm.secretHeroUnlockedPopup.value)

            // 5. Gather up to 50 shards -> still locked until explicitly opened, then unlockSecretHero opens her + emits popup
            repeat(8) {
                vm.openRarityChestWithSecretRoll(secretDropRollPermille = 20, fixedSecretShards = 6)
                vm.dismissChestOpening()
            }
            val readyHero = vm.allHeroes.value.first { it.id == "elios" }
            assertEquals(54, readyHero.shards)
            assertFalse("Secret hero must stay locked until player clicks Open", readyHero.isUnlocked)

            vm.unlockSecretHero("elios")
            val unlocked = vm.allHeroes.value.first { it.id == "elios" }
            assertTrue(unlocked.isUnlocked)
            assertEquals(4, unlocked.shards)
            assertEquals(10, unlocked.shardsNeeded)
            assertEquals("elios", vm.secretHeroUnlockedPopup.value?.id)

            vm.dismissSecretHeroUnlockedPopup()
            assertNull(vm.secretHeroUnlockedPopup.value)
        } finally {
            prefs.edit().clear().commit()
            GameLocaleState.resetForTest(AppLanguage.RU, chosen = false)
        }
    }

    @Test
    fun `secret hero Elios combat attack splits 100 front and 50 back, skills freeze and weaken, and has no battle quotes`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = GameDataRepository(app)
        val eliosTemplate = repo.getInitialHeroCards().first { it.id == "elios" }
        val skill1 = eliosTemplate.availableSkills[0] // Остужение (Lv 3, 2 energy)
        val skill2 = eliosTemplate.availableSkills[1] // Гололед (Lv 5, 4 energy)

        val eliosPlayer = BattleCard(
            heroId = "elios",
            name = "Эллиос",
            title = "Маг-альбинос",
            heroType = HeroType.HYBRID,
            rarity = CardRarity.MYTHIC,
            element = Element.WATER,
            imageRes = R.drawable.hero_elios_1,
            slotIndex = 2,
            isPlayer = true,
            maxHp = 100,
            currentHp = 100,
            maxArmor = 0,
            currentArmor = 0,
            atk = 20,
            level = 5,
            attackPattern = AttackPattern.FRONT_50_BACK_25,
            skill = skill1
        )
        val enemyFront0 = BattleCard(
            heroId = "e0", name = "F0", title = "", heroType = HeroType.MELEE, rarity = CardRarity.BASIC,
            element = Element.FIRE, imageRes = R.drawable.hero_olga, slotIndex = 0, isPlayer = false,
            maxHp = 100, currentHp = 100, maxArmor = 0, currentArmor = 0, atk = 20
        )
        val enemyFront1 = BattleCard(
            heroId = "e1", name = "F1", title = "", heroType = HeroType.MELEE, rarity = CardRarity.BASIC,
            element = Element.FIRE, imageRes = R.drawable.hero_brunhilde, slotIndex = 1, isPlayer = false,
            maxHp = 100, currentHp = 100, maxArmor = 0, currentArmor = 0, atk = 20
        )
        val enemyBack2 = BattleCard(
            heroId = "e2", name = "B2", title = "", heroType = HeroType.RANGED, rarity = CardRarity.BASIC,
            element = Element.DARK, imageRes = R.drawable.hero_berta, slotIndex = 2, isPlayer = false,
            maxHp = 100, currentHp = 100, maxArmor = 0, currentArmor = 0, atk = 20
        )
        val enemyBack3 = BattleCard(
            heroId = "e3", name = "B3", title = "", heroType = HeroType.RANGED, rarity = CardRarity.BASIC,
            element = Element.DARK, imageRes = R.drawable.hero_witch, slotIndex = 3, isPlayer = false,
            maxHp = 100, currentHp = 100, maxArmor = 0, currentArmor = 0, atk = 20
        )

        // Part 1: Basic attack (50% front = 10 dmg, 25% back = 5 dmg) & zero battle quotes
        val bvm1 = BattleViewModel()
        bvm1.startBattle(
            playerCards = listOf(eliosPlayer.copy()),
            enemyCards = listOf(enemyFront0.copy(), enemyFront1.copy(), enemyBack2.copy(), enemyBack3.copy()),
            enemyName = "Test", enemyTitle = "Test", isArena = false,
            rewardGold = 0, rewardGems = 0, rewardExp = 0,
            onBattleEnd = { _, _ -> }
        )
        assertNull("Elios must never speak start-of-battle quotes", bvm1.battleState.value?.battleStartQuote)
        assertNull("Elios must never speak victory quotes", bvm1.battleState.value?.victoryQuote)
        assertNull(pickHeroBattleQuote(eliosPlayer, QuoteContext.BATTLE_START))
        assertNull(pickHeroBattleQuote(eliosPlayer, QuoteContext.VICTORY))

        bvm1.executePlayerAttack(0)
        val afterBasic = bvm1.battleState.value!!
        val front0Dmg = 100 - afterBasic.enemyCards.first { it.slotIndex == 0 }.currentHp
        val front1Dmg = 100 - afterBasic.enemyCards.first { it.slotIndex == 1 }.currentHp
        val back2Dmg = 100 - afterBasic.enemyCards.first { it.slotIndex == 2 }.currentHp
        val back3Dmg = 100 - afterBasic.enemyCards.first { it.slotIndex == 3 }.currentHp
        assertTrue("Front damage must be 10 (or 15 on 5% crit), got $front0Dmg", front0Dmg == 10 || front0Dmg == 15)
        assertEquals(front0Dmg, front1Dmg)
        assertEquals(front0Dmg / 2, back2Dmg)
        assertEquals(back2Dmg, back3Dmg)

        // Part 2: Skill 1 (Остужение) — deals 50% ATK (10 dmg) and freezes 2 enemies for 1 turn
        val bvm2 = BattleViewModel()
        bvm2.startBattle(
            playerCards = listOf(eliosPlayer.copy()),
            enemyCards = listOf(enemyFront0.copy(), enemyFront1.copy(), enemyBack2.copy()),
            enemyName = "Test", enemyTitle = "Test", isArena = false,
            rewardGold = 0, rewardGems = 0, rewardExp = 0,
            initialPlayerMana = 2,
            onBattleEnd = { _, _ -> }
        )
        bvm2.executePlayerSkill()
        val afterSkill1 = bvm2.battleState.value!!
        val frozenEnemies = afterSkill1.enemyCards.filter { it.isFrozen }
        assertEquals(2, frozenEnemies.size)
        assertTrue(frozenEnemies.all { it.freezeRounds == 1 && it.currentHp == 90 })

        // Part 3: Skill 2 (Гололед) — weakens 2 enemies by 50%
        val bvm3 = BattleViewModel()
        val eliosSkill2Card = eliosPlayer.copy(skill = skill2)
        bvm3.startBattle(
            playerCards = listOf(eliosSkill2Card),
            enemyCards = listOf(enemyFront0.copy(), enemyFront1.copy(), enemyBack2.copy()),
            enemyName = "Test", enemyTitle = "Test", isArena = false,
            rewardGold = 0, rewardGems = 0, rewardExp = 0,
            initialPlayerMana = 4,
            onBattleEnd = { _, _ -> }
        )
        bvm3.executePlayerSkill()
        val afterSkill2 = bvm3.battleState.value!!
        val weakenedEnemies = afterSkill2.enemyCards.filter { it.isWeakened }
        assertEquals(2, weakenedEnemies.size)
        assertTrue(weakenedEnemies.all { it.weakenedRounds >= 1 })
    }

    @Test
    fun `currency conversion exchanges 2500 coins to 100 gems and back with balance guards`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = GameDataRepository(app)
        val prefs = app.getSharedPreferences("plump_heroes_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        try {
            repo.savePlayerProfile(
                PlayerProfile(
                    name = "Exchanger",
                    gold = 3000,
                    gems = 50,
                    isTutorialCompleted = true,
                    isStoryRead = true
                )
            )
            val vm = GameViewModel(app)

            // 3000g -> convert 2500g to 100 gems -> 500g, 150 gems
            vm.convertGoldToGems()
            assertEquals(500, vm.playerProfile.value.gold)
            assertEquals(150, vm.playerProfile.value.gems)

            // Insufficient gold guard: still 500g, 150 gems
            vm.convertGoldToGems()
            assertEquals(500, vm.playerProfile.value.gold)
            assertEquals(150, vm.playerProfile.value.gems)

            // 150 gems -> convert 100 gems to 2500g -> 3000g, 50 gems
            vm.convertGemsToGold()
            assertEquals(3000, vm.playerProfile.value.gold)
            assertEquals(50, vm.playerProfile.value.gems)

            // Insufficient gems guard: still 3000g, 50 gems
            vm.convertGemsToGold()
            assertEquals(3000, vm.playerProfile.value.gold)
            assertEquals(50, vm.playerProfile.value.gems)

            // Persisted on reload
            val reloaded = repo.loadPlayerProfile()
            assertEquals(3000, reloaded.gold)
            assertEquals(50, reloaded.gems)
        } finally {
            prefs.edit().clear().commit()
        }
    }

    @Test
    fun `level up awards proportional coins and gems and shows celebration popup only once per level`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = GameDataRepository(app)
        val prefs = app.getSharedPreferences("plump_heroes_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        try {
            repo.savePlayerProfile(
                PlayerProfile(
                    name = "LevelTester",
                    level = 1,
                    exp = 80,
                    maxExp = 100,
                    gold = 1000,
                    gems = 10,
                    lastCelebratedLevel = 1,
                    isTutorialCompleted = true,
                    isStoryRead = true
                )
            )
            val vm = GameViewModel(app)
            assertEquals(50, vm.calculateLevelUpGoldReward(2))
            assertEquals(10, vm.calculateLevelUpGemsReward(2))
            assertEquals(90, vm.calculateLevelUpGoldReward(3))
            assertEquals(20, vm.calculateLevelUpGemsReward(3))
            assertEquals(130, vm.calculateLevelUpGoldReward(4))
            assertEquals(30, vm.calculateLevelUpGemsReward(4))

            val stage101 = vm.campaignChapters.value.first().stages.first()
            vm.startCampaignBattle(stage101)
            val battle = vm.battleState.value!!
            // Kill all enemies cleanly in 1 hit
            battle.enemyCards.forEach { e ->
                e.currentHp = 1
                e.currentArmor = 0
            }
            // Launch attacks or trigger skill/hit to kill enemies; simulate direct lethal hits per alive enemy
            val playerAttacker = vm.battleState.value!!.playerCards.first()
            battle.enemyCards.drop(1).forEach { e -> e.isAlive = false; e.currentHp = 0 }
            // Ensure player can hit slot 0
            playerAttacker.atk = 999
            vm.executePlayerAttack(0)

            // Wait - verify victory was processed
            assertEquals(BattlePhase.VICTORY, vm.battleState.value?.phase)
            assertEquals(2, vm.playerProfile.value.level)
            // Gold = 1000 + stage reward + Lv2 bonus (50); Gems = 10 + stage gems + Lv2 bonus (10)
            assertEquals(1000 + stage101.rewardGold + 50, vm.playerProfile.value.gold)
            assertEquals(10 + stage101.rewardGems + 10, vm.playerProfile.value.gems)

            val popup = vm.levelUpRewardPopup.value
            assertNotNull("Level-up celebration popup must appear", popup)
            assertEquals(2, popup?.newLevel)
            assertEquals(50, popup?.goldReward)
            assertEquals(10, popup?.gemsReward)

            vm.dismissLevelUpRewardPopup()
            assertNull(vm.levelUpRewardPopup.value)
            assertEquals(2, vm.playerProfile.value.lastCelebratedLevel)
            assertEquals(2, repo.loadPlayerProfile().lastCelebratedLevel)
        } finally {
            prefs.edit().clear().commit()
        }
    }

    @Test
    fun `arena bots scale from deck power with boss challenger and auto refresh on exit or battle finish`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = GameDataRepository(app)
        val prefs = app.getSharedPreferences("plump_heroes_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        try {
            repo.savePlayerProfile(
                PlayerProfile(
                    name = "ArenaHero",
                    level = 4,
                    arenaKeys = 5,
                    activeDeckHeroIds = listOf("brunhilde", "olga", "berta", "witch"),
                    isTutorialCompleted = true,
                    isStoryRead = true
                )
            )
            val vm = GameViewModel(app)
            val (deckAtk, deckHp) = vm.calculatePlayerDeckTotals()
            val bots = vm.arenaBots.value
            assertEquals("Arena must have 3 regular bots + 1 Hard Boss bot", 4, bots.size)

            val regularBots = bots.filter { !it.isHardChallenge }
            val hardBots = bots.filter { it.isHardChallenge }
            assertEquals(3, regularBots.size)
            assertEquals(1, hardBots.size)

            regularBots.forEachIndexed { index, bot ->
                val expectedAtk = kotlin.math.ceil(deckAtk * (1.10 + index * 0.04) * 0.90 * 1.05).toInt()
                val expectedHp = kotlin.math.ceil(deckHp * (1.00 + index * 0.04) * 0.85 * 1.05).toInt()
                assertEquals(
                    "Regular bot #$index ATK (${bot.targetTotalAtk}) must reflect +5% increase",
                    expectedAtk,
                    bot.targetTotalAtk
                )
                assertEquals(
                    "Regular bot #$index HP (${bot.targetTotalHp}) must reflect +5% increase",
                    expectedHp,
                    bot.targetTotalHp
                )
            }

            val hardBot = hardBots.first()
            assertEquals(kotlin.math.ceil(deckAtk * 1.35 * 1.05).toInt(), hardBot.targetTotalAtk)
            assertEquals(kotlin.math.ceil(deckHp * 1.15 * 1.05).toInt(), hardBot.targetTotalHp)
            assertEquals(500, hardBot.rewardGold)
            assertEquals(30, hardBot.rewardGems)
            assertEquals(100, hardBot.rewardExp)

            // Verify starting battle with hardBot passes scaled totals and elevated rewards
            vm.startArenaBattle(hardBot)
            val battle = vm.battleState.value!!
            assertEquals(500, battle.rewardGold)
            assertEquals(30, battle.rewardGems)
            assertEquals(100, battle.rewardExp)
            assertEquals(hardBot.targetTotalAtk, battle.enemyCards.sumOf { it.atk })
            assertEquals(hardBot.targetTotalHp, battle.enemyCards.sumOf { it.maxHp })

            // Exiting Arena battle triggers refreshArenaBots()
            val prevBotNames = vm.arenaBots.value
            vm.exitBattle()
            assertNull(vm.battleState.value)
            assertEquals(4, vm.arenaBots.value.size)
            assertNotSame(prevBotNames, vm.arenaBots.value)
        } finally {
            prefs.edit().clear().commit()
        }
    }

    @Test
    fun `skill animations never target dead cards for player or bot and battle end reports once without duplicate notifications`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val bvm = BattleViewModel()

        val enemyDead0 = BattleCard(
            heroId = "e0", name = "E0", title = "", heroType = HeroType.MELEE, rarity = CardRarity.BASIC,
            element = Element.FIRE, imageRes = R.drawable.hero_olga, slotIndex = 0, isPlayer = false,
            maxHp = 100, currentHp = 0, isAlive = false, maxArmor = 0, currentArmor = 0, atk = 10
        )
        val enemyAlive1 = BattleCard(
            heroId = "e1", name = "E1", title = "", heroType = HeroType.MELEE, rarity = CardRarity.BASIC,
            element = Element.FIRE, imageRes = R.drawable.hero_brunhilde, slotIndex = 1, isPlayer = false,
            maxHp = 100, currentHp = 100, isAlive = true, maxArmor = 0, currentArmor = 0, atk = 10
        )
        val enemyDead2 = BattleCard(
            heroId = "e2", name = "E2", title = "", heroType = HeroType.RANGED, rarity = CardRarity.BASIC,
            element = Element.DARK, imageRes = R.drawable.hero_berta, slotIndex = 2, isPlayer = false,
            maxHp = 100, currentHp = 0, isAlive = false, maxArmor = 0, currentArmor = 0, atk = 10
        )

        // 1. Two-target offensive skill (dem_skill_1 hits front row 0 & 1) with slot 0 dead -> ONLY slot 1 in targetSlots & combatTexts
        val demCard = BattleCard(
            heroId = "demoness", name = "Лиллит", title = "", heroType = HeroType.MELEE, rarity = CardRarity.MYTHIC,
            element = Element.FIRE, imageRes = R.drawable.hero_demoness, slotIndex = 0, isPlayer = true, level = 3,
            maxHp = 120, currentHp = 120, maxArmor = 0, currentArmor = 0, atk = 20,
            skill = HeroSkill("dem_skill_1", "Кара", "", energyCost = 1, unlockLevel = 3)
        )
        val player2 = BattleCard(
            heroId = "iva", name = "Ива", title = "", heroType = HeroType.RANGED, rarity = CardRarity.RARE,
            element = Element.NATURE, imageRes = R.drawable.hero_iva, slotIndex = 2, isPlayer = true, level = 3,
            maxHp = 100, currentHp = 80, maxArmor = 0, currentArmor = 0, atk = 15,
            skill = HeroSkill("iva_skill_1", "Исцеление", "", energyCost = 1, unlockLevel = 3)
        )

        var endCallbacks = 0
        bvm.startBattle(
            playerCards = listOf(demCard.copy(), player2.copy()),
            enemyCards = listOf(enemyDead0.copy(), enemyAlive1.copy(), enemyDead2.copy()),
            enemyName = "Test", enemyTitle = "Test", isArena = false,
            rewardGold = 100, rewardGems = 0, rewardExp = 10,
            initialPlayerMana = 5,
            onBattleEnd = { _, _ -> endCallbacks++ }
        )

        bvm.executePlayerSkill()
        val afterDem = bvm.battleState.value!!
        assertFalse("Dead slot 0 must NOT be in skill targetSlots", afterDem.activeAttackAnimation!!.targetSlots.contains(0))
        assertEquals(listOf(1), afterDem.activeAttackAnimation!!.targetSlots)
        assertTrue(afterDem.combatTexts.none { !it.isPlayer && (it.slotIndex == 0 || it.slotIndex == 2) })

        // 2. Support/heal skill with dead enemy slot 0 must NOT fall back to targetSlots = [0]
        bvm.selectPlayerSlot(2)
        bvm.executePlayerSkill()
        val afterHeal = bvm.battleState.value!!
        assertTrue("Ally heal skill must have empty enemy targetSlots", afterHeal.activeAttackAnimation!!.targetSlots.isEmpty())

        // 3. Killing the final alive enemy triggers callback ONCE
        val bvmVictory = BattleViewModel()
        var victoryCount = 0
        bvmVictory.startBattle(
            playerCards = listOf(demCard.copy(atk = 999)),
            enemyCards = listOf(enemyDead0.copy(), enemyAlive1.copy(currentHp = 5)),
            enemyName = "Boss", enemyTitle = "Boss", isArena = false,
            rewardGold = 100, rewardGems = 0, rewardExp = 10,
            onBattleEnd = { isWin, _ -> if (isWin) victoryCount++ }
        )
        bvmVictory.executePlayerAttack(1)
        bvmVictory.executePlayerAttack(1) // extra call after battle over must be ignored
        assertEquals(1, victoryCount)

        // 4. Verify new quests WIN_TWO_HYBRID & WIN_FOUR_RANGED + bonus_final_1 drawable
        val repo = GameDataRepository(app)
        val catalog = repo.getDailyQuestCatalog()
        assertTrue(catalog.any { it.type == QuestType.WIN_TWO_HYBRID && it.id == "q_t3_two_hybrid_1" })
        assertTrue(catalog.any { it.type == QuestType.WIN_TWO_HYBRID && it.id == "q_t5_two_hybrid_2" })
        assertTrue(catalog.any { it.type == QuestType.WIN_FOUR_RANGED && it.id == "q_t4_four_ranged_1" })
        assertTrue(catalog.none { it.type == QuestType.WIN_ARENA_TWO_MELEE })
        assertNotNull(androidx.core.content.ContextCompat.getDrawable(app, R.drawable.bonus_final_1))
        assertNotNull(androidx.core.content.ContextCompat.getDrawable(app, R.drawable.bonus_final_2))
        assertNotNull(androidx.core.content.ContextCompat.getDrawable(app, R.drawable.bonus_final_3))
        assertNotNull(androidx.core.content.ContextCompat.getDrawable(app, R.drawable.bonus_final_4))
        val finaleRu = com.example.ui.screens.buildBonusGallerySections(
            heroes = repo.getInitialHeroCards(),
            isCampaign100Percent = true,
            isEnglish = false
        ).last { it.isFinale }.items.map { it.imageRes }
        assertEquals(
            listOf(
                R.drawable.bonus_final_1,
                R.drawable.bonus_final_2,
                R.drawable.bonus_final_3,
                R.drawable.bonus_final_4,
                R.drawable.final_6
            ),
            finaleRu
        )
        val finaleEn = com.example.ui.screens.buildBonusGallerySections(
            heroes = repo.getInitialHeroCards(),
            isCampaign100Percent = true,
            isEnglish = true
        ).last { it.isFinale }.items.map { it.imageRes }
        assertEquals(R.drawable.final_6_en, finaleEn.last())
    }

    @Test
    fun `coliseum requires level 5, enforces 2 entries per day, shows tutorial once, and blocks duplicates or melee in back row`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = GameDataRepository(app)
        val prefs = app.getSharedPreferences("plump_heroes_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        try {
            // Level 4 cannot enter Coliseum
            repo.savePlayerProfile(
                PlayerProfile(
                    name = "Novice",
                    level = 4,
                    isTutorialCompleted = true,
                    isStoryRead = true
                )
            )
            val vmLow = GameViewModel(app)
            vmLow.openColiseumScreen(nowMs = 1_000_000L)
            assertFalse("Level 4 player must not open Coliseum", vmLow.isColiseumOpen.value)
            assertTrue(
                vmLow.latestDropMessage.value?.contains("5 уровня") == true ||
                    vmLow.latestDropMessage.value?.contains("Level 5") == true
            )

            // Level 5 can enter Coliseum and sees 2 daily entries + one-time tutorial
            val baseCal = java.util.Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                add(java.util.Calendar.DAY_OF_MONTH, 1)
                set(java.util.Calendar.HOUR_OF_DAY, 10)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val t0 = baseCal.timeInMillis
            repo.savePlayerProfile(
                PlayerProfile(
                    name = "Gladiator",
                    level = 5,
                    maxSeenTimestamp = t0,
                    isTutorialCompleted = true,
                    isStoryRead = true,
                    isColiseumTutorialCompleted = false
                )
            )
            val vm = GameViewModel(app)
            assertEquals(2, vm.getColiseumRemainingEntries(t0))

            vm.openColiseumScreen(nowMs = t0)
            assertTrue("Level 5 player can open Coliseum", vm.isColiseumOpen.value)
            assertTrue("First entry displays Coliseum tutorial once", vm.showColiseumTutorial.value)
            assertEquals(listOf<String?>(null, null, null, null), vm.coliseumDeckHeroIds.value)

            // Exit without completing tutorial explicitly -> second entry NEVER repeats tutorial
            vm.closeColiseumToMenu(nowMs = t0 + 1000L)
            assertFalse(vm.showColiseumTutorial.value)
            vm.openColiseumScreen(nowMs = t0 + 2000L)
            assertFalse("Second entry must NEVER show Coliseum tutorial again", vm.showColiseumTutorial.value)

            // Assign duplicate blocked and Melee in back row blocked
            vm.assignHeroToColiseumSlot(0, "brunhilde")
            vm.assignHeroToColiseumSlot(1, "brunhilde") // duplicate
            assertNull("Duplicate hero must be rejected", vm.coliseumDeckHeroIds.value[1])

            vm.assignHeroToColiseumSlot(2, "olga") // Melee in back row
            assertNull("Melee hero must be rejected in back row", vm.coliseumDeckHeroIds.value[2])

            vm.assignHeroToColiseumSlot(1, "olga")
            vm.assignHeroToColiseumSlot(2, "berta")
            vm.assignHeroToColiseumSlot(3, "witch")
            assertEquals(listOf("brunhilde", "olga", "berta", "witch"), vm.coliseumDeckHeroIds.value)

            // Battle 1 opens directly on Transition Screen without modal overlay; leaving before fighting does NOT consume daily entry
            vm.startColiseumRunAndPreviewFirstBattle()
            assertEquals(1, vm.coliseumBattlePreview.value?.battleNumber)
            assertFalse("Wave 1 must open directly on Transition Screen without popup overlay", vm.isColiseumPreviewVisible.value)
            assertNotNull(vm.coliseumRunState.value)
            vm.claimColiseumPrizeAndExit(nowMs = t0 + 3000L)
            assertNull(vm.coliseumBattlePreview.value)
            assertFalse(vm.isColiseumPreviewVisible.value)
            assertNull(vm.coliseumRunState.value)
            assertEquals(2, vm.getColiseumRemainingEntries(t0 + 3000L))
            assertEquals(0L, vm.getColiseumCooldownRemainingMs(t0 + 3000L))
            vm.openColiseumScreen(nowMs = t0 + 3100L)
            vm.assignHeroToColiseumSlot(0, "brunhilde")
            vm.assignHeroToColiseumSlot(1, "olga")
            vm.assignHeroToColiseumSlot(2, "berta")
            vm.assignHeroToColiseumSlot(3, "witch")

            // Entry #1: Start & confirm battle 1 -> remaining entries drops 2 -> 1
            vm.startColiseumRunAndPreviewFirstBattle()
            vm.confirmStartPreviewedColiseumBattle(nowMs = t0 + 4000L)
            val bState = vm.battleState.value!!
            assertTrue(bState.isColiseum)
            assertEquals(1, bState.coliseumBattleNumber)
            assertTrue("Player always moves first in Coliseum", bState.isPlayerFirstTurn)
            assertFalse("No random Arena coin-toss modal in Coliseum", bState.showArenaFirstTurnModal)
            assertEquals(1, vm.getColiseumRemainingEntries(t0 + 5000L))
            assertEquals(0L, vm.getColiseumCooldownRemainingMs(t0 + 5000L))

            // Claim/exit run 1 and verify player can still enter a 2nd time today
            vm.claimColiseumPrizeAndExit(nowMs = t0 + 6000L)
            vm.openColiseumScreen(nowMs = t0 + 7000L)
            assertTrue("Player can enter Coliseum a 2nd time in the same day", vm.isColiseumOpen.value)
            vm.assignHeroToColiseumSlot(0, "brunhilde")
            vm.assignHeroToColiseumSlot(1, "olga")
            vm.assignHeroToColiseumSlot(2, "berta")
            vm.assignHeroToColiseumSlot(3, "witch")
            vm.startColiseumRunAndPreviewFirstBattle()
            vm.confirmStartPreviewedColiseumBattle(nowMs = t0 + 8000L)
            vm.claimColiseumPrizeAndExit(nowMs = t0 + 9000L)

            // Now 0/2 remain today -> 3rd entry blocked with midnight reset timer
            assertEquals(0, vm.getColiseumRemainingEntries(t0 + 10_000L))
            assertTrue(vm.getColiseumCooldownRemainingMs(t0 + 10_000L) > 0L)
            vm.openColiseumScreen(nowMs = t0 + 10_000L)
            assertFalse("3rd Coliseum entry on same day must be blocked", vm.isColiseumOpen.value)

            // Next calendar day resets back to 2 entries
            val nextDayCal = (baseCal.clone() as java.util.Calendar).apply {
                add(java.util.Calendar.DAY_OF_MONTH, 1)
            }
            val tNextDay = nextDayCal.timeInMillis
            assertEquals(2, vm.getColiseumRemainingEntries(tNextDay))
            assertEquals(0L, vm.getColiseumCooldownRemainingMs(tNextDay))
        } finally {
            prefs.edit().clear().commit()
        }
    }

    @Test
    fun `coliseum scaling, currency rewards, preview dismiss intermission, milestone heal revive, and defeat consolation`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = GameDataRepository(app)
        val prefs = app.getSharedPreferences("plump_heroes_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        try {
            // Verify updated repository scaling & currency reward formulas
            assertEquals(0.30, repo.calculateColiseumScalingMultiplier(1), 0.001)
            assertEquals(0.45, repo.calculateColiseumScalingMultiplier(2), 0.001)
            assertEquals(0.70, repo.calculateColiseumScalingMultiplier(3), 0.001)
            assertEquals(0.85, repo.calculateColiseumScalingMultiplier(4), 0.001)
            assertEquals(1.00, repo.calculateColiseumScalingMultiplier(5), 0.001)
            assertEquals(1.15, repo.calculateColiseumScalingMultiplier(6), 0.001)
            assertEquals(1.30, repo.calculateColiseumScalingMultiplier(7), 0.001)

            assertEquals(100 to 10, repo.calculateColiseumRewards(1))
            assertEquals(250 to 20, repo.calculateColiseumRewards(2))
            assertEquals(450 to 35, repo.calculateColiseumRewards(3))
            assertEquals(700 to 55, repo.calculateColiseumRewards(4))
            assertEquals(1000 to 80, repo.calculateColiseumRewards(5))
            assertEquals(1350 to 100, repo.calculateColiseumRewards(6))
            assertEquals(1750 to 120, repo.calculateColiseumRewards(7))

            val t0 = System.currentTimeMillis()
            repo.savePlayerProfile(
                PlayerProfile(
                    name = "Champion",
                    level = 6,
                    gold = 1000,
                    gems = 50,
                    exp = 20,
                    maxSeenTimestamp = t0,
                    isTutorialCompleted = true,
                    isStoryRead = true,
                    isColiseumTutorialCompleted = true
                )
            )
            val vm = GameViewModel(app)
            vm.openColiseumScreen(nowMs = t0)
            vm.assignHeroToColiseumSlot(0, "brunhilde")
            vm.assignHeroToColiseumSlot(1, "olga")
            vm.assignHeroToColiseumSlot(2, "berta")
            vm.assignHeroToColiseumSlot(3, "witch")

            fun winCurrentWave(damageSlot0To: Int? = null, killSlot1: Boolean = false) {
                val st = vm.battleState.value!!
                if (damageSlot0To != null) {
                    st.playerCards.first { it.slotIndex == 0 }.currentHp = damageSlot0To
                }
                if (killSlot1) {
                    st.playerCards.first { it.slotIndex == 1 }.apply {
                        currentHp = 0
                        isAlive = false
                    }
                }
                st.enemyCards.drop(1).forEach { e ->
                    e.currentHp = 0
                    e.isAlive = false
                }
                val aliveEnemy = st.enemyCards.first()
                aliveEnemy.currentArmor = 0
                aliveEnemy.currentHp = 1
                val attacker = st.playerCards.first { it.isAlive }
                attacker.atk = 999
                vm.selectPlayerSlot(attacker.slotIndex)
                vm.executePlayerAttack(aliveEnemy.slotIndex)
                assertEquals(BattlePhase.VICTORY, vm.battleState.value?.phase)
            }

            // Battle 1: take damage on slot 0 and lose slot 1 -> awards 100 gold & 10 gems
            vm.startColiseumRunAndPreviewFirstBattle()
            vm.confirmStartPreviewedColiseumBattle(nowMs = t0)
            val maxHp0 = vm.battleState.value!!.playerCards.first { it.slotIndex == 0 }.maxHp
            winCurrentWave(damageSlot0To = maxHp0 - 15, killSlot1 = true)
            assertEquals(100, vm.coliseumRunState.value?.accumulatedGold)
            assertEquals(10, vm.coliseumRunState.value?.accumulatedGems)
            assertEquals("No EXP awarded in Coliseum", 20, vm.playerProfile.value.exp)
            assertEquals(1, vm.playerProfile.value.coliseumWon)

            // Continue to Battle 2: unified Transition Screen opens directly (no blocking preview modal popup)
            vm.continueColiseumAfterVictory()
            assertEquals(2, vm.coliseumBattlePreview.value?.battleNumber)
            assertFalse("Battle 2 opens directly on unified Transition Screen", vm.isColiseumPreviewVisible.value)
            assertNotNull("Next battle info available on Transition Screen", vm.coliseumBattlePreview.value)
            val intermissionCards = vm.coliseumRunState.value!!.playerCards
            assertEquals(maxHp0 - 15, intermissionCards.first { it.slotIndex == 0 }.currentHp)
            assertFalse(intermissionCards.first { it.slotIndex == 1 }.isAlive)

            vm.confirmStartPreviewedColiseumBattle(nowMs = t0 + 1000L)
            val b2Cards = vm.battleState.value!!.playerCards
            assertEquals(maxHp0 - 15, b2Cards.first { it.slotIndex == 0 }.currentHp)
            assertFalse("Fallen hero remains dead into next wave", b2Cards.first { it.slotIndex == 1 }.isAlive)
            winCurrentWave()
            assertEquals(100 + 250, vm.coliseumRunState.value?.accumulatedGold)
            assertEquals(10 + 20, vm.coliseumRunState.value?.accumulatedGems)

            // Battle 3 -> Triggers Milestone Bonus Offer (20% heal / 35% revive)
            vm.continueColiseumAfterVictory()
            vm.confirmStartPreviewedColiseumBattle(nowMs = t0 + 2000L)
            winCurrentWave()
            assertEquals(100 + 250 + 450, vm.coliseumRunState.value?.accumulatedGold)
            assertEquals(10 + 20 + 35, vm.coliseumRunState.value?.accumulatedGems)
            assertEquals(3, vm.playerProfile.value.coliseumWon)

            vm.continueColiseumAfterVictory()
            val offer = vm.coliseumMilestoneBonusOffer.value!!
            assertEquals(3, offer.completedBattleNumber)
            assertEquals(20, offer.healPercent)
            assertEquals(35, offer.revivePercent)
            assertEquals(1, offer.deadCards.size)

            // Revive fallen slot 1 card -> enters battle 4 Transition Screen with revived HP = ceil(maxHp * 0.35)
            val slot1MaxHp = vm.coliseumRunState.value!!.playerCards.first { it.slotIndex == 1 }.maxHp
            vm.applyColiseumMilestoneRevive(1)
            assertNull(vm.coliseumMilestoneBonusOffer.value)
            assertEquals(4, vm.coliseumBattlePreview.value?.battleNumber)
            assertFalse("Battle 4 after milestone opens directly on Transition Screen", vm.isColiseumPreviewVisible.value)
            val revivedSlot1 = vm.coliseumRunState.value!!.playerCards.first { it.slotIndex == 1 }
            assertTrue(revivedSlot1.isAlive)
            assertEquals(kotlin.math.ceil(slot1MaxHp * 0.35).toInt(), revivedSlot1.currentHp)

            // Leave Coliseum from Transition Screen via Leave button / claimColiseumPrizeAndExit
            vm.claimColiseumPrizeAndExit(nowMs = t0 + 3000L)
            assertFalse(vm.isColiseumOpen.value)
            assertEquals(1000 + 800, vm.playerProfile.value.gold)
            assertEquals(50 + 65, vm.playerProfile.value.gems)

            // Entry #2 on the same day -> defeat consolation (20% of 100g, 10 gems = 20g, 2 gems) & W/L JSON persistence
            val t2 = t0 + 10_000L
            vm.openColiseumScreen(nowMs = t2)
            assertTrue("2nd Coliseum entry in the same day must be allowed", vm.isColiseumOpen.value)
            vm.assignHeroToColiseumSlot(0, "brunhilde")
            vm.assignHeroToColiseumSlot(1, "olga")
            vm.assignHeroToColiseumSlot(2, "berta")
            vm.assignHeroToColiseumSlot(3, "witch")
            vm.startColiseumRunAndPreviewFirstBattle()
            vm.confirmStartPreviewedColiseumBattle(nowMs = t2)
            // Surrender battle 1 (potential = 100 gold, 10 gems -> 20% = 20 gold, 2 gems)
            vm.surrenderBattle()
            assertEquals(1000 + 800 + 20, vm.playerProfile.value.gold)
            assertEquals(50 + 65 + 2, vm.playerProfile.value.gems)
            assertEquals(3, vm.playerProfile.value.coliseumWon)
            assertEquals(1, vm.playerProfile.value.coliseumLost)
            assertEquals(0, vm.getColiseumRemainingEntries(t2 + 1000L))

            // Persistence across reload and JSON export/import
            val reloaded = GameViewModel(app)
            assertEquals(3, reloaded.playerProfile.value.coliseumWon)
            assertEquals(1, reloaded.playerProfile.value.coliseumLost)
            assertEquals(0, reloaded.getColiseumRemainingEntries(t2 + 1000L))
            val json = reloaded.exportSaveToJson()
            assertTrue(reloaded.importSaveFromJson(json))
            assertEquals(3, reloaded.playerProfile.value.coliseumWon)
            assertEquals(1, reloaded.playerProfile.value.coliseumLost)
            assertEquals(0, reloaded.getColiseumRemainingEntries(t2 + 1000L))
        } finally {
            prefs.edit().clear().commit()
        }
    }

    @Test
    fun `first launch keeps language prompt open on relaunch until user explicitly picks RU or EN`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val prefs = app.getSharedPreferences("plump_heroes_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        GameLocaleState.resetForTest(AppLanguage.RU, chosen = false)

        try {
            // GameViewModel init runs checkDailyLoginBonus() -> saves maxSeenTimestamp & profile_name to prefs
            GameViewModel(app)
            assertFalse(
                "Background profile save on fresh launch must NOT skip first-launch language dialog",
                GameLocaleState.hasChosenLanguage
            )

            // Re-initializing GameLocaleState (simulating app restart before user clicked RU or EN) keeps it unchosen
            GameLocaleState.resetForTest(AppLanguage.RU, chosen = false)
            GameLocaleState.init(app)
            assertFalse(
                "Re-launch before language choice must still require language selection",
                GameLocaleState.hasChosenLanguage
            )

            // Explicit user selection persists choice
            GameLocaleState.setLanguage(app, AppLanguage.EN)
            assertTrue(GameLocaleState.hasChosenLanguage)
            assertEquals(AppLanguage.EN, GameLocaleState.language)

            GameLocaleState.resetForTest(AppLanguage.RU, chosen = false)
            GameLocaleState.init(app)
            assertTrue(GameLocaleState.hasChosenLanguage)
            assertEquals(AppLanguage.EN, GameLocaleState.language)
        } finally {
            prefs.edit().clear().commit()
            GameLocaleState.resetForTest(AppLanguage.RU, chosen = false)
        }
    }

    @Test
    fun `max-level heroines never receive shards or emit max-level text, and secret card tap omits drop rate`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = GameDataRepository(app)
        val prefs = app.getSharedPreferences("plump_heroes_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        try {
            // 1. Secret card locked message in RosterScreen & GameViewModel omits any drop probability
            GameLocaleState.resetForTest(AppLanguage.RU, chosen = true)
            val ruRosterMsg = com.example.ui.screens.formatSecretHeroLockedMessage(32)
            assertFalse("RU secret message must not show % probability: $ruRosterMsg", ruRosterMsg.contains("%"))
            assertFalse("RU secret message must not mention шанс: $ruRosterMsg", ruRosterMsg.contains("шанс", ignoreCase = true))

            GameLocaleState.resetForTest(AppLanguage.EN, chosen = true)
            val enRosterMsg = com.example.ui.screens.formatSecretHeroLockedMessage(32)
            assertFalse("EN secret message must not show % probability: $enRosterMsg", enRosterMsg.contains("%"))
            assertFalse("EN secret message must not mention drop rate: $enRosterMsg", enRosterMsg.contains("drop rate", ignoreCase = true))
            GameLocaleState.resetForTest(AppLanguage.RU, chosen = true)

            // 2. All heroines fully upgraded to Lv 12 -> 0 shards from Basic, Rarity, Wheel, Quests, and NO max-level messages
            val allMaxed = repo.getInitialHeroCards().map { hero ->
                hero.copy(level = hero.rarity.levelCap, shards = 0, isUnlocked = true)
            }
            val day7Cal = java.util.Calendar.getInstance().apply {
                set(2030, java.util.Calendar.MARCH, 16, 10, 0, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val day8Cal = java.util.Calendar.getInstance().apply {
                set(2030, java.util.Calendar.MARCH, 17, 10, 0, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val day7Time = day7Cal.timeInMillis
            val day8Time = day8Cal.timeInMillis

            repo.savePlayerProfile(
                PlayerProfile(
                    name = "MaxTester",
                    level = 12,
                    gold = 50000,
                    gems = 5000,
                    isTutorialCompleted = true,
                    isStoryRead = true,
                    dailyStreak = 7,
                    lastDailyClaimDate = "2030-03-16",
                    lastDailyClaimTimestamp = day7Time,
                    maxSeenTimestamp = day7Time
                )
            )
            val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(System.currentTimeMillis()))
            repo.saveHeroesState(allMaxed)
            repo.saveDailyQuestDate(todayStr)
            repo.saveDailyQuests(
                listOf(
                    DailyQuestState("q_shards_max_test", "Тест", "Тест", QuestType.OPEN_CHESTS, 1, 1, 1, QuestRewardType.SHARDS, 5),
                    DailyQuestState("q_t2_any_2", "Двойной триумф", "Выиграй 2 боя", QuestType.WIN_ANY_BATTLES, 2, 2, 0, QuestRewardType.GOLD, 230),
                    DailyQuestState("q_t3_arena_2", "Боец Арены", "Выиграй 2 боя на Арене", QuestType.WIN_ARENA_BATTLES, 3, 2, 0, QuestRewardType.GOLD, 360),
                    DailyQuestState("q_t4_any_4", "Серия побед", "Выиграй 4 боя", QuestType.WIN_ANY_BATTLES, 4, 4, 0, QuestRewardType.GOLD, 540),
                    DailyQuestState("q_t5_enemies_18", "Буря клинков", "Уничтожь 18 врагов", QuestType.DEFEAT_ENEMIES, 5, 18, 0, QuestRewardType.GOLD, 800)
                )
            )

            val vm = GameViewModel(app)

            // Basic chest when all maxed: 0 shards, artifact/gold fallback, NO max-level text
            vm.openSummonChest(com.example.ui.screens.ChestType.BASIC)
            val basicRes = vm.chestOpeningResult.value!!
            assertEquals(0, basicRes.shardAmount)
            assertNull(basicRes.heroId)
            assertFalse("Basic chest must not mention макс: ${basicRes.summaryMessage}", basicRes.summaryMessage.contains("макс", ignoreCase = true))
            assertFalse("Basic chest must not mention max Lv: ${basicRes.summaryMessage}", basicRes.summaryMessage.contains("max Lv", ignoreCase = true))
            assertTrue("Basic chest must award artifact or gold fallback", basicRes.dropArtifact != null || vm.playerProfile.value.gold > 49500)
            vm.dismissChestOpening()

            // Rarity chest when all maxed (including Elios Lv 12): 0 shards, no secret proc, no max-level text
            vm.openRarityChestWithSecretRoll(secretDropRoll = 0)
            val rarityRes = vm.chestOpeningResult.value!!
            assertEquals(0, rarityRes.shardAmount)
            assertNull(rarityRes.heroId)
            assertFalse(rarityRes.isSecretMystery)
            assertFalse("Rarity chest must not mention макс: ${rarityRes.summaryMessage}", rarityRes.summaryMessage.contains("макс", ignoreCase = true))
            assertFalse("Rarity chest must not mention max Lv: ${rarityRes.summaryMessage}", rarityRes.summaryMessage.contains("max Lv", ignoreCase = true))
            vm.dismissChestOpening()

            // Daily quest shard reward fallback: gives coins cleanly without max-level text
            val goldBeforeQuest = vm.playerProfile.value.gold
            vm.claimDailyQuestReward("q_shards_max_test")
            assertEquals(goldBeforeQuest + 200, vm.playerProfile.value.gold)
            val questMsg = vm.latestDropMessage.value.orEmpty()
            assertFalse("Quest message must not mention 12 ур: $questMsg", questMsg.contains("12 ур", ignoreCase = true))
            assertFalse("Quest message must not mention Lv. 12: $questMsg", questMsg.contains("Lv. 12", ignoreCase = true))

            // Fortune Wheel shard sector fallback: gives coins cleanly without max-level text
            vm.checkDailyLoginBonus(nowMs = day8Time)
            val shardSector = vm.fortuneWheelOffer.value!!.sectors.first { it.type == FortuneWheelSectorType.SHARDS_10 }
            val goldBeforeWheel = vm.playerProfile.value.gold
            vm.claimFortuneWheelReward(shardSector, nowMs = day8Time)
            assertEquals(goldBeforeWheel + 500, vm.playerProfile.value.gold)
            val wheelDesc = vm.fortuneWheelResult.value?.description.orEmpty()
            assertFalse("Fortune Wheel must not mention 12 ур: $wheelDesc", wheelDesc.contains("12 ур", ignoreCase = true))
            assertFalse("Fortune Wheel must not mention Lv. 12: $wheelDesc", wheelDesc.contains("Lv. 12", ignoreCase = true))

            // Verify every maxed hero still has exactly 0 shards
            assertTrue("All maxed heroes must still have 0 shards", vm.allHeroes.value.all { it.shards == 0 })

            // New day quests must not schedule SHARDS reward quests when all heroines are Lv 12
            val shardTemplate = repo.getDailyQuestCatalog().first { it.rewardType == QuestRewardType.SHARDS }
            assertFalse(
                "Shard quests must be infeasible once all heroines are Lv 12",
                vm.isQuestTemplateFeasible(shardTemplate)
            )
        } finally {
            prefs.edit().clear().commit()
            GameLocaleState.resetForTest(AppLanguage.RU, chosen = false)
        }
    }

    @Test
    fun `coliseum mana vial milestone bonus increases starting mana only in coliseum`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = GameDataRepository(app)
        val prefs = app.getSharedPreferences("plump_heroes_prefs", Context.MODE_PRIVATE)
        fun calMillis(year: Int, monthZeroBased: Int, day: Int, hour: Int): Long {
            return java.util.Calendar.getInstance().apply {
                set(year, monthZeroBased, day, hour, 0, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
        try {
            prefs.edit().clear().commit()
            val t0 = calMillis(2030, java.util.Calendar.MAY, 15, 11)
            val heroes = repo.getInitialHeroCards().map {
                if (it.id in listOf("brunhilde", "olga", "berta", "witch")) {
                    it.copy(isUnlocked = true, level = 8, baseAtk = 60, baseHp = 400, baseArmor = 20)
                } else it
            }
            repo.saveHeroesState(heroes)
            repo.savePlayerProfile(
                PlayerProfile(
                    name = "ManaTester",
                    level = 8,
                    gold = 1000,
                    gems = 100,
                    isTutorialCompleted = true,
                    isColiseumTutorialCompleted = true,
                    activeDeckHeroIds = listOf("brunhilde", "olga", "berta", "witch")
                )
            )
            val vm = GameViewModel(app)
            vm.openColiseumScreen(nowMs = t0)
            vm.assignHeroToColiseumSlot(0, "brunhilde")
            vm.assignHeroToColiseumSlot(1, "olga")
            vm.assignHeroToColiseumSlot(2, "berta")
            vm.assignHeroToColiseumSlot(3, "witch")

            fun winCurrentBattleAndAdvanceColiseum(advanceColiseum: Boolean = true) {
                val st = vm.battleState.value!!
                st.enemyCards.drop(1).forEach { e ->
                    e.currentHp = 0
                    e.isAlive = false
                }
                val aliveEnemy = st.enemyCards.first()
                aliveEnemy.currentArmor = 0
                aliveEnemy.currentHp = 1
                val attacker = st.playerCards.first { it.isAlive }
                attacker.atk = 999
                vm.selectPlayerSlot(attacker.slotIndex)
                vm.executePlayerAttack(aliveEnemy.slotIndex)
                assertEquals(BattlePhase.VICTORY, vm.battleState.value?.phase)
                if (advanceColiseum) {
                    vm.continueColiseumAfterVictory()
                }
            }

            // Battle 1 starts with 1 Mana
            vm.startColiseumRunAndPreviewFirstBattle()
            vm.confirmStartPreviewedColiseumBattle(nowMs = t0)
            assertEquals(1, vm.battleState.value?.playerTeamMana)

            // Win Battles 1, 2, 3 -> Milestone offer opens with currentBonusStartMana = 0
            winCurrentBattleAndAdvanceColiseum()
            vm.confirmStartPreviewedColiseumBattle(nowMs = t0 + 1000L)
            winCurrentBattleAndAdvanceColiseum()
            vm.confirmStartPreviewedColiseumBattle(nowMs = t0 + 2000L)
            winCurrentBattleAndAdvanceColiseum()

            val offer3 = vm.coliseumMilestoneBonusOffer.value
            assertNotNull("Milestone offer must appear at Battle 3", offer3)
            assertEquals(0, offer3!!.currentBonusStartMana)

            // Pick Mana Vial (+1 starting mana -> 2 mana in Coliseum Battle 4)
            vm.applyColiseumMilestoneMana()
            assertNull(vm.coliseumMilestoneBonusOffer.value)
            assertEquals(1, vm.coliseumRunState.value?.bonusStartMana)

            vm.confirmStartPreviewedColiseumBattle(nowMs = t0 + 3000L)
            assertEquals(2, vm.battleState.value?.playerTeamMana)

            // Win Battles 4, 5, 6 -> Milestone offer opens again and Mana Vial stacks to 3
            winCurrentBattleAndAdvanceColiseum()
            vm.confirmStartPreviewedColiseumBattle(nowMs = t0 + 4000L)
            winCurrentBattleAndAdvanceColiseum()
            vm.confirmStartPreviewedColiseumBattle(nowMs = t0 + 5000L)
            winCurrentBattleAndAdvanceColiseum()

            val offer6 = vm.coliseumMilestoneBonusOffer.value
            assertNotNull("Milestone offer must appear at Battle 6", offer6)
            assertEquals(1, offer6!!.currentBonusStartMana)
            vm.applyColiseumMilestoneMana()
            assertEquals(2, vm.coliseumRunState.value?.bonusStartMana)

            vm.confirmStartPreviewedColiseumBattle(nowMs = t0 + 6000L)
            assertEquals(3, vm.battleState.value?.playerTeamMana)

            // Exit Coliseum and start a normal Career battle -> starting mana MUST remain 1
            vm.claimColiseumPrizeAndExit(nowMs = t0 + 7000L)
            val stage101 = vm.campaignChapters.value.flatMap { it.stages }.first { it.id == 101 }
            vm.startCampaignBattle(stage101)
            assertEquals("Starting Mana bonus applies ONLY in Coliseum", 1, vm.battleState.value?.playerTeamMana)
        } finally {
            prefs.edit().clear().commit()
            GameLocaleState.resetForTest(AppLanguage.RU, chosen = false)
        }
    }

    @Test
    fun `artifact rarity stats are strictly monotonic and forge preserves two-of-a-kind slot with varied named catalog items`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = GameDataRepository(app)
        val prefs = app.getSharedPreferences("plump_heroes_prefs", Context.MODE_PRIVATE)
        try {
            prefs.edit().clear().commit()
            val catalog = repo.getArtifactCatalog().values.toList()
            val basics = catalog.filter { it.rarity == CardRarity.BASIC }
            val specials = catalog.filter { it.rarity == CardRarity.SPECIAL }
            val rares = catalog.filter { it.rarity == CardRarity.RARE }
            val mythics = catalog.filter { it.rarity == CardRarity.MYTHIC }

            // At least 20 named Mythic artifacts, none referencing Elios
            assertTrue("Should have >= 6 Mythic artifacts", mythics.size >= 6)
            assertEquals(20, mythics.size)
            assertTrue(
                "No artifact may reference Elios",
                catalog.none { it.name.contains("Эллиос", ignoreCase = true) || it.localizedName().contains("Ellios", ignoreCase = true) }
            )

            // Strict monotonic stat bounds across rarities: max(Basic) < min(Special), max(Special) < min(Rare), max(Rare) < min(Mythic)
            assertTrue("Special min ATK > Basic max ATK", specials.minOf { it.atkBonus } > basics.maxOf { it.atkBonus })
            assertTrue("Special min ARMOR > Basic max ARMOR", specials.minOf { it.armorBonus } > basics.maxOf { it.armorBonus })
            assertTrue("Special min HP > Basic max HP", specials.minOf { it.hpBonus } > basics.maxOf { it.hpBonus })

            assertTrue("Rare min ATK > Special max ATK", rares.minOf { it.atkBonus } > specials.maxOf { it.atkBonus })
            assertTrue("Rare min ARMOR > Special max ARMOR", rares.minOf { it.armorBonus } > specials.maxOf { it.armorBonus })
            assertTrue("Rare min HP > Special max HP", rares.minOf { it.hpBonus } > specials.maxOf { it.hpBonus })

            assertTrue("Mythic min ATK > Rare max ATK", mythics.minOf { it.atkBonus } > rares.maxOf { it.atkBonus })
            assertTrue("Mythic min ARMOR > Rare max ARMOR", mythics.minOf { it.armorBonus } > rares.maxOf { it.armorBonus })
            assertTrue("Mythic min HP > Rare max HP", mythics.minOf { it.hpBonus } > rares.maxOf { it.hpBonus })

            // Forge 2 WEAPON + 1 ARMOR (Rare -> Mythic) must produce a Mythic WEAPON named artifact
            val rareW1 = rares.first { it.slot == ArtifactSlot.WEAPON }.copy(id = "test_rw1")
            val rareW2 = rares.first { it.slot == ArtifactSlot.WEAPON }.copy(id = "test_rw2")
            val rareA1 = rares.first { it.slot == ArtifactSlot.ARMOR }.copy(id = "test_ra1")
            // Forge 2 ARMOR + 1 RELIC (Special -> Rare) must produce a Rare ARMOR
            val specA1 = specials.first { it.slot == ArtifactSlot.ARMOR }.copy(id = "test_sa1")
            val specA2 = specials.first { it.slot == ArtifactSlot.ARMOR }.copy(id = "test_sa2")
            val specR1 = specials.first { it.slot == ArtifactSlot.RELIC }.copy(id = "test_sr1")
            // Forge 2 RELIC + 1 WEAPON (Rare -> Mythic) must produce a Mythic RELIC
            val rareR1 = rares.first { it.slot == ArtifactSlot.RELIC }.copy(id = "test_rr1")
            val rareR2 = rares.first { it.slot == ArtifactSlot.RELIC }.copy(id = "test_rr2")
            val rareW3 = rares.first { it.slot == ArtifactSlot.WEAPON }.copy(id = "test_rw3")

            repo.savePlayerProfile(PlayerProfile(name = "ForgeTester", gold = 10000, isTutorialCompleted = true))
            repo.saveArtifacts(listOf(rareW1, rareW2, rareA1, specA1, specA2, specR1, rareR1, rareR2, rareW3))

            val vm = GameViewModel(app)
            vm.toggleForgeArtifactSelection("test_rw1")
            vm.toggleForgeArtifactSelection("test_rw2")
            vm.toggleForgeArtifactSelection("test_ra1")
            vm.combineArtifacts()
            val forgedMythicWeapon = vm.inventoryArtifacts.value.first { it.id.startsWith("forged_") }
            assertEquals(CardRarity.MYTHIC, forgedMythicWeapon.rarity)
            assertEquals(ArtifactSlot.WEAPON, forgedMythicWeapon.slot)
            assertTrue(mythics.any { it.name == forgedMythicWeapon.name })

            vm.toggleForgeArtifactSelection("test_sa1")
            vm.toggleForgeArtifactSelection("test_sa2")
            vm.toggleForgeArtifactSelection("test_sr1")
            vm.combineArtifacts()
            val forgedRareArmor = vm.inventoryArtifacts.value.first { it.rarity == CardRarity.RARE && it.id.startsWith("forged_") }
            assertEquals(ArtifactSlot.ARMOR, forgedRareArmor.slot)

            vm.toggleForgeArtifactSelection("test_rr1")
            vm.toggleForgeArtifactSelection("test_rr2")
            vm.toggleForgeArtifactSelection("test_rw3")
            vm.combineArtifacts()
            val forgedMythicRelic = vm.inventoryArtifacts.value.first { it.rarity == CardRarity.MYTHIC && it.slot == ArtifactSlot.RELIC }
            assertTrue(mythics.any { it.name == forgedMythicRelic.name })
        } finally {
            prefs.edit().clear().commit()
            GameLocaleState.resetForTest(AppLanguage.RU, chosen = false)
        }
    }

    @Test
    fun `unlocked secret hero Elios stops using 15 percent mystery roll and drops as Rare in chests wheel and career`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = GameDataRepository(app)
        val prefs = app.getSharedPreferences("plump_heroes_prefs", Context.MODE_PRIVATE)
        fun calMillis(year: Int, monthZeroBased: Int, day: Int, hour: Int): Long {
            return java.util.Calendar.getInstance().apply {
                set(year, monthZeroBased, day, hour, 0, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
        try {
            prefs.edit().clear().commit()
            val day7Time = calMillis(2030, java.util.Calendar.JUNE, 10, 12)
            val day8Time = calMillis(2030, java.util.Calendar.JUNE, 11, 12)

            // Max out all non-secret heroes so Elios (unlocked, Lv 1) is the sole shard-eligible hero
            // -> isolates Basic Chest, Rarity Chest, Fortune Wheel, and Career stage 201 (Rare) drops cleanly
            val heroesSetup = repo.getInitialHeroCards().map {
                if (it.id == "elios") {
                    it.copy(isUnlocked = true, level = 1, shards = 0, shardsNeeded = 10)
                } else {
                    it.copy(isUnlocked = true, level = it.rarity.levelCap, shards = 0)
                }
            }
            repo.saveHeroesState(heroesSetup)
            repo.savePlayerProfile(
                PlayerProfile(
                    name = "UnlockedSecretTester",
                    level = 12,
                    gold = 50000,
                    gems = 5000,
                    isTutorialCompleted = true,
                    dailyStreak = 7,
                    lastDailyClaimDate = "2030-06-10",
                    lastDailyClaimTimestamp = day7Time,
                    maxSeenTimestamp = day7Time,
                    activeDeckHeroIds = listOf("brunhilde", "olga", "berta", "witch")
                )
            )

            val vm = GameViewModel(app)

            // 1. Unlocked Elios in Rarity Chest at roll = 0 must NOT trigger mystery drop (uses normal scaled 35..45 shard drop since >50% heroes are Lv>9, not 1..6 secret)
            vm.openRarityChestWithSecretRoll(secretDropRoll = 0, fixedSecretShards = 6)
            val rarityRes = vm.chestOpeningResult.value!!
            assertFalse("Unlocked secret hero must NOT trigger mystery flag", rarityRes.isSecretMystery)
            assertEquals("elios", rarityRes.heroId)
            assertTrue("Normal Rarity Chest with >50% Lv>9 heroes yields 35..45 shards", rarityRes.shardAmount in 35..45)
            vm.dismissChestOpening()

            // 2. Unlocked Elios drops in Basic Chest as Rare (15..20 shards since >50% heroes are Lv>9 and effective rarity is RARE)
            val shardsBeforeBasic = vm.allHeroes.value.first { it.id == "elios" }.shards
            vm.openSummonChest(com.example.ui.screens.ChestType.BASIC)
            val basicRes = vm.chestOpeningResult.value!!
            assertEquals("elios", basicRes.heroId)
            assertTrue(basicRes.shardAmount in 15..20)
            assertEquals(shardsBeforeBasic + basicRes.shardAmount, vm.allHeroes.value.first { it.id == "elios" }.shards)
            vm.dismissChestOpening()

            // 3. Unlocked Elios can receive Fortune Wheel SHARDS_5 & reverts sector 1 to SHARDS_5
            vm.checkDailyLoginBonus(nowMs = day8Time)
            val wheelOffer = vm.fortuneWheelOffer.value!!
            assertFalse(wheelOffer.sectors.any { it.type == FortuneWheelSectorType.SECRET_SHARDS_3 })
            val shards5 = wheelOffer.sectors.first { it.type == FortuneWheelSectorType.SHARDS_5 }
            val shardsBeforeWheel = vm.allHeroes.value.first { it.id == "elios" }.shards
            vm.claimFortuneWheelReward(shards5, nowMs = day8Time)
            assertEquals(shardsBeforeWheel + 5, vm.allHeroes.value.first { it.id == "elios" }.shards)

            // 4. Unlocked Elios matches Career stage 201 (Rare +5 shards reward)
            val shardsBeforeCareer = vm.allHeroes.value.first { it.id == "elios" }.shards
            val stage201 = vm.campaignChapters.value.flatMap { it.stages }.first { it.id == 201 }
            vm.startCampaignBattle(stage201)
            val st = vm.battleState.value!!
            st.enemyCards.drop(1).forEach { e ->
                e.currentHp = 0
                e.isAlive = false
            }
            val aliveEnemy = st.enemyCards.first()
            aliveEnemy.currentArmor = 0
            aliveEnemy.currentHp = 1
            val attacker = st.playerCards.first { it.isAlive }
            attacker.atk = 999
            vm.selectPlayerSlot(attacker.slotIndex)
            vm.executePlayerAttack(aliveEnemy.slotIndex)
            assertEquals(BattlePhase.VICTORY, vm.battleState.value?.phase)
            assertEquals(shardsBeforeCareer + 5, vm.allHeroes.value.first { it.id == "elios" }.shards)
        } finally {
            prefs.edit().clear().commit()
            GameLocaleState.resetForTest(AppLanguage.RU, chosen = false)
        }
    }

    @Test
    fun `career stage rewards drop to 35 percent after more than 5 clears and 20 percent after more than 10 clears`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = GameDataRepository(app)
        val prefs = app.getSharedPreferences("plump_heroes_prefs", Context.MODE_PRIVATE)
        try {
            prefs.edit().clear().commit()
            val stageTemplate = repo.getCampaignChapters().first().stages.first { it.id == 101 }

            // 1. Verify payout curve directly across clearCount boundaries
            val firstClear = calculateCareerStagePayout(stageTemplate.copy(stars = 0, clearCount = 0))
            assertEquals(stageTemplate.rewardGold, firstClear.gold)
            assertEquals(stageTemplate.rewardGems, firstClear.gems)
            assertEquals(stageTemplate.rewardExp, firstClear.exp)
            assertEquals(5, firstClear.shards)

            val fifthClearRepeat = calculateCareerStagePayout(stageTemplate.copy(stars = 4, clearCount = 5))
            assertEquals((stageTemplate.rewardGold * 0.50f).toInt(), fifthClearRepeat.gold)
            assertEquals(stageTemplate.rewardGems, fifthClearRepeat.gems)
            assertEquals(stageTemplate.rewardExp, fifthClearRepeat.exp)
            assertEquals(5, fifthClearRepeat.shards)

            val overFiveClears = calculateCareerStagePayout(stageTemplate.copy(stars = 4, clearCount = 6))
            assertEquals((stageTemplate.rewardGold * 0.35f).toInt(), overFiveClears.gold)
            assertEquals((stageTemplate.rewardGems * 0.35f).toInt(), overFiveClears.gems)
            assertEquals((stageTemplate.rewardExp * 0.35f).toInt(), overFiveClears.exp)
            assertEquals(2, overFiveClears.shards)

            val tenthClear = calculateCareerStagePayout(stageTemplate.copy(stars = 4, clearCount = 10))
            assertEquals(overFiveClears, tenthClear)

            val overTenClears = calculateCareerStagePayout(stageTemplate.copy(stars = 4, clearCount = 11))
            assertEquals((stageTemplate.rewardGold * 0.20f).toInt(), overTenClears.gold)
            assertEquals((stageTemplate.rewardGems * 0.20f).toInt(), overTenClears.gems)
            assertEquals((stageTemplate.rewardExp * 0.20f).toInt(), overTenClears.exp)
            assertEquals(1, overTenClears.shards)

            // 2. End-to-end ViewModel execution with persisted clearCounts: 5 clears -> 6th (normal repeat) -> 7th (35%) -> 12th (20%)
            repo.savePlayerProfile(
                PlayerProfile(
                    name = "Farmer",
                    level = 12,
                    gold = 1000,
                    gems = 100,
                    exp = 0,
                    maxExp = 999999,
                    isTutorialCompleted = true,
                    activeDeckHeroIds = listOf("brunhilde", "olga", "berta", "witch")
                )
            )
            repo.saveCampaignStars(mapOf(101 to 4))
            repo.saveCampaignClearCounts(mapOf(101 to 5))
            val heroes = repo.getInitialHeroCards().map {
                if (it.rarity == CardRarity.BASIC && it.id != "brunhilde") {
                    it.copy(isUnlocked = true, level = it.rarity.levelCap, shards = 0)
                } else if (it.id == "brunhilde") {
                    it.copy(isUnlocked = true, level = 1, shards = 0)
                } else it
            }
            repo.saveHeroesState(heroes)

            val vm = GameViewModel(app)
            fun winStage101() {
                val st101 = vm.campaignChapters.value.first().stages.first { it.id == 101 }
                vm.startCampaignBattle(st101)
                val st = vm.battleState.value!!
                st.enemyCards.drop(1).forEach { e -> e.currentHp = 0; e.isAlive = false }
                val aliveEnemy = st.enemyCards.first()
                aliveEnemy.currentArmor = 0
                aliveEnemy.currentHp = 1
                val attacker = st.playerCards.first { it.isAlive }
                attacker.atk = 999
                vm.selectPlayerSlot(attacker.slotIndex)
                vm.executePlayerAttack(aliveEnemy.slotIndex)
                assertEquals(BattlePhase.VICTORY, vm.battleState.value?.phase)
                vm.exitBattle()
            }

            // clearCount == 5 (NOT > 5): repeat rule applies (50% gold, 100% gems/exp/shards) -> advances clearCount to 6
            winStage101()
            assertEquals(1000 + fifthClearRepeat.gold, vm.playerProfile.value.gold)
            assertEquals(100 + fifthClearRepeat.gems, vm.playerProfile.value.gems)
            assertEquals(fifthClearRepeat.exp, vm.playerProfile.value.exp)
            assertEquals(5, vm.allHeroes.value.first { it.id == "brunhilde" }.shards)
            assertEquals(6, repo.loadCampaignClearCounts()[101])

            // clearCount == 6 (> 5): 35% of initial rewards apply!
            val goldBefore35 = vm.playerProfile.value.gold
            val gemsBefore35 = vm.playerProfile.value.gems
            val expBefore35 = vm.playerProfile.value.exp
            val shardsBefore35 = vm.allHeroes.value.first { it.id == "brunhilde" }.shards
            winStage101()
            assertEquals(goldBefore35 + overFiveClears.gold, vm.playerProfile.value.gold)
            assertEquals(gemsBefore35 + overFiveClears.gems, vm.playerProfile.value.gems)
            assertEquals(expBefore35 + overFiveClears.exp, vm.playerProfile.value.exp)
            assertEquals(shardsBefore35 + overFiveClears.shards, vm.allHeroes.value.first { it.id == "brunhilde" }.shards)
            assertEquals(7, repo.loadCampaignClearCounts()[101])

            // Advance persisted clearCount to 11 (> 10) -> 20% of initial rewards apply!
            repo.saveCampaignClearCounts(mapOf(101 to 11))
            val vmOver10 = GameViewModel(app)
            val goldBefore20 = vmOver10.playerProfile.value.gold
            val gemsBefore20 = vmOver10.playerProfile.value.gems
            val expBefore20 = vmOver10.playerProfile.value.exp
            val shardsBefore20 = vmOver10.allHeroes.value.first { it.id == "brunhilde" }.shards
            val st101Over10 = vmOver10.campaignChapters.value.first().stages.first { it.id == 101 }
            vmOver10.startCampaignBattle(st101Over10)
            val st = vmOver10.battleState.value!!
            st.enemyCards.drop(1).forEach { e -> e.currentHp = 0; e.isAlive = false }
            val aliveEnemy = st.enemyCards.first()
            aliveEnemy.currentArmor = 0
            aliveEnemy.currentHp = 1
            val attacker = st.playerCards.first { it.isAlive }
            attacker.atk = 999
            vmOver10.selectPlayerSlot(attacker.slotIndex)
            vmOver10.executePlayerAttack(aliveEnemy.slotIndex)
            assertEquals(BattlePhase.VICTORY, vmOver10.battleState.value?.phase)
            assertEquals(goldBefore20 + overTenClears.gold, vmOver10.playerProfile.value.gold)
            assertEquals(gemsBefore20 + overTenClears.gems, vmOver10.playerProfile.value.gems)
            assertEquals(expBefore20 + overTenClears.exp, vmOver10.playerProfile.value.exp)
            assertEquals(shardsBefore20 + overTenClears.shards, vmOver10.allHeroes.value.first { it.id == "brunhilde" }.shards)
            assertEquals(12, repo.loadCampaignClearCounts()[101])
        } finally {
            prefs.edit().clear().commit()
            GameLocaleState.resetForTest(AppLanguage.RU, chosen = false)
        }
    }

    @Test
    fun `rarity chest probabilities boost rare by 5 percent and special by 10 percent at cost of basic`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val prefs = app.getSharedPreferences("plump_heroes_prefs", Context.MODE_PRIVATE)
        try {
            prefs.edit().clear().commit()
            val vm = GameViewModel(app)
            val distribution = (0 until 100).groupingBy { vm.resolveRarityChestTargetRarity(it) }.eachCount()

            assertEquals("Basic must be 15% (reduced from 30% by 15%)", 15, distribution[CardRarity.BASIC])
            assertEquals("Special must be 40% (boosted by +10% from 30%)", 40, distribution[CardRarity.SPECIAL])
            assertEquals("Rare must be 25% (boosted by +5% from 20%)", 25, distribution[CardRarity.RARE])
            assertEquals("Mythic must remain 20%", 20, distribution[CardRarity.MYTHIC])
        } finally {
            prefs.edit().clear().commit()
        }
    }

    @Test
    fun `secret hero Vasilisa profile, slow ATK growth, 6 portraits, gallery, zero quotes, EN localization, and Arena exclusion`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = GameDataRepository(app)
        val prefs = app.getSharedPreferences("plump_heroes_prefs", Context.MODE_PRIVATE)
        try {
            prefs.edit().clear().commit()
            val vasilisa = repo.getInitialHeroCards().first { it.id == "vasilisa" }

            assertTrue(vasilisa.isSecret)
            assertFalse(vasilisa.isUnlocked)
            assertEquals(VASILISA_UNLOCK_SHARDS, vasilisa.shardsNeeded)
            assertEquals(75, vasilisa.secretUnlockShardsTarget())
            assertEquals("Василиса", vasilisa.name)
            assertEquals("Снайпер Пустыни", vasilisa.title)
            assertEquals(HeroType.HYBRID, vasilisa.heroType)
            assertEquals(CardRarity.MYTHIC, vasilisa.rarity)
            assertEquals(Element.FIRE, vasilisa.element)
            assertEquals(AttackPattern.SINGLE, vasilisa.attackPattern)
            assertEquals(10, vasilisa.baseAtk)
            assertEquals(0, vasilisa.baseArmor)
            assertEquals(20, vasilisa.baseHp)
            assertEquals(1, vasilisa.atkIncrementPerLevel())
            assertEquals(3, vasilisa.availableSkills.size)
            assertEquals("vasilisa_skill_1", vasilisa.availableSkills[0].id)
            assertEquals("По касательной", vasilisa.availableSkills[0].name)
            assertEquals(3, vasilisa.availableSkills[0].unlockLevel)
            assertEquals(2, vasilisa.availableSkills[0].energyCost)
            assertEquals("vasilisa_skill_2", vasilisa.availableSkills[1].id)
            assertEquals("Легкая рука", vasilisa.availableSkills[1].name)
            assertEquals(5, vasilisa.availableSkills[1].unlockLevel)
            assertEquals(3, vasilisa.availableSkills[1].energyCost)
            assertEquals("vasilisa_skill_3", vasilisa.availableSkills[2].id)
            assertEquals("Зигзак", vasilisa.availableSkills[2].name)
            assertEquals(8, vasilisa.availableSkills[2].unlockLevel)
            assertEquals(4, vasilisa.availableSkills[2].energyCost)

            // EN localization check
            GameLocaleState.resetForTest(AppLanguage.EN, chosen = true)
            assertEquals("Vasilisa", vasilisa.localizedName())
            assertEquals("Desert Sniper", vasilisa.localizedTitle())
            assertEquals("Graze Shot", vasilisa.availableSkills[0].localizedName())
            assertEquals("Light Hand", vasilisa.availableSkills[1].localizedName())
            assertEquals("Zigzag", vasilisa.availableSkills[2].localizedName())
            GameLocaleState.resetForTest(AppLanguage.RU, chosen = false)

            // 6 distinct portrait tiers and 6 gallery images
            val tierPortraits = listOf(1, 3, 5, 8, 10, 12).map { lvl ->
                resolveHeroImageNameForLevel("vasilisa", lvl)
            }
            assertEquals(
                listOf(
                    "hero_vasilisa_0",
                    "hero_vasilisa",
                    "hero_vasilisa_1",
                    "hero_vasilisa_2",
                    "hero_vasilisa_3",
                    "hero_vasilisa_max"
                ),
                tierPortraits
            )
            assertEquals(6, getUnlockedHeroGalleryImages("vasilisa", 12, R.drawable.hero_vasilisa_max).size)

            // Zero battle quotes at any level
            for (lvl in 1..12) {
                assertTrue(getHeroPhrasesForLevel("vasilisa", lvl, QuoteContext.BATTLE_START).isEmpty())
                assertTrue(getHeroPhrasesForLevel("vasilisa", lvl, QuoteContext.VICTORY).isEmpty())
                val card = BattleCard(
                    heroId = "vasilisa", name = "Василиса", title = "Снайпер Пустыни",
                    heroType = HeroType.HYBRID, rarity = CardRarity.MYTHIC, element = Element.FIRE,
                    imageRes = R.drawable.hero_vasilisa_max, slotIndex = 0, isPlayer = true,
                    atk = 20, maxArmor = 0, currentArmor = 0, maxHp = 50,
                    currentHp = 50, level = lvl
                )
                assertNull(pickHeroBattleQuote(card, QuoteContext.BATTLE_START))
                assertNull(pickHeroBattleQuote(card, QuoteContext.VICTORY))
            }

            // Slow ATK growth: upgrading Vasilisa Lv 1 -> Lv 3 increases baseAtk by +2 (+1/lvl), HP by +12 (+6/lvl)
            val heroesReady = repo.getInitialHeroCards().map {
                if (it.id == "vasilisa") it.copy(isUnlocked = true, level = 1, shards = 100, shardsNeeded = 10)
                else it
            }
            repo.saveHeroesState(heroesReady)
            repo.savePlayerProfile(PlayerProfile(name = "Tester", level = 10, gold = 50000, isTutorialCompleted = true))
            val vm = GameViewModel(app)
            vm.upgradeHeroLevel("vasilisa")
            vm.upgradeHeroLevel("vasilisa")
            val upgradedVasilisa = vm.allHeroes.value.first { it.id == "vasilisa" }
            assertEquals(3, upgradedVasilisa.level)
            assertEquals(12, upgradedVasilisa.baseAtk)
            assertEquals(32, upgradedVasilisa.baseHp)

            // Arena must NEVER include elios or vasilisa
            val arenaPool = repo.generateArenaOpponents(playerLevel = 12, playerTrophies = 1500)
            arenaPool.forEach { bot ->
                assertFalse("Arena must not spawn Elios", bot.heroIds.contains("elios"))
                assertFalse("Arena must not spawn Vasilisa", bot.heroIds.contains("vasilisa"))
            }
        } finally {
            prefs.edit().clear().commit()
            GameLocaleState.resetForTest(AppLanguage.RU, chosen = false)
        }
    }

    @Test
    fun `dual secret drop odds split 7_5 percent each and Vasilisa unlocks via Open at 75 shards`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val prefs = app.getSharedPreferences("plump_heroes_prefs", Context.MODE_PRIVATE)
        try {
            prefs.edit().clear().commit()
            val repo = GameDataRepository(app)
            repo.savePlayerProfile(PlayerProfile(name = "SecretDuoTester", level = 5, gold = 10000, gems = 5000, isTutorialCompleted = true))
            val vm = GameViewModel(app)

            // Both Elios & Vasilisa locked: 0..74 -> Elios (7.5%), 75..149 -> Vasilisa (7.5%), 150..999 -> normal
            vm.openRarityChestWithSecretRoll(secretDropRollPermille = 149, fixedSecretShards = 6)
            val res = vm.chestOpeningResult.value!!
            assertTrue(res.isSecretMystery)
            assertNull("Mystery drop must hide heroId", res.heroId)
            assertEquals(6, res.shardAmount)
            assertEquals(6, vm.allHeroes.value.first { it.id == "vasilisa" }.shards)
            assertEquals(0, vm.allHeroes.value.first { it.id == "elios" }.shards)
            assertFalse(res.summaryMessage.contains("Василиса"))
            assertFalse(res.summaryMessage.contains("Vasilisa"))
            assertTrue(vm.latestDropMessage.value?.contains("секретный персонаж", ignoreCase = true) == true)
            vm.dismissChestOpening()

            // Cannot unlock at 74 shards (75 required)
            repeat(11) {
                vm.openRarityChestWithSecretRoll(secretDropRollPermille = 100, fixedSecretShards = 6)
                vm.dismissChestOpening()
            }
            vm.openRarityChestWithSecretRoll(secretDropRollPermille = 100, fixedSecretShards = 2)
            vm.dismissChestOpening()
            assertEquals(74, vm.allHeroes.value.first { it.id == "vasilisa" }.shards)
            vm.unlockSecretHero("vasilisa")
            assertFalse(vm.allHeroes.value.first { it.id == "vasilisa" }.isUnlocked)
            assertNull(vm.secretHeroUnlockedPopup.value)

            // Reach 77 shards -> still locked until clicking Open, then unlocks carrying 2 shards + shardsNeeded=10
            vm.openRarityChestWithSecretRoll(secretDropRollPermille = 100, fixedSecretShards = 3)
            vm.dismissChestOpening()
            assertEquals(77, vm.allHeroes.value.first { it.id == "vasilisa" }.shards)
            assertFalse(vm.allHeroes.value.first { it.id == "vasilisa" }.isUnlocked)

            vm.unlockSecretHero("vasilisa")
            val unlockedVasilisa = vm.allHeroes.value.first { it.id == "vasilisa" }
            assertTrue(unlockedVasilisa.isUnlocked)
            assertEquals(2, unlockedVasilisa.shards)
            assertEquals(10, unlockedVasilisa.shardsNeeded)
            assertEquals("vasilisa", vm.secretHeroUnlockedPopup.value?.id)
            vm.dismissSecretHeroUnlockedPopup()

            // Once Vasilisa is unlocked while Elios stays locked:
            // 0..74 (7.5%) -> Elios mystery, 75..999 (92.5%) -> normal drop (never triggers mystery)
            val eliosShardsBefore = vm.allHeroes.value.first { it.id == "elios" }.shards
            vm.openRarityChestWithSecretRoll(secretDropRollPermille = 74, fixedSecretShards = 5)
            assertTrue(vm.chestOpeningResult.value?.isSecretMystery == true)
            assertNull(vm.chestOpeningResult.value?.heroId)
            assertEquals(eliosShardsBefore + 5, vm.allHeroes.value.first { it.id == "elios" }.shards)
            vm.dismissChestOpening()

            vm.openRarityChestWithSecretRoll(secretDropRollPermille = 75, fixedSecretShards = 5)
            assertFalse("Permille 75 must be normal drop once only 1 secret remains locked", vm.chestOpeningResult.value?.isSecretMystery ?: true)
            vm.dismissChestOpening()
        } finally {
            prefs.edit().clear().commit()
            GameLocaleState.resetForTest(AppLanguage.RU, chosen = false)
        }
    }

    @Test
    fun `secret hero Vasilisa targets any row and executes Graze Shot, Light Hand, and Zigzag accurately`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = GameDataRepository(app)
        val vasilisaTemplate = repo.getInitialHeroCards().first { it.id == "vasilisa" }
        val skill1 = vasilisaTemplate.availableSkills[0] // По касательной
        val skill2 = vasilisaTemplate.availableSkills[1] // Легкая рука
        val skill3 = vasilisaTemplate.availableSkills[2] // Зигзак

        fun buildBattleVm(activeSkill: HeroSkill, initialMana: Int = 10): BattleViewModel {
            val vasilisaPlayer = BattleCard(
                heroId = "vasilisa", name = "Василиса", title = "Снайпер Пустыни",
                heroType = HeroType.HYBRID, rarity = CardRarity.MYTHIC, element = Element.FIRE,
                imageRes = R.drawable.hero_vasilisa_max, slotIndex = 2, isPlayer = true,
                atk = 40, maxArmor = 0, currentArmor = 0, maxHp = 120,
                currentHp = 120, level = 12,
                skill = activeSkill,
                attackPattern = AttackPattern.SINGLE
            )
            val enemies = (0..3).map { s ->
                BattleCard(
                    heroId = "brunhilde", name = "Enemy$s", title = "T",
                    heroType = HeroType.MELEE, rarity = CardRarity.BASIC, element = Element.LIGHT,
                    imageRes = R.drawable.hero_brunhilde, slotIndex = s, isPlayer = false,
                    atk = 10, maxArmor = 0, currentArmor = 0, maxHp = 200,
                    currentHp = 200, level = 5
                )
            }
            return BattleViewModel().also { bvm ->
                bvm.startBattle(
                    playerCards = listOf(vasilisaPlayer),
                    enemyCards = enemies,
                    enemyName = "Test",
                    enemyTitle = "Test",
                    isArena = false,
                    rewardGold = 0,
                    rewardGems = 0,
                    rewardExp = 0,
                    initialPlayerMana = initialMana,
                    onBattleEnd = { _, _ -> }
                )
            }
        }

        // 1. HYBRID can target ANY alive slot (0..3) even while front row is alive
        val bvm1 = buildBattleVm(skill1)
        val st1 = bvm1.battleState.value!!
        for (slot in 0..3) {
            assertTrue("Vasilisa Hybrid must be able to target slot $slot", bvm1.canCardTargetSlot(st1.playerCards[0], slot, st1.enemyCards))
        }

        // 2. Skill 1 (По касательной): deals 100% (40) to primary + 25% (10) to another alive enemy
        bvm1.selectPlayerSlot(2)
        bvm1.executePlayerSkill()
        val afterSkill1 = bvm1.battleState.value!!.enemyCards
        val damagedCount1 = afterSkill1.count { it.currentHp < it.maxHp }
        val totalDmg1 = afterSkill1.sumOf { it.maxHp - it.currentHp }
        assertEquals(2, damagedCount1)
        assertEquals(50, totalDmg1) // 40 + 10

        // 3. Skill 2 (Легкая рука): 35% crit (roll < 35 -> 60 dmg, roll >= 35 -> 40 dmg)
        val bvmCrit = buildBattleVm(skill2)
        bvmCrit.vasilisaCritRollOverride = 34
        bvmCrit.selectPlayerSlot(2)
        bvmCrit.executePlayerSkill()
        assertEquals(140, bvmCrit.battleState.value!!.enemyCards.first().currentHp)

        val bvmNorm = buildBattleVm(skill2)
        bvmNorm.vasilisaCritRollOverride = 35
        bvmNorm.selectPlayerSlot(2)
        bvmNorm.executePlayerSkill()
        assertEquals(160, bvmNorm.battleState.value!!.enemyCards.first().currentHp)

        // 4. Skill 3 (Зигзак): hits slot 0 (100% = 40) AND diagonal slot 3 (100% = 40)
        val bvmZig = buildBattleVm(skill3)
        bvmZig.selectPlayerSlot(2)
        bvmZig.executePlayerSkill()
        val zigEnemies = bvmZig.battleState.value!!.enemyCards
        assertEquals(160, zigEnemies[0].currentHp)
        assertEquals(200, zigEnemies[1].currentHp)
        assertEquals(200, zigEnemies[2].currentHp)
        assertEquals(160, zigEnemies[3].currentHp)

        // Zigzag with dead diagonal slot 3 skips dead card cleanly
        val bvmZigDeadDiag = buildBattleVm(skill3)
        bvmZigDeadDiag.battleState.value!!.enemyCards[3].apply { currentHp = 0; isAlive = false }
        bvmZigDeadDiag.battleState.value!!.enemyCards[2].apply { currentHp = 0; isAlive = false }
        bvmZigDeadDiag.battleState.value!!.enemyCards[1].apply { currentHp = 0; isAlive = false }
        bvmZigDeadDiag.selectPlayerSlot(2)
        bvmZigDeadDiag.executePlayerSkill()
        assertEquals(160, bvmZigDeadDiag.battleState.value!!.enemyCards[0].currentHp)
        assertEquals(0, bvmZigDeadDiag.battleState.value!!.enemyCards[3].currentHp)
        assertFalse(bvmZigDeadDiag.battleState.value!!.combatTexts.any { !it.isPlayer && it.slotIndex == 3 })
    }

    @Test
    fun `career stage 203 swaps back row for Vasilisa after unlock and coliseum spawns unlocked secrets from battle 6 at max 30 percent`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = GameDataRepository(app)
        val prefs = app.getSharedPreferences("plump_heroes_prefs", Context.MODE_PRIVATE)
        try {
            prefs.edit().clear().commit()
            val stage203 = repo.getCampaignChapters().flatMap { it.stages }.first { it.id == 203 }
            val lockedHeroes = repo.getInitialHeroCards()
            val unlockedVasilisaHeroes = lockedHeroes.map {
                if (it.id == "vasilisa") it.copy(isUnlocked = true) else it
            }

            val vm = GameViewModel(app)
            // Locked -> original stage 203 roster preserved
            assertEquals(stage203.enemyHeroIds, vm.resolveEffectiveCampaignEnemyHeroIds(stage203, lockedHeroes, replaceBackSlot = 2))

            // Unlocked -> replaces requested back row slot (2 or 3) with vasilisa, keeping front row untouched
            val swappedSlot2 = vm.resolveEffectiveCampaignEnemyHeroIds(stage203, unlockedVasilisaHeroes, replaceBackSlot = 2)
            assertEquals(listOf(stage203.enemyHeroIds[0], stage203.enemyHeroIds[1], "vasilisa", stage203.enemyHeroIds[3]), swappedSlot2)
            val swappedSlot3 = vm.resolveEffectiveCampaignEnemyHeroIds(stage203, unlockedVasilisaHeroes, replaceBackSlot = 3)
            assertEquals(listOf(stage203.enemyHeroIds[0], stage203.enemyHeroIds[1], stage203.enemyHeroIds[2], "vasilisa"), swappedSlot3)

            // Coliseum: battles 1..5 NEVER spawn secret heroes even at secretSpawnRoll = 0
            for (battleNo in 1..5) {
                val bot = repo.generateColiseumOpponent(
                    battleNumber = battleNo,
                    playerDeckAtk = 100,
                    playerDeckHp = 300,
                    avgHeroLevel = 8,
                    unlockedSecretHeroIds = setOf("elios", "vasilisa"),
                    secretSpawnRoll = 0
                )
                assertFalse(bot.heroIds.contains("elios"))
                assertFalse(bot.heroIds.contains("vasilisa"))
            }

            // Battle >= 6 with NO unlocked secrets -> never spawns secret heroes
            val botLocked6 = repo.generateColiseumOpponent(
                battleNumber = 6,
                playerDeckAtk = 100,
                playerDeckHp = 300,
                avgHeroLevel = 8,
                unlockedSecretHeroIds = emptySet(),
                secretSpawnRoll = 0
            )
            assertFalse(botLocked6.heroIds.contains("elios"))
            assertFalse(botLocked6.heroIds.contains("vasilisa"))

            // Battle >= 6 with unlocked secret and roll == 30 (outside 30%) -> never spawns
            val botMiss6 = repo.generateColiseumOpponent(
                battleNumber = 6,
                playerDeckAtk = 100,
                playerDeckHp = 300,
                avgHeroLevel = 8,
                unlockedSecretHeroIds = setOf("elios", "vasilisa"),
                secretSpawnRoll = 30
            )
            assertFalse(botMiss6.heroIds.contains("elios"))
            assertFalse(botMiss6.heroIds.contains("vasilisa"))

            // Battle >= 6 with unlocked secret and roll == 29 (< 30%) -> spawns unlocked secret hero
            val botHit6 = repo.generateColiseumOpponent(
                battleNumber = 6,
                playerDeckAtk = 100,
                playerDeckHp = 300,
                avgHeroLevel = 8,
                unlockedSecretHeroIds = setOf("elios", "vasilisa"),
                secretSpawnRoll = 29
            )
            assertTrue("Coliseum battle 6+ must include an unlocked secret when roll < 30", botHit6.heroIds.any { it == "elios" || it == "vasilisa" })
        } finally {
            prefs.edit().clear().commit()
        }
    }

    @Test
    fun `chest shard ranges scale when more than 50 percent of roster is level 5+ or above level 9`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = GameDataRepository(app)
        val prefs = app.getSharedPreferences("plump_heroes_prefs", Context.MODE_PRIVATE)
        try {
            prefs.edit().clear().commit()
            val baseHeroes = repo.getInitialHeroCards()
            val vm = GameViewModel(app)

            // 1. Exactly 50% (7 of 14) at Lv 5 -> NOT > 50%, stays default 3..7 and 8..15 for all rarities
            val exactlyHalfGe5 = baseHeroes.mapIndexed { idx, h ->
                if (idx < 7) h.copy(isUnlocked = true, level = 5) else h.copy(level = 1)
            }
            assertEquals(3..7, vm.resolveBasicChestShardRange(exactlyHalfGe5, CardRarity.BASIC))
            assertEquals(3..7, vm.resolveBasicChestShardRange(exactlyHalfGe5, CardRarity.RARE))
            assertEquals(8..15, vm.resolveRarityChestShardRange(exactlyHalfGe5, CardRarity.SPECIAL))
            assertEquals(8..15, vm.resolveRarityChestShardRange(exactlyHalfGe5, CardRarity.RARE))

            // 2. More than 50% (8 of 14) at Lv >= 5 (and <= 9) -> Basic chest: 10..15 BASIC, 8..10 SPECIAL/RARE/MYTHIC; Rarity chest: 20..25
            val overHalfGe5 = baseHeroes.mapIndexed { idx, h ->
                if (idx < 8) h.copy(isUnlocked = true, level = 5) else h.copy(level = 1)
            }
            assertEquals(10..15, vm.resolveBasicChestShardRange(overHalfGe5, CardRarity.BASIC))
            assertEquals(8..10, vm.resolveBasicChestShardRange(overHalfGe5, CardRarity.SPECIAL))
            assertEquals(8..10, vm.resolveBasicChestShardRange(overHalfGe5, CardRarity.RARE))
            assertEquals(8..10, vm.resolveBasicChestShardRange(overHalfGe5, CardRarity.MYTHIC))
            assertEquals(20..25, vm.resolveRarityChestShardRange(overHalfGe5, CardRarity.BASIC))
            assertEquals(20..25, vm.resolveRarityChestShardRange(overHalfGe5, CardRarity.SPECIAL))
            assertEquals(20..25, vm.resolveRarityChestShardRange(overHalfGe5, CardRarity.RARE))
            assertEquals(20..25, vm.resolveRarityChestShardRange(overHalfGe5, CardRarity.MYTHIC))

            // 3. Exactly 50% (7 of 14) > Lv 9, others Lv 5 -> >50% ge5 tier applies (10..15 / 8..10 & 20..25), NOT >9 tier yet
            val halfOver9HalfGe5 = baseHeroes.mapIndexed { idx, h ->
                if (idx < 7) h.copy(isUnlocked = true, level = 10) else h.copy(isUnlocked = true, level = 5)
            }
            assertEquals(10..15, vm.resolveBasicChestShardRange(halfOver9HalfGe5, CardRarity.BASIC))
            assertEquals(8..10, vm.resolveBasicChestShardRange(halfOver9HalfGe5, CardRarity.RARE))
            assertEquals(20..25, vm.resolveRarityChestShardRange(halfOver9HalfGe5, CardRarity.SPECIAL))

            // 4. More than 50% (8 of 14) > Lv 9 (Lv 10+) -> Basic chest: 25..33 BASIC, 15..20 SPECIAL/RARE/MYTHIC; Rarity chest: 35..45
            val overHalfOver9 = baseHeroes.mapIndexed { idx, h ->
                if (idx < 8) h.copy(isUnlocked = true, level = 10) else h.copy(level = 1)
            }
            assertEquals(25..33, vm.resolveBasicChestShardRange(overHalfOver9, CardRarity.BASIC))
            assertEquals(15..20, vm.resolveBasicChestShardRange(overHalfOver9, CardRarity.SPECIAL))
            assertEquals(15..20, vm.resolveBasicChestShardRange(overHalfOver9, CardRarity.RARE))
            assertEquals(15..20, vm.resolveBasicChestShardRange(overHalfOver9, CardRarity.MYTHIC))
            assertEquals(35..45, vm.resolveRarityChestShardRange(overHalfOver9, CardRarity.BASIC))
            assertEquals(35..45, vm.resolveRarityChestShardRange(overHalfOver9, CardRarity.SPECIAL))
            assertEquals(35..45, vm.resolveRarityChestShardRange(overHalfOver9, CardRarity.RARE))
            assertEquals(35..45, vm.resolveRarityChestShardRange(overHalfOver9, CardRarity.MYTHIC))

            // End-to-end chest open check under > 50% Lv >= 5 state (with 8 heroes at Lv 5 and unlocked secrets so no 1..6 mystery proc)
            val savedMid = baseHeroes.mapIndexed { idx, h ->
                h.copy(isUnlocked = true, level = if (idx < 8) 5 else 1, shards = 0)
            }
            repo.saveHeroesState(savedMid)
            repo.savePlayerProfile(PlayerProfile(name = "ChestScaleTester", level = 10, gold = 50000, gems = 5000, isTutorialCompleted = true))
            val vmMid = GameViewModel(app)
            repeat(10) {
                vmMid.openSummonChest(com.example.ui.screens.ChestType.BASIC)
                val basicResult = vmMid.chestOpeningResult.value!!
                val droppedHero = vmMid.allHeroes.value.first { it.id == basicResult.heroId }
                val dropRarity = if (droppedHero.isSecret && droppedHero.isUnlocked) CardRarity.RARE else droppedHero.rarity
                val expectedBasicRange = if (dropRarity == CardRarity.BASIC) 10..15 else 8..10
                assertTrue(
                    "Expected $expectedBasicRange for $dropRarity, got ${basicResult.shardAmount}",
                    basicResult.shardAmount in expectedBasicRange
                )
                vmMid.dismissChestOpening()

                vmMid.openRarityChestWithSecretRoll(secretDropRollPermille = 900)
                val rarityResult = vmMid.chestOpeningResult.value!!
                assertTrue(rarityResult.shardAmount in 20..25)
                vmMid.dismissChestOpening()
            }

            // End-to-end chest open check under > 50% Lv > 9 state (with 8 heroes at Lv 10 and unlocked secrets so no 1..6 mystery proc)
            val savedHigh = baseHeroes.mapIndexed { idx, h ->
                h.copy(isUnlocked = true, level = if (idx < 8) 10 else 1, shards = 0)
            }
            repo.saveHeroesState(savedHigh)
            val vmHigh = GameViewModel(app)
            repeat(10) {
                vmHigh.openSummonChest(com.example.ui.screens.ChestType.BASIC)
                val basicResult = vmHigh.chestOpeningResult.value!!
                val droppedHero = vmHigh.allHeroes.value.first { it.id == basicResult.heroId }
                val dropRarity = if (droppedHero.isSecret && droppedHero.isUnlocked) CardRarity.RARE else droppedHero.rarity
                val expectedBasicRange = if (dropRarity == CardRarity.BASIC) 25..33 else 15..20
                assertTrue(
                    "Expected $expectedBasicRange for $dropRarity, got ${basicResult.shardAmount}",
                    basicResult.shardAmount in expectedBasicRange
                )
                vmHigh.dismissChestOpening()

                vmHigh.openRarityChestWithSecretRoll(secretDropRollPermille = 900)
                val rarityResult = vmHigh.chestOpeningResult.value!!
                assertTrue(rarityResult.shardAmount in 35..45)
                vmHigh.dismissChestOpening()
            }
        } finally {
            prefs.edit().clear().commit()
        }
    }

    @Test
    fun `berta skill deals 50 percent each to two backline enemies and 100 percent when only one remains`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = GameDataRepository(app)
        val bertaSkill = repo.getInitialHeroCards().first { it.id == "berta" }.availableSkills.first()

        fun buildBertaBattle(aliveBackCount: Int): BattleViewModel {
            val bertaPlayer = BattleCard(
                heroId = "berta", name = "Берта", title = "Пиромантка",
                heroType = HeroType.RANGED, rarity = CardRarity.BASIC, element = Element.FIRE,
                imageRes = R.drawable.hero_berta, slotIndex = 2, isPlayer = true,
                maxHp = 100, currentHp = 100, maxArmor = 0, currentArmor = 0, atk = 40, level = 5,
                skill = bertaSkill
            )
            val front0 = BattleCard(
                heroId = "f0", name = "F0", title = "", heroType = HeroType.MELEE,
                rarity = CardRarity.BASIC, element = Element.LIGHT, imageRes = R.drawable.hero_brunhilde,
                slotIndex = 0, isPlayer = false, maxHp = 200, currentHp = 200, maxArmor = 0, currentArmor = 0, atk = 10
            )
            val back2 = BattleCard(
                heroId = "b2", name = "B2", title = "", heroType = HeroType.RANGED,
                rarity = CardRarity.BASIC, element = Element.LIGHT, imageRes = R.drawable.hero_berta,
                slotIndex = 2, isPlayer = false, maxHp = 200, currentHp = 200, maxArmor = 0, currentArmor = 0, atk = 10
            )
            val back3 = BattleCard(
                heroId = "b3", name = "B3", title = "", heroType = HeroType.RANGED,
                rarity = CardRarity.BASIC, element = Element.LIGHT, imageRes = R.drawable.hero_witch,
                slotIndex = 3, isPlayer = false,
                maxHp = 200,
                currentHp = if (aliveBackCount >= 2) 200 else 0,
                maxArmor = 0, currentArmor = 0, atk = 10,
                isAlive = aliveBackCount >= 2
            )
            return BattleViewModel().also { bvm ->
                bvm.startBattle(
                    playerCards = listOf(bertaPlayer),
                    enemyCards = listOf(front0, back2, back3),
                    enemyName = "Test", enemyTitle = "Test", isArena = false,
                    rewardGold = 0, rewardGems = 0, rewardExp = 0, initialPlayerMana = 10,
                    onBattleEnd = { _, _ -> }
                )
            }
        }

        // 1. Both back-row enemies alive -> each receives 50% (20 dmg)
        val bvmTwo = buildBertaBattle(aliveBackCount = 2)
        bvmTwo.executePlayerSkill()
        val enemiesTwo = bvmTwo.battleState.value!!.enemyCards
        assertEquals(200, enemiesTwo.first { it.slotIndex == 0 }.currentHp)
        assertEquals(180, enemiesTwo.first { it.slotIndex == 2 }.currentHp)
        assertEquals(180, enemiesTwo.first { it.slotIndex == 3 }.currentHp)

        // 2. Only 1 back-row enemy alive -> single back-row enemy receives 100% (40 dmg)
        val bvmOne = buildBertaBattle(aliveBackCount = 1)
        bvmOne.executePlayerSkill()
        val enemiesOne = bvmOne.battleState.value!!.enemyCards
        assertEquals(200, enemiesOne.first { it.slotIndex == 0 }.currentHp)
        assertEquals(160, enemiesOne.first { it.slotIndex == 2 }.currentHp)
        assertEquals(0, enemiesOne.first { it.slotIndex == 3 }.currentHp)
    }

    @Test
    fun `trial tower 7 day rotation, rewards, deck eligibility, progression and save persistence`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = GameDataRepository(app)
        val prefs = app.getSharedPreferences("plump_heroes_prefs", Context.MODE_PRIVATE)
        try {
            prefs.edit().clear().commit()

            // 1. All 7 weekdays resolve cleanly to their exact TowerDayRule
            val cal = java.util.Calendar.getInstance()
            val expectedMap = mapOf(
                java.util.Calendar.MONDAY to TowerDayRule.MONDAY_STEEL,
                java.util.Calendar.TUESDAY to TowerDayRule.TUESDAY_ARCANE_VOLLEY,
                java.util.Calendar.WEDNESDAY to TowerDayRule.WEDNESDAY_ELEMENTAL_STORM,
                java.util.Calendar.THURSDAY to TowerDayRule.THURSDAY_MANA_SURGE,
                java.util.Calendar.FRIDAY to TowerDayRule.FRIDAY_COMMONERS_VALOR,
                java.util.Calendar.SATURDAY to TowerDayRule.SATURDAY_BOSS_SIEGE,
                java.util.Calendar.SUNDAY to TowerDayRule.SUNDAY_GOLDEN_ASSAULT
            )
            for ((calDay, expectedRule) in expectedMap) {
                cal.set(2026, java.util.Calendar.NOVEMBER, 23, 12, 0, 0) // 2026-11-23 is Monday
                while (cal.get(java.util.Calendar.DAY_OF_WEEK) != calDay) {
                    cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
                }
                assertEquals(expectedRule, resolveTowerDayRule(cal.timeInMillis))
            }

            // Wednesday element rotates by week
            val w1 = resolveWednesdayTowerElement(0L)
            val w2 = resolveWednesdayTowerElement(7L * 24L * 3600_000L)
            assertNotSame(w1, w2)

            // 2. Floor generation: Sunday 2x gold, Saturday all bosses + 10 gems, Floor 5 has 10 bonus shards
            val baseFloors = repo.getTowerFloorsForDay(TowerDayRule.THURSDAY_MANA_SURGE, playerLevel = 5, avgDeckLevel = 5)
            val sunFloors = repo.getTowerFloorsForDay(TowerDayRule.SUNDAY_GOLDEN_ASSAULT, playerLevel = 5, avgDeckLevel = 5)
            val satFloors = repo.getTowerFloorsForDay(TowerDayRule.SATURDAY_BOSS_SIEGE, playerLevel = 5, avgDeckLevel = 5)

            assertEquals(TOWER_TOTAL_FLOORS, baseFloors.size)
            assertEquals(10, baseFloors.last().bonusShards)
            assertTrue(baseFloors.last().isBoss)
            assertFalse(baseFloors.first().isBoss)

            for (i in 0 until TOWER_TOTAL_FLOORS) {
                assertEquals("Sunday gold must be 2x", baseFloors[i].rewardGold * 2, sunFloors[i].rewardGold)
                assertTrue("Saturday floor ${i + 1} must be Boss", satFloors[i].isBoss)
                assertEquals("Saturday gems must be +10", baseFloors[i].rewardGems + 10, satFloors[i].rewardGems)
            }

            // 3. Monday & Friday deck eligibility: lenient when < 4 eligible unlocked, strict when >= 4
            val initialHeroes = repo.getInitialHeroCards()
            val vm = GameViewModel(app)
            // Starter roster has 2 Melee + 2 Ranged -> eligible Melee+Hybrid count == 2 (< 4) -> Monday accepts starter deck
            assertTrue(vm.isDeckEligibleForTowerRule(TowerDayRule.MONDAY_STEEL, initialHeroes, listOf("brunhilde", "olga", "berta", "witch")))

            // Unlock all heroes -> plenty of Melee/Hybrid & Rare/Mythic exist
            val allUnlocked = initialHeroes.map { it.copy(isUnlocked = true) }
            val mixedDeckWithRanged = listOf("brunhilde", "olga", "berta", "witch")
            assertFalse(
                "Monday strict rule rejects Ranged when >= 4 Melee/Hybrid heroes are unlocked",
                vm.isDeckEligibleForTowerRule(TowerDayRule.MONDAY_STEEL, allUnlocked, mixedDeckWithRanged)
            )
            val meleeHybridIds = allUnlocked.filter { it.heroType == HeroType.MELEE || it.heroType == HeroType.HYBRID }.take(4).map { it.id }
            assertTrue(
                "Monday strict rule accepts 4 Melee/Hybrid heroes",
                vm.isDeckEligibleForTowerRule(TowerDayRule.MONDAY_STEEL, allUnlocked, meleeHybridIds)
            )

            // Friday strict rule rejects Rare hero when >= 4 Basic/Special unlocked
            val rareId = allUnlocked.first { it.rarity == CardRarity.RARE }.id
            val basicSpecialIds = allUnlocked.filter { it.rarity == CardRarity.BASIC || it.rarity == CardRarity.SPECIAL }.take(4).map { it.id }
            assertFalse(
                "Friday strict rule rejects Rare heroine when >=4 Basic/Special unlocked",
                vm.isDeckEligibleForTowerRule(TowerDayRule.FRIDAY_COMMONERS_VALOR, allUnlocked, basicSpecialIds.take(3) + rareId)
            )
            assertTrue(
                "Friday strict rule accepts 4 Basic/Special heroines",
                vm.isDeckEligibleForTowerRule(TowerDayRule.FRIDAY_COMMONERS_VALOR, allUnlocked, basicSpecialIds)
            )

            // 4. Tower is locked below Profile Level 7
            val thursdayCal = java.util.Calendar.getInstance().apply {
                set(2026, java.util.Calendar.NOVEMBER, 26, 14, 0, 0) // Thursday
            }
            val thuMs = thursdayCal.timeInMillis
            val thuFloors = repo.getTowerFloorsForDay(TowerDayRule.THURSDAY_MANA_SURGE, playerLevel = 7, avgDeckLevel = 3)
            assertFalse("Tower must be locked at player level 1", vm.openTowerScreen(thuMs))
            vm.startTowerBattle(thuFloors[0], nowMs = thuMs)
            assertNull("Tower battle must not start below player level 7", vm.battleState.value)

            // Unlock at level 7 with clean daily attempts
            repo.savePlayerProfile(PlayerProfile(name = "Башенная", level = 7, maxSeenTimestamp = thuMs))
            val vmLv7 = GameViewModel(app)
            assertTrue("Tower opens at player level 7", vmLv7.openTowerScreen(thuMs))
            assertEquals(2, vmLv7.getTowerRemainingAttempts(thuMs))

            // Trying Floor 2 before Floor 1 must be blocked
            vmLv7.startTowerBattle(thuFloors[1], nowMs = thuMs)
            assertNull("Floor 2 must not start before Floor 1 is cleared", vmLv7.battleState.value)

            // Floor 1 starts with 3 player mana on Thursday and deducts 1 attempt while in battle
            vmLv7.startTowerBattle(thuFloors[0], nowMs = thuMs)
            val thuBattle = vmLv7.battleState.value
            assertNotNull(thuBattle)
            assertTrue(thuBattle!!.isTower)
            assertEquals(1, thuBattle.towerFloor)
            assertEquals(3, thuBattle.playerTeamMana)
            assertEquals(1, vmLv7.getTowerRemainingAttempts(thuMs))

            // Surrendering consumes that 1st attempt while remaining at 0 cleared floors; 2nd attempt remains
            vmLv7.surrenderBattle()
            assertEquals(1, vmLv7.getTowerRemainingAttempts(thuMs))
            assertEquals(0, computeTowerClearedFloorToday(vmLv7.playerProfile.value, thuMs))

            // Lose 2nd attempt -> 0 attempts remaining -> next startTowerBattle blocked
            vmLv7.startTowerBattle(thuFloors[0], nowMs = thuMs)
            assertNotNull(vmLv7.battleState.value)
            vmLv7.surrenderBattle()
            assertEquals(0, vmLv7.getTowerRemainingAttempts(thuMs))

            vmLv7.startTowerBattle(thuFloors[0], nowMs = thuMs)
            assertNull("Should block entering Tower when 0/2 daily attempts remain", vmLv7.battleState.value)

            // Next day resets cleared floors to 0 and attempts back to 2
            val fridayMs = thuMs + 24L * 3600_000L
            assertEquals(2, computeTowerRemainingAttempts(vmLv7.playerProfile.value, fridayMs))
            assertEquals(0, computeTowerClearedFloorToday(vmLv7.playerProfile.value, fridayMs))

            // Simulate Floor 2 cleared today & 1 attempt used, then verify JSON export/import persistence
            val todayStr = formatColiseumCalendarDate(System.currentTimeMillis())
            val towerProfile = PlayerProfile(
                name = "Башенная",
                level = 8,
                towerProgressDate = todayStr,
                towerClearedFloorToday = 2,
                towerAttemptsUsedToday = 1,
                towerTotalWins = 9,
                isTowerTutorialCompleted = true
            )
            repo.savePlayerProfile(towerProfile)
            val loadedProfile = repo.loadPlayerProfile()
            assertEquals(todayStr, loadedProfile.towerProgressDate)
            assertEquals(2, loadedProfile.towerClearedFloorToday)
            assertEquals(1, loadedProfile.towerAttemptsUsedToday)
            assertEquals(9, loadedProfile.towerTotalWins)
            assertTrue(loadedProfile.isTowerTutorialCompleted)
            assertEquals(2, computeTowerClearedFloorToday(loadedProfile))
            assertEquals(1, computeTowerRemainingAttempts(loadedProfile))

            val exported = repo.exportSaveToJson(loadedProfile, initialHeroes, repo.getInitialArtifacts(), emptyMap())
            assertTrue(exported.contains("towerClearedFloorToday"))
            assertTrue(exported.contains("towerAttemptsUsedToday"))
            assertTrue(exported.contains("towerTotalWins"))
            assertTrue(exported.contains("isTowerTutorialCompleted"))
            val imported = repo.importSaveFromJson(exported, initialHeroes)
            assertEquals(todayStr, imported?.first?.towerProgressDate)
            assertEquals(2, imported?.first?.towerClearedFloorToday)
            assertEquals(1, imported?.first?.towerAttemptsUsedToday)
            assertEquals(9, imported?.first?.towerTotalWins)
            assertTrue(imported?.first?.isTowerTutorialCompleted == true)
        } finally {
            prefs.edit().clear().commit()
        }
    }

    @Test
    fun `battle hub and tower tutorials follow updated sequences in RU and EN`() {
        val f1Ru = com.example.ui.components.getTutorialFrame(1, "Алиса")
        val f12Ru = com.example.ui.components.getTutorialFrame(12, "Алиса")
        val f15Ru = com.example.ui.components.getTutorialFrame(15, "Алиса")
        val f17Ru = com.example.ui.components.getTutorialFrame(17, "Алиса")
        val f18Ru = com.example.ui.components.getTutorialFrame(18, "Алиса")
        val f21Ru = com.example.ui.components.getTutorialFrame(21, "Алиса")
        val f24Ru = com.example.ui.components.getTutorialFrame(24, "Алиса")
        val f25Ru = com.example.ui.components.getTutorialFrame(25, "Алиса")
        val f28Ru = com.example.ui.components.getTutorialFrame(28, "Алиса")

        assertEquals(0, f1Ru.targetTab)
        assertEquals(1, f12Ru.targetTab) // Forge comes right after Heroes
        assertNull(f12Ru.targetHubSection)
        assertEquals(2, f15Ru.targetTab) // Citadel Hub
        assertEquals("А здесь у нас Цитадель Аврезии или же Хаб.", f15Ru.textTemplate)
        assertEquals(2, f17Ru.targetTab)
        assertEquals("Их тут много. Давай я расскажу о главных, сестра!", f17Ru.textTemplate)
        assertEquals(2, f18Ru.targetTab) // Career
        assertEquals(com.example.ui.screens.HubBattleSection.CAREER, f18Ru.targetHubSection)
        assertEquals(2, f21Ru.targetTab) // Arena
        assertEquals(com.example.ui.screens.HubBattleSection.ARENA, f21Ru.targetHubSection)
        assertEquals(2, f24Ru.targetTab) // Seasoned warriors teaser
        assertNull(f24Ru.targetHubSection)
        assertEquals("Остальные режимы доступны лишь опытным воинам.", f24Ru.textTemplate)
        assertEquals(3, f25Ru.targetTab) // Chests
        assertEquals(2, f28Ru.targetTab) // Back to Hub

        val f15En = com.example.i18n.getTutorialFrameEn(15, "Alice")
        val f17En = com.example.i18n.getTutorialFrameEn(17, "Alice")
        val f18En = com.example.i18n.getTutorialFrameEn(18, "Alice")
        val f21En = com.example.i18n.getTutorialFrameEn(21, "Alice")
        val f24En = com.example.i18n.getTutorialFrameEn(24, "Alice")
        val f28En = com.example.i18n.getTutorialFrameEn(28, "Alice")
        assertEquals(2, f15En.targetTab)
        assertTrue(f15En.textTemplate.contains("Citadel of Avresia"))
        assertEquals("There are quite a few of them. Let me tell you about the main ones, sister!", f17En.textTemplate)
        assertEquals(com.example.ui.screens.HubBattleSection.CAREER, f18En.targetHubSection)
        assertEquals(com.example.ui.screens.HubBattleSection.ARENA, f21En.targetHubSection)
        assertEquals("The remaining modes are only available to seasoned warriors.", f24En.textTemplate)
        assertEquals(2, f28En.targetTab)

        // 6-step Tower tutorial RU & EN
        val t1Ru = com.example.ui.components.getTowerTutorialFrame(1)
        val t2Ru = com.example.ui.components.getTowerTutorialFrame(2)
        val t5Ru = com.example.ui.components.getTowerTutorialFrame(5)
        val t6Ru = com.example.ui.components.getTowerTutorialFrame(6)
        assertEquals("Добро пожаловать в Башню страданий! Вуаха!", t1Ru.textTemplate)
        assertEquals("Сестра! Не выдумывай! Это Башня испытаний!", t2Ru.textTemplate)
        assertEquals("В отличие от Колизея прогресс сохраняется до конца дня.", t5Ru.textTemplate)
        assertEquals("Но у вас только 2 попытки. Удачи!", t6Ru.textTemplate)

        val cyrillicRegex = Regex("[А-Яа-яЁё]")
        for (step in 1..6) {
            val enFrame = com.example.i18n.getTowerTutorialFrameEn(step)
            assertFalse("EN Tower tutorial step $step must have no Cyrillic", cyrillicRegex.containsMatchIn(enFrame.textTemplate))
        }
    }

    @Test
    fun `tower scales like coliseum, unlocks roof at 20 stars with 2x reward, persists damaged enemy HPs, keeps tower deck separate, and bots obey rules`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = GameDataRepository(app)
        val prefs = app.getSharedPreferences("plump_heroes_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        try {
            val templates = repo.getInitialHeroCards()
            val allUnlocked = templates.map { it.copy(isUnlocked = true, level = 8) }
            val activeDeck = listOf("brunhilde", "olga", "berta", "witch")
            val artifacts = repo.getInitialArtifacts()

            // Verify bot decks obey daily Tower rules & row placement across all 7 days
            for (rule in TowerDayRule.entries) {
                val floors = repo.getTowerFloorsForDay(
                    rule = rule,
                    playerLevel = 10,
                    avgDeckLevel = 8,
                    wednesdayElement = Element.FIRE
                )
                val roof = repo.getTowerRoofFloorForDay(
                    rule = rule,
                    playerLevel = 10,
                    avgDeckLevel = 8,
                    wednesdayElement = Element.FIRE
                )
                assertEquals(TOWER_ROOF_FLOOR, roof.floor)
                assertEquals(floors.last().rewardGold * 2, roof.rewardGold)
                assertEquals(floors.last().rewardGems * 2, roof.rewardGems)
                assertEquals(floors.last().rewardExp * 2, roof.rewardExp)
                assertEquals(20, roof.bonusShards)

                for (floor in floors + roof) {
                    val slotHeroes = floor.enemyHeroIds.map { id -> templates.first { it.id == id } }
                    assertEquals("Floor ${floor.floor} must have 4 enemies", 4, slotHeroes.size)
                    // Back row (slots 2, 3) must NEVER hold pure MELEE heroines
                    assertTrue(
                        "Back row slot 2 cannot be MELEE (${rule.name})",
                        slotHeroes[2].heroType != HeroType.MELEE
                    )
                    assertTrue(
                        "Back row slot 3 cannot be MELEE (${rule.name})",
                        slotHeroes[3].heroType != HeroType.MELEE
                    )
                    when (rule) {
                        TowerDayRule.MONDAY_STEEL -> assertTrue(
                            "Monday enemy squad must all be MELEE/HYBRID",
                            slotHeroes.all { it.heroType == HeroType.MELEE || it.heroType == HeroType.HYBRID }
                        )
                        TowerDayRule.FRIDAY_COMMONERS_VALOR -> assertTrue(
                            "Friday enemy squad must all be BASIC/SPECIAL",
                            slotHeroes.all { it.rarity == CardRarity.BASIC || it.rarity == CardRarity.SPECIAL }
                        )
                        TowerDayRule.WEDNESDAY_ELEMENTAL_STORM -> assertTrue(
                            "Wednesday enemy squad must prioritize FIRE element",
                            slotHeroes.count { it.element == Element.FIRE } >= 3
                        )
                        else -> {}
                    }
                }
            }

            // Dedicated Tower squad (assignHeroToTowerSlot) must NOT mutate activeDeckHeroIds
            val baseTimeCal = java.util.Calendar.getInstance().apply {
                set(2026, java.util.Calendar.NOVEMBER, 26, 12, 0, 0) // 2026-11-26 is Thursday
            }
            val thuMs = baseTimeCal.timeInMillis
            repo.savePlayerProfile(
                PlayerProfile(
                    name = "Башня",
                    level = 10,
                    activeDeckHeroIds = activeDeck,
                    maxSeenTimestamp = thuMs,
                    isTutorialCompleted = true,
                    isStoryRead = true,
                    isTowerTutorialCompleted = true
                )
            )
            repo.saveHeroesState(allUnlocked)
            val vm = GameViewModel(app)
            vm.openTowerScreen(thuMs)
            assertEquals(activeDeck, vm.getEffectiveTowerDeckIds())

            vm.assignHeroToTowerSlot(2, "iva")
            assertEquals(listOf("brunhilde", "olga", "iva", "witch"), vm.getEffectiveTowerDeckIds())
            assertEquals(
                "Main active deck must remain unchanged when customizing Tower squad",
                activeDeck,
                vm.playerProfile.value.activeDeckHeroIds
            )

            // Verify scaling factors (-30%, -15%, 0%, +15%, +30%, Roof +50%)
            assertEquals(0.70, calculateTowerScalingMultiplier(1), 0.0001)
            assertEquals(0.85, calculateTowerScalingMultiplier(2), 0.0001)
            assertEquals(1.00, calculateTowerScalingMultiplier(3), 0.0001)
            assertEquals(1.15, calculateTowerScalingMultiplier(4), 0.0001)
            assertEquals(1.30, calculateTowerScalingMultiplier(5), 0.0001)
            assertEquals(1.50, calculateTowerScalingMultiplier(TOWER_ROOF_FLOOR), 0.0001)

            // Verify battle scaling + Thursday 3 enemy mana + defeat persists wounded HP & dead enemies
            val vmHeroes = vm.allHeroes.value
            val towerDeckHeroes = vm.getEffectiveTowerDeckIds().map { id -> vmHeroes.first { it.id == id } }
            val playerBaseTotalAtk = towerDeckHeroes.sumOf { it.calculateTotalAtk(artifacts) }
            val playerBaseTotalHp = towerDeckHeroes.sumOf { it.calculateTotalHp(artifacts) }

            val thuFloors = vm.getTowerFloorsForToday(thuMs)
            vm.startTowerBattle(thuFloors[0], nowMs = thuMs)
            val firstEntry = vm.battleState.value!!
            assertEquals("Thursday enemies should also start with 3 mana", 3, firstEntry.enemyTeamMana)
            val expectedFloor1Hp = kotlin.math.ceil(playerBaseTotalHp * 0.70).toInt()
            val expectedFloor1Atk = kotlin.math.ceil(playerBaseTotalAtk * 0.70).toInt()
            assertEquals(expectedFloor1Hp, firstEntry.enemyCards.sumOf { it.maxHp })
            assertEquals(expectedFloor1Atk, firstEntry.enemyCards.sumOf { it.atk })

            // Attack enemy 0 and surrender -> wounded HP must be saved and restored on re-entry
            vm.executePlayerAttack(0)
            val afterHitState = vm.battleState.value!!
            val enemy0AfterHit = afterHitState.enemyCards.first { it.slotIndex == 0 }
            assertTrue("Enemy 0 must take damage or die after attack", enemy0AfterHit.currentHp < enemy0AfterHit.maxHp)
            val expectedSavedHp0 = if (enemy0AfterHit.isAlive) enemy0AfterHit.currentHp else 0

            vm.surrenderBattle()
            assertEquals(1, vm.playerProfile.value.towerDamagedFloor)
            assertEquals(expectedSavedHp0, vm.playerProfile.value.towerSavedEnemyHps[0])

            vm.startTowerBattle(thuFloors[0], nowMs = thuMs)
            val secondEntry = vm.battleState.value!!
            val reenteredEnemy0 = secondEntry.enemyCards.first { it.slotIndex == 0 }
            assertEquals(expectedSavedHp0, reenteredEnemy0.currentHp)
            assertEquals(expectedSavedHp0 > 0, reenteredEnemy0.isAlive)
            vm.surrenderBattle()

            // Explicitly verify defeated enemy slot (0 HP) stays dead (isAlive == false) & wounded slot stays wounded on re-entry
            val todayStr = formatColiseumCalendarDate(thuMs)
            repo.savePlayerProfile(
                PlayerProfile(
                    name = "Башня",
                    level = 10,
                    activeDeckHeroIds = activeDeck,
                    towerDeckHeroIds = listOf("brunhilde", "olga", "iva", "witch"),
                    towerProgressDate = todayStr,
                    towerClearedFloorToday = 0,
                    towerDamagedFloor = 1,
                    towerSavedEnemyHps = listOf(0, 12, 0, 25),
                    towerAttemptsUsedToday = 0,
                    maxSeenTimestamp = thuMs,
                    isTutorialCompleted = true,
                    isStoryRead = true,
                    isTowerTutorialCompleted = true
                )
            )
            val vmSaved = GameViewModel(app)
            vmSaved.startTowerBattle(thuFloors[0], nowMs = thuMs)
            val savedEnemies = vmSaved.battleState.value!!.enemyCards.sortedBy { it.slotIndex }
            assertFalse("Killed bot in slot 0 must NOT resurrect", savedEnemies[0].isAlive)
            assertEquals(0, savedEnemies[0].currentHp)
            assertTrue("Wounded bot in slot 1 stays alive with saved HP", savedEnemies[1].isAlive)
            assertEquals(12, savedEnemies[1].currentHp)
            assertFalse("Killed bot in slot 2 must NOT resurrect", savedEnemies[2].isAlive)
            assertEquals(0, savedEnemies[2].currentHp)
            assertTrue(savedEnemies[3].isAlive)
            assertEquals(25, savedEnemies[3].currentHp)
            vmSaved.surrenderBattle()

            // Verify Roof unlock logic: requires Floors 1..5 all cleared with 4 stars each
            val partialStarsProfile = PlayerProfile(
                name = "Башня",
                level = 10,
                towerProgressDate = todayStr,
                towerClearedFloorToday = 5,
                towerFloorStarsToday = listOf(4, 4, 3, 4, 4),
                maxSeenTimestamp = thuMs
            )
            assertFalse("Roof stays locked when Floor 3 has only 3 stars", isTowerRoofUnlockedToday(partialStarsProfile, thuMs))

            val fullFourStarsProfile = partialStarsProfile.copy(towerFloorStarsToday = listOf(4, 4, 4, 4, 4))
            assertTrue("Roof unlocks once all 5 floors have 4 stars", isTowerRoofUnlockedToday(fullFourStarsProfile, thuMs))
        } finally {
            prefs.edit().clear().commit()
        }
    }

    @Test
    fun `bonus gallery shows unlocked heroine portraits by level in hero order, hides finale before 100 percent career, and tracks viewed gold borders`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = GameDataRepository(app)
        val prefs = app.getSharedPreferences("plump_heroes_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        try {
            val baseHeroes = repo.getInitialHeroCards()

            // 1. Fresh player (only 4 base heroes unlocked at Level 1, Career < 100%):
            //    Only 4 sections, 1 image (_0) each, no locked heroes, no Finale section.
            val freshSections = com.example.ui.screens.buildBonusGallerySections(
                heroes = baseHeroes,
                isCampaign100Percent = false,
                isEnglish = false
            )
            assertEquals(4, freshSections.size)
            assertEquals(listOf("hero_brunhilde", "hero_olga", "hero_berta", "hero_witch"), freshSections.map { it.id })
            assertTrue(freshSections.all { it.items.size == 1 && !it.isFinale })
            assertTrue(freshSections.none { it.isFinale })

            // 2. Raising hero level reveals portraits strictly in that heroine's section & keeps allHeroes order
            val leveledHeroes = baseHeroes.map { h ->
                when (h.id) {
                    "brunhilde" -> h.copy(level = 5) // _0, default, _1 -> 3 images
                    "alisa" -> h.copy(isUnlocked = true, level = 12) // all 6 forms unlocked
                    else -> h
                }
            }
            val midSections = com.example.ui.screens.buildBonusGallerySections(
                heroes = leveledHeroes,
                isCampaign100Percent = false,
                isEnglish = false
            )
            assertEquals(5, midSections.size)
            assertEquals("hero_brunhilde", midSections[0].id)
            assertEquals(3, midSections[0].items.size)
            assertEquals(
                listOf("hero_brunhilde_0", "hero_brunhilde", "hero_brunhilde_1"),
                midSections[0].items.map { it.key }
            )
            assertEquals("hero_alisa", midSections[4].id)
            assertEquals(6, midSections[4].items.size)
            assertTrue(midSections.none { it.isFinale })

            // 3. After 100% Career: Finale appears at the VERY END with bonus_final_1..4 and final_6 last
            val fullSections = com.example.ui.screens.buildBonusGallerySections(
                heroes = leveledHeroes,
                isCampaign100Percent = true,
                isEnglish = false
            )
            assertTrue(fullSections.last().isFinale)
            assertEquals(
                listOf("bonus_final_1", "bonus_final_2", "bonus_final_3", "bonus_final_4", "bonus_final_6"),
                fullSections.last().items.map { it.key }
            )
            assertEquals(R.drawable.final_6, fullSections.last().items.last().imageRes)

            // 4. Unseen tracking (`viewedGalleryImageKeys`) toggles gold border state & persists across save/JSON
            repo.savePlayerProfile(PlayerProfile(name = "Галерея", level = 5, isTutorialCompleted = true, isStoryRead = true))
            val vm = GameViewModel(app)
            assertTrue(vm.playerProfile.value.viewedGalleryImageKeys.isEmpty())
            vm.markBonusGalleryImageViewed("hero_brunhilde_0")
            assertTrue("hero_brunhilde_0" in vm.playerProfile.value.viewedGalleryImageKeys)

            val loaded = repo.loadPlayerProfile()
            assertTrue("hero_brunhilde_0" in loaded.viewedGalleryImageKeys)

            val exported = repo.exportSaveToJson(loaded, leveledHeroes, repo.getInitialArtifacts(), emptyMap())
            val imported = repo.importSaveFromJson(exported, baseHeroes)
            assertTrue("hero_brunhilde_0" in (imported?.first?.viewedGalleryImageKeys ?: emptySet()))
        } finally {
            prefs.edit().clear().commit()
        }
    }

    @Test
    fun `roster supports dual hero type and 4-element filtering while disabling unavailable elements`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = GameDataRepository(app)
        val heroes = repo.getInitialHeroCards()

        // 4 elements are represented in roster (NATURE is unused)
        assertEquals(
            listOf(Element.FIRE, Element.WATER, Element.LIGHT, Element.DARK),
            HERO_FILTER_ELEMENTS
        )

        // With null (All) type filter: all 4 elements are available
        val allAvailable = com.example.ui.screens.availableElementsForHeroType(heroes, null)
        assertEquals(setOf(Element.FIRE, Element.WATER, Element.LIGHT, Element.DARK), allAvailable)

        // MELEE has LIGHT, WATER, FIRE — DARK must be unavailable (disabled)
        val meleeAvailable = com.example.ui.screens.availableElementsForHeroType(heroes, HeroType.MELEE)
        assertEquals(setOf(Element.FIRE, Element.WATER, Element.LIGHT), meleeAvailable)
        assertFalse(Element.DARK in meleeAvailable)

        // RANGED has all 4 elements
        val rangedAvailable = com.example.ui.screens.availableElementsForHeroType(heroes, HeroType.RANGED)
        assertEquals(setOf(Element.FIRE, Element.WATER, Element.LIGHT, Element.DARK), rangedAvailable)

        // HYBRID has FIRE, WATER, DARK — LIGHT must be unavailable (disabled)
        val hybridAvailable = com.example.ui.screens.availableElementsForHeroType(heroes, HeroType.HYBRID)
        assertEquals(setOf(Element.FIRE, Element.WATER, Element.DARK), hybridAvailable)
        assertFalse(Element.LIGHT in hybridAvailable)

        // Element-only filter (DARK across all attack types)
        val darkOnly = com.example.ui.screens.filterRosterHeroes(
            heroes = heroes,
            typeFilter = null,
            elementFilter = Element.DARK
        )
        assertEquals(setOf("witch", "demoness"), darkOnly.map { it.id }.toSet())

        // Dual filter (RANGED + FIRE and HYBRID + FIRE)
        val rangedFire = com.example.ui.screens.filterRosterHeroes(
            heroes = heroes,
            typeFilter = HeroType.RANGED,
            elementFilter = Element.FIRE
        )
        assertEquals(setOf("berta"), rangedFire.map { it.id }.toSet())

        val hybridFire = com.example.ui.screens.filterRosterHeroes(
            heroes = heroes,
            typeFilter = HeroType.HYBRID,
            elementFilter = Element.FIRE
        )
        assertEquals(setOf("mi_and_na", "baker", "vasilisa"), hybridFire.map { it.id }.toSet())

        // Incompatible dual filter (MELEE + DARK) returns empty list
        val meleeDark = com.example.ui.screens.filterRosterHeroes(
            heroes = heroes,
            typeFilter = HeroType.MELEE,
            elementFilter = Element.DARK
        )
        assertTrue(meleeDark.isEmpty())
    }

    @Test
    fun `audio mode resolves tavern battle boss victory defeat states and sound mute persists across restarts`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = GameDataRepository(app)
        val prefs = app.getSharedPreferences("plump_heroes_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        com.example.audio.GameAudioManager.resetForTesting(muted = false)

        try {
            // 1. Mode resolution across menu / battle / boss / victory / defeat
            assertEquals(
                com.example.audio.AudioTrackMode.MENU_TAVERN,
                com.example.audio.resolveDesiredAudioMode(null)
            )
            assertEquals(
                com.example.audio.AudioTrackMode.BATTLE_NORMAL,
                com.example.audio.resolveDesiredAudioMode(BattleState(phase = BattlePhase.PLAYER_TURN, isBoss = false))
            )
            assertEquals(
                com.example.audio.AudioTrackMode.BATTLE_BOSS,
                com.example.audio.resolveDesiredAudioMode(BattleState(phase = BattlePhase.ENEMY_TURN, isBoss = true))
            )
            assertEquals(
                com.example.audio.AudioTrackMode.SFX_VICTORY,
                com.example.audio.resolveDesiredAudioMode(BattleState(phase = BattlePhase.VICTORY, isBoss = true))
            )
            assertEquals(
                com.example.audio.AudioTrackMode.SFX_DEFEAT,
                com.example.audio.resolveDesiredAudioMode(BattleState(phase = BattlePhase.DEFEAT, isBoss = false))
            )

            // 2. GameAudioManager reflects mode and mute state cleanly
            com.example.audio.GameAudioManager.setMode(com.example.audio.AudioTrackMode.BATTLE_BOSS)
            assertEquals(com.example.audio.AudioTrackMode.BATTLE_BOSS, com.example.audio.GameAudioManager.getEffectivePlaybackMode())

            // 3. Toggling sound mute in ViewModel persists via SharedPreferences & survives VM recreation
            repo.savePlayerProfile(PlayerProfile(name = "Музыкант", level = 10, isTutorialCompleted = true, isStoryRead = true))
            val vm = GameViewModel(app)
            assertFalse(vm.playerProfile.value.isSoundMuted)
            assertFalse(com.example.audio.GameAudioManager.isMuted())

            vm.toggleSoundMuted()
            assertTrue(vm.playerProfile.value.isSoundMuted)
            assertTrue(com.example.audio.GameAudioManager.isMuted())
            assertEquals(com.example.audio.AudioTrackMode.SILENT, com.example.audio.GameAudioManager.getEffectivePlaybackMode())

            // Simulate app restart (new GameViewModel reading persisted profile)
            com.example.audio.GameAudioManager.resetForTesting(muted = false)
            val restartedVm = GameViewModel(app)
            assertTrue("Sound mute must remain enabled after restart", restartedVm.playerProfile.value.isSoundMuted)
            assertTrue("Audio manager must be muted immediately on restart", com.example.audio.GameAudioManager.isMuted())

            // Verify JSON export/import preserves isSoundMuted
            val exported = repo.exportSaveToJson(
                restartedVm.playerProfile.value,
                restartedVm.allHeroes.value,
                restartedVm.inventoryArtifacts.value,
                emptyMap()
            )
            val imported = repo.importSaveFromJson(exported, repo.getInitialHeroCards())
            assertTrue("Export/import JSON must keep isSoundMuted=true", imported?.first?.isSoundMuted == true)
        } finally {
            com.example.audio.GameAudioManager.resetForTesting(muted = false)
            prefs.edit().clear().commit()
        }
    }

    @Test
    fun `github apk update checker parses version json validates trusted url respects skipped version and names apk consistently`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = GameDataRepository(app)
        val prefs = app.getSharedPreferences("plump_heroes_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        try {
            // 1. APK filename naming convention: plump_heroes_{version}.apk
            assertEquals("plump_heroes_1.3.apk", com.example.network.AppUpdateChecker.buildApkFileName("1.3"))
            assertEquals("plump_heroes_1.4_Beta.apk", com.example.network.AppUpdateChecker.buildApkFileName("1.4 Beta"))

            // 2. Parse valid remote version.json for fatanime/plump_heroes
            val validJson = """
                {
                  "versionCode": 32,
                  "versionName": "1.4",
                  "apkUrl": "https://raw.githubusercontent.com/fatanime/plump_heroes/main/apk-distribution/plump_heroes_1.4.apk",
                  "changelogRu": "Новый режим и баланс",
                  "changelogEn": "New mode and balance"
                }
            """.trimIndent()
            val parsed = com.example.network.AppUpdateChecker.parseRemoteVersionJson(validJson)
            assertNotNull(parsed)
            assertEquals(32, parsed!!.versionCode)
            assertEquals("1.4", parsed.versionName)
            assertEquals("Новый режим и баланс", parsed.changelogRu)
            assertEquals("New mode and balance", parsed.changelogEn)

            // 3. Reject untrusted/non-HTTPS URLs
            val untrustedJson = """
                {
                  "versionCode": 32,
                  "versionName": "1.4",
                  "apkUrl": "https://evil.example.com/app.apk"
                }
            """.trimIndent()
            assertNull(com.example.network.AppUpdateChecker.parseRemoteVersionJson(untrustedJson))

            // 4. Version comparison & skipped version behavior (automatic vs manual)
            assertTrue(
                com.example.network.AppUpdateChecker.shouldNotifyUpdate(
                    remote = parsed,
                    installedVersionCode = 31,
                    skippedVersionCode = 0,
                    ignoreSkipped = false
                )
            )
            assertFalse(
                com.example.network.AppUpdateChecker.shouldNotifyUpdate(
                    remote = parsed,
                    installedVersionCode = 32,
                    skippedVersionCode = 0,
                    ignoreSkipped = true
                )
            )
            assertFalse(
                com.example.network.AppUpdateChecker.shouldNotifyUpdate(
                    remote = parsed,
                    installedVersionCode = 31,
                    skippedVersionCode = 32,
                    ignoreSkipped = false
                )
            )
            assertTrue(
                com.example.network.AppUpdateChecker.shouldNotifyUpdate(
                    remote = parsed,
                    installedVersionCode = 31,
                    skippedVersionCode = 32,
                    ignoreSkipped = true
                )
            )

            // 5. Fallback branch check (main returns null -> master succeeds)
            val fallbackOutcome = kotlinx.coroutines.runBlocking {
                com.example.network.AppUpdateChecker.checkRemoteVersion(
                    installedVersionCode = 31,
                    installedVersionName = "1.3",
                    skippedVersionCode = 0,
                    ignoreSkipped = false,
                    httpFetcher = { url -> if (url.contains("/master/")) validJson else null }
                )
            }
            assertTrue(fallbackOutcome is com.example.network.AppUpdateCheckOutcome.UpdateAvailable)
            assertEquals(32, (fallbackOutcome as com.example.network.AppUpdateCheckOutcome.UpdateAvailable).info.versionCode)

            // 6. Persist skippedUpdateVersionCode across SharedPreferences & JSON export/import
            val profile = PlayerProfile(name = "Обновление", skippedUpdateVersionCode = 32)
            repo.savePlayerProfile(profile)
            assertEquals(32, repo.loadPlayerProfile().skippedUpdateVersionCode)

            val exported = repo.exportSaveToJson(profile, repo.getInitialHeroCards(), repo.getInitialArtifacts(), emptyMap())
            val imported = repo.importSaveFromJson(exported, repo.getInitialHeroCards())
            assertEquals(32, imported?.first?.skippedUpdateVersionCode)
        } finally {
            prefs.edit().clear().commit()
        }
    }

    @Test
    fun `8bit chiptune synthesizer renders clean non-clipping PCM across full menu battle boss loops and one-shot SFX terminate`() {
        val sampleRate = 44100
        val synth = com.example.audio.ChiptuneAudioSynthesizer(sampleRate)
        val buffer = ShortArray(2048)

        // Verify all 3 looping themes render active musical signal across 18 seconds without clipping/zeroing out
        val loopingModes = listOf(
            com.example.audio.AudioTrackMode.MENU_TAVERN,
            com.example.audio.AudioTrackMode.BATTLE_NORMAL,
            com.example.audio.AudioTrackMode.BATTLE_BOSS
        )
        for (mode in loopingModes) {
            synth.reset()
            var peak = 0
            var finished = false
            // Render ~18 seconds
            for (chunk in 0 until 390) {
                if (synth.renderChunk(mode, chunk * 2048L, buffer)) {
                    finished = true
                }
                for (sample in buffer) {
                    val absSample = kotlin.math.abs(sample.toInt())
                    if (absSample > peak) peak = absSample
                }
            }
            assertFalse("Looping theme $mode must never signal one-shot finished", finished)
            assertTrue("Looping theme $mode must produce audible output (peak=$peak)", peak > 3000)
            assertTrue("Looping theme $mode must avoid hard digital clipping (peak=$peak)", peak < 32700)
        }

        // Verify one-shot SFX produce audio and signal completion within ~3 seconds
        val oneShotModes = listOf(
            com.example.audio.AudioTrackMode.SFX_VICTORY,
            com.example.audio.AudioTrackMode.SFX_DEFEAT
        )
        for (sfx in oneShotModes) {
            synth.reset()
            var finished = false
            var peak = 0
            for (chunk in 0 until 70) {
                if (synth.renderChunk(sfx, chunk * 2048L, buffer)) {
                    finished = true
                }
                for (sample in buffer) {
                    val absSample = kotlin.math.abs(sample.toInt())
                    if (absSample > peak) peak = absSample
                }
            }
            assertTrue("One-shot SFX $sfx must report finished=true", finished)
            assertTrue("One-shot SFX $sfx must produce audible output (peak=$peak)", peak > 3000)
        }

        // Verify apk filename for 1.3.0
        assertEquals("plump_heroes_1.3.0.apk", com.example.network.AppUpdateChecker.buildApkFileName("1.3.0"))
    }
}
