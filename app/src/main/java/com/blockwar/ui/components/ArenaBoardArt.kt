package com.blockwar.ui.components

import com.blockwar.model.PlayerState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/** Quiet playing surface; the photographic scenery belongs behind the whole arena. */
object ArenaBoardArt {
    fun supports(id: String) = id in setOf("real_fundicao", "real_catedral", "real_caverna")

    fun DrawScope.render(id: String, phase: Float = 0f) {
        val accent = CosmeticArt.accent(id)
        val cell = size.width / PlayerState.BOARD_COLS
        drawRect(Color(0xFF080D14))
        drawRect(Brush.horizontalGradient(listOf(accent.copy(alpha=.07f), Color.Transparent, accent.copy(alpha=.07f))))
        fun rail(start: Offset, end: Offset) {
            drawLine(accent.copy(alpha=.055f), start, end, (cell*.09f).coerceAtLeast(1f))
            drawLine(accent.copy(alpha=.22f), start, end, (cell*.025f).coerceAtLeast(.5f))
        }
        for (x in 1 until PlayerState.BOARD_COLS) rail(Offset(x*cell,0f),Offset(x*cell,size.height))
        for (y in 1 until PlayerState.BOARD_ROWS) rail(Offset(0f,y*cell),Offset(size.width,y*cell))
        // Small accents stay on the perimeter, never over the falling pieces.
        for (row in 0 until PlayerState.BOARD_ROWS) {
            val y = row*cell
            for (x in listOf(0f,size.width-cell*.12f)) {
                when (id) {
                    "real_fundicao" -> {
                        drawLine(Color(0xFFFF5A22).copy(alpha=.6f),Offset(x+cell*.04f,y+cell*.82f),Offset(x+cell*.07f,y+cell*.32f),cell*.04f)
                        drawLine(Color(0xFFFFC36A).copy(alpha=.65f),Offset(x+cell*.07f,y+cell*.67f),Offset(x+cell*.09f,y+cell*.48f),cell*.025f)
                    }
                    "real_catedral" -> {
                        drawLine(accent.copy(alpha=.65f),Offset(x,y+cell*.2f),Offset(x+cell*.1f,y+cell*.6f),cell*.025f)
                        drawLine(Color.White.copy(alpha=.35f),Offset(x+cell*.1f,y+cell*.6f),Offset(x+cell*.025f,y+cell*.8f),cell*.02f)
                    }
                    else -> drawRect(accent.copy(alpha=.55f),Offset(x,y+cell*.45f),Size(cell*.08f,cell*.16f))
                }
            }
        }
        if(id=="real_fundicao")with(ForgeFire){border(phase,cell*.38f)}
    }
}
