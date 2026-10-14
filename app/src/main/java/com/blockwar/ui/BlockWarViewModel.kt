package com.blockwar.ui

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.blockwar.audio.SoundManager
import com.blockwar.engine.OpponentAI
import com.blockwar.engine.BlockWarEngine
import com.blockwar.history.OnlineStatisticsStore
import com.blockwar.history.MatchRewardPolicy
import com.blockwar.history.PendingMatchReward
import com.blockwar.history.PendingMatchRewardStore
import com.blockwar.history.MatchHistoryManager
import com.blockwar.model.AggregatedStats
import com.blockwar.model.AppScreen
import com.blockwar.model.AttackEvent
import com.blockwar.model.ControlSettings
import com.blockwar.model.CpuDifficulty
import com.blockwar.model.GameMode
import com.blockwar.model.MatchRecord
import com.blockwar.model.MatchStatus
import com.blockwar.model.Mission
import com.blockwar.model.MissionType
import com.blockwar.model.PieceType
import com.blockwar.model.PlayerState
import com.blockwar.model.PlayerInventory
import com.blockwar.model.ShopItem
import com.blockwar.model.BlockPiece
import com.blockwar.model.VersusGameState
import com.blockwar.network.OnlineAttackInbox
import com.blockwar.network.FirebaseRealtimeManager
import com.blockwar.network.OnlineRoomState
import com.blockwar.network.OnlineDisconnectPolicy
import com.blockwar.network.OnlineSessionStore
import com.blockwar.network.InterruptedOnlineSession
import com.blockwar.ui.components.ParticleController
import com.blockwar.settings.ControlSettingsManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.blockwar.missions.MissionCalendar
import com.blockwar.missions.MissionEvent
import com.blockwar.missions.MissionStore
import com.blockwar.economy.EconomyOperation
import com.blockwar.economy.EconomyPolicy
import com.blockwar.economy.EconomyStore
import com.blockwar.economy.ShopCatalog
import java.util.UUID
import kotlin.random.Random

class BlockWarViewModel(application: Application) : AndroidViewModel(application) {

    val soundManager = SoundManager(application)
    private val opponentAI = OpponentAI()
    private val firebaseManager = FirebaseRealtimeManager()
    private val controlSettingsManager = ControlSettingsManager(application)
    private val onlineStatisticsStore = OnlineStatisticsStore(application)
    private var statisticsOwner = ""
    private var statisticsGeneration = 0
    private val matchHistoryManager = MatchHistoryManager(application)
    private val onlineSessionStore = OnlineSessionStore(application)
    private var lastOnlineRoom: OnlineRoomState? = null
    private var localOnlineConnected = false
    private var onlineResultConfirmed = false
    private var onlineResultPending = false
    private var onlineFinishReason = ""
    private val profileRequestGate = ProfileRequestGate()
    private val playGamesProfiles = com.blockwar.auth.PlayGamesProfileRepository()
    private var playGamesLoginOwner = ""
    private var activePlayGamesIdentity: com.blockwar.auth.PlayGamesIdentity? = null
    private var confirmedPlayGamesOwner = ""
    private var pendingPlayGamesLink: com.blockwar.auth.PlayGamesIdentity? = null
    private var verifiedProfileOwner = ""

    private val recoveringOnlineSessions = mutableSetOf<String>()

    private val economyStore = EconomyStore(application)
    private var economyOwner = ""
    private var economySyncInFlight = false
    private var economyGeneration = 0

    private val missionStore = MissionStore(application)
    private var missionDefinitions: List<Mission> = emptyList()
    private var missionOwner = ""
    private var missionEvents: List<MissionEvent> = emptyList()
    private var missionPeriods: Map<String, String> = emptyMap()
    private var missionSyncInFlight = false

    private val playerId = "P_${UUID.randomUUID()}"

    private val verifiedReplay = com.blockwar.history.VerifiedMatchRecorder(application)
    private var verifiedStartInFlight = false
    private var refreshAfterPlayAuthentication = false
    private val pendingMatchRewardStore = PendingMatchRewardStore(application)
    private val rewardSavesInFlight = mutableSetOf<String>()
    private val matchFinalizationGuard = MatchFinalizationGuard()
    private var matchRewardId = UUID.randomUUID().toString()

    private var matchStartTimeMs: Long = System.currentTimeMillis()
    private var totalAttacksSentCounter: Int = 0
    private val onlineAttackInbox = OnlineAttackInbox()

    private val _gameState = MutableStateFlow(VersusGameState(
        inventory = economyStore.inventory(controlSettingsManager.getPlayerName()) ?: PlayerInventory(),
        coins = economyStore.inventory(controlSettingsManager.getPlayerName())?.coins ?: PlayerInventory().coins,
        controlSettings = controlSettingsManager.loadSettings(),
        nickname = controlSettingsManager.getPlayerName()
    ))
    val gameState: StateFlow<VersusGameState> = _gameState.asStateFlow()

    init {
        firebaseManager.replayProvider = {id -> verifiedReplay.events(id)}
        initializeMissions()
        val name = controlSettingsManager.getPlayerName()
        if (name.isNotEmpty()) {
            syncPlayerProfileFromCloud(name)
        }
        viewModelScope.launch {
            while (true) {
                refreshMissionPeriod()
                syncMissionProgress()
                retryEconomyOperations()
                recoverInterruptedOnlineSession()
                delay(30_000)
            }
        }
        viewModelScope.launch {
            while (true) {
                updateOnlineConnectionState()
                delay(1_000)
            }
        }
    }

    private fun saveOnlineSession() {
        val state = _gameState.value
        if (state.gameMode != GameMode.ONLINE_1V1 || state.onlineRoomCode.isBlank() || onlineResultConfirmed) return
        if (state.matchStatus != MatchStatus.PLAYING && state.matchStatus != MatchStatus.PAUSED) return
        onlineSessionStore.save(InterruptedOnlineSession(state.onlineRoomCode, playerId, state.isPlayer1,
            getPlayerName().lowercase().trim(), matchRewardId, matchStartTimeMs, state.player.score,
            state.player.linesCleared, totalAttacksSentCounter, state.player.maxCombo))
    }

    private fun recoverInterruptedOnlineSession() {
        val owner = getPlayerName().trim().lowercase(java.util.Locale.ROOT)
        val state = _gameState.value
        for (session in onlineSessionStore.pending(owner)) {
            val stillPlaying = session.playerId == playerId && session.matchId == matchRewardId &&
                state.onlineRoomCode == session.roomCode &&
                (state.matchStatus == MatchStatus.PLAYING || state.matchStatus == MatchStatus.PAUSED)
            if (stillPlaying || !recoveringOnlineSessions.add(session.matchId)) continue
            firebaseManager.finishOnlineMatch(session.roomCode, session.playerId, "processo_encerrado",
                onError = { recoveringOnlineSessions.remove(session.matchId) }) { room ->
                recoveringOnlineSessions.remove(session.matchId)
                settleInterruptedOnlineSession(session, room)
            }
        }
    }

    private fun settleInterruptedOnlineSession(session: InterruptedOnlineSession, room: OnlineRoomState?) {
        // The player may have switched accounts while Firebase was confirming the old result.
        // Keep that record until its owner returns; never apply a guest reward to another profile.
        if (session.nickname.trim().lowercase(java.util.Locale.ROOT) !=
            getPlayerName().trim().lowercase(java.util.Locale.ROOT)) return
        if (room != null && room.estado != "finalizada" && room.estado != "cancelada") return
        if (room?.estado == "finalizada") {
            val won = room.vencedor == session.playerId
            val forfeit = !won && (room.finishReason == "desconexao" || room.finishReason == "abandono")
            val reward = PendingMatchReward(session.matchId, session.nickname,
                if (forfeit) -10 else MatchRewardPolicy.bwDelta(session.score, won),
                if (forfeit) 0 else 50, won, isOnline=true, matchPoints=session.score,
                linesCleared=session.lines,attacksSent=session.attacks,
                durationSeconds=(((room.finishedAt.takeIf {it>0}?:System.currentTimeMillis())-session.startedAt)/1000).coerceAtLeast(1),
                finishedAtMillis=room.finishedAt.takeIf {it>0}?:System.currentTimeMillis())
            if (session.nickname.isNotBlank()) {
                if (!pendingMatchRewardStore.save(reward)) return
                submitPendingMatchReward(reward)
            } else queueMatchReward(reward)
            if (matchHistoryManager.getHistory().none { it.id == session.matchId }) {
                val end = room.finishedAt.takeIf { it > 0 } ?: System.currentTimeMillis()
                matchHistoryManager.saveMatch(MatchRecord(session.matchId, end,
                    "ONLINE 1V1 (PARTIDA INTERROMPIDA)", won, session.lines, session.attacks,
                    ((end - session.startedAt) / 1000).coerceAtLeast(1), session.score, session.combo))
                if (!won && session.nickname == getPlayerName().lowercase().trim()) {
                    _gameState.update { it.copy(losses = it.losses + 1) }
                }
            }
        }
        onlineSessionStore.clear(session.matchId)
    }

    private fun canStartOnlineSession(): Boolean {
        if(_gameState.value.playGamesBusy) {
            _gameState.update {it.copy(onlineErrorMessage="Aguarde a confirmação da conta e tente novamente.")}
            return false
        }
        if (onlineSessionStore.pending(getPlayerName()).isEmpty()) return true
        recoverInterruptedOnlineSession()
        _gameState.update { it.copy(onlineErrorMessage = "Aguarde a confirmação da partida anterior e tente novamente.") }
        return false
    }
    private fun cancelOnlineSession(message: String) {
        playerLoopJob?.cancel()
        garbageFallingJob?.cancel()
        soundManager.stopBgm()
        onlineSessionStore.clear(matchRewardId)
        firebaseManager.stopOnlinePresence(markOffline = false)
        firebaseManager.stopRoomListener()
        _gameState.update { it.copy(matchStatus = MatchStatus.STARTING,
            onlineSessionCancelled = true, onlineConnectionMessage = message,
            isSearchingOnlineOpponent = false, onlineRoomStatus = "erro", onlineErrorMessage = message) }
    }

    private fun updateOnlineConnectionState() {
        val state = _gameState.value
        if (state.gameMode != GameMode.ONLINE_1V1 || onlineResultConfirmed || state.onlineSessionCancelled) return
        if (onlineResultPending) return
        if (state.matchStatus != MatchStatus.PLAYING && state.matchStatus != MatchStatus.PAUSED) return
        val room = lastOnlineRoom
        val opponentConnected = room != null && if (state.isPlayer1) room.p2Connected else room.p1Connected
        val ready = localOnlineConnected && isAppActive && opponentConnected
        if (ready) {
            if (state.matchStatus == MatchStatus.PAUSED && state.isAudioEnabled) startBgmWithEquipped()
            _gameState.update { it.copy(matchStatus = MatchStatus.PLAYING, onlineConnectionMessage = "") }
        } else {
            val disconnectedAt = if (state.isPlayer1) (room?.p2DisconnectedAt?.takeIf {it>0} ?: room?.p2JoinedAt ?: 0)
                else (room?.p1DisconnectedAt?.takeIf {it>0} ?: room?.p1JoinedAt ?: 0)
            val remaining = if (disconnectedAt > 0) ((OnlineDisconnectPolicy.GRACE_MS -
                (firebaseManager.onlineNow() - disconnectedAt)).coerceAtLeast(0) + 999) / 1000 else 30
            val message = if (!localOnlineConnected || !isAppActive)
                "Conexão interrompida. Reconecte em até 30 segundos."
                else "Adversário desconectado. Aguardando reconexão: ${remaining}s."
            _gameState.update { it.copy(matchStatus = MatchStatus.PAUSED, onlineConnectionMessage = message) }
            soundManager.stopBgm()
            if (localOnlineConnected && isAppActive && OnlineDisconnectPolicy.expired(disconnectedAt, firebaseManager.onlineNow())) {
                firebaseManager.resolveOnlineTimeout()
            }
        }
    }

