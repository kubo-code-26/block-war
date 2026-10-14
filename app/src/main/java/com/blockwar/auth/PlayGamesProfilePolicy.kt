package com.blockwar.auth

import com.blockwar.network.FirebaseRealtimeManager
import java.util.Locale

internal object PlayGamesProfilePolicy {
    fun key(name: String) = name.trim().lowercase(Locale.ROOT)
    fun canReserve(existingNickname: String, nickname: String): Boolean =
        nickname.isNotBlank() && (existingNickname.isBlank() || key(existingNickname) == key(nickname))

    fun canLink(existingUid: String, uid: String, pin: String, savedHash: String,
        savedPin: String, salt: String): Boolean {
        if (uid.isBlank()) return false
        if (existingUid.isNotBlank()) return existingUid == uid
        // A legacy nickname must be proven with its PIN, not just the cached device ID.
        if (pin.length != 4 || !pin.all(Char::isDigit)) return false
        val expected = savedHash.ifBlank { savedPin }
        if (expected.isBlank()) return false
        return if (salt.isBlank()) pin == expected
            else FirebaseRealtimeManager.hashPin(pin, salt) == expected
    }
}
