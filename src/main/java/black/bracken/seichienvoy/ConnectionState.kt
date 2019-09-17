package black.bracken.seichienvoy

import kotlin.coroutines.Continuation

sealed class ConnectionState
data class RequestedPlayerDataUnloading(val continuation: Continuation<Unit>): ConnectionState()
object PlayerDataUnloaded: ConnectionState()
