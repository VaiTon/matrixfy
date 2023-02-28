package io.matrixfy

import kotlinx.html.DIV
import kotlinx.html.div
import kotlinx.html.stream.createHTML
import net.folivo.trixnity.client.room.message.MessageBuilder
import net.folivo.trixnity.client.room.message.text
import net.folivo.trixnity.core.TrixnityDsl

@TrixnityDsl
inline fun MessageBuilder.html(
    crossinline block: DIV.() -> Unit,
) {
    val body = createHTML().div(block = block)
        .removePrefix("<div>")
        .removeSuffix("</div>")
    text(
        body = body,
        format = "org.matrix.custom.html",
        formattedBody = body
    )
}