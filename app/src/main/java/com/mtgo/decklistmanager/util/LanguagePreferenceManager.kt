package com.mtgo.decklistmanager.util

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 语言设置管理器
 * 用于控制卡牌信息的中英文显示
 */
@Singleton
class LanguagePreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("language_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CARD_LANGUAGE = "card_language"
        const val LANGUAGE_CHINESE = "zh"
        const val LANGUAGE_ENGLISH = "en"
    }

    /**
     * 获取当前卡牌语言设置
     */
    fun getCardLanguage(): String {
        return prefs.getString(KEY_CARD_LANGUAGE, LANGUAGE_CHINESE) ?: LANGUAGE_CHINESE
    }

    /**
     * 设置卡牌语言
     */
    fun setCardLanguage(language: String) {
        prefs.edit().putString(KEY_CARD_LANGUAGE, language).apply()
    }

    /**
     * 是否使用中文
     */
    fun isChinese(): Boolean {
        return getCardLanguage() == LANGUAGE_CHINESE
    }

    /**
     * 切换语言
     */
    fun toggleLanguage(): String {
        val current = getCardLanguage()
        val newLanguage = if (current == LANGUAGE_CHINESE) {
            LANGUAGE_ENGLISH
        } else {
            LANGUAGE_CHINESE
        }
        setCardLanguage(newLanguage)
        return newLanguage
    }
}
