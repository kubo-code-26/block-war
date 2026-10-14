package com.blockwar.auth

import com.blockwar.network.SecureBackend

class PlayGamesProfileRepository {
    fun find(identity:PlayGamesIdentity,onSuccess:(String?)->Unit,onError:(String)->Unit) {
        SecureBackend.call("bwPlayFind",emptyMap(),{onSuccess(it["nickname"] as? String)},onError)
    }
    fun link(identity:PlayGamesIdentity,nickname:String,pin:String,onSuccess:()->Unit,onError:(String)->Unit) {
        SecureBackend.call("bwPlayLink",mapOf("nickname" to nickname,"pin" to pin),{onSuccess()},onError)
    }
}
