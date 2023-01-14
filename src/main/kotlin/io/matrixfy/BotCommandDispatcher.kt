package io.matrixfy

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent

class BotCommandDispatcher private constructor(
    val client: MatrixClient,
    private val commandPrefix: String,
) {

    private val commands = mutableMapOf<String, Command>()
    private val textListeners = mutableListOf<TextListener>()
    private val afterCommandListeners = mutableListOf<AfterCommandListener>()

    suspend fun listen() {
        val eventFlow = client.room.getTimelineEventsFromNowOn()

        coroutineScope {
            eventFlow.collect { dispatchEvent(it) }
        }
    }

    private suspend fun dispatchEvent(timelineEvent: TimelineEvent) {
        // Skip events not correctly decrypted or not messages
        val eventContent = timelineEvent.content?.getOrNull() ?: return
        val messageContent =
            eventContent as? RoomMessageEventContent.TextMessageEventContent ?: return

        // Skip events created by the bot
        if (timelineEvent.event.sender == client.userId) return

        // Pass to text listeners, skip if any of them returned true
        if (dispatchToTextListeners(timelineEvent, messageContent)) return


        val split = messageContent.body.split(" ")
        val commandStr = split
            .firstOrNull()
            ?.removePrefixIfDirect(timelineEvent)
            ?: return

        val commandListener = commands[commandStr]?.commandListener
        if (commandListener != null) {
            timelineEvent.commandListener(messageContent, split)
        } else {
            // Pass to after command listeners
            afterCommandListeners.forEach { it(timelineEvent, messageContent) }
        }
    }

    private suspend fun String.removePrefixIfDirect(timelineEvent: TimelineEvent): String {
        val isDirectChat = client.room.getById(timelineEvent.roomId)
            .firstOrNull()
            ?.isDirect == true

        return if (isDirectChat) removePrefix(commandPrefix) else this
    }

    private suspend fun dispatchToTextListeners(
        timelineEvent: TimelineEvent,
        messageContent: RoomMessageEventContent.TextMessageEventContent,
    ): Boolean {
        return textListeners.any { it(timelineEvent, messageContent) }
    }

    fun registerCommand(
        command: String,
        transparent: Boolean = false,
        listener: CommandListener,
    ): Command {
        val cmd = Command(command, transparent, listener)
        commands[command] = cmd
        return cmd
    }

    fun registerTextListener(action: TextListener) {
        textListeners += action
    }

    fun registerAfterCommandListener(acton: AfterCommandListener) {
        afterCommandListeners += acton
    }

    data class Command(
        val name: String,
        val transparent: Boolean = false,
        val commandListener: CommandListener,
    )


    companion object {
        suspend fun MatrixClient.dispatcher(
            commandPrefix: String = "!",
            block: BotCommandDispatcher.() -> Unit,
        ) {
            val dispatcher = BotCommandDispatcher(this, commandPrefix)
            dispatcher.block()
            dispatcher.listen()
        }


    }
}

typealias CommandListener = suspend TimelineEvent.(content: RoomMessageEventContent, args: List<String>) -> Unit
typealias AfterCommandListener = suspend TimelineEvent.(content: RoomMessageEventContent) -> Unit
typealias TextListener = suspend TimelineEvent.(content: RoomMessageEventContent) -> Boolean

