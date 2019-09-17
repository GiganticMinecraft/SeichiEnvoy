package black.bracken.seichienvoy

import kotlinx.coroutines.runBlocking
import net.md_5.bungee.api.event.PluginMessageEvent
import net.md_5.bungee.api.event.ServerConnectEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.event.EventHandler
import java.io.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SeichiEnvoy : Plugin(), Listener {
    private val serverSwitchWaitingMap: MutableMap<String, Continuation<Unit>> = HashMap()

    companion object {
        const val CHANNEL = "SeichiAssistBungee"
        private const val SUB_CHANNEL_SEND = "SaveAndDiscardPlayerData"
        private const val SUB_CHANNEL_RECEIVE = "PlayerDataSavedAndDiscarded"
    }

    override fun onEnable() {
        with(proxy) {
            registerChannel(CHANNEL)
            pluginManager.registerListener(this@SeichiEnvoy, this@SeichiEnvoy)
        }
    }

    private fun writtenMessage(vararg messages: String): ByteArray {
        val b = ByteArrayOutputStream()
        val out = DataOutputStream(b)

        try {
            messages.forEach { out.writeUTF(it) }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return b.toByteArray()
    }

   @EventHandler
    fun onServerConnect(event: ServerConnectEvent) = runBlocking {
        val player = event.player
        val message = writtenMessage(
                SUB_CHANNEL_SEND,
                player.name
        )

        player.server.info.sendData("SeichiAssistBungee", message)
        suspendCoroutine { continuation: Continuation<Unit> ->
            serverSwitchWaitingMap[player.name] = continuation
        }
    }

    @EventHandler
    fun onPluginMessage(event: PluginMessageEvent) {
        if (event.tag != SUB_CHANNEL_RECEIVE) return

        val input = DataInputStream(ByteArrayInputStream(event.data))
        try {
            if (input.readUTF() != SUB_CHANNEL_RECEIVE) {
                return
            }

            val signaledPlayerName = input.readUTF()

            serverSwitchWaitingMap.remove(signaledPlayerName)!!.resume(Unit)
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }

}