package com.noctplayer.app.ui.player

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer

data class TrackOption(
    val groupIndex: Int,
    val trackIndexInGroup: Int,
    val label: String,
    val language: String?,
    val isSelected: Boolean,
    val format: Format
)

/** Reads currently available audio tracks from the player's active Tracks. */
fun ExoPlayer.listAudioTracks(): List<TrackOption> = listTracksOfType(C.TRACK_TYPE_AUDIO)

/** Reads currently available text (subtitle) tracks from the player's active Tracks. */
fun ExoPlayer.listSubtitleTracks(): List<TrackOption> = listTracksOfType(C.TRACK_TYPE_TEXT)

private fun ExoPlayer.listTracksOfType(type: Int): List<TrackOption> {
    val tracks: Tracks = currentTracks
    val options = mutableListOf<TrackOption>()
    tracks.groups.forEachIndexed { groupIndex, group ->
        if (group.type != type) return@forEachIndexed
        for (i in 0 until group.length) {
            val format = group.getTrackFormat(i)
            val label = format.label
                ?: format.language?.uppercase()
                ?: "Track ${groupIndex + 1}.${i + 1}"
            options += TrackOption(
                groupIndex = groupIndex,
                trackIndexInGroup = i,
                label = label,
                language = format.language,
                isSelected = group.isTrackSelected(i),
                format = format
            )
        }
    }
    return options
}

/** Selects a single audio track by overriding track selection parameters. */
fun ExoPlayer.selectAudioTrack(option: TrackOption) {
    val group = currentTracks.groups.getOrNull(option.groupIndex) ?: return
    trackSelectionParameters = trackSelectionParameters.buildUpon()
        .setOverrideForType(
            TrackSelectionOverride(group.mediaTrackGroup, option.trackIndexInGroup)
        )
        .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
        .build()
}

/** Selects a single subtitle track. Pass null to disable subtitles entirely. */
fun ExoPlayer.selectSubtitleTrack(option: TrackOption?) {
    if (option == null) {
        trackSelectionParameters = trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()
        return
    }
    val group = currentTracks.groups.getOrNull(option.groupIndex) ?: return
    trackSelectionParameters = trackSelectionParameters.buildUpon()
        .setOverrideForType(
            TrackSelectionOverride(group.mediaTrackGroup, option.trackIndexInGroup)
        )
        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
        .build()
}

/**
 * Rebuilds the current media item with an additional external subtitle track
 * (SRT/ASS/SSA/VTT picked via SAF), preserving playback position.
 */
fun ExoPlayer.attachExternalSubtitle(subtitleUri: android.net.Uri, displayName: String, mimeType: String) {
    val current = currentMediaItem ?: return
    val resumePosition = currentPosition
    val wasPlaying = isPlaying

    val newSubtitle = MediaItem.SubtitleConfiguration.Builder(subtitleUri)
        .setMimeType(mimeType)
        .setLanguage("ext")
        .setLabel(displayName)
        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
        .build()

    val existingSubtitles = current.localConfiguration?.subtitleConfigurations ?: emptyList()

    val rebuilt = current.buildUpon()
        .setSubtitleConfigurations(existingSubtitles + newSubtitle)
        .build()

    setMediaItem(rebuilt, resumePosition)
    prepare()
    playWhenReady = wasPlaying
}
