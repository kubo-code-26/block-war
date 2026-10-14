package com.blockwar.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class ParticleShape {
    SQUARE,
    CIRCLE_COIN,
    SPARK_STAR,
    FLAME
}

data class PixelParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    val size: Float,
    var alpha: Float = 1.0f,
    val lifeTimeMs: Long = 500L,
    var ageMs: Long = 0L,
    val shape: ParticleShape = ParticleShape.SQUARE,
    var rotation: Float = 0f,
    val rotationSpeed: Float = 0f
)

class ParticleController {
    val particles = mutableStateListOf<PixelParticle>()

    fun triggerExplosion(
        centerX: Float,
        centerY: Float,
        count: Int = 30,
        colors: List<Color> = listOf(
            Color(0xFF00E5FF),
            Color(0xFF2979FF),
            Color(0xFFFFD600)
        ),
        vfxId: String = "sparks"
    ) {
        val newParticles = mutableListOf<PixelParticle>()
        val actualCount = when (vfxId) {
            "explosion" -> count * 2
            "plasma" -> (count * 1.5f).toInt()
            "coins" -> (count * 1.2f).toInt()
            else -> count
        }

        for (i in 0 until actualCount) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            var speed = Random.nextFloat() * 12f + 4f
            var vx = cos(angle.toDouble()).toFloat() * speed
            var vy = sin(angle.toDouble()).toFloat() * speed - 2f
            var size = Random.nextFloat() * 8f + 6f
            var color = colors.random()
            var life = Random.nextLong(400L, 850L)
            var shape = ParticleShape.SQUARE
            val rotSpeed = (Random.nextFloat() - 0.5f) * 15f

            when (vfxId) {
                "efeito_explosao_plasma" -> { color=Color(0xFFCF6AFF); shape=ParticleShape.FLAME; size*=1.6f; vy=-kotlin.math.abs(vy) }
                "efeito_chuva_de_confetes" -> { color=listOf(Color(0xFFFF73BE),Color(0xFF66EABB),Color(0xFFFFD760),Color(0xFF75ADFF))[i%4]; size*=.7f; vy-=8f }
                "efeito_impacto_terrestre" -> { color=listOf(Color(0xFFB08A64),Color(0xFF76634C),Color(0xFFCFBA96))[i%3]; size*=1.4f; vy-=10f }
                "efeito_onda_de_choque" -> { color=Color(0xFF65ECFF); vx=cos(angle.toDouble()).toFloat()*18f; vy=sin(angle.toDouble()).toFloat()*18f; size*=.6f }
                "efeito_brilho_celestial" -> { color=listOf(Color.White,Color(0xFFFFEAB0),Color(0xFFC9C0FF))[i%3]; shape=ParticleShape.SPARK_STAR; vy-=4f; life=1000L }

                "plasma" -> {
                    // Upward swirling plasma / flame particles with magenta and orange fire
                    vx = (Random.nextFloat() - 0.5f) * 6f
                    vy = -(Random.nextFloat() * 12f + 6f)
                    color = when (Random.nextInt(4)) {
                        0 -> Color(0xFFFF1744)
                        1 -> Color(0xFFFF9100)
                        2 -> Color(0xFFFFD600)
                        else -> Color(0xFFE040FB)
                    }
                    size = Random.nextFloat() * 14f + 8f
                    life = Random.nextLong(500L, 1000L)
                    shape = ParticleShape.FLAME
                }
                "coins" -> {
                    // Bouncing gleaming golden coins with gravity
                    vx = (Random.nextFloat() - 0.5f) * 16f
                    vy = -(Random.nextFloat() * 16f + 8f)
                    color = when (Random.nextInt(3)) {
                        0 -> Color(0xFFFFD600)
                        1 -> Color(0xFFFFEA00)
                        else -> Color(0xFFFFA000)
                    }
                    size = Random.nextFloat() * 8f + 12f
                    life = Random.nextLong(600L, 1100L)
                    shape = ParticleShape.CIRCLE_COIN
                }
                "explosion" -> {
                    // High-speed radial pixel fragments
                    speed = Random.nextFloat() * 22f + 6f
                    vx = cos(angle.toDouble()).toFloat() * speed
                    vy = sin(angle.toDouble()).toFloat() * speed
                    size = Random.nextFloat() * 14f + 4f
                    color = when (Random.nextInt(4)) {
                        0 -> Color(0xFFFF5252)
                        1 -> Color(0xFFFFAB00)
                        2 -> Color(0xFF00E5FF)
                        else -> Color(0xFFE040FB)
                    }
                    shape = ParticleShape.SQUARE
                }
                else -> { // "sparks"
                    // Electric sparks with sparkling star diamonds
                    speed = Random.nextFloat() * 14f + 4f
                    vx = cos(angle.toDouble()).toFloat() * speed
                    vy = sin(angle.toDouble()).toFloat() * speed - 1f
                    color = when (Random.nextInt(3)) {
                        0 -> Color(0xFF00E5FF)
                        1 -> Color(0xFF80D8FF)
                        else -> Color(0xFFFFD600)
                    }
                    size = Random.nextFloat() * 10f + 6f
                    shape = ParticleShape.SPARK_STAR
                }
            }

            newParticles.add(
                PixelParticle(
                    x = centerX + (Random.nextFloat() - 0.5f) * 40f,
                    y = centerY + (Random.nextFloat() - 0.5f) * 40f,
                    vx = vx,
                    vy = vy,
                    color = color,
                    size = size,
                    lifeTimeMs = life,
                    shape = shape,
                    rotationSpeed = rotSpeed
                )
            )
        }
        particles.addAll(newParticles)
    }

    fun update(deltaMs: Long) {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.ageMs += deltaMs
            if (p.ageMs >= p.lifeTimeMs) {
                iterator.remove()
            } else {
                p.x += p.vx
                p.y += p.vy
                p.rotation += p.rotationSpeed
                if (p.shape != ParticleShape.FLAME) {
                    p.vy += 0.75f // Gravity for sparks, explosions and coins
                } else {
                    p.vy *= 0.96f // Flames decelerate smoothly upwards
                }
                val progress = p.ageMs.toFloat() / p.lifeTimeMs.toFloat()
                p.alpha = (1.0f - progress).coerceIn(0f, 1f)
            }
        }
    }
}

