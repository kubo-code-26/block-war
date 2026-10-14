package com.blockwar.ui.components

import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.blockwar.achievements.AchievementCatalog
import kotlin.math.cos
import kotlin.math.sin

/** Seven silhouettes, shared category symbols. Animation invalidates drawing only. */
@Composable
fun AchievementShield(category: String, level: Int, unlocked: Boolean, modifier: Modifier = Modifier,
    allowAnimation: Boolean = true) {
    val tier=AchievementCatalog.tiers.getOrNull(level)
    val rank=level.coerceIn(0,6)
    val color=if(unlocked && tier!=null) Color(tier.color) else Color(0xFF667184)
    val accent=if(unlocked && rank==6) Color(0xFFFFD36B) else color
    val context=LocalContext.current
    val systemMotion=remember(context) {
        runCatching { Settings.Global.getFloat(context.contentResolver,Settings.Global.ANIMATOR_DURATION_SCALE,1f)>0f }.getOrDefault(true)
    }
    val pulse: State<Float>
    val phase: State<Float>
    if(unlocked && level>=4 && allowAnimation && systemMotion) {
        val transition=rememberInfiniteTransition(label="emblem_light")
        pulse=transition.animateFloat(.3f,.85f,infiniteRepeatable(tween(1900),RepeatMode.Reverse),label="halo")
        phase=transition.animateFloat(0f,1f,infiniteRepeatable(tween(3600,easing=LinearEasing)),label="shine")
    } else {
        pulse=rememberUpdatedState(.4f)
        phase=rememberUpdatedState(.25f)
    }
    val icon=when(category) {
        "wins"->Icons.Default.EmojiEvents
        "attacks"->Icons.Default.Bolt
        "lines"->Icons.Default.GridView
        "combos"->Icons.Default.LocalFireDepartment
        "matches"->Icons.Default.SportsEsports
        else->Icons.Default.ShoppingBag
    }
    Box(modifier.semantics {
        contentDescription="$category, ${tier?.name ?: "sem escudo"}, ${if(unlocked) "conquistado" else "bloqueado"}"
    },contentAlignment=Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w=size.width;val h=size.height
            val outline=emblemOutline(rank,w,h)
            val frame=Stroke(width=w*.026f)
            val glow=pulse.value
            if(unlocked && rank>=4) {
                drawCircle(color.copy(alpha=.055f+glow*.05f),radius=w*.46f,center=Offset(w*.5f,h*.5f))
                drawPath(outline,color.copy(alpha=glow*.13f),style=Stroke(w*.12f))
            }
            // Platinum and Legendary have a winged silhouette, not just another paint color.
            if(rank==3 || rank==6) {
                for(side in listOf(-1,1)) {
                    val wing=Path().apply {
                        moveTo(w*.5f,h*.40f)
                        lineTo(w*(.5f+side*.46f),h*.23f)
                        lineTo(w*(.5f+side*.40f),h*.45f)
                        lineTo(w*(.5f+side*.30f),h*.43f)
                        lineTo(w*(.5f+side*.35f),h*.59f)
                        lineTo(w*(.5f+side*.24f),h*.56f)
                        lineTo(w*(.5f+side*.24f),h*.73f)
                        lineTo(w*.5f,h*.68f);close()
                    }
                    drawPath(wing,accent.copy(alpha=.22f));drawPath(wing,accent,style=frame)
                }
            }
            drawPath(outline,Brush.verticalGradient(listOf(color.copy(alpha=.48f),Color(0xFF10131B),color.copy(alpha=.14f))))
            drawPath(outline,accent,style=frame)
            when(rank) {
                0 -> { // Riveted, triangular bronze buckler.
                    for(x in listOf(.28f,.72f)) drawCircle(color,radius=w*.026f,center=Offset(w*x,h*.27f))
                    drawLine(color.copy(alpha=.5f),Offset(w*.5f,h*.18f),Offset(w*.5f,h*.81f),w*.014f)
                }
                1 -> { // Round silver medallion and curved shield.
                    drawCircle(color.copy(alpha=.55f),w*.235f,Offset(w*.5f,h*.47f),style=Stroke(w*.018f))
                    drawCircle(color,w*.03f,Offset(w*.5f,h*.77f))
                }
                2 -> { // Crown and hexagonal armor.
                    val crown=Path().apply {
                        moveTo(w*.30f,h*.23f);lineTo(w*.27f,h*.10f);lineTo(w*.42f,h*.17f)
                        lineTo(w*.50f,h*.04f);lineTo(w*.58f,h*.17f);lineTo(w*.73f,h*.10f)
                        lineTo(w*.70f,h*.23f);close()
                    }
                    drawPath(crown,color);drawLine(color,Offset(w*.32f,h*.72f),Offset(w*.68f,h*.72f),w*.025f)
                }
                3 -> {
                    val inner=emblemOutline(3,w*.76f,h*.80f)
                    drawContext.canvas.save();drawContext.canvas.translate(w*.12f,h*.10f)
                    drawPath(inner,color.copy(alpha=.45f),style=Stroke(w*.012f));drawContext.canvas.restore()
                }
                4 -> { // Cut gemstone facets.
                    for(p in listOf(Offset(w*.5f,h*.08f),Offset(w*.89f,h*.40f),Offset(w*.5f,h*.94f),Offset(w*.11f,h*.40f)))
                        drawLine(color.copy(alpha=.7f),p,Offset(w*.5f,h*.47f),w*.015f)
                    drawLine(color,Offset(w*.11f,h*.40f),Offset(w*.89f,h*.40f),w*.015f)
                }
                5 -> { // Onyx rune octagon, with four pointed crystals.
                    for(i in 0..3) {
                        val a=i*Math.PI/2
                        val x=w*.5f+cos(a).toFloat()*w*.40f
                        val y=h*.49f+sin(a).toFloat()*h*.40f
                        spark(x,y,w*.06f,color)
                    }
                    drawCircle(color.copy(alpha=.5f),w*.29f,Offset(w*.5f,h*.49f),style=Stroke(w*.014f))
                }
                6 -> { // Imperial crown, golden double trim, red central shield.
                    val crown=Path().apply {
                        moveTo(w*.30f,h*.24f);lineTo(w*.25f,h*.08f);lineTo(w*.40f,h*.15f)
                        lineTo(w*.5f,h*.01f);lineTo(w*.60f,h*.15f);lineTo(w*.75f,h*.08f)
                        lineTo(w*.70f,h*.24f);close()
                    }
                    drawPath(crown,accent);drawPath(outline,accent.copy(alpha=.25f),style=Stroke(w*.075f))
                    spark(w*.5f,h*.82f,w*.06f,accent)
                }
            }
            if(unlocked && rank>=4) {
                clipPath(outline) {
                    val x=(phase.value*1.8f-.4f)*w
                    val shine=Path().apply {
                        moveTo(x-w*.18f,0f);lineTo(x+w*.02f,0f)
                        lineTo(x+w*.38f,h);lineTo(x+w*.18f,h);close()
                    }
                    drawPath(shine,Color.White.copy(alpha=.10f+glow*.12f))
                }
                if(rank>=5) repeat(if(rank==6)3 else 2) { i ->
                    val a=(phase.value+i/3f)*Math.PI*2
                    val x=w*.5f+cos(a).toFloat()*w*.41f
                    val y=h*.5f+sin(a).toFloat()*h*.39f
                    spark(x,y,w*(.025f+.018f*glow),accent.copy(alpha=.55f+.4f*glow))
                }
            }
        }
        Icon(icon,null,tint=if(unlocked) accent else color,modifier=Modifier.size(18.dp))
        if(!unlocked) Icon(Icons.Default.Lock,null,tint=Color(0xFFB9CACB),modifier=Modifier.align(Alignment.BottomEnd).size(12.dp))
    }
}

