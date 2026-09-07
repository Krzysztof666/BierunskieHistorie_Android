package pl.bierun.historie.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import pl.bierun.historie.model.PoiImage

import pl.bierun.historie.model.ArchiveVideo

class ArchiveRepository(private val context: Context, private val languageManager: AppLanguageManager) {
    private val gson = Gson()

    fun getArchiveImages(): List<PoiImage> {
        return loadListFromJson("archives.json")
    }

    fun getArtistsVideos(): List<ArchiveVideo> {
        return loadListFromJson("artists.json")
    }

    fun getEventsVideos(): List<ArchiveVideo> {
        return loadListFromJson("events.json")
    }

    private inline fun <reified T> loadListFromJson(fileName: String): List<T> {
        val currentLang = languageManager.getCurrentLanguage()
        val jsonString = try {
            context.assets.open("$currentLang/$fileName").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            try {
                context.assets.open("pl/$fileName").bufferedReader().use { it.readText() }
            } catch (e2: Exception) {
                return emptyList()
            }
        }
        val listType = object : TypeToken<List<T>>() {}.type
        return gson.fromJson(jsonString, listType) ?: emptyList()
    }
}
