package app.zangdale.kanjian.db

import androidx.datastore.core.Serializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDateTime

class AppSettingsSerializer : Serializer<AppSettings> {
    override val defaultValue: AppSettings = AppSettings.getDefaultInstance()

    private val json = Json {
        ignoreUnknownKeys = true
    }

    override suspend fun readFrom(input: InputStream): AppSettings {
        return try {
            val raw = input.readBytes().decodeToString()
            val element = json.parseToJsonElement(raw)
            val obj = element.jsonObject
            var settings = json.decodeFromJsonElement(AppSettings.serializer(), element)

            // Migrate removed internal encrypted storage to media folder.
            if (settings.saveFolder == null || settings.saveFolder == "_'internal") {
                settings = settings.copy(saveFolder = "_'media")
            }

            // Migrate maxDuration -> maxSegments for older settings files.
            if (!obj.containsKey("maxSegments") && obj.containsKey("maxDuration")) {
                val segments = (settings.maxDuration / settings.intervalDuration)
                    .toInt()
                    .coerceIn(1, 10_000)
                settings = settings.copy(
                    maxSegments = segments,
                    maxDuration = segments.toLong() * settings.intervalDuration,
                )
            }

            settings
        } catch (error: SerializationException) {
            error.printStackTrace()
            defaultValue
        } catch (error: Exception) {
            error.printStackTrace()
            defaultValue
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun writeTo(t: AppSettings, output: OutputStream) {
        output.write(
            json.encodeToString(
                AppSettings.serializer(),
                t
            ).encodeToByteArray()
        )
    }
}

class LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LocalDateTime {
        return LocalDateTime.parse(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(value.toString())
    }
}