    private fun initializeMissions() {
        _gameState.update {
            it.copy(
                dailyMissions = listOf(
                    Mission(
                        id = "daily_first_win",
                        title = "Primeira Vitória",
                        description = "Vença 1 partida",
                        reward = 75,
                        progress = 0,
                        goal = 1,
                        type = MissionType.DAILY,
                        rewardType = "BLOCKOIN"
                    ),
                    Mission(
                        id = "daily_cleaner",
                        title = "Limpador",
                        description = "Limpe 20 linhas",
                        reward = 100,
                        progress = 0,
                        goal = 20,
                        type = MissionType.DAILY,
                        rewardType = "BLOCKOIN"
                    ),
                    Mission(
                        id = "daily_combo_master",
                        title = "Mestre dos Combos",
                        description = "Faça um combo x3",
                        reward = 125,
                        progress = 0,
                        goal = 3,
                        type = MissionType.DAILY,
                        rewardType = "BLOCKOIN"
                    ),
                    Mission(
                        id = "weekly_online_warrior",
                        title = "Guerreiro Online",
                        description = "Vença 5 partidas online",
                        reward = 400,
                        progress = 0,
                        goal = 5,
                        type = MissionType.WEEKLY,
                        rewardType = "BLOCKOIN"
                    ),
                    Mission(
                        id = "weekly_heavy_attack",
                        title = "Ataque Pesado",
                        description = "Envie 40 blocos de garbage para oponentes",
                        reward = 500,
                        progress = 0,
                        goal = 40,
                        type = MissionType.WEEKLY,
                        rewardType = "BLOCKOIN"
                    ),
                    Mission(
                        id = "weekly_constancy",
                        title = "Constância",
                        description = "Conclua 10 partidas sem abandonar",
                        reward = 350,
                        progress = 0,
                        goal = 10,
                        type = MissionType.WEEKLY,
                        rewardType = "BLOCKOIN"
                    ),
                    Mission(
                        id = "monthly_ranking_legend",
                        title = "Lenda do Ranking",
                        description = "Alcance 600 BW ou mais",
                        reward = 1500,
                        progress = 0,
                        goal = 600,
                        type = MissionType.MONTHLY,
                        rewardType = "BLOCKOIN"
                    ),
                    Mission(
                        id = "monthly_demolisher",
                        title = "Demolidor",
                        description = "Limpe 300 linhas acumuladas",
                        reward = 2000,
                        progress = 0,
                        goal = 300,
                        type = MissionType.MONTHLY,
                        rewardType = "BLOCKOIN"
                    ),
                    Mission(
                        id = "monthly_collector",
                        title = "Colecionador",
                        description = "Acumule 20 vitórias",
                        reward = 2500,
                        progress = 0,
                        goal = 20,
                        type = MissionType.MONTHLY,
                        rewardType = "BLOCKOIN"
                    )
                )
            )
        }
        missionDefinitions = _gameState.value.dailyMissions
        refreshMissionPeriod()
    }

    fun buyItem(item: ShopItem) {
        val canonical = ShopCatalog.find(item.id) ?: return
        if (canonical.id in _gameState.value.inventory.boughtItems) return
        enqueueEconomyOperation("BUY", canonical.id)
    }

    fun addCoins(amount: Int) {
        showEconomyMessage("Recarga de teste desativada.")
    }

    fun equipItem(item: ShopItem) {
        val canonical = ShopCatalog.find(item.id) ?: return
        enqueueEconomyOperation("EQUIP", canonical.id)
    }

    private fun showEconomyMessage(message: String) {
        _gameState.update { it.copy(economyMessage = message) }
    }

    private fun enqueueEconomyOperation(kind: String, itemId: String = "", amount: Int = 0) {
        val owner = getPlayerName().trim().lowercase(java.util.Locale.ROOT)
        val operation = EconomyOperation(UUID.randomUUID().toString(), owner, kind, itemId, amount)
        if (owner.isBlank()) {
            val result = EconomyPolicy.apply(_gameState.value.inventory, operation)
            if (!result.accepted) { showEconomyMessage(result.message); return }
            try { economyStore.cache(owner, result.inventory) }
            catch (_: Exception) { showEconomyMessage("Não foi possível salvar a compra no aparelho."); return }
            applyEconomyInventory(owner, result.inventory)
            showEconomyMessage("Operação salva no aparelho.")
            if (kind == "BUY" || kind == "CREDIT") soundManager.playVictory() else soundManager.playRotate()
            return
        }
        if (kind != "CREDIT" && economyStore.pending(owner).any { it.kind == kind && it.itemId == itemId }) {
            showEconomyMessage("Esta operação já está aguardando confirmação.")
            return
        }
        try { economyStore.enqueue(operation) }
        catch (_: Exception) { showEconomyMessage("Não foi possível guardar a operação. Tente novamente."); return }
        showEconomyMessage("Salvando operação da loja…")
        retryEconomyOperations()
    }

    private fun applyEconomyInventory(owner: String, inventory: PlayerInventory) {
        if (getPlayerName().trim().lowercase(java.util.Locale.ROOT) != owner) return
        val oldMusic = _gameState.value.inventory.equippedItems["musicas"]
        economyStore.cache(owner, inventory)
        _gameState.update { it.copy(inventory = inventory, coins = inventory.coins) }
        val music = inventory.equippedItems["musicas"]
        if (music != null && music != oldMusic && _gameState.value.matchStatus == MatchStatus.PLAYING && _gameState.value.isAudioEnabled) {
            soundManager.startBgm(music)
        }
    }

    private fun startOnlineStatisticsSync(nickname:String) {
        val owner=nickname.trim().lowercase(java.util.Locale.ROOT)
        if(owner==statisticsOwner && owner.isNotBlank())return
        statisticsOwner=owner
        val generation=++statisticsGeneration
        firebaseManager.stopOnlineStatisticsListener()
        _gameState.update {it.copy(onlineStats=onlineStatisticsStore.load(owner))}
        if(owner.isBlank())return
        firebaseManager.listenToOnlineStatistics(owner,
            onUpdate={stats ->
                if(generation==statisticsGeneration && getPlayerName().trim().lowercase(java.util.Locale.ROOT)==owner) {
                    onlineStatisticsStore.save(owner,stats)
                    _gameState.update {it.copy(onlineStats=stats)}
                }
            },onError={ android.util.Log.w("OnlineStatistics","Estatísticas aguardando conexão: $it") })
    }

    private fun startEconomySync(nickname: String) {
        startOnlineStatisticsSync(nickname)
        val owner = nickname.trim().lowercase(java.util.Locale.ROOT)
        if (owner.isBlank() || owner == economyOwner) return
        economyOwner = owner
        economyGeneration++
        val generation = economyGeneration
        firebaseManager.listenToEconomy(owner,
            onUpdate = { inventory ->
                if (generation == economyGeneration && owner == economyOwner) {
                    applyEconomyInventory(owner, inventory)
                }
            },
            onError = { if (owner == economyOwner) showEconomyMessage("Saldo aguardando conexão: $it") })
        retryEconomyOperations()
    }

    private fun retryEconomyOperations() {
        val owner = getPlayerName().trim().lowercase(java.util.Locale.ROOT)
        if (owner != economyOwner) {
            firebaseManager.stopEconomyListener()
            economyOwner = ""
            economyGeneration++
            return
        }
        if (owner.isBlank() || economySyncInFlight) return
        val operation = economyStore.pending(owner).firstOrNull() ?: return
        economySyncInFlight = true
        firebaseManager.applyEconomyOperation(operation,
            onSuccess = { accepted, message ->
                economyStore.remove(operation)
                economySyncInFlight = false
                if (owner == getPlayerName().trim().lowercase(java.util.Locale.ROOT)) {
                    showEconomyMessage(if (!accepted) message else if (economyStore.pending(owner).isNotEmpty())
                        "Salvando operações pendentes…" else "Operação salva.")
                    if (accepted) {
                        if (operation.kind == "EQUIP") soundManager.playRotate() else soundManager.playVictory()
                    }
                }
                retryEconomyOperations()
            },
            onError = {
                economySyncInFlight = false
                if (owner == getPlayerName().trim().lowercase(java.util.Locale.ROOT)) {
                    showEconomyMessage("Operação pendente. Tentaremos novamente ao conectar.")
                }
            })
    }
    fun claimMission(missionId: String) {
        refreshMissionPeriod()
        if (missionOwner.isBlank()) {
            _gameState.update { it.copy(missionSaveMessage = "Entre no seu perfil para resgatar Blockoins.") }
            return
        }
        syncMissionProgress(missionId)
    }
    private fun refreshMissionPeriod() {
        if (missionDefinitions.isEmpty()) return
        val owner = getPlayerName().trim().lowercase(java.util.Locale.ROOT)
        val periods = missionDefinitions.associate { it.id to MissionCalendar.period(it.type) }
        if (owner == missionOwner && periods == missionPeriods) return
        val sameOwner = owner == missionOwner && missionPeriods.isNotEmpty()
        missionOwner = owner
        missionPeriods = periods
        missionEvents = missionStore.pending(owner)
        val loaded = missionStore.load(owner, missionDefinitions)
        _gameState.update { it.copy(dailyMissions = loaded) }
        missionStore.save(owner, loaded, missionEvents)
        if (sameOwner) updateMissionProgress("monthly_ranking_legend", _gameState.value.pontos_bw, isSet = true)
    }

    private fun syncMissionProgress(claimId: String? = null) {
        refreshMissionPeriod()
        if (missionOwner.isBlank()) return
        if (missionSyncInFlight) {
            if (claimId != null) _gameState.update { it.copy(missionSaveMessage = "Aguarde a sincronização e tente resgatar novamente.") }
            return
        }
        val owner = missionOwner
        val events = missionEvents.toList()
        val periods = missionPeriods.toMap()
        missionSyncInFlight = true
        firebaseManager.syncMissions(owner, missionDefinitions, events, claimId,
            onSuccess = { missions, coins ->
                missionSyncInFlight = false
                val remaining = missionStore.pending(owner).filterNot { e -> events.any { it.id == e.id } }
                if (getPlayerName().trim().lowercase(java.util.Locale.ROOT) == owner) {
                    refreshMissionPeriod()
                    missionEvents = remaining
                    var merged = missionDefinitions.map { definition ->
                        if (periods[definition.id] == missionPeriods[definition.id])
                            missions.first { it.id == definition.id } else
                            _gameState.value.dailyMissions.first { it.id == definition.id }
                    }
                    remaining.forEach { e -> merged = merged.map { m ->
                        if (e.missionId == m.id && e.period == missionPeriods[m.id]) MissionCalendar.advance(m, e) else m
                    } }
                    missionStore.save(owner, merged, remaining)
                    _gameState.update { state ->
                        state.copy(dailyMissions = merged,
                            coins = state.coins, inventory = state.inventory,
                            missionSaveMessage = if (claimId != null) "Recompensa confirmada." else state.missionSaveMessage)
                    }
                    if (claimId != null && merged.any { it.id == claimId && it.isClaimed }) soundManager.playVictory()
                }
            },
            onError = { message ->
                missionSyncInFlight = false
                if (claimId != null && owner == missionOwner) _gameState.update { it.copy(missionSaveMessage = message) }
            })
    }

