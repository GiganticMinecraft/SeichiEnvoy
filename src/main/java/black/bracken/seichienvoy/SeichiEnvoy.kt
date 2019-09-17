package black.bracken.seichienvoy

import kotlinx.coroutines.runBlocking
import net.md_5.bungee.api.event.PluginMessageEvent
import net.md_5.bungee.api.event.ServerConnectEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.event.EventHandler
import java.io.ByteArrayInputStream
import java.io.DataInputStream

class SeichiEnvoy : Plugin(), Listener {

    companion object {
        const val CHANNEL = "SeichiAssistBungee"
        private const val SUB_CHANNEL_RECEIVE = "PlayerDataSavedAndDiscarded"
    }

    override fun onEnable() {
        with(proxy) {
            registerChannel(CHANNEL)
            pluginManager.registerListener(this@SeichiEnvoy, this@SeichiEnvoy)
        }
    }

    @EventHandler
    fun onServerConnect(event: ServerConnectEvent) = runBlocking {
        val player = event.player
        val from = player.server.info
        val to = event.target

        from.sendData(to.name, player.name.toByteArray())
        // blocking
    }

    @EventHandler
    fun onPluginMessage(event: PluginMessageEvent) {
        if (event.tag != SUB_CHANNEL_RECEIVE) return

        val input = DataInputStream(ByteArrayInputStream(event.data))
        try {
            if (input.readUTF() != SUB_CHANNEL_RECEIVE) {
                return
            }

            // onReceive(input.readUTF())
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }

}