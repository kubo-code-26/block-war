package com.blockwar.ui.components

import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import com.blockwar.model.Cell

/** Procedural surfaces keep the exact logical cell and silhouette; no per-frame randomness. */
object RealisticMaterials {
    private val palettes=mapOf(
        "real_aco" to listOf(0xFFE9F0F4,0xFF647680,0xFF24313B),
        "real_titanio" to listOf(0xFFE8E4FF,0xFF8A8CA9,0xFF303346),
        "real_cobre" to listOf(0xFFFFD5A4,0xFFB47242,0xFF3D4943),
        "real_obsidiana" to listOf(0xFF717B89,0xFF1F222D,0xFF090C15),
        "real_magma" to listOf(0xFFFFB46B,0xFF8E321B,0xFF251C20),
        "real_vidro" to listOf(0xFFE3FFFF,0xFF658C9A,0xFF27384D),
        "real_gelo" to listOf(0xFFF0FFFF,0xFF7BC6DC,0xFF234966),
        "real_cristal" to listOf(0xFFF4DEFF,0xFF9571C8,0xFF37264F),
        "real_marmore" to listOf(0xFFFFFAE9,0xFFB7ADA0,0xFF5C5B62),
        "real_madeira" to listOf(0xFFE5BB83,0xFF9C6033,0xFF402A25)
    )
    private val colorPalettes=palettes.mapValues{(_,colors)->colors.map{Color(it)}}
    private val garbageColors=listOf(Color(0xFFBEC4C9),Color(0xFF616A73),Color(0xFF272E37))
    fun draw(scope:DrawScope,origin:Offset,s:Float,cell:Cell,alpha:Float,id:String) = with(scope) {
        val colors=if(cell.isGarbage)garbageColors else colorPalettes[id]?:colorPalettes.getValue("real_aco")
        withTransform({translate(origin.x,origin.y)}) {
            val radius=CornerRadius(s*.1f,s*.1f)
            drawRoundRect(Color.Black.copy(alpha=alpha),Offset(0f,s*.04f),Size(s,s),radius)
            drawRoundRect(Brush.linearGradient(listOf(colors[0],colors[1],colors[2]),Offset.Zero,Offset(s,s)),
                Offset(s*.04f,s*.03f),Size(s*.92f,s*.91f),radius,alpha=alpha)
            if(!cell.isGarbage)RealisticAssets.material(this,id,Offset(s*.1f,s*.1f),s*.8f,alpha*.96f)
            drawRoundRect(colors[0].copy(alpha=.8f*alpha),Offset(s*.08f,s*.07f),Size(s*.84f,s*.82f),radius,style=Stroke(s*.025f))
            if(cell.isGarbage) {
                for(i in 0..3)drawLine(Color.Black.copy(alpha=.5f*alpha),Offset(s*.15f,s*(.25f+i*.16f)),Offset(s*.85f,s*(.25f+i*.16f)),s*.045f)
            } else when(id) {
                "real_aco","real_titanio","real_cobre" -> {
                    for(i in 0..8)drawLine(colors[0].copy(alpha=(if(i%2==0).17f else .07f)*alpha),Offset(s*.12f,s*(.18f+i*.075f)),Offset(s*.88f,s*(.14f+i*.075f)),s*.013f)
                    for(x in listOf(.16f,.84f))drawCircle(colors[2],s*.035f,Offset(s*x,s*.8f),alpha=alpha)
                }
                "real_obsidiana","real_magma" -> {
                    val fissure=if(id=="real_magma")Color(0xFFFFA64B) else Color(0xFFEB844B)
                    val path=Path().apply{moveTo(s*.2f,s*.1f);lineTo(s*.35f,s*.4f);lineTo(s*.25f,s*.57f);lineTo(s*.65f,s*.9f)}
                    drawPath(path,fissure.copy(alpha=.3f*alpha),style=Stroke(s*.1f))
                    drawPath(path,fissure.copy(alpha=.9f*alpha),style=Stroke(s*.022f))
                }
                "real_vidro","real_gelo" -> {
                    val pane=Path().apply{moveTo(s*.1f,s*.12f);lineTo(s*.88f,s*.12f);lineTo(s*.4f,s*.65f);lineTo(s*.1f,s*.8f);close()}
                    drawPath(pane,Color.White.copy(alpha=.23f*alpha))
                    if(id=="real_gelo") {
                        drawLine(Color.White.copy(alpha=.75f*alpha),Offset(s*.6f,s*.18f),Offset(s*.36f,s*.85f),s*.025f)
                        drawLine(Color.White.copy(alpha=.6f*alpha),Offset(s*.45f,s*.6f),Offset(s*.8f,s*.75f),s*.017f)
                    }
                }
                "real_cristal" -> {
                    val facet=Path().apply{moveTo(s*.5f,s*.12f);lineTo(s*.88f,s*.45f);lineTo(s*.5f,s*.86f);lineTo(s*.14f,s*.45f);close()}
                    drawPath(facet,Brush.linearGradient(listOf(Color.White.copy(alpha=.7f),colors[1],colors[2]),Offset.Zero,Offset(s,s)),alpha=alpha)
                    drawLine(Color.White.copy(alpha=.7f*alpha),Offset(s*.5f,s*.12f),Offset(s*.5f,s*.86f),s*.025f)
                }
                "real_marmore","real_madeira" -> for(i in 1..5) {
                    val y=s*i/7
                    val grain=Path().apply{moveTo(s*.1f,y);cubicTo(s*.35f,y-s*.12f,s*.65f,y+s*.13f,s*.9f,y)}
                    drawPath(grain,colors[2].copy(alpha=(if(id=="real_madeira").55f else .28f)*alpha),style=Stroke(s*.018f))
                }
            }
            drawLine(Color.White.copy(alpha=.55f*alpha),Offset(s*.15f,s*.13f),Offset(s*.65f,s*.13f),s*.025f)
            // A small color inlay identifies the piece without losing the material surface.
            if(!cell.isGarbage)drawLine(cell.color.copy(alpha=alpha),Offset(s*.2f,s*.86f),Offset(s*.8f,s*.86f),s*.045f)
        }
    }
}
