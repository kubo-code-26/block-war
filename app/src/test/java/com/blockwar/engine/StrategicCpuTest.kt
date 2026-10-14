package com.blockwar.engine
import com.blockwar.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],manifest=Config.NONE)
class StrategicCpuTest {
 @Test fun clearsDeepWellAndProducesCrossLanguageFixtures() {
  val well=List(20){y->List<Cell?>(12){x->if(y>=16&&x!=11)Cell.GARBAGE else null}}
  val d=StrategicCpu.decide(well,BlockPiece.spawn(PieceType.LINE_4),PieceType.LINE_2)
  assertEquals(11,d.targetX);assertEquals(1,d.targetRotation)
  val fixtures=mutableListOf<Map<String,Any>>()
  for(seed in 0..79) {
   val random=kotlin.random.Random(seed)
   val b=List(20){y->List<Cell?>(12){_->if(y>=20-seed%15 && random.nextInt(4)!=0)Cell.GARBAGE else null}}
   val type=PieceType.entries[seed%4];val next=PieceType.entries[(seed/4)%4]
   val move=StrategicCpu.decide(b,BlockPiece.spawn(type),next)
   fixtures.add(mapOf("board" to b.map{row->row.map{if(it==null)0 else 1}},"type" to type.blockCount,
    "next" to next.blockCount,"x" to move.targetX,"r" to move.targetRotation,"score" to move.score.toInt()))
  }
  File("build/strategic-cpu-fixture.json").apply {parentFile.mkdirs();writeText(org.json.JSONArray(fixtures).toString())}
 }
}
