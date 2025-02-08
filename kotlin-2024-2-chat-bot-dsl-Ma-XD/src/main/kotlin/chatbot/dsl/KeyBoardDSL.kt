package chatbot.dsl

import chatbot.api.Keyboard

@ChatBotBuilderDSL
interface KeyBoardBuilder {
    var oneTime: Boolean
    var keyboard: MutableList<MutableList<Keyboard.Button>>
    fun row(init: RowBuilder.() -> Unit)
}

@ChatBotBuilderDSL
interface RowBuilder {
    fun button(text: String)
    operator fun String.unaryMinus()
}

class KeyBoardBuilderImpl : KeyBoardBuilder {
    override var oneTime = false
    override var keyboard: MutableList<MutableList<Keyboard.Button>> = mutableListOf()

    override fun row(init: RowBuilder.() -> Unit) {
        keyboard += RowBuilderImpl().apply(init).buttons
    }

    fun build(): Keyboard.Markup? {
        val notEmptyKeyboard = keyboard.filter { it.isNotEmpty() }
        return if (notEmptyKeyboard.isNotEmpty()) {
            Keyboard.Markup(oneTime, notEmptyKeyboard)
        } else {
            null
        }
    }
}

class RowBuilderImpl : RowBuilder {
    val buttons: MutableList<Keyboard.Button> = mutableListOf()

    override fun button(text: String) {
        buttons += Keyboard.Button(text)
    }

    override operator fun String.unaryMinus() {
        button(this@unaryMinus)
    }
}
