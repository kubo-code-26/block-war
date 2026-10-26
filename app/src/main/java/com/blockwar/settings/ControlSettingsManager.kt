package com.blockwar.settings

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import com.blockwar.model.ButtonPos
import com.blockwar.model.ControlMode
import com.blockwar.model.ControlSettings
import com.blockwar.storage.commit
import java.security.MessageDigest
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class ControlSettingsManager internal constructor(context: Context, private val pinCipher: PinCipher) {
    constructor(context: Context) : this(context, AndroidPinKeyStore.cipher())
    private val prefs = context.getSharedPreferences("controls", Context.MODE_PRIVATE)

    fun getDeviceId(): String {
        var id = prefs.getString("device_id", null)
        if (id == null) {
            val newId = UUID.randomUUID().toString()
            id = newId
            prefs.edit { putString("device_id", newId) }
        }
        return id!!
    }

    fun getPlayerName(): String = prefs.getString("player_name", "") ?: ""
    fun savePlayerName(name: String) = prefs.edit { putString("player_name", name) }

    private fun getPinCipherKey(): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest("bw_pin_salt_${getDeviceId()}".toByteArray(Charsets.UTF_8))
    }

    @Synchronized
    fun getPlayerPin(): String {
        val raw = prefs.getString("player_pin", "").orEmpty()
        if (raw.isEmpty()) return ""
        return try {
            if (raw.startsWith(PinCipher.PREFIX)) pinCipher.decrypt(raw)
            else {
                // Read legacy data once; only the new Keystore format is ever written.
                val pin = if (raw.startsWith("ENC:")) {
                    val payload = Base64.decode(raw.removePrefix("ENC:"), Base64.NO_WRAP)
                    require(payload.size > 16)
                    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                    cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(getPinCipherKey(), "AES"), IvParameterSpec(payload.copyOfRange(0, 16)))
                    String(cipher.doFinal(payload.copyOfRange(16, payload.size)), Charsets.UTF_8)
                } else raw
                require(pin.matches(Regex("[0-9]{4}")))
                check(prefs.commit { putString("player_pin", pinCipher.encrypt(pin)) })
                pin
            }
        } catch (_: Exception) {
            // Corrupt backup or unavailable device key: require PIN entry, never weaken encryption.
            prefs.commit { remove("player_pin") }
            ""
        }
    }

    @Synchronized
    fun savePlayerPin(pin: String) {
        if (pin.isEmpty()) {
            prefs.commit { remove("player_pin") }
            return
        }
        require(pin.matches(Regex("[0-9]{4}"))) { "Informe um PIN de quatro números." }
        try {
            check(prefs.commit { putString("player_pin", pinCipher.encrypt(pin)) })
        } catch (_: Exception) {
            prefs.commit { remove("player_pin") }
            android.util.Log.w("BlockWarSecurity", "Não foi possível lembrar o PIN com segurança neste aparelho.")
        }
    }

    fun loadSettings(): ControlSettings {
        val scale = prefs.getFloat("scale", 1.0f)
        val opacity = prefs.getFloat("opacity", 1.0f)
        val buttonGap = prefs.getFloat("buttonGap", 8.0f)
        val modeStr = prefs.getString("controlMode", ControlMode.BUTTONS.name) ?: ControlMode.BUTTONS.name
        val controlMode = try {
            ControlMode.valueOf(modeStr)
        } catch (e: Exception) {
            ControlMode.BUTTONS
        }
        val posLeft = ButtonPos(prefs.getFloat("posLeftX", 0f), prefs.getFloat("posLeftY", 0f))
        val posRotate = ButtonPos(prefs.getFloat("posRotateX", 0f), prefs.getFloat("posRotateY", 0f))
        val posRight = ButtonPos(prefs.getFloat("posRightX", 0f), prefs.getFloat("posRightY", 0f))
        val posSoftDrop = ButtonPos(prefs.getFloat("posSoftDropX", 0f), prefs.getFloat("posSoftDropY", 0f))
        val posHardDrop = ButtonPos(prefs.getFloat("posHardDropX", 0f), prefs.getFloat("posHardDropY", 0f))

        val gestureSensitivity = prefs.getFloat("gestureSensitivity", 1.0f)

        return ControlSettings(
            scale = scale,
            opacity = opacity,
            posLeft = posLeft,
            posRotate = posRotate,
            posRight = posRight,
            posSoftDrop = posSoftDrop,
            posHardDrop = posHardDrop,
            buttonGap = buttonGap,
            controlMode = controlMode,
            gestureSensitivity = gestureSensitivity
        )
    }

    fun saveSettings(settings: ControlSettings) {
        prefs.edit {
            putFloat("scale", settings.scale)
            putFloat("opacity", settings.opacity)
            putFloat("buttonGap", settings.buttonGap)
            putString("controlMode", settings.controlMode.name)
            putFloat("gestureSensitivity", settings.gestureSensitivity)
            putFloat("posLeftX", settings.posLeft.x).putFloat("posLeftY", settings.posLeft.y)
            putFloat("posRotateX", settings.posRotate.x).putFloat("posRotateY", settings.posRotate.y)
            putFloat("posRightX", settings.posRight.x).putFloat("posRightY", settings.posRight.y)
            putFloat("posSoftDropX", settings.posSoftDrop.x).putFloat("posSoftDropY", settings.posSoftDrop.y)
            putFloat("posHardDropX", settings.posHardDrop.x).putFloat("posHardDropY", settings.posHardDrop.y)
        }
    }

    fun resetToDefaults(): ControlSettings {
        val defaultSettings = ControlSettings()
        saveSettings(defaultSettings)
        return defaultSettings
    }
}
