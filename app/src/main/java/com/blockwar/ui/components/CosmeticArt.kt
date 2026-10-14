package com.blockwar.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.Alignment
import androidx.compose.runtime.*
import androidx.compose.animation.core.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import com.blockwar.model.*
import kotlin.math.*

/** Shared artwork: the shop shows the same drawing that is used in the game. */
object CosmeticArt {
    fun index(id: String): Int = com.blockwar.economy.ShopCatalog.items
        .filter { it.itemCategory == com.blockwar.economy.ShopCatalog.find(id)?.itemCategory }
        .indexOfFirst { it.id == id }.coerceAtLeast(0)

    fun accent(id: String): Color = when {
        id.contains("forja") || id.contains("fagulhas") || id.contains("fundicao") -> Color(0xFFFFB05C)
        id.contains("glacial") || id.contains("catedral") -> Color(0xFF9BE4FF)
        id.contains("cristal") || id.contains("prismas") || id.contains("caverna") -> Color(0xFFCCAAFF)
        id.contains("ouro") || id.contains("gold") || id.contains("gladiador") -> Color(0xFFFFD760)
        id.contains("gelo") || id.contains("cristal") -> Color(0xFF9CEBFF)
        id.contains("fogo") || id.contains("vulcan") || id.contains("chamas") || id.contains("meteor") -> Color(0xFFFF774B)
        id.contains("floresta") || id.contains("matrix") || id.contains("aurora") -> Color(0xFF64F5AC)
        id.contains("madeira") || id.contains("templo") || id.contains("ruinas") -> Color(0xFFCDA878)
        id.contains("sombrio") || id.contains("portal") || id.contains("galaxia") -> Color(0xFFC786FF)
        else -> listOf(Color(0xFF55E7FF), Color(0xFFEC8EFF), Color(0xFF84ACFF), Color(0xFFFF9EBD))[index(id) % 4]
    }

    fun DrawScope.scene(id: String, phase: Float = 0f, board: Boolean = false) {
        if (board && ArenaBoardArt.supports(id)) {
            clipRect { with(ArenaBoardArt) { render(id,phase) } }
            return
        }
        if(RealisticAssets.draw(this,id)) {
            drawRect(Color.Black.copy(alpha=if(board).24f else .12f))
            return
        }
        val c = accent(id); val w = size.width; val h = size.height
        val n = index(id)
        drawRect(Brush.verticalGradient(listOf(Color(0xFF07101C), c.copy(alpha = .17f), Color(0xFF03060F))))
        when {
            id.contains("tempestade") -> {
                for (i in 0..4) {
                    val x = (i + .5f) * w / 5
                    val p = Path().apply {moveTo(x,h*.1f);lineTo(x-w*.04f,h*.4f);lineTo(x+w*.03f,h*.4f);lineTo(x-w*.05f,h*.85f)}
                    drawPath(p,c.copy(alpha=.55f+.3f*sin(phase*6+i)),style=Stroke(2f))
                }
            }
            id.contains("explosao_estelar") || id.contains("supernova") -> {
                val center=Offset(w*.5f,h*.5f)
                for (i in 0..23) {
                    val a=i*PI.toFloat()/12
                    val end=center+Offset(cos(a)*w*.4f,sin(a)*h*.4f)
                    drawLine(c.copy(alpha=.45f),center,end,if(i%3==0) 3f else 1f)
                }
                drawCircle(c,w*.06f,center)
            }
            id.contains("galaxia") -> {
                for (arm in 0..2) {
                    val p=Path()
                    for (i in 0..100) {
                        val a=i*.08f+arm*PI.toFloat()*2/3+phase
                        val x=w*.5f+cos(a)*w*.004f*i; val y=h*.5f+sin(a)*h*.003f*i
                        if(i==0)p.moveTo(x,y) else p.lineTo(x,y)
                    }
                    drawPath(p,c.copy(alpha=.5f),style=Stroke(2f))
                }
            }
            id.contains("cidade") || id.contains("paisagem") || id.contains("arena") || id.contains("templo") || id.contains("ruinas") -> {
                for (i in 0..12) {
                    val x = w * i / 13; val bh = h * (.15f + ((i * 7 + n) % 9) / 25f)
                    drawRect(c.copy(alpha = .16f), Offset(x, h - bh), Size(w / 15, bh))
                    for (j in 1..5) drawLine(c.copy(alpha = .55f), Offset(x + w / 80, h - bh + j * bh / 6), Offset(x + w / 22, h - bh + j * bh / 6), 1f)
                }
            }
            id.contains("floresta") || id.contains("caverna") || id.contains("cristal") -> {
                for (i in 0..10) {
                    val x = i * w / 10
                    val p = Path().apply { moveTo(x, h); lineTo(x + w / 22, h * (.2f + (i % 4) * .12f)); lineTo(x + w / 9, h); close() }
                    drawPath(p, c.copy(alpha = .25f)); drawPath(p, c.copy(alpha = .45f), style = Stroke(1f))
                }
            }
            id.contains("matrix") -> for (i in 0..14) for (j in 0..8) {
                val y = ((j / 9f + phase + (i % 3) / 12f) % 1f) * h
                drawLine(c.copy(alpha = .25f + j / 15f), Offset(i * w / 15, y), Offset(i * w / 15, y + h / 28), 2f)
            }
            id.contains("abismo") || id.contains("aurora") || id.contains("sunset") -> for (i in 0..8) {
                val path = Path()
                for (x in 0..40) {
                    val px = x * w / 40; val y = h * (.2f + i * .07f) + sin(x * .18f + phase * 6 + i) * h * .08f
                    if (x == 0) path.moveTo(px, y) else path.lineTo(px, y)
                }
                drawPath(path, c.copy(alpha = .25f), style = Stroke(h / 65))
            }
            else -> {
                for (i in 0..34) {
                    val x = ((i * 37 + n * 19) % 101) / 101f * w
                    val y = ((i * 53 + n * 11) % 103) / 103f * h
                    drawCircle(c.copy(alpha = .3f + (i % 3) * .2f), 1f + i % 3, Offset(x, y))
                    if (id.contains("meteor")) drawLine(c.copy(alpha = .4f), Offset(x, y), Offset(x - w / 15, y - h / 12), 2f)
                }
                val center = Offset(w * .5f, h * .45f)
                for (i in 1..4) drawOval(c.copy(alpha = .12f + i * .03f), Offset(center.x - w * i / 12, center.y - h * i / 17), Size(w * i / 6, h * i / 8), style = Stroke(1f + i))
            }
        }
        if (board) drawRect(Color.Black.copy(alpha = .48f)) // Keep pieces and garbage legible.
    }

