package com.blockwar.engine

import com.blockwar.model.BlockPiece
import com.blockwar.model.Cell
import com.blockwar.model.CpuDifficulty
import com.blockwar.model.PieceType
import com.blockwar.model.PlayerState

class OpponentAI {

    data class MoveDecision(
        val targetX: Int,
        val targetRotation: Int,
        val score: Double
    )

    fun findBestMove(
        board: List<List<Cell?>>,
        pieceType: PieceType,
        difficulty: CpuDifficulty,
        randomInt: (Int) -> Int = {kotlin.random.Random.nextInt(it)}
    ): MoveDecision {
        var bestMove = MoveDecision(PlayerState.BOARD_COLS / 2, 0, -Double.MAX_VALUE)

        for (rot in 0..1) { // 0 and 90 degrees for simple pieces
            for (col in -2 until PlayerState.BOARD_COLS + 2) {
                val testPiece = BlockPiece(type = pieceType, x = col, y = 0, rotation = rot)
                if (!BlockWarEngine.isValidPosition(board, testPiece)) {
                    val testPiece2 = BlockPiece(type = pieceType, x = col, y = -1, rotation = rot)
                    if (!BlockWarEngine.isValidPosition(board, testPiece2)) continue
                }

                val ghost = BlockWarEngine.calculateGhost(board, testPiece)
                val lockResult = BlockWarEngine.lockPiece(board, ghost)
                
                val moveScore = evaluateBoard(lockResult.newBoard)
                if (moveScore > bestMove.score) {
                    bestMove = MoveDecision(col, rot, moveScore)
                }
            }
        }

        // Add some randomness based on difficulty
        if (difficulty == CpuDifficulty.EASY && randomInt(1000000) < 300000) {
             return MoveDecision(randomInt(PlayerState.BOARD_COLS), randomInt(2), 0.0)
        }

        return bestMove
    }

    private fun evaluateBoard(board: List<List<Cell?>>): Double {
        var score = 0.0
        
        // 1. Height penalty
        var maxHeight = 0
        for (c in 0 until PlayerState.BOARD_COLS) {
            for (r in 0 until PlayerState.BOARD_ROWS) {
                if (board[r][c] != null) {
                    val h = PlayerState.BOARD_ROWS - r
                    if (h > maxHeight) maxHeight = h
                    score -= h * 0.5
                    break
                }
            }
        }

        // 2. Holes penalty
        var holes = 0
        for (c in 0 until PlayerState.BOARD_COLS) {
            var foundBlock = false
            for (r in 0 until PlayerState.BOARD_ROWS) {
                if (board[r][c] != null) foundBlock = true
                else if (foundBlock) holes++
            }
        }
        score -= holes * 4.0

        // 3. Complete lines bonus
        var lines = 0
        for (r in 0 until PlayerState.BOARD_ROWS) {
            if (board[r].all { it != null }) lines++
        }
        score += lines * 10.0

        return score
    }
}

private class Random {
    fun nextDouble() = kotlin.random.Random.nextDouble()
    fun nextInt(bound: Int) = kotlin.random.Random.nextInt(bound)
}
