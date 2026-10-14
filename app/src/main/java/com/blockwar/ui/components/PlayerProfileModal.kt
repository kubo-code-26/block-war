package com.blockwar.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.blockwar.model.AggregatedStats
import com.blockwar.model.MatchRecord
import com.blockwar.model.ShopItem
import com.blockwar.model.VersusGameState
import com.blockwar.network.LeaderboardEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// Paleta "Neon Grid Battle" (Stitch)
private val PfBackground = Color(0xFF121318)
private val PfSurfaceLowest = Color(0xFF0D0E13)
private val PfSurfaceLow = Color(0xFF1A1B21)
private val PfSurface = Color(0xFF1E1F25)
private val PfSurfaceHigh = Color(0xFF292A2F)
private val PfOnSurface = Color(0xFFE3E1E9)
private val PfOnSurfaceVariant = Color(0xFFB9CACB)
private val PfOutlineVariant = Color(0xFF3B494B)
private val PfCyan = Color(0xFF00F0FF)
private val PfOnCyan = Color(0xFF006970)
private val PfPurple = Color(0xFFCF5CFF)
private val PfPurpleSoft = Color(0xFFECB2FF)
private val PfGold = Color(0xFFFFB300)
private val PfGreen = Color(0xFF39FF14)
private val PfRed = Color(0xFFFF5470)

private fun formatInt(value: Int): String =
    String.format(Locale("pt", "BR"), "%,d", value)

private fun formatDuration(totalSeconds: Long): String {
    val seconds = totalSeconds.coerceAtLeast(0L)
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) "${hours}h ${minutes}min" else "${minutes}m ${secs}s"
}

private val ProfileCategoryTabs = listOf(
    "blocos" to "BLOCOS",
    "tabuleiros" to "TABULEIROS",
    "avatares" to "AVATARES",
    "molduras" to "MOLDURAS",
    "banners" to "BANNERS",
    "fundos" to "FUNDOS",
    "musicas" to "MÚSICAS",
    "efeitos" to "EFEITOS",
    "animacoes" to "ANIMAÇÕES"
)

private fun profileCategoryIcon(key: String): ImageVector = when (key) {
    "blocos" -> Icons.Default.GridView
    "tabuleiros" -> Icons.Default.Dashboard
    "avatares" -> Icons.Default.AccountCircle
    "molduras" -> Icons.Default.CropSquare
    "banners" -> Icons.Default.Flag
    "fundos" -> Icons.Default.Wallpaper
    "efeitos" -> Icons.Default.AutoAwesome
    "musicas" -> Icons.Default.MusicNote
    else -> Icons.Default.Bolt
}

