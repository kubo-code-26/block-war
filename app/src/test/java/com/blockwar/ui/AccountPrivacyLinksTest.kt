package com.blockwar.ui

import android.content.Intent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.blockwar.ui.components.AccountPrivacyLinks
import com.blockwar.ui.components.AccountSupport
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w393dp-h850dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AccountPrivacyLinksTest {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    @Test fun privacyAndTermsOpenTheirExistingPublicPages() {
        val opened = mutableListOf<Intent>()
        compose.setContent { MaterialTheme { AccountPrivacyLinks("Jogador", open = { opened.add(it); true }) } }
        compose.onNodeWithTag("settings_privacy").performClick()
        compose.onNodeWithTag("settings_terms").performClick()
        compose.runOnIdle {
            assertEquals(listOf("${AccountSupport.ORIGIN}/privacidade/", "${AccountSupport.ORIGIN}/termos/"), opened.map { it.data.toString() })
            assertTrue(opened.all { it.action == Intent.ACTION_VIEW })
        }
    }
    @Test fun deletionShowsExplanationAndOnlyOpensRequestWhenChosen() {
        val opened = mutableListOf<Intent>()
        compose.setContent { MaterialTheme { AccountPrivacyLinks("Jogador", open = { opened.add(it); true }) } }
        compose.onNodeWithTag("settings_delete_account").performClick()
        compose.runOnIdle { assertTrue(opened.isEmpty()) }
        compose.onNodeWithText("Solicitar pelo site").performClick()
        compose.runOnIdle { assertEquals("${AccountSupport.ORIGIN}/excluir-conta/", opened.single().data.toString()) }
    }
    @Test fun noEmailAppShowsFallbackAndNeverClaimsRequestWasSent() {
        compose.setContent { MaterialTheme { AccountPrivacyLinks("Jogador", open = { false }) } }
        compose.onNodeWithTag("settings_delete_account").performClick()
        compose.onNodeWithText("Preparar e-mail").performScrollTo().performClick()
        compose.onNodeWithText("Não foi possível abrir.", substring = true).assertExists()
    }
    @Test fun emailIsDraftAndNicknameIsEncodedAsBodyInsteadOfUriInstructions() {
        val name = "A&B?subject=Outro"
        val intent = AccountSupport.deletionEmail(name)
        assertEquals(Intent.ACTION_SENDTO, intent.action)
        assertEquals("mailto", intent.data!!.scheme)
        assertTrue(intent.data.toString().contains(android.net.Uri.encode(name)))
        assertFalse(intent.data.toString().contains("?subject=Outro"))
    }
}
