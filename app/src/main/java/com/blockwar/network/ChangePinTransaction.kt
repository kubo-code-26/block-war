package com.blockwar.network

import com.google.firebase.database.*

internal class ChangePinTransaction(
    private val oldPin: String, private val newPin: String, private val uid: String,
    private val deviceId: String,
    private val complete: (DatabaseError?, Boolean, DataSnapshot?) -> Unit
) : Transaction.Handler {
    private val salt = FirebaseRealtimeManager.generateSalt()
    override fun doTransaction(data: MutableData): Transaction.Result {
        if (data.value == null) return Transaction.success(data)
        if (!newPin.matches(Regex("[0-9]{4}"))) return Transaction.abort()
        val owner = data.child("firebaseAuthUid").value?.toString().orEmpty()
        val savedSalt = data.child("pin_salt").value?.toString().orEmpty()
        val savedHash = data.child("pin_hash").value?.toString().orEmpty()
            .ifBlank { data.child("pin").value?.toString().orEmpty() }
        val validPin = savedHash.isNotBlank() && oldPin.matches(Regex("[0-9]{4}")) &&
            (if (savedSalt.isBlank()) oldPin == savedHash else FirebaseRealtimeManager.hashPin(oldPin, savedSalt) == savedHash)
        val validGoogle = owner.isNotBlank() && uid.isNotBlank() && owner == uid
        if (!validPin && !validGoogle) return Transaction.abort()
        val hash = FirebaseRealtimeManager.hashPin(newPin, salt)
        data.child("pin_hash").value = hash
        data.child("pin").value = hash
        data.child("pin_salt").value = salt
        data.child("deviceId").value = deviceId
        return Transaction.success(data)
    }
    override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) = complete(error, committed, snapshot)
}
