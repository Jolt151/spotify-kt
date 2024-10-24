import io.github.tiefensuche.spotify.api.Album
import io.github.tiefensuche.spotify.api.Show
import io.github.tiefensuche.spotify.api.SpotifyApi
import io.github.tiefensuche.spotify.api.Track
import kotlin.test.Test

class Test {
    private val api: SpotifyApi = SpotifyApi()

    init {
        api.token = "<INSERT-TOKEN-HERE>"
    }

    private fun printTracks(tracks: List<Track>) {
        tracks.forEach { println("id: ${it.id}, artist: ${it.artist}, title: ${it.title}, album: ${it.album}, duration: ${it.duration}, artwork: ${it.artwork}, url: ${it.url}, liked: ${it.liked}") }
    }

    private fun printAlbums(tracks: List<Album>) {
        tracks.forEach {
            println("id: ${it.id}, artist: ${it.artist}, title: ${it.title}, artwork: ${it.artwork}, tracks: ${it.tracks.size}")
            printTracks(it.tracks)
        }
    }

    private fun printShows(tracks: List<Show>) {
        tracks.forEach { println("id: ${it.id}, title: ${it.title}, artwork: ${it.artwork}, uri: ${it.uri}") }
    }

    @Test
    fun testGetUsersSavedTracks() {
        val tracks = api.getUsersSavedTracks(false)
        printTracks(tracks)
    }

    @Test
    fun testGetAlbums() {
        val albums = api.getUsersSavedAlbums(false)
        printAlbums(albums)
    }

    @Test
    fun getShows() {
        val shows = api.getUsersSavedShows(false)
        printShows(shows)
        val episodes = api.getEpisodes(shows[0].id, false)
        println(episodes)
    }

    @Test
    fun getBrowseCategories() {
        val categories = api.getBrowseCategories(false)
        println(categories)
        val playlists = api.getCategoryPlaylists(categories[0].id, false)
        println(playlists)
        val tracks = api.getPlaylist(playlists[0].id, false);
        printTracks(tracks)
    }
}