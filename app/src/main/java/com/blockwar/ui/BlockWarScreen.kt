package com.blockwar.ui

import com.blockwar.ui.components.CosmeticScene

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.blockwar.model.AppScreen
import com.blockwar.model.ControlMode
import com.blockwar.model.GameMode
import com.blockwar.model.MatchStatus
import com.blockwar.ui.components.AttackBanner
import com.blockwar.ui.components.BoardCanvas
import com.blockwar.ui.components.ControlsEditorScreen
import com.blockwar.ui.components.GameOverDialog
import com.blockwar.ui.components.AvatarImage
import com.blockwar.ui.components.AvatarWithFrame
import com.blockwar.ui.components.MatchHistoryDialog
import com.blockwar.ui.components.MiniBoardCanvas
import com.blockwar.ui.components.ParticleController
import com.blockwar.ui.components.ParticleOverlay
import com.blockwar.ui.components.PiecePreviewBox
import com.blockwar.ui.components.StartScreen
import com.blockwar.ui.components.VirtualControls
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun BlockWarScreen(
    viewModel: BlockWarViewModel = viewModel(),
    onGooglePlayGamesSignIn: () -> Unit = {}
) {
    val gameState by viewModel.gameState.collectAsState()
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showAbandonConfirmDialog by remember { mutableStateOf(false) }
    var showPauseMenu by remember { mutableStateOf(false) }
    var wasPausedBeforeAbandonDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE, androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    viewModel.onAppPause()
                }
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    viewModel.onAppResume()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Intercept system back button during active gameplay
    BackHandler(enabled = gameState.matchStatus == MatchStatus.PLAYING || gameState.matchStatus == MatchStatus.PAUSED) {
        wasPausedBeforeAbandonDialog = true
        if (gameState.gameMode == GameMode.VS_CPU && gameState.matchStatus == MatchStatus.PLAYING) {
            viewModel.togglePause()
        }
        showPauseMenu = true
    }

    LaunchedEffect(gameState.matchStatus, gameState.currentScreen) {
        if (gameState.matchStatus == MatchStatus.VICTORY || gameState.matchStatus == MatchStatus.DEFEAT ||
            gameState.currentScreen == AppScreen.MENU) {
            showPauseMenu = false
            showAbandonConfirmDialog = false
        }
    }
    val shakeOffsetX = remember { Animatable(0f) }
    val shakeOffsetY = remember { Animatable(0f) }
    val particleController = viewModel.particleController

    // Screen Shake Effect when player receives garbage pieces, pending garbage warning, or hardDrop impact (impactIntensity >= 18f)
    LaunchedEffect(gameState.player.fallingGarbagePieces.size, gameState.player.pendingGarbage, gameState.player.lastImpactTime) {
        if (gameState.player.fallingGarbagePieces.isNotEmpty() || gameState.player.pendingGarbage > 0 || gameState.player.impactIntensity >= 18f) {
            val isHeavy = gameState.player.impactIntensity >= 18f || gameState.player.fallingGarbagePieces.isNotEmpty()
            val shakes = if (isHeavy) 8 else 4
            val maxOffset = if (isHeavy) 16f else 8f
            repeat(shakes) {
                shakeOffsetX.snapTo((Random.nextFloat() - 0.5f) * maxOffset)
                shakeOffsetY.snapTo((Random.nextFloat() - 0.5f) * maxOffset)
                delay(25)
            }
            shakeOffsetX.animateTo(0f)
            shakeOffsetY.animateTo(0f)
        }
    }

    if (gameState.currentScreen == AppScreen.MENU) {
        StartScreen(
            gameState = gameState,
            onLogout = { viewModel.logoutProfile() },
            onGooglePlayGamesSignIn = onGooglePlayGamesSignIn,
            onStartVsCpu = { difficulty -> viewModel.startVsCpuMatch(difficulty) },
            onOpenOnlineLobby = { viewModel.openOnlineLobby() },
            onStartQuickMatch = { viewModel.startQuickMatch() },
            onCancelQuickMatch = { viewModel.cancelQuickMatch() },
            onCreateOnlineRoom = { viewModel.createOnlineRoom() },
            onJoinOnlineRoom = { code -> viewModel.joinOnlineRoom(code) },
            onCloseOnlineLobby = { viewModel.closeOnlineLobby() },
            onOpenControlsEditor = { viewModel.openControlsEditor() },
            onOpenHistory = { showHistoryDialog = true },
            onToggleAudio = { viewModel.toggleAudio() },
            getPlayerName = { viewModel.getPlayerName() },
            getPlayerId = { viewModel.getPlayerId() },
            hasSavedNickname = { viewModel.hasSavedNickname() },
            onSavePlayerName = { name, pin -> viewModel.savePlayerName(name, pin) },
            onFetchLeaderboard = { onSuccess, onError -> viewModel.fetchLeaderboard(onSuccess, onError) },
            onClearNicknameError = { viewModel.clearNicknameError() },
            getLocalPin = { viewModel.getLocalPin() },
            onSaveLocalPin = { pin -> viewModel.saveLocalPin(pin) },
            onCheckNicknameExists = { name, onResult -> viewModel.checkNicknameExists(name, onResult) },
            getPatentInfo = { pontosBw -> viewModel.getPatentInfo(pontosBw) },
            onBuyItem = { item -> viewModel.buyItem(item) },
            onEquipItem = { item -> viewModel.equipItem(item) },
            onClaimMission = { missionId -> viewModel.claimMission(missionId) },
            onAddCoins = { amount -> viewModel.addCoins(amount) },
            getAggregatedStats = { viewModel.getOnlineStatistics() },
            getMatchHistory = { viewModel.getMatchHistory() },
            onSaveControlSettings = { settings -> viewModel.saveControlSettings(settings) }
        )

        if (showHistoryDialog) {
            MatchHistoryDialog(
                stats = viewModel.getAggregatedStats(),
                history = viewModel.getMatchHistory(),
                onClearHistory = { viewModel.clearMatchHistory() },
                onDismiss = { showHistoryDialog = false }
            )
        }
        return
    }

    if (gameState.currentScreen == AppScreen.CUSTOMIZE_CONTROLS) {
        ControlsEditorScreen(
            initialSettings = gameState.controlSettings,
            onSaveSettings = { updatedSettings -> viewModel.saveControlSettings(updatedSettings) },
            onResetDefaults = { viewModel.resetControlSettings() },
            onCancel = { viewModel.navigateToMenu() }
        )
        return
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("block_war_screen"),
        containerColor = Color(0xFF101116)
    ) { innerPadding ->
        val equippedSkin = gameState.inventory.equippedItems["blocos"] ?: "neon"
        val equippedVfx = gameState.inventory.equippedItems["efeitos"] ?: "sparks"
        val equippedBgm = gameState.inventory.equippedItems["musicas"] ?: "arcade"
        val equippedBg = gameState.inventory.equippedItems["fundos"] ?: "space"
        val equippedAnim = gameState.inventory.equippedItems["animacoes"] ?: "anim_flash"

        LaunchedEffect(gameState.matchStatus, equippedBgm, gameState.isAudioEnabled) {
            if (gameState.matchStatus == MatchStatus.PLAYING && gameState.isAudioEnabled) {
                viewModel.soundManager.startBgm(equippedBgm)
            } else {
                viewModel.soundManager.stopBgm()
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                viewModel.soundManager.stopBgm()
            }
        }

        val bgBrush = when (equippedBg) {
            "matrix" -> Brush.verticalGradient(
                listOf(
                    Color(0xFF001508),
                    Color(0xFF032612),
                    Color(0xFF011409),
                    Color(0xFF000803)
                )
            )
            "sunset" -> Brush.verticalGradient(
                listOf(
                    Color(0xFF240A34),
                    Color(0xFF4A1040),
                    Color(0xFF280B3A),
                    Color(0xFF0E0416)
                )
            )
            "arena" -> Brush.verticalGradient(
                listOf(
                    Color(0xFF0A153A),
                    Color(0xFF132A6A),
                    Color(0xFF0E1A42),
                    Color(0xFF050918)
                )
            )
            else -> Brush.verticalGradient(
                listOf(
                    Color(0xFF15161D),
                    Color(0xFF101116),
                    Color(0xFF090B0F)
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(bgBrush)
        ) {
            if (equippedBg.startsWith("fundo_") && equippedBg != "fundo_padrao") CosmeticScene(equippedBg, Modifier.fillMaxSize())

            Canvas(Modifier.fillMaxSize()) {
                val step = 18.dp.toPx()
                var y = 0f
                while (y < size.height) {
                    var x = 0f
                    while (x < size.width) {
                        drawCircle(Color(0xFFCF5CFF).copy(alpha = 0.16f),
                            radius = 0.65.dp.toPx(), center = Offset(x, y))
                        x += step
                    }
                    y += step
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 8.dp, end = 8.dp, top = 2.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // ==========================================
                // 1. TOP ARCADE HEADER BAR
                // ==========================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (gameState.matchStatus == MatchStatus.PLAYING || gameState.matchStatus == MatchStatus.PAUSED) {
                                    wasPausedBeforeAbandonDialog = true
                                    if (gameState.gameMode == GameMode.VS_CPU && gameState.matchStatus == MatchStatus.PLAYING) {
                                        viewModel.togglePause()
                                    }
                                    showPauseMenu = true
                                } else {
                                    viewModel.navigateToMenu()
                                }
                            },
                            modifier = Modifier
                                .size(36.dp).background(Color(0xFF1E1F25), RoundedCornerShape(6.dp))
                                .testTag("header_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Menu Principal",
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(Modifier.weight(1f)) {
                            Text(
                                text = if (gameState.gameMode == GameMode.VS_CPU)
                                    "BATALHA VS CPU · ${gameState.cpuDifficulty.displayName}" else "BATALHA ONLINE",
                                color = Color(0xFF00F0FF), fontSize = 12.sp,
                                fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.toggleAudio() },
                            modifier = Modifier
                                .size(36.dp).background(Color(0xFF1E1F25), RoundedCornerShape(6.dp))
                                .testTag("audio_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (gameState.isAudioEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = "Audio Toggle",
                                tint = if (gameState.isAudioEnabled) Color(0xFFFFD600) else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (gameState.gameMode == GameMode.VS_CPU) {
                            IconButton(
                                onClick = { viewModel.startNewMatch() },
                                modifier = Modifier
                                    .size(36.dp).background(Color(0xFF1E1F25), RoundedCornerShape(6.dp))
                                    .testTag("restart_match_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reiniciar",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    wasPausedBeforeAbandonDialog = true
                                    if (gameState.matchStatus == MatchStatus.PLAYING) viewModel.togglePause()
                                    showPauseMenu = true
                                },
                                modifier = Modifier
                                    .size(36.dp).background(Color(0xFF1E1F25), RoundedCornerShape(6.dp))
                                    .testTag("pause_button")
                            ) {
                                Icon(
                                    imageVector = if (gameState.matchStatus == MatchStatus.PAUSED)
                                        Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = "Pausa",
                                    tint = Color(0xFFFF4081),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1A1B21))
                        .border(1.dp, Color(0xFF3B494B).copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("${gameState.player.score} BW", color = Color(0xFF76EBF5), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("COMBO ${gameState.player.combo}x", color = Color(0xFFECB2FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("LINHAS ${gameState.player.linesCleared}", color = Color(0xFF00E676), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("ATAQUE ${gameState.player.pendingGarbage}", color = Color(0xFFFFB59C), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(2.dp))

                // ==========================================
                // 2. CENTER DUAL BATTLE ARENA (LEFT PLAYER | RIGHT OPPONENT)
                // ==========================================
                BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) arena@{
                    // Keep the 10x20 board square cells within the available height.
                    val arenaWidth = minOf(this@arena.maxWidth,
                        ((this@arena.maxHeight - 32.dp).coerceAtLeast(0.dp) / 1.32f) + 8.dp)
                    Row(
                        modifier = Modifier.width(arenaWidth).align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // LEFT SIDE: Main Player Screen (10x20)
                        Column(
                            modifier = Modifier.weight(0.66f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Top
                        ) {
                            // Player Header Info
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(28.dp).padding(bottom = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val equippedAvatar = gameState.inventory.equippedItems["avatares"] ?: "avatar_default"
                                    val equippedMoldura = gameState.inventory.equippedItems["molduras"] ?: "moldura_simple"
                                    AvatarWithFrame(
                                        avatarId = equippedAvatar,
                                        molduraId = equippedMoldura,
                                        size = 24.dp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "VOCÊ",
                                        color = Color(0xFF00E5FF),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            // Main 10x20 Board with Particle Overlay and Screen Shake
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxWidth().aspectRatio(10f / 20f)
                            ) {
                                BoardCanvas(
                                    playerState = gameState.player,
                                    particleController = particleController,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .offset { IntOffset(shakeOffsetX.value.roundToInt(), shakeOffsetY.value.roundToInt()) },
                                    skinId = equippedSkin,
                                    vfxId = equippedVfx,
                                    animId = equippedAnim,
                                    boardThemeId = gameState.inventory.equippedItems["tabuleiros"] ?: "tab_neon"
                                )
                                ParticleOverlay(controller = particleController)

                                // High-impact combo indicator
                                if (gameState.player.combo > 1) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black.copy(alpha = 0.8f))
                                            .border(2.dp, Color(0xFFFF4081), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                            .testTag("combo_visual_overlay"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "COMBO",
                                                color = Color(0xFF00E5FF),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace,
                                                letterSpacing = 2.sp
                                            )
                                            Text(
                                                text = "x${gameState.player.combo}",
                                                color = Color(0xFFECB2FF),
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace,
                                                letterSpacing = 1.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // RIGHT SIDE: Opponent Mini Screen (Top Right) + Next Piece Box
                        Column(
                            modifier = Modifier.weight(0.34f).padding(top = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Preview Card (Next Only)
                            PiecePreviewBox(
                                title = "PRÓXIMA",
                                pieceType = gameState.player.nextPiece,
                                tag = "next_piece_box",
                                modifier = Modifier.fillMaxWidth(),
                                skinId = equippedSkin
                            )

                            // TOP RIGHT: Mirrored Mini Opponent Board (10x20)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                MiniBoardCanvas(
                                    opponentState = gameState.opponent,
                                    modifier = Modifier.fillMaxWidth(),
                                    skinId = equippedSkin,
                                    boardThemeId = gameState.inventory.equippedItems["tabuleiros"] ?: "tab_neon"
                                )

                                // Opponent Combo Indicator
                                if (gameState.opponent.combo > 1) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.Black.copy(alpha = 0.8f))
                                            .border(1.5.dp, Color(0xFFFF4081), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "COMBO",
                                                color = Color(0xFFB388FF),
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = "x${gameState.opponent.combo}",
                                                color = Color(0xFFECB2FF),
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }


                        }
                    }

                }

                // ==========================================
                // 3. CONTROLS AREA (VIRTUAL BUTTONS OR DEDICATED TOUCHPAD)
                // ==========================================
                if (gameState.controlSettings.controlMode == ControlMode.BUTTONS) {
                    VirtualControls(
                        onLeft = { viewModel.moveLeft() },
                        onRight = { viewModel.moveRight() },
                        onRotate = { viewModel.rotate() },
                        onHardDrop = { viewModel.hardDrop() },
                        onSoftDrop = { viewModel.softDrop() },
                        controlSettings = gameState.controlSettings
                    )
                } else {
                    TouchpadControlArea(
                        viewModel = viewModel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(290.dp)
                    )
                }
            }

            // Floating attack banner when garbage lines are exchanged
            AttackBanner(
                attackEvents = gameState.attackEvents,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 90.dp)
            )

            // Game over / Victory popup with Detailed Statistics
            if (!showAbandonConfirmDialog && gameState.gameMode == GameMode.ONLINE_1V1 &&
                (gameState.onlineSessionCancelled || gameState.matchStatus == MatchStatus.PAUSED) &&
                gameState.onlineConnectionMessage.isNotBlank()) {
                androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
                    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF10131C), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(16.dp)).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(if (gameState.onlineSessionCancelled) "PARTIDA ENCERRADA" else "CONEXÃO INTERROMPIDA",
                            color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                        Text(gameState.onlineConnectionMessage, color = Color.White)
                        androidx.compose.material3.TextButton(onClick = {
                            if (gameState.onlineSessionCancelled) viewModel.navigateToMenu()
                            else {
                                wasPausedBeforeAbandonDialog = true
                                showPauseMenu = false
                                showAbandonConfirmDialog = true
                            }
                        }) {
                            Text(if (gameState.onlineSessionCancelled) "MENU PRINCIPAL" else "ABANDONAR PARTIDA",
                                color = Color(0xFFFF4081))
                        }
                    }
                }
            }
            GameOverDialog(
                gameState = gameState,
                matchDurationSeconds = gameState.matchDurationSeconds,
                totalAttacksSent = gameState.totalAttacksSent,
                onRestart = { viewModel.startNewMatch() },
                onQuickMatch = { viewModel.startQuickMatch() },
                onOpenHistory = { showHistoryDialog = true },
                onMenu = { viewModel.navigateToMenu() }
            )

            // Performance History Dialog
            if (showHistoryDialog) {
                MatchHistoryDialog(
                    stats = viewModel.getAggregatedStats(),
                    history = viewModel.getMatchHistory(),
                    onClearHistory = { viewModel.clearMatchHistory() },
                    onDismiss = { showHistoryDialog = false }
                )
            }

            // One pause menu shared by the toolbar home, pause and system back actions.
            if (!showAbandonConfirmDialog &&
                (gameState.gameMode == GameMode.VS_CPU || gameState.onlineConnectionMessage.isBlank()) &&
                (showPauseMenu || (gameState.matchStatus == MatchStatus.PAUSED && gameState.gameMode == GameMode.VS_CPU))) {
                val resumeMatch: () -> Unit = {
                    showPauseMenu = false
                    if (gameState.gameMode == GameMode.VS_CPU && gameState.matchStatus == MatchStatus.PAUSED) {
                        viewModel.togglePause()
                    }
                }
                Dialog(
                    onDismissRequest = resumeMatch,
                    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = true)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.78f))
                            .padding(24.dp)
                            .testTag("pause_overlay"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFF0D0E16))
                                .border(2.dp, Color(0xFFFF4081), RoundedCornerShape(24.dp))
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = null,
                                tint = Color(0xFFFF4081), modifier = Modifier.size(64.dp))
                            Text(
                                text = if (gameState.gameMode == GameMode.VS_CPU) "JOGO PAUSADO" else "PARTIDA ONLINE",
                                color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center
                            )
                            if (gameState.gameMode == GameMode.ONLINE_1V1) {
                                Text("A partida online continua enquanto este menu está aberto.",
                                    color = Color(0xFFB9CACB), fontSize = 12.sp, textAlign = TextAlign.Center)
                            }
                            Text("TOQUE PARA RETOMAR", color = Color(0xFF00E5FF), fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth().clickable(onClick = resumeMatch)
                                    .padding(vertical = 12.dp).testTag("resume_match_button"),
                                textAlign = TextAlign.Center)
                            Button(
                                onClick = {
                                    wasPausedBeforeAbandonDialog = true
                                    showPauseMenu = true
                                    showAbandonConfirmDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF20172D)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                                    .border(1.dp, Color(0xFFFF4081), RoundedCornerShape(12.dp))
                                    .testTag("pause_lobby_button")
                            ) {
                                Text("VOLTAR PARA LOBBY", color = Color(0xFFFF70AF), fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
            // Abandon Match Confirmation Dialog
            if (showAbandonConfirmDialog) {
                Dialog(
                    onDismissRequest = {
                        showAbandonConfirmDialog = false
                        if (gameState.gameMode == GameMode.VS_CPU && !wasPausedBeforeAbandonDialog && gameState.matchStatus == MatchStatus.PAUSED) {
                            viewModel.togglePause()
                        }
                    },
                    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = true)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.8f))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF101322))
                                .border(2.dp, Color(0xFFFF1744), RoundedCornerShape(16.dp))
                                .padding(20.dp)
                                .testTag("abandon_confirm_dialog")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFF1744),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "ABANDONAR PARTIDA",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (gameState.gameMode == GameMode.ONLINE_1V1)
                                    "Abandonar registra uma derrota, desconta 10 BW e não concede Blockoins."
                                else
                                    "Abandonar registra uma derrota, sem alterar seus BW e sem conceder Blockoins.",
                                color = Color(0xFFB0B7D0),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        showAbandonConfirmDialog = false
                                        if (gameState.gameMode == GameMode.VS_CPU && !wasPausedBeforeAbandonDialog && gameState.matchStatus == MatchStatus.PAUSED) {
                                            viewModel.togglePause()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF242944)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("continue_match_button")
                                ) {
                                    Text(
                                        text = "CONTINUAR",
                                        color = Color(0xFF00E5FF),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Button(
                                    onClick = {
                                        showAbandonConfirmDialog = false
                                        showPauseMenu = false
                                        viewModel.navigateToMenu()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("confirm_abandon_button")
                                ) {
                                    Text(
                                        text = "ABANDONAR",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TouchpadControlArea(
    viewModel: BlockWarViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .testTag("touchpad_control_area")
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F1326).copy(alpha = 0.85f),
                        Color(0xFF070914).copy(alpha = 0.95f)
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF00E5FF).copy(alpha = 0.6f), Color(0xFF7C4DFF).copy(alpha = 0.3f))
                ),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startTime = System.currentTimeMillis()
                    var lastTime = startTime
                    var totalDragX = 0f
                    var totalDragY = 0f
                    var accumulatedSoftDropY = 0f
                    var hasSwipedOrDragged = false
                    var isLongPressing = false
                    var isHardDropped = false
                    var longPressJob: Job? = null

                    longPressJob = coroutineScope.launch {
                        delay(200L)
                        if (!hasSwipedOrDragged && !isHardDropped) {
                            isLongPressing = true
                            while (isActive && isLongPressing) {
                                viewModel.softDrop()
                                delay(60L)
                            }
                        }
                    }

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        val sens = viewModel.gameState.value.controlSettings.gestureSensitivity.coerceIn(0.5f, 2.0f)
                        val dragThreshold = 15f / sens
                        val softDropStepPx = 20f / sens
                        val horizStepPx = 20f / sens
                        val flickDistShort = 35f / sens
                        val flickDistFast = 80f / sens

                        if (!change.pressed) {
                            change.consume()
                            longPressJob?.cancel()
                            val duration = System.currentTimeMillis() - startTime
                            val absTotalX = kotlin.math.abs(totalDragX)
                            val absTotalY = kotlin.math.abs(totalDragY)
                            if (!hasSwipedOrDragged && !isLongPressing && !isHardDropped && duration < 200L && absTotalX < dragThreshold && absTotalY < dragThreshold) {
                                viewModel.rotate()
                            }
                            break
                        } else {
                            val dragAmount = change.positionChange()
                            val currentTime = System.currentTimeMillis()
                            val dt = (currentTime - lastTime).coerceAtLeast(1L)
                            lastTime = currentTime

                            totalDragX += dragAmount.x
                            totalDragY += dragAmount.y

                            val absX = kotlin.math.abs(totalDragX)
                            val absY = kotlin.math.abs(totalDragY)

                            if (absX > dragThreshold || absY > dragThreshold) {
                                if (!hasSwipedOrDragged) {
                                    hasSwipedOrDragged = true
                                    longPressJob?.cancel()
                                    isLongPressing = false
                                }
                            }

                            if (hasSwipedOrDragged && !isHardDropped) {
                                val instantSpeedY = dragAmount.y / dt.toFloat()

                                // 1. Fast Flick / Rapid Swipe Down -> Hard Drop
                                val isFastFlickDown = ((instantSpeedY * sens) > 1.0f && totalDragY > flickDistShort && totalDragY > absX * 1.1f) ||
                                        (totalDragY > flickDistFast && (currentTime - startTime) < 250L && totalDragY > absX * 1.2f)

                                if (isFastFlickDown) {
                                    isHardDropped = true
                                    change.consume()
                                    viewModel.hardDrop()
                                    break
                                }

                                // 2. Smooth / Continuous Downward Drag -> Soft Drop
                                if (totalDragY > softDropStepPx && totalDragY > absX * 1.0f && dragAmount.y > 0f) {
                                    accumulatedSoftDropY += dragAmount.y
                                    if (accumulatedSoftDropY >= softDropStepPx) {
                                        val steps = (accumulatedSoftDropY / softDropStepPx).toInt()
                                        repeat(steps) { viewModel.softDrop() }
                                        accumulatedSoftDropY -= steps * softDropStepPx
                                        change.consume()
                                    }
                                }

                                // 3. Horizontal Drag -> Move Left / Move Right
                                if (absX > horizStepPx && absX > absY * 0.9f) {
                                    change.consume()
                                    if (totalDragX > horizStepPx) {
                                        viewModel.moveRight()
                                        totalDragX = 0f
                                    } else if (totalDragX < -horizStepPx) {
                                        viewModel.moveLeft()
                                        totalDragX = 0f
                                    }
                                }
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF).copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ÁREA DE CONTROLE TÁTIL",
                    color = Color(0xFF00E5FF).copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "TAP: GIRAR   •   ARRASTAR: MOVER ◄ ►\nDESLIZAR SUAVE ↓: QUEDA SUAVE   •   FLICK ↓: QUEDA INSTANTÂNEA",
                color = Color.LightGray.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}
