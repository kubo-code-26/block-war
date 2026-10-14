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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.blockwar.model.Cell
import com.blockwar.model.PlayerState

@Composable
fun BoardCanvas(
    playerState: PlayerState,
    particleController: ParticleController,
    modifier: Modifier = Modifier,
    skinId: String = "neon",
    vfxId: String = "sparks",
    animId: String = "anim_flash",
    boardThemeId: String = "tab_neon"
) {
    val infiniteTransition = rememberInfiniteTransition(label = "warningGlow")
    var boardWidthPx by remember { mutableStateOf(360f) }

    // Fast ticker (16ms) to drive 60 FPS smooth line clear animation without blocking main thread
    val animTick by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(16),
            repeatMode = RepeatMode.Restart
        ),
        label = "animTick"
    )

    val warningAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    // Trigger destruction particles when receiving attack blocks
    LaunchedEffect(playerState.fallingGarbagePieces.size) {
        if (playerState.fallingGarbagePieces.isNotEmpty()) {
            val bw = if (boardWidthPx > 0f) boardWidthPx else 360f
            val bs = bw / PlayerState.BOARD_COLS
            particleController.triggerExplosion(
                centerX = bw / 2f,
                centerY = bs * 2f,
                count = 25,
                colors = listOf(Color(0xFFFF1744), Color(0xFFFF9100), Color(0xFF9E9E9E))
            )
        }
    }

    // Trigger weight-based impact flash and particles when piece locks / impacts ground
    LaunchedEffect(playerState.lastImpactTime) {
        if (playerState.lastImpactTime > 0L) {
            val bw = if (boardWidthPx > 0f) boardWidthPx else 360f
            val count = (playerState.impactIntensity * 2.5f).toInt().coerceIn(12, 60)
            particleController.triggerExplosion(
                centerX = playerState.impactColumn.coerceIn(.5f,9.5f) * bw / 10f,
                centerY = playerState.impactRow.coerceIn(.5f,19.5f) * bw / 10f,
                count = count,
                colors = listOf(Color(0xFF00E5FF), Color(0xFFFFD600), Color(0xFFFF1744), Color.White),
                vfxId = vfxId
            )
        }
    }

    val backgroundColor = when (boardThemeId) {
        "tab_matrix" -> Color(0xFF001508)
        "tab_holo" -> Color(0xFF1A0A2A)
        "tab_glass" -> Color(0xFF050505)
        else -> Color(0xFF0B1014)
    }

    Box(
        modifier = modifier
            .testTag("player_board_container")
            .border(1.dp, Color(0xFF29616A), RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .aspectRatio(10f / 20f)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { boardWidthPx = it.width.toFloat() }
                .testTag("player_board_canvas")
        ) {
            @Suppress("UNUSED_EXPRESSION")
            animTick // Subscribe to fast ticker for smooth per-frame animation

            if (boardThemeId.startsWith("tabuleiro_") && boardThemeId != "tabuleiro_padrao") { with(CosmeticArt) { scene(boardThemeId, board = true) } }
            val blockSize = size.width / PlayerState.BOARD_COLS
            val boardWidth = size.width
            val boardHeight = size.height

            // 1. Grid lines
            val gridColor = when (boardThemeId) {
                "tab_matrix" -> Color(0xFF00E676).copy(alpha = 0.3f)
                "tab_holo" -> Color(0xFFE040FB).copy(alpha = 0.3f)
                "tab_glass" -> Color.Transparent
                else -> Color(0xFF181B2C)
            }
            if (boardThemeId != "tab_glass") {
                for (c in 1 until PlayerState.BOARD_COLS) {
                    val x = c * blockSize
                    drawLine(
                        color = gridColor,
                        start = Offset(x, 0f),
                        end = Offset(x, boardHeight),
                        strokeWidth = 1f
                    )
                }
                for (r in 1 until PlayerState.BOARD_ROWS) {
                    val y = r * blockSize
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(boardWidth, y),
                        strokeWidth = 1f
                    )
                }
            }

            // 2. Locked cells & Line Clear Collapse Animation
            val now = System.currentTimeMillis()
            val elapsed = now - playerState.lastClearTime

            for (r in 0 until PlayerState.BOARD_ROWS) {
                val isRowClearing = playerState.isClearingLines && playerState.clearingRows.contains(r)

                if (isRowClearing && elapsed in 0L..200L) {
                    val progress = (elapsed / 200f).coerceIn(0f, 1f)

                    for (c in 0 until PlayerState.BOARD_COLS) {
                        val cell=playerState.board[r][c] ?: continue
                        PixelBlockDrawer.drawPixelBlock(this,Offset(c*blockSize,r*blockSize),blockSize,cell,
                            alpha=(1f-progress).coerceIn(0f,1f),skinId=skinId)
                    }

                } else {
                    for (c in 0 until PlayerState.BOARD_COLS) {
                        val cell = playerState.board[r][c]
                        if (cell != null) {
                            PixelBlockDrawer.drawPixelBlock(
                                drawScope = this,
                                topLeft = Offset(c * blockSize, r * blockSize),
                                blockSize = blockSize,
                                cell = cell,
                                skinId = skinId,
                                alpha = 1.0f
                            )
                        }
                    }
                }
            }

            // 2b. Visible falling garbage pieces
            for (fp in playerState.fallingGarbagePieces) {
                if (fp.spawnDelayTicks == 0 && fp.y in 0 until PlayerState.BOARD_ROWS) {
                    val endCol = (fp.startCol + fp.size).coerceAtMost(PlayerState.BOARD_COLS)
                    for (c in fp.startCol until endCol) {
                        PixelBlockDrawer.drawPixelBlock(
                            drawScope = this,
                            topLeft = Offset(c * blockSize, fp.y * blockSize),
                            blockSize = blockSize,
                            cell = Cell.GARBAGE,
                            skinId = skinId
                        )
                    }
                }
            }

            // 3. Ghost piece projection
            val ghost = playerState.ghostPiece
            val current = playerState.currentPiece
            if (ghost != null && current != null && ghost.y != current.y) {
                for ((gx, gy) in ghost.getOccupiedCoordinates()) {
                    if (gy in 0 until PlayerState.BOARD_ROWS && gx in 0 until PlayerState.BOARD_COLS) {
                        PixelBlockDrawer.drawGhostBlock(
                            drawScope = this,
                            topLeft = Offset(gx * blockSize, gy * blockSize),
                            blockSize = blockSize,
                            color = current.type.primaryColor
                        )
                    }
                }
            }

            // 4. Active falling piece
            if (current != null) {
                val activeCell = Cell.fromPiece(current.type)
                for ((cx, cy) in current.getOccupiedCoordinates()) {
                    if (cy in 0 until PlayerState.BOARD_ROWS && cx in 0 until PlayerState.BOARD_COLS) {
                        PixelBlockDrawer.drawPixelBlock(
                            drawScope = this,
                            topLeft = Offset(cx * blockSize, cy * blockSize),
                            blockSize = blockSize,
                            cell = activeCell,
                            skinId = skinId
                        )
                    }
                }
            }

            // 5. Ultra-fast Line Clear Flash & Collapse Visual Effects (200ms total)
            if (playerState.clearingRows.isNotEmpty() && elapsed in 0L..200L) {
                val progress = (elapsed / 200f).coerceIn(0f, 1f)
                for (r in playerState.clearingRows) {
                    val topY = r * blockSize
                    with(LineClearArt) { render(animId,topY,boardWidth,blockSize,progress) }

                }
            }

            // 6. Pending garbage danger meter on right edge
            if (playerState.pendingGarbage > 0) {
                val meterWidth = 5f
                val garbageHeight = (playerState.pendingGarbage * (blockSize / 5f)).coerceAtMost(boardHeight)
                drawRect(
                    color = Color(0xFFFF1744).copy(alpha = warningAlpha),
                    topLeft = Offset(boardWidth - meterWidth, boardHeight - garbageHeight),
                    size = Size(meterWidth, garbageHeight)
                )
            }
        }

        // Particle Overlay layer for destruction explosion sparks
        ParticleOverlay(controller = particleController)
    }
}
