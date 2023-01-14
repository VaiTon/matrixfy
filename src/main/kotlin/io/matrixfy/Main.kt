package io.matrixfy

import io.matrixfy.BotCommandDispatcher.Companion.dispatcher
import com.adamratzman.spotify.*
import com.adamratzman.spotify.endpoints.pub.SearchApi
import com.adamratzman.spotify.models.SpotifyTrackUri
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.html.*
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.fromStore
import net.folivo.trixnity.client.loginWith
import net.folivo.trixnity.client.media.InMemoryMediaStore
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.message.reply
import net.folivo.trixnity.client.room.message.text
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.repository.realm.createRealmRepositoriesModule
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClientImpl
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType
import net.folivo.trixnity.clientserverapi.model.authentication.LoginType
import kotlin.io.path.Path
import kotlin.io.path.createFile
import kotlin.io.path.exists

private val supervisorJob = SupervisorJob()
private val scope = CoroutineScope(supervisorJob)

private const val DB_FILE = "matrixfy.db"

suspend fun main() {
    AppData.init()

    val matrixUser = IdentifierType.User(AppData.settings.matrixUser)
    val homeserverUrl = Url(AppData.settings.matrixHomeserverUrl)

    val db = Path(DB_FILE)
    if (!db.exists())
        db.createFile()

    val repositoriesModule = createRealmRepositoriesModule()
    val mediaStore = InMemoryMediaStore()

    val matrixClient = MatrixClient.fromStore(
        repositoriesModule,
        mediaStore,
        scope = scope
    ).getOrThrow() ?: MatrixClient.loginWith(
        baseUrl = homeserverUrl,
        scope = scope,
        repositoriesModule = repositoriesModule,
        mediaStore = mediaStore,
        getLoginInfo = { loginViaCLI(homeserverUrl, matrixUser, it ) }
    ).getOrThrow()

    scope.launch { matrixClient.startSync() }

    matrixClient.dispatcher {
        registerLoginCmd()
        registerSearchCmd()
        registerPlayingCmd()

        registerCommand("skip") { _, _ ->
            val spotify = getSpotifyClient(client) ?: return@registerCommand

            spotify.player.skipForward()
            matrixClient.room.sendMessage(event.roomId) {
                html { +"Skipped" }
            }
        }

        registerDefaultTextListener()

        registerAfterCommandListener { roomMessageEventContent ->
            val isDirectChat = client.room.getById(roomId)
                .first()?.isDirect == true

            if (isDirectChat) {
                searchSong(this, matrixClient, roomMessageEventContent.body)
            }
        }
    }
    supervisorJob.join()
}

private fun BotCommandDispatcher.registerDefaultTextListener() {
    registerTextListener { content ->
        val spotifyClient = getSpotifyClient(client, this) ?: return@registerTextListener false
        val input = content.body

        if (input.startsWith("spotify:track")) {
            val uri = SpotifyTrackUri(input)

            val track = spotifyClient.tracks.getTrack(uri.id)!!
            spotifyClient.player.startPlayback(trackIdsToPlay = listOf(track.id))

            sendNowPlaying(this, client)
            return@registerTextListener true
        }

        false
    }
}


suspend fun sendNowPlaying(event: TimelineEvent, client: MatrixClient) {
    val spotifyClient = getSpotifyClient(client, event) ?: return

    val track =
        spotifyClient.player.getCurrentlyPlaying()?.item?.asTrack ?: return

    client.room.sendMessage(roomId = event.roomId) {
        reply(event)
        html {
            +"Now playing"
            h2 { +track.name }
            p { i { +track.artists.joinToString { it.name } } }
        }
    }
}

/**
 * @param query the search query
 * @param client the matrix client
 * @param event the event that prompted the search
 */
suspend fun searchSong(
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

private suspend fun getSpotifyClient(
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

private suspend fun loginViaCLI(
    homeserverUrl: Url,
    matrixUser: IdentifierType.User,
    api: MatrixClientServerApiClientImpl,
): Result<MatrixClient.LoginInfo> {
    print("Login via this url: ")
    println("${homeserverUrl}_matrix/client/v3/login/sso/redirect?redirectUrl=http://localhost")

    print("Now input the url: ")
    val ssoResponse = readln()

    val loginToken = ssoResponse.substringAfter("loginToken=").substringBefore("&")

    val loginResponse = api.authentication.login(
        type = LoginType.Token,
        identifier = matrixUser,
        token = loginToken,
    ).getOrThrow()

    val profile = api.users.getProfile(loginResponse.userId).getOrThrow()

    return Result.success(
        MatrixClient.LoginInfo(
            userId = loginResponse.userId,
            accessToken = loginResponse.accessToken,
            deviceId = loginResponse.deviceId,
            displayName = profile.displayName,
            avatarUrl = profile.avatarUrl
        )
    )
}