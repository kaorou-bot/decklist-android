package com.mtgo.decklistmanager.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 语言设置管理器
 * 默认使用简体中文显示卡牌信息
 */
@Singleton
class LanguagePreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val LANGUAGE_CHINESE = "zh"
        const val LANGUAGE_ENGLISH = "en"
    }

    /**
     * 获取当前卡牌语言设置
     * 固定返回简体中文
     */
    fun getCardLanguage(): String {
        return LANGUAGE_CHINESE
    }

    /**
     * 是否使用中文
     * 固定返回 true
     */
    fun isChinese(): Boolean {
        return true
    }
}
