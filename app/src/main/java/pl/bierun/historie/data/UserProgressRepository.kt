package pl.bierun.historie.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_progress")

class UserProgressRepository(private val context: Context) {
    companion object {
        private val KEY_VISITED = stringSetPreferencesKey("visited_pois")
    }

    val visitedPoisFlow: Flow<Set<String>> = context.dataStore.data.map { it[KEY_VISITED] ?: emptySet() }

    suspend fun markPoiAsVisited(id: String) {
        context.dataStore.edit { it[KEY_VISITED] = (it[KEY_VISITED] ?: emptySet()) + id }
    }
}