private fun emblemOutline(level: Int,w:Float,h:Float)=Path().apply {
    fun polygon(vararg points:Pair<Float,Float>) {
        moveTo(points[0].first*w,points[0].second*h)
        points.drop(1).forEach { lineTo(it.first*w,it.second*h) };close()
    }
    when(level) {
        0 -> polygon(.19f to .19f,.81f to .19f,.76f to .58f,.5f to .90f,.24f to .58f)
        1 -> {
            moveTo(w*.16f,h*.28f);quadraticTo(w*.5f,-h*.04f,w*.84f,h*.28f)
            lineTo(w*.80f,h*.61f);quadraticTo(w*.72f,h*.85f,w*.5f,h*.93f)
            quadraticTo(w*.28f,h*.85f,w*.20f,h*.61f);close()
        }
        2 -> polygon(.28f to .21f,.72f to .21f,.89f to .44f,.78f to .71f,.5f to .94f,.22f to .71f,.11f to .44f)
        3 -> polygon(.5f to .10f,.74f to .28f,.70f to .69f,.5f to .94f,.30f to .69f,.26f to .28f)
        4 -> polygon(.5f to .06f,.89f to .40f,.71f to .73f,.5f to .96f,.29f to .73f,.11f to .40f)
        5 -> polygon(.35f to .10f,.65f to .10f,.83f to .29f,.83f to .66f,.65f to .86f,.5f to .96f,.35f to .86f,.17f to .66f,.17f to .29f)
        else -> {
            moveTo(w*.28f,h*.25f);lineTo(w*.72f,h*.25f);lineTo(w*.74f,h*.61f)
            quadraticTo(w*.70f,h*.82f,w*.5f,h*.96f)
            quadraticTo(w*.30f,h*.82f,w*.26f,h*.61f);close()
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.spark(x:Float,y:Float,r:Float,color:Color) {
    val p=Path().apply {moveTo(x,y-r);lineTo(x+r*.4f,y-r*.3f);lineTo(x+r,y)
        lineTo(x+r*.3f,y+r*.3f);lineTo(x,y+r);lineTo(x-r*.3f,y+r*.3f)
        lineTo(x-r,y);lineTo(x-r*.3f,y-r*.3f);close()}
    drawPath(p,color)
}
