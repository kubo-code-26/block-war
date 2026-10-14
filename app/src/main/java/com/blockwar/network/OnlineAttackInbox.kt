package com.blockwar.network

/** Deduplicates Firebase callbacks while this match remains active in the app. */
class OnlineAttackInbox {
    private var activeRoom: String? = null
    private val processedIds = mutableSetOf<String>()

    fun beginRoom(roomCode: String) {
        if (activeRoom != roomCode) {
            activeRoom = roomCode
            processedIds.clear()
        }
    }

    fun pending(attacks: Map<String, Int>): List<Pair<String, Int>> =
        attacks.toSortedMap().entries
            .filter { it.value > 0 && it.key !in processedIds }
            .map { it.key to it.value }

    fun markProcessed(attackId: String) {
        processedIds.add(attackId)
    }
}
