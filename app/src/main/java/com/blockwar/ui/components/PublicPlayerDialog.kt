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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blockwar.achievements.AchievementCatalog
import com.blockwar.model.FeaturedBadge
import com.blockwar.network.EngagementBackend
import com.blockwar.ui.theme.ArenaStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class PublicPlayerViewModel:ViewModel() {
    var data by mutableStateOf<Map<String,Any?>>(emptyMap());private set
    var busy by mutableStateOf(false);private set
    var error by mutableStateOf("");private set
    fun load(owner:String,target:String) {
        if(busy)return
        busy=true;error=""
        EngagementBackend.call("bwPlayerProfile",mapOf("nickname" to owner,"target" to target),
            {data=it;busy=false},{data=emptyMap();error=it;busy=false})
    }
}

/** Keep the avatar artwork; add a labelled, accessible route to the real player. */
@Composable internal fun InspectablePlayerAvatar(owner:String,target:String,modifier:Modifier=Modifier,content:@Composable ()->Unit) {
    var open by remember(owner,target){mutableStateOf(false)}
    val enabled=owner.isNotBlank()&&target.isNotBlank()&&!owner.equals(target,true)
    Box(modifier.clickable(enabled=enabled,role=Role.Button,onClickLabel="Ver perfil de $target"){open=true},contentAlignment=Alignment.Center) {content()}
    if(open)PublicPlayerDialog(owner,target,{open=false})
}

