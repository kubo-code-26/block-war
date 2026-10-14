package com.blockwar.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.blockwar.model.*
import com.blockwar.ui.theme.ArenaStyle
import java.util.Locale

@Composable
fun GameOverDialog(
    gameState: VersusGameState,
    matchDurationSeconds: Long,
    totalAttacksSent: Int,
    onRestart: () -> Unit,
    onQuickMatch: () -> Unit = {},
    onRematch: () -> Unit = {},
    onOpenHistory: () -> Unit,
    onMenu: () -> Unit
) {
    if(gameState.matchStatus !in listOf(MatchStatus.VICTORY,MatchStatus.DEFEAT))return
    val won=gameState.matchStatus==MatchStatus.VICTORY
    val online=gameState.gameMode==GameMode.ONLINE_1V1
    val accent=if(won)ArenaStyle.Gold else ArenaStyle.Purple
    val shape=CutCornerShape(12.dp)
    Dialog(onDismissRequest={},properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
        BoxWithConstraints(Modifier.fillMaxSize().appSafeDrawingPadding().padding(12.dp),contentAlignment=Alignment.Center) {
            Column(Modifier.widthIn(max=440.dp).fillMaxWidth().heightIn(max=maxHeight)
                .background(Brush.verticalGradient(listOf(ArenaStyle.Surface,ArenaStyle.Recessed)),shape)
                .border(1.dp,Brush.verticalGradient(listOf(accent,ArenaStyle.Outline)),shape)
                .verticalScroll(rememberScrollState()).padding(16.dp).testTag("game_over_dialog"),
                verticalArrangement=Arrangement.spacedBy(10.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                Icon(if(won)Icons.Default.EmojiEvents else Icons.Default.HeartBroken,null,tint=accent,modifier=Modifier.size(40.dp))
                Text(if(won)"VITÓRIA!" else "DERROTA",color=accent,fontSize=26.sp,fontWeight=FontWeight.Black)
                Text(if(won) {if(online)"Partida online concluída" else "Batalha contra CPU concluída"} else "A próxima batalha é sua",
                    color=ArenaStyle.Muted,fontSize=12.sp,textAlign=TextAlign.Center)
                Row(Modifier.fillMaxWidth().resultPanel().padding(10.dp),verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    AvatarWithFrame(gameState.inventory.equippedItems["avatares"]?:"avatar_default",
                        gameState.inventory.equippedItems["molduras"]?:"moldura_simple",36.dp)
                    Text(gameState.nickname.ifBlank{"VOCÊ"},color=ArenaStyle.Cyan,fontWeight=FontWeight.Bold,fontSize=12.sp,
                        maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(1f))
                    Text("VS",color=ArenaStyle.Muted,fontSize=11.sp)
                    Text(if(online)gameState.opponentNickname.ifBlank{"RIVAL"} else "CPU · ${gameState.cpuDifficulty.displayName}",
                        color=ArenaStyle.Purple,fontSize=12.sp,maxLines=2,overflow=TextOverflow.Ellipsis,
                        textAlign=TextAlign.End,modifier=Modifier.weight(1f))
                    if(online)InspectablePlayerAvatar(gameState.nickname,gameState.opponentNickname,Modifier.size(48.dp)) {AvatarImage(gameState.opponentAvatar,Modifier.size(36.dp))}
                    else Icon(Icons.Default.SmartToy,null,tint=ArenaStyle.Purple,modifier=Modifier.size(30.dp))
                }
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    ResultStat("TEMPO",String.format(Locale.ROOT,"%02d:%02d",matchDurationSeconds.coerceAtLeast(0)/60,matchDurationSeconds.coerceAtLeast(0)%60),Icons.Default.Timer,Modifier.weight(1f))
                    ResultStat("LINHAS",gameState.player.linesCleared.toString(),Icons.Default.GridView,Modifier.weight(1f))
                }
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    ResultStat("ATAQUES","$totalAttacksSent blocos",Icons.Default.Bolt,Modifier.weight(1f))
                    ResultStat("COMBO","${gameState.player.maxCombo}x",Icons.Default.Star,Modifier.weight(1f))
                }
                Text("${gameState.player.score} BW na partida",color=ArenaStyle.Muted,fontSize=11.sp)
                ResultRewards(gameState)
                if(gameState.onlineConnectionMessage.isNotBlank())Text(gameState.onlineConnectionMessage,color=ArenaStyle.Muted,fontSize=11.sp)
                Button(onClick=if(online)onQuickMatch else onRestart,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("restart_button"),
                    shape=CutCornerShape(6.dp),colors=ButtonDefaults.buttonColors(containerColor=if(won)ArenaStyle.Gold else ArenaStyle.Cyan,contentColor=ArenaStyle.Recessed)) {
                    Icon(Icons.Default.Replay,null,modifier=Modifier.size(20.dp));Spacer(Modifier.width(6.dp))
                    Text(if(won)"JOGAR NOVAMENTE" else "TENTAR NOVAMENTE",fontWeight=FontWeight.Black,fontSize=13.sp)
                }
                if(online&&gameState.nickname.isNotBlank()&&gameState.opponentNickname.isNotBlank())
                    AddOpponentFriendButton(gameState.nickname,gameState.opponentNickname)
                if(online) {
                    TextButton(onClick=onRematch,enabled=!gameState.rematchBusy,modifier=Modifier.testTag("rematch_button")) {
                        Icon(Icons.Default.Sync,null,modifier=Modifier.size(18.dp));Spacer(Modifier.width(6.dp))
                        Text(if(gameState.rematchBusy)"PREPARANDO…" else "REVANCHE COM ESTE RIVAL",fontSize=11.sp)
                    }
                    if(gameState.rematchMessage.isNotBlank())Text(gameState.rematchMessage,color=ArenaStyle.Muted,fontSize=11.sp)
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    ShareResultButton(gameState,matchDurationSeconds,totalAttacksSent,Modifier.weight(1f),compact=true)
                    OutlinedButton(onClick=onMenu,modifier=Modifier.weight(1f).heightIn(min=48.dp).testTag("menu_button"),shape=CutCornerShape(6.dp)) {
                        Icon(Icons.Default.Home,null,modifier=Modifier.size(18.dp));Spacer(Modifier.width(4.dp));Text("Menu",fontSize=12.sp)
                    }
                }
                TextButton(onClick=onOpenHistory,modifier=Modifier.testTag("history_button")) {
                    Icon(Icons.Default.History,null,modifier=Modifier.size(16.dp));Spacer(Modifier.width(4.dp));Text("Ver histórico",fontSize=11.sp,color=ArenaStyle.Muted)
                }
            }
        }
    }
}
private fun Modifier.resultPanel()=background(ArenaStyle.Recessed,CutCornerShape(6.dp)).border(1.dp,ArenaStyle.Outline.copy(alpha=.65f),CutCornerShape(6.dp))
@Composable private fun ResultStat(label:String,value:String,icon:ImageVector,modifier:Modifier) {
    Row(modifier.resultPanel().padding(10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        Icon(icon,null,tint=ArenaStyle.CyanSoft,modifier=Modifier.size(22.dp))
        Column(Modifier.weight(1f)) {
            Text(label,color=ArenaStyle.Muted,fontSize=10.sp,fontWeight=FontWeight.Bold)
            Text(value,color=ArenaStyle.Text,fontSize=15.sp,fontWeight=FontWeight.Bold)
        }
    }
}
@Composable private fun ResultRewards(state:VersusGameState) {
    val r=state.matchRewardSummary
    Column(Modifier.fillMaxWidth().resultPanel().padding(10.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Text("RECOMPENSAS",color=ArenaStyle.Muted,fontSize=10.sp,fontWeight=FontWeight.Bold)
        if(r!=null) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly) {
                RewardAmount(r.bw?.let{if(it>=0)"+$it" else "$it"}?:"Salvo","BW",ArenaStyle.Gold)
                RewardAmount("+${r.blockoins}","Blockoins",ArenaStyle.Gold)
                RewardAmount(r.xp?.let{"+$it"}?:"Salvo","XP",ArenaStyle.Purple)
            }
            Text("✓ Recompensas confirmadas",color=ArenaStyle.Success,fontSize=11.sp,modifier=Modifier.testTag("result_rewards_confirmed"))
        } else Text(state.rewardSaveMessage.ifBlank{"Aguardando confirmação das recompensas…"},color=ArenaStyle.Muted,fontSize=11.sp,textAlign=TextAlign.Center,modifier=Modifier.testTag("result_rewards_pending"))
    }
}
@Composable private fun RewardAmount(value:String,label:String,color:Color) {
    Column(horizontalAlignment=Alignment.CenterHorizontally) {
        Text(value,color=color,fontSize=16.sp,fontWeight=FontWeight.Black)
        Text(label,color=ArenaStyle.Muted,fontSize=10.sp)
    }
}
