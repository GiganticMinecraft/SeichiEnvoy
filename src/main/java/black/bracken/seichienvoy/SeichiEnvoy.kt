package black.bracken.seichienvoy

import black.bracken.seichienvoy.MessagingChannels.CHANNEL
import net.md_5.bungee.api.plugin.Plugin

class SeichiEnvoy : Plugin() {
    override fun onEnable() {
        val listener = PlayerDataSynchronizer()

        with(proxy) {
            registerChannel(CHANNEL)
            pluginManager.registerListener(this@SeichiEnvoy, listener)
        }
    }
}