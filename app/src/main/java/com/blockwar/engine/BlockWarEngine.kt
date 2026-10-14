package com.blockwar.engine

import com.blockwar.model.BlockPiece
import com.blockwar.model.Cell
import com.blockwar.model.FallingGarbagePiece
import com.blockwar.model.PieceType
import com.blockwar.model.PlayerState
import kotlin.random.Random

object BlockWarEngine {

    data class LockResult(
        val newBoard: List<List<Cell?>>,
        val boardWithPiece: List<List<Cell?>>,
        val clearedRows: List<Int>,
        val isTopOut: Boolean
    )

    data class StepGarbagePiecesResult(
        val newBoard: List<List<Cell?>>,
        val remainingFalling: List<FallingGarbagePiece>,
        val isTopOut: Boolean
    )

    private var nextFallingId = 1L

    fun isValidPosition(board: List<List<Cell?>>, piece: BlockPiece): Boolean {
        for ((x, y) in piece.getOccupiedCoordinates()) {
            if (x < 0 || x >= PlayerState.BOARD_COLS || y >= PlayerState.BOARD_ROWS) return false
            if (y >= 0 && board[y][x] != null) return false
        }
        return true
    }

    fun tryMove(board: List<List<Cell?>>, piece: BlockPiece, dx: Int, dy: Int): BlockPiece? {
        val moved = piece.move(dx, dy)
        return if (isValidPosition(board, moved)) moved else null
    }

    fun tryRotate(board: List<List<Cell?>>, piece: BlockPiece): BlockPiece? {
        val rotated = piece.rotateClockwise()
        if (isValidPosition(board, rotated)) return rotated
        
        // Wall kick: try moving left or right
        for (dx in listOf(-1, 1, -2, 2)) {
            val kicked = rotated.move(dx, 0)
            if (isValidPosition(board, kicked)) return kicked
        }
        return null
    }

    fun calculateGhost(board: List<List<Cell?>>, piece: BlockPiece): BlockPiece {
        var current = piece
        while (true) {
            val next = tryMove(board, current, 0, 1)
            if (next == null) break
            current = next
        }
        return current
    }

    fun processStandardLineClears(board: List<List<Cell?>>): Pair<List<List<Cell?>>, List<Int>> {
        val clearedRows = mutableListOf<Int>()
        for (r in 0 until PlayerState.BOARD_ROWS) {
            if (board[r].all { it != null }) {
                clearedRows.add(r)
            }
        }
        val newBoard = board.filterIndexed { index, _ -> !clearedRows.contains(index) }.toMutableList()
        repeat(clearedRows.size) {
            newBoard.add(0, List(PlayerState.BOARD_COLS) { null })
        }
        return Pair(newBoard, clearedRows)
    }

    fun lockPiece(board: List<List<Cell?>>, piece: BlockPiece): LockResult {
        val boardWithPiece = board.map { it.toMutableList() }
        var isTopOut = false
        val cell = Cell.fromPiece(piece.type)
        
        for ((x, y) in piece.getOccupiedCoordinates()) {
            if (y < 0) {
                isTopOut = true
            } else if (y < PlayerState.BOARD_ROWS && x in 0 until PlayerState.BOARD_COLS) {
                boardWithPiece[y][x] = cell
            }
        }

        val (newBoard, clearedRows) = processStandardLineClears(boardWithPiece.map { it.toList() })

        return LockResult(
            newBoard = newBoard,
            boardWithPiece = boardWithPiece.map { it.toList() },
            clearedRows = clearedRows,
            isTopOut = isTopOut
        )
    }

    fun calculatePoints(linesCleared: Int, combo: Int): Int {
        val basePoints = when (linesCleared) {
            1 -> 5
            2 -> 8
            3 -> 12
            4 -> 16
            else -> 0
        }
        return basePoints * combo.coerceIn(1, 4)
    }

    fun calculateMatchBwBalance(currentBalance: Int, matchPoints: Int, isVictory: Boolean): Int {
        val resultBonus = if (isVictory) 20 else -10
        return (currentBalance + matchPoints + resultBonus).coerceAtLeast(0)
    }
    fun calculateAttackBlocks(linesCleared: Int): Int {
        return when (linesCleared) {
            1 -> 5
            2 -> 8
            3 -> 12
            4 -> 16
            else -> 0
        }
    }

    fun partitionAttackIntoPieces(totalBlocks: Int): List<Int> {
        val pieces = mutableListOf<Int>()
        var remaining = totalBlocks

        while (remaining > 0) {
            val size = if (remaining == 1) {
                1
            } else {
                if (Random.nextFloat() < 0.60f) 1 else 2
            }
            pieces.add(size)
            remaining -= size
        }

        return pieces
    }

