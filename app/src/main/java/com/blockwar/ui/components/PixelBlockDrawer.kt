package com.blockwar.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.blockwar.model.Cell

object PixelBlockDrawer {

    /**
     * Draws an authentic retro pixel arcade block with skins:
     * - "neon": Vibrant Cyan/Blue glow with 3D bevels and arcade colors
     * - "cyber": Magenta / Purple Neon gradient border with dark core and glowing center
     * - "gold": Shiny metallic Gold/Amber gradient with gleaming highlights
     */
    fun drawPixelBlock(
        drawScope: DrawScope,
        topLeft: Offset,
        blockSize: Float,
        cell: Cell,
        alpha: Float = 1.0f,
        skinId: String = "neon"
    ) {
        if (skinId.startsWith("skin_") && skinId != "skin_padrao") {
            with(drawScope) {
                val c = if (cell.isGarbage) Color(0xFF777777) else CosmeticArt.accent(skinId)
                val s = blockSize
                drawRect(c.copy(alpha = alpha), topLeft, Size(s, s))
                drawRect(Color.Black.copy(alpha = .45f * alpha), topLeft + Offset(s*.12f,s*.12f), Size(s*.76f,s*.76f))
                when (skinId) {
                    "skin_pedra_vulcanica" -> for (i in 1..3) drawLine(c.copy(alpha=alpha), topLeft+Offset(s*i/4,0f), topLeft+Offset(s*(4-i)/4,s), s*.045f)
                    "skin_madeira_rustica" -> for (i in 1..4) drawLine(c.copy(alpha=.7f*alpha), topLeft+Offset(s*.15f,s*i/5), topLeft+Offset(s*.85f,s*i/5), s*.035f)
                    "skin_gelo_cristalino" -> { drawLine(Color.White.copy(alpha=.8f*alpha),topLeft+Offset(s*.2f,s*.15f),topLeft+Offset(s*.8f,s*.8f),s*.055f); drawLine(c.copy(alpha=alpha),topLeft+Offset(s*.2f,s*.8f),topLeft+Offset(s*.8f,s*.2f),s*.04f) }
                    "skin_ouro_macico" -> { drawRect(Brush.linearGradient(listOf(c,Color(0xFFB87718),Color(0xFFFFEAB0))),topLeft+Offset(s*.12f,s*.12f),Size(s*.76f,s*.76f),alpha=alpha); drawLine(Color.White.copy(alpha=.8f*alpha),topLeft+Offset(s*.2f,s*.25f),topLeft+Offset(s*.65f,s*.25f),s*.06f) }
                    else -> { drawRect(c.copy(alpha=alpha),topLeft+Offset(s*.22f,s*.22f),Size(s*.56f,s*.56f),style=Stroke(s*.045f)); drawCircle(Color.White.copy(alpha=.8f*alpha),s*.08f,topLeft+Offset(s*.35f,s*.35f)) }
                }
                if(cell.isGarbage) drawRect(Color(0xFF999999).copy(alpha=alpha),topLeft+Offset(s*.3f,s*.3f),Size(s*.4f,s*.4f))
            }
            return
        }
        val bevel = blockSize * 0.16f

        when (skinId) {
            "gold" -> {
                // Gold Skin: Metallic Gold Gradient (#FFF59D / #FFD600 / #FFA000)
                if (cell.isGarbage) {
                    // Antique bullion / dark gold metal for garbage
                    val garbageGoldBrush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFD7CCC8).copy(alpha = alpha),
                            Color(0xFF8D6E63).copy(alpha = alpha),
                            Color(0xFF5D4037).copy(alpha = alpha)
                        ),
                        startY = topLeft.y,
                        endY = topLeft.y + blockSize
                    )
                    drawScope.drawRect(
                        brush = garbageGoldBrush,
                        topLeft = topLeft,
                        size = Size(blockSize, blockSize)
                    )
                    // Bevels
                    drawScope.drawRect(
                        color = Color(0xFFFFD54F).copy(alpha = 0.6f * alpha),
                        topLeft = topLeft,
                        size = Size(blockSize, bevel)
                    )
                    drawScope.drawRect(
                        color = Color(0xFF3E2723).copy(alpha = 0.8f * alpha),
                        topLeft = Offset(topLeft.x, topLeft.y + blockSize - bevel),
                        size = Size(blockSize, bevel)
                    )
                } else {
                    val goldBrush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFF9C4).copy(alpha = alpha),
                            Color(0xFFFFD600).copy(alpha = alpha),
                            Color(0xFFFFA000).copy(alpha = alpha),
                            Color(0xFFFF8F00).copy(alpha = alpha)
                        ),
                        startY = topLeft.y,
                        endY = topLeft.y + blockSize
                    )
                    drawScope.drawRect(
                        brush = goldBrush,
                        topLeft = topLeft,
                        size = Size(blockSize, blockSize)
                    )

                    // Metallic top highlight
                    drawScope.drawRect(
                        color = Color(0xFFFFFFFF).copy(alpha = 0.9f * alpha),
                        topLeft = topLeft,
                        size = Size(blockSize, bevel)
                    )
                    // Left highlight
                    drawScope.drawRect(
                        color = Color(0xFFFFF59D).copy(alpha = 0.85f * alpha),
                        topLeft = topLeft,
                        size = Size(bevel, blockSize)
                    )
                    // Bottom shadow
                    drawScope.drawRect(
                        color = Color(0xFFE65100).copy(alpha = 0.9f * alpha),
                        topLeft = Offset(topLeft.x, topLeft.y + blockSize - bevel),
                        size = Size(blockSize, bevel)
                    )
                    // Right shadow
                    drawScope.drawRect(
                        color = Color(0xFFFF6F00).copy(alpha = 0.85f * alpha),
                        topLeft = Offset(topLeft.x + blockSize - bevel, topLeft.y),
                        size = Size(bevel, blockSize)
                    )

                    // Gleaming gold corner glint
                    val innerMargin = bevel * 1.3f
                    val innerSize = blockSize * 0.18f
                    drawScope.drawRect(
                        color = Color.White.copy(alpha = 0.95f * alpha),
                        topLeft = Offset(topLeft.x + innerMargin, topLeft.y + innerMargin),
                        size = Size(innerSize, innerSize)
                    )
                }

                // Metallic Golden Border
                drawScope.drawRect(
                    color = Color(0xFFFFD54F).copy(alpha = 0.9f * alpha),
                    topLeft = topLeft,
                    size = Size(blockSize, blockSize),
                    style = Stroke(width = 1.5f)
                )
            }
            "cyber" -> {
                // Cyberpunk Glow: Magenta / Purple Neon gradient border (#E040FB / #7C4DFF) with dark center
                val darkCoreColor = if (cell.isGarbage) Color(0xFF090514).copy(alpha = alpha) else Color(0xFF150A26).copy(alpha = alpha)
                drawScope.drawRect(
                    color = darkCoreColor,
                    topLeft = topLeft,
                    size = Size(blockSize, blockSize)
                )

                // Neon inner core glow
                val glowColor = if (cell.isGarbage) Color(0xFF9C27B0).copy(alpha = 0.4f * alpha) else Color(0xFFE040FB).copy(alpha = 0.55f * alpha)
                val coreMargin = bevel * 1.2f
                val coreSize = (blockSize - (coreMargin * 2f)).coerceAtLeast(1f)
                drawScope.drawRect(
                    color = glowColor,
                    topLeft = Offset(topLeft.x + coreMargin, topLeft.y + coreMargin),
                    size = Size(coreSize, coreSize)
                )

                // Top & Left Neon Magenta Bevel
                val magentaBevel = Color(0xFFE040FB).copy(alpha = 0.95f * alpha)
                drawScope.drawRect(
                    color = magentaBevel,
                    topLeft = topLeft,
                    size = Size(blockSize, bevel)
                )
                drawScope.drawRect(
                    color = magentaBevel,
                    topLeft = topLeft,
                    size = Size(bevel, blockSize)
                )

                // Bottom & Right Deep Violet Shadow Bevel
                val purpleBevel = Color(0xFF7C4DFF).copy(alpha = 0.95f * alpha)
                drawScope.drawRect(
                    color = purpleBevel,
                    topLeft = Offset(topLeft.x, topLeft.y + blockSize - bevel),
                    size = Size(blockSize, bevel)
                )
                drawScope.drawRect(
                    color = purpleBevel,
                    topLeft = Offset(topLeft.x + blockSize - bevel, topLeft.y),
                    size = Size(bevel, blockSize)
                )

                // Cyberpunk bright pixel sparkle
                val sparkMargin = bevel * 1.5f
                val sparkSize = blockSize * 0.15f
                drawScope.drawRect(
                    color = Color(0xFFFF80AB).copy(alpha = 0.95f * alpha),
                    topLeft = Offset(topLeft.x + sparkMargin, topLeft.y + sparkMargin),
                    size = Size(sparkSize, sparkSize)
                )

                // Outer Cyber Neon Border Gradient
                val cyberBorderBrush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFE040FB).copy(alpha = alpha),
                        Color(0xFF7C4DFF).copy(alpha = alpha)
                    ),
                    start = topLeft,
                    end = Offset(topLeft.x + blockSize, topLeft.y + blockSize)
                )
                drawScope.drawRect(
                    brush = cyberBorderBrush,
                    topLeft = topLeft,
                    size = Size(blockSize, blockSize),
                    style = Stroke(width = 1.5f)
                )
            }
            "gb" -> {
                // Game Boy Classic: Shades of Green/Mint
                val color = Color(0xFF8BAC0F).copy(alpha = alpha)
                val highlight = Color(0xFF9BBC0F).copy(alpha = alpha)
                val shadow = Color(0xFF306230).copy(alpha = alpha)

                drawScope.drawRect(color = color, topLeft = topLeft, size = Size(blockSize, blockSize))
                drawScope.drawRect(color = highlight, topLeft = topLeft, size = Size(blockSize, bevel))
                drawScope.drawRect(color = highlight, topLeft = topLeft, size = Size(bevel, blockSize))
                drawScope.drawRect(color = shadow, topLeft = Offset(topLeft.x, topLeft.y + blockSize - bevel), size = Size(blockSize, bevel))
                drawScope.drawRect(color = shadow, topLeft = Offset(topLeft.x + blockSize - bevel, topLeft.y), size = Size(bevel, blockSize))
                drawScope.drawRect(color = Color(0xFF1B381B), topLeft = topLeft, size = Size(blockSize, blockSize), style = Stroke(width = 1f))
            }
            "fireice" -> {
                val color = if (cell.color.red > cell.color.blue) Color(0xFFFF5722).copy(alpha = alpha) else Color(0xFF03A9F4).copy(alpha = alpha)
                val highlight = Color.White.copy(alpha = 0.8f * alpha)
                val shadow = Color.Black.copy(alpha = 0.5f * alpha)

                drawScope.drawRect(color = color, topLeft = topLeft, size = Size(blockSize, blockSize))
                drawScope.drawRect(color = highlight, topLeft = topLeft, size = Size(blockSize, bevel))
                drawScope.drawRect(color = highlight, topLeft = topLeft, size = Size(bevel, blockSize))
                drawScope.drawRect(color = shadow, topLeft = Offset(topLeft.x, topLeft.y + blockSize - bevel), size = Size(blockSize, bevel))
                drawScope.drawRect(color = shadow, topLeft = Offset(topLeft.x + blockSize - bevel, topLeft.y), size = Size(bevel, blockSize))
                drawScope.drawRect(color = Color(0x99000000), topLeft = topLeft, size = Size(blockSize, blockSize), style = Stroke(width = 1f))
            }
            else -> {
                // "neon" - Default Arcade Cyan/Blue Glow with 3D Bevels
                val color = cell.color.copy(alpha = cell.color.alpha * alpha)
                val highlight = cell.highlightColor.copy(alpha = cell.highlightColor.alpha * alpha)
                val shadow = cell.shadowColor.copy(alpha = cell.shadowColor.alpha * alpha)

                // Base fill
                drawScope.drawRect(
                    color = color,
                    topLeft = topLeft,
                    size = Size(blockSize, blockSize)
                )

                // Top highlight
                drawScope.drawRect(
                    color = highlight,
                    topLeft = topLeft,
                    size = Size(blockSize, bevel)
                )

                // Left highlight
                drawScope.drawRect(
                    color = highlight,
                    topLeft = topLeft,
                    size = Size(bevel, blockSize)
                )

                // Bottom shadow
                drawScope.drawRect(
                    color = shadow,
                    topLeft = Offset(topLeft.x, topLeft.y + blockSize - bevel),
                    size = Size(blockSize, bevel)
                )

                // Right shadow
                drawScope.drawRect(
                    color = shadow,
                    topLeft = Offset(topLeft.x + blockSize - bevel, topLeft.y),
                    size = Size(bevel, blockSize)
                )

                // Inner pixel dot highlight
                val innerMargin = bevel * 1.5f
                val innerSize = blockSize * 0.16f
                drawScope.drawRect(
                    color = Color.White.copy(alpha = 0.6f * alpha),
                    topLeft = Offset(topLeft.x + innerMargin, topLeft.y + innerMargin),
                    size = Size(innerSize, innerSize)
                )

                // Crisp Cyan-tinted outer stroke
                val strokeColor = if (cell.isGarbage) Color(0x99000000) else Color(0x6600E5FF)
                drawScope.drawRect(
                    color = strokeColor,
                    topLeft = topLeft,
                    size = Size(blockSize, blockSize),
                    style = Stroke(width = 1f)
                )
            }
        }

        // Garbage center rivet pattern
        if (cell.isGarbage && skinId != "gold") {
            val center = Offset(topLeft.x + blockSize / 2f, topLeft.y + blockSize / 2f)
            val rivetSize = blockSize * 0.14f
            drawScope.drawRect(
                color = Color.Black.copy(alpha = 0.55f * alpha),
                topLeft = Offset(center.x - rivetSize / 2f, center.y - rivetSize / 2f),
                size = Size(rivetSize, rivetSize)
            )
        }
    }

    /**
     * Draws a ghost block outline
     */
    fun drawGhostBlock(
        drawScope: DrawScope,
        topLeft: Offset,
        blockSize: Float,
        color: Color
    ) {
        val strokeWidth = 2.5f
        drawScope.drawRect(
            color = color.copy(alpha = 0.22f),
            topLeft = topLeft,
            size = Size(blockSize, blockSize)
        )
        drawScope.drawRect(
            color = color.copy(alpha = 0.65f),
            topLeft = topLeft,
            size = Size(blockSize, blockSize),
            style = Stroke(width = strokeWidth)
        )
    }
}
