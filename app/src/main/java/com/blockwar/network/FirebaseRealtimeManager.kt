package com.blockwar.network

import com.blockwar.history.OnlineStatisticsPolicy
import com.blockwar.model.AggregatedStats
import com.blockwar.history.PendingMatchReward
import com.blockwar.history.MatchRewardPolicy
import com.blockwar.model.PlayerState
import com.blockwar.model.Cell
import com.blockwar.model.ControlSettings
import com.blockwar.model.PlayerInventory
import com.blockwar.model.PieceType
import com.blockwar.model.ButtonPos
import com.google.firebase.database.*
import androidx.compose.ui.graphics.Color
import android.util.Log
import java.security.MessageDigest
import java.security.SecureRandom
import com.blockwar.economy.EconomyOperation
import com.blockwar.economy.EconomyPolicy
import com.blockwar.missions.MissionCalendar
import com.blockwar.missions.MissionEvent
import com.blockwar.model.Mission

@com.google.firebase.database.IgnoreExtraProperties
data class RematchInfo(val code:String="",val hostUid:String="",val invitedUid:String="",val createdAt:Long=0)

@com.google.firebase.database.IgnoreExtraProperties
data class OnlineRoomState(
    val p1Avatar:String="avatar_default",val p2Avatar:String="avatar_default",
    val p1Skin:String="neon",val p2Skin:String="neon",val p1Theme:String="tab_neon",val p2Theme:String="tab_neon",
    val rematch:RematchInfo?=null,
    val p1Badge: com.blockwar.model.FeaturedBadge? = null,
    val p2Badge: com.blockwar.model.FeaturedBadge? = null,
    val p1Nickname: String = "",
    val p2Nickname: String = "",
    val protocolVersion: Int = 3,
    val p1JoinedAt: Long = 0,
    val p2JoinedAt: Long = 0,
    val p1Connected: Boolean = false,
    val p2Connected: Boolean = false,
    val p1DisconnectedAt: Long = 0,
    val p2DisconnectedAt: Long = 0,
    val finishReason: String = "",
    val finishedAt: Long = 0,
    val loserId: String = "",
    val code: String = "",
    val estado: String = "menu",
    val p1Id: String = "",
    val p2Id: String = "",
    val p1BoardStr: String = "",
    val p2BoardStr: String = "",
    val p1PieceStr: String = "",
    val p2PieceStr: String = "",
    val p1FallingGarbage: String = "",
    val p2FallingGarbage: String = "",
    val p1Score: Int = 0,
    val p2Score: Int = 0,
    val p1Lines: Int = 0,
    val p2Lines: Int = 0,
    val p1GameOver: Boolean = false,
    val p2GameOver: Boolean = false,
    val attackForP1: Int = 0,
    val attackForP2: Int = 0,
    val p1AttackId: Long = 0L,
    val p2AttackId: Long = 0L,
    val vencedor: String = "",
    val attacksForP1: Map<String, Int> = emptyMap(),
    val attacksForP2: Map<String, Int> = emptyMap()
)

data class LeaderboardEntry(
    val id: String = "",
    val nickname: String = "",
    val total_vitorias: Int = 0,
    val pontos_bw: Int = 0
)

class FirebaseRealtimeManager {
    var currentVerifiedMatchId: String = ""
    var roomIsPrivate: Boolean = true
    private var quickSearchGeneration = 0
    var replayProvider: (String, (List<Map<String,Any?>>)->Unit, (String)->Unit) -> Unit = { _, ready, _ -> ready(emptyList()) }
    fun beginVerifiedMatch(nickname:String, mode:String, roomCode:String, difficulty:String,
        onSuccess:(Map<String,Any?>)->Unit,onError:(String)->Unit) {
        SecureBackend.call("bwBeginMatch",mapOf("cpuVersion" to 2,"boardCols" to PlayerState.BOARD_COLS,"nickname" to nickname,"mode" to mode,"roomCode" to roomCode,"difficulty" to difficulty),{
            if((it["boardCols"] as? Number)?.toInt()!=PlayerState.BOARD_COLS) {
                onError("Servidor em atualiza\u00e7\u00e3o. Tente iniciar novamente em instantes.")
            } else {currentVerifiedMatchId=it["id"]?.toString().orEmpty();onSuccess(it)}
        },onError)
    }


