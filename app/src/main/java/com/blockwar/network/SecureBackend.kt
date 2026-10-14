package com.blockwar.network

import com.blockwar.model.ControlSettings
import com.blockwar.model.PlayerInventory
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/** Callable requests carry the Firebase Auth token; no client-side secret or admin credential. */
object SecureBackend {
    private var anonymousTask: Task<AuthResult>? = null
    private val gson = Gson()
    fun waitForInitialSession(onReady:()->Unit) {
        val pending=synchronized(this){anonymousTask}
        if(pending==null)onReady() else pending.addOnCompleteListener {onReady()}
    }
    fun call(name: String, data: Map<String, Any?>, onSuccess: (Map<String, Any?>) -> Unit,
        onError: (String) -> Unit) {
        fun invoke() {
            FirebaseFunctions.getInstance("us-central1").getHttpsCallable(name).call(data)
                .addOnSuccessListener { result ->
                    @Suppress("UNCHECKED_CAST")
                    val value = result.data as? Map<String, Any?>
                    if (value == null) onError("Resposta inválida do servidor.") else onSuccess(value)
                }.addOnFailureListener { error ->
                    val code=(error as? FirebaseFunctionsException)?.code
                    onError(if (code==FirebaseFunctionsException.Code.NOT_FOUND || code==FirebaseFunctionsException.Code.UNAVAILABLE)
                        "Servidor protegido indisponível. Publique o servidor do pacote 02 antes de usar esta versão."
                        else error.message ?: "Não foi possível confirmar a operação no servidor.")
                }
        }
        val auth=FirebaseAuth.getInstance()
        if(auth.currentUser!=null) { invoke();return }
        val task=synchronized(this) {
            anonymousTask ?: auth.signInAnonymously().also { anonymousTask=it }
        }
        task.addOnCompleteListener {
            synchronized(this) { if(anonymousTask===task)anonymousTask=null }
            if(it.isSuccessful)invoke()
            else onError("Não foi possível iniciar a sessão. Confira a conexão e habilite Autenticação anônima no Firebase.")
        }
    }
    fun inventory(data:Map<String,Any?>):PlayerInventory =
        runCatching {gson.fromJson(gson.toJson(data["inventory"]),PlayerInventory::class.java)}.getOrNull()
            ?.copy(coins=(data["coins"] as? Number)?.toInt()?:100) ?: PlayerInventory()
    fun layout(data:Map<String,Any?>):ControlSettings =
        runCatching {gson.fromJson(gson.toJson(data["layout"]),ControlSettings::class.java)}.getOrNull()?:ControlSettings()
    fun map(value:Any):Map<String,Any?> = gson.fromJson(gson.toJson(value),object:TypeToken<Map<String,Any?>>(){}.type)
}
