package black.bracken.seichienvoy

import black.bracken.seichienvoy.MessagingChannels.CHANNEL
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.config.Configuration
import net.md_5.bungee.config.ConfigurationProvider
import net.md_5.bungee.config.YamlConfiguration
import java.io.File
import java.nio.file.Files

class SeichiEnvoy : Plugin() {
  private fun createDefaultConfiguration() {
    if (!dataFolder.exists()) dataFolder.mkdir()

    val configurationFile = File(dataFolder, "config.yml")

    if (!configurationFile.exists()) {
      getResourceAsStream("config.yml")
          .use { inputStream -> Files.copy(inputStream, configurationFile.toPath()) }
    }
  }

  override fun onEnable() {
    createDefaultConfiguration()

    val synchronizingOriginNames = run {
      val configuration: Configuration =
          ConfigurationProvider
              .getProvider(YamlConfiguration::class.java)
              .load(File(dataFolder, "config.yml"))

      configuration.getStringList("synchronizing-origin-names").toSet()
    }

    val listener = PlayerDataSynchronizer(synchronizingOriginNames)

    with(proxy) {
      registerChannel(CHANNEL)
      pluginManager.registerListener(this@SeichiEnvoy, listener)
    }
  }
}
