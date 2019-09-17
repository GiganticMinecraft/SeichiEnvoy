package black.bracken.seichienvoy

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException

fun writtenMessage(vararg messages: String): ByteArray {
    val b = ByteArrayOutputStream()
    val out = DataOutputStream(b)

    try {
        messages.forEach { out.writeUTF(it) }
    } catch (e: IOException) {
        e.printStackTrace()
    }

    return b.toByteArray()
}

object MessagingChannels {
    const val CHANNEL = "SeichiAssistBungee"
    const val SUB_CHANNEL_SEND = "SaveAndDiscardPlayerData"
    const val SUB_CHANNEL_RECEIVE = "PlayerDataSavedAndDiscarded"
}
