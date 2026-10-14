package com.blockwar

import com.blockwar.model.*
import com.blockwar.engine.BlockWarEngine
import org.junit.Assert.*
import org.junit.Test

class BoardDimensionsTest {
    @Test fun newBoardUsesTwentyRowsAndTwelveColumnsWithCenteredSpawn() {
        val board=PlayerState().board
        assertEquals(20,board.size)
        assertTrue(board.all{it.size==12})
        assertEquals(4,BlockPiece.spawn(PieceType.LINE_4).x)
        val right=BlockPiece(PieceType.LINE_2,10,19)
        assertTrue(BlockWarEngine.isValidPosition(board,right))
        assertFalse(BlockWarEngine.isValidPosition(board,right.move(1,0)))
    }
    @Test fun tenFilledCellsDoNotClearUntilTheLastTwoColumnsAreFilled() {
        val board=PlayerState().board.map{it.toMutableList()}.toMutableList()
        for(x in 0..9)board[19][x]=Cell.fromPiece(PieceType.LINE_1)
        assertTrue(BlockWarEngine.processStandardLineClears(board).second.isEmpty())
        val locked=BlockWarEngine.lockPiece(board,BlockPiece(PieceType.LINE_2,10,19))
        assertEquals(listOf(19),locked.clearedRows)
        assertEquals(20,locked.newBoard.size)
        assertTrue(locked.newBoard.all{it.size==12})
        assertTrue(locked.newBoard[19].all{it==null})
    }
}