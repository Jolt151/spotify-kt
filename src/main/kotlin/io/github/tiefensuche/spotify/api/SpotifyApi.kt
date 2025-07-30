package io.github.tiefensuche.spotify.api

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.net.URL
import java.net.URLEncoder
import java.util.*
import javax.net.ssl.HttpsURLConnection

class SpotifyApi {

    var token: String = ""
    private val nextQueryUrls: HashMap<String, String> = HashMap()
    private var savedTrackIds: MutableSet<String> = HashSet()

    fun query(query: String, refresh: Boolean): List<Track> {
        val items = collectionRequest(String.format(QUERY_URL, URLEncoder.encode(query, "utf-8")), refresh, "tracks")
        return parseTracks(items)
    }

    fun getTrack(id: String): Track {
        val item = request(String.format(TRACK_URL, URLEncoder.encode(id, "utf-8")))
        return parseTrack(JSONObject(item.value))
    }

    fun getReleaseRadar(refresh: Boolean): List<Track> {
        val response = JSONObject(request(RELEASE_RADAR_URL).value)
        val id = response.getJSONObject("playlists").getJSONArray("items").getJSONObject(0).getString("id")
        val items = collectionRequest(String.format(PLAYLIST_URL, id), refresh)
        return parseTracks(items)
    }

    fun getUsersSavedTracks(refresh: Boolean): List<Track> {
        val items = collectionRequest(USERS_SAVED_TRACKS_URL, refresh)
        return parseTracks(items)
    }

    fun getArtists(refresh: Boolean): List<Playlist> {
        val items = collectionRequest(USERS_FOLLOWING, refresh, "artists")
        return parsePlaylists(items)
    }

    fun getArtist(id: String, refresh: Boolean): List<Track> {
        val items = collectionRequest(ARTIST_TRACKS.format(id), refresh, "tracks")
        return parseTracks(items)
    }

    fun getUsersPlaylists(refresh: Boolean): List<Playlist> {
        val items = collectionRequest(USERS_PLAYLISTS, refresh)
        return parsePlaylists(items)
    }

    fun getPlaylist(id: String, refresh: Boolean): List<Track> {
        val items = collectionRequest(String.format(PLAYLIST_URL, id), refresh)
        return parseTracks(items)
    }

    fun getUsersSavedAlbums(refresh: Boolean): List<Album> {
        val items = collectionRequest(USERS_SAVED_ALBUMS, refresh)
        return parseAlbums(items)
    }

    fun getUsersSavedShows(refresh: Boolean): List<Show> {
        val items = collectionRequest(USERS_SAVED_SHOWS, refresh)
        println(items)
        return parseShows(items)
    }

    fun getEpisodes(id: String, refresh: Boolean): List<Episode> {
        val items = collectionRequest(SHOW_EPISODES.format(id), refresh)
        println(items)
        return parseEpisodes(items)
    }

    fun getBrowseCategories(refresh: Boolean): List<Category> {
        val items = collectionRequest(BROWSE_CATEGORIES, refresh, "categories")
        return parseCategories(items)
    }

    fun getCategoryPlaylists(id: String, refresh: Boolean): List<Playlist> {
        val items = collectionRequest(CATEGORIES_PLAYLISTS.format(id), refresh, "playlists")
        return parsePlaylists(items)
    }

    private fun collectionRequest(url: String, refresh: Boolean, type: String? = null): JSONArray {
        val request = if (!refresh && url in nextQueryUrls) {
            if (nextQueryUrls[url] == "null") {
                return JSONArray()
            }
            nextQueryUrls[url].toString()
        } else {
            url
        }
        var response = JSONObject(request(request).value)

        if (type != null && response.get(type) is JSONObject) {
            response = response.getJSONObject(type)
        }
        nextQueryUrls[url] = if (!response.isNull("next")) response.getString("next") else "null"
        if (type != null && response.has(type))
            return response.getJSONArray(type)
        return response.getJSONArray("items")
    }

    private fun parseTracks(items: JSONArray): List<Track> {
        val result = mutableListOf<Track>()
        if (items.length() == 0) {
            return result
        }

        val savedStatus = getTracksSavedStatus(items)
        for (i in 0 until items.length()) {
            var item = items.getJSONObject(i)
            if (item.has("track")) {
                item = item.getJSONObject("track")
            }

            result.add(
                Track(item.getString("uri"),
                item.getJSONArray("artists").getJSONObject(0).getString("name"),
                item.getString("name"),
                if (item.has("album")) item.getJSONObject("album").getString("name") else "",
                item.getLong("duration_ms"),
                if (item.has("album")) item.getJSONObject("album").getJSONArray("images").getJSONObject(0).getString("url") else "",
                item.getString("uri"),
                savedStatus.getBoolean(i))
            )

            if (savedStatus.getBoolean(i)) {
                savedTrackIds.add(item.getString("uri"))
            }
        }
        return result
    }

