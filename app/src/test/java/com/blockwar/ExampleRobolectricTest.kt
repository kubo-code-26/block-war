package com.blockwar

import com.blockwar.engine.BlockWarEngine
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.blockwar.model.Cell
import com.blockwar.model.PieceType
import com.blockwar.model.PlayerState
import com.blockwar.model.BlockPiece
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Block War", appName)
    }

    @Test
    fun `pieces include only 4 straight horizontal pieces`() {
        val allPieces = PieceType.ALL_PIECES.toList()

        // 1 Quadradinho (1x1)
        assertTrue(allPieces.contains(PieceType.LINE_1))
        assertEquals(1, PieceType.LINE_1.blockCount)

        // 2 Quadradinhos na horizontal (1x2)
        assertTrue(allPieces.contains(PieceType.LINE_2))
        assertEquals(2, PieceType.LINE_2.blockCount)

        // 3 Quadradinhos na horizontal (1x3)
        assertTrue(allPieces.contains(PieceType.LINE_3))
        assertEquals(3, PieceType.LINE_3.blockCount)

        // 4 Quadradinhos na horizontal (1x4)
        assertTrue(allPieces.contains(PieceType.LINE_4))
        assertEquals(4, PieceType.LINE_4.blockCount)

        // Exactly 4 piece types in total
        assertEquals(4, allPieces.size)
    }

    @Test
    fun `attack mechanic calculates eliminated squares attack blocks`() {
        assertEquals(5, BlockWarEngine.calculateAttackBlocks(1))
        assertEquals(8, BlockWarEngine.calculateAttackBlocks(2))
        assertEquals(12, BlockWarEngine.calculateAttackBlocks(3))
        assertEquals(16, BlockWarEngine.calculateAttackBlocks(4))
    }

    @Test
    fun `attack squares are correctly partitioned into pieces`() {
        val totals = listOf(0, 1, 2, 5, 8, 12, 16)
        for (total in totals) {
            val pieces = BlockWarEngine.partitionAttackIntoPieces(total)
            assertEquals(total, pieces.sum())
            if (total > 0) {
                assertTrue(pieces.all { it == 1 || it == 2 })
            } else {
                assertTrue(pieces.isEmpty())
            }
        }
    }

    @Test
    fun `progressive falling 1x1 and 1x2 garbage pieces descend from top and lock into board`() {
        val emptyBoard = List(PlayerState.BOARD_ROWS) { List<Cell?>(PlayerState.BOARD_COLS) { null } }
        val wave = BlockWarEngine.createFallingGarbagePieceWave(emptyBoard, emptyList(), 6)
        assertTrue(wave.isNotEmpty())

        // Verify piece sizes are <= 2
        assertTrue(wave.all { it.size <= 2 })

        // Simulate stepping falling garbage pieces down
        var currentBoard = emptyBoard
        var currentFalling = wave

        for (step in 0 until 40) {
            val result = BlockWarEngine.stepFallingGarbagePieces(currentBoard, currentFalling)
            currentBoard = result.newBoard
            currentFalling = result.remainingFalling
            if (currentFalling.isEmpty()) break
        }

        // After falling steps complete, all 6 blocks must have locked into currentBoard
        var lockedCount = 0
        for (r in 0 until PlayerState.BOARD_ROWS) {
            for (c in 0 until PlayerState.BOARD_COLS) {
                if (currentBoard[r][c] != null && currentBoard[r][c]!!.isGarbage) {
                    lockedCount++
                }
            }
        }
        assertEquals(6, lockedCount)
    }

    @Test
    fun `line clears correctly remove full rows regardless of garbage cells`() {
        // Create a board with a row entirely occupied by normal and garbage cells
        val board = MutableList(PlayerState.BOARD_ROWS) { MutableList<Cell?>(PlayerState.BOARD_COLS) { null } }

        // Row 15 will be fully occupied (5 normal, 5 garbage)
        for (c in 0 until 5) {
            board[15][c] = Cell(androidx.compose.ui.graphics.Color.Red, androidx.compose.ui.graphics.Color.White, androidx.compose.ui.graphics.Color.Black, isGarbage = false)
        }
        for (c in 5 until PlayerState.BOARD_COLS) {
            board[15][c] = Cell.GARBAGE
        }

        val (clearedBoard, clearedRows) = BlockWarEngine.processStandardLineClears(board.map { it.toList() })

        // The row should be identified as cleared
        assertEquals(1, clearedRows.size)
        assertEquals(15, clearedRows[0])

        // The resulting board should have all null cells in that row (as other rows shifted down)
        assertTrue(clearedBoard[15].all { it == null })
    }

    @Test
    fun `stepFallingGarbagePieces clears completed lines immediately`() {
        val board = MutableList(PlayerState.BOARD_ROWS) { MutableList<Cell?>(PlayerState.BOARD_COLS) { null } }

        // Almost fill row 19 (leave columns 8 and 9 empty)
        for (c in 0 until 8) {
            board[19][c] = Cell.GARBAGE
        }

        // Add a falling garbage piece of size 2 that will land in row 19, columns 8 and 9
        val fallingPiece = com.blockwar.model.FallingGarbagePiece(
            id = 999L,
            size = 2,
            startCol = 8,
            y = 19, // Already reached targetY
            targetY = 19
        )

        val result = BlockWarEngine.stepFallingGarbagePieces(board.map { it.toList() }, listOf(fallingPiece))

        // Since the piece completes row 19, it must be cleared immediately.
        // Therefore, row 19 in result.newBoard should be completely cleared (all nulls)
        assertTrue(result.newBoard[19].all { it == null })
        assertTrue(result.remainingFalling.isEmpty())
    }

    @Test
    fun `movement and rotation work correctly with straight pieces`() {
        val emptyBoard = List(PlayerState.BOARD_ROWS) { List<Cell?>(PlayerState.BOARD_COLS) { null } }
        val spawned = BlockPiece.spawn(PieceType.LINE_3)
        val movedRight = BlockWarEngine.tryMove(emptyBoard, spawned, 1, 0)
        assertNotNull(movedRight)
        assertEquals(spawned.x + 1, movedRight!!.x)

        val rotated = BlockWarEngine.tryRotate(emptyBoard, spawned)
        assertNotNull(rotated)
        assertEquals(1, rotated!!.rotation)
    }
}
