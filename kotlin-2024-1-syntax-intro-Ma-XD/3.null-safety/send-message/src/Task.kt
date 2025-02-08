fun sendMessageToClient(
    client: Client?,
    message: String?,
    mailer: Mailer,
) {
    client?.personalInfo?.email?.let {
        mailer.sendMessage(it, message ?: "Hello!")
    }
}
