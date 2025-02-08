package library.notifications

interface EmailSender {
    suspend fun send(to: String, subject: String, text: String)
}
