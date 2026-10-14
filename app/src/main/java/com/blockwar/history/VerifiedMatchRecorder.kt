package com.blockwar.history

import android.content.Context
import android.os.SystemClock
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.blockwar.model.BlockPiece
import com.blockwar.model.FallingGarbagePiece
import com.blockwar.model.PieceType
import com.blockwar.engine.OpponentAI
import com.blockwar.network.SecureBackend
import org.json.JSONArray
import org.json.JSONObject

class VerifiedSequence(seed:Long) {
    private var state=seed and 2147483647L
    fun next(bound:Int):Int {
        require(bound in 1..8388608)
        val limit=(8388608/bound)*bound
        var value:Int
        do {state=(1103515245L*state+12345L) and 2147483647L;value=(state shr 8).toInt()} while(value>=limit)
        return value%bound
    }
}

/** Append-only per-match evidence, retained until the server acknowledges the reward. */
class VerifiedMatchRecorder(context:Context) {
    private val database=object:SQLiteOpenHelper(context,"verified_replays_v1.db",null,1) {
        override fun onCreate(db:SQLiteDatabase) {
            db.execSQL("CREATE TABLE replay_events (match_id TEXT NOT NULL, event_index INTEGER NOT NULL, json TEXT NOT NULL, PRIMARY KEY(match_id,event_index))")
        }
        override fun onUpgrade(db:SQLiteDatabase,oldVersion:Int,newVersion:Int) {}
    }.apply {setWriteAheadLoggingEnabled(true)}.writableDatabase
    private var id=""
    private var start=0L
    private var count=0
    private var frozen=false
    private var player=VerifiedSequence(1)
    private var opponent=VerifiedSequence(2)
    private var cpuRandom=VerifiedSequence(3)
    fun begin(matchId:String, seed:Long, opponentSeed:Long) {
        id=matchId;start=SystemClock.elapsedRealtime();count=0;frozen=false
        player=VerifiedSequence(seed);opponent=VerifiedSequence(opponentSeed);cpuRandom=VerifiedSequence(opponentSeed xor 18523L)
        database.delete("replay_events","match_id = ?",arrayOf(id))
    }
    fun nextPiece(actor:Int):PieceType = PieceType.entries[(if(actor==0)player else opponent).next(4)]
    fun nextCpuRandom(bound:Int):Int=cpuRandom.next(bound)
    fun cpuDecision(d:OpponentAI.MoveDecision)=append("decision",1,mapOf("targetX" to d.targetX,"rotation" to d.targetRotation))
    fun cpuMove(dx:Int,dy:Int)=append("move",1,mapOf("dx" to dx,"dy" to dy))
    fun cpuRotate()=append("rotate",1)
    fun cpuLift(p:BlockPiece)=append("lift",1,piece(p))
    private fun piece(p:BlockPiece)=mapOf("type" to p.type.blockCount,"x" to p.x,"y" to p.y,"r" to p.rotation)
    private fun append(kind:String,actor:Int,details:Map<String,Any?> = emptyMap()) {
        if(id.isBlank()||frozen)return
        val event=JSONObject(details+mapOf("kind" to kind,"actor" to actor,"t" to (SystemClock.elapsedRealtime()-start)))
        database.insertOrThrow("replay_events",null,ContentValues().apply {
            put("match_id",id);put("event_index",count);put("json",event.toString())
        })
        count++
    }
    fun lock(actor:Int,p:BlockPiece)=append("lock",actor,piece(p))
    fun commit(actor:Int)=append("commit",actor)
    fun step(actor:Int,p:BlockPiece?)=append("step",actor,mapOf("active" to p?.let(::piece)))
    fun wave(actor:Int,pieces:List<FallingGarbagePiece>,receipt:String="") {
        if(pieces.isEmpty())return
        append("wave",actor,mapOf("receipt" to receipt,"pieces" to pieces.map {
            mapOf("id" to it.id,"size" to it.size,"col" to it.startCol,"y" to it.y,"target" to it.targetY)
        }))
    }
    fun events(matchId:String):List<Map<String,Any?>> {
        val values=mutableListOf<Map<String,Any?>>()
        database.query("replay_events",arrayOf("json"),"match_id = ?",arrayOf(matchId),null,null,"event_index ASC").use {cursor ->
            while(cursor.moveToNext())values.add(JSONObject(cursor.getString(0)).toMap())
        }
        return values
    }
    fun freeze() {frozen=true}
    fun remove(matchId:String) {
        database.delete("replay_events","match_id = ?",arrayOf(matchId))
    }
}

private fun JSONObject.toMap():Map<String,Any?> = keys().asSequence().associateWith {key -> jsonValue(get(key))}
private fun jsonValue(v:Any?):Any? = when(v) {
    JSONObject.NULL -> null
    is JSONObject -> v.toMap()
    is JSONArray -> (0 until v.length()).map {jsonValue(v.get(it))}
    else -> v
}
