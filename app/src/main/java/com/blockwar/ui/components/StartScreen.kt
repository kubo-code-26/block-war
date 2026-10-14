package com.blockwar.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.blockwar.model.CpuDifficulty
import com.blockwar.model.VersusGameState
import com.blockwar.model.ShopItem
import com.blockwar.model.Mission

@Composable
fun StartScreen(
    gameState: VersusGameState,
    onStartVsCpu: (CpuDifficulty) -> Unit,
    onOpenOnlineLobby: () -> Unit,
    onStartQuickMatch: () -> Unit,
    onCancelQuickMatch: () -> Unit,
    onCreateOnlineRoom: () -> Unit,
    onJoinOnlineRoom: (String) -> Unit,
    onCloseOnlineLobby: () -> Unit,
    onOpenControlsEditor: () -> Unit,
    onOpenHistory: () -> Unit,
    onToggleAudio: () -> Unit,
    getPlayerName: () -> String,
    getPlayerId: () -> String,
    hasSavedNickname: () -> Boolean,
    onSavePlayerName: (String, String) -> Unit,
    onFetchLeaderboard: (onSuccess: (List<com.blockwar.network.LeaderboardEntry>) -> Unit, onError: (String) -> Unit) -> Unit,
    onClearNicknameError: () -> Unit,
    getLocalPin: () -> String,
    onSaveLocalPin: (String) -> Unit,
    onCheckNicknameExists: (String, (Boolean) -> Unit) -> Unit,
    getPatentInfo: (Int) -> Pair<String, Color>,
    onBuyItem: (ShopItem) -> Unit,
    onEquipItem: (ShopItem) -> Unit,
    onClaimMission: (String) -> Unit,
    onAddCoins: (Int) -> Unit = {},
    getAggregatedStats: () -> com.blockwar.model.AggregatedStats = { com.blockwar.model.AggregatedStats() },
    getMatchHistory: () -> List<com.blockwar.model.MatchRecord> = { emptyList() },
    onSaveControlSettings: (com.blockwar.model.ControlSettings) -> Unit = {},
    modifier: Modifier = Modifier,
    onLogout: () -> Unit = {},
    onGooglePlayGamesSignIn: () -> Unit = {}
) {
    var showRulesDialog by remember { mutableStateOf(false) }
    var showSecureStartError by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(gameState.secureStartErrorMessage) {
        showSecureStartError=gameState.secureStartErrorMessage.isNotBlank()
    }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showCpuDifficultyDialog by remember { mutableStateOf(false) }
    var selectedDifficulty by remember { mutableStateOf(CpuDifficulty.MEDIUM) }
    var showLeaderboardDialog by remember { mutableStateOf(false) }
    var showShopDialog by remember { mutableStateOf(false) }
    var showMissionsDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var leaderboardList by remember { mutableStateOf<List<com.blockwar.network.LeaderboardEntry>>(emptyList()) }
    var leaderboardLoading by remember { mutableStateOf(false) }
    var leaderboardError by remember { mutableStateOf("") }
    var currentNickname by remember { mutableStateOf(getPlayerName()) }
    val hasSession = hasSavedNickname()
    var showFirstTimeNicknameDialog by remember(hasSession) { mutableStateOf(!hasSession) }
    var firstTimeInputName by remember { mutableStateOf("") }
    var firstTimeInputPin by remember { mutableStateOf("") }
    var inputRoomCode by remember { mutableStateOf("") }
    var lobbyTabMode by remember { mutableStateOf("menu") } // "menu", "join"

    var activeAuthTab by remember { mutableStateOf("signup") } // "signup", "login"
    var localErrorMessage by remember { mutableStateOf("") }
    var isValidating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    androidx.compose.runtime.LaunchedEffect(gameState.nicknameErrorMessage, gameState.nickname) {
        if (gameState.nicknameErrorMessage.isNotEmpty()) {
            isValidating = false
            localErrorMessage = gameState.nicknameErrorMessage
        } else if (gameState.nickname.isNotEmpty()) {
            isValidating = false
            localErrorMessage = ""
        }
    }

    androidx.compose.runtime.LaunchedEffect(gameState.wins, gameState.nickname) {
        val currentName = gameState.nickname
        if (currentName.isNotEmpty()) {
            if (showFirstTimeNicknameDialog) {
                showFirstTimeNicknameDialog = false
                showProfileDialog = true
                leaderboardLoading = true
                onFetchLeaderboard(
                    { list ->
                        leaderboardList = list
                        leaderboardLoading = false
                    },
                    { error ->
                        leaderboardLoading = false
                    }
                )
            }
            currentNickname = currentName
        }
    }

    androidx.compose.runtime.LaunchedEffect(showLeaderboardDialog) {
        if (showLeaderboardDialog) {
            leaderboardLoading = true
            leaderboardError = ""
            onFetchLeaderboard(
                { list ->
                    leaderboardList = list
                    leaderboardLoading = false
                },
                { error ->
                    leaderboardError = error
                    leaderboardLoading = false
                }
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val bannerPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bannerPhase"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF08090D))
            .testTag("start_menu_screen")
    ) {
        // Cyberpunk ambient radial glows, cybernetic grid and scanlines background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Top-left Cyan Glow
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x2200F0FF), Color.Transparent),
                    center = Offset(w * 0.2f, h * 0.2f),
                    radius = w * 0.8f
                )
            )

            // Bottom-right Purple Radial Glow
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x22D500F9), Color.Transparent),
                    center = Offset(w * 0.8f, h * 0.8f),
                    radius = w * 0.8f
                )
            )

            // Cybernetic grid lines (#00F0FF at 5% opacity)
            val gridStep = 36.dp.toPx()
            val gridColor = Color(0xFF00F0FF).copy(alpha = 0.05f)

            var x = 0f
            while (x < w) {
                drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
                x += gridStep
            }
            var y = 0f
            while (y < h) {
                drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                y += gridStep
            }

            // Horizontal scanlines effect
            val scanlineStep = 4.dp.toPx()
            val scanlineColor = Color(0x0F000000)
            var sy = 0f
            while (sy < h) {
                drawLine(scanlineColor, Offset(0f, sy), Offset(w, sy), strokeWidth = 1f)
                sy += scanlineStep
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .appSafeDrawingPadding()
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // New Top Header: Profile Info & Wallet
            val (patentName, patentColor) = getPatentInfo(gameState.pontos_bw)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Square avatar (14dp rounded corner) with cyan-blue-pink gradient
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        showMissionsDialog = false
                        showShopDialog = false
                        showSettingsDialog = false
                        showLeaderboardDialog = false
                        showRulesDialog = false
                        showProfileDialog = true
                    }
                ) {
                    val equippedAvatar = gameState.inventory.equippedItems["avatares"] ?: "avatar_default"
                    val equippedMoldura = gameState.inventory.equippedItems["molduras"] ?: "moldura_simple"
                    AvatarWithFrame(
                        avatarId = equippedAvatar,
                        molduraId = equippedMoldura,
                        size = 44.dp
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentNickname,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Patent badge with dark purple background (#3A2046)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF3A2046))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = patentName.uppercase(),
                                    color = patentColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Text(
                            text = "${gameState.pontos_bw} PONTOS BW",
                            color = Color(0xFFA0A5C0),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Right: Dark coin pill (#1B1C24)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Dark Coin Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF1B1C24))
                            .border(1.dp, Color(0xFF2D3045), RoundedCornerShape(20.dp))
                            .clickable {
                                showMissionsDialog = false
                                showSettingsDialog = false
                                showProfileDialog = false
                                showLeaderboardDialog = false
                                showRulesDialog = false
                                showShopDialog = true
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BlockoinIcon(28.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = String.format(java.util.Locale("pt", "BR"), "%,d", gameState.inventory.coins),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Center Section: Logo & Holographic Board Container
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp)
            ) {
                // Título: "BLOCK" em ciano neon e "WAR" dentro do quadro vermelho
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "BLOCK",
                        style = TextStyle(
                            color = Color(0xFF00F0FF),
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 6.sp,
                            shadow = Shadow(
                                color = Color(0xFF00F0FF).copy(alpha = 0.8f),
                                offset = Offset.Zero,
                                blurRadius = 28f
                            )
                        ),
                        maxLines = 1,
                        softWrap = false
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFFFF2D55), Color(0xFFD5002F))
                                )
                            )
                            .border(1.dp, Color(0xFFFF6B88), RoundedCornerShape(8.dp))
                            .padding(horizontal = 30.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "WAR",
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 44.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 6.sp,
                                shadow = Shadow(
                                    color = Color.White.copy(alpha = 0.6f),
                                    offset = Offset.Zero,
                                    blurRadius = 16f
                                )
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))

                // Glassmorphic Holographic Card (#12141D) with responsive banner animation
                val equippedBanner = gameState.inventory.equippedItems["banners"] ?: "banner_bot"
                val cardBorderColor = when (equippedBanner) {
                    "banner_overclock" -> Color(0xFFFF2E93)
                    "banner_matrix" -> Color(0xFF00FF66)
                    "banner_supernova" -> Color(0xFFBD00FF)
                    else -> Color(0xFF00F0FF)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .height(150.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xE60B0C11))
                        .border(1.dp, cardBorderColor.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedBanner(equippedBanner, Modifier.fillMaxSize())
                    // Textura de pontos (grade pontilhada) atrás do banner
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val step = 14.dp.toPx()
                        var px = step / 2f
                        while (px < size.width) {
                            var py = step / 2f
                            while (py < size.height) {
                                drawCircle(
                                    color = Color(0xFF00F0FF).copy(alpha = 0.10f),
                                    radius = 1.dp.toPx(),
                                    center = Offset(px, py)
                                )
                                py += step
                            }
                            px += step
                        }
                    }

                }

                Spacer(modifier = Modifier.height(24.dp))

                // Main Game Modes Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Botão 1: JOGAR VS CPU
                    HomeModeButton(
                        title = "JOGAR VS CPU",
                        subtitle = "DESAFIE O BOT LOCAL (${selectedDifficulty.displayName})",
                        icon = Icons.Default.SportsEsports,
                        accent = Color(0xFF18E39A),
                        fillStart = Color(0xFF0A3A2B),
                        fillEnd = Color(0xFF0C5A3C),
                        testTag = "vs_cpu_button",
                        onClick = { showCpuDifficultyDialog = true }
                    )

                    // Botão 2: JOGAR ONLINE 1V1
                    HomeModeButton(
                        title = "JOGAR ONLINE 1V1",
                        subtitle = "LOBBY MULTIPLAYER EM TEMPO REAL",
                        icon = Icons.Default.Public,
                        accent = Color(0xFFB04CFF),
                        fillStart = Color(0xFF2B0F57),
                        fillEnd = Color(0xFF4A1470),
                        testTag = "online_1v1_button",
                        onClick = {
                            lobbyTabMode = "menu"
                            onOpenOnlineLobby()
                        }
                    )
                }
            }

            // Barra de navegação inferior (visual Stitch)
            HomeBottomNav(
                onOpenTasks = {
                    showShopDialog = false
                    showSettingsDialog = false
                    showProfileDialog = false
                    showLeaderboardDialog = false
                    showRulesDialog = false
                    showMissionsDialog = true
                },
                onOpenShop = {
                    showMissionsDialog = false
                    showSettingsDialog = false
                    showProfileDialog = false
                    showLeaderboardDialog = false
                    showRulesDialog = false
                    showShopDialog = true
                },
                onOpenRanking = {
                    showMissionsDialog = false
                    showShopDialog = false
                    showSettingsDialog = false
                    showProfileDialog = false
                    showRulesDialog = false
                    showLeaderboardDialog = true
                },
                onOpenSettings = {
                    showMissionsDialog = false
                    showShopDialog = false
                    showProfileDialog = false
                    showLeaderboardDialog = false
                    showRulesDialog = false
                    showSettingsDialog = true
                },
                onOpenProfile = {
                    showMissionsDialog = false
                    showShopDialog = false
                    showSettingsDialog = false
                    showLeaderboardDialog = false
                    showRulesDialog = false
                    showProfileDialog = true
                }
            )
        }

        // Settings Dialog
        // Configurações (visual Stitch)
        if (showSettingsDialog) {
            SettingsModal(
                controlSettings = gameState.controlSettings,
                isAudioEnabled = gameState.isAudioEnabled,
                nickname = currentNickname,
                onToggleAudio = onToggleAudio,
                onOpenControlsEditor = {
                    showSettingsDialog = false
                    onOpenControlsEditor()
                },
                onOpenRules = {
                    showSettingsDialog = false
                    showRulesDialog = true
                },
                onClose = { showSettingsDialog = false },
                playGamesConnected = gameState.playGamesConnected,
                playGamesAccountName = gameState.playGamesAccountName,
                playGamesBusy = gameState.playGamesBusy,
                playGamesMessage = gameState.playGamesMessage,
                onConnectPlayGames = onGooglePlayGamesSignIn
            )
        }

        // Catálogo da loja (usado pela Loja e pelo Perfil)
        val shopCatalog = com.blockwar.economy.ShopCatalog.items

        // Shop Modal
        if (showShopDialog) {
            ShopModal(
                inventory = gameState.inventory,
                items = shopCatalog,
                onBuyItem = onBuyItem,
                onEquipItem = onEquipItem,
                onAddCoins = onAddCoins,
                onClose = { showShopDialog = false },
                operationMessage = gameState.economyMessage
            )
        }

        // Missions Modal
        if (showMissionsDialog) {
            MissionsModal(
                missions = gameState.dailyMissions,
                onClaimReward = { onClaimMission(it.id) },
                onClose = { showMissionsDialog = false },
                operationMessage = gameState.missionSaveMessage
            )
        }

        // CPU Difficulty Selection Dialog
        if (showCpuDifficultyDialog) {
            Dialog(
                onDismissRequest = { showCpuDifficultyDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = true)
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 440.dp).fillMaxWidth(0.92f).verticalScroll(rememberScrollState())
                        .border(1.dp, Color(0xFF176171), RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF10131C))
                        .padding(20.dp)
                        .testTag("cpu_difficulty_dialog")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Memory, contentDescription = null,
                                tint = Color(0xFF00E676), modifier = Modifier.size(22.dp))
                            Text("DIFICULDADE DA CPU", color = Color(0xFF00E676),
                                fontSize = 17.sp, fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace)
                        }
                        Text("ESCOLHA A DIFICULDADE", color = Color(0xFF94A3B8),
                            fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CpuDifficulty.entries.forEach { diff ->
                                val isSelected = selectedDifficulty == diff
                                val borderColor = when (diff) {
                                    CpuDifficulty.EASY -> Color(0xFF00E676)
                                    CpuDifficulty.MEDIUM -> Color(0xFF00E5FF)
                                    CpuDifficulty.HARD -> Color(0xFFFF1744)
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) Color(0xFF00F0FF) else Color(0xFF283244),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSelected) Color(0xFF102833) else Color(0xFF151722))
                                        .clickable { selectedDifficulty = diff }
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier.size(42.dp)
                                                .background(borderColor.copy(alpha = if (isSelected) 0.15f else 0.07f), RoundedCornerShape(12.dp))
                                                .border(1.dp, Color(0xFF2D394D), RoundedCornerShape(12.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = when (diff) {
                                                    CpuDifficulty.EASY -> Icons.Default.SportsEsports
                                                    CpuDifficulty.MEDIUM -> Icons.Default.Bolt
                                                    CpuDifficulty.HARD -> Icons.Default.LocalFireDepartment
                                                },
                                                contentDescription = null, tint = borderColor,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = diff.displayName,
                                                color = if (isSelected) Color(0xFF00F0FF) else Color(0xFF94A3B8),
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = diff.description,
                                                color = Color(0xFFB0BEC5),
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        Box(
                                            modifier = Modifier.size(20.dp)
                                                .border(if (isSelected) 2.dp else 1.dp,
                                                    if (isSelected) Color(0xFF00F0FF) else Color(0xFF536174), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) Box(Modifier.size(10.dp).background(Color(0xFF00F0FF), CircleShape))
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .background(Color(0xFF111923), RoundedCornerShape(12.dp)).border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                                    .clickable { showCpuDifficultyDialog = false }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "CANCELAR",
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(48.dp)
                                    .background(Color(0xFF00E676), RoundedCornerShape(8.dp))
                                    .clickable {
                                        showCpuDifficultyDialog = false
                                        onStartVsCpu(selectedDifficulty)
                                    }
                                    .testTag("start_vs_cpu_confirm_button")
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "INICIAR VS CPU",
                                    color = Color.Black,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        // Online Lobby Dialog
        if (gameState.isSearchingOnlineOpponent) {
            Dialog(
                onDismissRequest = onCloseOnlineLobby,
                properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = true)
            ) {
                val screenHeight = LocalConfiguration.current.screenHeightDp.dp
                val maxModalHeight = minOf(screenHeight * 0.85f, screenHeight - 60.dp)

                Box(
                    modifier = Modifier
                        .widthIn(max = 440.dp).fillMaxWidth(0.92f)
                        .heightIn(max = maxModalHeight)
                        .imePadding()
                        .border(1.dp, Color(0xFF176171), RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF10131C))
                        .padding(20.dp)
                        .testTag("online_lobby_dialog")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = "Online",
                                tint = Color(0xFF00F0FF),
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "LOBBY MULTIPLAYER 1V1",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        if (gameState.onlineRoomStatus == "buscando") {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "⚡ JOGO RÁPIDO / MATCHMAKING",
                                    color = Color(0xFFFFD600),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF00E5FF).copy(alpha = pulseAlpha))
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Buscando oponente...",
                                        color = Color(0xFF00E5FF),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                if (gameState.onlineRoomCode.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF1E2442), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 16.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "SALA: ${gameState.onlineRoomCode}",
                                            color = Color.LightGray,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                Text(
                                    text = "Conectando automaticamente à primeira sala livre encontrada no Firebase...",
                                    color = Color(0xFF9E9E9E),
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center,
                                    fontFamily = FontFamily.Monospace
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .border(2.dp, Color(0xFFFF1744), RoundedCornerShape(8.dp))
                                        .background(Color(0xFF3E101B), RoundedCornerShape(8.dp))
                                        .clickable { onCancelQuickMatch() }
                                        .testTag("cancel_quick_match_button")
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "CANCELAR BUSCA",
                                        color = Color(0xFFFF5252),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        } else if (gameState.onlineRoomStatus == "aguardando_jogadores") {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "SALA CRIADA COM SUCESSO!",
                                    color = Color(0xFF00E676),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )

                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF1E2442), RoundedCornerShape(8.dp))
                                        .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 24.dp, vertical = 12.dp)
                                ) {
                                    Text(
                                        text = gameState.onlineRoomCode,
                                        color = Color(0xFFFFD600),
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 4.sp
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF00E5FF).copy(alpha = pulseAlpha))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "AGUARDANDO OPONENTE (JOGADOR 2)...",
                                        color = Color(0xFF00E5FF),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                Text(
                                    text = "Informe este código de 4 caracteres ao outro jogador para ele entrar.",
                                    color = Color(0xFF9E9E9E),
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center,
                                    fontFamily = FontFamily.Monospace
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                                        .clickable { onCloseOnlineLobby() }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "CANCELAR E VOLTAR",
                                        color = Color.LightGray,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        } else if (lobbyTabMode == "join" || gameState.onlineRoomStatus == "entrando") {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "DIGITE O CÓDIGO DA SALA (4 CARACTERES):",
                                    color = Color(0xFF00E5FF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center
                                )

                                OutlinedTextField(
                                    value = inputRoomCode,
                                    onValueChange = { if (it.length <= 4) inputRoomCode = it.uppercase() },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF00E5FF),
                                        unfocusedBorderColor = Color(0xFF00F0FF),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = {
                                        if (inputRoomCode.length == 4) {
                                            onJoinOnlineRoom(inputRoomCode)
                                        }
                                    }),
                                    placeholder = {
                                        Text(
                                            "EX: A8F3",
                                            color = Color.Gray,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("room_code_input")
                                )

                                if (gameState.onlineErrorMessage.isNotEmpty()) {
                                    Text(
                                        text = gameState.onlineErrorMessage,
                                        color = Color(0xFFFF1744),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                                            .clickable { lobbyTabMode = "menu" }
                                            .padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "VOLTAR",
                                            color = Color.LightGray,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1.5f)
                                            .height(48.dp)
                                            .background(
                                                if (inputRoomCode.length == 4) Color(0xFF00E676) else Color.DarkGray,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable(enabled = inputRoomCode.length == 4) {
                                                onJoinOnlineRoom(inputRoomCode)
                                            }
                                            .testTag("confirm_join_room_button")
                                            .padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "ENTRAR NA SALA",
                                            color = if (inputRoomCode.length == 4) Color.Black else Color.Gray,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "ESCOLHA COMO DESEJA CONECTAR:",
                                    color = Color(0xFFB0BEC5),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(72.dp)
                                        .border(1.5.dp, Color(0xFFFFD600), RoundedCornerShape(14.dp))
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF211D16))
                                        .clickable { onStartQuickMatch() }
                                        .testTag("quick_match_button")
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier.size(42.dp)
                                                .background(Color(0xFFFFD600).copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                                .border(1.dp, Color(0xFFFFD600).copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Bolt, contentDescription = "Jogo Rápido",
                                                tint = Color(0xFFFFD600), modifier = Modifier.size(24.dp))
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "JOGO RÁPIDO / ALEATÓRIO",
                                                color = Color(0xFFFFD600),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = "BUSCA AUTOMÁTICA DE SALA ABERTA",
                                                color = Color(0xFFFFE082),
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(72.dp)
                                        .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(14.dp))
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF132338))
                                        .clickable { onCreateOnlineRoom() }
                                        .testTag("create_room_button")
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier.size(42.dp)
                                                .background(Color(0xFF00E5FF).copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.AddCircleOutline, contentDescription = "Criar Sala",
                                                tint = Color(0xFF00E5FF), modifier = Modifier.size(24.dp))
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "CRIAR SALA PRIVADA",
                                                color = Color(0xFF00E5FF),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = "GERAR CÓDIGO E CONVIDAR AMIGO",
                                                color = Color(0xFF80DEEA),
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(72.dp)
                                        .border(1.5.dp, Color(0xFF00F0FF), RoundedCornerShape(14.dp))
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF22192F))
                                        .clickable {
                                            inputRoomCode = ""
                                            lobbyTabMode = "join"
                                        }
                                        .testTag("join_room_button")
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier.size(42.dp)
                                                .background(Color(0xFFB388FF).copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                                .border(1.dp, Color(0xFFB388FF).copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Login, contentDescription = "Entrar em Sala",
                                                tint = Color(0xFFB388FF), modifier = Modifier.size(24.dp))
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "ENTRAR EM SALA",
                                                color = Color(0xFFB388FF),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = "DIGITAR CÓDIGO DA SALA DE UM AMIGO",
                                                color = Color(0xFFD1C4E9),
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                                        .clickable { onCloseOnlineLobby() }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "CANCELAR",
                                        color = Color.LightGray,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Rules / Instructions Dialog
        if (showRulesDialog) {
            Dialog(
                onDismissRequest = { showRulesDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = true)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .border(2.dp, Color(0xFF00E5FF), RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0F1224))
                        .padding(20.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "REGRAS DO JOGO",
                            color = Color(0xFF00E5FF),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )

                        Text(
                            text = "1. PEÇAS PERMITIDAS:\n" +
                                    "Apenas formas retas horizontais: 1x1, 1x2, 1x3 e 1x4 sorteadas 100% aleatoriamente.\n\n" +
                                    "2. SISTEMA DE COMBOS:\n" +
                                    "Destruir linhas seguidas aplica multiplicadores de ataque (Combo x2 -> 1.5x, Combo x3 -> 2.0x).\n\n" +
                                    "3. SISTEMA DE ATAQUE 75%:\n" +
                                    "Ao destruir linhas, 75% x multiplicador de combo são enviados como peças rígidas de 1x1 (65%) e 1x2 (35%) para o oponente.\n\n" +
                                    "4. MODO ONLINE 1V1:\n" +
                                    "Use JOGO RÁPIDO para busca automática ou crie/entre em salas com código de 4 caracteres via Firebase.",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .background(Color(0xFF00E5FF), RoundedCornerShape(8.dp))
                                .clickable { showRulesDialog = false }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "ENTENDI!",
                                color = Color.Black,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Leaderboard / Tabela de Líderes Dialog
        if (showLeaderboardDialog) {
            RankingModal(
                gameState = gameState,
                leaderboardList = leaderboardList,
                isLoading = leaderboardLoading,
                errorMessage = leaderboardError,
                currentNickname = currentNickname,
                getPatentInfo = getPatentInfo,
                onDismiss = { showLeaderboardDialog = false },
                onPlayAction = {
                    showLeaderboardDialog = false
                    onStartVsCpu(gameState.cpuDifficulty)
                }
            )
        }

        // First Time Nickname Entry Popup
        if (showFirstTimeNicknameDialog) {
            Dialog(
                onDismissRequest = { /* forces interaction to proceed */ },
                properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = true)
            ) {
                val screenHeight = LocalConfiguration.current.screenHeightDp.dp
                val maxModalHeight = minOf(screenHeight * 0.85f, screenHeight - 40.dp)

                val accentColor = if (activeAuthTab == "login") Color(0xFF00F0FF) else Color(0xFFBD00FF)
                val accentGradient = if (activeAuthTab == "login") listOf(Color(0xFF00F0FF), Color(0xFF00B8D4)) else listOf(Color(0xFFBD00FF), Color(0xFF7928CA))
                val accentBtnTextColor = if (activeAuthTab == "login") Color.Black else Color.White

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .widthIn(max = 420.dp)
                        .heightIn(max = maxModalHeight)
                        .imePadding()
                        .border(1.5.dp, accentColor, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0B0D17))
                        .padding(22.dp)
                        .testTag("first_time_nickname_dialog")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "¡BEM-VINDO AO BLOCK WAR!",
                            color = accentColor,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            style = TextStyle(
                                shadow = Shadow(
                                    color = accentColor.copy(alpha = 0.8f),
                                    blurRadius = 12f
                                )
                            )
                        )

                        PlayGamesButton(gameState, onGooglePlayGamesSignIn, "ENTRAR COM PLAY GAMES")

                        // Auth Tabs (CADASTRAR / ENTRAR)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF1C223D), RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF101424))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (activeAuthTab == "signup") Color(0xFFBD00FF) else Color.Transparent)
                                    .clickable {
                                        activeAuthTab = "signup"
                                        localErrorMessage = ""
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "CADASTRAR",
                                    color = if (activeAuthTab == "signup") Color.White else Color(0xFFA0AAB8),
                                    fontSize = 12.sp,
                                    fontWeight = if (activeAuthTab == "signup") FontWeight.Black else FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (activeAuthTab == "login") Color(0xFF00F0FF) else Color.Transparent)
                                    .clickable {
                                        activeAuthTab = "login"
                                        localErrorMessage = ""
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "ENTRAR",
                                    color = if (activeAuthTab == "login") Color.Black else Color(0xFFA0AAB8),
                                    fontSize = 12.sp,
                                    fontWeight = if (activeAuthTab == "login") FontWeight.Black else FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Text(
                            text = if (activeAuthTab == "login") "Insira seu Nickname e seu PIN de 4 dígitos" else "Crie seu Nickname e escolha um PIN de 4 dígitos",
                            color = Color(0xFFA0AAB8),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        // Input 1: Nickname
                        OutlinedTextField(
                            value = firstTimeInputName,
                            onValueChange = { newValue ->
                                firstTimeInputName = newValue.take(12) // max 12 characters
                                isValidating = false
                                localErrorMessage = ""
                            },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Nickname",
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            placeholder = {
                                Text(
                                    text = "Seu Nickname",
                                    color = Color(0xFF5A6578),
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = Color(0xFF1C223D),
                                focusedContainerColor = Color(0xFF101424),
                                unfocusedContainerColor = Color(0xFF0D101D)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("first_time_nickname_input")
                        )

                        // Input 2: PIN de 4 dígitos
                        OutlinedTextField(
                            value = firstTimeInputPin,
                            onValueChange = { newValue ->
                                if (newValue.length <= 4 && newValue.all { c -> c.isDigit() }) {
                                    firstTimeInputPin = newValue
                                    isValidating = false
                                    localErrorMessage = ""
                                }
                            },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "PIN",
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            placeholder = {
                                Text(
                                    text = "PIN de 4 dígitos",
                                    color = Color(0xFF5A6578),
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Done
                            ),
                            visualTransformation = PasswordVisualTransformation(),
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = Color(0xFF1C223D),
                                focusedContainerColor = Color(0xFF101424),
                                unfocusedContainerColor = Color(0xFF0D101D)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("first_time_pin_input")
                        )

                        if (localErrorMessage.isNotEmpty()) {
                            Text(
                                text = localErrorMessage,
                                color = Color(0xFFFF1744),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }

                        // Botão de Ação Principal
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isValidating) {
                                        Brush.horizontalGradient(listOf(Color(0xFF374151), Color(0xFF374151)))
                                    } else {
                                        Brush.horizontalGradient(accentGradient)
                                    }
                                )
                                .clickable(enabled = !isValidating) {
                                    val finalName = firstTimeInputName.trim().lowercase() // Normalized
                                    val finalPin = firstTimeInputPin.trim()
                                    if (finalName.isEmpty()) {
                                        localErrorMessage = "Por favor, digite um Nickname."
                                        return@clickable
                                    }
                                    if (finalPin.length != 4 || !finalPin.all { it.isDigit() }) {
                                        localErrorMessage = "O PIN de 4 dígitos é obrigatório."
                                        return@clickable
                                    }
                                    
                                    isValidating = true
                                    localErrorMessage = ""
                                    
                                    // Implementation of timeout
                                    val timeoutJob = scope.launch {
                                        kotlinx.coroutines.delay(4000)
                                        if (isValidating) {
                                            localErrorMessage = "Tiempo de espera agotado. Verifica tu conexión"
                                            isValidating = false
                                        }
                                    }

                                    try {
                                        onCheckNicknameExists(finalName) { exists ->
                                            timeoutJob.cancel()
                                            if (!isValidating) return@onCheckNicknameExists

                                            if (activeAuthTab == "signup") {
                                                if (exists) {
                                                    isValidating = false
                                                    localErrorMessage = "Este Nickname já está registrado. Faça login ou escolha outro nome."
                                                } else {
                                                    onSavePlayerName(finalName, finalPin)
                                                    currentNickname = finalName
                                                    isValidating = false
                                                }
                                            } else {
                                                // login
                                                if (!exists) {
                                                    isValidating = false
                                                    localErrorMessage = "El usuario no existe. Regístrate primero."
                                                } else {
                                                    onSavePlayerName(finalName, finalPin)
                                                    currentNickname = finalName
                                                    // We'll reset isValidating via LaunchedEffect when nicknameErrorMessage or Name changes
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        timeoutJob.cancel()
                                        localErrorMessage = "Erro inesperado"
                                        isValidating = false
                                    }
                                }
                                .testTag("first_time_nickname_button")
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isValidating) "VERIFICANDO..." else if (activeAuthTab == "signup") "CRIAR CONTA" else "ENTRAR NA ARENA",
                                color = if (isValidating) Color.LightGray else accentBtnTextColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Error Dialog when nicknameErrorMessage is not empty
        if (gameState.nicknameErrorMessage.isNotEmpty()) {
            Dialog(
                onDismissRequest = {
                    isValidating = false
                    onClearNicknameError()
                },
                properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = true)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .border(3.dp, Color.Red, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E0A0A))
                        .padding(20.dp)
                        .testTag("nickname_error_dialog")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "ERRO DE AUTENTICAÇÃO",
                            color = Color.Red,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = gameState.nicknameErrorMessage,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .background(Color.Red, RoundedCornerShape(8.dp))
                                .clickable {
                                    isValidating = false
                                    onClearNicknameError()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "FECHAR",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Profile Dialog
        // Perfil do jogador (visual Stitch)
        if (showProfileDialog) {
            PlayerProfileModal(
                gameState = gameState,
                stats = getAggregatedStats(),
                matchHistory = getMatchHistory(),
                leaderboardList = leaderboardList,
                getPatentInfo = getPatentInfo,
                onLogout = {
                    onLogout()
                    showProfileDialog = false
                    currentNickname = ""
                    firstTimeInputName = ""
                    firstTimeInputPin = ""
                    activeAuthTab = "login"
                    showFirstTimeNicknameDialog = true
                },
                onClose = { showProfileDialog = false },
                onOpenHistory = {
                    showProfileDialog = false
                    onOpenHistory()
                },
                onOpenMissions = {
                    showProfileDialog = false
                    showMissionsDialog = true
                },
                onOpenShop = {
                    showProfileDialog = false
                    showShopDialog = true
                },
                onOpenRanking = {
                    showProfileDialog = false
                    showLeaderboardDialog = true
                },
                onOpenSettings = {
                    showProfileDialog = false
                    showSettingsDialog = true
                },
                onSavePlayerName = { name, pin ->
                    onSavePlayerName(name, pin)
                    currentNickname = name
                },
                onSaveLocalPin = onSaveLocalPin,
                onEquipItem = onEquipItem,
                getLocalPin = getLocalPin,
                shopItems = shopCatalog,
                onGooglePlayGamesSignIn = onGooglePlayGamesSignIn
            )
        }
    }
    if(showSecureStartError) {
        androidx.compose.material3.AlertDialog(onDismissRequest={showSecureStartError=false},
            title={Text("Não foi possível iniciar a partida")},
            text={Text(gameState.secureStartErrorMessage)},
            confirmButton={androidx.compose.material3.TextButton(onClick={showSecureStartError=false}){Text("OK")}})
    }

}

@Composable
private fun HomeModeButton(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    fillStart: Color,
    fillEnd: Color,
    testTag: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(fillStart, fillEnd)))
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(
                    listOf(accent.copy(alpha = 0.95f), accent.copy(alpha = 0.55f))
                ),
                shape = shape
            )
            .clickable { onClick() }
            .testTag(testTag)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp,
                maxLines = 1
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.75f),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun HomeNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color(0xFFE3E1E9),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = Color(0xFFE3E1E9),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1
        )
    }
}

@Composable
private fun HomeBottomNav(
    onOpenTasks: () -> Unit,
    onOpenShop: () -> Unit,
    onOpenRanking: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(shape)
            .background(Color(0xFF0F1118))
            .border(1.dp, Color(0xFF1F2230), shape)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        HomeNavItem(Icons.Default.List, "TAREFAS", "nav_tarefas_button", onOpenTasks)
        HomeNavItem(Icons.Default.ShoppingCart, "LOJA", "nav_loja_button", onOpenShop)
        HomeNavItem(Icons.Default.WorkspacePremium, "RANKING", "nav_ranking_button", onOpenRanking)
        HomeNavItem(Icons.Default.Settings, "AJUSTES", "nav_config_button", onOpenSettings)
        HomeNavItem(Icons.Default.Person, "PERFIL", "nav_perfil_button", onOpenProfile)
    }
}
