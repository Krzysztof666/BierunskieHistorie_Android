package pl.bierun.historie.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import pl.bierun.historie.model.PoiImage

class ArchiveRepository(private val context: Context, private val languageManager: AppLanguageManager) {
    private val gson = Gson()

    fun getArchiveImages(): List<PoiImage> {
        val currentLang = languageManager.getCurrentLanguage()
        val jsonString = try {
            context.assets.open("$currentLang/archives.json").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            try {
                context.assets.open("pl/archives.json").bufferedReader().use { it.readText() }
            } catch (e2: Exception) {
                return emptyList()
            }
        }
        val listType = object : TypeToken<List<PoiImage>>() {}.type
        return gson.fromJson(jsonString, listType) ?: emptyList()
    }
}
