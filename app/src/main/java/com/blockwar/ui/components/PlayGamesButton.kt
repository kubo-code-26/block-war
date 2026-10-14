package com.blockwar.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blockwar.model.VersusGameState

@Composable
fun PlayGamesButton(state: VersusGameState, onClick: () -> Unit, label: String) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(onClick = onClick, enabled = !state.playGamesBusy,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E676))) {
            if (state.playGamesBusy) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF00E676))
            } else Icon(Icons.Default.SportsEsports, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (state.playGamesBusy) "CONECTANDO…" else label,
                fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        if (state.playGamesMessage.isNotBlank()) {
            Text(state.playGamesMessage, color = Color(0xFFB0BEC5), fontSize = 12.sp)
        }
    }
}
