package com.blockwar.ui.components

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.blockwar.model.VersusGameState
import com.blockwar.network.LeaderboardEntry

// Paleta "Neon Grid Battle" (Stitch)
private val RkBackground = Color(0xFF121318)
private val RkSurfaceLow = Color(0xFF1A1B21)
private val RkSurface = Color(0xFF1E1F25)
private val RkSurfaceHigh = Color(0xFF292A2F)
private val RkSurfaceHighest = Color(0xFF34343A)
private val RkOnSurface = Color(0xFFE3E1E9)
private val RkOnSurfaceVariant = Color(0xFFB9CACB)
private val RkOutlineVariant = Color(0xFF3B494B)
private val RkCyan = Color(0xFF00F0FF)
private val RkCyanSoft = Color(0xFFDBFCFF)
private val RkPurple = Color(0xFFCF5CFF)
private val RkPeach = Color(0xFFFFDBCF)
private val RkPeachDim = Color(0xFFFFB59C)
private val RkLime = Color(0xFF39FF14)

private val RkNpcAvatars = listOf("avatar_cyber_pilot", "avatar_pixel_ninja", "avatar_mecha_bot", "avatar_hacker")

private fun rkAvatarId(entry: LeaderboardEntry, isSelf: Boolean, selfAvatarId: String): String =
    if (isSelf) selfAvatarId
    else RkNpcAvatars[entry.nickname.hashCode().coerceAtLeast(0) % RkNpcAvatars.size]

private fun formatPbw(value: Int): String =
    String.format(java.util.Locale("pt", "BR"), "%,d", value) + " PBW"

