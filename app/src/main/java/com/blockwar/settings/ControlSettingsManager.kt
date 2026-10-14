package com.blockwar.settings

import android.content.Context
import android.util.Base64
import com.blockwar.model.ButtonPos
import com.blockwar.model.ControlMode
import com.blockwar.model.ControlSettings
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class ControlSettingsManager(context: Context) {
    private val prefs = context.getSharedPreferences("controls", Context.MODE_PRIVATE)

    fun getDeviceId(): String {
        var id = prefs.getString("device_id", null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString("device_id", id).apply()
        }
        return id!!
    }

    fun getPlayerName(): String = prefs.getString("player_name", "") ?: ""
    fun savePlayerName(name: String) = prefs.edit().putString("player_name", name).apply()

    private fun getPinCipherKey(): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest("bw_pin_salt_${getDeviceId()}".toByteArray(Charsets.UTF_8))
    }

    fun getPlayerPin(): String {
        val raw = prefs.getString("player_pin", "") ?: ""
        if (raw.isEmpty()) return ""
        if (!raw.startsWith("ENC:")) {
            // Migração automática de PIN em texto puro existente no aparelho
            savePlayerPin(raw)
            return raw
        }
        return try {
            val payload = Base64.decode(raw.removePrefix("ENC:"), Base64.NO_WRAP)
            if (payload.size <= 16) return ""
            val iv = payload.copyOfRange(0, 16)
            val encrypted = payload.copyOfRange(16, payload.size)
            val key = getPinCipherKey()
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }

    fun savePlayerPin(pin: String) {
        if (pin.isEmpty()) {
            prefs.edit().putString("player_pin", "").apply()
            return
        }
        try {
            val key = getPinCipherKey()
            val iv = ByteArray(16).also { SecureRandom().nextBytes(it) }
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
            val encrypted = cipher.doFinal(pin.toByteArray(Charsets.UTF_8))
            val combined = iv + encrypted
            val encoded = Base64.encodeToString(combined, Base64.NO_WRAP)
            prefs.edit().putString("player_pin", "ENC:$encoded").apply()
        } catch (e: Exception) {
            // Fallback silencioso
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
        prefs.edit()
            .putFloat("scale", settings.scale)
            .putFloat("opacity", settings.opacity)
            .putFloat("buttonGap", settings.buttonGap)
            .putString("controlMode", settings.controlMode.name)
            .putFloat("gestureSensitivity", settings.gestureSensitivity)
            .putFloat("posLeftX", settings.posLeft.x).putFloat("posLeftY", settings.posLeft.y)
            .putFloat("posRotateX", settings.posRotate.x).putFloat("posRotateY", settings.posRotate.y)
            .putFloat("posRightX", settings.posRight.x).putFloat("posRightY", settings.posRight.y)
            .putFloat("posSoftDropX", settings.posSoftDrop.x).putFloat("posSoftDropY", settings.posSoftDrop.y)
            .putFloat("posHardDropX", settings.posHardDrop.x).putFloat("posHardDropY", settings.posHardDrop.y)
            .apply()
    }

    fun resetToDefaults(): ControlSettings {
        val defaultSettings = ControlSettings()
        saveSettings(defaultSettings)
        return defaultSettings
    }
}
