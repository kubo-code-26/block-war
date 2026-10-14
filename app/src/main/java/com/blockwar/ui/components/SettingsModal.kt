package com.blockwar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.blockwar.model.ControlMode
import com.blockwar.model.ControlSettings

// Paleta "Neon Grid Battle" (Stitch)
private val StBackground = Color(0xFF121318)
private val StSurfaceLowest = Color(0xFF0D0E13)
private val StSurfaceLow = Color(0xFF1A1B21)
private val StSurfaceHigh = Color(0xFF292A2F)
private val StSurfaceHighest = Color(0xFF34343A)
private val StOnSurface = Color(0xFFE3E1E9)
private val StOnSurfaceVariant = Color(0xFFB9CACB)
private val StOutlineVariant = Color(0xFF3B494B)
private val StCyan = Color(0xFF00F0FF)

@Composable
fun SettingsModal(
    controlSettings: ControlSettings,
    isAudioEnabled: Boolean,
    nickname: String,
    onToggleAudio: () -> Unit,
    onOpenControlsEditor: () -> Unit,
    onOpenRules: () -> Unit,
    onClose: () -> Unit,
    playGamesConnected: Boolean = false,
    playGamesAccountName: String = "",
    playGamesBusy: Boolean = false,
    playGamesMessage: String = "",
    onConnectPlayGames: () -> Unit = {}
) {
    var showPlayGamesDialog by remember { mutableStateOf(false) }
    val connectionText = when {
        playGamesBusy -> "Sincronizando com Google Play Games…"
        playGamesConnected -> "Conectado à conta ${playGamesAccountName.ifBlank { "do Play Games" }}"
        else -> "Desconectado · toque para conectar"
    }
    val modeLabel = if (controlSettings.controlMode == ControlMode.GESTURES) {
        "Modo atual: gestos táteis"
    } else {
        "Modo atual: botões virtuais"
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(StBackground)
                .testTag("settings_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize().appSafeDrawingPadding()) {
                SettingsHeader(onBack = onClose)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(StOutlineVariant.copy(alpha = 0.5f))
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ---------------- ÁUDIO & TRILHA ----------------
                    SettingsSection(
                        title = "ÁUDIO & TRILHA",
                        icon = Icons.Default.VolumeUp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(StSurfaceLowest)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Áudio do jogo",
                                    color = StOnSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif
                                )
                                Text(
                                    text = "Efeitos sonoros e trilha chiptune",
                                    color = StOnSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }
                            Switch(
                                checked = isAudioEnabled,
                                onCheckedChange = { onToggleAudio() },
                                thumbContent = if (isAudioEnabled) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = StCyan,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                } else null,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = StBackground,
                                    checkedTrackColor = StCyan,
                                    checkedBorderColor = StCyan,
                                    uncheckedThumbColor = StOnSurfaceVariant,
                                    uncheckedTrackColor = StSurfaceHighest,
                                    uncheckedBorderColor = StOutlineVariant
                                ),
                                modifier = Modifier.testTag("settings_audio_switch")
                            )
                        }
                    }

                    // ---------------- PERSONALIZAR CONTROLES ----------------
                    val controlsShape = RoundedCornerShape(14.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(controlsShape)
                            .background(StSurfaceLow)
                            .border(1.dp, StOutlineVariant.copy(alpha = 0.4f), controlsShape)
                            .clickable { onOpenControlsEditor() }
                            .padding(12.dp)
                            .testTag("settings_customize_controls"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = StCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PERSONALIZAR CONTROLES",
                                color = StOnSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                letterSpacing = 0.3.sp,
                                maxLines = 1
                            )
                            Text(
                                text = modeLabel,
                                color = StOnSurfaceVariant,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = StOnSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // ---------------- CONTA & SUPORTE ----------------
                    SettingsSection(
                        title = "CONTA & SUPORTE",
                        icon = Icons.Default.Shield
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(StSurfaceLowest)
                                .clickable { showPlayGamesDialog = true }
                                .testTag("settings_play_games_account")
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(StSurfaceHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SportsEsports,
                                    contentDescription = null,
                                    tint = StCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Google Play Games",
                                    color = StOnSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif
                                )
                                Text(
                                    text = connectionText,
                                    color = if (playGamesConnected) Color(0xFF00E676) else StOnSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (playGamesBusy) {
                                CircularProgressIndicator(Modifier.size(20.dp), color = StCyan, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Abrir vínculo do Play Games",
                                    tint = StOnSurfaceVariant, modifier = Modifier.size(20.dp))
                            }
                        }

                        SettingsLinkRow(
                            icon = Icons.Default.HelpOutline,
                            label = "Como jogar & regras",
                            tag = "settings_open_rules",
                            onClick = onOpenRules
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
    if (showPlayGamesDialog) {
        AlertDialog(
            onDismissRequest = { showPlayGamesDialog = false },
            containerColor = StSurfaceLow,
            titleContentColor = StOnSurface,
            textContentColor = StOnSurfaceVariant,
            icon = { Icon(Icons.Default.SportsEsports, null, tint = StCyan) },
            title = { Text("GOOGLE PLAY GAMES", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(connectionText, color = if (playGamesConnected) Color(0xFF00E676) else StOnSurfaceVariant)
                    Text(if (playGamesConnected)
                        "Seu perfil Block War está vinculado. Toque em sincronizar para confirmar a conexão e atualizar os dados."
                        else "Conecte sua conta do Play Games para vincular e recuperar seu perfil Block War.")
                    if (playGamesMessage.isNotBlank()) Text(playGamesMessage)
                    if (playGamesBusy) CircularProgressIndicator(Modifier.size(24.dp), color = StCyan, strokeWidth = 2.dp)
                }
            },
            confirmButton = {
                TextButton(onClick = onConnectPlayGames, enabled = !playGamesBusy,
                    modifier = Modifier.testTag("settings_connect_play_games")) {
                    Text(if (playGamesBusy) "SINCRONIZANDO…" else if (playGamesConnected) "SINCRONIZAR" else "CONECTAR", color = StCyan)
                }
            },
            dismissButton = { TextButton(onClick = { showPlayGamesDialog = false }) { Text("FECHAR", color = StOnSurfaceVariant) } }
        )
    }

}

@Composable
private fun SettingsHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(StSurfaceHigh)
                .clickable { onBack() }
                .testTag("close_settings_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Voltar",
                tint = StOnSurface,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(StCyan.copy(alpha = 0.12f))
                .border(1.dp, StCyan.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Configurações",
                tint = StCyan,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "CONFIGURAÇÕES",
            color = StOnSurface,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 0.5.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(StSurfaceLow)
            .border(1.dp, StOutlineVariant.copy(alpha = 0.4f), shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = StCyan,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = StOnSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 0.3.sp,
                maxLines = 1
            )
        }
        content()
    }
}

@Composable
private fun SettingsLinkRow(
    icon: ImageVector,
    label: String,
    tag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 10.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = StOnSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = StOnSurface,
            fontSize = 14.sp,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = StOnSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}