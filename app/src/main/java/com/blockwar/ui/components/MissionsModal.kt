package com.blockwar.ui.components

import com.blockwar.ui.theme.ArenaStyle

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.blockwar.model.Mission
import com.blockwar.model.MissionType

// Paleta "Neon Grid Battle" (Stitch)
private val MsBackground = ArenaStyle.Background
private val MsSurfaceLowest = ArenaStyle.Recessed
private val MsSurfaceLow = ArenaStyle.SurfaceLow
private val MsSurface = ArenaStyle.Surface
private val MsSurfaceHigh = ArenaStyle.SurfaceHigh
private val MsSurfaceHighest = ArenaStyle.SurfaceHighest
private val MsSurfaceBright = ArenaStyle.SurfaceHighest
private val MsOnSurface = ArenaStyle.Text
private val MsOnSurfaceVariant = ArenaStyle.Muted
private val MsOutline = Color(0xFF849495)
private val MsOutlineVariant = ArenaStyle.Outline
private val MsCyan = ArenaStyle.Cyan
private val MsCyanDim = Color(0xFF00DBE9)
private val MsOnCyan = Color(0xFF006970)
private val MsPurple = ArenaStyle.Purple
private val MsOnPurple = Color(0xFF480063)
private val MsPurpleSoft = Color(0xFFECB2FF)
private val MsPeach = Color(0xFFFFDBCF)
private val MsLime = ArenaStyle.Success

enum class MissionTab {
    DAILY, WEEKLY, MONTHLY, ACHIEVEMENTS
}

@Composable
fun MissionsModal(
    missions: List<Mission>,
    onClaimReward: (Mission) -> Unit,
    onClose: () -> Unit,
    onPlay: () -> Unit = onClose,
    operationMessage: String = ""
) {
    var selectedTab by remember { mutableStateOf(MissionTab.DAILY) }

    val dailyClaimable = missions.count { it.type == MissionType.DAILY && it.isCompleted && !it.isClaimed }
    val weeklyClaimable = missions.count { it.type == MissionType.WEEKLY && it.isCompleted && !it.isClaimed }
    val monthlyClaimable = missions.count { it.type == MissionType.MONTHLY && it.isCompleted && !it.isClaimed }

    val filteredMissions = when (selectedTab) {
        MissionTab.DAILY -> missions.filter { it.type == MissionType.DAILY && !it.isClaimed }
        MissionTab.WEEKLY -> missions.filter { it.type == MissionType.WEEKLY && !it.isClaimed }
        MissionTab.MONTHLY -> missions.filter { it.type == MissionType.MONTHLY && !it.isClaimed }
        MissionTab.ACHIEVEMENTS -> missions.filter { it.isClaimed }
    }

    // Meta diária: quantas missões diárias já foram concluídas (ou coletadas)
    val dailyAll = missions.filter { it.type == MissionType.DAILY }
    val dailyTotal = dailyAll.size
    val dailyDone = dailyAll.count { it.isCompleted || it.isClaimed }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MsBackground)
                .testTag("missions_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize().appSafeDrawingPadding()) {
                MissionsHeader(onBack = onClose)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MsOutlineVariant.copy(alpha = 0.5f))
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 64.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (operationMessage.isNotBlank()) {
                        item { Text(text = operationMessage, color = MsCyan, fontSize = 12.sp) }
                    }
                    if (dailyTotal > 0) {
                        item {
                            DailyGoalCard(done = dailyDone, total = dailyTotal)
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(androidx.compose.foundation.shape.CutCornerShape(6.dp))
                                .background(MsSurfaceLow)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            MissionTabItem(
                                title = "DIÁRIAS",
                                isSelected = selectedTab == MissionTab.DAILY,
                                claimableCount = dailyClaimable,
                                activeColor = MsCyan,
                                modifier = Modifier.weight(1f),
                                onClick = { selectedTab = MissionTab.DAILY }
                            )
                            MissionTabItem(
                                title = "SEMANAIS",
                                isSelected = selectedTab == MissionTab.WEEKLY,
                                claimableCount = weeklyClaimable,
                                activeColor = MsCyan,
                                modifier = Modifier.weight(1f),
                                onClick = { selectedTab = MissionTab.WEEKLY }
                            )
                            MissionTabItem(
                                title = "MENSAIS",
                                isSelected = selectedTab == MissionTab.MONTHLY,
                                claimableCount = monthlyClaimable,
                                activeColor = MsCyan,
                                modifier = Modifier.weight(1f),
                                onClick = { selectedTab = MissionTab.MONTHLY }
                            )
                            MissionTabItem(
                                title = "CONQUISTAS",
                                isSelected = selectedTab == MissionTab.ACHIEVEMENTS,
                                claimableCount = 0,
                                activeColor = MsCyan,
                                modifier = Modifier.weight(1.25f),
                                onClick = { selectedTab = MissionTab.ACHIEVEMENTS }
                            )
                        }
                    }

                    if (filteredMissions.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 160.dp)
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Nenhuma missão disponível nesta categoria.",
                                    color = MsOnSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    } else {
                        items(filteredMissions, key = { it.id }) { mission ->
                            MissionCard(mission, onClaimReward, onPlay)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MissionsHeader(onBack: () -> Unit) {
    ArenaHeader("TAREFAS", onBack, Icons.Default.TaskAlt, "close_missions_button")
}

@Composable
private fun NeonProgressBar(
    fraction: Float,
    barHeight: androidx.compose.ui.unit.Dp,
    fillBrush: Brush,
    trackColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(barHeight)
            .clip(RoundedCornerShape(50))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(50))
                .background(fillBrush)
        )
    }
}

