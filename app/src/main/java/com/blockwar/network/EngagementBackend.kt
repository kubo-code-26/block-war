package com.blockwar.network

import com.blockwar.auth.ResultDeadline

/** New arena operations always release loading state, even if an SDK callback stalls. */
object EngagementBackend {
    fun call(name:String,data:Map<String,Any?>,success:(Map<String,Any?>)->Unit,error:(String)->Unit) {
        val result=ResultDeadline<Map<String,Any?>>(70_000,success,error,
            "A confirmação demorou demais. Confira a conexão e atualize para consultar o resultado.")
        SecureBackend.call(name,data,result::succeed,result::fail)
    }
}
