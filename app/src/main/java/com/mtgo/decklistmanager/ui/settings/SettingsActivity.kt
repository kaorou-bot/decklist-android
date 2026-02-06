package com.mtgo.decklistmanager.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.google.android.material.button.MaterialButton
import com.mtgo.decklistmanager.R
import com.mtgo.decklistmanager.util.AppLogger

/**
 * Settings Activity - 设置界面
 */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Setup back button
        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Load settings fragment
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
    }

    /**
     * Settings Fragment - 设置选项
     */
    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.settings_preferences, rootKey)

            // Theme preference
            val themePreference = findPreference<ListPreference>("theme_mode")
            themePreference?.setOnPreferenceChangeListener { _, newValue ->
                val mode = when (newValue as String) {
                    "light" -> AppCompatDelegate.MODE_NIGHT_NO
                    "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                AppCompatDelegate.setDefaultNightMode(mode)
                AppLogger.d("SettingsActivity", "Theme changed to: $newValue")
                true
            }
        }
    }
}
