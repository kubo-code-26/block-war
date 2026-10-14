package com.blockwar.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import kotlin.math.*

/** Every animation is a 200 ms line-clear effect, shared by gameplay and the shop preview. */
object LineClearArt {
    fun DrawScope.render(id: String, y: Float, width: Float, height: Float, progress: Float) {
        val p=progress.coerceIn(0f,1f); val c=CosmeticArt.accent(id); val center=Offset(width/2,y+height/2)
        if(id=="season_anim_forja") {
            clipRect(0f,y-height*.8f,width,y+height) {
                drawRect(Brush.verticalGradient(listOf(Color(0xFFFFB64D),Color(0xFF501408),Color.Black),y,y+height),Offset(0f,y),Size(width,height),alpha=sin(p*PI.toFloat())*.7f)
                with(ForgeFire){strip(Offset(0f,y+height*(1-p)),width,height*(.8f+p),p,24,1-p)}
                for(i in 0..19) {
                    val x=width*(i+.5f)/20
                    drawCircle(Color(0xFFFFD27C).copy(alpha=1-p),height*.035f,Offset(x+sin(i+p*8)*height*.1f,y-height*p*(.5f+i%3*.2f)))
                }
            }
            return
        }
        if(id.startsWith("season_anim_")) {
            for(i in 0..23) {
                val x=width*(i+.5f)/24
                val travel=p*height*2
                val fragment=Path().apply{moveTo(x,y-travel);lineTo(x+height*.22f,y+height*.3f-travel);lineTo(x-height*.1f,y+height*.7f-travel);close()}
                drawPath(fragment,c.copy(alpha=1-p))
                drawLine(Color.White.copy(alpha=(1-p)*.65f),Offset(x,y-travel),Offset(x+height*.1f,y+height*.2f-travel),1.5f)
            }
            return
        }
        clipRect(0f,y,width,y+height) {
            when(id) {
                "anim_laser", "animacao_entrada_triunfal" -> {
                    val x=width*p
                    drawRect(c.copy(alpha=(1-p)*.6f),Offset(0f,y),Size(x,height))
                    drawLine(Color.White,Offset(x,y),Offset(x,y+height),height*.18f)
                }
                "anim_glitch", "animacao_derrota_dramatica" -> {
                    val color=if(id=="anim_glitch") c else Color(0xFFFF6588)
                    for(i in 0..29) {
                        val x=((i*17+(p*70).toInt())%31)/31f*width
                        val py=y+(i%4)*height/4
                        drawRect(color.copy(alpha=1-p),Offset(x,py),Size(width/14,height/5))
                    }
                }
                "anim_supernova" -> {
                    drawOval(c.copy(alpha=1-p),Offset(width*p/2,y),Size(width*(1-p),height),style=Stroke(height*.12f))
                }
                "animacao_vitoria_espetacular" -> {
                    for(i in 0..19) {
                        val x=width*(i+.5f)/20
                        val py=y+height*.5f+sin(i+p*6)*height*.35f
                        drawLine(Color(0xFFFFD760).copy(alpha=1-p),Offset(x-3,py),Offset(x+3,py),2f)
                        drawLine(Color.White.copy(alpha=1-p),Offset(x,py-3),Offset(x,py+3),2f)
                    }
                }
                "animacao_level_up_brilhante" -> {
                    for(i in 0..9) {
                        val x=width*(i+.5f)/10;val r=height*.4f*(1-p)
                        val path=Path().apply {moveTo(x,center.y-r);lineTo(x+r,center.y);lineTo(x,center.y+r);lineTo(x-r,center.y);close()}
                        drawPath(path,Color(0xFF9CEBFF).copy(alpha=1-p),style=Stroke(2f))
                    }
                }
                "animacao_combo_devastador" -> {
                    val path=Path()
                    for(i in 0..20) {val x=i*width/20;val py=center.y+sin(i*2f+p*15)*height*.3f;if(i==0)path.moveTo(x,py)else path.lineTo(x,py)}
                    drawPath(path,c.copy(alpha=1-p),style=Stroke(3f))
                }
                else -> drawRect(Color.White.copy(alpha=sin(p*PI.toFloat())*.65f),Offset(0f,y),Size(width,height))
            }
        }
    }
}

object BannerArt {
    fun DrawScope.render(id: String, phase: Float) {
        with(CosmeticArt) { scene(id,phase) }
        if(id=="season_banner_forja")with(ForgeFire){border(phase,size.minDimension*.32f)}
        val w=size.width;val h=size.height;val c=CosmeticArt.accent(id)
        for(i in 0..17) {
            val x=((i*37)%101)/101f*w
            val y=((i/18f+phase*(.3f+(i%3)*.2f))%1)*h
            when(id) {
                "banner_bot", "banner_padrao" -> {
                    val s=min(w,h)/14
                    drawRect(c.copy(alpha=.35f),Offset(x,y),Size(s,s))
                    drawRect(c.copy(alpha=.35f),Offset(x+s,y),Size(s,s))
                    drawRect(c.copy(alpha=.35f),Offset(x,y+s),Size(s,s))
                }
                "banner_chuva_de_meteoritos" -> drawLine(c,Offset(x,y),Offset(x-w*.04f,y-h*.08f),2f)
                "banner_portal_dimensional", "banner_overclock" -> {
                    val a=phase*2*PI.toFloat()+i*.35f
                    drawCircle(c,2f,Offset(w*.5f+cos(a)*w*.35f,h*.5f+sin(a)*h*.35f))
                }
                else -> drawCircle(c.copy(alpha=.4f+.4f*sin(phase*6+i).pow(2)),1f+i%3,Offset(x,y))
            }
        }
    }
}

@Composable
fun AnimatedBanner(id: String, modifier: Modifier = Modifier) {
    PrepareRealisticAsset(id)
    val transition=rememberInfiniteTransition(label="bannerArt")
    val phase by transition.animateFloat(0f,1f,infiniteRepeatable(tween(6000,easing=LinearEasing)),label="bannerMotion")
    Canvas(modifier) { with(BannerArt) {render(id,phase)} }
}