@Composable
fun ParticleOverlay(
    controller: ParticleController,
    modifier: Modifier = Modifier
) {
    var tick by remember { mutableStateOf(0L) }

    LaunchedEffect(controller.particles.size) {
        while (controller.particles.isNotEmpty()) {
            delay(16) // ~60 FPS update loop
            controller.update(16)
            tick++
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        // Force recomposition on tick
        @Suppress("UNUSED_EXPRESSION")
        tick

        for (p in controller.particles) {
            val currentAlpha = p.alpha
            when (p.shape) {
                ParticleShape.CIRCLE_COIN -> {
                    // Render golden coin disk with inner rim and shine
                    val radius = p.size / 2f
                    drawCircle(
                        color = p.color.copy(alpha = currentAlpha),
                        center = Offset(p.x, p.y),
                        radius = radius
                    )
                    drawCircle(
                        color = Color(0xFFFFF9C4).copy(alpha = currentAlpha * 0.9f),
                        center = Offset(p.x, p.y),
                        radius = radius * 0.65f,
                        style = Stroke(width = 1.5f)
                    )
                }
                ParticleShape.SPARK_STAR -> {
                    // Render diamond spark star
                    val half = p.size / 2f
                    val starPath = Path().apply {
                        moveTo(p.x, p.y - half)
                        lineTo(p.x + half * 0.4f, p.y - half * 0.4f)
                        lineTo(p.x + half, p.y)
                        lineTo(p.x + half * 0.4f, p.y + half * 0.4f)
                        lineTo(p.x, p.y + half)
                        lineTo(p.x - half * 0.4f, p.y + half * 0.4f)
                        lineTo(p.x - half, p.y)
                        lineTo(p.x - half * 0.4f, p.y - half * 0.4f)
                        close()
                    }
                    drawPath(
                        path = starPath,
                        color = p.color.copy(alpha = currentAlpha)
                    )
                }
                ParticleShape.FLAME -> {
                    // Render glowing organic flame drop
                    drawCircle(
                        color = p.color.copy(alpha = currentAlpha * 0.8f),
                        center = Offset(p.x, p.y),
                        radius = p.size / 2f
                    )
                    drawCircle(
                        color = Color(0xFFFFFFFF).copy(alpha = currentAlpha * 0.6f),
                        center = Offset(p.x, p.y - p.size * 0.15f),
                        radius = p.size * 0.25f
                    )
                }
                ParticleShape.SQUARE -> {
                    drawRect(
                        color = p.color.copy(alpha = currentAlpha),
                        topLeft = Offset(p.x - p.size / 2f, p.y - p.size / 2f),
                        size = Size(p.size, p.size)
                    )
                }
            }
        }
    }
}
