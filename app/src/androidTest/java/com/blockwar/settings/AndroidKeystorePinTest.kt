package com.blockwar.settings

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.security.KeyStore
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Only encrypts a synthetic PIN. Does not read or change player preferences or delete keys. */
@RunWith(AndroidJUnit4::class)
class AndroidKeystorePinTest {
    @Test fun deviceKeyIsNonExportableAndSupportsAuthenticatedPinEncryption() {
        val cipher = AndroidPinKeyStore.cipher()
        val first = cipher.encrypt("4927")
        val second = cipher.encrypt("4927")
        assertTrue(first.startsWith(PinCipher.PREFIX))
        assertNotEquals(first, second)
        assertEquals("4927", cipher.decrypt(first))
        assertEquals("4927", cipher.decrypt(second))
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val key = store.getKey("blockwar.remembered-pin.v2", null)
        assertNotNull(key)
        assertNull(key.encoded)
    }
}
