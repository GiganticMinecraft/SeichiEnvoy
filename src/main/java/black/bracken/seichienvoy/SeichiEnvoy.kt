package black.bracken.seichienvoy

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.md_5.bungee.api.event.PluginMessageEvent
import net.md_5.bungee.api.event.ServerConnectEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.event.EventHandler
import java.io.*
import java.lang.IllegalStateException
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

sealed class ConnectionState
data class Switching(val continuation: Continuation<Unit>): ConnectionState()
object Switched: ConnectionState()

class SeichiEnvoy : Plugin(), Listener {
    private val serverSwitchWaitingMap: MutableMap<String, ConnectionState> = HashMap()

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
    fun onServerConnect(event: ServerConnectEvent) {
        val player = event.player
        val message = writtenMessage(
                SUB_CHANNEL_SEND,
                player.name
        )

        val connectedServerInfo = player.server?.info ?: return
        val targetServer = event.target

        when (serverSwitchWaitingMap[player.name]) {
            is Switched -> {
                serverSwitchWaitingMap.remove(player.name)
            }
            else -> {
                event.isCancelled = true

                GlobalScope.launch {
                    connectedServerInfo.sendData(CHANNEL, message)
                    suspendCoroutine { continuation: Continuation<Unit> ->
                        serverSwitchWaitingMap[player.name] = Switching(continuation)
                    }
                    player.connect(targetServer)
                }
            }
        }
    }

    @EventHandler
    fun onPluginMessage(event: PluginMessageEvent) {
        if (event.tag != CHANNEL) return

        val input = DataInputStream(ByteArrayInputStream(event.data))
        try {
            if (input.readUTF() != SUB_CHANNEL_RECEIVE) return

            val signaledPlayerName = input.readUTF()
            when (val switchingState = serverSwitchWaitingMap[signaledPlayerName]) {
                is Switching -> {
                    serverSwitchWaitingMap[signaledPlayerName] = Switched
                    switchingState.continuation.resume(Unit)
                }
                else -> {
                    throw IllegalStateException("PlayerData discarded for non-switching player $signaledPlayerName")
                }
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }

}