@Composable internal fun PublicPlayerDialog(owner:String,target:String,onClose:()->Unit,onInvite:((String)->Unit)?=null) {
    val vm:PublicPlayerViewModel=viewModel(key="public-player-${owner.lowercase(Locale.ROOT)}-${target.lowercase(Locale.ROOT)}")
    LaunchedEffect(owner,target){vm.load(owner,target)}
    Dialog(onDismissRequest=onClose,properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
        BoxWithConstraints(Modifier.fillMaxSize().appSafeDrawingPadding().padding(12.dp),contentAlignment=Alignment.Center) {
            Column(Modifier.widthIn(max=460.dp).fillMaxWidth().heightIn(max=maxHeight)
                .background(ArenaStyle.Background,CutCornerShape(12.dp)).border(1.dp,ArenaStyle.Outline,CutCornerShape(12.dp))
                .testTag("public_player_dialog")) {
                ArenaHeader("PERFIL DO JOGADOR",onClose,Icons.Default.Person) {
                    IconButton(onClick={vm.load(owner,target)},enabled=!vm.busy){Icon(Icons.Default.Refresh,"Atualizar perfil",tint=ArenaStyle.Cyan)}
                }
                if(vm.busy)LinearProgressIndicator(Modifier.fillMaxWidth())
                Column(Modifier.weight(1f,fill=false).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    when {
                        vm.error.isNotBlank()->{
                            Text(vm.error,color=ArenaStyle.Muted)
                            TextButton(onClick={vm.load(owner,target)}){Text("Tentar novamente")}
                        }
                        vm.data.isEmpty()->Text("Carregando perfil…",color=ArenaStyle.Muted)
                        else->{
                            PublicPlayerContent(vm.data)
                            if(onInvite!=null) {
                                val social=friendsModel(owner)
                                LaunchedEffect(owner){social.call(owner)}
                                if(social.contacts.any{it.nickname.equals(target,true)&&it.status=="friend"}) {
                                    Button(onClick={onInvite(target)},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {Icon(Icons.Default.SportsEsports,null);Spacer(Modifier.width(8.dp));Text("CONVIDAR PARA JOGAR")}
                                } else AddOpponentFriendButton(owner,target)
                            } else AddOpponentFriendButton(owner,target)
                        }
                    }
                }
            }
        }
    }
}

@Composable internal fun PublicPlayerContent(data:Map<String,Any?>) {
    val nickname=data["nickname"]?.toString().orEmpty()
    val points=(data["pontosBw"] as? Number)?.toInt()?:0
    val badge=(data["featuredBadge"] as? Map<*,*>)?.let{FeaturedBadge(it["category"]?.toString().orEmpty(),(it["tier"] as? Number)?.toInt()?:0)}
    Row(Modifier.fillMaxWidth().playerPanel().padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        AvatarWithFrame(data["avatar"]?.toString()?:"avatar_default",data["frame"]?.toString()?:"moldura_simple",64.dp)
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text(nickname,color=ArenaStyle.Text,fontSize=22.sp,fontWeight=FontWeight.Black)
            Text(when {points>=2000->"Mestre Pixel";points>=1500->"Diamante Pixel";points>=1000->"Platina Pixel";points>=600->"Ouro Pixel";points>=300->"Prata Pixel";else->"Bronze Pixel"},color=ArenaStyle.Purple,fontSize=12.sp)
            Text("$points PONTOS BW",color=ArenaStyle.Cyan,fontSize=12.sp,fontWeight=FontWeight.Bold)
            FeaturedBadgeView(badge,compact=true)
        }
    }
    val stats=data["statistics"] as? Map<*,*>?:emptyMap<Any,Any>()
    fun number(key:String)=(stats[key] as? Number)?.toInt()?:0
    PlayerHeading("ESTATÍSTICAS ONLINE")
    val metrics=listOf("Vitórias" to number("wins").toString(),"Derrotas" to number("losses").toString(),
        "Aproveitamento" to "${number("winRate")}%","Partidas" to number("matches").toString(),
        "Linhas" to number("lines").toString(),"Ataques" to number("attacks").toString(),
        "Combos" to number("combos").toString(),"Recorde BW" to number("highScore").toString())
    metrics.chunked(2).forEach{pair->
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            pair.forEach{(label,value)->Column(Modifier.weight(1f).playerPanel().padding(10.dp)) {
                Text(value,color=ArenaStyle.Text,fontSize=19.sp,fontWeight=FontWeight.Bold)
                Text(label,color=ArenaStyle.Muted,fontSize=11.sp)
            }}
        }
    }
    PlayerHeading("CONQUISTAS")
    val achievements=(data["achievements"] as? List<*>)?.filterIsInstance<Map<*,*>>().orEmpty()
    achievements.forEach{row->
        val category=AchievementCatalog.categories.firstOrNull{it.id==row["id"]}?:return@forEach
        val unlocked=((row["unlocked"] as? Number)?.toInt()?:0).coerceIn(0,7)
        var expanded by remember(nickname,category.id){mutableStateOf(false)}
        Column(Modifier.fillMaxWidth().playerPanel().padding(10.dp)) {
            Row(Modifier.fillMaxWidth().clickable(role=Role.Button,onClickLabel="Ver escudos de ${category.name}"){expanded=!expanded},verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                AchievementShield(category.id,(unlocked-1).coerceAtLeast(0),unlocked>0,Modifier.size(42.dp),allowAnimation=false)
                Column(Modifier.weight(1f)) {Text(category.name,color=ArenaStyle.Text,fontWeight=FontWeight.Bold);Text("$unlocked de 7 escudos",color=ArenaStyle.Muted,fontSize=11.sp)}
                Icon(if(expanded)Icons.Default.ExpandLess else Icons.Default.ExpandMore,null,tint=ArenaStyle.Cyan)
            }
            if(expanded)PublicShields(category.id,unlocked)
        }
    }
    PlayerHeading("ÚLTIMAS 5 PARTIDAS ONLINE")
    val history=(data["history"] as? List<*>)?.filterIsInstance<Map<*,*>>().orEmpty().take(5)
    if(history.isEmpty())Text("Ainda não há partidas online confirmadas.",color=ArenaStyle.Muted,fontSize=12.sp,modifier=Modifier.testTag("public_history_empty"))
    val format=remember{SimpleDateFormat("dd/MM · HH:mm",Locale("pt","BR")).apply{timeZone=TimeZone.getTimeZone("America/Sao_Paulo")}}
    history.forEachIndexed{i,row->
        val won=row["won"]==true
        val duration=(row["duration"] as? Number)?.toLong()?:0L
        Column(Modifier.fillMaxWidth().playerPanel().padding(12.dp).testTag("public_history_$i"),verticalArrangement=Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Text(if(won)"VITÓRIA" else "DERROTA",color=if(won)ArenaStyle.Success else ArenaStyle.Purple,fontWeight=FontWeight.Bold,fontSize=12.sp)
                Text((row["finishedAt"] as? Number)?.toLong()?.let{format.format(Date(it))}.orEmpty(),color=ArenaStyle.Muted,fontSize=10.sp)
            }
            Text("VS ${row["opponent"]?:"Adversário"}",color=ArenaStyle.Text,fontWeight=FontWeight.Bold)
            Text("${(row["score"] as? Number)?.toInt()?:0} BW · ${duration/60}m ${duration%60}s",color=ArenaStyle.Cyan,fontSize=12.sp)
            Text("${(row["lines"] as? Number)?.toInt()?:0} linhas · ${(row["attacks"] as? Number)?.toInt()?:0} blocos de ataque",color=ArenaStyle.Muted,fontSize=11.sp)
        }
    }
}
private fun Modifier.playerPanel()=background(ArenaStyle.SurfaceLow,CutCornerShape(8.dp)).border(1.dp,ArenaStyle.Outline.copy(alpha=.6f),CutCornerShape(8.dp))
@Composable private fun PlayerHeading(text:String){Text(text,color=ArenaStyle.Text,fontSize=14.sp,fontWeight=FontWeight.Black,modifier=Modifier.padding(top=4.dp))}
@Composable @OptIn(ExperimentalLayoutApi::class) private fun PublicShields(category:String,unlocked:Int) {
    FlowRow(Modifier.fillMaxWidth().padding(top=8.dp),horizontalArrangement=Arrangement.spacedBy(6.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        AchievementCatalog.tiers.forEachIndexed{i,tier->Column(horizontalAlignment=Alignment.CenterHorizontally) {
            AchievementShield(category,i,i<unlocked,Modifier.size(40.dp),allowAnimation=false)
            Text(tier.name,color=ArenaStyle.Muted,fontSize=8.sp)
        }}
    }
}
