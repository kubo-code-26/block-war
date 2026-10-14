package com.blockwar

import com.blockwar.engine.BlockWarEngine
import com.blockwar.model.Cell
import com.blockwar.model.FallingGarbagePiece
import com.blockwar.model.PlayerState
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class GarbageDistributionTest {
    private fun board() = List(PlayerState.BOARD_ROWS) { List<Cell?>(PlayerState.BOARD_COLS) { null } }
    private fun columns(wave: List<FallingGarbagePiece>) = wave.flatMap { (it.startCol until it.startCol + it.size).toList() }

    @Test fun aDoubleLineAttackUsesEightDistinctColumns() {
        repeat(300) {
            val wave = BlockWarEngine.createFallingGarbagePieceWave(board(), emptyList(), 8)
            assertEquals(8, wave.sumOf { it.size })
            assertEquals(8, columns(wave).toSet().size)
            assertTrue(wave.all { it.targetY == PlayerState.BOARD_ROWS - 1 })
        }
    }

    @Test fun newWavesAvoidAlreadyFallingPiecesWhenTheyFitElsewhere() {
        val incoming = listOf(FallingGarbagePiece(id = 900001L, size = 2, startCol = 4, y = 0, targetY = 19))
        repeat(200) {
            val wave = BlockWarEngine.createFallingGarbagePieceWave(board(), incoming, 8)
            assertEquals(8, columns(wave).toSet().size)
            assertTrue(columns(wave).none { it == 4 || it == 5 })
        }
    }

    @Test fun allAttackSizesPreserveTheirBlocksAndHaveValidLandingPositions() {
        for (total in listOf(0, 1, 2, 5, 8, 10, 12, 16)) repeat(100) {
            val wave = BlockWarEngine.createFallingGarbagePieceWave(board(), emptyList(), total)
            assertEquals(total, wave.sumOf { it.size })
            assertTrue(wave.all { it.startCol >= 0 && it.startCol + it.size <= PlayerState.BOARD_COLS })
            assertTrue(wave.all { it.size in 1..2 && it.targetY in 0 until PlayerState.BOARD_ROWS })
            if (total <= PlayerState.BOARD_COLS) assertEquals(total, columns(wave).toSet().size)
        }
    }
}
