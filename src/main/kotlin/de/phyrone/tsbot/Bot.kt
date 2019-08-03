package de.phyrone.tsbot

import com.github.manevolent.ts3j.api.Client
import com.github.manevolent.ts3j.api.TextMessageTargetMode
import com.github.manevolent.ts3j.audio.Microphone
import com.github.manevolent.ts3j.enums.CodecType
import com.github.manevolent.ts3j.event.ClientJoinEvent
import com.github.manevolent.ts3j.event.ClientLeaveEvent
import com.github.manevolent.ts3j.event.TS3Listener
import com.github.manevolent.ts3j.event.TextMessageEvent
import com.github.manevolent.ts3j.identity.LocalIdentity
import com.github.manevolent.ts3j.protocol.TS3DNS
import com.github.manevolent.ts3j.protocol.socket.client.LocalTeamspeakClientSocket
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import de.phyrone.brig.wrapper.GreedyStringArgument
import de.phyrone.brig.wrapper.getArgument
import de.phyrone.brig.wrapper.literal
import de.phyrone.brig.wrapper.runs
import de.phyrone.tsbot.config.BotConfig
import java.io.File
import java.net.InetSocketAddress
import java.util.concurrent.LinkedBlockingQueue
import java.util.logging.Logger


class Bot(val folder: File, val config: BotConfig, val manager: AudioPlayerManager, server: TsBotServer) {
    val name = folder.name
    val dispatcher = CommandDispatcher<ExecutedCommand>()
    val logger = Logger.getLogger("Bot-$name")
    val player = manager.createPlayer().also { player ->
        player.addListener(PlayerEventAdapter())
    }
    val idleFile = File(folder, idleFileName)
    private val identityFile = File(folder, identityFileName)

    val socket = LocalTeamspeakClientSocket().also { socket ->
        socket.microphone = LavaMicrophone(player)
        socket.setIdentity(LocalIdentity.read(identityFile))
        socket.setEventMultiThreading(true)
        socket.addListener(BotEventAdapter())
    }
    val trackQueue = LinkedBlockingQueue<AudioTrack>()
    var idleTracks = mutableSetOf<AudioTrack>()
    private val nextTrackLock = Any()

    init {
        reloadIdleList()
        registerCommands()
        //player.setFrameBufferDuration(470)
    }

    private fun registerCommands() {
        dispatcher.literal("help") {
            alias("?")
            runs {
                sender.sendMessage("Commands:")
                sender.sendMessages(
                    dispatcher.getSmartUsage(
                        dispatcher.root,
                        this
                    ).mapNotNull { (_, line) -> "- $line" })

            }
        }
        dispatcher.literal("play") {
            require { sender.hasPermission("audio.play") }
            argument("track", GreedyStringArgument) {
                runs {
                    manager.loadItem(it.getArgument("track"), object : AudioLoadResultHandler {
                        override fun loadFailed(exception: FriendlyException) {
                            exception.printStackTrace()
                            sender.sendMessage("Load Track Failed")
                            sender.sendMessage(exception.localizedMessage)
                        }

                        override fun trackLoaded(track: AudioTrack) {
                            player.playTrack(track)
                        }

                        override fun noMatches() {
                            sender.sendMessage("No Track Found!")
                        }

                        override fun playlistLoaded(playlist: AudioPlaylist) {

                            val track = playlist.selectedTrack
                                ?: playlist.tracks.firstOrNull()
                                ?: run { noMatches();return }
                            player.playTrack(track)
                        }

                    })
                }
            }

        }
        dispatcher.literal("stop") {
            require { sender.hasPermission("audio.stop") }
            runs {

                player.stopTrack()
                sender.sendMessage("Player Stopped")
            }
        }
        dispatcher.literal("skip") {
            require { sender.hasPermission("audio.skip") }
            runs {
                player.stopTrack()
                playNextTrack(null)
            }
        }
        dispatcher.literal("pause") {
            require { sender.hasPermission("audio.pause") }
            runs {
                player.isPaused = true
            }
        }
        dispatcher.literal("start") {
            alias("resume")
            require { sender.hasPermission("audio.resume") }
            runs {
                player.isPaused = false
                playNextTrack(null)
            }
        }
        dispatcher.literal("volume") {
            require { sender.hasPermission("audio.volume.get") }
            runs {
                sender.sendMessage("Volume: [b]${player.volume}[/b]")
            }
            argument("volume", IntegerArgumentType.integer(0, 100)) {
                require { sender.hasPermission("audio.volume.set") }
                runs {
                    player.volume = it.getArgument("volume")
                    sender.sendMessage("Volume is now [b]${player.volume}[/b]")
                }
            }
        }

        dispatcher.literal("queue") {
            require { sender.hasPermission("audio.queue") }
            literal("add") {
                require { sender.hasPermission("audio.queue.add") }
                argument("track", GreedyStringArgument) {
                    runs {
                        val trackString = it.getArgument<String>("track")
                        manager.loadItem(trackString, object : AudioLoadResultHandler {
                            override fun loadFailed(exception: FriendlyException) {
                                sender.sendMessage(exception.localizedMessage)
                            }

                            override fun trackLoaded(track: AudioTrack) {
                                trackQueue.add(track)
                                sender.sendMessage("${track.info.title} added to Queue")
                            }

                            override fun noMatches() {
                                sender.sendMessage("No Matches")
                            }

                            override fun playlistLoaded(playlist: AudioPlaylist) {
                                trackQueue.addAll(playlist.tracks)
                                sender.sendMessage("${playlist.tracks.size} Tracks added to Queue")
                            }

                        })
                    }
                }
            }
            literal("list") {
                require { sender.hasPermission("audio.queue.list") }
                runs {
                    sender.sendMessage("Tracks in Queue:")
                    sender.sendMessages(trackQueue.map { track -> track.info.title })
                }

            }
            literal("remove") {
                require { sender.hasPermission("audio.queue.remove") }
                argument("track", GreedyStringArgument) {
                    runs {
                        val trackString = it.getArgument<String>("track")
                        trackQueue.removeIf { track -> track.info.title.startsWith(trackString, true) }
                    }
                }
            }
            literal("clear") {
                require { sender.hasPermission("audio.queue.clear") }
                runs {
                    trackQueue.clear()
                }
            }
        }
    }

