package com.blockwar.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AvatarWithFrame(
    avatarId: String,
    molduraId: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    if (molduraId in setOf("moldura_chamas_infernais", "moldura_teia_de_aranha_neon", "moldura_correntes_de_ferro", "moldura_gelo_congelante", "moldura_ouro_cravejado")) {
        Box(modifier.size(size), contentAlignment = Alignment.Center) {
            AvatarImage(avatarId, Modifier.padding(size * .12f).fillMaxSize())
            Canvas(Modifier.fillMaxSize()) { with(CosmeticArt) { frame(molduraId) } }
        }
        return
    }
    val infiniteTransition = rememberInfiniteTransition(label = "MolduraTransition")

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val rotateAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotateAngle"
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp)), // Apply clipping here for square with rounded corners
        contentAlignment = Alignment.Center
    ) {
        // 1. Draw the Moldura Background effects
        when (molduraId) {
            "moldura_neon" -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = this.size.width
                    val h = this.size.height
                    val stroke = w * 0.08f * pulse
                    drawRoundRect(
                        brush = Brush.sweepGradient(listOf(Color(0xFF00F0FF), Color(0xFFBD00FF), Color(0xFF00F0FF))),
                        size = androidx.compose.ui.geometry.Size(w - stroke, h - stroke),
                        topLeft = Offset(stroke / 2, stroke / 2),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f),
                        style = Stroke(width = stroke)
                    )
                }
            }
            "moldura_plasma" -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = this.size.width
                    val h = this.size.height
                    val stroke = w * 0.10f
                    drawRoundRect(
                        brush = Brush.sweepGradient(
                            listOf(Color(0xFFFF9100), Color(0xFF00E5FF), Color(0xFFFF3D00), Color(0xFFFF9100)),
                            center = Offset(w / 2, h / 2)
                        ),
                        size = androidx.compose.ui.geometry.Size(w - stroke, h - stroke),
                        topLeft = Offset(stroke / 2, stroke / 2),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f),
                        style = Stroke(width = stroke * pulse)
                    )
                }
            }
            "moldura_cyber_gold" -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = this.size.width
                    val h = this.size.height
                    val stroke = w * 0.08f
                    
                    // Main golden ring (rounded)
                    drawRoundRect(
                        brush = Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA000), Color(0xFFFFD700))),
                        size = androidx.compose.ui.geometry.Size(w - stroke, h - stroke),
                        topLeft = Offset(stroke / 2, stroke / 2),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f),
                        style = Stroke(width = stroke)
                    )
                    // Metallic inner ring
                    drawRoundRect(
                        color = Color(0xFFFFF9C4).copy(alpha = 0.7f),
                        size = androidx.compose.ui.geometry.Size(w - (stroke * 2) - 3f, h - (stroke * 2) - 3f),
                        topLeft = Offset(stroke + 1.5f, stroke + 1.5f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f),
                        style = Stroke(width = 1.5f)
                    )
                }
            }
            else -> {
                // "moldura_simple" or fallback
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = this.size.width
                    val h = this.size.height
                    val stroke = w * 0.05f
                    drawRoundRect(
                        color = Color(0xFF8E99C7),
                        size = androidx.compose.ui.geometry.Size(w - stroke, h - stroke),
                        topLeft = Offset(stroke / 2, stroke / 2),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f),
                        style = Stroke(width = stroke)
                    )
                }
            }
        }

        // 2. Inner Avatar
        val paddingAmt = when (molduraId) {
            "moldura_neon" -> size * 0.08f
            "moldura_plasma" -> size * 0.09f
            "moldura_cyber_gold" -> size * 0.08f
            else -> size * 0.05f
        }

        AvatarImage(
            avatarId = avatarId,
            modifier = Modifier
                .padding(paddingAmt)
                .fillMaxSize()
        )
    }
}
