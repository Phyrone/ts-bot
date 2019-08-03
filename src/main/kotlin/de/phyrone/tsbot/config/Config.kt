package de.phyrone.tsbot.config

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

val mapper = ObjectMapper().registerKotlinModule()

data class Config(
    @JsonProperty("Permissions") val permissions: List<PermissionGroupConfig> = mutableListOf(
        PermissionGroupConfig(
            listOf(),
            listOf(6),
            listOf("*")
        ), PermissionGroupConfig(
            listOf(),
            listOf(),
            listOf(
                "audio.queue.add"
            )
        )
    )
    // ,@JsonProperty("YoutubeDL")val youtubedl: YoutubeDlConfig = YoutubeDlConfig()
) {
    companion object {

        private var configField = Config()
        val config: Config
            get() = configField

        fun load(file: File) {
            try {
                configField = mapper.readValue(file, Config::class.java)
            } catch (e: FileNotFoundException) {
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }

        fun save(file: File) {
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, configField)
        }

    }
}

data class YoutubeDlConfig(
    @JsonProperty("Enabled") val enabled: Boolean = false

)

data class PermissionGroupConfig(
    val users: List<String>,
    val groups: List<Int>,
    val permissions: List<String>
)

data class BotConfig(
    @JsonProperty("NickName") val nickName: String,
    @JsonProperty("Host") val host: String,
    @JsonProperty("Port") val port: Int,
    @JsonProperty("UseSRVRecord") val useSrvRecord: Boolean,
    @JsonProperty("Timeout") val timeout: Long,
    @JsonProperty("Volume") val volume: Int,
    @JsonProperty("ServerPassword") val serverPassword: String?,
    @JsonProperty("Channel") val channel: Int?,
    @JsonProperty("ChannelPassword") val channelPassword: String?,
    @JsonProperty("Version") val version: ConfigClientVersion?
)

data class ConfigClientVersion(
    @JsonProperty("Platform") val platform: String,
    @JsonProperty("Version") val version: String,
    @JsonProperty("Sign") val sign: String
)