    fun DrawScope.avatar(id: String, phase:Float=0f) {
        if(id=="avatar_forja" && RealisticAssets.draw(this,id)) {
            with(ForgeFire){border(phase,size.minDimension*.09f)}
            return
        }
        val c = accent(id); val w = size.width; val h = size.height
        drawRect(Color(0xFF0B1120))
        val helmet = Path().apply {
            moveTo(w * .18f, h * .8f); lineTo(w * .12f, h * .38f)
            lineTo(w * .32f, h * .15f); lineTo(w * .68f, h * .15f)
            lineTo(w * .88f, h * .38f); lineTo(w * .82f, h * .8f); close()
        }
        drawPath(helmet, c.copy(alpha = .25f)); drawPath(helmet, c, style = Stroke(w / 30))
        if (id.contains("mago")) {
            drawPath(Path().apply {moveTo(w * .12f, h * .4f); lineTo(w * .5f, h * .02f); lineTo(w * .88f, h * .4f); close()}, c)
        }
        if (id.contains("dragao") || id.contains("cavaleiro")) {
            for (x in listOf(.18f, .82f)) drawLine(c, Offset(w * x, h * .35f), Offset(w * x, h * .04f), w * .08f)
        }
        for (x in listOf(.3f, .6f)) drawRect(c, Offset(w * x, h * .44f), Size(w * .13f, h * .08f))
        if (id.contains("ciborgue")) drawCircle(Color(0xFFFF596B), w * .1f, Offset(w * .66f, h * .48f))
        if (id.contains("ninja")) drawLine(c, Offset(w * .15f, h * .6f), Offset(w * .85f, h * .6f), h * .09f)
        else drawLine(c.copy(alpha = .7f), Offset(w * .36f, h * .7f), Offset(w * .64f, h * .7f), h / 30)
        if(id=="avatar_forja") {
            drawPath(helmet,Brush.linearGradient(listOf(Color(0xFF73737A),Color(0xFF25252B),Color(0xFF06090F))))
            drawPath(helmet,Color(0xFFFF8A32),style=Stroke(w*.025f))
            drawLine(Color(0xFFFFE6A0),Offset(w*.25f,h*.45f),Offset(w*.75f,h*.45f),h*.06f)
            with(ForgeFire){border(phase,h*.2f)}
        }
    }

