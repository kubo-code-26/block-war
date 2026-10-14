package com.blockwar.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blockwar.model.ButtonPos
import com.blockwar.model.ControlMode
import com.blockwar.model.ControlSettings
import kotlin.math.roundToInt

// Paleta "Neon Grid Battle" (Stitch)
private val CeBackground = Color(0xFF0B0C10)
private val CePanel = Color(0xFF1E1F25)
private val CeCard = Color(0xFF292A2F)
private val CeCyan = Color(0xFF00F0FF)
private val CeCyanSoft = Color(0xFF76EBF5)
private val CeYellow = Color(0xFFFFD600)
private val CeAmber = Color(0xFFFBBF24)
private val CeGreen = Color(0xFF00E676)
private val CePink = Color(0xFFFF4DA6)
private val CePurple = Color(0xFFCF5CFF)
private val CeBlue = Color(0xFF38BDF8)
private val CeRose = Color(0xFFFB7185)
private val CeRoseBorder = Color(0xFFF43F5E)
private val CeSlateText = Color(0xFFCBD5E1)
private val CeSlateMuted = Color(0xFF94A3B8)
private val CeTrack = Color(0xFF1B2230)

@Composable
fun ControlsEditorScreen(
    initialSettings: ControlSettings,
    onSaveSettings: (ControlSettings) -> Unit,
    onResetDefaults: () -> ControlSettings,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedControlMode by remember { mutableStateOf(initialSettings.controlMode) }
    var scale by remember { mutableFloatStateOf(initialSettings.scale) }
    var opacity by remember { mutableFloatStateOf(initialSettings.opacity) }
    var buttonGap by remember { mutableFloatStateOf(initialSettings.buttonGap) }
    var gestureSensitivity by remember { mutableFloatStateOf(initialSettings.gestureSensitivity) }
    var posLeft by remember { mutableStateOf(initialSettings.posLeft) }
    var posRotate by remember { mutableStateOf(initialSettings.posRotate) }
    var posRight by remember { mutableStateOf(initialSettings.posRight) }
    var posSoftDrop by remember { mutableStateOf(initialSettings.posSoftDrop) }
    var posHardDrop by remember { mutableStateOf(initialSettings.posHardDrop) }
    val isGestures = selectedControlMode == ControlMode.GESTURES

    Column(modifier.fillMaxSize().background(Color(0xFF121318))
        .statusBarsPadding().navigationBarsPadding().testTag("controls_editor_screen")) {
        Row(Modifier.fillMaxWidth().background(Color(0xFF0D0E13)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clickable(onClick = onCancel), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = CeCyan)
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.Settings, null, tint = CeCyan, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text("PERSONALIZAR CONTROLES", color = Color(0xFFE3E1E9), fontSize = 16.sp,
                fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            UnifiedControlSection("DISPOSIÇÃO DOS BOTÕES", Icons.Default.SportsEsports, !isGestures) {
                Text(if (isGestures) "Botões desativados enquanto os gestos estão ligados."
                else "Arraste os botões para reposicionar.",
                    color = if (isGestures) Color(0xFF62646C) else CeSlateMuted, fontSize = 11.sp)
                Box(Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1E1F25)), contentAlignment = Alignment.Center) {
                    Column(Modifier.fillMaxWidth().padding(8.dp).alpha(opacity),
                        verticalArrangement = Arrangement.spacedBy(buttonGap.dp)) {
                        val cyan = if (isGestures) Color(0xFF55575F) else CeCyan
                        val purple = if (isGestures) Color(0xFF55575F) else CePurple
                        val peach = if (isGestures) Color(0xFF55575F) else Color(0xFFFFB59C)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(buttonGap.dp)) {
                            DraggableEditorButton("ESQUERDA", Icons.AutoMirrored.Filled.ArrowBack, cyan,
                                posLeft, scale, { dx, dy -> posLeft = ButtonPos(posLeft.x + dx, posLeft.y + dy) }, Modifier.weight(1f))
                            DraggableEditorButton("GIRAR", Icons.Default.Refresh, purple,
                                posRotate, scale, { dx, dy -> posRotate = ButtonPos(posRotate.x + dx, posRotate.y + dy) }, Modifier.weight(1f))
                            DraggableEditorButton("DIREITA", Icons.AutoMirrored.Filled.ArrowForward, cyan,
                                posRight, scale, { dx, dy -> posRight = ButtonPos(posRight.x + dx, posRight.y + dy) }, Modifier.weight(1f))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(buttonGap.dp)) {
                            DraggableEditorButton("BAIXAR", Icons.Default.ArrowDownward, cyan,
                                posSoftDrop, scale, { dx, dy -> posSoftDrop = ButtonPos(posSoftDrop.x + dx, posSoftDrop.y + dy) }, Modifier.weight(1f))
                            DraggableEditorButton("QUEDA", Icons.Default.KeyboardDoubleArrowDown, peach,
                                posHardDrop, scale, { dx, dy -> posHardDrop = ButtonPos(posHardDrop.x + dx, posHardDrop.y + dy) }, Modifier.weight(1f))
                        }
                    }
                }
            }
            UnifiedControlSection("CALIBRAÇÃO DE BOTÕES", Icons.Default.Settings, !isGestures) {
                EditorSliderRow("Tamanho / Escala dos Botões", CeCyan, "${(scale * 100).roundToInt()}%",
                    scale, 0.8f..1.5f, CeCyan, { scale = it })
                EditorSliderRow("Opacidade do HUD", CeCyan, "${(opacity * 100).roundToInt()}%",
                    opacity, 0.3f..1.0f, CeCyan, { opacity = it })
                EditorSliderRow("Espaçamento Lateral / Margem", CeCyan, "${buttonGap.roundToInt()} dp",
                    buttonGap, 4f..24f, CeCyan, { buttonGap = it })
            }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFF1E1F25)).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TouchApp, null, tint = if (isGestures) CePurple else Color(0xFF62646C))
                    Spacer(Modifier.width(8.dp))
                    Text("GESTOS TÁTEIS", color = if (isGestures) Color(0xFFE3E1E9) else Color(0xFF62646C),
                        fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Switch(checked = isGestures,
                        onCheckedChange = { selectedControlMode = if (it) ControlMode.GESTURES else ControlMode.BUTTONS },
                        colors = SwitchDefaults.colors(checkedTrackColor = CeCyan, checkedThumbColor = Color(0xFF0D0E13),
                            uncheckedTrackColor = CeTrack, uncheckedThumbColor = CeSlateMuted),
                        modifier = Modifier.testTag("controls_gestures_switch"))
                }
                CompositionLocalProvider(LocalEditorSectionEnabled provides isGestures) {
                    EditorSliderRow("Sensibilidade de Arraste", CePurple,
                        "${String.format(java.util.Locale.US, "%.1f", gestureSensitivity)}x",
                        gestureSensitivity, 0.1f..1.5f, CePurple, { gestureSensitivity = it })
                    Text("GUIA DE GESTOS · 5 COMANDOS", color = if (isGestures) CeSlateText else Color(0xFF62646C),
                        fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    GestureGuideCard(Icons.Default.TouchApp, CePurple, "TOQUE RÁPIDO · GIRAR",
                        AnnotatedString("Toque rápido (< 200ms) para girar a peça."))
                    GestureGuideCard(Icons.Default.SwapHoriz, CeCyan, "DESLIZAR HORIZONTAL · MOVER",
                        AnnotatedString("Arraste para a esquerda ou direita para mover a peça."))
                    GestureGuideCard(Icons.Default.ArrowDownward, CeCyanSoft, "ARRASTAR BAIXO LENTO · SOFT DROP",
                        AnnotatedString("Deslize para baixo de forma moderada para descida contínua."))
                    GestureGuideCard(Icons.Default.KeyboardDoubleArrowDown, Color(0xFFFFB59C), "FLICK VERTICAL BAIXO · HARD DROP",
                        AnnotatedString("Gesto rápido para baixo leva a peça ao fundo."))
                    GestureGuideCard(Icons.Default.Timer, CeCyan, "TOQUE PROLONGADO · DESCIDA CONTÍNUA",
                        AnnotatedString("Pressione e segure sem arrastar para queda suave contínua."))
                }
            }
        }
        Row(Modifier.fillMaxWidth().background(Color(0xFF0D0E13)).padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UnifiedEditorAction("RESTAURAR", CeRose, "reset_controls_button", Modifier.weight(1f)) {
                val reset = onResetDefaults()
                scale = reset.scale
                opacity = reset.opacity
                buttonGap = reset.buttonGap
                gestureSensitivity = reset.gestureSensitivity
                posLeft = reset.posLeft
                posRotate = reset.posRotate
                posRight = reset.posRight
                posSoftDrop = reset.posSoftDrop
                posHardDrop = reset.posHardDrop
                selectedControlMode = reset.controlMode
            }
            UnifiedEditorAction("CANCELAR", CeSlateText, "cancel_controls_button", Modifier.weight(1f), onCancel)
            UnifiedEditorAction("SALVAR HUD", CeCyan, "save_controls_button", Modifier.weight(1.3f)) {
                onSaveSettings(initialSettings.copy(scale = scale, opacity = opacity, buttonGap = buttonGap,
                    gestureSensitivity = gestureSensitivity, controlMode = selectedControlMode,
                    posLeft = posLeft, posRotate = posRotate, posRight = posRight,
                    posSoftDrop = posSoftDrop, posHardDrop = posHardDrop))
            }
        }
    }
}

