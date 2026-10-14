package com.blockwar.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import com.blockwar.model.Cell
import com.blockwar.model.PieceType
import kotlin.math.min
import kotlin.math.sin

/** Cosmetic display only: separate material samples, never a playable piece silhouette. */
internal object MaterialCubePreview {
    fun draw(scope:DrawScope,id:String,phase:Float)=with(scope) {
        val r=min(size.width/6.4f,size.height/3.5f)
        if(r<=0f)return@with
        val dx=(size.width-6.4f*r)/2;val dy=(size.height-3.5f*r)/2
        val forge=id=="real_obsidiana"||id=="real_magma"
        val edge=if(forge)Color(0xFFFFAE58) else CosmeticArt.accent(id)
        fun cube(cx:Float,cy:Float,s:Float) {
            drawOval(Color.Black.copy(alpha=.5f),Offset(cx-s*1.1f,cy+s*1.8f),Size(s*2.2f,s*.42f))
            fun face(x:Float,y:Float,ux:Float,uy:Float,vx:Float,vy:Float,shade:Float) {
                val matrix=Matrix().apply{this[0,0]=ux;this[0,1]=uy;this[1,0]=vx;this[1,1]=vy;this[3,0]=x;this[3,1]=y}
                withTransform({transform(matrix)}) {
                    drawRect(edge.copy(alpha=.9f),Offset.Zero,Size(s,s))
                    if(id.startsWith("real_")) {
                        RealisticAssets.material(this,id,Offset.Zero,s,1f)
                        if(forge) {
                            val glow=.7f+.25f*sin(phase*6.283f)
                            val crack=Path().apply{moveTo(s*.32f,0f);lineTo(s*.48f,s*.30f);lineTo(s*.27f,s*.51f);lineTo(s*.67f,s*.89f);lineTo(s*.61f,s)}
                            drawPath(crack,Color(0xFFFF651C).copy(alpha=glow*.35f),style=Stroke(s*.12f))
                            drawPath(crack,Color(0xFFFFC36D).copy(alpha=glow),style=Stroke(s*.025f))
                        }
                    } else PixelBlockDrawer.drawPixelBlock(this,Offset.Zero,s,Cell.fromPiece(PieceType.LINE_3),skinId=id,firePhase=phase)
                    drawRect(Color.Black.copy(alpha=shade),Offset.Zero,Size(s,s))
                }
            }
            face(cx-s,cy+s*.5f,1f,.5f,0f,1f,.24f)
            face(cx,cy+s,1f,-.5f,0f,1f,.10f)
            face(cx-s,cy+s*.5f,1f,-.5f,1f,.5f,0f)
            val outline=Path().apply{moveTo(cx,cy);lineTo(cx+s,cy+s*.5f);lineTo(cx+s,cy+s*1.5f);lineTo(cx,cy+s*2f);lineTo(cx-s,cy+s*1.5f);lineTo(cx-s,cy+s*.5f);close()}
            drawPath(outline,Color(0xFF0A0D14),style=Stroke(s*.055f))
            drawLine(edge.copy(alpha=.75f),Offset(cx-s,cy+s*.5f),Offset(cx,cy+s),s*.025f)
            drawLine(Color.White.copy(alpha=.45f),Offset(cx,cy),Offset(cx+s,cy+s*.5f),s*.025f)
            drawLine(edge.copy(alpha=.7f),Offset(cx,cy+s),Offset(cx+s,cy+s*.5f),s*.02f)
            drawLine(edge.copy(alpha=.5f),Offset(cx,cy+s),Offset(cx,cy+s*2f),s*.02f)
        }
        cube(dx+3.05f*r,dy+.08f*r,r)
        cube(dx+1.15f*r,dy+1.20f*r,r*.85f)
        cube(dx+5.05f*r,dy+1.30f*r,r*.85f)
    }
}