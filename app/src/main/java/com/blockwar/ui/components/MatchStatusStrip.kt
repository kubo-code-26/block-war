package com.blockwar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blockwar.model.*
import com.blockwar.ui.theme.ArenaStyle

@Composable internal fun MatchStatusStrip(state:VersusGameState) {
    val online=state.gameMode==GameMode.ONLINE_1V1
    val ready=state.localOnlineReady&&!state.onlineSessionCancelled
    val color=if(!online)ArenaStyle.Cyan else if(ready)ArenaStyle.Success else ArenaStyle.Gold
    Row(Modifier.fillMaxWidth().background(ArenaStyle.Recessed,CutCornerShape(6.dp))
        .border(1.dp,ArenaStyle.Outline.copy(alpha=.6f),CutCornerShape(6.dp)).padding(horizontal=10.dp,vertical=7.dp).testTag("match_status_strip"),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        Icon(if(online)Icons.Default.Wifi else Icons.Default.SmartToy,null,tint=color,modifier=Modifier.size(17.dp))
        Text(if(!online)"Modo CPU" else if(ready)"Conectado" else "Reconectando…",color=color,fontSize=10.sp,modifier=Modifier.weight(1f))
        if(online)AvatarImage(state.opponentAvatar,Modifier.size(20.dp))
        Text(if(!online)state.cpuDifficulty.displayName else if(state.opponentOnlineReady)"Rival conectado" else "Aguardando rival",
            color=ArenaStyle.Muted,fontSize=10.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
    }
}
