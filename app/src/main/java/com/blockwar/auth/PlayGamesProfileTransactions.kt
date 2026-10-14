package com.blockwar.auth

import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction

internal fun reservePlayGamesAccount(data: MutableData, nickname: String): Transaction.Result {
    val existing = data.child("nickname").value?.toString().orEmpty()
    if (!PlayGamesProfilePolicy.canReserve(existing, nickname)) return Transaction.abort()
    data.child("nickname").value = nickname
    return Transaction.success(data)
}

internal fun attachPlayGamesProfile(data: MutableData, identity: PlayGamesIdentity,
    pin: String): Transaction.Result {
    // A null initial cache is retried against the server. Never create a new user here.
    if (data.value == null) return Transaction.success(data)
    val existingUid = data.child("firebaseAuthUid").value?.toString().orEmpty()
    if (!PlayGamesProfilePolicy.canLink(existingUid, identity.firebaseUid, pin,
        data.child("pin_hash").value?.toString().orEmpty(),
        data.child("pin").value?.toString().orEmpty(),
        data.child("pin_salt").value?.toString().orEmpty())) return Transaction.abort()
    data.child("firebaseAuthUid").value = identity.firebaseUid
    data.child("google_play_id").value = identity.playerId
    data.child("googlePlayGamesId").value = identity.playerId
    if (identity.email.isNotBlank()) data.child("email").value = identity.email
    return Transaction.success(data)
}
