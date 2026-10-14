package com.blockwar.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blockwar.model.Cell
import com.blockwar.model.PieceType

@Composable
fun PiecePreviewBox(
    title: String,
    pieceType: PieceType?,
    tag: String,
    modifier: Modifier = Modifier,
    skinId: String = "neon"
) {
    PrepareRealisticAsset(skinId)
    val forgePhase=rememberForgePhase(skinId=="real_obsidiana"||skinId=="real_magma")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .testTag(tag)
            .border(1.dp, Color(0xFF3B494B), RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1F25))
            .padding(6.dp)
    ) {
        Text(
            text = title,
            color = Color(0xFF76EBF5),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .background(Color(0xFF15161B), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (pieceType != null) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val shape = pieceType.getShape(0)
                    val rows = shape.size
                    val cols = shape[0].size
                    val blockSize = 14.5.dp.toPx()

                    val totalWidth = cols * blockSize
                    val totalHeight = rows * blockSize
                    val startX = (size.width - totalWidth) / 2f
                    val startY = (size.height - totalHeight) / 2f

                    val cell = Cell.fromPiece(pieceType)

                    for (r in 0 until rows) {
                        for (c in 0 until cols) {
                            if (shape[r][c] != 0) {
                                PixelBlockDrawer.drawPixelBlock(
                                    drawScope = this,
                                    topLeft = Offset(startX + c * blockSize, startY + r * blockSize),
                                    blockSize = blockSize,
                                    cell = cell,
                                    skinId = skinId, firePhase = forgePhase
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
