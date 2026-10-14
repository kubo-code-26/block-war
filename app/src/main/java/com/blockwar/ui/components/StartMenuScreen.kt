package com.blockwar.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.blockwar.model.Cell
import com.blockwar.model.PieceType
import com.blockwar.model.VersusGameState

@Composable
fun StartMenuScreen(
    gameState: VersusGameState,
    onStartVsCpu: () -> Unit,
    onStartOnlineLobby: () -> Unit,
    onConfirmOnlineMatch: () -> Unit,
    onCloseOnlineLobby: () -> Unit,
    onToggleAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "titleGlow")
    val titleGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "titleAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("start_menu_screen")
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0D0F22),
                        Color(0xFF080914),
                        Color(0xFF040509)
                    )
                )
            )
    ) {
        // Decorative Retro CRT Background Grid
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridSpacing = 24.dp.toPx()
            val gridColor = Color(0xFF151833)
            for (x in 0 until (size.width / gridSpacing).toInt()) {
                drawLine(
                    color = gridColor,
                    start = Offset(x * gridSpacing, 0f),
                    end = Offset(x * gridSpacing, size.height),
                    strokeWidth = 1f
                )
            }
            for (y in 0 until (size.height / gridSpacing).toInt()) {
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y * gridSpacing),
                    end = Offset(size.width, y * gridSpacing),
                    strokeWidth = 1f
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar: Audio Sound Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleAudio,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("menu_audio_toggle")
                        .clip(CircleShape)
                        .background(Color(0xFF161933))
                ) {
                    Icon(
                        imageVector = if (gameState.isAudioEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = "Audio Toggle",
                        tint = if (gameState.isAudioEnabled) Color(0xFFFFD600) else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Center Content: Title Marquee & Decorative Blocks
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                // Decorative Pixel Block Row (1x1, 1x2, 1x3, 1x4)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val samplePieces = listOf(
                        PieceType.LINE_1,
                        PieceType.LINE_2,
                        PieceType.LINE_3,
                        PieceType.LINE_4
                    )
                    for (p in samplePieces) {
                        Box(
                            modifier = Modifier
                                .size(width = (p.blockCount * 14).dp, height = 14.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(p.primaryColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                BlockWarLogo(Modifier.size(160.dp))
                Spacer(modifier = Modifier.height(24.dp))

                // Mode Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // BUTTON 1: [ JOGAR VS CPU ]
                    Button(
                        onClick = onStartVsCpu,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .testTag("btn_play_vs_cpu")
                            .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = Color(0xFF00E5FF)),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PrecisionManufacturing,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "JOGAR VS CPU",
                                color = Color.Black,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // BUTTON 2: [ JOGAR ONLINE 1v1 ]
                    Button(
                        onClick = onStartOnlineLobby,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .testTag("btn_play_online")
                            .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = Color(0xFFFF4081)),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4081)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "JOGAR ONLINE 1v1",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }

            // Bottom Footer
            Text(
                text = "PEÇAS RETAS: 1x1, 1x2, 1x3 e 1x4 • ATAQUE 75%",
                color = Color(0xFF6B7280),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
        }

        // Online Lobby / Matchmaking Modal Dialog
        if (gameState.isSearchingOnlineOpponent) {
            Dialog(
                onDismissRequest = onCloseOnlineLobby,
                properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = true)
            ) {
                Box(
                    modifier = Modifier
                        .testTag("online_lobby_modal")
                        .shadow(20.dp, RoundedCornerShape(16.dp), spotColor = Color(0xFFFF4081))
                        .border(
                            width = 2.dp,
                            brush = Brush.verticalGradient(
                                listOf(Color(0xFFFF4081), Color(0xFF7C4DFF))
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0F1122))
                        .padding(20.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LOBBY ONLINE 1v1",
                                color = Color(0xFFFF4081),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                            IconButton(onClick = onCloseOnlineLobby) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Fechar",
                                    tint = Color.Gray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(48.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "SALA: ${gameState.onlineRoomCode}",
                            color = Color(0xFFFFD600),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Buscando oponente em rede...",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = onConfirmOnlineMatch,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_confirm_online_match"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "INICIAR CONEXÃO 1v1",
                                color = Color.Black,
                                fontSize = 12.sp,
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
