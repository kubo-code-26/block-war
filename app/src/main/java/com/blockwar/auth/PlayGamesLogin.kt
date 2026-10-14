package com.blockwar.auth

import android.app.Activity
import com.blockwar.R
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.games.PlayGames
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PlayGamesAuthProvider

/** The server authorization code is exchanged with Firebase, never persisted or logged. */
class PlayGamesLogin(private val activity: Activity) {
    fun authenticate(interactive: Boolean, onSuccess: (PlayGamesIdentity) -> Unit,
        onError: (String) -> Unit) {
        if (activity.isFinishing || activity.isDestroyed) {
            onError("Abra o jogo novamente para conectar."); return
        }
        val client = PlayGames.getGamesSignInClient(activity)
        val task = if (interactive) client.signIn() else client.isAuthenticated
        task.addOnSuccessListener { result ->
            if (!result.isAuthenticated) {
                onError("Entre em uma conta do Google Play Games para continuar.")
                return@addOnSuccessListener
            }
            client.requestServerSideAccess(activity.getString(R.string.play_games_server_client_id), false)
                .addOnSuccessListener { code ->
                    if (code.isNullOrBlank()) {
                        onError("O Play Games não retornou a autorização. Confira a credencial de servidor.")
                        return@addOnSuccessListener
                    }
                    com.blockwar.network.SecureBackend.waitForInitialSession {
                    FirebaseAuth.getInstance().signInWithCredential(PlayGamesAuthProvider.getCredential(code))
                        .addOnSuccessListener { authResult ->
                            val user = authResult.user
                            val provider = user?.providerData?.firstOrNull {
                                it.providerId == PlayGamesAuthProvider.PROVIDER_ID
                            }
                            if (user == null || provider == null || provider.uid.isBlank()) {
                                onError("Não foi possível confirmar a identidade do Play Games.")
                            } else {
                                // The gamer name is display information; Firebase remains the authority for identity.
                                PlayGames.getPlayersClient(activity).currentPlayer.addOnCompleteListener { playerTask ->
                                    val gamerName = if (playerTask.isSuccessful) playerTask.result?.displayName.orEmpty()
                                        else user.displayName.orEmpty()
                                    onSuccess(PlayGamesIdentity(user.uid, provider.uid, user.email.orEmpty(), gamerName))
                                }
                            }
                        }.addOnFailureListener {
                            onError("Falha ao autenticar no Firebase. Confira o provedor Play Games, o cliente Web e a conexão.")
                        }
                    }
                }.addOnFailureListener { onError(playGamesError(it)) }
        }.addOnFailureListener { onError(playGamesError(it)) }
    }

    private fun playGamesError(error: Exception): String {
        val code = (error as? ApiException)?.statusCode
        return "Não foi possível conectar ao Play Games" + (code?.let { " (código $it)" } ?: "") +
            ". Confira a conta de teste, o pacote e o SHA-1 da versão instalada."
    }
}

data class PlayGamesIdentity(val firebaseUid: String, val playerId: String, val email: String = "",
    val accountName: String = "")
