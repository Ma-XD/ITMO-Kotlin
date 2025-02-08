package chatbot.dsl

import chatbot.api.ChatContext
import chatbot.api.Message
import chatbot.bot.MessageHandler
import chatbot.bot.MessageProcessor
import chatbot.utils.ChatContextUtils.switchContext

@ChatBotBuilderDSL
open class BehaviourBuilder<C : ChatContext?> {
    val messageHandlers: MutableList<MessageHandler<C>> = mutableListOf()

    fun onCommand(command: String, processor: MessageProcessor<C>) = addMessageHandler(
        predicate = { message, _ -> message.text.startsWith("/$command") },
        processor = processor,
    )

    fun onMessage(predicate: (Message) -> Boolean, processor: MessageProcessor<C>) = addMessageHandler(
        predicate = { message, _ -> predicate(message) },
        processor = processor,
    )

    fun onMessage(messageTextExactly: String? = null, processor: MessageProcessor<C>) = addMessageHandler(
        predicate = { message, _ -> messageTextExactly?.let { it == message.text } ?: true },
        processor = processor,
    )

    fun onMessagePrefix(prefix: String, processor: MessageProcessor<C>) = addMessageHandler(
        predicate = { message, _ -> message.text.startsWith(prefix) },
        processor = processor,
    )

    fun onMessageContains(text: String, processor: MessageProcessor<C>) = addMessageHandler(
        predicate = { message, _ -> message.text.contains(text) },
        processor = processor,
    )

    private fun addMessageHandler(predicate: (Message, C) -> Boolean, processor: MessageProcessor<C>) {
        messageHandlers.add(MessageHandler(predicate, processor))
    }
}

class ContextBuilder<C : ChatContext?> : BehaviourBuilder<C>() {
    inline fun <reified T : ChatContext?> into(init: BehaviourBuilder<T>.() -> Unit) {
        messageHandlers += BehaviourBuilder<T>()
            .apply(init)
            .messageHandlers
            .map { switchContext<T, C>(it) }
    }

    infix fun <T : ChatContext?> T.into(init: BehaviourBuilder<C>.() -> Unit) {
        messageHandlers += BehaviourBuilder<C>()
            .apply(init)
            .messageHandlers
            .map {
                MessageHandler(
                    predicate = { message, context ->
                        if (context == this@into) it.predicate(message, context) else false
                    },
                    processor = it.processor,
                )
            }
    }
}
