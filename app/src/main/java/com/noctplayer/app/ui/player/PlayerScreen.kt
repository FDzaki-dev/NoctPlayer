package com.noctplayer.app.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.provider.Settings
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

private const val SEEK_STEP_MS = 10_000L
private val SPEED_OPTIONS = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    mediaStoreId: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val view = LocalView.current
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(mediaStoreId) { viewModel.load(mediaStoreId) }

    // Force landscape + immersive fullscreen while this screen is active.
    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val controller = window?.let { androidx.core.view.WindowCompat.getInsetsController(it, view) }
        controller?.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            controller?.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
    }

    if (state.errorMessage != null) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color.White)
                Spacer(Modifier.height(8.dp))
                Text(state.errorMessage ?: "Playback error", color = Color.White)
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onBack) { Text("Go back") }
            }
        }
        return
    }

    if (!state.isReady) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(state.uri))
            seekTo(state.resumePositionMs)
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.savePosition(exoPlayer.currentPosition, exoPlayer.duration.coerceAtLeast(0))
            exoPlayer.release()
        }
    }

    // Periodically persist resume position so a crash mid-playback doesn't lose progress.
    LaunchedEffect(exoPlayer) {
        while (true) {
            delay(5000)
            if (exoPlayer.duration > 0) {
                viewModel.savePosition(exoPlayer.currentPosition, exoPlayer.duration)
            }
        }
    }

    var controlsVisible by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var speed by remember { mutableStateOf(1f) }
    var speedMenuOpen by remember { mutableStateOf(false) }
    var gestureHint by remember { mutableStateOf<String?>(null) }
    var zoom by remember { mutableStateOf(1f) }

    val audioManager = remember { context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC) }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    // Auto-hide controls after 3s of inactivity while playing.
    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(3000)
            controlsVisible = false
        }
    }
    LaunchedEffect(gestureHint) {
        if (gestureHint != null) {
            delay(700)
            gestureHint = null
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { controlsVisible = !controlsVisible },
                    onDoubleTap = { offset ->
                        val width = size.width
                        if (offset.x < width / 2f) {
                            exoPlayer.seekTo((exoPlayer.currentPosition - SEEK_STEP_MS).coerceAtLeast(0))
                            gestureHint = "-10s"
                        } else {
                            exoPlayer.seekTo(exoPlayer.currentPosition + SEEK_STEP_MS)
                            gestureHint = "+10s"
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoomChange, _ ->
                    zoom = (zoom * zoomChange).coerceIn(1f, 3f)
                }
            }
            .pointerInput(Unit) {
                // Vertical drag: left half = brightness, right half = volume
                detectVerticalDragGestures { change, dragAmount ->
                    val width = size.width
                    val isLeftSide = change.position.x < width / 2f
                    if (isLeftSide) {
                        val window = activity?.window
                        val current = window?.attributes?.screenBrightness.let {
                            if (it == null || it < 0f) 0.5f else it
                        }
                        val newValue = (current - dragAmount / 1000f).coerceIn(0.02f, 1f)
                        window?.attributes = window?.attributes?.apply { screenBrightness = newValue }
                        gestureHint = "Brightness ${(newValue * 100).roundToInt()}%"
                    } else {
                        val current = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
                        val delta = (-dragAmount / 1000f * maxVolume).roundToInt()
                        val newVol = (current + delta).coerceIn(0, maxVolume)
                        audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, newVol, 0)
                        gestureHint = "Volume ${(newVol * 100 / maxVolume)}%"
                    }
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    val seekDeltaMs = (dragAmount * 200).toLong()
                    exoPlayer.seekTo((exoPlayer.currentPosition + seekDeltaMs).coerceIn(0, exoPlayer.duration.coerceAtLeast(0)))
                }
            }
    ) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    player = exoPlayer
                    useController = false
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayerScale(zoom)
        )

        gestureHint?.let { hint ->
            Box(Modifier.align(Alignment.Center)) {
                Surface(color = Color.Black.copy(alpha = 0.6f), shape = MaterialTheme.shapes.medium) {
                    Text(hint, color = Color.White, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
            }
        }

        if (controlsVisible) {
            PlayerControlsOverlay(
                title = state.title,
                isPlaying = isPlaying,
                isFavorite = state.isFavorite,
                speed = speed,
                exoPlayer = exoPlayer,
                onBack = onBack,
                onPlayPause = {
                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                },
                onFavorite = viewModel::toggleFavorite,
                onSpeedClick = { speedMenuOpen = true }
            )
        }

        if (speedMenuOpen) {
            SpeedPickerDialog(
                current = speed,
                onDismiss = { speedMenuOpen = false },
                onSelect = {
                    speed = it
                    exoPlayer.playbackParameters = PlaybackParameters(it)
                    speedMenuOpen = false
                }
            )
        }
    }
}

private fun Modifier.graphicsLayerScale(scale: Float): Modifier = this.then(
    Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
)

@Composable
private fun PlayerControlsOverlay(
    title: String,
    isPlaying: Boolean,
    isFavorite: Boolean,
    speed: Float,
    exoPlayer: ExoPlayer,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onFavorite: () -> Unit,
    onSpeedClick: () -> Unit
) {
    var position by remember { mutableStateOf(exoPlayer.currentPosition) }
    val duration = exoPlayer.duration.coerceAtLeast(1)

    LaunchedEffect(exoPlayer) {
        while (true) {
            position = exoPlayer.currentPosition
            delay(500)
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Top bar
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(title, color = Color.White, maxLines = 1, modifier = Modifier.weight(1f).padding(start = 4.dp))
            IconButton(onClick = onFavorite) {
                Icon(
                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = Color.White
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Bottom bar
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(12.dp)
        ) {
            Slider(
                value = position.toFloat().coerceIn(0f, duration.toFloat()),
                onValueChange = { exoPlayer.seekTo(it.toLong()) },
                valueRange = 0f..duration.toFloat()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatTime(position), color = Color.White, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onSpeedClick) {
                    Text("${speed}x", color = Color.White)
                }
                IconButton(onClick = onPlayPause) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White
                    )
                }
                Text(formatTime(duration), color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun SpeedPickerDialog(current: Float, onDismiss: () -> Unit, onSelect: (Float) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Playback speed") },
        text = {
            Column {
                SPEED_OPTIONS.forEach { option ->
                    TextButton(onClick = { onSelect(option) }) {
                        Text("${option}x" + if (option == current) "  ✓" else "")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%d:%02d", m, s)
}
