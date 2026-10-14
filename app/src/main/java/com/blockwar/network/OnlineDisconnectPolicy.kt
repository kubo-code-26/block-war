package com.blockwar.network

object OnlineDisconnectPolicy {
    const val GRACE_MS = 30_000L
    enum class Decision { CONTINUE, SELF_LOSS, OPPONENT_LOSS, CANCEL }
    fun expired(disconnectedAt: Long, now: Long): Boolean = disconnectedAt > 0 && now - disconnectedAt >= GRACE_MS
    fun decide(selfConnected: Boolean, selfAt: Long, opponentConnected: Boolean,
               opponentAt: Long, now: Long, reconnecting: Boolean): Decision {
        if (reconnecting && !selfConnected && expired(selfAt, now)) {
            return if (opponentConnected) Decision.SELF_LOSS else Decision.CANCEL
        }
        if ((selfConnected || reconnecting) && !opponentConnected && expired(opponentAt, now)) {
            return Decision.OPPONENT_LOSS
        }
        return Decision.CONTINUE
    }
}
