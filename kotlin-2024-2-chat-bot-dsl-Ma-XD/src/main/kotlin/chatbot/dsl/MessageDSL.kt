package chatbot.dsl

import chatbot.api.*
import chatbot.bot.MessageProcessorContext

fun <C : ChatContext?> MessageProcessorContext<C>.sendMessage(chatId: ChatId, text: String) {
    client.sendMessage(chatId, text)
}

fun <C : ChatContext?> MessageProcessorContext<C>.sendMessage(chatId: ChatId, init: MessageBuilder.() -> Unit) {
    MessageBuilderImpl(message).apply {
        init()
        if (text.isNotBlank() || keyboard != null) {
            this@sendMessage.client.sendMessage(chatId, text, keyboard, replyTo)
        }
    }
}

@ChatBotBuilderDSL
interface MessageBuilder {
    val message: Message
    var text: String
    var replyTo: MessageId?
    fun removeKeyboard()
    fun withKeyboard(init: KeyBoardBuilder.() -> Unit)
}

class MessageBuilderImpl(override val message: Message) : MessageBuilder {
    override var text: String = ""
    override var replyTo: MessageId? = null
    var keyboard: Keyboard? = null

    override fun removeKeyboard() {
        keyboard = Keyboard.Remove
    }

    override fun withKeyboard(init: KeyBoardBuilder.() -> Unit) {
        keyboard = KeyBoardBuilderImpl().apply(init).build()
    }
}
