package io.matrixfy

import kotlinx.html.DIV
import kotlinx.html.div
import kotlinx.html.stream.createHTML
import net.folivo.trixnity.client.room.message.MessageBuilder
import net.folivo.trixnity.client.room.message.text
import net.folivo.trixnity.core.TrixnityDsl

@TrixnityDsl
inline fun MessageBuilder.html(
    crossinline block: DIV.() -> Unit
) {
    val body = createHTML().div(block = block)
    text(
        body = body,
        format = "org.matrix.custom.io.matrixfy.html",
        formattedBody = body
    )
}