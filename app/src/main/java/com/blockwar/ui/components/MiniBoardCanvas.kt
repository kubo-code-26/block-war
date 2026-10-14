package com.blockwar.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blockwar.model.Cell
import com.blockwar.model.PlayerState

@Composable
fun MiniBoardCanvas(
    opponentState: PlayerState,
    modifier: Modifier = Modifier,
    skinId: String = "neon",
    boardThemeId: String = "tab_neon"
) {
    PrepareRealisticAsset(skinId)
    val forgePhase=rememberForgePhase(boardThemeId=="real_fundicao"||skinId=="real_obsidiana"||skinId=="real_magma")
    PrepareRealisticAsset(boardThemeId)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val backgroundColor = when (boardThemeId) {
        "tab_matrix" -> Color(0xFF001508)
        "tab_holo" -> Color(0xFF1A0A2A)
        "tab_glass" -> Color(0xFF050505)
        else -> Color(0xFF09060E)
    }

    Column(
        modifier = modifier
            .testTag("mini_board_wrapper")
            .border(1.dp, Color(0xFF634379), RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF18131D))
            .padding(4.dp)
    ) {
        // Opponent header banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCF5CFF).copy(alpha = pulseAlpha))
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "RIVAL",
                color = Color(0xFFECB2FF),
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${opponentState.linesCleared}L",
                color = Color(0xFFFFD600),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Mini 12x20 Grid
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(PlayerState.BOARD_COLS.toFloat() / PlayerState.BOARD_ROWS)
                .background(backgroundColor)
                .clip(RoundedCornerShape(4.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize().testTag("opponent_mini_canvas")) {
                if (RealisticAssets.resource(boardThemeId)!=null || boardThemeId.startsWith("tabuleiro_") && boardThemeId != "tabuleiro_padrao") { with(CosmeticArt) { scene(boardThemeId,phase=forgePhase, board = true) } }
                val blockSize = size.width / PlayerState.BOARD_COLS
                val boardWidth = size.width
                val boardHeight = size.height

                // Grid lines (faint)
                val gridColor = when (boardThemeId) {
                    "tab_matrix" -> Color(0xFF00E676).copy(alpha = 0.1f)
                    "tab_holo" -> Color(0xFFE040FB).copy(alpha = 0.1f)
                    "tab_glass" -> Color.Transparent
                    else -> Color(0xFF1F1226)
                }
                if (boardThemeId != "tab_glass" && !ArenaBoardArt.supports(boardThemeId)) {
                    for (c in 1 until PlayerState.BOARD_COLS) {
                        val x = c * blockSize
                        drawLine(
                            color = gridColor,
                            start = Offset(x, 0f),
                            end = Offset(x, boardHeight),
                            strokeWidth = 0.5f
                        )
                    }
                }

                // Locked cells & Line Clear Collapse Animation
                val now = System.currentTimeMillis()
                val elapsed = now - opponentState.lastClearTime

                for (r in 0 until PlayerState.BOARD_ROWS) {
                    val isRowClearing = opponentState.isClearingLines && opponentState.clearingRows.contains(r)

                    if (isRowClearing && elapsed in 0L..200L) {
                        val scaleY = if (elapsed > 100L) {
                            (1.0f - (elapsed - 100f) / 100f).coerceIn(0f, 1f)
                        } else 1.0f

                        if (scaleY > 0f) {
                            for (c in 0 until PlayerState.BOARD_COLS) {
                                val cell = opponentState.board[r][c]
                                if (cell != null) {
                                    withTransform({
                                        scale(
                                            scaleX = 1.0f,
                                            scaleY = scaleY,
                                            pivot = Offset(c * blockSize + blockSize / 2f, r * blockSize + blockSize / 2f)
                                        )
                                    }) {
                                        PixelBlockDrawer.drawPixelBlock(
                                            drawScope = this,
                                            topLeft = Offset(c * blockSize, r * blockSize),
                                            blockSize = blockSize,
                                            cell = cell,
                                            skinId = skinId, firePhase = forgePhase,
                                            alpha = 1.0f
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        for (c in 0 until PlayerState.BOARD_COLS) {
                            val cell = opponentState.board[r][c]
                            if (cell != null) {
                                PixelBlockDrawer.drawPixelBlock(
                                    drawScope = this,
                                    topLeft = Offset(c * blockSize, r * blockSize),
                                    blockSize = blockSize,
                                    cell = cell,
                                    skinId = skinId, firePhase = forgePhase,
                                    alpha = 1.0f
                                )
                            }
                        }
                    }
                }

                // Line clear flash & collapse effect for opponent (200ms total)
                if (opponentState.clearingRows.isNotEmpty() && elapsed in 0L..200L) {
                    for (r in opponentState.clearingRows) {
                        if (elapsed <= 100L) {
                            val flashAlpha = (0.95f - (elapsed / 100f) * 0.15f).coerceIn(0.7f, 0.95f)
                            drawRect(
                                color = Color(0xFF00E5FF).copy(alpha = flashAlpha),
                                topLeft = Offset(0f, r * blockSize),
                                size = Size(boardWidth, blockSize)
                            )
                        } else {
                            val phase2Progress = ((elapsed - 100f) / 100f).coerceIn(0f, 1f)
                            val scaleY = 1.0f - phase2Progress
                            val height = blockSize * scaleY
                            val topY = r * blockSize + (blockSize - height) / 2f

                            if (height > 0f) {
                                drawRect(
                                    color = Color(0xFF00E5FF).copy(alpha = scaleY * 0.85f),
                                    topLeft = Offset(0f, topY),
                                    size = Size(boardWidth, height)
                                )
                            }
                        }
                    }
                }

                // Falling 1x2 and 1x3 garbage pieces on opponent board
                for (fp in opponentState.fallingGarbagePieces) {
                    if (fp.spawnDelayTicks == 0 && fp.y in 0 until PlayerState.BOARD_ROWS) {
                        val endCol = (fp.startCol + fp.size).coerceAtMost(PlayerState.BOARD_COLS)
                        for (c in fp.startCol until endCol) {
                            PixelBlockDrawer.drawPixelBlock(
                                drawScope = this,
                                topLeft = Offset(c * blockSize, fp.y * blockSize),
                                blockSize = blockSize,
                                cell = Cell.GARBAGE,
                                skinId = skinId, firePhase = forgePhase
                            )
                        }
                    }
                }

                // Opponent active piece
                val current = opponentState.currentPiece
                if (current != null) {
                    val activeCell = Cell.fromPiece(current.type)
                    for ((cx, cy) in current.getOccupiedCoordinates()) {
                        if (cy in 0 until PlayerState.BOARD_ROWS && cx in 0 until PlayerState.BOARD_COLS) {
                            PixelBlockDrawer.drawPixelBlock(
                                drawScope = this,
                                topLeft = Offset(cx * blockSize, cy * blockSize),
                                blockSize = blockSize,
                                cell = activeCell,
                                skinId = skinId, firePhase = forgePhase
                            )
                        }
                    }
                }

                // Opponent pending garbage
                if (opponentState.pendingGarbage > 0) {
                    val meterWidth = 3f
                    val garbageHeight = (opponentState.pendingGarbage * (blockSize / 5f)).coerceAtMost(boardHeight)
                    drawRect(
                        color = Color(0xFFFF5252).copy(alpha = pulseAlpha),
                        topLeft = Offset(0f, boardHeight - garbageHeight),
                        size = Size(meterWidth, garbageHeight)
                    )
                }
            }
        }
    }
}
