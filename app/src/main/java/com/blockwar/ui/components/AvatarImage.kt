package com.blockwar.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AvatarImage(
    avatarId: String,
    modifier: Modifier = Modifier
) {
    PrepareRealisticAsset(avatarId)
    val firePhase=rememberForgePhase(avatarId=="avatar_forja")
    if (avatarId.startsWith("avatar_") && avatarId !in setOf("avatar_default", "avatar_padrao", "avatar_cyber_pilot", "avatar_pixel_ninja", "avatar_mecha_bot", "avatar_hacker")) {
        Canvas(modifier) { with(CosmeticArt) { avatar(avatarId,firePhase) } }
        return
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0F1122)),
        contentAlignment = Alignment.Center
    ) {
        when (avatarId) {
            "avatar_cyber_pilot" -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    
                    // Helmet base
                    drawRoundRect(
                        color = Color(0xFF1E2244),
                        size = Size(w * 0.9f, h * 0.9f),
                        topLeft = Offset(w * 0.05f, h * 0.05f),
                        cornerRadius = CornerRadius(w * 0.1f, w * 0.1f)
                    )
                    // Glow visor
                    drawRoundRect(
                        brush = Brush.horizontalGradient(listOf(Color(0xFF00F0FF), Color(0xFFBD00FF))),
                        topLeft = Offset(w * 0.15f, h * 0.35f),
                        size = Size(w * 0.7f, h * 0.3f),
                        cornerRadius = CornerRadius(w * 0.1f, w * 0.1f)
                    )
                    // Visor reflection
                    drawOval(
                        color = Color.White.copy(alpha = 0.6f),
                        topLeft = Offset(w * 0.25f, h * 0.38f),
                        size = Size(w * 0.2f, h * 0.08f)
                    )
                }
            }
            "avatar_pixel_ninja" -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    
                    drawRoundRect(
                        color = Color(0xFF181A22),
                        size = Size(w * 0.9f, h * 0.9f),
                        topLeft = Offset(w * 0.05f, h * 0.05f),
                        cornerRadius = CornerRadius(w * 0.1f, w * 0.1f)
                    )
                    drawRect(
                        color = Color(0xFFFFCC80),
                        topLeft = Offset(w * 0.2f, h * 0.35f),
                        size = Size(w * 0.6f, h * 0.2f)
                    )
                    drawRect(
                        color = Color(0xFFFF1744),
                        topLeft = Offset(w * 0.28f, h * 0.42f),
                        size = Size(w * 0.12f, h * 0.08f)
                    )
                    drawRect(
                        color = Color(0xFFFF1744),
                        topLeft = Offset(w * 0.6f, h * 0.42f),
                        size = Size(w * 0.12f, h * 0.08f)
                    )
                    drawLine(
                        color = Color.Black,
                        start = Offset(w * 0.15f, h * 0.35f),
                        end = Offset(w * 0.85f, h * 0.35f),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = Color.Black,
                        start = Offset(w * 0.15f, h * 0.55f),
                        end = Offset(w * 0.85f, h * 0.55f),
                        strokeWidth = 2f
                    )
                }
            }
            "avatar_mecha_bot" -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    
                    drawRoundRect(
                        color = Color(0xFF37474F),
                        size = Size(w * 0.9f, h * 0.9f),
                        topLeft = Offset(w * 0.05f, h * 0.05f),
                        cornerRadius = CornerRadius(w * 0.1f, w * 0.1f)
                    )
                    drawRect(
                        color = Color(0xFF00F0FF),
                        topLeft = Offset(w * 0.08f, h * 0.35f),
                        size = Size(w * 0.08f, h * 0.3f)
                    )
                    drawRect(
                        color = Color(0xFF00F0FF),
                        topLeft = Offset(w * 0.84f, h * 0.35f),
                        size = Size(w * 0.08f, h * 0.3f)
                    )
                    val path = Path().apply {
                        moveTo(w * 0.25f, h * 0.3f)
                        lineTo(w * 0.75f, h * 0.3f)
                        lineTo(w * 0.65f, h * 0.75f)
                        lineTo(w * 0.35f, h * 0.75f)
                        close()
                    }
                    drawPath(path = path, color = Color(0xFF1A237E))
                    
                    drawRoundRect(
                        color = Color(0xFFFFD600),
                        size = Size(w * 0.12f, w * 0.12f),
                        topLeft = Offset(w * 0.34f, h * 0.42f),
                        cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
                    )
                    drawRoundRect(
                        color = Color(0xFFFFD600),
                        size = Size(w * 0.12f, w * 0.12f),
                        topLeft = Offset(w * 0.54f, h * 0.42f),
                        cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
                    )
                }
            }
            "avatar_hacker" -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    
                    drawRoundRect(
                        color = Color(0xFF0A1C0E),
                        size = Size(w * 0.9f, h * 0.9f),
                        topLeft = Offset(w * 0.05f, h * 0.05f),
                        cornerRadius = CornerRadius(w * 0.1f, w * 0.1f)
                    )
                    val path = Path().apply {
                        moveTo(w * 0.5f, h * 0.15f)
                        cubicTo(
                            w * 0.25f, h * 0.18f,
                            w * 0.18f, h * 0.45f,
                            w * 0.22f, h * 0.85f
                        )
                        lineTo(w * 0.78f, h * 0.85f)
                        cubicTo(
                            w * 0.82f, h * 0.45f,
                            w * 0.75f, h * 0.18f,
                            w * 0.5f, h * 0.15f
                        )
                        close()
                    }
                    drawPath(path = path, color = Color(0xFF141923))
                    
                    drawRoundRect(
                        color = Color(0xFF00E676),
                        topLeft = Offset(w * 0.3f, h * 0.44f),
                        size = Size(w * 0.4f, h * 0.1f),
                        cornerRadius = CornerRadius(w * 0.02f, w * 0.02f)
                    )
                    drawLine(
                        color = Color(0xFF00E676),
                        start = Offset(w * 0.35f, h * 0.49f),
                        end = Offset(w * 0.65f, h * 0.49f),
                        strokeWidth = 2f
                    )
                }
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.SportsEsports,
                    contentDescription = "Arcade Joystick",
                    tint = Color(0xFFFFD600),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
