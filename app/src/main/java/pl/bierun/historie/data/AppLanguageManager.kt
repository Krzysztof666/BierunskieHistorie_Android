package pl.bierun.historie.data

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

class AppLanguageManager(context: Context) {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    fun initLanguageOnAppStart() {
        val isFirst = prefs.getBoolean("is_first", true)
        val lang = if (isFirst) {
            val detected = when (Locale.getDefault().language.lowercase()) {
                "pl" -> "pl"
                "de" -> "de"
                "en" -> "en"
                else -> "en"
            }
            prefs.edit().putString("lang", detected).putBoolean("is_first", false).apply()
            detected
        } else {
            prefs.getString("lang", "pl") ?: "pl"
        }
        applyLanguage(lang)
    }

    fun setUserSelectedLanguage(code: String) {
        prefs.edit().putString("lang", code).apply()
        applyLanguage(code)
    }

    private fun applyLanguage(code: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code))
    }

    fun getCurrentLanguage(): String = prefs.getString("lang", "pl") ?: "pl"
}