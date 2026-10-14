package com.blockwar.auth

import com.blockwar.network.SecureBackend

class PlayGamesProfileRepository {
    fun find(identity:PlayGamesIdentity,onSuccess:(String?)->Unit,onError:(String)->Unit) {
        val pending = ResultDeadline<String?>(70_000, onSuccess, onError,
            "O servidor demorou para confirmar o perfil. Confira a conexão e tente novamente.")
        SecureBackend.call("bwPlayFind",emptyMap(),{pending.succeed(it["nickname"] as? String)},pending::fail)
    }
    fun link(identity:PlayGamesIdentity,nickname:String,pin:String,onSuccess:()->Unit,onError:(String)->Unit) {
        val pending = ResultDeadline<Unit>(70_000, { onSuccess() }, onError,
            "Não foi possível confirmar o vínculo a tempo. Reconecte para conferir o resultado antes de tentar novamente.")
        SecureBackend.call("bwPlayLink",mapOf("nickname" to nickname,"pin" to pin),{pending.succeed(Unit)},pending::fail)
    }
}
