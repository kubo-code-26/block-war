package com.blockwar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Leaderboard
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.blockwar.model.AggregatedStats
import com.blockwar.model.MatchRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MatchHistoryDialog(
    stats: AggregatedStats,
    history: List<MatchRecord>,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = rememberDateFormat()
    var selectedMatch by remember { mutableStateOf<MatchRecord?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .border(2.dp, Color(0xFF00E5FF), RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0F1224))
                .padding(16.dp)
                .testTag("match_history_dialog")
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Leaderboard,
                            contentDescription = "Histórico",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "HISTÓRICO DE DESEMPENHO",
                            color = Color(0xFF00E5FF),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Aggregate Summary Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF232C52), RoundedCornerShape(10.dp))
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF141933))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                StatBadge("VITÓRIAS", "${stats.totalWins} / ${stats.totalMatches} (${stats.winRatePercent}%)", Color(0xFF00E676))
                                StatBadge("RECORDE", "${stats.highScore} BW", Color(0xFFFFD600))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                StatBadge("LINHAS LIMPAS", "${stats.totalLinesCleared}", Color(0xFF00E5FF))
                                StatBadge("ATAQUES SENT", "${stats.totalAttacksSent} BLOS", Color(0xFFFF1744))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // History List
                if (history.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "NENHUMA PARTIDA REGISTRADA AINDA.\nJOGUE PARA VER SUAS ESTATÍSTICAS!",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(history) { record ->
                            MatchRecordCard(
                                record = record,
                                dateFormat = dateFormat,
                                onClick = { selectedMatch = record }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (history.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .border(1.dp, Color(0xFFFF1744), RoundedCornerShape(8.dp))
                                .background(Color(0xFF381216), RoundedCornerShape(8.dp))
                                .clickable { onClearHistory() }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Limpar",
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "LIMPAR",
                                    color = Color(0xFFFF5252),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1.5f)
                            .height(44.dp)
                            .background(Color(0xFF00E5FF), RoundedCornerShape(8.dp))
                            .clickable { onDismiss() }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "FECHAR",
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

    if (selectedMatch != null) {
        MatchDetailsDialog(
            record = selectedMatch!!,
            dateFormat = dateFormat,
            onDismiss = { selectedMatch = null }
        )
    }
}

@Composable
fun MatchDetailsDialog(
    record: MatchRecord,
    dateFormat: SimpleDateFormat,
    onDismiss: () -> Unit
) {
    val resultColor = if (record.isVictory) Color(0xFF00E676) else Color(0xFFFF1744)
    val resultText = if (record.isVictory) "VITÓRIA" else "DERROTA"
    val dateStr = dateFormat.format(Date(record.timestamp))
    val durationFormatted = formatSeconds(record.durationSeconds)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .border(2.dp, resultColor, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0F1224))
                .padding(20.dp)
                .testTag("match_details_dialog")
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Badge
                Box(
                    modifier = Modifier
                        .background(resultColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = resultText,
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = "DETALHES DA PARTIDA",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = dateStr,
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                // Detail Items List
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF232C52), RoundedCornerShape(10.dp))
                        .background(Color(0xFF141933))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DetailRow(label = "Modo de jogo:", value = record.gameMode.ifEmpty { "VERSUS" }, valueColor = Color(0xFF00E5FF))
                    DetailRow(label = "BW da partida:", value = "${record.score} BW", valueColor = Color(0xFFFFD600))
                    DetailRow(label = "Maior Combo:", value = "x${record.maxCombo}", valueColor = Color(0xFFE040FB))
                    DetailRow(label = "Linhas Limpas:", value = "${record.linesCleared}", valueColor = Color(0xFF00E5FF))
                    DetailRow(label = "Blocos de Ataque:", value = "${record.attacksSent} blocos", valueColor = Color(0xFFFF1744))
                    DetailRow(label = "Duração total:", value = "$durationFormatted (${record.durationSeconds}s)", valueColor = Color.White)
                }

                // Close Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(Color(0xFF00E5FF), RoundedCornerShape(8.dp))
                        .clickable { onDismiss() }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "FECHAR",
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

@Composable
private fun DetailRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(0xFF8088AD),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun StatBadge(label: String, value: String, color: Color) {
    Column {
        Text(
            text = label,
            color = Color(0xFF8088AD),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun MatchRecordCard(
    record: MatchRecord,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit
) {
    val resultColor = if (record.isVictory) Color(0xFF00E676) else Color(0xFFFF1744)
    val resultText = if (record.isVictory) "VITÓRIA" else "DERROTA"
    val durationFormatted = formatSeconds(record.durationSeconds)
    val dateStr = dateFormat.format(Date(record.timestamp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, resultColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF13172B))
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(resultColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = resultText,
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = record.gameMode.ifEmpty { "VERSUS" },
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = dateStr,
                    color = Color.Gray,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Linhas: ${record.linesCleared} • Ataques: ${record.attacksSent}B • Combo: ${record.maxCombo}x",
                    color = Color(0xFFB0BEC5),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "⏱️ $durationFormatted  |  ${record.score} BW",
                    color = Color(0xFFFFD600),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun rememberDateFormat(): SimpleDateFormat {
    return remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }
}

private fun formatSeconds(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", m, s)
}
