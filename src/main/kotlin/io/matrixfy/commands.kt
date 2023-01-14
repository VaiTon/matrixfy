package io.matrixfy

import com.adamratzman.spotify.SpotifyScope
import com.adamratzman.spotify.SpotifyUserAuthorization
import com.adamratzman.spotify.getSpotifyAuthorizationUrl
import com.adamratzman.spotify.spotifyClientApi
import kotlinx.html.a
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.message.reply
import net.folivo.trixnity.client.room.message.text

internal fun BotCommandDispatcher.registerPlayingCmd() {
    registerCommand("playing") { _, _ ->
        sendNowPlaying(this, client)
    }
}

internal fun BotCommandDispatcher.registerSearchCmd() {
    registerCommand("search") { content, _ ->
        searchSong(this, client, content.body)
    }
}

internal fun BotCommandDispatcher.registerLoginCmd() {
    registerCommand("login") command@{ content, body ->
        val senderId = event.sender

        val clientId = AppData.settings.spotifyClientId
        val clientSecret = AppData.settings.spotifyClientSecret
        val redirectUrl = AppData.settings.spotifyRedirectUrl

        if (body.isEmpty()) {
            val authorizationUrl = getSpotifyAuthorizationUrl(
                clientId = clientId,
                redirectUri = redirectUrl,
                scopes = SpotifyScope.values(),
            )
            client.room.sendMessage(event.roomId) {
                reply(this@command)
                html {
                    a(authorizationUrl)
                }
            }
            return@command
        }

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
                text("OK")
            }
        } else {
            client.room.sendMessage(event.roomId) {
                reply(this@command)
                text("Login failed")
            }
        }
    }
}