@Composable
private fun DailyGoalCard(done: Int, total: Int) {
    val fraction = if (total > 0) done.toFloat() / total.toFloat() else 0f
    val percent = (fraction * 100f).toInt()
    val remaining = (total - done).coerceAtLeast(0)
    val shape = androidx.compose.foundation.shape.CutCornerShape(6.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(MsSurfaceHigh, Color(0xFF1F3A3E))))
            .border(1.dp, MsCyan.copy(alpha = 0.2f), shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Meta Diária",
                color = MsOnSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MsCyan.copy(alpha = 0.2f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$done/$total CONCLUÍDAS",
                    color = MsCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }
        }

        NeonProgressBar(
            fraction = fraction,
            barHeight = 10.dp,
            fillBrush = Brush.horizontalGradient(listOf(MsCyanDim, MsCyan)),
            trackColor = MsSurfaceLowest
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when {
                    remaining == 0 -> "Meta diária completa!"
                    remaining == 1 -> "Falta 1 tarefa"
                    else -> "Faltam $remaining tarefas"
                },
                color = MsOnSurfaceVariant,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$percent% COMPLETO",
                color = MsCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun MissionTabItem(
    title: String,
    isSelected: Boolean,
    claimableCount: Int,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) activeColor else Color.Transparent,
        label = "tab_bg"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = if (isSelected) MsOnCyan else MsOnSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false
            )
            if (claimableCount > 0) {
                Spacer(modifier = Modifier.width(3.dp))
                Box(
                    modifier = Modifier
                        .size(15.dp)
                        .clip(CircleShape)
                        .background(MsLime),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$claimableCount",
                        color = Color.Black,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

private fun missionIcon(mission: Mission): ImageVector {
    val t = mission.title.lowercase()
    return when {
        t.contains("combo") -> Icons.Default.Bolt
        t.contains("cpu") -> Icons.Default.SmartToy
        t.contains("linha") -> Icons.Default.GridView
        t.contains("vença") || t.contains("vitória") || t.contains("torneio") -> Icons.Default.EmojiEvents
        t.contains("login") -> Icons.Default.CalendarMonth
        else -> when (mission.type) {
            MissionType.DAILY -> Icons.Default.Today
            MissionType.WEEKLY -> Icons.Default.DateRange
            MissionType.MONTHLY -> Icons.Default.CalendarMonth
        }
    }
}

@Composable
fun MissionCard(
    mission: Mission,
    onClaimReward: (Mission) -> Unit,
    onPlay: () -> Unit = {}
) {
    val isClaimed = mission.isClaimed
    val isClaimable = mission.isCompleted && !mission.isClaimed
    val shape = androidx.compose.foundation.shape.CutCornerShape(6.dp)

    val accent = if (isClaimable) MsPurple else MsCyan
    val progressFraction = if (isClaimed) 1f else {
        if (mission.goal > 0) (mission.progress.toFloat() / mission.goal.toFloat()).coerceIn(0f, 1f) else 0f
    }
    val shownProgress = if (isClaimed || isClaimable) mission.goal else mission.progress
    val percent = (progressFraction * 100f).toInt()

    val cardModifier = when {
        isClaimed -> Modifier
            .alpha(0.75f)
            .clip(shape)
            .background(MsSurfaceLow)
            .border(1.dp, MsOutlineVariant.copy(alpha = 0.1f), shape)
        isClaimable -> Modifier
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(MsSurface, MsPurple.copy(alpha = 0.12f))))
            .border(1.dp, MsPurple.copy(alpha = 0.5f), shape)
        else -> Modifier
            .clip(shape)
            .background(MsSurface)
            .border(1.dp, MsOutlineVariant.copy(alpha = 0.2f), shape)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(cardModifier)
            .padding(12.dp)
            .testTag("mission_card_${mission.id}"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Top
            ) {
                // Ícone da missão
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                isClaimable -> MsPurple.copy(alpha = 0.2f)
                                isClaimed -> MsSurfaceHighest.copy(alpha = 0.6f)
                                else -> MsSurfaceHighest
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = missionIcon(mission),
                        contentDescription = null,
                        tint = when {
                            isClaimable -> MsPurpleSoft
                            isClaimed -> MsOnSurfaceVariant
                            else -> MsCyan
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = mission.title,
                            color = if (isClaimed) MsOnSurfaceVariant else MsOnSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            textDecoration = if (isClaimed) TextDecoration.LineThrough else TextDecoration.None,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isClaimable) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Missão completa",
                                tint = MsPurpleSoft,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    if (mission.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = mission.description,
                            color = if (isClaimed) MsOutline else MsOnSurfaceVariant,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.SansSerif,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isClaimed) {
                        BlockoinIcon(size = 18.dp)
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = if (isClaimed) "RECOMPENSA COLETADA"
                        else "+${mission.reward} ${mission.rewardType.uppercase()}",
                        color = when {
                            isClaimed -> MsOutline
                            isClaimable -> MsPurpleSoft
                            else -> MsPeach
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Ação: JOGAR / COLETAR / COLETADO
            when {
                isClaimed -> {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MsSurfaceHighest)
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MsOnSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "COLETADO",
                            color = MsOnSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.SansSerif,
                            maxLines = 1
                        )
                    }
                }
                isClaimable -> {
                    val pulse by rememberInfiniteTransition(label = "claimPulse").animateFloat(
                        initialValue = 0.8f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(900),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "claimPulseAlpha"
                    )
                    Row(
                        modifier = Modifier
                            .alpha(pulse)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MsPurple)
                            .clickable { onClaimReward(mission) }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                            .testTag("claim_mission_${mission.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "COLETAR",
                            color = MsOnPurple,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Redeem,
                            contentDescription = null,
                            tint = MsOnPurple,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                else -> {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MsSurfaceBright)
                            .border(1.dp, MsCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .clickable { onPlay() }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "JOGAR",
                            color = MsCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MsCyan,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // Progresso
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        isClaimed -> "Completado"
                        isClaimable -> "MISSÃO COMPLETA!"
                        else -> "Progresso"
                    },
                    color = when {
                        isClaimed -> MsOutline
                        isClaimable -> MsPurpleSoft
                        else -> MsOnSurfaceVariant
                    },
                    fontSize = 11.sp,
                    fontWeight = if (isClaimable) FontWeight.Bold else FontWeight.Normal,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "$shownProgress / ${mission.goal} ($percent%)",
                    color = when {
                        isClaimed -> MsOutline
                        isClaimable -> MsPurpleSoft
                        mission.progress > 0 -> MsCyan
                        else -> MsOnSurfaceVariant
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    softWrap = false
                )
            }
            NeonProgressBar(
                fraction = progressFraction,
                barHeight = 6.dp,
                fillBrush = solidBrush(
                    when {
                        isClaimed -> MsOutlineVariant
                        else -> accent
                    }
                ),
                trackColor = MsSurfaceHighest
            )
        }
    }
}

private fun solidBrush(color: Color): Brush = Brush.horizontalGradient(listOf(color, color))
