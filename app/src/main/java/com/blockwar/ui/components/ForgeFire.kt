package com.blockwar.ui.components

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import kotlin.math.*

/** Layered, curved flame tongues: visual only, bounded and independent of game randomness. */
object ForgeFire {
    fun phase(): Float = (android.os.SystemClock.uptimeMillis()%2400L)/2400f
    private val ember=Color(0xFFEF3D0A)
    private val orange=Color(0xFFFF871C)
    private val core=Color(0xFFFFE9A2)
    fun DrawScope.strip(origin:Offset,length:Float,depth:Float,phase:Float,count:Int=12,alpha:Float=1f) {
        if(length<=0f||depth<=0f)return
        val step=length/count
        for(i in 0 until count) {
            val t=phase*2*PI.toFloat()+i*2.399f
            val x=origin.x+(i+.5f)*step
            val reach=depth*(.52f+.25f*sin(t)+.16f*sin(t*2+i))
            val sway=sin(t+i*.4f)*step*.32f
            for(layer in 0..2) {
                val scale=1f-layer*.23f
                val w=step*(.64f-layer*.13f)
                val h=reach*scale
                val flame=Path().apply {
                    moveTo(x-w,origin.y)
                    cubicTo(x-w,origin.y-h*.45f,x+sway-w*.6f,origin.y-h*.62f,x+sway,origin.y-h)
                    cubicTo(x+sway+w*.15f,origin.y-h*.5f,x+w,origin.y-h*.36f,x+w,origin.y)
                    close()
                }
                val shade=when(layer){0->ember;1->orange;else->core}
                // Fade both the tips and the sides instead of opaque, outlined triangles.
                val glow=Brush.radialGradient(listOf(shade.copy(alpha=.8f),shade.copy(alpha=.34f),Color.Transparent),
                    center=Offset(x,origin.y-h*.08f),radius=max(h,w)*1.04f)
                drawPath(flame,glow,alpha=alpha)
                if(layer==1) {
                    val wisp=Path().apply {
                        moveTo(x,origin.y-h*.16f)
                        cubicTo(x-w*.28f,origin.y-h*.46f,x+sway+w*.15f,origin.y-h*.65f,x+sway,origin.y-h*.86f)
                    }
                    drawPath(wisp,Brush.verticalGradient(listOf(Color.Transparent,core.copy(alpha=.38f),orange.copy(alpha=.18f)),origin.y-h,origin.y),
                        alpha=alpha,style=Stroke((w*.12f).coerceAtLeast(.4f),cap=StrokeCap.Round))
                }
            }
        }
    }
    fun DrawScope.border(phase:Float,depth:Float=size.minDimension*.11f,alpha:Float=1f) {
        clipRect {
            val w=size.width;val h=size.height
            drawRect(orange.copy(alpha=.18f*alpha),style=Stroke(depth*.7f))
            strip(Offset(0f,h),w,depth,phase,12,alpha)
            withTransform({translate(w,h);rotate(-90f,Offset.Zero)}){strip(Offset.Zero,h,depth,phase+.17f,20,alpha)}
            withTransform({rotate(90f,Offset.Zero)}){strip(Offset.Zero,h,depth,phase+.41f,20,alpha)}
            withTransform({translate(w,0f);rotate(180f,Offset.Zero)}){strip(Offset.Zero,w,depth,phase+.63f,12,alpha)}
        }
    }
    fun DrawScope.cell(origin:Offset,side:Float,phase:Float,alpha:Float) {
        clipRect(origin.x,origin.y,origin.x+side,origin.y+side) {
            drawRoundRect(orange.copy(alpha=alpha*(.48f+.18f*sin(phase*2*PI.toFloat()))),
                origin+Offset(side*.055f,side*.055f),Size(side*.89f,side*.89f),CornerRadius(side*.08f),style=Stroke(side*.055f))
            strip(origin+Offset(side*.08f,side*.24f),side*.84f,side*.21f,phase,3,alpha)
            strip(origin+Offset(side*.08f,side*.96f),side*.84f,side*.14f,phase+.3f,3,alpha*.8f)
        }
    }
}

@Composable
fun rememberForgePhase(enabled:Boolean):Float {
    if(!enabled)return 0f
    val transition=rememberInfiniteTransition(label="forgeFlames")
    val phase by transition.animateFloat(0f,1f,infiniteRepeatable(tween(2400,easing=LinearEasing)),label="forgeFlameFlow")
    return phase
}