    private var presenceRef: DatabaseReference? = null
    private var presencePlayerId = ""
    private var presenceIsP1 = true
    private var foreground = true
    private var transportConnected = false
    private var presenceReady = false
    private var presenceGeneration = 0
    private var presenceSession = 0
    private var connectionListener: ValueEventListener? = null
    private var connectionCallback: ((Boolean) -> Unit)? = null
    private var connectionError: ((String) -> Unit)? = null
    private var timeoutTransactionRunning = false
    fun onlineNow(): Long = System.currentTimeMillis() + MissionCalendar.serverOffsetMillis

    private fun setRoomResult(data: MutableData, loser: String, reason: String) {
        if (data.child("estado").value != "em_jogo") return
        val p1 = data.child("p1Id").value?.toString() ?: ""
        val p2 = data.child("p2Id").value?.toString() ?: ""
        if (loser != p1 && loser != p2) return
        data.child("estado").value = "finalizada"
        data.child("vencedor").value = if (loser == p1) p2 else p1
        data.child("loserId").value = loser
        data.child("finishReason").value = reason
        data.child("finishedAt").value = ServerValue.TIMESTAMP
        data.child(if (loser == p1) "p1GameOver" else "p2GameOver").value = true
    }

    private fun checkDisconnect(data: MutableData, playerId: String, reconnecting: Boolean) {
        if (data.child("estado").value != "em_jogo") return
        val p1 = data.child("p1Id").value?.toString() ?: ""
        val p2 = data.child("p2Id").value?.toString() ?: ""
        if (playerId != p1 && playerId != p2) return
        val self = if (playerId == p1) "p1" else "p2"
        val other = if (playerId == p1) "p2" else "p1"
        val decision = OnlineDisconnectPolicy.decide(data.child("${self}Connected").value == true,
            (data.child("${self}DisconnectedAt").value as? Number)?.toLong() ?: 0,
            data.child("${other}Connected").value == true,
            (data.child("${other}DisconnectedAt").value as? Number)?.toLong() ?: 0, onlineNow(), reconnecting)
        when (decision) {
            OnlineDisconnectPolicy.Decision.SELF_LOSS -> setRoomResult(data, playerId, "desconexao")
            OnlineDisconnectPolicy.Decision.OPPONENT_LOSS -> setRoomResult(data, if (playerId == p1) p2 else p1, "desconexao")
            OnlineDisconnectPolicy.Decision.CANCEL -> {
                data.child("estado").value = "cancelada"
                data.child("finishReason").value = "ambos_desconectados"
                data.child("finishedAt").value = ServerValue.TIMESTAMP
                data.child("vencedor").value = ""
            }
            OnlineDisconnectPolicy.Decision.CONTINUE -> Unit
        }
    }

