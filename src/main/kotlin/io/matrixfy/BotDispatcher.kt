package io.matrixfy

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent

class BotDispatcher private constructor(
    val client: MatrixClient,
    private val commandPrefix: String,
) {

    private val commands = mutableMapOf<String, Command>()
    private val textListeners = mutableListOf<TextListener>()

    suspend fun listen() {
        val eventFlow = client.room.getTimelineEventsFromNowOn()

        // Start listening for events
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

        val split = messageContent.body.split(" ")
        val commandStr = split
            .firstOrNull()
            ?.removePrefixIfNotDirect(timelineEvent)
            ?: return

        val commandListener = commands[commandStr]?.commandListener
        if (commandListener != null) {
            timelineEvent.commandListener(messageContent, split.drop(1))
        } else {
            // Pass to after command listeners
            for (listener in textListeners) {
                if (listener(timelineEvent, messageContent))
                    break
            }
        }
    }

    private suspend fun String.removePrefixIfNotDirect(timelineEvent: TimelineEvent): String {
        val room = client.room.getById(timelineEvent.roomId).first()
        val isDirectChat = room?.isDirect ?: false

        return if (isDirectChat) this else removePrefix(commandPrefix)
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

    data class Command(
        val name: String,
        val transparent: Boolean = false,
        val commandListener: CommandListener,
    )


    companion object {
        suspend fun MatrixClient.dispatcher(
            commandPrefix: String = "!",
            block: BotDispatcher.() -> Unit,
        ) {
            val dispatcher = BotDispatcher(this, commandPrefix)
            dispatcher.block()
            dispatcher.listen()
        }
    }
}
typealias CommandListener = suspend TimelineEvent.(content: RoomMessageEventContent, args: List<String>) -> Unit
typealias TextListener = suspend TimelineEvent.(content: RoomMessageEventContent) -> Boolean