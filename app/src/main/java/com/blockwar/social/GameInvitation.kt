package com.blockwar.social

import android.content.Context
import android.content.Intent
import android.net.Uri

object GameInvitation {
    const val HOST="block-war-convites.web.app"
    fun code(value:String?):String? = value?.trim()?.uppercase(java.util.Locale.ROOT)?.takeIf {it.matches(Regex("[A-Z0-9]{4}"))}
    fun link(room:String):String = "https://$HOST/?sala=${requireNotNull(code(room))}"
    fun parse(uri:Uri?):String? {
        if(uri==null)return null
        return when {
            uri.scheme=="https" && uri.host==HOST && uri.path in listOf("","/") -> code(uri.getQueryParameter("sala"))
            uri.scheme=="blockwar" && uri.host=="join" -> code(uri.getQueryParameter("sala"))
            else -> null
        }
    }
    fun share(context:Context,room:String) {
        val text="Bora disputar uma partida de Block War? Sala ${requireNotNull(code(room))}\n${link(room)}"
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT,text),"Desafiar um amigo").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