    fun watchOnlineConnection(roomCode: String, playerId: String, isPlayer1: Boolean,
                              isForeground: Boolean, onReady: (Boolean) -> Unit, onError: (String) -> Unit) {
        stopOnlinePresence()
        presenceRef = matchesRef.child(roomCode)
        presencePlayerId = playerId
        presenceIsP1 = isPlayer1
        foreground = isForeground
        connectionCallback = onReady
        connectionError = onError
        val session = presenceSession
        connectionListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (session != presenceSession) return
                transportConnected = snapshot.value == true
                if (!transportConnected) {
                    presenceReady = false
                    connectionCallback?.invoke(false)
                } else if (foreground) armPresence()
            }
            override fun onCancelled(error: DatabaseError) {
                presenceReady = false
                connectionCallback?.invoke(false)
                connectionError?.invoke(error.message)
            }
        }
        db.getReference(".info/connected").addValueEventListener(connectionListener!!)
    }

    private fun armPresence() {
        val ref=presenceRef?:return;val player=presencePlayerId;val prefix=if(presenceIsP1)"p1" else "p2"
        val generation=++presenceGeneration;presenceReady=false
        SecureBackend.call("bwPreparePresence",mapOf("roomCode" to ref.key,"playerId" to player),{
        if(generation==presenceGeneration&&foreground&&transportConnected)
        ref.onDisconnect().updateChildren(mapOf("${prefix}Connected" to false,"${prefix}DisconnectedAt" to ServerValue.TIMESTAMP)) {error,_->
            if(generation!=presenceGeneration||!foreground||!transportConnected)return@updateChildren
            if(error!=null){connectionError?.invoke(error.message);return@updateChildren}
            SecureBackend.call("bwRoomPresence",mapOf("roomCode" to ref.key,"playerId" to player),{
                if(generation==presenceGeneration&&foreground&&transportConnected){presenceReady=true;connectionCallback?.invoke(true)}
            },{message -> if(generation==presenceGeneration){presenceReady=false;connectionCallback?.invoke(false);connectionError?.invoke(message)}})
        }
        },{message -> if(generation==presenceGeneration){connectionError?.invoke(message)}})
    }

    fun setOnlineForeground(active: Boolean) {
        foreground = active
        if (active) { if (transportConnected) armPresence() }
        else {
            presenceGeneration++
            presenceReady = false
            connectionCallback?.invoke(false)
            markPresenceOffline()
        }
    }

    private fun markPresenceOffline() {
        val ref=presenceRef?:return;val prefix=if(presenceIsP1)"p1" else "p2";val generation=presenceGeneration
        ref.get().addOnSuccessListener {r ->
            if(ref.key==presenceRef?.key&&generation!=presenceGeneration)return@addOnSuccessListener
            if(r.child("${prefix}Connected").value==true || (r.child("${prefix}DisconnectedAt").value as? Number)?.toLong()==0L)
                ref.updateChildren(mapOf("${prefix}Connected" to false,"${prefix}DisconnectedAt" to ServerValue.TIMESTAMP))
        }
    }

    fun resolveOnlineTimeout() {
        val ref=presenceRef?:return
        if(!presenceReady||!foreground||timeoutTransactionRunning)return
        timeoutTransactionRunning=true
        SecureBackend.call("bwRoomTimeout",mapOf("roomCode" to ref.key),{timeoutTransactionRunning=false},{timeoutTransactionRunning=false})
    }

    fun finishOnlineMatch(roomCode:String,loserId:String,reason:String,onError:(String)->Unit={},onComplete:(OnlineRoomState?)->Unit={}) {
        val matchId = currentVerifiedMatchId
        replayProvider(matchId, { events ->
            SecureBackend.call("bwFinishRoom",mapOf("roomCode" to roomCode,"playerId" to loserId,"reason" to reason,
                "matchId" to matchId,"events" to events),{
                    matchesRef.child(roomCode).get().addOnSuccessListener {onComplete(it.getValue(OnlineRoomState::class.java))}
                        .addOnFailureListener {onError(it.message?:"Resultado aguardando conexão.")}
                },onError)
        }, onError)
    }

    fun stopOnlinePresence(markOffline: Boolean = true) {
        presenceSession++
        presenceGeneration++
        connectionListener?.let { db.getReference(".info/connected").removeEventListener(it) }
        if (markOffline) {
            markPresenceOffline()
        }
        // Keep the server-side disconnect fallback armed when leaving an unfinished room.
        presenceRef = null
        connectionListener = null
        connectionCallback = null
        connectionError = null
        presenceReady = false
        transportConnected = false
    }

    private var economyRef: DatabaseReference? = null
    private var economyListener: ValueEventListener? = null

    fun stopEconomyListener() {
        economyListener?.let { listener -> economyRef?.removeEventListener(listener) }
        economyListener = null
        economyRef = null
    }

    fun listenToEconomy(nickname: String, onUpdate: (PlayerInventory) -> Unit, onError: (String) -> Unit,
        onBadgeUpdate:(com.blockwar.model.FeaturedBadge?)->Unit = {}) {
        stopEconomyListener()
        val ref = usersRef.child(nickname.trim().lowercase())
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return
                val raw = snapshot.child("inventory").getValue(PlayerInventory::class.java) ?: PlayerInventory()
                val coins = (snapshot.child("coins").value as? Number)?.toInt()
                    ?: (snapshot.child("inventory").child("coins").value as? Number)?.toInt() ?: 0
                onUpdate(raw.copy(coins = coins.coerceAtLeast(0)))
                onBadgeUpdate(snapshot.child("featuredBadge").getValue(com.blockwar.model.FeaturedBadge::class.java))
            }
            override fun onCancelled(error: DatabaseError) { onError(error.message) }
        }
        economyRef = ref
        economyListener = listener
        ref.addValueEventListener(listener)
    }

    private var statisticsRef: DatabaseReference? = null
    private var statisticsListener: ValueEventListener? = null

    fun stopOnlineStatisticsListener() {
        statisticsListener?.let { statisticsRef?.removeEventListener(it) }
        statisticsRef=null;statisticsListener=null
    }

    fun listenToOnlineStatistics(nickname:String,onUpdate:(AggregatedStats)->Unit,onError:(String)->Unit) {
        stopOnlineStatisticsListener()
        val ref=usersRef.child(nickname.trim().lowercase(java.util.Locale.ROOT)).child("onlineStats")
        val listener=object:ValueEventListener {
            override fun onDataChange(snapshot:DataSnapshot) {
                onUpdate(OnlineStatisticsPolicy.fromMap(snapshot.value as? Map<*,*>))
            }
            override fun onCancelled(error:DatabaseError) {onError(error.message)}
        }
        statisticsRef=ref;statisticsListener=listener;ref.addValueEventListener(listener)
    }

    fun applyEconomyOperation(operation: EconomyOperation, onSuccess:(Boolean,String)->Unit,onError:(String)->Unit) {
        SecureBackend.call("bwEconomy",SecureBackend.map(operation),{onSuccess(it["accepted"]==true,it["message"]?.toString().orEmpty())},onError)
    }

    fun syncMissions(nickname:String,definitions:List<Mission>,events:List<MissionEvent>,claimId:String?,
        onSuccess:(List<Mission>,Int)->Unit,onError:(String)->Unit) {
        SecureBackend.call("bwMissions",mapOf("nickname" to nickname,"claimId" to claimId),{result ->
            val raw=result["missions"] as? List<*> ?: emptyList<Any>()
            val missions=com.blockwar.missions.ServerMissions.parse(raw)
            if(missions.isEmpty()) { onError("Não foi possível carregar as missões do servidor."); return@call }
            onSuccess(missions,(result["coins"] as? Number)?.toInt()?:100)
        },onError)
    }

    private val databaseUrl = "https://block-war-ca797-default-rtdb.firebaseio.com"
    private val db: FirebaseDatabase = FirebaseDatabase.getInstance(databaseUrl)
    private val usersRef: DatabaseReference = db.getReference("users")
    private val matchesRef: DatabaseReference = db.getReference("partidas")

    init {
        db.getReference(".info/serverTimeOffset").get().addOnSuccessListener {
            MissionCalendar.serverOffsetMillis = (it.value as? Number)?.toLong() ?: 0L
        }
    }

    private val pendingAttackAcknowledgements = mutableMapOf<String, MutableSet<String>>()

    private var activeRoomRef: DatabaseReference? = null
    private var activeRoomListener: ValueEventListener? = null

    companion object {
        fun generateSalt(): String {
            val bytes = ByteArray(16)
            SecureRandom().nextBytes(bytes)
            return bytes.joinToString("") { "%02x".format(it) }
        }

        fun hashPin(pin: String, salt: String): String {
            val md = MessageDigest.getInstance("SHA-256")
            val input = "$salt:$pin"
            val digest = md.digest(input.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }

    fun fetchPlayerProfile(nickname:String,deviceId:String,pin:String,onPinRequired:()->Unit,
        onSuccess:(String,Int,PlayerInventory,Int,Int,ControlSettings,String,String,String)->Unit,
        onError:(String)->Unit,createIfMissing:Boolean=true) {
        SecureBackend.call("bwProfileLogin",mapOf("nickname" to nickname,"pin" to pin,"createIfMissing" to createIfMissing),{r ->
            onSuccess(r["nickname"]?.toString()?:nickname,(r["pontos_bw"] as? Number)?.toInt()?:0,
                SecureBackend.inventory(r),(r["total_vitorias"] as? Number)?.toInt()?:0,1,SecureBackend.layout(r),
                pin.takeIf {r["pinVerified"]==true&&it.matches(Regex("[0-9]{4}"))}.orEmpty(),r["googlePlayGamesId"]?.toString().orEmpty(),r["email"]?.toString().orEmpty())
        },{message -> if(message.contains("PIN errado"))onPinRequired() else onError(message)})
    }

    fun checkNicknameExists(name:String,onResult:(Boolean)->Unit) {
        SecureBackend.call("bwProfileExists",mapOf("nickname" to name),{onResult(it["exists"]==true)},{onResult(false)})
    }

    fun savePlayerLayout(nickname:String,settings:ControlSettings) {
        SecureBackend.call("bwSaveLayout",mapOf("nickname" to nickname,"layout" to SecureBackend.map(settings)),{},{Log.w("Firebase",it)})
    }

    fun changePlayerPin(nickname:String,oldPin:String,newPin:String,deviceId:String,onSuccess:()->Unit,onError:(String)->Unit) {
        SecureBackend.call("bwChangePin",mapOf("nickname" to nickname,"oldPin" to oldPin,"newPin" to newPin),{onSuccess()},onError)
    }

    @Deprecated("Use changePlayerPin with verified ownership and completion callbacks")
    fun updateDeviceIdAndPin(nickname: String, deviceId: String, pin: String) {
        // Never update a PIN through the old unchecked write path.
    }

    fun fetchLeaderboard(onSuccess: (List<LeaderboardEntry>) -> Unit, onError: (String) -> Unit) {
        db.getReference("ranking_public").orderByChild("pontos_bw").limitToLast(100).get().addOnSuccessListener { snapshot ->
            val list = mutableListOf<LeaderboardEntry>()
            if (snapshot.exists()) {
                snapshot.children.forEach { child ->
                    val rawNick = child.child("nickname").value?.toString()?.trim()
                    val nick = if (!rawNick.isNullOrEmpty()) rawNick else (child.key ?: "Anon")

                    val pontosBw = (child.child("pontos_bw").value as? Number)?.toInt()
                        ?: child.child("pontos_bw").value?.toString()?.toIntOrNull()
                        ?: 0

                    val totalVitorias = (child.child("total_vitorias").value as? Number)?.toInt()
                        ?: child.child("total_vitorias").value?.toString()?.toIntOrNull()
                        ?: 0



                    val entry = LeaderboardEntry(
                        id = child.key ?: nick,
                        nickname = nick,
                        total_vitorias = totalVitorias,
                        pontos_bw = pontosBw
                    )
                    list.add(entry)
                }
            }
            onSuccess(list.sortedByDescending { it.pontos_bw })
        }.addOnFailureListener { error ->
            Log.e("Firebase", "Erro ao carregar ranking: ${error.message}", error)
            onError(error.message ?: "Falha ao carregar ranking")
        }
    }

    fun findQuickMatch(playerId:String,onFound:(String)->Unit,onNotFound:()->Unit) {
        val generation=++quickSearchGeneration
        SecureBackend.call("bwFindQuickRoom",mapOf("boardCols" to PlayerState.BOARD_COLS,"playerId" to playerId),{r ->
            val code=r["roomCode"] as? String
            if(generation!=quickSearchGeneration) {
                if(!code.isNullOrBlank())SecureBackend.call("bwCancelJoinedRoom",mapOf("roomCode" to code,"playerId" to playerId),{},{})
            } else if(code.isNullOrBlank())onNotFound() else onFound(code)
        },{if(generation==quickSearchGeneration)onNotFound()})
    }
    fun generateRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..4).map { chars.random() }.joinToString("")
    }

    fun createRoom(roomCode:String,playerId:String,onSuccess:()->Unit,onError:(String)->Unit) {
        val generation=quickSearchGeneration
        SecureBackend.call("bwCreateRoom",mapOf("boardCols" to PlayerState.BOARD_COLS,"roomCode" to roomCode,"playerId" to playerId,"isPrivate" to roomIsPrivate),{
            if(generation==quickSearchGeneration)onSuccess() else deleteRoom(roomCode)
        },{if(generation==quickSearchGeneration)onError(it)})
    }
    fun joinRoom(roomCode:String,playerId:String,onSuccess:(OnlineRoomState)->Unit,onError:(String)->Unit) {
        SecureBackend.call("bwJoinRoom",mapOf("boardCols" to PlayerState.BOARD_COLS,"roomCode" to roomCode,"playerId" to playerId),{
            matchesRef.child(roomCode).get().addOnSuccessListener {snapshot ->
                val state=snapshot.getValue(OnlineRoomState::class.java)
                if(state!=null)onSuccess(state) else onError("Sala não encontrada")
            }.addOnFailureListener {onError(it.message?:"Falha ao ler sala.")}
        },onError)
    }
    fun listenToRoom(roomCode: String, onUpdate: (OnlineRoomState?) -> Unit) {
        stopRoomListener()
        val ref = matchesRef.child(roomCode)
        activeRoomRef = ref
        activeRoomListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    onUpdate(null)
                    return
                }
                val state = snapshot.getValue(OnlineRoomState::class.java)
                onUpdate(state)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Erro na escuta da sala: ${error.message}")
                connectionCallback?.invoke(false)
                connectionError?.invoke(error.message)
            }
        }
        ref.addValueEventListener(activeRoomListener!!)
    }

    fun stopRoomListener() {
        quickSearchGeneration++
        activeRoomListener?.let { activeRoomRef?.removeEventListener(it) }
        activeRoomListener = null
        activeRoomRef = null
    }

    fun updatePlayerState(
        roomCode: String,
        isPlayer1: Boolean,
        board: List<List<Cell?>>,
        currentPiece: com.blockwar.model.BlockPiece?,
        fallingGarbagePieces: List<com.blockwar.model.FallingGarbagePiece> = emptyList(),
        score: Int,
        lines: Int,
        isGameOver: Boolean
    ) {
        if (!presenceReady || presenceRef?.key != roomCode) return
        val prefix = if (isPlayer1) "p1" else "p2"
        val updates = mapOf(
            "${prefix}BoardStr" to serializeBoard(board),
            "${prefix}PieceStr" to serializePiece(currentPiece),
            "${prefix}FallingGarbage" to serializeFallingGarbage(fallingGarbagePieces)
        )
        matchesRef.child(roomCode).updateChildren(updates)

    }

    fun sendAttackToOpponent(roomCode:String,fromPlayer1:Boolean,blocks:Int) {
        if(roomCode.isBlank()||blocks<=0||currentVerifiedMatchId.isBlank())return
        val matchId = currentVerifiedMatchId
        replayProvider(matchId, { events ->
            SecureBackend.call("bwSendAttack",mapOf("matchId" to matchId,"events" to events),{},{Log.w("Firebase",it)})
        }, { Log.w("Firebase",it) })
    }

    fun confirmAttackReceived(roomCode: String, forPlayer1: Boolean, attackId: String) {
        val target = if (forPlayer1) "attacksForP1" else "attacksForP2"
        val key = "$roomCode/$target"
        pendingAttackAcknowledgements.getOrPut(key) { mutableSetOf() }.add(attackId)
        clearPendingAttacks(roomCode, forPlayer1)
    }

    fun clearPendingAttacks(roomCode: String, forPlayer1: Boolean) {
        val target = if (forPlayer1) "attacksForP1" else "attacksForP2"
        val key = "$roomCode/$target"
        val acknowledgedIds = pendingAttackAcknowledgements[key] ?: return
        // Never remove the queue itself: a later attack must survive this acknowledgement.
        for (attackId in acknowledgedIds.toList()) {
            matchesRef.child(roomCode).child(target).child(attackId).removeValue()
                .addOnSuccessListener {
                    acknowledgedIds.remove(attackId)
                    if (acknowledgedIds.isEmpty()) pendingAttackAcknowledgements.remove(key)
                }
                .addOnFailureListener { error ->
                    Log.e("Firebase", "Falha ao confirmar ataque $attackId em $roomCode", error)
                }
        }
    }
    fun deleteRoom(roomCode:String) {
        SecureBackend.call("bwDeleteRoom",mapOf("roomCode" to roomCode),{},{Log.w("Firebase",it)})
    }

    @Deprecated("Absolute BW writes are disabled; use applyMatchReward.")
    fun updatePlayerPoints(nickname: String, pontos_bw: Int) {
        Log.w("Firebase", "Gravação absoluta de BW ignorada; use resultado transacional de partida.")
    }
    fun applyMatchReward(reward:PendingMatchReward,onSuccess:(Int,PlayerInventory,Int,com.blockwar.model.MatchRewardSummary?)->Unit,onError:(String)->Unit) {
        replayProvider(reward.matchId, { events ->
        SecureBackend.call("bwSettleMatch",mapOf("matchId" to reward.matchId,"events" to events),{r ->
            val receipt=(r["rewardReceipt"] as? Map<*,*>)?.takeIf{it["matchId"]==reward.matchId}
            val summary=receipt?.let {com.blockwar.model.MatchRewardSummary((it["awardedBw"] as? Number)?.toInt(),
                (it["blockoins"] as? Number)?.toInt()?:0,(it["seasonXp"] as? Number)?.toInt())}
            onSuccess((r["pontos_bw"] as? Number)?.toInt()?:0,SecureBackend.inventory(r),(r["total_vitorias"] as? Number)?.toInt()?:0,summary)
        },onError)
        }, onError)
    }
    fun addBWPoints(nickname:String,pointsToAdd:Int) { /* Only verified server rewards may alter BW. */ }

    fun submitRankingProgress(nickname:String,score:Int,level:Int,isVictory:Boolean) { /* Legacy write disabled. */ }

    fun updatePlayerProgression(nickname:String,pontos_bw:Int,coins:Int,inventory:PlayerInventory,
        googlePlayGamesId:String="",email:String="") { /* All financial and identity writes are server-owned. */ }

    fun updateGooglePlayGamesInfo(nickname:String,googlePlayGamesId:String,email:String,onSuccess:()->Unit={},onError:(String)->Unit={}) {
        onError("Use o vínculo autenticado do Play Games.")
    }

    fun serializePiece(piece: com.blockwar.model.BlockPiece?): String {
        if (piece == null) return ""
        return "${piece.type.name},${piece.x},${piece.y},${piece.rotation}"
    }

    fun deserializePiece(pieceStr: String): com.blockwar.model.BlockPiece? {
        if (pieceStr.isEmpty()) return null
        return try {
            val parts = pieceStr.split(",")
            if (parts.size != 4) return null
            com.blockwar.model.BlockPiece(
                type = com.blockwar.model.PieceType.valueOf(parts[0]),
                x = parts[1].toInt(),
                y = parts[2].toInt(),
                rotation = parts[3].toInt()
            )
        } catch (e: Exception) {
            null
        }
    }

    fun serializeBoard(board: List<List<Cell?>>): String {
        return board.joinToString(";") { row ->
            row.joinToString(",") { cell ->
                if (cell == null) "0"
                else if (cell.isGarbage) "G"
                else "1"
            }
        }
    }

    fun deserializeBoard(boardStr: String): List<List<Cell?>> {
        if (boardStr.isEmpty()) return List(PlayerState.BOARD_ROWS) { List(PlayerState.BOARD_COLS) { null } }
        return try {
            boardStr.split(";").map { rowStr ->
                rowStr.split(",").map { cellChar ->
                    when (cellChar) {
                        "G" -> Cell.GARBAGE
                        "1" -> Cell(Color.Cyan, Color.White, Color.Blue)
                        else -> null
                    }
                }
            }.also { require(it.size==PlayerState.BOARD_ROWS && it.all { row -> row.size==PlayerState.BOARD_COLS }) }
        } catch (e: Exception) {
            List(PlayerState.BOARD_ROWS) { List(PlayerState.BOARD_COLS) { null } }
        }
    }

    fun serializeFallingGarbage(pieces: List<com.blockwar.model.FallingGarbagePiece>): String {
        if (pieces.isEmpty()) return ""
        return pieces.joinToString(";") { "${it.id},${it.size},${it.startCol},${it.y},${it.targetY}" }
    }

    fun deserializeFallingGarbage(str: String): List<com.blockwar.model.FallingGarbagePiece> {
        if (str.isEmpty()) return emptyList()
        return try {
            str.split(";").mapNotNull { pieceStr ->
                val parts = pieceStr.split(",")
                if (parts.size == 5) {
                    val parsedId = parts[0].toLongOrNull() ?: return@mapNotNull null
                    val parsedY = parts[3].toIntOrNull() ?: parts[3].toFloatOrNull()?.toInt() ?: return@mapNotNull null
                    com.blockwar.model.FallingGarbagePiece(
                        id = parsedId,
                        size = parts[1].toInt(),
                        startCol = parts[2].toInt(),
                        y = parsedY,
                        targetY = parts[4].toInt()
                    )
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

/** Reserve a nickname without overwriting any existing data, including on Firebase retries. */
internal class CreatePlayerProfileTransaction(
    private val newProfile: Map<String, Any>,
    private val complete: (DatabaseError?, Boolean, DataSnapshot?) -> Unit
) : Transaction.Handler {
    override fun doTransaction(currentData: MutableData): Transaction.Result {
        if (currentData.value == null) currentData.value = newProfile
        return Transaction.success(currentData)
    }

    override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
        complete(error, committed, currentData)
    }
}
