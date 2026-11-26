package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.example.i18n.AppLanguage
import com.example.i18n.GameLocaleState
import com.example.model.PlayerProfile
import com.example.ui.screens.SummonScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
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
}
