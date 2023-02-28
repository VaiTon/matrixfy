package io.matrixfy

import io.matrixfy.BotDispatcher.Companion.dispatcher
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.fromStore
import net.folivo.trixnity.client.loginWith
import net.folivo.trixnity.client.media.InMemoryMediaStore
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
        getLoginInfo = { loginViaCLI(homeserverUrl, matrixUser, it) }
    ).getOrThrow()

    scope.launch { matrixClient.startSync() }

    matrixClient.dispatcher {
        registerLoginCmd()
        registerSearchCmd()
        registerPlayingCmd()
        registerSkipCmd()

        registerTrackPlay()
        registerSearch()
    }
    supervisorJob.join()
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
    println("Logging in...")

    val loginResponse = api.authentication.login(
        type = LoginType.Token,
        identifier = matrixUser,
        token = loginToken,
    ).getOrElse { return Result.failure(it) }

    val profile = api.users.getProfile(loginResponse.userId)
        .getOrElse { return Result.failure(it) }

    println("Logged in as ${profile.displayName}")

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