    private fun parseTrack(item: JSONObject): Track {
        return Track(
            id = item.getString("uri"),
            artist = item.getJSONArray("artists").getJSONObject(0).getString("name"),
            title = item.getString("name"),
            album = if (item.has("album")) item.getJSONObject("album").getString("name") else "",
            duration = item.getLong("duration_ms"),
            artwork = if (item.has("album") &&
                item.getJSONObject("album").has("images") &&
                !item.getJSONObject("album").getJSONArray("images").isEmpty
            ) {
                item.getJSONObject("album").getJSONArray("images").getJSONObject(0).getString("url")
            } else "",
            url = item.getString("uri"),
            liked = false
        )
    }

    private fun parsePlaylists(items: JSONArray): List<Playlist> {
        val result = mutableListOf<Playlist>()
        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            result.add(
                Playlist(
                    item.getString("id"),
                    item.getString("name"),
                    if (item.isNull("images") || item.getJSONArray("images").length() == 0) ""
                    else item.getJSONArray("images").getJSONObject(0).getString("url")
                )
            )
        }
        return result
    }

    private fun parseAlbums(items: JSONArray): List<Album> {
        val result = mutableListOf<Album>()
        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i).getJSONObject("album")
            result.add(
                Album(
                    item.getString("id"),
                    item.getJSONArray("artists").getJSONObject(0).getString("name"),
                    item.getString("name"),
                    item.getJSONArray("images").getJSONObject(0).getString("url"),
                    item.getString("uri"),
                    parseTracks(item.getJSONObject("tracks").getJSONArray("items"))
                )
            )
        }
        return result
    }

    private fun parseShows(items: JSONArray): List<Show> {
        val result = mutableListOf<Show>()
        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i).getJSONObject("show")
            result.add(
                Show(
                    item.getString("id"),
                    item.getString("name"),
                    item.getString("description"),
                    item.getJSONArray("images").getJSONObject(0).getString("url"),
                    item.getString("uri")
                )
            )
        }
        return result
    }

    private fun parseEpisodes(items: JSONArray): List<Episode> {
        val result = mutableListOf<Episode>()
        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            result.add(
                Episode(
                    item.getString("id"),
                    item.getString("name"),
                    item.getString("description"),
                    item.getLong("duration_ms"),
                    item.getJSONArray("images").getJSONObject(0).getString("url"),
                    item.getString("uri")
                )
            )
        }
        return result
    }

    private fun parseCategories(items: JSONArray): List<Category> {
        val result = mutableListOf<Category>()
        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            result.add(
                Category(
                    item.getString("id"),
                    item.getString("name"),
                    item.getJSONArray("icons").getJSONObject(0).getString("url"),
                )
            )
        }
        return result
    }

    private fun getTracksSavedStatus(items: JSONArray): JSONArray {
        val ids = mutableListOf<String>()
        for (i in 0 until items.length()) {
            var item = items.getJSONObject(i)
            if (item.has("track")) {
                item = item.getJSONObject("track")
            }
            ids.add(item.getString("id"))
        }
        return JSONArray(request("$USERS_SAVED_TRACKS_URL/contains?ids=${ids.joinToString(",")}").value)
    }

    fun saveTrack(uri: String): Boolean {
        return try {
            val exists = savedTrackIds.contains(uri)
            request(
                "$USERS_SAVED_TRACKS_URL?ids=${uri.substringAfterLast(':')}",
                if (exists) "DELETE" else "PUT"
            )
            if (exists) {
                savedTrackIds.remove(uri)
            } else {
                savedTrackIds.add(uri)
            }
            true
        } catch (e: HttpException) {
            false
        }
    }

    class Response(val status: Int, val value: String)

    private fun request(url: String, method: String = "GET"): Response {
        val con = URL(url).openConnection() as? HttpsURLConnection ?: throw IOException()
        con.requestMethod = method
        con.setRequestProperty("Authorization", token)
        if (con.responseCode < 400) {
            return Response(con.responseCode, con.inputStream.bufferedReader().use(BufferedReader::readText))
        } else {
            throw HttpException(con.responseCode, con.errorStream.bufferedReader().use(BufferedReader::readText))
        }
    }

    class HttpException(val code: Int, message: String?) : IOException(message)

    companion object {
        private const val BASE_URL = "https://api.spotify.com/v1"
        private const val QUERY_URL = "$BASE_URL/search?q=%s&type=track"
        private const val TRACK_URL = "$BASE_URL/tracks/%s"
        private const val USERS_SAVED_TRACKS_URL = "$BASE_URL/me/tracks"
        private const val USERS_FOLLOWING = "$BASE_URL/me/following?type=artist"
        private const val ARTIST_TRACKS = "$BASE_URL/artists/%s/top-tracks?market=US"
        private const val USERS_PLAYLISTS = "$BASE_URL/me/playlists"
        private const val USERS_SAVED_ALBUMS = "$BASE_URL/me/albums"
        private const val USERS_SAVED_SHOWS = "$BASE_URL/me/shows"
        private const val SHOW_EPISODES = "$BASE_URL/shows/%s/episodes"
        private const val BROWSE_CATEGORIES = "$BASE_URL/browse/categories?limit=50"
        private const val CATEGORIES_PLAYLISTS = "$BASE_URL/browse/categories/%s/playlists"
        private const val RELEASE_RADAR_URL = "$BASE_URL/search?q=Release-Radar&type=playlist&limit=1"
        private const val PLAYLIST_URL = "$BASE_URL/playlists/%s/tracks?limit=50"
    }
}