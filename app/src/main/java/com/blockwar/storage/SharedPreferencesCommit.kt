package com.blockwar.storage

import android.annotation.SuppressLint
import android.content.SharedPreferences

// PIN, pending match, reward, economy, and mission writes must report durable completion.
@SuppressLint("ApplySharedPref", "UseKtx")
internal inline fun SharedPreferences.commit(
    action: SharedPreferences.Editor.() -> Unit
): Boolean {
    val editor = edit()
    action(editor)
    return editor.commit()
}
