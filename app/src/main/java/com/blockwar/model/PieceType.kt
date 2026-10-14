package com.blockwar.model

import androidx.compose.ui.graphics.Color
import kotlin.random.Random

enum class PieceType(
    val blockCount: Int,
    val primaryColor: Color,
    val highlightColor: Color,
    val shadowColor: Color
) {
    LINE_1(
        1,
        Color(0xFFFFD600),
        Color(0xFFFFF176),
        Color(0xFFFBC02D)
    ),
    LINE_2(
        2,
        Color(0xFF00E676),
        Color(0xFF69F0AE),
        Color(0xFF00C853)
    ),
    LINE_3(
        3,
        Color(0xFF00E5FF),
        Color(0xFF18FFFF),
        Color(0xFF00B8D4)
    ),
    LINE_4(
        4,
        Color(0xFFFF1744),
        Color(0xFFFF5252),
        Color(0xFFD50000)
    );

    fun getShape(rotation: Int): List<List<Int>> {
        val base = when (this) {
            LINE_1 -> listOf(listOf(1))
            LINE_2 -> listOf(listOf(1, 1))
            LINE_3 -> listOf(listOf(1, 1, 1))
            LINE_4 -> listOf(listOf(1, 1, 1, 1))
        }
        
        return if (rotation % 2 == 0) {
            base
        } else {
            // Rotate 90 degrees: from horizontal to vertical
            List(base[0].size) { listOf(1) }
        }
    }

    companion object {
        val ALL_PIECES = values()
        fun getRandomPiece(): PieceType = ALL_PIECES[Random.nextInt(ALL_PIECES.size)]
    }
}
