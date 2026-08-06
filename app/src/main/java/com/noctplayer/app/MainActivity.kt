package com.noctplayer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.noctplayer.app.data.local.prefs.SafFolderPrefs
import com.noctplayer.app.data.repository.MediaRepository
import com.noctplayer.app.data.scanner.MediaStoreScanner
import com.noctplayer.app.data.scanner.SafFolderScanner
import com.noctplayer.app.ui.navigation.NoctNavGraph
import com.noctplayer.app.ui.theme.NoctPlayerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as NoctPlayerApp
        val database = app.database
        val repository = MediaRepository(
            mediaStoreScanner = MediaStoreScanner(this),
            safFolderScanner = SafFolderScanner(this),
            safFolderPrefs = SafFolderPrefs(this),
            mediaDao = database.mediaDao(),
            watchProgressDao = database.watchProgressDao(),
            favoriteDao = database.favoriteDao()
        )

        setContent {
            NoctPlayerTheme {
                NoctNavGraph(repository = repository)
            }
        }
    }
}
