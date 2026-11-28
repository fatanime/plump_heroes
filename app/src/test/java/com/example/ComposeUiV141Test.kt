package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.example.i18n.AppLanguage
import com.example.i18n.GameLocaleState
import com.example.model.BattleCard
import com.example.model.BattlePhase
import com.example.model.BattleState
import com.example.model.CardRarity
import com.example.model.Element
import com.example.model.HeroSkill
import com.example.model.HeroType
import com.example.model.PlayerProfile
import com.example.model.SkillType
import com.example.ui.screens.BattleScreen
import com.example.ui.screens.SummonScreen
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class ComposeUiV141Test {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `SummonScreen shows Mystery Casket in RU only when locked secret heroes exist`() {
        try {
            GameLocaleState.resetForTest(AppLanguage.RU, chosen = true)
            var hasLocked by mutableStateOf(true)
            composeTestRule.setContent {
                SummonScreen(
                    profile = PlayerProfile(name = "UiTest", level = 10, gems = 500, isTutorialCompleted = true),
                    hasLockedSecretHeroes = hasLocked,
                    onOpenChest = {}
                )
            }
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("open_mystery_chest").assertExists()
            composeTestRule.onNodeWithText("Шкатулка редкостей").assertExists()

            // When all secret heroines are unlocked, Mystery Casket disappears completely
            hasLocked = false
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("open_mystery_chest").assertDoesNotExist()
        } finally {
            GameLocaleState.resetForTest(AppLanguage.RU, chosen = false)
        }
    }

    @Test
    fun `SummonScreen shows Mystery Casket in EN when locked secret heroes exist`() {
        try {
            GameLocaleState.resetForTest(AppLanguage.EN, chosen = true)
            composeTestRule.setContent {
                SummonScreen(
                    profile = PlayerProfile(name = "UiTest", level = 10, gems = 500, isTutorialCompleted = true),
                    hasLockedSecretHeroes = true,
                    onOpenChest = {}
                )
            }
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("open_mystery_chest").assertExists()
            composeTestRule.onNodeWithText("Mystery Casket").assertExists()
        } finally {
            GameLocaleState.resetForTest(AppLanguage.RU, chosen = false)
        }
    }

    @Test
    fun `BattleScreen shows ally targeting prompt and allows tapping frozen ally when Leya skill is pending`() {
        try {
            GameLocaleState.resetForTest(AppLanguage.RU, chosen = true)
            var tappedSlot: Int? = null
            val leya = BattleCard(
                heroId = "leya",
                name = "Лея",
                title = "Лесная дикарка",
                heroType = HeroType.HYBRID,
                rarity = CardRarity.SPECIAL,
                element = Element.LIGHT,
                imageRes = R.drawable.hero_leya_1,
                slotIndex = 0,
                isPlayer = true,
                level = 5,
                maxHp = 100,
                currentHp = 100,
                maxArmor = 0,
                currentArmor = 0,
                atk = 20,
                skill = HeroSkill("leya_skill_2", "Очищение", "", energyCost = 3, skillType = SkillType.SUPPORT, unlockLevel = 5)
            )
            val frozenAlly = BattleCard(
                heroId = "olga",
                name = "Ольга",
                title = "",
                heroType = HeroType.MELEE,
                rarity = CardRarity.BASIC,
                element = Element.STEEL,
                imageRes = R.drawable.hero_olga,
                slotIndex = 1,
                isPlayer = true,
                level = 5,
                maxHp = 100,
                currentHp = 80,
                maxArmor = 0,
                currentArmor = 0,
                atk = 15,
                isFrozen = true,
                freezeRounds = 1
            )
            val enemy = BattleCard(
                heroId = "witch",
                name = "Ведьма",
                title = "",
                heroType = HeroType.RANGED,
                rarity = CardRarity.BASIC,
                element = Element.BLIGHT,
                imageRes = R.drawable.hero_witch,
                slotIndex = 0,
                isPlayer = false,
                level = 5,
                maxHp = 100,
                currentHp = 100,
                maxArmor = 0,
                currentArmor = 0,
                atk = 10
            )
            val state = BattleState(
                playerCards = listOf(leya, frozenAlly),
                enemyCards = listOf(enemy),
                enemyName = "Тест",
                enemyTitle = "Арена",
                selectedPlayerSlot = 0,
                pendingSkillSlot = 0,
                playerTeamMana = 3,
                phase = BattlePhase.PLAYER_TURN
            )

            composeTestRule.setContent {
                BattleScreen(
                    battleState = state,
                    canCardTargetSlot = { _, _, _ -> true },
                    onSelectPlayerSlot = { tappedSlot = it },
                    onAttackTarget = {},
                    onCastSkill = {},
                    onToggleAuto = {},
                    onSetSpeed = {},
                    onExitBattle = {}
                )
            }
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("Выберите союзницу 🌿").assertExists()
            composeTestRule.onNodeWithText("Отмена").assertExists()
            composeTestRule.onNodeWithTag("player_slot_1").performClick()
            composeTestRule.waitForIdle()
            assertEquals(1, tappedSlot)
        } finally {
            GameLocaleState.resetForTest(AppLanguage.RU, chosen = false)
        }
    }
}
