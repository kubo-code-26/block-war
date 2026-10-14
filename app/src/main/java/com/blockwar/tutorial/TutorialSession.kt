package com.blockwar.tutorial

import com.blockwar.engine.BlockWarEngine
import com.blockwar.model.BlockPiece
import com.blockwar.model.PlayerState
import com.blockwar.model.Cell
import com.blockwar.model.PieceType

/** Isolated practice board: never submits matches, coins, missions or statistics. */
data class TutorialSession(
    val lesson: Int = 0,
    val x: Int = 3,
    val rotation: Int = 0,
    val completed: Boolean = false,
    val retry: Boolean = false
) {
    val type: PieceType get() = when (lesson) { 0 -> PieceType.LINE_2; 1 -> PieceType.LINE_3; else -> PieceType.LINE_4 }
    val piece: BlockPiece get() = BlockPiece(type, x, 0, rotation)
    val board: List<List<Cell?>> get() = List(PlayerState.BOARD_ROWS) { row -> List(PlayerState.BOARD_COLS) { col ->
        val filled = when (lesson) {
            0 -> row == 19 && col !in 6..7
            1 -> row in 17..19 && col != 5
            else -> row == 19 && col !in 3..6
        }
        if (filled) Cell.fromPiece(PieceType.LINE_1) else null
    } }
    val landing: BlockPiece get() = BlockWarEngine.calculateGhost(board, piece)
    private val result get() = BlockWarEngine.lockPiece(board, landing)
    val displayBoard get() = if (completed) result.newBoard else board
    val clearedLines: Int get() = if (completed) result.clearedRows.size else 0
    val attack: Int get() = BlockWarEngine.calculateAttackBlocks(clearedLines)
    fun move(dx: Int): TutorialSession {
        if (completed) return this
        val next = BlockWarEngine.tryMove(board, piece, dx, 0) ?: return this
        return copy(x=next.x, rotation=next.rotation, retry=false)
    }
    fun rotate(): TutorialSession {
        if (completed) return this
        val next = BlockWarEngine.tryRotate(board, piece) ?: return this
        return copy(x=next.x, rotation=next.rotation, retry=false)
    }
    fun drop(): TutorialSession = if (completed) this else if (result.clearedRows.isNotEmpty() && !result.isTopOut)
        copy(completed=true, retry=false) else start(lesson).copy(retry=true)
    fun next(): TutorialSession = if (completed && lesson < 2) start(lesson+1) else this
    companion object {
        fun start(lesson: Int) = TutorialSession(lesson=lesson.coerceIn(0,2), x=if(lesson==2) 0 else 3)
    }
}
