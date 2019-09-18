package black.bracken.seichienvoy

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.md_5.bungee.api.event.PluginMessageEvent
import net.md_5.bungee.api.event.ServerConnectEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PlayerDataSynchronizer(private val synchronizingOriginNames: Set<String>) : Listener {
  private val serverSwitchWaitingMap: MutableMap<String, ConnectionState> = HashMap()

  @EventHandler
  fun onServerConnect(event: ServerConnectEvent) {
    val player = event.player
    val message = writtenMessage(
        MessagingChannels.SUB_CHANNEL_SEND,
        player.name
    )

    val connectedServerInfo = player.server?.info ?: return
    val targetServer = event.target

    if (connectedServerInfo.name !in synchronizingOriginNames) return
    if (connectedServerInfo.address == targetServer.address) return

    when (serverSwitchWaitingMap[player.name]) {
      is PlayerDataUnloaded -> {
        serverSwitchWaitingMap.remove(player.name)
      }
      else -> {
        event.isCancelled = true

        GlobalScope.launch {
          connectedServerInfo.sendData(MessagingChannels.CHANNEL, message)
          suspendCoroutine { continuation: Continuation<Unit> ->
            serverSwitchWaitingMap[player.name] = RequestedPlayerDataUnloading(continuation)
          }
          player.connect(targetServer)
        }
      }
    }
  }

  @EventHandler
  fun onPluginMessage(event: PluginMessageEvent) {
    if (event.tag != MessagingChannels.CHANNEL) return

    val input = DataInputStream(ByteArrayInputStream(event.data))
    try {
      val subChannel = input.readUTF()
      val signaledPlayerName = input.readUTF()

      when (subChannel) {
        MessagingChannels.SUB_CHANNEL_RECEIVE_OK -> {
          when (val switchingState = serverSwitchWaitingMap[signaledPlayerName]) {
            is RequestedPlayerDataUnloading -> {
              serverSwitchWaitingMap[signaledPlayerName] = PlayerDataUnloaded
              switchingState.continuation.resume(Unit)
            }
            else -> {
              throw IllegalStateException("PlayerData discarded for non-switching player $signaledPlayerName")
            }
          }
        }
        MessagingChannels.SUB_CHANNEL_RECEIVE_FAIL -> {
          serverSwitchWaitingMap.remove(signaledPlayerName)
        }
      }
    } catch (exception: Exception) {
      exception.printStackTrace()
    }
  }
}
