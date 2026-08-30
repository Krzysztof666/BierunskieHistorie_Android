package pl.bierun.historie.data

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import pl.bierun.historie.model.PoiItem

class PoiRepository(private val context: Context, val languageManager: AppLanguageManager) {
    private val gson = Gson()

    fun getPoisForCurrentLanguage(): List<PoiItem> {
        val currentLang = languageManager.getCurrentLanguage()
        val jsonString = try {
            context.assets.open("$currentLang/pois.json").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            android.util.Log.e("PoiRepository", "Error loading pois.json for $currentLang", e)
            try {
                context.assets.open("pl/pois.json").bufferedReader().use { it.readText() }
            } catch (e2: Exception) {
                android.util.Log.e("PoiRepository", "Error loading fallback pl/pois.json", e2)
                return emptyList()
            }
        }
        
        return try {
            val listType = object : TypeToken<List<PoiItem>>() {}.type
            val list: List<PoiItem>? = gson.fromJson(jsonString, listType)
            list?.map { it.copy(
                description = it.description ?: "",
                title = it.title ?: "Bez nazwy",
                images = it.images ?: emptyList(),
                audioFiles = it.audioFiles ?: emptyList()
            ) } ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.e("PoiRepository", "Error parsing JSON", e)
            emptyList()
        }
    }

    private val REMOTE_AUDIO_BASE_URL = "https://pyblog.cba.pl/app/bierunguide/audio/"

    fun getAudioUri(audioFileName: String): Uri {
        val currentLang = languageManager.getCurrentLanguage()
        val url = "$REMOTE_AUDIO_BASE_URL$currentLang/$audioFileName"
        return Uri.parse(url)
    }

    fun getImageUri(imageFileName: String): String {
        return "file:///android_asset/images/$imageFileName"
    }
}