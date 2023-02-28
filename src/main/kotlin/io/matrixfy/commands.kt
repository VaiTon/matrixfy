package io.matrixfy

import com.adamratzman.spotify.SpotifyScope
import com.adamratzman.spotify.SpotifyUserAuthorization
import com.adamratzman.spotify.endpoints.pub.SearchApi
import com.adamratzman.spotify.getSpotifyAuthorizationUrl
import com.adamratzman.spotify.models.SpotifyTrackUri
import com.adamratzman.spotify.spotifyClientApi
import kotlinx.coroutines.flow.first
import kotlinx.html.*
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.message.reply
import net.folivo.trixnity.client.room.message.text
import net.folivo.trixnity.client.store.TimelineEvent

internal fun BotDispatcher.registerPlayingCmd() {
    registerCommand("playing") { _, _ ->
        sendNowPlaying(this, client)
    }
}

internal fun BotDispatcher.registerSearchCmd() {
    registerCommand("search") { content, _ ->
        searchSong(this, client, content.body)
    }
}

internal fun BotDispatcher.registerLoginCmd() {
    registerCommand("login") command@{ content, args ->
        val senderId = event.sender

        val clientId = AppData.settings.spotifyClientId
        val clientSecret = AppData.settings.spotifyClientSecret
        val redirectUrl = AppData.settings.spotifyRedirectUrl

        if (args.isEmpty()) {
            val authorizationUrl = getSpotifyAuthorizationUrl(
                clientId = clientId,
                redirectUri = redirectUrl,
                scopes = SpotifyScope.values(),
            )
            client.room.sendMessage(event.roomId) {
                reply(this@command)
                text(authorizationUrl)
            }
            return@command
        } else {

            val authCode = content.body.substringAfter("code=").substringBefore("&").trim()
            val spotifyClientApi = spotifyClientApi(
                clientId,
                clientSecret,
                redirectUrl,
                SpotifyUserAuthorization(authCode)
            ).build()

            if (spotifyClientApi.isTokenValid().isValid) {
                AppData.addUser(User(senderId, spotifyClientApi.token))
                client.room.sendMessage(event.roomId) {
                    reply(this@command)
                    text("Logged in as ${spotifyClientApi.users.getClientProfile().displayName}")
                }
            } else {
                client.room.sendMessage(event.roomId) {
                    reply(this@command)
                    text("Login failed")
                }
            }
        }
    }
}

internal fun BotDispatcher.registerSearch() {
    registerTextListener { roomMessageEventContent ->
        val isDirectChat = client.room.getById(roomId).first()?.isDirect
        if (isDirectChat != true) {
            return@registerTextListener false
        }

        searchSong(this, client, roomMessageEventContent.body)
        return@registerTextListener true
    }
}

internal fun BotDispatcher.registerTrackPlay() {
    registerTextListener { content ->
        val spotifyClient = getSpotifyClient(client, this)
            ?: return@registerTextListener false

        val input = content.body
        if (!input.startsWith("spotify:track")) {
            return@registerTextListener false
        }

        val uri = SpotifyTrackUri(input)

        val track = spotifyClient.tracks.getTrack(uri.id)!!
        spotifyClient.player.startPlayback(trackIdsToPlay = listOf(track.id))

        sendNowPlaying(this, client)
        return@registerTextListener true
    }
}

internal suspend fun sendNowPlaying(event: TimelineEvent, client: MatrixClient) {
    val spotifyClient = getSpotifyClient(client, event) ?: return

    val track =
        spotifyClient.player.getCurrentlyPlaying()?.item?.asTrack ?: return

    client.room.sendMessage(roomId = event.roomId) {
        reply(event)
        html {
            +"Now playing "
            strong { +track.name }
            +" by "
            em { +track.artists.joinToString { it.name } }
        }
    }
}

/**
 * @param query the search query
 * @param client the matrix client
 * @param event the event that prompted the search
 */
internal suspend fun searchSong(
    event: TimelineEvent,
    client: MatrixClient,
    query: String,
) {
    val spotifyClient = getSpotifyClient(client, event) ?: return

    val searchResult = spotifyClient.search.search(query, SearchApi.SearchType.Track)
    val trackList = searchResult.tracks?.take(5) ?: return

    client.room.sendMessage(event.roomId) {
        reply(event)
        html {

            ul {
                trackList.forEach { track ->
                    li {
                        +"${track.name} by ${track.artists.joinToString { it.name }} - "
                        pre { code { +track.uri.uri } }
                    }
                }
            }
        }
    }
}

internal fun BotDispatcher.registerSkipCmd() {
    registerCommand("skip") { _, _ ->
        val spotify = getSpotifyClient(client) ?: return@registerCommand

        spotify.player.skipForward()
        client.room.sendMessage(event.roomId) {
            html { +"Skipped" }
        }
    }
}