private val LocalEditorSectionEnabled = staticCompositionLocalOf { true }

@Composable
private fun UnifiedControlSection(title: String, icon: ImageVector, enabled: Boolean, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFF1E1F25)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (enabled) CeCyan else Color(0xFF62646C))
            Spacer(Modifier.width(8.dp))
            Text(title, color = if (enabled) Color(0xFFE3E1E9) else Color(0xFF62646C),
                fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        CompositionLocalProvider(LocalEditorSectionEnabled provides enabled) { content() }
    }
}

@Composable
private fun UnifiedEditorAction(label: String, color: Color, tag: String, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.height(48.dp).clip(RoundedCornerShape(8.dp)).background(if (tag == "save_controls_button") CeCyan else Color(0xFF292A2F))
        .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
        .clickable(onClick = onClick).testTag(tag).padding(4.dp), contentAlignment = Alignment.Center) {
        val actionColor = if (tag == "save_controls_button") Color(0xFF04222A) else color
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(imageVector = when (tag) {
                "reset_controls_button" -> Icons.Default.Restore
                "save_controls_button" -> Icons.Default.Save
                else -> Icons.Default.Close
            }, contentDescription = null, tint = actionColor, modifier = Modifier.size(16.dp))
            Text(label, color = actionColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EditorModeTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    activeBg: Color,
    activeText: Color,
    inactiveText: Color,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.horizontalGradient(if (selected) listOf(activeBg, activeBg.copy(alpha = 0.7f)) else listOf(CeCard, CeCard)))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) activeText else inactiveText,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = if (selected) activeText else inactiveText,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 0.6.sp,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun EditorSliderRow(
    label: String,
    labelColor: Color,
    chipText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    color: Color,
    onValueChange: (Float) -> Unit
) {
    val enabled = LocalEditorSectionEnabled.current
    val effectiveColor = if (enabled) color else Color(0xFF55575F)
    val effectiveLabel = if (enabled) labelColor else Color(0xFF62646C)

    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(CeCard).border(1.dp, effectiveLabel.copy(alpha = 0.3f), RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(imageVector = when {
                    label.startsWith("Tamanho") -> Icons.Default.AspectRatio
                    label.startsWith("Opacidade") -> Icons.Default.Opacity
                    label.startsWith("Espaçamento") -> Icons.Default.SettingsEthernet
                    else -> Icons.Default.Speed
                }, contentDescription = null,
                    tint = if (enabled) (if (label.startsWith("Tamanho")) CePurple else CeCyan) else Color(0xFF62646C),
                    modifier = Modifier.size(18.dp))
                Text(
                    text = label,
                    color = effectiveLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 0.8.sp
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(effectiveColor.copy(alpha = 0.15f))
                    .border(1.dp, effectiveColor.copy(alpha = 0.4f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text(
                    text = chipText,
                    color = effectiveColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            enabled = enabled,
            thumb = {
                Canvas(Modifier.size(18.dp)) {
                    drawCircle(effectiveColor.copy(alpha = 0.18f))
                    drawCircle(effectiveColor, radius = 6.dp.toPx())
                    drawCircle(Color(0xFF080A10), radius = 6.dp.toPx(),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
                }
            },
            track = {
                Canvas(Modifier.fillMaxWidth().height(4.dp)) {
                    val fraction = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
                    val start = Offset(0f, size.height / 2f)
                    drawLine(CeTrack, start, Offset(size.width, size.height / 2f), strokeWidth = size.height)
                    if (fraction > 0f) drawLine(effectiveColor, start, Offset(size.width * fraction, size.height / 2f), strokeWidth = size.height)
                }
            },
            colors = SliderDefaults.colors(
                thumbColor = effectiveColor,
                activeTrackColor = effectiveColor,
                inactiveTrackColor = CeTrack
            ),
            modifier = Modifier.fillMaxWidth().height(32.dp)
        )
    }
}

private fun richDescription(prefix: String, bold: String, suffix: String): AnnotatedString =
    buildAnnotatedString {
        append(prefix)
        withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
            append(bold)
        }
        append(suffix)
    }

@Composable
private fun GestureGuideCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: AnnotatedString
) {
    val enabled = LocalEditorSectionEnabled.current
    val effectiveIconColor = if (enabled) iconColor else Color(0xFF55575F)

    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CeCard)
            .border(1.dp, effectiveIconColor.copy(alpha = 0.4f), shape)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(effectiveIconColor.copy(alpha = 0.15f))
                .border(1.dp, effectiveIconColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = effectiveIconColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = effectiveIconColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 0.3.sp
            )
            Text(
                text = description,
                color = if (enabled) CeSlateText else Color(0xFF62646C),
                fontSize = 11.sp,
                fontFamily = FontFamily.SansSerif,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun DraggableEditorButton(
    label: String,
    icon: ImageVector,
    baseColor: Color,
    pos: ButtonPos,
    scale: Float,
    onDrag: (dx: Float, dy: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val enabled = LocalEditorSectionEnabled.current
    val currentOnDrag by rememberUpdatedState(onDrag)

    val btnHeight = (56 * scale).dp
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .offset { IntOffset(pos.x.roundToInt(), pos.y.roundToInt()) }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    currentOnDrag(dragAmount.x, dragAmount.y)
                }
            }
            .height(btnHeight)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF292A2F), Color(0xFF24252B))
                )
            )
            .border(1.dp, baseColor.copy(alpha = if (enabled) 0.8f else 0.35f), shape),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = baseColor,
                modifier = Modifier.size((22 * scale).dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = baseColor,
                fontSize = (9 * scale).sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 0.5.sp,
                maxLines = 1
            )
        }
    }
}