    private fun updateMissionProgress(missionId: String, amount: Int, isSet: Boolean = false) {
        refreshMissionPeriod()
        val mission = _gameState.value.dailyMissions.firstOrNull { it.id == missionId } ?: return
        if (mission.isCompleted || mission.isClaimed) return
        val event = MissionEvent(UUID.randomUUID().toString(), missionId,
            MissionCalendar.period(mission.type), amount.coerceAtLeast(0), isSet)
        val updated = MissionCalendar.advance(mission, event)
        if (updated == mission) return
        val missions = _gameState.value.dailyMissions.map { if (it.id == missionId) updated else it }
        missionEvents = missionEvents + event
        missionStore.save(missionOwner, missions, missionEvents)
        _gameState.update { it.copy(dailyMissions = missions) }
        if (updated.isCompleted) soundManager.playVictory()
    }
    fun syncPlayerProfileFromCloud(nickname: String) {
        val request = profileRequestGate.refresh(nickname, getPlayerName()) ?: return

        firebaseManager.fetchPlayerProfile(
            nickname = nickname,
            deviceId = controlSettingsManager.getDeviceId(),
            pin = controlSettingsManager.getPlayerPin(),
            onPinRequired = {
                if (!profileRequestGate.complete(request, getPlayerName())) return@fetchPlayerProfile
                controlSettingsManager.savePlayerName("")
                refreshAfterPlayAuthentication=false
                _gameState.update {it.copy(playGamesBusy=false)}
                _gameState.update {
                    it.copy(
                        nickname = "",
                        nicknameShowPinPrompt = false,
                        nicknameToVerify = "",
                        nicknameErrorMessage = "PIN errado"
                    )
                }
            },
            onSuccess = { fetchedNickname, pontos_bw, inventory, totalVitorias, level, layout, savedPin, gpgsId, email ->
                if (!profileRequestGate.complete(request, getPlayerName())) return@fetchPlayerProfile
                val profileInventory = if (economyOwner == fetchedNickname.trim().lowercase(java.util.Locale.ROOT))
                    _gameState.value.inventory else inventory
                controlSettingsManager.savePlayerName(fetchedNickname)
                controlSettingsManager.saveSettings(layout)
                if (savedPin.isNotEmpty() && savedPin.length == 4) {
                    controlSettingsManager.savePlayerPin(savedPin)
                } else {
                    controlSettingsManager.savePlayerPin("")
                }
                val finalCoins = profileInventory.coins
                val effectiveInventory = profileInventory
                _gameState.update {
                    it.copy(
                        nickname = fetchedNickname,
                        controlSettings = layout,
                        wins = totalVitorias,
                        pontos_bw = pontos_bw,
                        coins = finalCoins,
                        inventory = effectiveInventory,
                        googlePlayGamesId = gpgsId,
                        email = email,
                        nicknameShowPinPrompt = false,
                        nicknameToVerify = "",
                        nicknameErrorMessage = ""
                    )
                }
                if(refreshAfterPlayAuthentication) {
                    refreshAfterPlayAuthentication=false
                    _gameState.update {it.copy(playGamesBusy=false)}
                }
                verifiedProfileOwner = com.blockwar.auth.PlayGamesProfilePolicy.key(fetchedNickname)
                refreshPlayGamesConnection(gpgsId)
                completePendingPlayGamesLink()
                startEconomySync(fetchedNickname)
                updateMissionProgress("monthly_ranking_legend", pontos_bw, isSet = true)
                retryPendingMatchRewards()
            },
            onError = { errorMsg ->
                if (!profileRequestGate.complete(request, getPlayerName())) return@fetchPlayerProfile
                if(refreshAfterPlayAuthentication) {
                    refreshAfterPlayAuthentication=false
                    _gameState.update {it.copy(playGamesBusy=false)}
                }
                _gameState.update {
                    it.copy(
                        nicknameErrorMessage = errorMsg,
                        nicknameShowPinPrompt = false
                    )
                }
            }
        )
    }

    fun openControlsEditor() {
        _gameState.update {
            it.copy(
                currentScreen = AppScreen.CUSTOMIZE_CONTROLS,
                isSearchingOnlineOpponent = false
            )
        }
    }

    fun saveControlSettings(settings: ControlSettings) {
        controlSettingsManager.saveSettings(settings)
        _gameState.update {
            it.copy(
                controlSettings = settings,
                currentScreen = AppScreen.MENU
            )
        }
        val nickname = getPlayerName()
        if (nickname.isNotEmpty()) {
            firebaseManager.savePlayerLayout(nickname, settings)
        }
    }

    fun resetControlSettings(): ControlSettings {
        val defaults = controlSettingsManager.resetToDefaults()
        _gameState.update {
            it.copy(controlSettings = defaults)
        }
        val nickname = getPlayerName()
        if (nickname.isNotEmpty()) {
            firebaseManager.savePlayerLayout(nickname, defaults)
        }
        return defaults
    }

    fun getMatchHistory(): List<MatchRecord> {
        return matchHistoryManager.getHistory()
    }

    fun getAggregatedStats(): AggregatedStats {
        return matchHistoryManager.getAggregatedStats()
    }

    fun getOnlineStatistics(): AggregatedStats {
        val name=getPlayerName().trim().lowercase(java.util.Locale.ROOT)
        if(name.isBlank())return AggregatedStats()
        return if(name==statisticsOwner) _gameState.value.onlineStats else onlineStatisticsStore.load(name)
    }

    fun clearMatchHistory() {
        matchHistoryManager.clearHistory()
    }

    fun getPlayerName(): String {
        return controlSettingsManager.getPlayerName()
    }

    fun logoutProfile() {
        val state = _gameState.value
        if (state.currentScreen != AppScreen.MENU || state.onlineRoomCode.isNotBlank() || state.isSearchingOnlineOpponent || state.playGamesBusy) return
        profileRequestGate.invalidate()
        economyGeneration++
        statisticsGeneration++
        firebaseManager.stopEconomyListener()
        firebaseManager.stopOnlineStatisticsListener()
        economyOwner = ""
        statisticsOwner = ""
        missionOwner = ""
        missionEvents = emptyList()
        verifiedProfileOwner = ""
        confirmedPlayGamesOwner = ""
        playGamesLoginOwner = ""
        activePlayGamesIdentity = null
        pendingPlayGamesLink = null
        refreshAfterPlayAuthentication = false
        getApplication<Application>().getSharedPreferences("session_control", 0).edit()
            .putBoolean("explicit_logout", true).apply()
        controlSettingsManager.savePlayerName("")
        controlSettingsManager.savePlayerPin("")
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
        _gameState.update { it.copy(nickname = "", wins = 0, losses = 0, pontos_bw = 0, dailyMissions = emptyList(),
            economyMessage = "", missionSaveMessage = "",
            inventory = com.blockwar.model.PlayerInventory(), onlineStats = com.blockwar.model.AggregatedStats(),
            googlePlayGamesId = "", email = "", playGamesConnected = false,
            playGamesAccountName = "", playGamesMessage = "", nicknameErrorMessage = "",
            nicknameShowPinPrompt = false, nicknameToVerify = "") }
    }

    fun hasSavedNickname(): Boolean {
        return controlSettingsManager.getPlayerName().isNotEmpty()
    }

    fun checkNicknameExists(name: String, onResult: (Boolean) -> Unit) {
        firebaseManager.checkNicknameExists(name, onResult)
    }

    fun savePlayerName(name: String, pin: String = "", createIfMissing: Boolean = true) {
        if (_gameState.value.playGamesBusy) return
        val owner = com.blockwar.auth.PlayGamesProfilePolicy.key(getPlayerName())
        if (owner.isNotBlank() && owner == com.blockwar.auth.PlayGamesProfilePolicy.key(name) &&
            pin.isNotBlank() && pin != getLocalPin() && verifiedProfileOwner == owner) {
            saveLocalPin(pin)
            return
        }
        verifiedProfileOwner = ""
        if (com.blockwar.auth.PlayGamesProfilePolicy.key(name) != com.blockwar.auth.PlayGamesProfilePolicy.key(getPlayerName())) {
            _gameState.update { it.copy(playGamesConnected = false, playGamesAccountName = "") }
        }

        val request = profileRequestGate.select(name)

        _gameState.update {
            it.copy(
                nicknameErrorMessage = "",
                nicknameShowPinPrompt = false,
                nicknameToVerify = ""
            )
        }
        firebaseManager.fetchPlayerProfile(
            nickname = name,
            deviceId = controlSettingsManager.getDeviceId(),
            pin = pin,
            createIfMissing = createIfMissing,
            onPinRequired = {
                if (!profileRequestGate.complete(request, getPlayerName())) return@fetchPlayerProfile
                if(refreshAfterPlayAuthentication) {
                    refreshAfterPlayAuthentication=false
                    _gameState.update {it.copy(playGamesBusy=false)}
                }
                _gameState.update {
                    it.copy(
                        nicknameShowPinPrompt = false,
                        nicknameToVerify = "",
                        nicknameErrorMessage = "PIN errado"
                    )
                }
            },
            onSuccess = { fetchedNickname, pontos_bw, inventory, totalVitorias, level, layout, savedPin, gpgsId, email ->
                if (!profileRequestGate.complete(request, getPlayerName())) return@fetchPlayerProfile
                val profileInventory = if (economyOwner == fetchedNickname.trim().lowercase(java.util.Locale.ROOT))
                    _gameState.value.inventory else inventory
                controlSettingsManager.savePlayerName(fetchedNickname)
                controlSettingsManager.saveSettings(layout)
                if (savedPin.isNotEmpty() && savedPin.length == 4) {
                    controlSettingsManager.savePlayerPin(savedPin)
                } else {
                    controlSettingsManager.savePlayerPin("")
                }
                _gameState.update {
                    it.copy(
                        nickname = fetchedNickname,
                        controlSettings = layout,
                        wins = totalVitorias,
                        pontos_bw = pontos_bw,
                        coins = profileInventory.coins,
                        inventory = profileInventory,
                        googlePlayGamesId = gpgsId,
                        email = email,
                        nicknameShowPinPrompt = false,
                        nicknameToVerify = "",
                        nicknameErrorMessage = ""
                    )
                }
                if(refreshAfterPlayAuthentication) {
                    refreshAfterPlayAuthentication=false
                    _gameState.update {it.copy(playGamesBusy=false)}
                }
                verifiedProfileOwner = com.blockwar.auth.PlayGamesProfilePolicy.key(fetchedNickname)
                refreshPlayGamesConnection(gpgsId)
                completePendingPlayGamesLink()
                startEconomySync(fetchedNickname)
                updateMissionProgress("monthly_ranking_legend", pontos_bw, isSet = true)
                retryPendingMatchRewards()
            },
            onError = { errorMsg ->
                if (!profileRequestGate.complete(request, getPlayerName())) return@fetchPlayerProfile
                if(refreshAfterPlayAuthentication) {
                    refreshAfterPlayAuthentication=false
                    _gameState.update {it.copy(playGamesBusy=false)}
                }
                _gameState.update {
                    it.copy(
                        nicknameErrorMessage = errorMsg,
                        nicknameShowPinPrompt = false
                    )
                }
            }
        )
    }

    private fun refreshPlayGamesConnection(profilePlayerId: String) {
        val identity = activePlayGamesIdentity
        val connected = identity != null && confirmedPlayGamesOwner.isNotBlank() &&
            confirmedPlayGamesOwner == com.blockwar.auth.PlayGamesProfilePolicy.key(getPlayerName()) &&
            identity.playerId == profilePlayerId &&
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid == identity.firebaseUid
        _gameState.update { it.copy(playGamesConnected = connected,
            playGamesAccountName = if (connected) identity!!.accountName.ifBlank {
                identity.email.ifBlank { "Conta do Play Games" }
            } else "") }
    }

    fun beginPlayGamesLogin(interactive: Boolean): Boolean {
        val sessionPrefs = getApplication<Application>().getSharedPreferences("session_control", 0)
        if (!interactive && sessionPrefs.getBoolean("explicit_logout", false)) return false
        if (_gameState.value.playGamesBusy || _gameState.value.currentScreen != AppScreen.MENU || _gameState.value.onlineRoomCode.isNotBlank()) return false
        if (interactive) sessionPrefs.edit().putBoolean("explicit_logout", false).apply()
        playGamesLoginOwner = com.blockwar.auth.PlayGamesProfilePolicy.key(getPlayerName())
        _gameState.update { it.copy(playGamesBusy = true, playGamesMessage = if (interactive) "Conectando ao Google Play Games…" else "") }
        return true
    }