    fun createFallingGarbagePieceWave(
        board: List<List<Cell?>>,
        currentFalling: List<FallingGarbagePiece>,
        attackBlocks: Int
    ): List<FallingGarbagePiece> {
        // Place wider pieces first so single blocks cannot fragment the available spaces.
        val sizes = partitionAttackIntoPieces(attackBlocks).sortedDescending()
        val tempBoard = board.map { it.toMutableList() }
        val incomingColumns = IntArray(PlayerState.BOARD_COLS)
        val waveColumns = IntArray(PlayerState.BOARD_COLS)
        val centers = mutableListOf<Double>()
        
        // Also account for pieces already falling to avoid overlapping landing spots
        for (p in currentFalling) {
            for (c in p.startCol until p.startCol + p.size) {
                if (c in incomingColumns.indices) incomingColumns[c]++
            }
            centers.add(p.startCol + (p.size - 1) / 2.0)
            if (p.targetY in 0 until PlayerState.BOARD_ROWS) {
                for (c in p.startCol until (p.startCol + p.size).coerceAtMost(PlayerState.BOARD_COLS)) {
                    tempBoard[p.targetY][c] = Cell.GARBAGE
                }
            }
        }

        // Check that remaining 1x2 pieces still fit, rather than leaving isolated free cells.
        fun canFit(blocked: BooleanArray, remaining: List<Int>): Boolean {
            if (blocked.count { !it } < remaining.sum()) return false
            var pairs = 0
            var run = 0
            for (occupied in blocked) {
                if (occupied) { pairs += run / 2; run = 0 } else run++
            }
            pairs += run / 2
            return pairs >= remaining.count { it == 2 }
        }
        val avoidIncoming = canFit(BooleanArray(PlayerState.BOARD_COLS) { incomingColumns[it] > 0 }, sizes)
        val avoidWaveOverlap = attackBlocks <= PlayerState.BOARD_COLS
        data class Placement(val col: Int, val reused: Int, val incoming: Int, val adjacent: Int, val distance: Double)
        val preference = compareBy<Placement> { it.reused }
            .thenBy { it.incoming }.thenBy { it.adjacent }.thenByDescending { it.distance }

        return sizes.mapIndexed { index, size ->
            val remaining = sizes.drop(index + 1)
            val candidates = (0..PlayerState.BOARD_COLS - size).mapNotNull { col ->
                val span = col until col + size
                if (avoidWaveOverlap && span.any { waveColumns[it] > 0 }) return@mapNotNull null
                if (avoidIncoming && span.any { incomingColumns[it] > 0 }) return@mapNotNull null
                if (avoidWaveOverlap) {
                    val blocked = BooleanArray(PlayerState.BOARD_COLS) { c ->
                        waveColumns[c] > 0 || c in span || (avoidIncoming && incomingColumns[c] > 0)
                    }
                    if (!canFit(blocked, remaining)) return@mapNotNull null
                }
                val center = col + (size - 1) / 2.0
                val adjacent = listOf(col - 1, col + size).count { c ->
                    c in waveColumns.indices && (waveColumns[c] > 0 || incomingColumns[c] > 0)
                }
                Placement(col, span.sumOf { waveColumns[it] }, span.sumOf { incomingColumns[it] },
                    adjacent, centers.minOfOrNull { kotlin.math.abs(center - it) } ?: 0.0)
            }
            val best = candidates.minWith(preference)
            // Randomness only breaks equally spread placements.
            val startCol = candidates.filter { preference.compare(it, best) == 0 }.random().col
            val landingRow = calculateLandingRow(tempBoard, startCol, size)
            for (c in startCol until startCol + size) waveColumns[c]++
            centers.add(startCol + (size - 1) / 2.0)
            
            // Update tempBoard so next piece in wave stacks correctly
            if (landingRow in 0 until PlayerState.BOARD_ROWS) {
                for (c in startCol until startCol + size) {
                    tempBoard[landingRow][c] = Cell.GARBAGE
                }
            }
            
            FallingGarbagePiece(
                id = nextFallingId++,
                size = size,
                startCol = startCol,
                y = -1 - index, // Staggered start
                targetY = landingRow
            )
        }
    }

    private fun calculateLandingRow(board: List<List<Cell?>>, col: Int, size: Int): Int {
        for (r in 0 until PlayerState.BOARD_ROWS) {
            for (c in col until col + size) {
                if (board[r][c] != null) return r - 1
            }
        }
        return PlayerState.BOARD_ROWS - 1
    }

    fun stepFallingGarbagePieces(
        board: List<List<Cell?>>,
        fallingPieces: List<FallingGarbagePiece>
    ): StepGarbagePiecesResult {
        val remaining = mutableListOf<FallingGarbagePiece>()
        val newBoard = board.map { it.toMutableList() }
        var isTopOut = false

        for (p in fallingPieces) {
            val nextY = p.y + 1
            var hasCollision = false

            if (nextY >= PlayerState.BOARD_ROWS) {
                hasCollision = true
            } else if (nextY >= 0) {
                for (c in p.startCol until (p.startCol + p.size).coerceAtMost(PlayerState.BOARD_COLS)) {
                    if (newBoard[nextY][c] != null) {
                        hasCollision = true
                        break
                    }
                }
            }

            if (hasCollision) {
                // Lock into board at current p.y
                for (c in p.startCol until (p.startCol + p.size).coerceAtMost(PlayerState.BOARD_COLS)) {
                    if (p.y < 0) {
                        isTopOut = true
                    } else if (p.y < PlayerState.BOARD_ROWS) {
                        newBoard[p.y][c] = Cell.GARBAGE
                    }
                }
            } else {
                remaining.add(p.copy(y = nextY))
            }
        }

        val (clearedBoard, _) = processStandardLineClears(newBoard.map { it.toList() })

        return StepGarbagePiecesResult(
            newBoard = clearedBoard,
            remainingFalling = remaining,
            isTopOut = isTopOut
        )
    }
}