@Composable
fun PlayerProfileModal(
    gameState: VersusGameState,
    stats: AggregatedStats,
    matchHistory: List<MatchRecord>,
    leaderboardList: List<LeaderboardEntry>,
    getPatentInfo: (Int) -> Pair<String, Color>,
    onClose: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenMissions: () -> Unit,
    onOpenShop: () -> Unit,
    onOpenRanking: () -> Unit,
    onOpenSettings: () -> Unit,
    onSavePlayerName: (String, String) -> Unit,
    onSaveLocalPin: (String) -> Unit,
    onEquipItem: (ShopItem) -> Unit,
    getLocalPin: () -> String,
    shopItems: List<ShopItem> = emptyList(),
    modifier: Modifier = Modifier,
    onLogout: () -> Unit = {},
    onGooglePlayGamesSignIn: () -> Unit = {}
) {
    var showLogoutConfirmation by remember { mutableStateOf(false) }
    val rawName = gameState.nickname.ifEmpty { "JOGADOR" }
    var showEditPanel by remember { mutableStateOf(false) }
    var tempNickname by remember { mutableStateOf(rawName) }
    var tempPin by remember { mutableStateOf(getLocalPin()) }
    var selectedEquipTab by remember { mutableStateOf("avatares") }

    var achievementsExpanded by remember { mutableStateOf(true) }
    var statsExpanded by remember { mutableStateOf(true) }
    var customExpanded by remember { mutableStateOf(false) }
    var historyExpanded by remember { mutableStateOf(false) }

    val currentRankIndex = leaderboardList.indexOfFirst { it.id == rawName.lowercase().trim() }
    val globalRankText = if (currentRankIndex != -1) "#${currentRankIndex + 1} GLOBAL" else "FORA DO TOP"
    val (patentName, patentColor) = getPatentInfo(gameState.pontos_bw)

    val currentAvatarId = gameState.inventory.equippedItems["avatares"] ?: "avatar_default"
    val currentMolduraId = gameState.inventory.equippedItems["molduras"] ?: "moldura_simple"

    // Lifetime online statistics confirmed by Firebase; local history remains independent.
    val currentWinStreak = stats.currentWinStreak
    val bestWinStreak = stats.bestWinStreak
    val avgLines = if (stats.totalMatches > 0) stats.totalLinesCleared.toFloat() / stats.totalMatches else 0f
    val avgLinesText = String.format(Locale("pt", "BR"), "%.1f", avgLines)

    val effectiveWins = stats.totalWins

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PfBackground)
                .testTag("profile_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize().appSafeDrawingPadding().imePadding()) {
                ProfileHeader(onBack = onClose)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(PfOutlineVariant.copy(alpha = 0.5f))
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 64.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ---------------- Cartão principal ----------------
                    val cardShape = RoundedCornerShape(16.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(cardShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF1B2A2F), PfSurfaceLow, Color(0xFF2A1B35))
                                )
                            )
                            .border(1.dp, PfOutlineVariant.copy(alpha = 0.6f), cardShape)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(
                                            width = 2.dp,
                                            brush = Brush.linearGradient(listOf(PfCyan, PfPurple)),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(2.dp)
                                ) {
                                    AvatarWithFrame(
                                        avatarId = currentAvatarId,
                                        molduraId = currentMolduraId,
                                        size = 76.dp
                                    )
                                }
                                // Indicador de status
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .offset(x = 6.dp, y = 6.dp)
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(PfCyan)
                                        .border(3.dp, PfBackground, CircleShape)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = rawName.uppercase(),
                                    color = PfOnSurface,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(patentColor.copy(alpha = 0.2f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = patentName.uppercase(),
                                            color = patentColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = globalRankText,
                                        color = PfOnSurfaceVariant,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    text = "${formatInt(gameState.pontos_bw)} PONTOS BW",
                                    color = PfCyan,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1
                                )
                            }
                        }

                        // Botão EDITAR PERFIL
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PfSurfaceHigh)
                                .clickable { showEditPanel = !showEditPanel }
                                .testTag("edit_profile_toggle_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = PfOnSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (showEditPanel) "FECHAR EDIÇÃO" else "EDITAR PERFIL",
                                    color = PfOnSurface,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        AnimatedVisibility(visible = showEditPanel) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PfSurfaceLowest)
                                    .border(1.dp, PfPurple.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = tempNickname,
                                    onValueChange = { tempNickname = it.take(12).trim() },
                                    singleLine = true,
                                    label = { Text("NICKNAME", fontSize = 10.sp, color = PfCyan) },
                                    textStyle = TextStyle(
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PfCyan,
                                        unfocusedBorderColor = PfOutlineVariant
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("edit_nickname_input")
                                )

                                OutlinedTextField(
                                    value = tempPin,
                                    onValueChange = { newValue ->
                                        if (newValue.length <= 4 && newValue.all { it.isDigit() }) {
                                            tempPin = newValue
                                        }
                                    },
                                    singleLine = true,
                                    label = { Text("PIN DE SEGURANÇA (4 DÍGITOS)", fontSize = 10.sp, color = PfGold) },
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    textStyle = TextStyle(
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PfGold,
                                        unfocusedBorderColor = PfOutlineVariant
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("edit_pin_input")
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(PfPurple)
                                        .clickable {
                                            if (tempNickname.isNotEmpty()) {
                                                onSavePlayerName(tempNickname, tempPin)
                                                showEditPanel = false
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "SALVAR ALTERAÇÕES",
                                        color = Color(0xFF480063),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.SansSerif
                                    )
                                }
                            }
                        }
                    }


                    ProfileSection(
                        title = "CONQUISTAS",
                        icon = Icons.Default.MilitaryTech,
                        accent = PfGold,
                        expanded = achievementsExpanded,
                        onToggle = { achievementsExpanded = !achievementsExpanded },
                        tag = "profile_section_achievements"
                    ) {
                        AchievementsPanel(stats, gameState.inventory, shopItems)
                    }

                    // ---------------- ESTATÍSTICAS ----------------
                    ProfileSection(
                        title = "ESTATÍSTICAS",
                        icon = Icons.Default.BarChart,
                        accent = PfCyan,
                        expanded = statsExpanded,
                        onToggle = { statsExpanded = !statsExpanded },
                        tag = "profile_section_stats"
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ProfileStatCard(
                                    title = "PARTIDAS",
                                    value = formatInt(stats.totalMatches),
                                    valueColor = PfOnSurface,
                                    subtitle = "Somente partidas online",
                                    subtitleColor = PfCyan,
                                    icon = Icons.Default.SportsEsports,
                                    iconColor = PfOnSurfaceVariant,
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )
                                ProfileStatCard(
                                    title = "VITÓRIAS · DERROTAS",
                                    value = "$effectiveWins · ${stats.totalLosses}",
                                    valueColor = PfOnSurface,
                                    subtitle = "Vitórias · Derrotas",
                                    subtitleColor = PfCyan,
                                    icon = Icons.Default.Flag,
                                    iconColor = PfOnSurfaceVariant,
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ProfileStatCard(
                                    title = "TAXA VITÓRIA",
                                    value = "${stats.winRatePercent}%",
                                    valueColor = PfPurpleSoft,
                                    subtitle = globalRankText,
                                    subtitleColor = PfOnSurfaceVariant,
                                    icon = Icons.Default.EmojiEvents,
                                    iconColor = PfPurpleSoft,
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )
                                ProfileStatCard(
                                    title = "SEQUÊNCIA DE VITÓRIAS",
                                    value = "$currentWinStreak",
                                    valueColor = PfGold,
                                    subtitle = "Melhor: $bestWinStreak",
                                    subtitleColor = PfOnSurfaceVariant,
                                    icon = Icons.Default.LocalFireDepartment,
                                    iconColor = PfGold,
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ProfileStatCard(
                                    title = "LINHAS LIMPAS",
                                    value = formatInt(stats.totalLinesCleared),
                                    valueColor = PfOnSurface,
                                    subtitle = "Média $avgLinesText/partida",
                                    subtitleColor = PfCyan,
                                    icon = Icons.Default.GridView,
                                    iconColor = PfOnSurfaceVariant,
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )
                                ProfileStatCard(
                                    title = "ATAQUES ENVIADOS",
                                    value = formatInt(stats.totalAttacksSent),
                                    valueColor = PfCyan,
                                    subtitle = "Blocos cinzas",
                                    subtitleColor = PfOnSurfaceVariant,
                                    icon = Icons.Default.Bolt,
                                    iconColor = PfCyan,
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ProfileStatCard(
                                    title = "MAIOR PONTUAÇÃO",
                                    value = "${formatInt(stats.highScore)} BW",
                                    valueColor = PfOnSurface,
                                    subtitle = "Total: ${formatInt(stats.totalScore)} BW",
                                    subtitleColor = PfCyan,
                                    icon = Icons.Default.Star,
                                    iconColor = PfOnSurfaceVariant,
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )
                                ProfileStatCard(
                                    title = "TEMPO MÉDIO",
                                    value = formatDuration(stats.averageDurationSeconds),
                                    valueColor = PfCyan,
                                    subtitle = "Total: ${formatDuration(stats.totalDurationSeconds)}",
                                    subtitleColor = PfOnSurfaceVariant,
                                    icon = Icons.Default.Timer,
                                    iconColor = PfCyan,
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )
                            }
                            ProfileStatCard(
                                title = "VITÓRIA MAIS RÁPIDA",
                                value = if(stats.fastestWinSeconds>0) formatDuration(stats.fastestWinSeconds) else "—",
                                valueColor = PfCyan,
                                subtitle = "Menor tempo de uma vitória online",
                                subtitleColor = PfOnSurfaceVariant,
                                icon = Icons.Default.Timer,
                                iconColor = PfCyan,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // ---------------- ITEM DE CUSTOMIZAÇÃO ----------------
                    ProfileSection(
                        title = "ITEM DE CUSTOMIZAÇÃO",
                        icon = Icons.Default.MilitaryTech,
                        accent = PfPurple,
                        expanded = customExpanded,
                        onToggle = { customExpanded = !customExpanded },
                        tag = "profile_section_custom"
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(PfSurfaceLowest)
                                    .horizontalScroll(rememberScrollState())
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                ProfileCategoryTabs.forEach { (catId, label) ->
                                    val isSelected = selectedEquipTab == catId
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) PfCyan else Color.Transparent)
                                            .clickable { selectedEquipTab = catId }
                                            .padding(horizontal = 10.dp, vertical = 8.dp)
                                            .testTag("profile_tab_$catId"),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = profileCategoryIcon(catId),
                                            contentDescription = null,
                                            tint = if (isSelected) PfOnCyan else PfOnSurfaceVariant,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = label,
                                            color = if (isSelected) PfOnCyan else PfOnSurfaceVariant,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.SansSerif,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }

                            val catalog = shopItems.filter { it.itemCategory == selectedEquipTab }
                            if (catalog.isEmpty()) {
                                Text(
                                    text = "Abra a loja para ver os itens desta categoria.",
                                    color = PfOnSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenShop() }
                                        .padding(vertical = 12.dp)
                                )
                            } else {
                                catalog.forEach { item ->
                                    EquipItemRow(
                                        item = item,
                                        isBought = gameState.inventory.boughtItems.contains(item.id),
                                        isEquipped = gameState.inventory.equippedItems[item.itemCategory] == item.id,
                                        onEquip = { onEquipItem(item) },
                                        onLockedClick = onOpenShop
                                    )
                                }
                            }
                        }
                    }

                    // ---------------- HISTÓRICO DE DESEMPENHO ----------------
                    ProfileSection(
                        title = "HISTÓRICO DE DESEMPENHO",
                        icon = Icons.Default.History,
                        accent = PfCyan,
                        expanded = historyExpanded,
                        onToggle = { historyExpanded = !historyExpanded },
                        tag = "match_history_button"
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (matchHistory.isEmpty()) {
                                Text(
                                    text = "Nenhuma partida registrada ainda. Jogue contra a CPU ou online para gerar estatísticas!",
                                    color = PfOnSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                val sortedHistory = remember(matchHistory) {
                                    matchHistory.sortedByDescending { it.timestamp }
                                }
                                sortedHistory.forEach { record ->
                                    ProfileMatchRow(record = record)
                                }
                            }
                        }
                    }

                    androidx.compose.material3.OutlinedButton(
                        onClick = { showLogoutConfirmation = true },
                        enabled = !gameState.playGamesBusy && !gameState.isSearchingOnlineOpponent && gameState.onlineRoomCode.isBlank(),
                        modifier = Modifier.fillMaxWidth().testTag("profile_logout_button")
                    ) { Text("SAIR DA CONTA", color = Color(0xFFFF6B88)) }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
    if (showLogoutConfirmation) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showLogoutConfirmation = false },
            title = { Text("Sair da conta?") },
            text = { Text("Você voltará à tela de entrada. Seu progresso salvo não será apagado. Para entrar novamente, use seu nome e PIN ou o Google Play Games vinculado.") },
            confirmButton = { androidx.compose.material3.TextButton(onClick = {
                showLogoutConfirmation = false
                onLogout()
            }) { Text("Sair") } },
            dismissButton = { androidx.compose.material3.TextButton(onClick = {
                showLogoutConfirmation = false
            }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun ProfileHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(PfSurfaceHigh)
                .clickable { onBack() }
                .testTag("close_profile_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Voltar",
                tint = PfOnSurface,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(PfCyan.copy(alpha = 0.12f))
                .border(1.dp, PfCyan.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Perfil",
                tint = PfCyan,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "PERFIL",
            color = PfOnSurface,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 0.5.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun ProfileSection(
    title: String,
    icon: ImageVector,
    accent: Color,
    expanded: Boolean,
    onToggle: () -> Unit,
    tag: String,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(PfSurfaceLow)
            .border(1.dp, PfOutlineVariant.copy(alpha = 0.4f), shape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(12.dp)
                .testTag(tag),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                color = PfOnSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 0.3.sp,
                maxLines = 2,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(24.dp)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun ProfileStatCard(
    title: String,
    value: String,
    valueColor: Color,
    subtitle: String,
    subtitleColor: Color,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    val cardMinHeight = with(LocalDensity.current) { 132.sp.toDp() }
    Column(
        modifier = modifier
            .heightIn(min = cardMinHeight)
            .clip(shape)
            .background(PfSurface)
            .border(1.dp, PfOutlineVariant.copy(alpha = 0.4f), shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = title,
                color = PfOnSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.8.sp,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = value,
            color = valueColor,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            maxLines = 1
        )
        Text(
            text = subtitle,
            color = subtitleColor,
            fontSize = 11.sp,
            fontFamily = FontFamily.SansSerif,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EquipItemRow(
    item: ShopItem,
    isBought: Boolean,
    isEquipped: Boolean,
    onEquip: () -> Unit,
    onLockedClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(PfSurface)
            .border(
                1.dp,
                if (isEquipped) PfGreen.copy(alpha = 0.6f) else PfOutlineVariant.copy(alpha = 0.4f),
                shape
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PfSurfaceLowest)
                .border(1.dp, PfOutlineVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            when (item.itemCategory) {
                "avatares" -> AvatarImage(avatarId = item.id, modifier = Modifier.fillMaxSize())
                "molduras" -> AvatarWithFrame(
                    avatarId = "avatar_default",
                    molduraId = item.id,
                    size = 36.dp
                )
                else -> Icon(
                    imageVector = profileCategoryIcon(item.itemCategory),
                    contentDescription = null,
                    tint = PfCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                color = PfOnSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.description.isNotBlank()) {
                Text(
                    text = item.description,
                    color = PfOnSurfaceVariant,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        when {
            isEquipped -> Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(PfGreen.copy(alpha = 0.12f))
                    .border(1.dp, PfGreen.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("item_equipped_tag_${item.id}")
            ) {
                Text(
                    text = "EM USO",
                    color = PfGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            isBought -> Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(PfCyan)
                    .clickable { onEquip() }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
                    .testTag("equip_item_${item.id}")
            ) {
                Text(
                    text = "EQUIPAR",
                    color = Color(0xFF002022),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            else -> Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(PfGold.copy(alpha = 0.12f))
                    .border(1.dp, PfGold.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .clickable { onLockedClick() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("item_locked_tag_${item.id}")
            ) {
                Text(
                    text = "NA LOJA",
                    color = PfGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun ProfileMatchRow(record: MatchRecord) {
    val isWin = record.isVictory
    val statusColor = if (isWin) PfGreen else PfRed
    val resultText = if (isWin) "VITÓRIA" else "DERROTA"
    val shape = RoundedCornerShape(10.dp)
    val minutes = record.durationSeconds / 60
    val seconds = record.durationSeconds % 60
    val durationText = "${minutes}m ${seconds}s"
    val dateText = remember(record.timestamp) {
        SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR")).format(Date(record.timestamp))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(PfSurface)
            .border(1.dp, PfOutlineVariant.copy(alpha = 0.4f), shape)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .border(1.dp, statusColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = resultText,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = record.gameMode.ifEmpty { "VERSUS" }.uppercase(),
                    color = PfOnSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = dateText,
                color = PfOnSurfaceVariant,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "BW: ${formatInt(record.score)}",
                color = PfGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "⏱ $durationText",
                color = PfCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Linhas: ${record.linesCleared}",
                color = PfOnSurfaceVariant,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Ataques: ${record.attacksSent}",
                color = PfOnSurfaceVariant,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Combo: x${record.maxCombo}",
                color = PfPurpleSoft,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}