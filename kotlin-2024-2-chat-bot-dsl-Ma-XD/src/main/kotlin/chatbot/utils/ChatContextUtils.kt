package chatbot.utils

import chatbot.api.ChatContext
import chatbot.bot.MessageHandler
import chatbot.bot.MessageProcessorContext

object ChatContextUtils {
    inline fun <reified T : ChatContext?, C : ChatContext?> switchContext(handler: MessageHandler<T>) =
        MessageHandler<C>(
            predicate = { message, context ->
                if (context is T) handler.predicate(message, context) else false
            },
            processor = {
                if (context is T) {
                    MessageProcessorContext<T>(
                        message = message,
                        client = client,
                        context = context,
                        setContext = setContext,
                    ).apply(handler.processor)
                }
            },
        )
}