    fun dispatch(command: ExecutedCommand): Int {
        var result: Int = -1
        val parsed = dispatcher.parse(command.command, command)
        try {
            result = dispatcher.execute(parsed)
        } catch (exception: CommandSyntaxException) {
            result = 2
            val node = parsed.context.nodes.lastOrNull()?.node ?: dispatcher.root
            val correctCmd = exception.input.substring(0, exception.cursor).trim() + " "
            command.sender.sendMessage(exception.localizedMessage)
            command.sender.sendMessage("Commands:")
            command.sender.sendMessages(
                dispatcher.getSmartUsage(node, command).map { entry -> "- $correctCmd${entry.value}" }
            )
        } finally {
            return result
        }

    }

    private fun reloadIdleList() {
        idleTracks.clear()
        try {
            if (idleFile.exists()) {
                idleFile.readLines().forEach { rawtrackString ->
                    val trackString = rawtrackString.trim()
                    if (trackString.isNotBlank() && !trackString.startsWith("#"))
                        manager.loadItem(trackString, object : AudioLoadResultHandler {
                            override fun loadFailed(exception: FriendlyException?) {
                                exception?.printStackTrace()
                            }

                            override fun trackLoaded(track: AudioTrack) {
                                idleTracks.add(track)
                                playNextTrack(null)
                            }

                            override fun noMatches() {
                                logger.warning("No Track Found for \"$trackString\"")
                            }

                            override fun playlistLoaded(playlist: AudioPlaylist) {
                                playlist.tracks.forEach { track -> idleTracks.add(track) }
                                playNextTrack(null)
                            }

                        })

                }
            } else {
                idleFile.createNewFile()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun connect() {
        player.volume = config.volume
        val address: InetSocketAddress = if (config.useSrvRecord) {
            runCatching { TS3DNS.lookup(config.host).first() }
                .getOrElse { InetSocketAddress(config.host, config.port) }
        } else InetSocketAddress(config.host, config.port)
        if (config.version != null) {
            socket.setClientVersion(config.version.platform, config.version.version, config.version.sign)
        }

        socket.nickname = config.nickName

        socket.connect(address, config.serverPassword, config.timeout)
        if (config.channel != null) {
            kotlin.runCatching { socket.joinChannel(config.channel, config.channelPassword) }
        }
    }

    fun playNextTrack(endReason: AudioTrackEndReason?) {
        synchronized(nextTrackLock) {
            if (player.playingTrack == null && endReason?.mayStartNext != false) {
                if (trackQueue.isEmpty()) {
                    if (idleTracks.isEmpty()) {
                        //this is the best english you ever seen
                        logger.warning("Cant play Idle track because there are no available")
                    } else {
                        player.playTrack(idleTracks.random().makeClone())
                    }
                } else {
                    player.playTrack(trackQueue.remove().makeClone())
                }
            }
        }
    }

    fun disconnect() {
        socket.disconnect("bye have a good time")

    }

    inner class BotCommandUser(private val client: Client) : CommandSender {
        constructor(id: Int) : this(socket.getClientInfo(id))

        private val id = client.id
        override fun hasPermission(permission: String): Boolean =
            de.phyrone.tsbot.util.hasPermission(permission, client)

        override fun sendMessage(message: String) {
            socket.sendPrivateMessage(id, message)
        }
    }

    inner class BotEventAdapter : TS3Listener {
        override fun onTextMessage(e: TextMessageEvent) {
            if (e.targetMode == TextMessageTargetMode.CLIENT) {
                var command = e.message.trim()
                if (command.startsWith("!") && command.substring(1).isNotBlank()) {
                    command = command.substring(1)
                    command = command.replace(Regex("\\[[/]?[a-zA-Z0-9]+]"), "")
                    command = command.trim()
                    dispatch(ExecutedCommand(command, BotCommandUser(e.invokerId)))
                }

            }
        }

        override fun onClientJoin(e: ClientJoinEvent) {

        }

        override fun onClientLeave(e: ClientLeaveEvent) {

        }
    }

    inner class PlayerEventAdapter : AudioEventAdapter() {
        override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
            runCatching { socket.setDescription("") }
            playNextTrack(endReason)
        }

        override fun onTrackStart(player: AudioPlayer, track: AudioTrack) {
            runCatching { socket.setDescription(track.info.title) }
        }
    }
}

private class LavaMicrophone(val player: AudioPlayer) : Microphone {

    var audioFrame: ByteArray? = null
    override fun getCodec(): CodecType = CodecType.OPUS_MUSIC
    // override fun isMuted(): Boolean = player.playingTrack != null
    override fun isReady(): Boolean {
        audioFrame = player.provide()?.data
        return audioFrame?.size ?: 0 > 0
    }

    override fun provide(): ByteArray = (audioFrame ?: byteArrayOf()).cutMaxSize(470)
    private fun ByteArray.cutMaxSize(maxSize: Int) = if (this.size > maxSize) {
        System.err.println("AudioFrame to large -> CUT!")
        this.copyOf(maxSize)
    } else this
}