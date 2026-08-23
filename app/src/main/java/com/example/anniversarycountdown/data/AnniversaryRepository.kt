package com.example.anniversarycountdown.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.anniversarycountdown.widget.AnniversaryWidgetUpdater
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.anniversaryDataStore by preferencesDataStore(name = "anniversaries")

class AnniversaryRepository(private val context: Context) {
    private val dataStore = context.anniversaryDataStore
    private val serializer = ListSerializer(Anniversary.serializer())
    private val json = Json { ignoreUnknownKeys = true }

    val anniversaries: Flow<List<Anniversary>> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw error
        }
        .map { preferences -> decode(preferences[ANNIVERSARIES].orEmpty()) }

    suspend fun upsert(anniversary: Anniversary) {
        dataStore.edit { preferences ->
            val current = decode(preferences[ANNIVERSARIES].orEmpty())
            preferences[ANNIVERSARIES] = json.encodeToString(
                serializer,
                current.filterNot { it.id == anniversary.id } + anniversary,
            )
        }
        AnniversaryWidgetUpdater.updateAll(context)
    }

    suspend fun delete(id: String) {
        dataStore.edit { preferences ->
            val current = decode(preferences[ANNIVERSARIES].orEmpty())
            preferences[ANNIVERSARIES] = json.encodeToString(
                serializer,
                current.filterNot { it.id == id },
            )
        }
        AnniversaryWidgetUpdater.updateAll(context)
    }

    private fun decode(value: String): List<Anniversary> {
        if (value.isBlank()) return emptyList()
        return try {
            json.decodeFromString(serializer, value)
        } catch (_: SerializationException) {
            emptyList()
        } catch (_: IllegalArgumentException) {
            emptyList()
        }
    }

    private companion object {
        val ANNIVERSARIES = stringPreferencesKey("anniversary_list")
    }
}