    fun DrawScope.frame(id: String, phase: Float = 0f) {
        if(id.startsWith("season_frame_")) {
            val edge=size.minDimension*.09f;val c=accent(id)
            drawRoundRect(Brush.linearGradient(listOf(Color.White,c,Color(0xFF27313B),c)),
                Offset(edge/2,edge/2),Size(size.width-edge,size.height-edge),CornerRadius(edge,edge),style=Stroke(edge))
            drawRoundRect(Color.White.copy(alpha=.6f),Offset(edge,edge),Size(size.width-edge*2,size.height-edge*2),CornerRadius(edge/2,edge/2),style=Stroke(edge*.16f))
            for(x in listOf(edge,size.width-edge))for(y in listOf(edge,size.height-edge)) {
                drawCircle(Color(0xFF303846),edge*.35f,Offset(x,y));drawCircle(c,edge*.19f,Offset(x,y))
            }
            if(id=="season_frame_forja")with(ForgeFire){border(phase,edge*1.6f)}
            return
        }
        val c = accent(id); val w = size.width; val h = size.height; val edge = min(w,h) * .035f
        drawRect(c, Offset(edge, edge), Size(w - edge * 2, h - edge * 2), style = Stroke(edge))
        val n = index(id)
        for (i in 0..11) {
            val x = (i + .5f) * w / 12
            val radius = edge * (1.2f + .4f * sin(phase * 6 + i))
            if (id.contains("chamas")) {
                drawPath(Path().apply {moveTo(x - edge, h); lineTo(x, h - edge * (4 + i % 3)); lineTo(x + edge, h); close()}, c)
            } else if (id.contains("teia")) {
                drawLine(c.copy(alpha = .55f), Offset(x, 0f), Offset(w * .5f, h * .5f), edge / 3)
            } else if (id.contains("correntes")) {
                drawOval(c, Offset(x - edge, 0f), Size(edge * 2, edge * 3), style = Stroke(edge / 2))
            } else drawCircle(c.copy(alpha = .9f), radius, Offset(x, if (n % 2 == 0) edge else h - edge))
        }
    }

    fun DrawScope.effect(id: String, phase: Float) {
        if(id=="real_fagulhas") {
            with(ForgeFire){strip(Offset(size.width*.12f,size.height*.8f),size.width*.76f,size.height*.65f,phase,9)}
            return
        }
        val c = accent(id); val center = Offset(size.width / 2, size.height / 2)
        val radius = min(size.width, size.height) * .43f
        when (id) {
            "anim_laser" -> {
                val y = size.height * phase
                drawLine(c.copy(alpha = .3f), Offset(0f,y), Offset(size.width,y), 12f)
                drawLine(Color.White, Offset(0f,y), Offset(size.width,y), 2f)
                return
            }
            "anim_glitch" -> {
                for (i in 0..19) drawRect(c.copy(alpha=.7f),Offset(size.width*((i*13+ (phase*20).toInt())%23)/23,size.height*(i%7)/7),Size(size.width/8,3f+i%4))
                return
            }
            "animacao_entrada_triunfal" -> {
                val y = size.height * (1f-phase)
                for (i in 0..2) {
                    val p = Path().apply { moveTo(center.x-radius*.4f,y+i*12);lineTo(center.x,y-radius*.35f+i*12);lineTo(center.x+radius*.4f,y+i*12) }
                    drawPath(p,c.copy(alpha=1f-phase*.5f),style=Stroke(3f))
                }
                return
            }
            "animacao_derrota_dramatica" -> {
                for (i in 0..15) drawRect(Color(0xFFFF718F).copy(alpha=1f-phase),Offset(size.width*(i%5)/5,size.height*((i/5f/4+phase)%1)),Size(8f,12f))
                return
            }
            "animacao_level_up_brilhante" -> {
                val r = radius*(.6f+phase*.4f)
                val p=Path().apply {moveTo(center.x,center.y-r);lineTo(center.x+r,center.y);lineTo(center.x,center.y+r);lineTo(center.x-r,center.y);close()}
                drawPath(p,Color(0xFFFFD760).copy(alpha=1f-phase*.5f),style=Stroke(3f))
                return
            }
            "animacao_combo_devastador" -> {
                val p=Path().apply {moveTo(center.x+radius*.3f,center.y-radius);lineTo(center.x-radius*.3f,center.y);lineTo(center.x+radius*.2f,center.y);lineTo(center.x-radius*.3f,center.y+radius)}
                drawPath(p,c,style=Stroke(5f))
            }
            "animacao_vitoria_espetacular" -> {
                val p=Path().apply {moveTo(center.x-radius*.5f,center.y);lineTo(center.x-radius*.65f,center.y-radius*.5f);lineTo(center.x-radius*.2f,center.y-radius*.2f);lineTo(center.x,center.y-radius*.8f);lineTo(center.x+radius*.2f,center.y-radius*.2f);lineTo(center.x+radius*.65f,center.y-radius*.5f);lineTo(center.x+radius*.5f,center.y);close()}
                drawPath(p,Color(0xFFFFD760).copy(alpha=.85f))
            }
        }
        if (id.contains("onda") || id.contains("plasma") || id.contains("supernova")) {
            for (i in 0..2) drawCircle(c.copy(alpha = .8f - i * .2f), radius * ((phase + i / 3f) % 1f).coerceAtLeast(.02f), center, style = Stroke(2f))
        }
        for (i in 0..19) {
            val angle = i * PI.toFloat() / 10
            val travel = ((phase + i * .033f) % 1f) * radius
            val p = center + Offset(cos(angle) * travel, sin(angle) * travel)
            val col = if (id.contains("confetes")) listOf(c, Color(0xFFFFD760), Color(0xFFFF73BE))[i % 3] else c
            if (id.contains("celestial") || id.contains("sparks")) {
                drawLine(col, p - Offset(4f, 0f), p + Offset(4f, 0f), 2f)
                drawLine(col, p - Offset(0f, 4f), p + Offset(0f, 4f), 2f)
            } else if (id.contains("coins")) drawCircle(Color(0xFFFFD760), 4f, p)
            else drawRect(col.copy(alpha = 1f - travel / radius * .7f), p, Size(4f + i % 3, 4f + i % 4))
        }
    }
}

