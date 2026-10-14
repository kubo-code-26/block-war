package com.blockwar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.blockwar.model.MatchStatus
import com.blockwar.model.VersusGameState
import java.util.Locale

@Composable
fun GameOverDialog(
    gameState: VersusGameState,
    matchDurationSeconds: Long,
    totalAttacksSent: Int,
    onRestart: () -> Unit,
    onQuickMatch: () -> Unit = {},
    onOpenHistory: () -> Unit,
    onMenu: () -> Unit
) {
    if (gameState.matchStatus != MatchStatus.VICTORY && gameState.matchStatus != MatchStatus.DEFEAT) {
        return
    }

    val isVictory = gameState.matchStatus == MatchStatus.VICTORY
    val themeColor = if (isVictory) Color(0xFFFFD600) else Color(0xFFFF1744)
    val titleText = if (isVictory) "VITÓRIA! K.O." else "DERROTA!"

    val formattedDuration = formatDuration(matchDurationSeconds)

    Dialog(
        onDismissRequest = { /* non-dismissible without button */ },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = true)
    ) {
        Box(
            modifier = Modifier
                .testTag("game_over_dialog")
                .shadow(24.dp, RoundedCornerShape(16.dp), spotColor = themeColor)
                .border(
                    width = 3.dp,
                    brush = Brush.verticalGradient(
                        listOf(themeColor, Color.White, themeColor)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0F111E))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isVictory) Icons.Default.EmojiEvents else Icons.Default.HeartBroken,
                    contentDescription = null,
                    tint = themeColor,
                    modifier = Modifier.size(52.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = titleText,
                    color = themeColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Detailed Post-Game Stats Table
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF16182B), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatRow(label = "Tempo", value = formattedDuration)
                        StatRow(label = "Linhas finalizadas", value = "${gameState.player.linesCleared}")
                        StatRow(label = "Ataques", value = "$totalAttacksSent blocos")
                        StatRow(label = "Combos", value = if (gameState.player.maxCombo >= 2) "${gameState.player.maxCombo}x" else "0")
                        StatRow(label = "Pontos de partida", value = "${gameState.player.score} BWs")
                    }
                }

                if (gameState.rewardSaveMessage.isNotBlank()) {
                    Text(gameState.rewardSaveMessage, color = Color(0xFFB0B7D0), fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                }
                if (gameState.onlineConnectionMessage.isNotBlank()) {
                    Text(gameState.onlineConnectionMessage, color = Color(0xFFB0B7D0), fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Play Again / Quick Match Button
                    val isOnline = gameState.gameMode == com.blockwar.model.GameMode.ONLINE_1V1
                    Button(
                        onClick = if (isOnline) onQuickMatch else onRestart,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("restart_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = null,
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = if (isOnline) "NOVA BUSCA" else "JOGAR NOVAMENTE",
                            color = Color.Black,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Open History Button
                    OutlinedButton(
                        onClick = onOpenHistory,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("history_button"),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD600)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Leaderboard,
                            contentDescription = null,
                            tint = Color(0xFFFFD600),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = "VER HISTÓRICO LOCAL",
                            color = Color(0xFFFFD600),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Main Menu Button
                    OutlinedButton(
                        onClick = onMenu,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("menu_button"),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = "MENU PRINCIPAL",
                            color = Color(0xFF00E5FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(0xFF9098BA),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
    }
}

private fun formatDuration(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", m, s)
}
