package library.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
data class Book(
    val id: String,
    val title: String,
    val author: String,
    val year: Int,
    @XmlSerialName("Description")
    val description: BookDescription,
)

@Serializable
data class BookDescription(
    val description: String,
    @Serializable(GenresSerializer::class)
    val genres: List<String> = emptyList(),
)

object GenresSerializer : KSerializer<List<String>> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("genres", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): List<String> {
        return decoder.decodeString().split(",")
    }

    override fun serialize(encoder: Encoder, value: List<String>) {
        encoder.encodeString(value.joinToString(","))
    }
}
