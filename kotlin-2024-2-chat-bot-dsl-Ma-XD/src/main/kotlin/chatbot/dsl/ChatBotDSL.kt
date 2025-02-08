package chatbot.dsl

import chatbot.api.*
import chatbot.bot.Bot
import chatbot.bot.MessageHandler

@DslMarker
annotation class ChatBotBuilderDSL

fun chatBot(client: Client, init: ChatBotBuilder.() -> Unit): ChatBot =
    ChatBotBuilderImpl(client).apply(init).build()

@ChatBotBuilderDSL
interface ChatBotBuilder {
    var contextManager: ChatContextsManager?
    operator fun LogLevel.unaryPlus()
    fun use(logLevel: LogLevel)
    fun use(otherContextManager: ChatContextsManager)
    fun behaviour(init: ContextBuilder<ChatContext?>.() -> Unit)
}

class ChatBotBuilderImpl(private val client: Client) : ChatBotBuilder {
    private var logLevel: LogLevel = LogLevel.ERROR
    private val messageHandlers: MutableList<MessageHandler<ChatContext?>> = mutableListOf()
    override var contextManager: ChatContextsManager? = null

    override operator fun LogLevel.unaryPlus() {
        use(this@unaryPlus)
    }

    override fun use(logLevel: LogLevel) {
        this.logLevel = logLevel
    }

    override fun use(otherContextManager: ChatContextsManager) {
        contextManager = otherContextManager
    }

    override fun behaviour(init: ContextBuilder<ChatContext?>.() -> Unit) {
        messageHandlers += ContextBuilder<ChatContext?>().apply(init).messageHandlers
    }

    fun build(): ChatBot = Bot(
        logLevel = logLevel,
        contextManager = contextManager,
        messageHandlers = messageHandlers,
        client = client,
    )
}