    fun onPlayGamesLoginError(message: String, interactive: Boolean) {
        activePlayGamesIdentity = null
        confirmedPlayGamesOwner = ""
        _gameState.update { it.copy(playGamesBusy = false, playGamesConnected = false,
            playGamesAccountName = "", playGamesMessage = if (interactive) message else "") }
    }

    fun onPlayGamesAuthenticated(identity: com.blockwar.auth.PlayGamesIdentity, interactive: Boolean) {
        activePlayGamesIdentity = identity
        confirmedPlayGamesOwner = ""
        _gameState.update { it.copy(playGamesConnected = false, playGamesAccountName = "") }
        val owner = playGamesLoginOwner
        fun stillCurrent() = com.blockwar.auth.PlayGamesProfilePolicy.key(getPlayerName()) == owner &&
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid == identity.firebaseUid
        playGamesProfiles.find(identity, onSuccess = { linkedName ->
            if (!stillCurrent()) { onPlayGamesLoginError("O perfil mudou. Conecte novamente.", interactive); return@find }
            if (linkedName != null) {
                pendingPlayGamesLink = null
                val linkedOwner = com.blockwar.auth.PlayGamesProfilePolicy.key(linkedName)
                if (!interactive && owner.isNotBlank() && owner != linkedOwner) {
                    _gameState.update { it.copy(playGamesBusy = false,
                        playGamesMessage = "A conta Google está vinculada a $linkedName. Toque em conectar para recuperar esse perfil.") }
                    return@find
                }
                _gameState.update { it.copy(playGamesBusy = false, playGamesMessage = "Perfil $linkedName conectado ao Play Games.") }
                confirmedPlayGamesOwner = linkedOwner
                savePlayerName(linkedName, createIfMissing = false)
            } else {
                if(!interactive&&owner.isNotBlank()) {
                    refreshAfterPlayAuthentication=true
                    syncPlayerProfileFromCloud(getPlayerName())
                    return@find
                }
                _gameState.update { it.copy(playGamesBusy = false) }
                if (interactive) {
                    pendingPlayGamesLink = identity
                    if (owner.isBlank()) {
                        _gameState.update { it.copy(playGamesMessage = "Google conectado. Crie ou entre no seu perfil com o PIN para vinculá-lo.") }
                    } else completePendingPlayGamesLink()
                }
            }
        }, onError = { message ->
            if (stillCurrent()) {
                // An interrupted reservation may be completed only after the current profile's PIN was verified.
                if (interactive && owner.isNotBlank() && verifiedProfileOwner == owner &&
                    message.startsWith("O vínculo de ")) {
                    pendingPlayGamesLink = identity
                    _gameState.update { it.copy(playGamesBusy = false) }
                    completePendingPlayGamesLink()
                } else onPlayGamesLoginError(message, interactive)
            } else onPlayGamesLoginError("O perfil mudou. Conecte novamente.", interactive)
        })
    }

