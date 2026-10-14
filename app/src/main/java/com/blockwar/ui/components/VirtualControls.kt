package com.blockwar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blockwar.model.ButtonPos
import com.blockwar.model.ControlSettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun VirtualControls(
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onRotate: () -> Unit,
    onHardDrop: () -> Unit,
    onSoftDrop: () -> Unit,
    controlSettings: ControlSettings = ControlSettings(),
    modifier: Modifier = Modifier
) {
    val scale = controlSettings.scale
    val btnHeight = (58 * scale).dp
    val gap = controlSettings.buttonGap.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("virtual_controls_panel")
            .alpha(controlSettings.opacity)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(Color(0xFF101116))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF262D4A), Color(0xFF141726))
                ),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(gap)
        ) {
            // ROW 1: [Esquerda], [Girar], [Direita]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. ESQUERDA
                RepeatableArcadeButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    label = "ESQUERDA",
                    tag = "btn_left",
                    baseColor = Color(0xFF00F0FF),
                    onClick = onLeft,
                    enableRepeat = true,
                    scale = scale,
                    pos = controlSettings.posLeft,
                    modifier = Modifier
                        .weight(1f)
                        .height(btnHeight)
                )

                // 2. GIRAR
                RepeatableArcadeButton(
                    icon = Icons.Default.Refresh,
                    label = "GIRAR",
                    tag = "btn_rotate",
                    baseColor = Color(0xFFCF5CFF),
                    onClick = onRotate,
                    enableRepeat = false,
                    highlightGlow = true,
                    scale = scale,
                    pos = controlSettings.posRotate,
                    modifier = Modifier
                        .weight(1f)
                        .height(btnHeight)
                )

                // 3. DIREITA
                RepeatableArcadeButton(
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    label = "DIREITA",
                    tag = "btn_right",
                    baseColor = Color(0xFF00F0FF),
                    onClick = onRight,
                    enableRepeat = true,
                    scale = scale,
                    pos = controlSettings.posRight,
                    modifier = Modifier
                        .weight(1f)
                        .height(btnHeight)
                )
            }

            // ROW 2: [Baixar], [Queda Rápida]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. BAIXAR
                RepeatableArcadeButton(
                    icon = Icons.Default.ArrowDownward,
                    label = "BAIXAR",
                    tag = "btn_down",
                    baseColor = Color(0xFF00F0FF),
                    onClick = onSoftDrop,
                    enableRepeat = true,
                    repeatInterval = 50L,
                    scale = scale,
                    pos = controlSettings.posSoftDrop,
                    modifier = Modifier
                        .weight(1f)
                        .height(btnHeight * 1.05f)
                )

                // 2. QUEDA RÁPIDA
                RepeatableArcadeButton(
                    icon = Icons.Default.KeyboardDoubleArrowDown,
                    label = "QUEDA",
                    tag = "btn_drop",
                    baseColor = Color(0xFFCF5CFF),
                    onClick = onHardDrop,
                    enableRepeat = false,
                    highlightGlow = true,
                    scale = scale,
                    pos = controlSettings.posHardDrop,
                    modifier = Modifier
                        .weight(1f)
                        .height(btnHeight * 1.05f)
                )
            }
        }
    }
}

@Composable
fun RepeatableArcadeButton(
    icon: ImageVector,
    label: String,
    tag: String,
    baseColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enableRepeat: Boolean = false,
    repeatInterval: Long = 70L,
    highlightGlow: Boolean = false,
    scale: Float = 1.0f,
    pos: ButtonPos = ButtonPos(0f, 0f)
) {
    var isPressed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var repeatJob by remember { mutableStateOf<Job?>(null) }

    Box(
        modifier = modifier
            .offset { IntOffset(pos.x.roundToInt(), pos.y.roundToInt()) }
            .testTag(tag)
            .border(1.dp, if (isPressed) baseColor else Color(0xFF34343A), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    if (isPressed) {
                        listOf(baseColor.copy(alpha = 0.5f), Color(0xFF0D0F1B))
                    } else {
                        listOf(Color(0xFF292A2F), Color(0xFF1E1F25))
                    }
                )
            )
            .pointerInput(enableRepeat) {
                awaitEachGesture {
                    awaitFirstDown()
                    isPressed = true
                    onClick()

                    if (enableRepeat) {
                        repeatJob = scope.launch {
                            delay(180) // Initial DAS delay
                            while (isActive) {
                                onClick()
                                delay(repeatInterval)
                            }
                        }
                    }

                    waitForUpOrCancellation()
                    isPressed = false
                    repeatJob?.cancel()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isPressed) Color.White.copy(alpha = 0.75f) else baseColor,
                modifier = Modifier.size((22 * scale).dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = baseColor,
                fontSize = (9 * scale).sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.3.sp,
                maxLines = 1
            )
        }
    }
}
