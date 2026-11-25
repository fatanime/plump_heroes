package com.example.i18n

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppLanguage(val code: String) {
    RU("ru"),
    EN("en");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            if (code.equals("en", ignoreCase = true)) EN else RU
    }
}

object GameLocaleState {
    private const val PREFS_NAME = "plump_heroes_prefs"
    private const val KEY_APP_LANGUAGE = "app_language"
    private const val KEY_LANGUAGE_SELECTED = "language_selected"

    var language by mutableStateOf(AppLanguage.RU)
        private set

    var hasChosenLanguage by mutableStateOf(false)
        private set

    private val _languageFlow = MutableStateFlow(AppLanguage.RU)
    val languageFlow: StateFlow<AppLanguage> = _languageFlow.asStateFlow()

    val isEnglish: Boolean
        get() = language == AppLanguage.EN

    fun init(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_LANGUAGE_SELECTED)) {
            val isExistingPlayer = prefs.getBoolean("tutorial_completed", false) ||
                prefs.getBoolean("story_read", false) ||
                prefs.getInt("profile_level", 1) > 1
            if (isExistingPlayer) {
                language = AppLanguage.RU
                hasChosenLanguage = true
                _languageFlow.value = AppLanguage.RU
                prefs.edit()
                    .putString(KEY_APP_LANGUAGE, AppLanguage.RU.code)
                    .putBoolean(KEY_LANGUAGE_SELECTED, true)
                    .commit()
                return
            }
        }
        val savedCode = prefs.getString(KEY_APP_LANGUAGE, AppLanguage.RU.code)
        val resolved = AppLanguage.fromCode(savedCode)
        language = resolved
        hasChosenLanguage = prefs.getBoolean(KEY_LANGUAGE_SELECTED, false)
        _languageFlow.value = resolved
    }

    fun setLanguage(context: Context?, lang: AppLanguage, markChosen: Boolean = true) {
        language = lang
        _languageFlow.value = lang
        if (markChosen) {
            hasChosenLanguage = true
        }
        if (context != null) {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString(KEY_APP_LANGUAGE, lang.code)
                if (markChosen) {
                    putBoolean(KEY_LANGUAGE_SELECTED, true)
                }
                commit()
            }
        }
    }

    @VisibleForTesting
    fun resetForTest(lang: AppLanguage = AppLanguage.RU, chosen: Boolean = false) {
        language = lang
        hasChosenLanguage = chosen
        _languageFlow.value = lang
    }
}

inline fun tr(ru: String, en: String): String =
    if (GameLocaleState.isEnglish) en else ru