    private fun completePendingPlayGamesLink() {
        val identity = pendingPlayGamesLink ?: return
        val name = getPlayerName()
        val owner = com.blockwar.auth.PlayGamesProfilePolicy.key(name)
        if (verifiedProfileOwner != owner || owner.isBlank()) {
            _gameState.update { it.copy(playGamesMessage = "Entre no seu perfil com o PIN antes de conectar o Google.") }
            return
        }
        if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid != identity.firebaseUid) {
            pendingPlayGamesLink = null
            onPlayGamesLoginError("A conta Google mudou. Conecte novamente.", true)
            return
        }
        _gameState.update { it.copy(playGamesBusy = true) }
        playGamesProfiles.link(identity, name, getLocalPin(), onSuccess = {
            pendingPlayGamesLink = null
            if (com.blockwar.auth.PlayGamesProfilePolicy.key(getPlayerName()) == owner) {
                activePlayGamesIdentity = identity
                confirmedPlayGamesOwner = owner
                refreshPlayGamesConnection(identity.playerId)
                economyOwner = ""
                statisticsOwner = ""
                startEconomySync(name)
                _gameState.update { it.copy(playGamesBusy = false, googlePlayGamesId = identity.playerId,
                    email = identity.email.ifBlank { it.email }, playGamesMessage = "Perfil conectado ao Google Play Games.") }
            } else onPlayGamesLoginError("O perfil mudou. Conecte novamente.", true)
        }, onError = { message ->
            pendingPlayGamesLink = null
            onPlayGamesLoginError(message, true)
        })
    }

    fun linkGooglePlayGamesInfo(gpgsId: String, email: String) {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return
        val provider = user.providerData.firstOrNull {
            it.providerId == com.google.firebase.auth.PlayGamesAuthProvider.PROVIDER_ID && it.uid == gpgsId
        } ?: return
        pendingPlayGamesLink = com.blockwar.auth.PlayGamesIdentity(user.uid, provider.uid, user.email.orEmpty())
        completePendingPlayGamesLink()
    }

    fun confirmNicknameWithPin(pin: String) {
        val nickname = _gameState.value.nicknameToVerify
        if (nickname.isNotEmpty()) {
            savePlayerName(nickname, pin)
        }
    }

    fun clearNicknameError() {
        _gameState.update {
            it.copy(
                nicknameErrorMessage = ""
            )
        }
    }

    fun cancelNicknamePinPrompt() {
        profileRequestGate.invalidate()
        _gameState.update {
            it.copy(
                nicknameShowPinPrompt = false,
                nicknameToVerify = "",
                nicknameErrorMessage = ""
            )
        }
    }

    fun getLocalPin(): String {
        return controlSettingsManager.getPlayerPin()
    }

    fun saveLocalPin(pin: String) {
        val name = getPlayerName()
        val owner = com.blockwar.auth.PlayGamesProfilePolicy.key(name)
        if (owner.isBlank() || verifiedProfileOwner != owner || !pin.matches(Regex("[0-9]{4}"))) {
            _gameState.update { it.copy(nicknameErrorMessage = "Entre no perfil antes de alterar o PIN.") }
            return
        }
        val oldPin = getLocalPin()
        firebaseManager.changePlayerPin(name, oldPin, pin, controlSettingsManager.getDeviceId(), onSuccess = {
            if (com.blockwar.auth.PlayGamesProfilePolicy.key(getPlayerName()) != owner) return@changePlayerPin
            controlSettingsManager.savePlayerPin(pin)
            _gameState.update { it.copy(nicknameErrorMessage = "") }
        }, onError = { message ->
            if (com.blockwar.auth.PlayGamesProfilePolicy.key(getPlayerName()) == owner)
                _gameState.update { it.copy(nicknameErrorMessage = message) }
        })
    }

    fun getPlayerId(): String {
        return playerId
    }

    fun fetchLeaderboard(
        onSuccess: (List<com.blockwar.network.LeaderboardEntry>) -> Unit,
        onError: (String) -> Unit
    ) {
        firebaseManager.fetchLeaderboard(onSuccess, onError)
    }

    fun getPatentInfo(pontos_bw: Int): Pair<String, Color> {
        return when {
            pontos_bw >= 2000 -> "🚀 Mestre Pixel" to Color(0xFFFFD700)
            pontos_bw >= 1500 -> "💎 Diamante Pixel" to Color(0xFF00E5FF)
            pontos_bw >= 1000 -> "⚔️ Platina Pixel" to Color(0xFF9E9E9E)
            pontos_bw >= 600 -> "⭐ Ouro Pixel" to Color(0xFFFFD600)
            pontos_bw >= 300 -> "🥈 Prata Pixel" to Color(0xFFB0BEC5)
            else -> "🥉 Bronze Pixel" to Color(0xFF8D6E63)
        }
    }

    @Deprecated("As moedas da partida são salvas junto com os BW em saveCompletedMatchReward.")
    fun awardMatchCoins(isOnline: Boolean) {
        // Compatibility entry point: rewards must not be credited twice.
    }
    private fun saveCompletedMatchReward(isVictory: Boolean) {
        val state = _gameState.value
        queueMatchReward(PendingMatchReward(
            matchId = matchRewardId,
            nickname = getPlayerName().lowercase().trim(),
            bwDelta = MatchRewardPolicy.bwDelta(state.player.score, isVictory),
            blockoins = MatchRewardPolicy.blockoins(state.gameMode == GameMode.ONLINE_1V1),
            isVictory = isVictory,
            isOnline = state.gameMode == GameMode.ONLINE_1V1,
            matchPoints = state.player.score, linesCleared = state.player.linesCleared, attacksSent = totalAttacksSentCounter,
            durationSeconds = ((System.currentTimeMillis()-matchStartTimeMs)/1000).coerceAtLeast(1),
            finishedAtMillis = lastOnlineRoom?.finishedAt?.takeIf {it>0} ?: System.currentTimeMillis()
        ))
    }

    private fun queueMatchReward(reward: PendingMatchReward) {
        if (reward.nickname.isBlank()) {
            // Guests retain the existing local-only behavior; registered profiles use Firebase.
            _gameState.update {
                val coins = MatchRewardPolicy.applyDelta(it.inventory.coins, reward.blockoins)
                it.copy(pontos_bw = MatchRewardPolicy.applyDelta(it.pontos_bw, reward.bwDelta),
                    inventory = it.inventory.copy(coins = coins), coins = coins,
                    rewardSaveMessage = "")
            }
            economyStore.cache("", _gameState.value.inventory)
            updateMissionProgress("monthly_ranking_legend", _gameState.value.pontos_bw, isSet = true)
            return
        }
        verifiedReplay.freeze()
        if (!pendingMatchRewardStore.save(reward)) {
            _gameState.update { it.copy(rewardSaveMessage = "Não foi possível guardar a recompensa no aparelho.") }
            return
        }
        submitPendingMatchReward(reward)
    }

    private fun retryPendingMatchRewards() {
        val nickname = getPlayerName().lowercase().trim()
        if (nickname.isBlank()) return
        for (reward in pendingMatchRewardStore.pending(nickname)) submitPendingMatchReward(reward)
    }

    private fun submitPendingMatchReward(reward: PendingMatchReward) {
        if (!rewardSavesInFlight.add(reward.matchId)) return
        _gameState.update { it.copy(rewardSaveMessage = "Salvando Pontos BW e Blockoins…") }
        firebaseManager.applyMatchReward(
            reward,
            onSuccess = { balance, inventory, totalWins ->
                pendingMatchRewardStore.remove(reward.matchId)
                verifiedReplay.remove(reward.matchId)
                rewardSavesInFlight.remove(reward.matchId)
                if (getPlayerName().lowercase().trim() == reward.nickname) {
                    val pending = pendingMatchRewardStore.pending(reward.nickname).isNotEmpty()
                    syncMissionProgress()
                    _gameState.update { it.copy(pontos_bw = balance, wins = totalWins,
                        rewardSaveMessage = if (pending) "Há recompensas aguardando salvamento." else "") }
                    updateMissionProgress("monthly_ranking_legend", balance, isSet = true)
                }
            },
            onError = { error ->
                rewardSavesInFlight.remove(reward.matchId)
                if (getPlayerName().lowercase().trim() == reward.nickname) {
                    _gameState.update { it.copy(rewardSaveMessage =
                        "Recompensa pendente. Voltaremos a tentar ao reabrir o app. $error") }
                }
            }
        )
    }

    private var playerLoopJob: Job? = null
    private var opponentLoopJob: Job? = null
    private var garbageFallingJob: Job? = null
    val particleController = ParticleController()

    private fun nextPlayerPiece(): PieceType {
        return verifiedReplay.nextPiece(0)
    }

    private fun nextOpponentPiece(): PieceType {
        return verifiedReplay.nextPiece(1)
    }

    fun startVsCpuMatch(difficulty: CpuDifficulty = CpuDifficulty.MEDIUM) {
        firebaseManager.stopOnlinePresence()
        firebaseManager.stopRoomListener()
        _gameState.update {
            it.copy(
                currentScreen = AppScreen.GAME_VS_CPU,
                gameMode = GameMode.VS_CPU,
                cpuDifficulty = difficulty,
                isSearchingOnlineOpponent = false
            )
        }
        startNewMatch()
    }

    fun openOnlineLobby() {
        _gameState.update {
            it.copy(
                isSearchingOnlineOpponent = true,
                onlineRoomCode = "",
                onlineRoomStatus = "menu",
                onlineErrorMessage = ""
            )
        }
    }

    fun startQuickMatch() {
        firebaseManager.roomIsPrivate=false
        if (!canStartOnlineSession()) return
        val oldRoomCode = _gameState.value.onlineRoomCode
        val wasPlayer1 = _gameState.value.isPlayer1
        if (oldRoomCode.isNotEmpty() && wasPlayer1) {
            firebaseManager.deleteRoom(oldRoomCode)
        }
        firebaseManager.stopRoomListener()
        playerLoopJob?.cancel()
        opponentLoopJob?.cancel()
        garbageFallingJob?.cancel()
        soundManager.stopBgm()

        _gameState.update {
            it.copy(
                currentScreen = AppScreen.MENU,
                gameMode = GameMode.ONLINE_1V1,
                matchStatus = MatchStatus.STARTING,
                isSearchingOnlineOpponent = true,
                onlineRoomStatus = "buscando",
                onlineRoomCode = "",
                onlineErrorMessage = "",
                attackEvents = emptyList(),
                totalAttacksSent = 0,
                matchDurationSeconds = 0
            )
        }

        firebaseManager.findQuickMatch(
            playerId = playerId,
            onFound = { foundRoomCode ->
                // findQuickMatch only reports a room after the second seat was committed.
                _gameState.update { it.copy(isPlayer1 = false, onlineRoomCode = foundRoomCode,
                    onlineRoomStatus = "entrando", onlineErrorMessage = "") }
                listenToOnlineRoom(foundRoomCode)
            },
            onNotFound = {
                val roomCode = firebaseManager.generateRoomCode()
                _gameState.update {
                    it.copy(
                        isSearchingOnlineOpponent = true,
                        onlineRoomCode = roomCode,
                        isPlayer1 = true,
                        onlineRoomStatus = "buscando",
                        onlineErrorMessage = ""
                    )
                }

                firebaseManager.createRoom(
                    roomCode = roomCode,
                    playerId = playerId,
                    onSuccess = {
                        listenToOnlineRoom(roomCode)
                    },
                    onError = { err ->
                        _gameState.update {
                            it.copy(
                                onlineRoomStatus = "erro",
                                onlineErrorMessage = err
                            )
                        }
                    }
                )
            }
        )
    }

    fun cancelQuickMatch() {
        firebaseManager.stopOnlinePresence()
        val currentCode = _gameState.value.onlineRoomCode
        if (currentCode.isNotEmpty() && _gameState.value.isPlayer1) {
            firebaseManager.deleteRoom(currentCode)
        }
        firebaseManager.stopRoomListener()
        _gameState.update {
            it.copy(
                isSearchingOnlineOpponent = true,
                onlineRoomStatus = "menu",
                onlineRoomCode = "",
                onlineErrorMessage = ""
            )
        }
    }

    fun createOnlineRoom() {
        firebaseManager.roomIsPrivate=true
        if (!canStartOnlineSession()) return
        val roomCode = firebaseManager.generateRoomCode()
        _gameState.update {
            it.copy(
                isSearchingOnlineOpponent = true,
                onlineRoomCode = roomCode,
                isPlayer1 = true,
                onlineRoomStatus = "aguardando_jogadores",
                onlineErrorMessage = ""
            )
        }

        firebaseManager.createRoom(
            roomCode = roomCode,
            playerId = playerId,
            onSuccess = {
                listenToOnlineRoom(roomCode)
            },
            onError = { err ->
                _gameState.update {
                    it.copy(
                        onlineRoomStatus = "erro",
                        onlineErrorMessage = err
                    )
                }
            }
        )
    }

    fun joinOnlineRoom(inputCode: String) {
        if (!canStartOnlineSession()) return
        val code = inputCode.trim().uppercase()
        if (code.length != 4) {
            _gameState.update {
                it.copy(onlineErrorMessage = "CÓDIGO INVÁLIDO (DIGITE 4 CARACTERES)")
            }
            return
        }

        _gameState.update {
            it.copy(
                onlineRoomStatus = "entrando",
                onlineErrorMessage = ""
            )
        }

        firebaseManager.joinRoom(
            roomCode = code,
            playerId = playerId,
            onSuccess = { roomState ->
                _gameState.update {
                    it.copy(
                        isPlayer1 = false,
                        onlineRoomCode = code
                    )
                }
                listenToOnlineRoom(code)
            },
            onError = { err ->
                _gameState.update {
                    it.copy(
                        onlineRoomStatus = "erro",
                        onlineErrorMessage = err
                    )
                }
            }
        )
    }

    private fun listenToOnlineRoom(roomCode: String) {
        lastOnlineRoom = null
        localOnlineConnected = false
        onlineResultConfirmed = false
        onlineResultPending = false
        onlineFinishReason = ""
        firebaseManager.watchOnlineConnection(roomCode, playerId, _gameState.value.isPlayer1, isAppActive,
            onReady = { ready ->
                localOnlineConnected = ready
                updateOnlineConnectionState()
                if (ready && _gameState.value.matchStatus == MatchStatus.PLAYING) syncOnlinePlayerState()
            }, onError = { message -> _gameState.update { it.copy(onlineConnectionMessage = message) } })
        firebaseManager.listenToRoom(roomCode) { roomState ->
            if (_gameState.value.onlineRoomCode != roomCode ||
                _gameState.value.gameMode != GameMode.ONLINE_1V1) return@listenToRoom
            if (roomState == null) {
                cancelOnlineSession("A sala foi encerrada. Nenhuma vitória ou recompensa foi atribuída.")
                return@listenToRoom
            }
            lastOnlineRoom = roomState
            if (roomState.estado == "cancelada") {
                cancelOnlineSession("Partida cancelada após perda de conexão dos dois jogadores. Sem alteração de BW ou Blockoins.")
                return@listenToRoom
            }
            if (roomState.estado == "finalizada") {
                onlineResultConfirmed = true
                onlineResultPending = false
                onlineFinishReason = roomState.finishReason
                if (roomState.vencedor == playerId) {
                    _gameState.update { it.copy(onlineConnectionMessage = if (roomState.finishReason == "desconexao" ||
                        roomState.finishReason == "abandono") "O adversário saiu da partida." else "") }
                    onMatchVictory()
                } else if (roomState.loserId == playerId) {
                    onMatchDefeat()
                } else cancelOnlineSession("A sala já está encerrada.")
                return@listenToRoom
            }

            var currentState = _gameState.value
            val isP1 = currentState.isPlayer1

            if (roomState.estado == "em_jogo" && currentState.matchStatus == MatchStatus.STARTING) {
                _gameState.update {
                    it.copy(
                        isSearchingOnlineOpponent = false,
                        currentScreen = AppScreen.GAME_ONLINE,
                        gameMode = GameMode.ONLINE_1V1,
                        onlineRoomStatus = "em_jogo"
                    )
                }
                startNewMatch()
                currentState = _gameState.value
            }
            updateOnlineConnectionState()
            currentState = _gameState.value

            val opponentBoardStr = if (isP1) roomState.p2BoardStr else roomState.p1BoardStr
            val opponentScore = if (isP1) roomState.p2Score else roomState.p1Score
            val opponentLines = if (isP1) roomState.p2Lines else roomState.p1Lines
            val opponentGameOver = if (isP1) roomState.p2GameOver else roomState.p1GameOver
            val opponentPieceStr = if (isP1) roomState.p2PieceStr else roomState.p1PieceStr
            val opponentFallingStr = if (isP1) roomState.p2FallingGarbage else roomState.p1FallingGarbage

            val deserializedBoard = firebaseManager.deserializeBoard(opponentBoardStr)
            val deserializedPiece = firebaseManager.deserializePiece(opponentPieceStr)
            val deserializedOpponentFalling = firebaseManager.deserializeFallingGarbage(opponentFallingStr)

            onlineAttackInbox.beginRoom(roomCode)
            val queuedAttacks = if (isP1) roomState.attacksForP1 else roomState.attacksForP2
            val pendingAttacks = if (currentState.matchStatus == MatchStatus.PLAYING) {
                onlineAttackInbox.pending(queuedAttacks)
            } else emptyList()
            var newPlayerFalling = currentState.player.fallingGarbagePieces
            for ((receiptId, blocks) in pendingAttacks) {
                val createdWave = BlockWarEngine.createFallingGarbagePieceWave(
                    currentState.player.board, newPlayerFalling, blocks)
                verifiedReplay.wave(0,createdWave,receiptId)
                newPlayerFalling = newPlayerFalling + createdWave
            }
            val updatedPlayerPending = newPlayerFalling.sumOf { it.size }

            _gameState.update { state ->
                val currentOpponent = state.opponent
                state.copy(
                    opponent = currentOpponent.copy(
                        board = deserializedBoard,
                        currentPiece = deserializedPiece,
                        fallingGarbagePieces = deserializedOpponentFalling,
                        score = opponentScore,
                        linesCleared = opponentLines,
                        isGameOver = opponentGameOver
                    ),
                    player = state.player.copy(
                        fallingGarbagePieces = newPlayerFalling,
                        pendingGarbage = updatedPlayerPending
                    )
                )
            }

            // Apply locally before acknowledging, and remember each ID before Firebase emits again.
            for ((attackId, _) in pendingAttacks) onlineAttackInbox.markProcessed(attackId)
            if (pendingAttacks.isNotEmpty()) {
                soundManager.playGarbageIncoming()
                syncOnlinePlayerState()
                for ((attackId, _) in pendingAttacks) {
                    firebaseManager.confirmAttackReceived(roomCode, isP1, attackId)
                }
            }
            // The room's committed result determines the winner, not a stale board flag.
        }
    }

    fun closeOnlineLobby() {
        firebaseManager.stopOnlinePresence()
        val currentCode = _gameState.value.onlineRoomCode
        if (currentCode.isNotEmpty() && _gameState.value.isPlayer1 && _gameState.value.matchStatus != MatchStatus.PLAYING) {
            firebaseManager.deleteRoom(currentCode)
        }
        firebaseManager.stopRoomListener()
        _gameState.update {
            it.copy(
                isSearchingOnlineOpponent = false,
                onlineRoomStatus = "menu",
                onlineRoomCode = ""
            )
        }
    }

    fun navigateToMenu() {
        val state = _gameState.value
        val code = state.onlineRoomCode
        val active = state.matchStatus == MatchStatus.PLAYING || state.matchStatus == MatchStatus.PAUSED
        val gameScreen = state.currentScreen == AppScreen.GAME_VS_CPU || state.currentScreen == AppScreen.GAME_ONLINE
        val abandoning = gameScreen && active && matchFinalizationGuard.tryFinalize(true)
        playerLoopJob?.cancel()
        opponentLoopJob?.cancel()
        garbageFallingJob?.cancel()
        firebaseManager.stopRoomListener()
        soundManager.stopBgm()
        if (abandoning && state.gameMode == GameMode.ONLINE_1V1 && code.isNotBlank()) {
            val session = InterruptedOnlineSession(code, playerId, state.isPlayer1, getPlayerName().lowercase().trim(),
                matchRewardId, matchStartTimeMs, state.player.score, state.player.linesCleared,
                totalAttacksSentCounter, state.player.maxCombo)
            onlineSessionStore.save(session)
            firebaseManager.finishOnlineMatch(code, playerId, "abandono") { room ->
                settleInterruptedOnlineSession(session, room)
            }
        } else if (abandoning) {
            val duration = ((System.currentTimeMillis() - matchStartTimeMs) / 1000).coerceAtLeast(1)
            matchHistoryManager.saveMatch(MatchRecord(matchRewardId, System.currentTimeMillis(),
                "VS CPU (${state.cpuDifficulty.displayName}) (DESISTÊNCIA)", false, state.player.linesCleared,
                totalAttacksSentCounter, duration, state.player.score, state.player.maxCombo))
            _gameState.update { it.copy(losses = it.losses + 1) }
        } else if (!active && code.isNotBlank() && state.isPlayer1 && state.matchStatus == MatchStatus.STARTING) {
            firebaseManager.deleteRoom(code)
        }
        firebaseManager.stopOnlinePresence()
        _gameState.update { it.copy(currentScreen = AppScreen.MENU, onlineSessionCancelled = false,
            onlineConnectionMessage = "", matchStatus = MatchStatus.STARTING, isSearchingOnlineOpponent = false,
            onlineRoomCode = "", onlineRoomStatus = "menu", onlineErrorMessage = "") }
    }    private fun startBgmWithEquipped() {
        val equippedBgm = _gameState.value.inventory.equippedItems["musicas"] ?: "arcade"
        soundManager.startBgm(equippedBgm)
    }

    fun startNewMatch() {
        if (verifiedStartInFlight) return
        val state=_gameState.value
        if (state.gameMode==GameMode.ONLINE_1V1 &&
            (state.matchStatus==MatchStatus.VICTORY||state.matchStatus==MatchStatus.DEFEAT)) {
            startQuickMatch();return
        }
        val name=getPlayerName()
        val screen=state.currentScreen
        verifiedStartInFlight=true
        _gameState.update {it.copy(matchStatus=MatchStatus.STARTING,secureStartErrorMessage="",rewardSaveMessage="Preparando partida verificável…")}
        firebaseManager.beginVerifiedMatch(name,if(state.gameMode==GameMode.ONLINE_1V1)"ONLINE" else "CPU",state.onlineRoomCode,state.cpuDifficulty.name,
            onSuccess={data ->
                verifiedStartInFlight=false
                if(getPlayerName()!=name||_gameState.value.currentScreen!=screen)return@beginVerifiedMatch
                matchRewardId=data["id"]?.toString().orEmpty()
                if(matchRewardId.isBlank())return@beginVerifiedMatch
                verifiedReplay.begin(matchRewardId,(data["seed"] as? Number)?.toLong()?:1,
                    (data["opponentSeed"] as? Number)?.toLong()?:2)
                _gameState.update {it.copy(rewardSaveMessage="")}
                startVerifiedMatch()
            },onError={message ->
                verifiedStartInFlight=false
                _gameState.update {it.copy(currentScreen=AppScreen.MENU,secureStartErrorMessage=message,
                    rewardSaveMessage=message,onlineErrorMessage=message,onlineConnectionMessage=message)}
            })
    }

    private fun startVerifiedMatch() {
        playerLoopJob?.cancel()
        opponentLoopJob?.cancel()
        garbageFallingJob?.cancel()

        matchFinalizationGuard.startMatch()
        // Match ID and sequence were issued by the server.
        matchStartTimeMs = System.currentTimeMillis()
        totalAttacksSentCounter = 0
        onlineResultConfirmed = false
        onlineResultPending = false
        onlineFinishReason = ""
        _gameState.update { it.copy(onlineSessionCancelled = false, onlineConnectionMessage = "") }
        // Online deduplication is reset only when entering a different room.

        startBgmWithEquipped()

        val pFirst = nextPlayerPiece()
        val pNext = nextPlayerPiece()
        val pSpawned = BlockPiece.spawn(pFirst)

        val oFirst = nextOpponentPiece()
        val oNext = nextOpponentPiece()
        val oSpawned = BlockPiece.spawn(oFirst)

        _gameState.update { current ->
            current.copy(
                player = PlayerState(
                    board = List(PlayerState.BOARD_ROWS) { List(PlayerState.BOARD_COLS) { null } },
                    currentPiece = pSpawned,
                    ghostPiece = BlockWarEngine.calculateGhost(
                        List(PlayerState.BOARD_ROWS) { List(PlayerState.BOARD_COLS) { null } },
                        pSpawned
                    ),
                    nextPiece = pNext,
                    score = 0,
                    linesCleared = 0,
                    pendingGarbage = 0,
                    fallingGarbagePieces = emptyList(),
                    combo = 0,
                    maxCombo = 0,
                    isGameOver = false
                ),
                opponent = PlayerState(
                    board = List(PlayerState.BOARD_ROWS) { List(PlayerState.BOARD_COLS) { null } },
                    currentPiece = oSpawned,
                    ghostPiece = null,
                    nextPiece = oNext,
                    score = 0,
                    linesCleared = 0,
                    pendingGarbage = 0,
                    fallingGarbagePieces = emptyList(),
                    combo = 0,
                    maxCombo = 0,
                    isGameOver = false
                ),
                matchStatus = MatchStatus.PLAYING,
                attackEvents = emptyList(),
                matchDurationSeconds = 0L,
                totalAttacksSent = 0
            )
        }

        startPlayerLoop()

        if (_gameState.value.gameMode == GameMode.VS_CPU) {
            startOpponentLoop()
        }

        startGarbageFallingLoop()

        if (_gameState.value.gameMode == GameMode.ONLINE_1V1) {
            syncOnlinePlayerState()
        }
    }

    private fun syncOnlinePlayerState() {
        val state = _gameState.value
        if (state.gameMode != GameMode.ONLINE_1V1 || state.onlineRoomCode.isEmpty()) return
        saveOnlineSession()
        firebaseManager.updatePlayerState(
            roomCode = state.onlineRoomCode,
            isPlayer1 = state.isPlayer1,
            board = state.player.board,
            currentPiece = state.player.currentPiece,
            fallingGarbagePieces = state.player.fallingGarbagePieces,
            score = state.player.score,
            lines = state.player.linesCleared,
            isGameOver = state.player.isGameOver
        )
    }

    private fun onMatchVictory() {
        if (_gameState.value.gameMode == GameMode.ONLINE_1V1 && !onlineResultConfirmed) return
        val status = _gameState.value.matchStatus
        if (!matchFinalizationGuard.tryFinalize(status == MatchStatus.PLAYING || status == MatchStatus.PAUSED)) return

        playerLoopJob?.cancel()
        opponentLoopJob?.cancel()
        garbageFallingJob?.cancel()
        soundManager.stopBgm()

        if (_gameState.value.gameMode == GameMode.ONLINE_1V1) {
            firebaseManager.stopRoomListener()
        }

        soundManager.playVictory()
        val durationSec = ((System.currentTimeMillis() - matchStartTimeMs) / 1000).coerceAtLeast(1)
        val modeStr = if (_gameState.value.gameMode == GameMode.VS_CPU) {
            "VS CPU (${_gameState.value.cpuDifficulty.displayName})"
        } else {
            "ONLINE 1V1"
        }

        val record = MatchRecord(
            id = matchRewardId,
            timestamp = System.currentTimeMillis(),
            gameMode = modeStr,
            isVictory = true,
            linesCleared = _gameState.value.player.linesCleared,
            attacksSent = totalAttacksSentCounter,
            durationSeconds = durationSec,
            score = _gameState.value.player.score,
            maxCombo = _gameState.value.player.maxCombo
        )

        matchHistoryManager.saveMatch(record)

        val pName = getPlayerName()

        updateMissionProgress("daily_first_win", 1)
        updateMissionProgress("weekly_constancy", 1)
        updateMissionProgress("monthly_collector", 1)

        saveCompletedMatchReward(isVictory = true)
        onlineSessionStore.clear(matchRewardId)
        firebaseManager.stopOnlinePresence(markOffline = false)
        if (_gameState.value.gameMode == GameMode.ONLINE_1V1) updateMissionProgress("weekly_online_warrior", 1)

        _gameState.update {
            it.copy(
                matchStatus = MatchStatus.VICTORY,
                wins = it.wins + 1,
                matchDurationSeconds = durationSec,
                totalAttacksSent = totalAttacksSentCounter
            )
        }

    }

    private fun onMatchDefeat() {
        if (_gameState.value.gameMode == GameMode.ONLINE_1V1 && !onlineResultConfirmed) {
            val current = _gameState.value
            if (current.matchStatus == MatchStatus.PLAYING || current.matchStatus == MatchStatus.PAUSED) {
                saveOnlineSession()
                onlineResultPending = true
                _gameState.update { it.copy(matchStatus = MatchStatus.PAUSED,
                    onlineConnectionMessage = "Confirmando o resultado da partida…") }
                firebaseManager.finishOnlineMatch(current.onlineRoomCode, playerId, "ko")
            }
            return
        }
        val status = _gameState.value.matchStatus
        if (!matchFinalizationGuard.tryFinalize(status == MatchStatus.PLAYING || status == MatchStatus.PAUSED)) return

        playerLoopJob?.cancel()
        opponentLoopJob?.cancel()
        garbageFallingJob?.cancel()
        soundManager.stopBgm()

        if (_gameState.value.gameMode == GameMode.ONLINE_1V1) {
            firebaseManager.stopRoomListener()
        }

        soundManager.playGameOver()
        val durationSec = ((System.currentTimeMillis() - matchStartTimeMs) / 1000).coerceAtLeast(1)
        val modeStr = if (_gameState.value.gameMode == GameMode.VS_CPU) {
            "VS CPU (${_gameState.value.cpuDifficulty.displayName})"
        } else {
            "ONLINE 1V1"
        }

        val record = MatchRecord(
            id = matchRewardId,
            timestamp = System.currentTimeMillis(),
            gameMode = modeStr,
            isVictory = false,
            linesCleared = _gameState.value.player.linesCleared,
            attacksSent = totalAttacksSentCounter,
            durationSeconds = durationSec,
            score = _gameState.value.player.score,
            maxCombo = _gameState.value.player.maxCombo
        )

        matchHistoryManager.saveMatch(record)

        val pName = getPlayerName()

        val forfeited = _gameState.value.gameMode == GameMode.ONLINE_1V1 &&
            (onlineFinishReason == "desconexao" || onlineFinishReason == "abandono")
        if (forfeited) {
            queueMatchReward(PendingMatchReward(matchRewardId, getPlayerName().lowercase().trim(), -10, 0, false,
                isOnline=true,matchPoints=_gameState.value.player.score,linesCleared=_gameState.value.player.linesCleared,
                attacksSent=totalAttacksSentCounter,durationSeconds=((System.currentTimeMillis()-matchStartTimeMs)/1000).coerceAtLeast(1),
                finishedAtMillis=lastOnlineRoom?.finishedAt?.takeIf {it>0}?:System.currentTimeMillis()))
            _gameState.update { it.copy(onlineConnectionMessage = "Saída: pontos da partida não creditados; -10 BW e nenhum Blockoin.") }
        } else {
            updateMissionProgress("weekly_constancy", 1)
            saveCompletedMatchReward(isVictory = false)
        }
        onlineSessionStore.clear(matchRewardId)
        firebaseManager.stopOnlinePresence(markOffline = false)

        _gameState.update {
            it.copy(
                matchStatus = MatchStatus.DEFEAT,
                losses = it.losses + 1,
                matchDurationSeconds = durationSec,
                totalAttacksSent = totalAttacksSentCounter,
                player = it.player.copy(isGameOver = true)
            )
        }
        if (_gameState.value.gameMode == GameMode.ONLINE_1V1) {
            syncOnlinePlayerState()
        }

    }

    fun moveLeft() {
        if (_gameState.value.matchStatus != MatchStatus.PLAYING) return
        val player = _gameState.value.player
        val piece = player.currentPiece ?: return

        val moved = BlockWarEngine.tryMove(player.board, piece, -1, 0)
        if (moved != null) {
            val ghost = BlockWarEngine.calculateGhost(player.board, moved)
            _gameState.update { it.copy(player = player.copy(currentPiece = moved, ghostPiece = ghost)) }
            soundManager.playMove()
            syncOnlinePlayerState()
        }
    }

    fun moveRight() {
        if (_gameState.value.matchStatus != MatchStatus.PLAYING) return
        val player = _gameState.value.player
        val piece = player.currentPiece ?: return

        val moved = BlockWarEngine.tryMove(player.board, piece, 1, 0)
        if (moved != null) {
            val ghost = BlockWarEngine.calculateGhost(player.board, moved)
            _gameState.update { it.copy(player = player.copy(currentPiece = moved, ghostPiece = ghost)) }
            soundManager.playMove()
            syncOnlinePlayerState()
        }
    }

    fun rotate() {
        if (_gameState.value.matchStatus != MatchStatus.PLAYING) return
        val player = _gameState.value.player
        val piece = player.currentPiece ?: return

        val rotated = BlockWarEngine.tryRotate(player.board, piece)
        if (rotated != null) {
            val ghost = BlockWarEngine.calculateGhost(player.board, rotated)
            _gameState.update { it.copy(player = player.copy(currentPiece = rotated, ghostPiece = ghost)) }
            soundManager.playRotate()
            syncOnlinePlayerState()
        }
    }

    fun softDrop() {
        if (_gameState.value.matchStatus != MatchStatus.PLAYING) return
        val player = _gameState.value.player
        val piece = player.currentPiece ?: return

        val currentBoard = player.board
        if (!BlockWarEngine.isValidPosition(currentBoard, piece.move(0, 1))) {
            viewModelScope.launch {
                lockPlayerPiece()
            }
            return
        }

        val moved = BlockWarEngine.tryMove(currentBoard, piece, 0, 1)
        if (moved != null) {
            _gameState.update {
                it.copy(
                    player = player.copy(
                        currentPiece = moved,
                        score = player.score,
                        lastImpactTime = 0L,
                        impactIntensity = 0f
                    )
                )
            }
            syncOnlinePlayerState()
        } else {
            viewModelScope.launch {
                lockPlayerPiece()
            }
        }
    }

    fun hardDrop() {
        if (_gameState.value.matchStatus != MatchStatus.PLAYING) return
        val player = _gameState.value.player
        val piece = player.currentPiece ?: return

        val ghost = BlockWarEngine.calculateGhost(player.board, piece)
        val dropDistance = ghost.y - piece.y
        _gameState.update {
            it.copy(
                player = player.copy(
                    currentPiece = ghost,
                    score = player.score,
                    impactColumn = ghost.getOccupiedCoordinates().map { it.first + .5f }.average().toFloat(),
                    impactRow = ghost.getOccupiedCoordinates().map { it.second + .5f }.average().toFloat(),
                    lastImpactTime = System.currentTimeMillis(),
                    impactIntensity = 20f,
                    impactPieceType = piece.type
                )
            )
        }
        soundManager.playDrop()
        viewModelScope.launch {
            if (_gameState.value.player.currentPiece != ghost) return@launch
            lockPlayerPiece()
        }
    }

    fun toggleAudio() {
        val newState = !soundManager.isEnabled
        soundManager.isEnabled = newState
        if (newState && _gameState.value.matchStatus == MatchStatus.PLAYING) {
            startBgmWithEquipped()
        } else {
            soundManager.stopBgm()
        }
        _gameState.update { it.copy(isAudioEnabled = newState) }
    }

    fun togglePause() {
        if (_gameState.value.gameMode != GameMode.VS_CPU) return
        val currentStatus = _gameState.value.matchStatus
        if (currentStatus == MatchStatus.PLAYING) {
            soundManager.stopBgm()
            _gameState.update { it.copy(matchStatus = MatchStatus.PAUSED) }
        } else if (currentStatus == MatchStatus.PAUSED) {
            startBgmWithEquipped()
            _gameState.update { it.copy(matchStatus = MatchStatus.PLAYING) }
        }
    }

    @Volatile
    private var isAppActive: Boolean = false

    fun onWindowVisibilityChanged(isVisible: Boolean) {
        // Kept for compatibility. Window focus/visibility is not the Activity lifecycle:
        // pause menus, confirmation dialogs and the connection dialog must not suspend it.
    }

    fun onAppPause() {
        if (!isAppActive) return
        isAppActive = false
        saveOnlineSession()
        firebaseManager.setOnlineForeground(false)
        soundManager.stopBgm()
        if (_gameState.value.matchStatus == MatchStatus.PLAYING && _gameState.value.gameMode == GameMode.VS_CPU) {
            _gameState.update { it.copy(matchStatus = MatchStatus.PAUSED) }
        }
    }

    fun onAppResume() {
        if (isAppActive) return
        isAppActive = true
        firebaseManager.setOnlineForeground(true)
        updateOnlineConnectionState()
        recoverInterruptedOnlineSession()
        startEconomySync(getPlayerName())
        retryEconomyOperations()
        refreshMissionPeriod()
        syncMissionProgress()
        retryPendingMatchRewards()
        if (_gameState.value.matchStatus == MatchStatus.PLAYING && _gameState.value.isAudioEnabled) {
            startBgmWithEquipped()
        }
    }

    private fun startGarbageFallingLoop() {
        garbageFallingJob?.cancel()
        garbageFallingJob = viewModelScope.launch {
            while (true) {
                if (!isAppActive) {
                    delay(300)
                    continue
                }
                val state = _gameState.value
                if (state.matchStatus == MatchStatus.PLAYING) {

                    if (state.player.fallingGarbagePieces.isNotEmpty() && !state.player.isClearingLines) {
                        verifiedReplay.step(0,state.player.currentPiece)
                        val result = BlockWarEngine.stepFallingGarbagePieces(
                            state.player.board,
                            state.player.fallingGarbagePieces
                        )

                        val updatedPending = result.remainingFalling.sumOf { it.size }
                        val ghost = state.player.currentPiece?.let {
                            BlockWarEngine.calculateGhost(result.newBoard, it)
                        }

                        _gameState.update {
                            it.copy(
                                player = it.player.copy(
                                    board = result.newBoard,
                                    fallingGarbagePieces = result.remainingFalling,
                                    pendingGarbage = updatedPending,
                                    ghostPiece = ghost
                                )
                            )
                        }

                        if (result.isTopOut) {
                            onMatchDefeat()
                        }

                        if (state.gameMode == GameMode.ONLINE_1V1) {
                            syncOnlinePlayerState()
                        }
                    }

                    if (state.gameMode == GameMode.VS_CPU && state.opponent.fallingGarbagePieces.isNotEmpty() && !state.opponent.isClearingLines) {
                        verifiedReplay.step(1,state.opponent.currentPiece)
                        val result = BlockWarEngine.stepFallingGarbagePieces(
                            state.opponent.board,
                            state.opponent.fallingGarbagePieces
                        )

                        val updatedPending = result.remainingFalling.sumOf { it.size }

                        _gameState.update {
                            it.copy(
                                opponent = it.opponent.copy(
                                    board = result.newBoard,
                                    fallingGarbagePieces = result.remainingFalling,
                                    pendingGarbage = updatedPending
                                )
                            )
                        }

                        if (result.isTopOut) {
                            onMatchVictory()
                        }
                    }
                }
                delay(60)
            }
        }
    }

    private fun startPlayerLoop() {
        playerLoopJob?.cancel()
        playerLoopJob = viewModelScope.launch {
            while (true) {
                if (!isAppActive) {
                    delay(300)
                    continue
                }
                val state = _gameState.value
                if (state.matchStatus == MatchStatus.PLAYING) {
                    val player = state.player
                    val piece = player.currentPiece

                    if (piece != null) {
                        val currentBoard = player.board
                        if (!BlockWarEngine.isValidPosition(currentBoard, piece.move(0, 1))) {
                            lockPlayerPiece()
                        } else {
                            val moved = BlockWarEngine.tryMove(currentBoard, piece, 0, 1)
                            if (moved != null) {
                                _gameState.update { it.copy(player = player.copy(currentPiece = moved, lastImpactTime = 0L, impactIntensity = 0f)) }
                                if (state.gameMode == GameMode.ONLINE_1V1) {
                                    syncOnlinePlayerState()
                                }
                            } else {
                                lockPlayerPiece()
                            }
                        }
                    }
                    val speed = 800L
                    delay(speed)
                } else {
                    delay(250)
                }
            }
        }
    }

    private suspend fun lockPlayerPiece() {
        val currentState = _gameState.value
        val player = currentState.player
        val piece = player.currentPiece ?: return

        soundManager.playDrop()

        verifiedReplay.lock(0,piece)
        val lockResult = BlockWarEngine.lockPiece(player.board, piece)
        val boardWithPiece = lockResult.boardWithPiece
        val newBoard = lockResult.newBoard
        val cleared = lockResult.clearedRows.size

        val isHardDrop = player.impactIntensity >= 18f
        val intensity = if (isHardDrop) player.impactIntensity else 6f
        val impactTime = if (isHardDrop) player.lastImpactTime else System.currentTimeMillis()
        val impactCells = piece.getOccupiedCoordinates()
        val impactColumn = impactCells.map { it.first + .5f }.average().toFloat()
        val impactRow = impactCells.map { it.second + .5f }.average().toFloat()

        if (cleared > 0) {
            _gameState.update {
                it.copy(
                    player = it.player.copy(
                        board = boardWithPiece,
                        currentPiece = null,
                        clearingRows = lockResult.clearedRows,
                        isClearingLines = true,
                        lastClearTime = System.currentTimeMillis(),
                        lastImpactTime = impactTime,
                        impactColumn = impactColumn, impactRow = impactRow,
                        impactIntensity = intensity,
                        impactPieceType = piece.type
                    )
                )
            }

            soundManager.playLineClear(cleared)

            updateMissionProgress("daily_cleaner", cleared)
            updateMissionProgress("monthly_demolisher", cleared)

            if (_gameState.value.gameMode == GameMode.ONLINE_1V1) {
                syncOnlinePlayerState()
            }

            delay(200)
        }

        verifiedReplay.commit(0)
        val newLines = player.linesCleared + cleared
        val newScore = player.score + BlockWarEngine.calculatePoints(cleared, if (cleared > 0) player.combo + 1 else 0)
        val newCombo = if (cleared > 0) player.combo + 1 else 0
        if (newCombo >= 2) {
            _gameState.update {
                it.copy(player = it.player.copy(maxCombo = maxOf(it.player.maxCombo, newCombo)))
            }
        }

        if (newCombo >= 3) {
            updateMissionProgress("daily_combo_master", newCombo, isSet = true)
        }

        val attackBlocks = BlockWarEngine.calculateAttackBlocks(cleared)
        if (attackBlocks > 0) {
            totalAttacksSentCounter += attackBlocks
            updateMissionProgress("weekly_heavy_attack", attackBlocks)
        }

        val opponent = _gameState.value.opponent
        val newOpponentFalling = if (attackBlocks > 0) {
            _gameState.update {
                it.copy(
                    attackEvents = (it.attackEvents + AttackEvent(blocks = attackBlocks, fromPlayer = true)).takeLast(4)
                )
            }
            if (currentState.gameMode == GameMode.VS_CPU) {
                soundManager.playGarbageIncoming()
                BlockWarEngine.createFallingGarbagePieceWave(opponent.board, opponent.fallingGarbagePieces, attackBlocks).also {verifiedReplay.wave(1,it)}
            } else {
                if (currentState.onlineRoomCode.isNotEmpty()) {
                    firebaseManager.sendAttackToOpponent(
                        currentState.onlineRoomCode,
                        currentState.isPlayer1,
                        attackBlocks
                    )
                }
                emptyList()
            }
        } else emptyList()

        // Commit the completed move before either end-of-match check.
        _gameState.update {
            val updatedOpponentFalling = it.opponent.fallingGarbagePieces + newOpponentFalling
            val updatedOpponentPending = updatedOpponentFalling.sumOf { p -> p.size }

            it.copy(
                player = it.player.copy(
                    board = newBoard,
                    currentPiece = null,
                    ghostPiece = null,
                    score = newScore,
                    linesCleared = newLines,
                    combo = newCombo,
                    clearingRows = emptyList(),
                    isClearingLines = false,
                    lastClearCount = cleared,
                    lastClearTime = it.player.lastClearTime,
                    lastImpactTime = impactTime,
                        impactColumn = impactColumn, impactRow = impactRow,
                    impactIntensity = intensity,
                    impactPieceType = piece.type
                ),
                opponent = it.opponent.copy(
                    fallingGarbagePieces = updatedOpponentFalling,
                    pendingGarbage = updatedOpponentPending
                )
            )
        }

        if (lockResult.isTopOut) {
            onMatchDefeat()
            return
        }

        val nextType = player.nextPiece
        val upcomingType = nextPlayerPiece()
        val spawned = BlockPiece.spawn(nextType)

        val canSpawn = BlockWarEngine.isValidPosition(newBoard, spawned)
        if (!canSpawn) {
            onMatchDefeat()
            return
        }

        val ghost = BlockWarEngine.calculateGhost(newBoard, spawned)

        _gameState.update {
            it.copy(player = it.player.copy(
                currentPiece = spawned,
                ghostPiece = ghost,
                nextPiece = upcomingType,
            ))
        }

        if (_gameState.value.gameMode == GameMode.ONLINE_1V1) {
            syncOnlinePlayerState()
        }
    }

    private fun startOpponentLoop() {
        opponentLoopJob?.cancel()
        opponentLoopJob = viewModelScope.launch {
            while (true) {
                if (!isAppActive) {
                    delay(300)
                    continue
                }
                val state = _gameState.value
                if (state.gameMode != GameMode.VS_CPU) {
                    break
                }

                if (state.matchStatus == MatchStatus.PLAYING) {
                    val opponent = state.opponent
                    val piece = opponent.currentPiece
                    val difficulty = state.cpuDifficulty

                    if (piece != null) {
                        val decision = opponentAI.findBestMove(opponent.board, piece.type, difficulty,verifiedReplay::nextCpuRandom)
                        verifiedReplay.cpuDecision(decision)

                        val stepDelay = difficulty.stepDelayMs
                        val dropDelay = difficulty.dropDelayMs

                        var currentRotPiece: BlockPiece = piece
                        while (currentRotPiece.rotation != decision.targetRotation) {
                            if (_gameState.value.matchStatus != MatchStatus.PLAYING) break
                            val currentBoard = _gameState.value.opponent.board
                            if (!BlockWarEngine.isValidPosition(currentBoard, currentRotPiece)) {
                                var validPiece = currentRotPiece
                                while (!BlockWarEngine.isValidPosition(currentBoard, validPiece) && validPiece.y > -4) {
                                    validPiece = validPiece.move(0, -1)
                                }
                                verifiedReplay.cpuLift(validPiece)
                                currentRotPiece = validPiece
                                _gameState.update { it.copy(opponent = it.opponent.copy(currentPiece = validPiece)) }
                                break
                            }
                            val nextRot = BlockWarEngine.tryRotate(currentBoard, currentRotPiece)
                            if (nextRot != null) {
                                verifiedReplay.cpuRotate()
                                currentRotPiece = nextRot
                                _gameState.update { it.copy(opponent = it.opponent.copy(currentPiece = nextRot)) }
                            } else {
                                break
                            }
                            delay(stepDelay)
                        }

                        var currentPosPiece: BlockPiece = currentRotPiece
                        val boardBeforeMove = _gameState.value.opponent.board
                        if (!BlockWarEngine.isValidPosition(boardBeforeMove, currentPosPiece)) {
                            var validPiece = currentPosPiece
                            while (!BlockWarEngine.isValidPosition(boardBeforeMove, validPiece) && validPiece.y > -4) {
                                validPiece = validPiece.move(0, -1)
                            }
                            verifiedReplay.cpuLift(validPiece)
                            currentPosPiece = validPiece
                            _gameState.update { it.copy(opponent = it.opponent.copy(currentPiece = validPiece)) }
                        } else {
                            while (currentPosPiece.x != decision.targetX) {
                                if (_gameState.value.matchStatus != MatchStatus.PLAYING) break
                                val currentBoard = _gameState.value.opponent.board
                                if (!BlockWarEngine.isValidPosition(currentBoard, currentPosPiece)) {
                                    var validPiece = currentPosPiece
                                    while (!BlockWarEngine.isValidPosition(currentBoard, validPiece) && validPiece.y > -4) {
                                        validPiece = validPiece.move(0, -1)
                                    }
                                    verifiedReplay.cpuLift(validPiece)
                            currentPosPiece = validPiece
                                    _gameState.update { it.copy(opponent = it.opponent.copy(currentPiece = validPiece)) }
                                    break
                                }
                                val dx = if (decision.targetX > currentPosPiece.x) 1 else -1
                                val moved = BlockWarEngine.tryMove(currentBoard, currentPosPiece, dx, 0)
                                if (moved != null) {
                                    verifiedReplay.cpuMove(dx,0)
                                currentPosPiece = moved
                                    _gameState.update { it.copy(opponent = it.opponent.copy(currentPiece = moved)) }
                                } else {
                                    break
                                }
                                delay(stepDelay)
                            }
                        }

                        val boardBeforeDrop = _gameState.value.opponent.board
                        if (!BlockWarEngine.isValidPosition(boardBeforeDrop, currentPosPiece)) {
                            var validPiece = currentPosPiece
                            while (!BlockWarEngine.isValidPosition(boardBeforeDrop, validPiece) && validPiece.y > -4) {
                                validPiece = validPiece.move(0, -1)
                            }
                            verifiedReplay.cpuLift(validPiece)
                            currentPosPiece = validPiece
                            _gameState.update { it.copy(opponent = it.opponent.copy(currentPiece = validPiece)) }
                        } else {
                            while (_gameState.value.matchStatus == MatchStatus.PLAYING) {
                                val currentBoard = _gameState.value.opponent.board
                                if (!BlockWarEngine.isValidPosition(currentBoard, currentPosPiece)) {
                                    var validPiece = currentPosPiece
                                    while (!BlockWarEngine.isValidPosition(currentBoard, validPiece) && validPiece.y > -4) {
                                        validPiece = validPiece.move(0, -1)
                                    }
                                    verifiedReplay.cpuLift(validPiece)
                            currentPosPiece = validPiece
                                    _gameState.update { it.copy(opponent = it.opponent.copy(currentPiece = validPiece)) }
                                    break
                                }

                                val movedDown = BlockWarEngine.tryMove(currentBoard, currentPosPiece, 0, 1)
                                if (movedDown != null) {
                                    verifiedReplay.cpuMove(0,1)
                                    currentPosPiece = movedDown
                                    _gameState.update { it.copy(opponent = it.opponent.copy(currentPiece = movedDown)) }
                                    delay(dropDelay)
                                } else {
                                    break
                                }
                            }
                        }

                        if (_gameState.value.matchStatus == MatchStatus.PLAYING) {
                            val finalBoard = _gameState.value.opponent.board
                            if (!BlockWarEngine.isValidPosition(finalBoard, currentPosPiece)) {
                                var validPiece = currentPosPiece
                                while (!BlockWarEngine.isValidPosition(finalBoard, validPiece) && validPiece.y > -4) {
                                    validPiece = validPiece.move(0, -1)
                                }
                                verifiedReplay.cpuLift(validPiece)
                            currentPosPiece = validPiece
                                _gameState.update { it.copy(opponent = it.opponent.copy(currentPiece = validPiece)) }
                            }
                            lockOpponentPiece()
                            delay(stepDelay * 2)
                        }
                    } else {
                        delay(150)
                    }
                } else {
                    delay(200)
                }
            }
        }
    }

    private suspend fun lockOpponentPiece() {
        val currentState = _gameState.value
        val opponent = currentState.opponent
        val piece = opponent.currentPiece ?: return

        verifiedReplay.lock(1,piece)
        val lockResult = BlockWarEngine.lockPiece(opponent.board, piece)
        val boardWithPiece = lockResult.boardWithPiece
        val newBoard = lockResult.newBoard
        val cleared = lockResult.clearedRows.size

        if (cleared > 0) {
            _gameState.update {
                it.copy(
                    opponent = it.opponent.copy(
                        board = boardWithPiece,
                        currentPiece = null,
                        clearingRows = lockResult.clearedRows,
                        isClearingLines = true,
                        lastClearTime = System.currentTimeMillis()
                    )
                )
            }
            delay(200)
        }
        verifiedReplay.commit(1)
        val newLines = opponent.linesCleared + cleared
        val newScore = opponent.score + BlockWarEngine.calculatePoints(cleared, if (cleared > 0) opponent.combo + 1 else 0)
        val newCombo = if (cleared > 0) opponent.combo + 1 else 0

        val attackBlocks = BlockWarEngine.calculateAttackBlocks(cleared)

        val player = _gameState.value.player
        val newPlayerFalling = if (attackBlocks > 0) {
            soundManager.playGarbageIncoming()
            _gameState.update {
                it.copy(
                    attackEvents = (it.attackEvents + AttackEvent(blocks = attackBlocks, fromPlayer = false)).takeLast(4)
                )
            }
            BlockWarEngine.createFallingGarbagePieceWave(player.board, player.fallingGarbagePieces, attackBlocks).also {verifiedReplay.wave(0,it)}
        } else emptyList()

        // Commit the completed move before either end-of-match check.
        _gameState.update {
            val updatedPlayerFalling = it.player.fallingGarbagePieces + newPlayerFalling
            val updatedPlayerPending = updatedPlayerFalling.sumOf { p -> p.size }

            it.copy(
                opponent = it.opponent.copy(
                    board = newBoard,
                    currentPiece = null,
                    score = newScore,
                    linesCleared = newLines,
                    combo = newCombo,
                    clearingRows = emptyList(),
                    isClearingLines = false,
                    lastClearCount = cleared,
                    lastClearTime = System.currentTimeMillis()
                ),
                player = it.player.copy(
                    fallingGarbagePieces = updatedPlayerFalling,
                    pendingGarbage = updatedPlayerPending
                )
            )
        }

        if (lockResult.isTopOut) {
            onMatchVictory()
            return
        }

        val nextType = opponent.nextPiece
        val upcomingType = nextOpponentPiece()
        val spawned = BlockPiece.spawn(nextType)

        val canSpawn = BlockWarEngine.isValidPosition(newBoard, spawned)
        if (!canSpawn) {
            onMatchVictory()
            return
        }

        _gameState.update {
            it.copy(opponent = it.opponent.copy(
                currentPiece = spawned,
                nextPiece = upcomingType,
            ))
        }
    }

    override fun onCleared() {
        statisticsGeneration++
        firebaseManager.stopOnlineStatisticsListener()
        profileRequestGate.invalidate()
        saveOnlineSession()
        firebaseManager.stopOnlinePresence()
        super.onCleared()
        firebaseManager.stopEconomyListener()
        playerLoopJob?.cancel()
        opponentLoopJob?.cancel()
        garbageFallingJob?.cancel()
        // Closing the host must not erase the opponent's match or its committed result.
        firebaseManager.stopRoomListener()
    }
}
/** Serializes completion/abandonment decisions on the ViewModel's main thread. */
internal class MatchFinalizationGuard {
    private var finalized = true
    fun startMatch() { finalized = false }
    fun tryFinalize(isActive: Boolean): Boolean {
        if (!isActive || finalized) return false
        finalized = true
        return true
    }
}
