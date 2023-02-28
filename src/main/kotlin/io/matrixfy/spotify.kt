package io.matrixfy

import com.adamratzman.spotify.SpotifyClientApi
import com.adamratzman.spotify.spotifyClientApi
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.message.reply
import net.folivo.trixnity.client.room.message.text
import net.folivo.trixnity.client.store.TimelineEvent

internal suspend fun getSpotifyClient(
    client: MatrixClient,
    event: TimelineEvent? = null,
): SpotifyClientApi? {
    val senderId = event?.event?.sender ?: return null
    val senderUser = AppData.getUser(senderId)

    return if (senderUser == null) {
        client.room.sendMessage(roomId = event.roomId) {
            reply(event)
            text("You need to login to Spotify first.\n !login")
        }
        null
    } else {
        spotifyClientApi(
            AppData.settings.spotifyClientId,
            AppData.settings.spotifyClientSecret,
            AppData.settings.spotifyRedirectUrl,
            senderUser.spotifyToken
        ).build()
    }
}