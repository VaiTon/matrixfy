package io.matrixfy

import com.adamratzman.spotify.models.Token
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.folivo.trixnity.core.model.UserId
import kotlin.io.path.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.system.exitProcess

@Serializable
data class User(
    val mxid: UserId,
    val spotifyToken: Token,
)

@Serializable
data class Settings(
    val spotifyClientId: String,
    val spotifyClientSecret: String,
    val spotifyRedirectUrl: String,
    val matrixUser: String,
    val matrixHomeserverUrl: String,
)

private val usersFile = Path("users.json")
private val configPath = Path("config.json")

object AppData {
    private lateinit var users: MutableMap<UserId, User>
    lateinit var settings: Settings
        private set


    fun getUser(mxid: UserId) = users[mxid]
    suspend fun addUser(user: User) = withContext(Dispatchers.IO) {
        users[user.mxid] = user
        saveUsers()
    }

    fun init() {
        loadSettings()
        loadUsers()
    }

    private fun loadUsers() {
        users = if (!usersFile.isRegularFile()) {
            mutableMapOf()
        } else {
            Json.decodeFromString(usersFile.readText())
        }
    }

    private fun saveUsers() {
        usersFile.writeText(Json.encodeToString(users))
    }

    private fun loadSettings() {
        if (!configPath.isRegularFile()) {
            System.err.println("Config file not found. Creating a new one...")
            configPath.toFile().writeText(
                Json.encodeToString(
                    Settings(
                        "",
                        "",
                        "",
                        "",
                        ""
                    )
                )
            )
            System.err.println("Please fill in the config file and restart the bot.")
            exitProcess(0)
        }

        val settings = Json.decodeFromString<Settings>(configPath.readText())
        AppData.settings = settings
    }

}