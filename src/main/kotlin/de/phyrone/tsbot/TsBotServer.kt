package de.phyrone.tsbot

import com.github.manevolent.ts3j.identity.LocalIdentity
import com.sedmelluq.discord.lavaplayer.format.OpusAudioDataFormat
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import de.phyrone.tsbot.config.BotConfig
import de.phyrone.tsbot.config.Config
import de.phyrone.tsbot.config.ConfigClientVersion
import de.phyrone.tsbot.config.mapper
import de.phyrone.tsbot.util.ClientVersions
import de.phyrone.tsbot.util.selectClientVersionFromNeuland
import org.beryx.textio.TextIoFactory
import org.fusesource.jansi.AnsiConsole
import picocli.CommandLine
import java.io.File
import java.math.BigInteger
import java.util.*
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis


@CommandLine.Command(
    name = "TsBot.jar",
    subcommands = [
        TsBotSetup::class,
        TsBotTools::class
    ]
)
class TsBotServer : Runnable {

    @CommandLine.Option(
        names = ["--imRunningAsRootItIsEvilAndIKnowIt"],
        hidden = true
    )
    var allowRoot = false

    @CommandLine.Option(
        names = ["--config-path", "-c"]
    )
    var configFile = File("config.json")

    @CommandLine.Option(
        names = ["--bots-folder", "-b"]
    )
    var botsFolder = File("bots/")

    @CommandLine.Option(
        names = ["--allow-files-as-tracks"]
    )
    var enableFileSource = false

    override fun run() {
        if (System.getProperty("user.name").equals("root", true) && !allowRoot) {
            System.err.println(""""Root is insecure! But you can use "--imRunningAsRootItIsEvilAndIKnowIt" to ignore this!""")
            exitProcess(1)
        }

        val playerManager = DefaultAudioPlayerManager()
        AudioSourceManagers.registerRemoteSources(playerManager)
        if (enableFileSource) {
            AudioSourceManagers.registerLocalSource(playerManager)
        }
        playerManager.frameBufferDuration = 470
        playerManager.configuration.outputFormat = OpusAudioDataFormat(2, 48000 / 2, 960 / 2)
        Config.load(configFile)
        Config.save(configFile)
        val bots = mutableSetOf<Bot>()

        fun loadBotFromFolder(folder: File) {
            val botCfg = mapper.readValue(File(folder, botConfigFileName), BotConfig::class.java)
            bots += Bot(folder, botCfg, playerManager, this)
        }

        fun loadBotsFromFiles() {
            botsFolder.listFiles { file -> file.isDirectory }
                ?.forEach { folder -> runCatching { loadBotFromFolder(folder) } }
        }
        loadBotsFromFiles()
        if (bots.isEmpty()) {

        }

        bots.forEach { bot ->
            try {
                bot.connect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        Runtime.getRuntime()
            .addShutdownHook(Thread({ bots.forEach { bot -> runCatching { bot.disconnect() } } }, "Shutdown-Bots"))
        while (true) {
            Thread.sleep(Long.MAX_VALUE)
        }
    }


    @CommandLine.Command(
        name = "help",
        aliases = ["--help", "-h"]
    )
    fun showHelp() {
        val commandLine = CommandLine(TsBotServer::class.java)
        println(commandLine.usageMessage)
    }

    fun getBotFolder(name: String) = File(botsFolder, name).also { it.mkdirs() }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            AnsiConsole.systemInstall()
            val commandLine = CommandLine(TsBotServer::class.java)
            commandLine.execute(*args)
        }

    }

}

internal const val idleFileName = "idle.txt"
internal const val identityFileName = "identity.ini"
internal const val botConfigFileName = "config.json"

@CommandLine.Command(
    name = "setup"
)
class TsBotSetup : Runnable {

    @CommandLine.ParentCommand
    lateinit var parent: TsBotServer

