package com.blockwar.settings

import android.content.Context
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class RememberedPinSecurityTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val prefs get() = context.getSharedPreferences("controls", Context.MODE_PRIVATE)
    private fun key() = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    @Before fun reset() { prefs.edit().clear().commit() }

    @Test fun plaintextMigratesWithoutChangingProfileOrDevice() {
        val secret = key()
        prefs.edit().putString("player_name", "Jogador").putString("device_id", "device-original").putString("player_pin", "1234").commit()
        val store = ControlSettingsManager(context, PinCipher { secret })
        assertEquals("1234", store.getPlayerPin())
        assertTrue(prefs.getString("player_pin", "")!!.startsWith("KS2:"))
        assertEquals("Jogador", store.getPlayerName())
        assertEquals("device-original", store.getDeviceId())
        assertEquals("1234", store.getPlayerPin())
    }
    @Test fun oldCbcPinMigratesToAuthenticatedEncryption() {
        val device = "device-original"
        val oldKey = MessageDigest.getInstance("SHA-256").digest("bw_pin_salt_$device".toByteArray())
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val iv = ByteArray(16) { it.toByte() }
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(oldKey, "AES"), IvParameterSpec(iv))
        val encoded = "ENC:" + Base64.encodeToString(iv + cipher.doFinal("5678".toByteArray()), Base64.NO_WRAP)
        prefs.edit().putString("device_id", device).putString("player_pin", encoded).commit()
        val secret = key()
        val store = ControlSettingsManager(context, PinCipher { secret })
        assertEquals("5678", store.getPlayerPin())
        assertTrue(prefs.getString("player_pin", "")!!.startsWith("KS2:"))
    }
    @Test fun everySaveUsesFreshNonce() {
        val secret = key(); val store = ControlSettingsManager(context, PinCipher { secret })
        store.savePlayerPin("1234"); val first = prefs.getString("player_pin", "")
        store.savePlayerPin("1234"); val second = prefs.getString("player_pin", "")
        assertNotEquals(first, second)
        assertEquals("1234", store.getPlayerPin())
    }
    @Test fun lostKeyOnlyClearsRememberedPin() {
        val secret = key()
        val store = ControlSettingsManager(context, PinCipher { secret })
        store.savePlayerName("Jogador"); store.savePlayerPin("1234")
        val anotherKey = key()
        assertEquals("", ControlSettingsManager(context, PinCipher { anotherKey }).getPlayerPin())
        assertFalse(prefs.contains("player_pin"))
        assertEquals("Jogador", store.getPlayerName())
    }
    @Test fun unavailableKeyNeverWritesPlaintextOrKeepsWeakLegacyPin() {
        prefs.edit().putString("player_pin", "1234").commit()
        val store = ControlSettingsManager(context, PinCipher { error("Keystore indisponível") })
        assertEquals("", store.getPlayerPin())
        store.savePlayerPin("5678")
        assertFalse(prefs.contains("player_pin"))
    }
    @Test fun tamperedCiphertextIsRejectedAndCleared() {
        val secret = key(); val store = ControlSettingsManager(context, PinCipher { secret })
        store.savePlayerPin("1234")
        val raw = prefs.getString("player_pin", "")!!
        val payload = Base64.decode(raw.removePrefix("KS2:"), Base64.NO_WRAP)
        payload[payload.lastIndex] = (payload.last().toInt() xor 1).toByte()
        prefs.edit().putString("player_pin", "KS2:" + Base64.encodeToString(payload, Base64.NO_WRAP)).commit()
        assertEquals("", store.getPlayerPin()); assertFalse(prefs.contains("player_pin"))
    }
}