@Composable
fun RankingModal(
    gameState: VersusGameState,
    leaderboardList: List<LeaderboardEntry>,
    isLoading: Boolean,
    errorMessage: String,
    currentNickname: String,
    getPatentInfo: (Int) -> Pair<String, Color>,
    onDismiss: () -> Unit,
    onPlayAction: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(RkBackground)
                .testTag("ranking_modal")
        ) {
            Column(modifier = Modifier.fillMaxSize().appSafeDrawingPadding()) {
                RankingHeader(onBack = onDismiss)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(RkOutlineVariant.copy(alpha = 0.5f))
                )

                when {
                    isLoading -> RankingStateMessage("CARREGANDO RANKING...", RkCyan)
                    errorMessage.isNotEmpty() -> RankingStateMessage("ERRO: $errorMessage", Color(0xFFFFB4AB))
                    leaderboardList.isEmpty() -> RankingStateMessage("NENHUM JOGADOR REGISTRADO", RkOnSurfaceVariant)
                    else -> {
                        val top3 = leaderboardList.take(3)
                        val restList = if (isExpanded) {
                            leaderboardList.drop(3).take(97) // #4 a #100
                        } else {
                            leaderboardList.drop(3).take(7) // #4 a #10
                        }
                        val lastShown = minOf(leaderboardList.size, if (isExpanded) 100 else 10)

                        val selfAvatarId = gameState.inventory.equippedItems["avatares"] ?: "avatar_default"
                        val selfMolduraId = gameState.inventory.equippedItems["molduras"] ?: "moldura_simple"

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            contentPadding = PaddingValues(top = 12.dp, bottom = 64.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                PodiumStage(
                                    top3 = top3,
                                    getPatentInfo = getPatentInfo,
                                    currentNickname = currentNickname,
                                    selfAvatarId = selfAvatarId,
                                    selfMolduraId = selfMolduraId
                                )
                            }

                            if (restList.isNotEmpty()) {
                                item { RankingSectionHeader(lastShown = lastShown) }
                            }

                            itemsIndexed(restList) { index, entry ->
                                val position = index + 4
                                val isSelf = entry.nickname.equals(currentNickname, ignoreCase = true)
                                RankingRowItem(
                                    position = position,
                                    entry = entry,
                                    isSelf = isSelf,
                                    selfAvatarId = selfAvatarId,
                                    selfMolduraId = selfMolduraId,
                                    getPatentInfo = getPatentInfo
                                )
                            }

                            if (leaderboardList.size > 10) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(RkSurface)
                                            .border(
                                                width = 1.dp,
                                                color = RkCyan.copy(alpha = 0.4f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { isExpanded = !isExpanded }
                                            .padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = if (isExpanded) "MOSTRAR MENOS" else "VER MAIS 90 (TOP 100)",
                                                color = RkCyan,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                letterSpacing = 1.sp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = null,
                                                tint = RkCyan,
                                                modifier = Modifier.size(18.dp)
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
    }
}

@Composable
private fun RankingHeader(onBack: () -> Unit) {
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
                .background(RkSurfaceHigh)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Voltar",
                tint = RkOnSurface,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(RkCyan.copy(alpha = 0.12f))
                .border(1.dp, RkCyan.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = "Troféu",
                tint = RkCyan,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "RANKING GLOBAL",
            color = RkOnSurface,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 0.5.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun RankingStateMessage(message: String, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun RankingSectionHeader(lastShown: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Leaderboard,
                contentDescription = null,
                tint = RkOnSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "QUADRO DE HONRA (#4 - #$lastShown)",
                color = RkOnSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp,
                maxLines = 1
            )
        }
        Text(
            text = "PONTOS BW",
            color = RkOnSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun PodiumStage(
    top3: List<LeaderboardEntry>,
    getPatentInfo: (Int) -> Pair<String, Color>,
    currentNickname: String,
    selfAvatarId: String,
    selfMolduraId: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "crownFloat")
    val crownYOffset by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "crownY"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF16272C), RkSurfaceLow, Color(0xFF111217))
                )
            )
            .border(1.dp, RkOutlineVariant.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .padding(top = 20.dp, start = 10.dp, end = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // #2 (esquerda)
            Box(modifier = Modifier.weight(1f)) {
                if (top3.size >= 2) {
                    val entry = top3[1]
                    val (patentName, patentColor) = getPatentInfo(entry.pontos_bw)
                    PodiumColumn(
                        rank = 2,
                        entry = entry,
                        patentName = patentName,
                        patentColor = patentColor,
                        isSelf = entry.nickname.equals(currentNickname, ignoreCase = true),
                        selfAvatarId = selfAvatarId,
                        selfMolduraId = selfMolduraId,
                        crownOffset = 0f
                    )
                }
            }

            // #1 (centro, elevado)
            Box(modifier = Modifier.weight(1.18f)) {
                if (top3.isNotEmpty()) {
                    val entry = top3[0]
                    val (patentName, patentColor) = getPatentInfo(entry.pontos_bw)
                    PodiumColumn(
                        rank = 1,
                        entry = entry,
                        patentName = patentName,
                        patentColor = patentColor,
                        isSelf = entry.nickname.equals(currentNickname, ignoreCase = true),
                        selfAvatarId = selfAvatarId,
                        selfMolduraId = selfMolduraId,
                        crownOffset = crownYOffset
                    )
                }
            }

            // #3 (direita)
            Box(modifier = Modifier.weight(1f)) {
                if (top3.size >= 3) {
                    val entry = top3[2]
                    val (patentName, patentColor) = getPatentInfo(entry.pontos_bw)
                    PodiumColumn(
                        rank = 3,
                        entry = entry,
                        patentName = patentName,
                        patentColor = patentColor,
                        isSelf = entry.nickname.equals(currentNickname, ignoreCase = true),
                        selfAvatarId = selfAvatarId,
                        selfMolduraId = selfMolduraId,
                        crownOffset = 0f
                    )
                }
            }
        }
    }
}

@Composable
private fun PodiumColumn(
    rank: Int,
    entry: LeaderboardEntry,
    patentName: String,
    patentColor: Color,
    isSelf: Boolean,
    selfAvatarId: String,
    selfMolduraId: String,
    crownOffset: Float
) {
    val isChamp = rank == 1
    val avatarSize = if (isChamp) 72.dp else 56.dp
    val ringShape = RoundedCornerShape(if (isChamp) 16.dp else 12.dp)

    val ringBrush = when (rank) {
        1 -> Brush.linearGradient(listOf(RkPeach, RkCyan, RkPurple))
        2 -> Brush.verticalGradient(listOf(RkOnSurfaceVariant, RkSurfaceHighest))
        else -> Brush.verticalGradient(listOf(RkPeachDim, Color(0xFF822700)))
    }

    val avatarId = rkAvatarId(entry, isSelf, selfAvatarId)
    val molduraId = if (isSelf) {
        selfMolduraId
    } else when (rank) {
        1 -> "moldura_cyber_gold"
        2 -> "moldura_neon"
        else -> "moldura_plasma"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isChamp) Modifier.offset(y = (-8).dp) else Modifier)
    ) {
        if (isChamp) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = "Coroa",
                tint = RkPeach,
                modifier = Modifier
                    .size(24.dp)
                    .offset(y = crownOffset.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
        }

        // Avatar com anel + selo de posição
        Box(contentAlignment = Alignment.BottomCenter) {
            Box(
                modifier = Modifier
                    .clip(ringShape)
                    .background(ringBrush)
                    .padding(if (isChamp) 3.dp else 2.dp),
                contentAlignment = Alignment.Center
            ) {
                AvatarWithFrame(
                    avatarId = avatarId,
                    molduraId = molduraId,
                    size = avatarSize
                )
            }

            Row(
                modifier = Modifier
                    .offset(y = 10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (isChamp) RkPeach else RkSurfaceHighest)
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isChamp) {
                    Icon(
                        imageVector = Icons.Default.MilitaryTech,
                        contentDescription = null,
                        tint = Color(0xFF390C00),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                }
                Text(
                    text = "#$rank",
                    color = when (rank) {
                        1 -> Color(0xFF390C00)
                        2 -> RkOnSurface
                        else -> RkPeachDim
                    },
                    fontSize = if (isChamp) 13.sp else 12.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pedestal
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(if (isChamp) RkSurfaceHighest else RkSurfaceHigh.copy(alpha = 0.9f))
                .padding(horizontal = 6.dp, vertical = if (isChamp) 10.dp else 8.dp)
        ) {
            Text(
                text = if (isSelf) "${entry.nickname} (VOCÊ)" else entry.nickname,
                color = if (isSelf) RkLime else if (isChamp) RkCyanSoft else RkOnSurface,
                fontSize = if (isChamp) 14.sp else 13.sp,
                fontWeight = if (isChamp) FontWeight.Black else FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatPbw(entry.pontos_bw),
                color = if (isChamp) RkPeach else RkCyan,
                fontSize = if (isChamp) 14.sp else 12.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                softWrap = false
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(if (isChamp) 50 else 4))
                    .background(patentColor.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = patentName.uppercase(),
                    color = patentColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun RankingRowItem(
    position: Int,
    entry: LeaderboardEntry,
    isSelf: Boolean,
    selfAvatarId: String,
    selfMolduraId: String,
    getPatentInfo: (Int) -> Pair<String, Color>
) {
    val (patentName, patentColor) = getPatentInfo(entry.pontos_bw)
    val shape = RoundedCornerShape(12.dp)
    val scoreColor = when {
        position == 4 -> RkCyan
        position <= 7 -> RkCyanSoft
        else -> RkOnSurface
    }
    val avatarId = rkAvatarId(entry, isSelf, selfAvatarId)
    val molduraId = if (isSelf) selfMolduraId else "moldura_simple"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isSelf) Color(0xFF14261A) else RkSurfaceLow)
            .then(
                if (isSelf) Modifier.border(1.dp, RkLime.copy(alpha = 0.8f), shape) else Modifier
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "#$position",
                color = RkOnSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.widthIn(min = 36.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))

            AvatarWithFrame(
                avatarId = avatarId,
                molduraId = molduraId,
                size = 40.dp
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isSelf) "${entry.nickname} (VOCÊ)" else entry.nickname,
                        color = if (isSelf) RkLime else RkOnSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.SansSerif,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(patentColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = patentName.uppercase(),
                            color = patentColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = buildAnnotatedString {
                        append("VITÓRIAS: ")
                        withStyle(SpanStyle(color = RkOnSurface, fontWeight = FontWeight.Bold)) {
                            append(entry.total_vitorias.toString())
                        }
                    },
                    color = RkOnSurfaceVariant,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = formatPbw(entry.pontos_bw),
            color = scoreColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            softWrap = false
        )
    }
}