@Composable
fun CosmeticScene(id: String, modifier: Modifier = Modifier, board: Boolean = false) {
    PrepareRealisticAsset(id)
    val phase=rememberForgePhase(id=="real_fundicao"||id=="fundo_forja")
    Canvas(modifier) { with(CosmeticArt) { scene(id,phase, board = board) };if(!board&&id=="fundo_forja")with(ForgeFire){border(phase,size.minDimension*.07f)} }
}

@Composable
fun CosmeticThumbnail(item: ShopItem, modifier: Modifier = Modifier) {
    PrepareRealisticAsset(item.id)
    val transition = rememberInfiniteTransition(label = "shopPreview")
    val phase by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "phase")
    when (item.itemCategory) {
        "blocos" -> Canvas(modifier) { MaterialCubePreview.draw(this,item.id,phase) }
        "avatares" -> Box(modifier, contentAlignment=Alignment.Center) {
            AvatarImage(item.id, Modifier.fillMaxHeight().aspectRatio(1f))
        }
        "molduras" -> Box(modifier, contentAlignment=Alignment.Center) {
            if(item.id.startsWith("season_frame_"))Canvas(Modifier.fillMaxHeight().aspectRatio(1f)) { with(CosmeticArt){frame(item.id,phase)} }
            else AvatarWithFrame("avatar_default", item.id, androidx.compose.ui.unit.Dp(64f), Modifier.fillMaxHeight().aspectRatio(1f))
        }
        else -> Canvas(modifier) {
            with(CosmeticArt) {
                when (item.itemCategory) {
                    "tabuleiros", "fundos", "banners" -> {
                        if (item.itemCategory == "banners") { with(BannerArt) { render(item.id, phase) } }
                        else scene(item.id, phase, item.itemCategory == "tabuleiros")
                        if (item.itemCategory == "tabuleiros" && !ArenaBoardArt.supports(item.id)) {
                            val s = min(size.width / PlayerState.BOARD_COLS, size.height / PlayerState.BOARD_ROWS)
                            val left = (size.width - s * PlayerState.BOARD_COLS) / 2
                            for (x in 0..PlayerState.BOARD_COLS) drawLine(accent(item.id).copy(alpha = .25f), Offset(left + x * s, 0f), Offset(left + x * s, size.height), 1f)
                            for (y in 0..PlayerState.BOARD_ROWS) drawLine(accent(item.id).copy(alpha = .25f), Offset(left, y * s), Offset(left + s * PlayerState.BOARD_COLS, y * s), 1f)
                            for (x in 0..3) drawRect(Color(0xFF55E7FF), Offset(left + (x + 3) * s, size.height * .5f), Size(s - 1, s - 1))
                        }
                    }
                    "musicas" -> {
                        val n = index(item.id)
                        for (i in 0..19) {
                            val height = size.height * (.15f + .65f * abs(sin(i * (.3f + n * .11f) + phase * 6)))
                            drawRect(accent(item.id), Offset(size.width * (i + 1) / 22, (size.height - height) / 2), Size(size.width / 32, height))
                        }
                    }
                    "animacoes" -> {
                        val h=size.height*.35f; val y=(size.height-h)/2
                        for(i in 0 until PlayerState.BOARD_COLS) drawRect(Color(0xFF55E7FF).copy(alpha=1-phase),Offset(i*size.width/PlayerState.BOARD_COLS,y),Size(size.width/PlayerState.BOARD_COLS-1,h))
                        with(LineClearArt) {render(item.id,y,size.width,h,phase)}
                    }
                    else -> effect(item.id, phase)
                }
            }
        }
    }
}

/** Retained for source compatibility; line animations now belong exclusively to BoardCanvas. */
@Composable
fun CosmeticEventOverlay(state: VersusGameState, modifier: Modifier = Modifier) { }
