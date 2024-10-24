package io.github.tiefensuche.spotify.api

data class Track(
    val id: String,
    val artist: String,
    val title: String,
    val album: String,
    val duration: Long,
    val artwork: String,
    val url: String,
    val liked: Boolean
)

data class Album(
    val id: String,
    val artist: String,
    val title: String,
    val artwork: String,
    val uri: String,
    val tracks: List<Track>
)

data class Playlist(
    val id: String,
    val title: String,
    val artwork: String
)

data class Show(
    val id: String,
    val title: String,
    val description: String,
    val artwork: String,
    val uri: String,
)

data class Episode(
    val id: String,
    val title: String,
    val description: String,
    val duration: Long,
    val artwork: String,
    val uri: String
)

data class Category(
    val id: String,
    val name: String,
    val artwork: String,
)