    override fun run() {
        runCatching {
            val textIO = TextIoFactory.getTextIO()
            val name = textIO.newStringInputReader()
                .withPattern(Regex("[^/]+").pattern).withDefaultValue(UUID.randomUUID().toString()).read("Name")
            val botFolder = parent.getBotFolder(name)
            val nickName = textIO.newStringInputReader().withMinLength(3).withDefaultValue("MusicBot").read("Nickname")
            val host = textIO.newStringInputReader().read("Host")
            val port = textIO.newIntInputReader().withDefaultValue(9987).read("Port")
            val useSRV = textIO.newBooleanInputReader().withDefaultValue(false).read("Use SRV Record")
            val generateIdentity = textIO.newBooleanInputReader().withDefaultValue(true).read("GenerateIdentity")
            var identity: LocalIdentity? = null
            if (generateIdentity) {
                val securityLevel = textIO.newIntInputReader().withMinVal(1).withDefaultValue(8).read("SecurityLevel")
                textIO.textTerminal.println("Generating Identity...")
                identity = LocalIdentity.generateNew(securityLevel)
                textIO.textTerminal.println("Identity Generated!")
            } else {
                textIO.textTerminal.println("Please put your identity as $idleFileName in ${botFolder.absolutePath}")
            }

            var version: ConfigClientVersion? = null
            if (textIO.newBooleanInputReader().withDefaultValue(false).read("Should the bot have an custom teamspeak version?")) {
                version = ClientVersions.selectClientVersionFromNeuland(textIO)
            }
            textIO.textTerminal.println("Creating Config...")
            val botConfig = BotConfig(
                nickName,
                host,
                port,
                useSRV,
                10000,
                100,
                null,
                null,
                null,
                version
            )
            textIO.textTerminal.println("Saving Config...")

            identity?.save(File(botFolder, identityFileName))
            val configFile = File(botFolder, botConfigFileName)
            mapper.writerWithDefaultPrettyPrinter().writeValue(configFile, botConfig)
            textIO.textTerminal.println("Config Saved")
            textIO.dispose("Bot Created")
        }.getOrElse {
            it.printStackTrace()
            exitProcess(-1)
        }
    }
}

@CommandLine.Command(
    name = "tool",
    aliases = ["tools"],
    subcommands = [
        TsBotIdentityInfoTool::class,
        TsBotGenerateIdentityTool::class,
        TsBotIncreaseIdentityTool::class
    ]
)
class TsBotTools : Runnable {
    override fun run() {
        val commandLine = CommandLine(TsBotTools::class.java)
        println(commandLine.usageMessage)
    }

    @CommandLine.Command(
        name = "clientVersions"
    )
    fun clientVersions() {
        val textIO = TextIoFactory.getTextIO()
        val version = ClientVersions.selectClientVersionFromNeuland(textIO)
        textIO.dispose()
        mapper.writerWithDefaultPrettyPrinter().writeValue(System.out, version)
    }

}

@CommandLine.Command(
    name = "generateIdentity"
)
class TsBotGenerateIdentityTool : Runnable {
    @CommandLine.Option(
        names = ["--security-level", "--level", "-l"]
    )
    var levelOption = 8

    @CommandLine.Option(
        names = ["--output-file", "-o"]
    )
    var outputFile = File(identityFileName)

    override fun run() {
        val level = levelOption.coerceAtLeast(1)
        println("Generating Identity with security level $level... (this may take a while)")
        val identity = LocalIdentity.generateNew(level)
        println("Identity Generated! -> Saving in File...")
        identity.save(outputFile)
        println("Finish")
    }
}

@CommandLine.Command(
    name = "increaseIdentityLevel"
)
class TsBotIncreaseIdentityTool : Runnable {
    @CommandLine.Option(
        names = ["--security-level", "--level", "-l"]
    )
    var levelOption = 12

    @CommandLine.Option(
        names = ["--input-file", "-i"]
    )
    var inputFile = File(identityFileName)

    @CommandLine.Option(
        names = ["--output-file", "-o"]
    )
    var outputFile = inputFile
    @CommandLine.Option(
        names = ["--in-steps", "-x"],
        description = ["Increase the security level in steps with saving between (recomendet for height levels)"]
    )
    var stepping = false

    override fun run() {
        println("Loading identity...")
        val identity = LocalIdentity.read(inputFile)
        val level = levelOption.coerceAtLeast(1)

        fun save() {
            println("Identity increased! -> Saving in File...")
            identity.save(outputFile)
        }
        if (stepping) {
            repeat(level) {
                val stepLevel = it + 1
                if (identity.securityLevel < stepLevel) {
                    println("Increasing security level to ${stepLevel + 1}... (this may take a while)")
                    val time = measureTimeMillis {
                        identity.improveSecurity(stepLevel)
                    }
                    println("Done after ${time}ms")
                    save()
                }
            }
        } else {
            println("Increasing security level to $level... (this may take a while)")
            identity.improveSecurity(level)
            save()
        }
        println("Finish")
    }
}

@CommandLine.Command(
    name = "identityInfo"
)
class TsBotIdentityInfoTool : Runnable {
    @CommandLine.Option(
        names = ["--input-file", "-i"]
    )
    var inputFile = File(identityFileName)

    override fun run() {
        val identity = LocalIdentity.read(inputFile)
        mapper.writerWithDefaultPrettyPrinter().writeValue(
            System.out, InfoOutputJson(
                identity.uid.toBase64(),
                identity.securityLevel,
                identity.publicKeyString,
                identity.privateKey
            )
        )
    }

    private data class InfoOutputJson(
        val uid: String,
        val secruityLevel: Int,
        val publicKey: String,
        val privateKey: BigInteger
    )
}
