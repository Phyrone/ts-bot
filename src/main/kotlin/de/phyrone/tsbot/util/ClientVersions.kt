package de.phyrone.tsbot.util

import de.phyrone.tsbot.config.ConfigClientVersion
import okhttp3.OkHttpClient
import okhttp3.Request
import org.beryx.textio.TextIO
import java.net.URL
import java.util.*
import kotlin.system.measureTimeMillis

object ClientVersions {
    private val pageUrl = URL("https://raw.githubusercontent.com/ReSpeak/tsdeclarations/master/Versions.csv")
    private val client = OkHttpClient()
    private val versions = mutableListOf<ClientVersion>()
    private fun reloadVersions() {
        val request = Request.Builder()
            .get()
            .url(pageUrl)
            .build()
        val call = client.newCall(request)
        val respone = call.execute()
        val bodyStream = (respone.body?.byteStream() ?: throw NullPointerException())
        val scanner = Scanner(bodyStream)
        scanner.nextLine()
        versions.clear()
        while (scanner.hasNext()) {
            val line = scanner.nextLine()
            val spilted = line.split(",", limit = 3)
            versions.add(
                ClientVersion(
                    version = spilted.component1(),
                    platform = spilted.component2(),
                    sign = spilted.component3(),
                    displayName = "${spilted.component2()} ${spilted.component1().split(" ").first()}"
                )
            )
        }
        scanner.close()
        versions.sortBy { clientVersion -> clientVersion.displayName }
    }

    fun getVersions(): List<ClientVersion> {
        if (versions.isEmpty()) reloadVersions()
        return versions
    }

}

fun ClientVersions.selectClientVersionFromNeuland(textIO: TextIO): ConfigClientVersion {
    textIO.textTerminal.println("Loading Versions...")
    lateinit var versions: List<ClientVersion>
    val time = measureTimeMillis {
        versions = ClientVersions.getVersions()
    }
    textIO.textTerminal.println("Found ${versions.size} versions! (took ${time}ms)")
    val versionStrings = versions.mapIndexed { id, version -> "  ${id + 1}: ${version.displayName}" }.toTypedArray()
    val selectedVersionInt = textIO.newIntInputReader().withMinVal(1).withMaxVal(versions.size + 1)
        .read("Versions:", *versionStrings, "Select your Version")
    return versions[selectedVersionInt - 1].toConfigEntry()
}

private fun ClientVersion.toConfigEntry() = ConfigClientVersion(platform, version, sign)
data class ClientVersion(val displayName: String, val platform: String, val version: String, val sign: String)