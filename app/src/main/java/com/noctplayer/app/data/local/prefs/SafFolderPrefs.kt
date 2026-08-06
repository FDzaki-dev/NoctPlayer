package com.noctplayer.app.data.local.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.folderDataStore by preferencesDataStore(name = "saf_folders")

/** Persists which SAF folder trees the user has added for library scanning. */
class SafFolderPrefs(private val context: Context) {
    private val key = stringSetPreferencesKey("folder_uris")

    val folderUris: Flow<Set<String>> = context.folderDataStore.data.map { it[key] ?: emptySet() }

    suspend fun addFolder(uriString: String) {
        context.folderDataStore.edit { prefs ->
            prefs[key] = (prefs[key] ?: emptySet()) + uriString
        }
    }

    suspend fun removeFolder(uriString: String) {
        context.folderDataStore.edit { prefs ->
            prefs[key] = (prefs[key] ?: emptySet()) - uriString
        }
    }
}
