package com.blockwar.social

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.*
import androidx.core.content.FileProvider
import com.blockwar.achievements.AchievementCatalog
import com.blockwar.model.FeaturedBadge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class ResultCardData(val nickname:String,val won:Boolean,val score:Int,val lines:Int,val attacks:Int,
    val combo:Int,val seconds:Long,val mode:String,val badge:FeaturedBadge?=null)

object ResultCard {
    fun render(data:ResultCardData):Bitmap {
        val bitmap=Bitmap.createBitmap(900,1600,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(bitmap);val paint=Paint(Paint.ANTI_ALIAS_FLAG)
        val accent=if(data.won)Color.rgb(255,201,71) else Color.rgb(0,229,255)
        paint.shader=LinearGradient(0f,0f,900f,1600f,intArrayOf(Color.rgb(9,22,34),Color.rgb(30,18,47),Color.rgb(8,14,24)),null,Shader.TileMode.CLAMP)
        canvas.drawRect(0f,0f,900f,1600f,paint);paint.shader=null
        paint.style=Paint.Style.STROKE;paint.strokeWidth=3f;paint.color=accent
        canvas.drawRoundRect(RectF(35f,35f,865f,1565f),42f,42f,paint);paint.style=Paint.Style.FILL
        fun text(value:String,y:Float,size:Float,color:Int=Color.WHITE,bold:Boolean=false) {
            paint.color=color;paint.textSize=size;paint.typeface=if(bold)Typeface.create("sans-serif",Typeface.BOLD) else Typeface.create("sans-serif",Typeface.NORMAL)
            canvas.drawText(value,80f,y,paint)
        }
        text("BLOCK WAR",155f,74f,Color.rgb(0,229,255),true)
        text(data.mode,216f,26f,Color.LTGRAY)
        text(if(data.won)"VITÓRIA" else "DESAFIO ACEITO",355f,if(data.won)100f else 63f,accent,true)
        text(data.nickname.take(12).ifBlank{"JOGADOR"},445f,60f,Color.WHITE,true)
        val badge=data.badge
        if(badge!=null) {
            val tier=AchievementCatalog.tiers.getOrNull(badge.tier)
            val category=AchievementCatalog.categories.firstOrNull{it.id==badge.category}
            if(tier!=null&&category!=null) {
                val color=tier.color.toInt();val shield=Path().apply {
                    moveTo(90f,490f);lineTo(150f,490f);lineTo(148f,540f);lineTo(120f,562f);lineTo(92f,540f);close()
                }
                paint.color=color;canvas.drawPath(shield,paint)
                paint.style=Paint.Style.STROKE;paint.color=Color.WHITE;paint.strokeWidth=2f;canvas.drawPath(shield,paint);paint.style=Paint.Style.FILL
                paint.color=color;paint.textSize=30f;canvas.drawText("${category.name} · ${tier.name}",180f,535f,paint)
            }
        }
        paint.color=Color.argb(180,31,43,60);canvas.drawRoundRect(RectF(65f,620f,835f,1300f),32f,32f,paint)
        text("${data.score.coerceAtLeast(0)} BW",765f,104f,accent,true)
        text("PONTOS DA PARTIDA",815f,25f,Color.LTGRAY)
        val stats=listOf("Linhas limpas" to data.lines.coerceAtLeast(0).toString(),
            "Blocos enviados" to data.attacks.coerceAtLeast(0).toString(),
            "Melhor combo" to "${data.combo.coerceAtLeast(0)}x",
            "Tempo de partida" to "${data.seconds.coerceAtLeast(0)/60}m ${data.seconds.coerceAtLeast(0)%60}s")
        stats.forEachIndexed {i,(label,value)->
            text(label,925f+i*88,30f,Color.LTGRAY)
            paint.textSize=36f;paint.color=Color.WHITE;paint.typeface=Typeface.create("sans-serif",Typeface.BOLD)
            canvas.drawText(value,790f-paint.measureText(value),925f+i*88,paint)
        }
        text("BORA PARA A ARENA?",1410f,43f,Color.WHITE,true)
        text("Desafie seus amigos no Block War",1470f,25f,Color.LTGRAY)
        return bitmap
    }
    suspend fun share(context:Context,data:ResultCardData) {
        val file=withContext(Dispatchers.IO) {
            val directory=File(context.cacheDir,"share_cards").apply{mkdirs()}
            val output=File(directory,"bw-result-${UUID.randomUUID()}.png")
            val bitmap=render(data)
            try{output.outputStream().use{check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it))}}
            finally{bitmap.recycle()}
            output
        }
        val uri=FileProvider.getUriForFile(context,"${context.packageName}.results",file)
        val intent=Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM,uri)
            .putExtra(Intent.EXTRA_TEXT,"Bora jogar Block War? https://play.google.com/store/apps/details?id=com.blockwar.game")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).apply{clipData=ClipData.newRawUri("Resultado Block War",uri)}
        context.startActivity(Intent.createChooser(intent,"Compartilhar resultado").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
