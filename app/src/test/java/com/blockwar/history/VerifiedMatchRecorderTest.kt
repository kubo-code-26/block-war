package com.blockwar.history

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.blockwar.engine.BlockWarEngine
import com.blockwar.model.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],manifest=Config.NONE)
class VerifiedMatchRecorderTest {
    @Test fun cpuDecisionsCanBeCheckedIndependentlyByTheServer() {
        val samples=mutableListOf<Map<String,Any>>()
        for(difficulty in CpuDifficulty.entries)for(seed in 1L..12L) {
            val board=List(PlayerState.BOARD_ROWS){y->List<Cell?>(PlayerState.BOARD_COLS){x->
                if(y>=18 && (x+y+seed.toInt())%3!=0)Cell.GARBAGE else null
            }}
            val random=VerifiedSequence(seed)
            val type=PieceType.entries[(seed%4).toInt()]
            val d=com.blockwar.engine.OpponentAI().findBestMove(board,type,difficulty){random.next(it)}
            samples.add(mapOf("board" to board.map{row->row.map{if(it==null)0 else 1}},"type" to type.blockCount,
                "difficulty" to difficulty.name,"seed" to seed,"x" to d.targetX,"r" to d.targetRotation))
        }
        val file=File("build/cpu-fixture.json");file.parentFile?.mkdirs()
        file.writeText(org.json.JSONArray(samples).toString())
    }
    @Test fun queuedWritesKeepTheirMatchAndOrderAcrossAFastRestart() {
        val first=recorder(); first.begin("queue-a",1,2)
        repeat(300) { first.cpuMove(if(it%2==0)1 else -1,0) }
        first.freeze();first.cpuRotate()
        first.begin("queue-b",3,4); first.cpuRotate();first.freeze()
        val recreated=recorder()
        val a=recreated.events("queue-a")
        assertEquals(300,a.size)
        a.forEachIndexed { index,event -> assertEquals(if(index%2==0)1 else -1,event["dx"]) }
        assertEquals(1,recreated.events("queue-b").size)
        recreated.remove("queue-a");recreated.remove("queue-b")
    }
    @Test fun asyncReadIncludesQueuedWritesAndReturnsOnMainThread() {
        val recorder=recorder();recorder.begin("async-read",1,2)
        repeat(50){recorder.cpuRotate()}
        var received: List<Map<String,Any?>>? = null
        var failure: String? = null
        recorder.eventsAsync("async-read", { received=it;assertEquals(android.os.Looper.getMainLooper(),android.os.Looper.myLooper()) }, { failure=it })
        recorder.events("async-read") // Barrier: the callback is now posted, never executed on the writer.
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertNull(failure);assertEquals(50,received?.size)
        recorder.remove("async-read")
    }
    private fun recorder()=VerifiedMatchRecorder(ApplicationProvider.getApplicationContext<Context>())
    @Test fun thePieceSequenceMatchesTheServerProtocol() {
        val sequence=VerifiedSequence(1)
        assertEquals(listOf(2,0,0,3),(0 until 4).map {sequence.next(4)})
    }
    @Test fun recordedEvidenceSurvivesRecreationAndOnlyItsReceiptDeletesIt() {
        val first=recorder();first.begin("first",1,2)
        first.lock(0,BlockPiece.spawn(PieceType.LINE_3).copy(y=19));first.commit(0);first.freeze()
        first.commit(0)
        val second=recorder();assertEquals(2,second.events("first").size)
        second.begin("second",3,4);second.lock(0,BlockPiece.spawn(PieceType.LINE_4).copy(y=19))
        second.remove("second");assertEquals(2,second.events("first").size);assertTrue(second.events("second").isEmpty())
        second.remove("first")
    }
    @Test fun realEngineProducesAReplayFixtureForIndependentServerVerification() {
        val audit=recorder();audit.begin("fixture",1,2)
        var type=audit.nextPiece(0);var next=audit.nextPiece(0)
        var board=List(PlayerState.BOARD_ROWS){List<Cell?>(PlayerState.BOARD_COLS){null}}
        var score=0;var lines=0;var combo=0;var count=0
        while(count<100) {
            var piece=BlockPiece.spawn(type)
            if(!BlockWarEngine.isValidPosition(board,piece))break
            val target=(count*3)%PlayerState.BOARD_COLS
            while(piece.x!=target) {
                val moved=BlockWarEngine.tryMove(board,piece,if(piece.x<target)1 else -1,0)?:break
                piece=moved
            }
            piece=BlockWarEngine.calculateGhost(board,piece)
            audit.lock(0,piece)
            val lock=BlockWarEngine.lockPiece(board,piece);audit.commit(0)
            combo=if(lock.clearedRows.isNotEmpty())combo+1 else 0
            score+=BlockWarEngine.calculatePoints(lock.clearedRows.size,combo);lines+=lock.clearedRows.size
            board=lock.newBoard;type=next;next=audit.nextPiece(0);count++
            if(lock.isTopOut)break
        }
        audit.freeze();assertTrue(count>0);assertFalse(BlockWarEngine.isValidPosition(board,BlockPiece.spawn(type)))
        val file=File("build/replay-fixture.json");file.parentFile.mkdirs()
        file.writeText(JSONObject(mapOf("session" to mapOf("boardCols" to PlayerState.BOARD_COLS,"seed" to 1,"opponentSeed" to 2,"mode" to "ONLINE"),
            "events" to audit.events("fixture"),"expectedScore" to score,"expectedLines" to lines)).toString())
    }
}
