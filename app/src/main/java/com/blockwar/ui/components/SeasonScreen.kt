package com.blockwar.ui.components

import com.blockwar.ui.theme.ArenaStyle

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blockwar.economy.ShopCatalog
import com.blockwar.model.*
import com.blockwar.network.EngagementBackend
import java.time.*
import java.util.Locale

private val SeasonCyan=ArenaStyle.Cyan
private val SeasonGold=Color(0xFFFFB34E)
private val SeasonMuted=ArenaStyle.Muted

class SeasonViewModel:ViewModel() {
    var data by mutableStateOf<Map<String,Any?>>(emptyMap());private set
    var busy by mutableStateOf(false);private set
    var message by mutableStateOf("");private set
    var celebration by mutableStateOf<List<String>>(emptyList());private set
    fun dismissCelebration(){celebration=emptyList()}
    fun load(nickname:String,claim:Int?=null) {
        if(busy)return
        busy=true;message=""
        EngagementBackend.call("bwSeason",mapOf("nickname" to nickname)+if(claim==null)emptyMap() else mapOf("claim" to claim),
            {result->
                data=result;busy=false
                if(claim!=null) {
                    val tier=(result["tiers"] as? List<*>)?.getOrNull(claim) as? Map<*,*>
                    if(tier?.get("claimed")==true) {
                        celebration=(tier["items"] as? List<*>)?.mapNotNull{ShopCatalog.find(it.toString())?.name}.orEmpty()
                        message="Recompensa conquistada! Equipe seus itens na loja ou no perfil."
                    }
                }
            },{busy=false;message=it})
    }
}

@Composable @OptIn(ExperimentalMaterial3Api::class)
fun SeasonScreen(nickname:String,onClose:()->Unit) {
    val vm:SeasonViewModel=viewModel(key="season-${nickname.lowercase(Locale.ROOT)}")
    var collection by remember{mutableStateOf<String?>(null)}
    LaunchedEffect(nickname){vm.load(nickname)}
    val board=vm.data["board"]?.toString()?:"real_fundicao"
    val accent=CosmeticArt.accent(board)
    Dialog(onDismissRequest=onClose,properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
        Box(Modifier.fillMaxSize().background(Color(0xFF080E16))) {
            CosmeticScene(if(board=="real_fundicao")"hero_forja" else board,Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Color(0xFF070E18).copy(alpha=.88f)))
            Column(Modifier.fillMaxSize().appSafeDrawingPadding()) {
                ArenaHeader("TEMPORADA", onClose, Icons.Default.WorkspacePremium) {
                    IconButton(onClick={vm.load(nickname)},enabled=!vm.busy){Icon(Icons.Default.Refresh,"Atualizar temporada",tint=SeasonCyan)}
                }
                PullToRefreshBox(isRefreshing=vm.busy,onRefresh={vm.load(nickname)},modifier=Modifier.weight(1f)) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        if(vm.data.isEmpty())SeasonPanel(accent){Text(if(vm.busy)"Preparando sua temporada…" else "Não foi possível carregar a temporada.",color=Color.White)}
                        else SeasonPageContent(vm.data,nickname,vm.busy,{vm.load(nickname,it)},{collection=it})
                        if(vm.message.isNotBlank())Text(vm.message,color=SeasonGold,modifier=Modifier.padding(8.dp))
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
    if(collection!=null)SeasonCollection(vm.data,collection!!){collection=null}
    if(vm.celebration.isNotEmpty())AlertDialog(onDismissRequest=vm::dismissCelebration,containerColor=Color(0xFF111A25),
        icon={Icon(Icons.Default.WorkspacePremium,null,tint=SeasonGold,modifier=Modifier.size(48.dp))},
        title={Text("RECOMPENSA CONQUISTADA",color=SeasonGold,fontWeight=FontWeight.Black,textAlign=TextAlign.Center)},
        text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
            AnimatedBanner("season_banner_${vm.data["id"]}",Modifier.fillMaxWidth().height(80.dp).clip(CutCornerShape(8.dp)))
            vm.celebration.forEach{Text("✓ $it",color=Color.White)}
            Text("Os itens continuam seus após o fim da temporada.",color=SeasonMuted)
        }},confirmButton={TextButton(onClick=vm::dismissCelebration){Text("CONTINUAR",color=SeasonCyan)}})
}

@Composable
private fun SeasonPanel(accent:Color=SeasonGold,modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit) {
    val shape=CutCornerShape(10.dp)
    Column(modifier.fillMaxWidth().clip(shape)
        .background(Brush.verticalGradient(listOf(Color(0xFF1B2734).copy(alpha=.96f),Color(0xFF09111C).copy(alpha=.98f))))
        .border(1.dp,Brush.linearGradient(listOf(accent.copy(alpha=.8f),Color(0xFF425261),accent.copy(alpha=.45f))),shape)
        .padding(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)
}
@Composable private fun SeasonHeading(text:String){Text(text,color=Color(0xFFCEDCF2),fontSize=16.sp,fontWeight=FontWeight.Black)}

internal fun seasonDaysRemaining(period:String,now:Instant=Instant.now()):Long?=runCatching {
    val end=YearMonth.parse(period).plusMonths(1).atDay(1).atStartOfDay(ZoneId.of("America/Sao_Paulo")).toInstant()
    (Duration.between(now,end).seconds.coerceAtLeast(0)+86399)/86400
}.getOrNull()

@Composable @OptIn(ExperimentalLayoutApi::class)
internal fun SeasonPageContent(data:Map<String,Any?>,nickname:String,busy:Boolean,onClaim:(Int)->Unit,onCollection:(String)->Unit) {
    val board=data["board"]?.toString()?:"real_fundicao"
    val theme=data["id"]?.toString()?:"forja"
    val accent=CosmeticArt.accent(board)
    val xp=(data["xp"] as? Number)?.toInt()?:0
    val tiers=(data["tiers"] as? List<*>)?.filterIsInstance<Map<*,*>>().orEmpty()
    val next=tiers.firstOrNull{((it["goal"] as? Number)?.toInt()?:Int.MAX_VALUE)>xp}
    val goal=(next?.get("goal") as? Number)?.toInt()?:((tiers.lastOrNull()?.get("goal") as? Number)?.toInt()?:1500)
    val title=tiers.lastOrNull{((it["goal"] as? Number)?.toInt()?:Int.MAX_VALUE)<=xp}?.get("title")?.toString()?:"Recruta"
    val days=seasonDaysRemaining(data["period"]?.toString().orEmpty())
    Box(Modifier.fillMaxWidth().heightIn(min=244.dp).clip(CutCornerShape(10.dp)).border(1.dp,accent.copy(alpha=.7f),CutCornerShape(10.dp))) {
        CosmeticScene(if(theme=="forja")"hero_forja" else board,Modifier.matchParentSize())
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(Color(0xFF07101C).copy(alpha=.72f),Color.Transparent))))
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("BLOCK WAR",color=SeasonCyan,fontSize=13.sp,fontWeight=FontWeight.Black,letterSpacing=2.sp)
            Text(if(theme=="forja")"FORJA\nDA ARENA" else data["name"]?.toString()?.uppercase(Locale.ROOT).orEmpty(),
                style=TextStyle(brush=Brush.verticalGradient(listOf(Color(0xFFFFE8BA),accent,Color(0xFF9F542F))),
                    fontSize=36.sp,lineHeight=38.sp,fontWeight=FontWeight.Black,shadow=Shadow(Color.Black,blurRadius=8f)))
            Row(Modifier.clip(RoundedCornerShape(20.dp)).background(Color.Black.copy(alpha=.72f))
                .border(1.dp,accent.copy(alpha=.8f),RoundedCornerShape(20.dp)).padding(horizontal=10.dp,vertical=7.dp),
                verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Schedule,null,tint=accent,modifier=Modifier.size(16.dp))
                Text(if(days==null)"TEMPORADA MENSAL" else if(days==0L)"TEMPORADA ENCERRADA" else "TERMINA EM $days ${if(days==1L)"DIA" else "DIAS"}",color=accent,fontSize=11.sp,fontWeight=FontWeight.Bold)
            }
            Text("Domine a arena. Conquiste a coleção.",color=Color(0xFFD4E2EF),fontSize=12.sp,fontWeight=FontWeight.SemiBold)
        }
    }
    SeasonPanel(accent) {
        SeasonHeading("SEU PROGRESSO")
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            if(theme=="forja")AvatarImage("avatar_forja",Modifier.size(66.dp).clip(CutCornerShape(8.dp)))
            else Icon(Icons.Default.WorkspacePremium,null,tint=accent,modifier=Modifier.size(56.dp))
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text(title,color=Color.White,fontWeight=FontWeight.Black,fontSize=22.sp)
                Text("$xp / $goal XP",color=SeasonCyan,fontWeight=FontWeight.Bold,fontSize=17.sp)
                LinearProgressIndicator(progress={(xp.toFloat()/goal.coerceAtLeast(1)).coerceIn(0f,1f)},
                    modifier=Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(10.dp)),color=SeasonCyan,trackColor=Color(0xFF263342),drawStopIndicator={})
                Text(if(next==null)"Todas as metas concluídas!" else "Faltam ${goal-xp} XP para ${next["title"]}",color=SeasonMuted,fontSize=11.sp)
            }
        }
    }
    SeasonRewardPath(tiers,xp,busy,accent,onClaim)
    SeasonPanel(accent) {
        SeasonHeading(if(theme=="forja")"COLEÇÃO FUNDIÇÃO" else "COLEÇÃO DA TEMPORADA")
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
            listOf(Triple("blocos","Peças",Icons.Default.Widgets),Triple("tabuleiros","Tabuleiro",Icons.Default.GridView),
                Triple("avatares","Avatar",Icons.Default.Person),Triple("molduras","Moldura",Icons.Default.CropSquare),
                Triple("banners","Banner",Icons.Default.Flag),Triple("efeitos","Efeitos",Icons.Default.LocalFireDepartment)).forEach{(key,label,icon)->
                Column(Modifier.weight(1f).fillMaxHeight().clip(CutCornerShape(6.dp)).background(Color(0xFF121D29))
                    .border(1.dp,accent.copy(alpha=.35f),CutCornerShape(6.dp)).clickable(role=Role.Button){onCollection(key)}.padding(horizontal=2.dp,vertical=8.dp),
                    horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Icon(icon,null,tint=accent,modifier=Modifier.size(22.dp));Text(label,color=SeasonMuted,fontSize=9.sp,textAlign=TextAlign.Center)
                }
            }
        }
        OutlinedButton(onClick={onCollection("")},modifier=Modifier.fillMaxWidth().testTag("season_collection"),shape=CutCornerShape(8.dp),
            border=BorderStroke(1.dp,SeasonCyan),colors=ButtonDefaults.outlinedButtonColors(contentColor=SeasonCyan)) {
            Text("VER COLE\u00c7\u00c3O",fontSize=10.sp,fontWeight=FontWeight.Black,textAlign=TextAlign.Center)
        }
    }
    SeasonRanking(data,nickname,xp,accent)
    Text("Partidas online rendem XP. Seus itens continuam com você.",color=SeasonMuted,fontSize=12.sp,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth().padding(6.dp))
    Text("Vitória: 30 XP · Derrota: 10 XP · Mais as linhas limpas (até 100). CPU e abandono não dão XP. Renova a cada mês, no horário de Brasília.",color=SeasonMuted.copy(alpha=.75f),fontSize=11.sp,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth())
}

@Composable internal fun SeasonRanking(data:Map<String,Any?>,nickname:String,xp:Int,accent:Color) {
    val rows=(data["leaderboard"] as? List<*>)?.filterIsInstance<Map<*,*>>().orEmpty()
    var expanded by remember{mutableStateOf(false)}
    SeasonPanel(accent) {
        SeasonHeading("RANKING DA TEMPORADA")
        if(rows.isEmpty())Text("A arena espera seus primeiros competidores. Conclua uma partida online para entrar no ranking.",color=SeasonMuted,fontSize=12.sp)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.Bottom) {
            for(rank in listOf(1,0,2)) {
                val row=rows.getOrNull(rank);val medal=when(rank){0->SeasonGold;1->Color(0xFFB4C9D9);else->Color(0xFFCB8A60)}
                Column(Modifier.weight(1f).clip(CutCornerShape(8.dp)).background(medal.copy(alpha=.07f))
                    .border(1.dp,medal.copy(alpha=.7f),CutCornerShape(8.dp)).padding(horizontal=6.dp,vertical=if(rank==0)18.dp else 12.dp),
                    horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.WorkspacePremium,null,tint=medal,modifier=Modifier.size(if(rank==0)34.dp else 28.dp))
                    Text("${rank+1}º",color=medal,fontWeight=FontWeight.Black,fontSize=18.sp)
                    if(row!=null)InspectablePlayerAvatar(nickname,row["nickname"]?.toString().orEmpty(),Modifier.size(48.dp)) {AvatarWithFrame(row["avatar"]?.toString()?:"avatar_default",row["frame"]?.toString()?:"none",40.dp)}
                    Text(row?.get("nickname")?.toString()?:"Vaga aberta",color=Color.White,fontSize=12.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
                    Text(row?.let{"${it["xp"]} XP"}?:"—",color=medal,fontSize=11.sp,fontWeight=FontWeight.Bold)
                }
            }
        }
        val position=rows.indexOfFirst{it["nickname"]?.toString()?.equals(nickname,true)==true}
        Row(Modifier.fillMaxWidth().clip(CutCornerShape(6.dp)).background(SeasonCyan.copy(alpha=.08f))
            .border(1.dp,SeasonCyan.copy(alpha=.7f),CutCornerShape(6.dp)).padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.Person,null,tint=SeasonCyan)
            Text(if(position>=0)"VOCÊ · ${position+1}º" else "VOCÊ · SEM POSIÇÃO NO TOP 50",color=Color.White,fontSize=11.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
            Text("$xp XP",color=SeasonCyan,fontWeight=FontWeight.Black,fontSize=14.sp)
        }
        if(rows.size>3)TextButton(onClick={expanded=!expanded}){Text(if(expanded)"Recolher ranking" else "Ver top ${rows.size}",color=SeasonCyan)}
        if(expanded)rows.drop(3).forEachIndexed{i,row->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
            Text("${i+4}º · ${row["nickname"]}",color=SeasonMuted,fontSize=12.sp);Text("${row["xp"]} XP",color=Color.White,fontSize=12.sp)
        }}
    }
}

@Composable private fun SeasonCollection(data:Map<String,Any?>,category:String,onClose:()->Unit) {
    val theme=data["id"]?.toString()?:"forja"
    val rewardIds=(data["tiers"] as? List<*>)?.filterIsInstance<Map<*,*>>()?.flatMap{(it["items"] as? List<*>)?.map{value->value.toString()}.orEmpty()}.orEmpty()
    val ids=rewardIds+if(theme=="forja")listOf("avatar_forja","fundo_forja","real_magma") else emptyList()
    val items=ids.distinct().mapNotNull(ShopCatalog::find).filter{category.isEmpty()||it.itemCategory==category||category=="efeitos"&&it.itemCategory=="animacoes"||category=="tabuleiros"&&it.itemCategory=="fundos"}
    ArenaPage("Coleção ${if(theme=="forja")"Fundição" else data["name"]?:"da temporada"}",onClose) {
        Text("Prévia dos itens. Recompensas são resgatadas na temporada; compras e equipamentos ficam na loja e no perfil.",color=SeasonMuted,fontSize=12.sp)
        items.forEach{item->SeasonPanel(CosmeticArt.accent(item.id)) {
            CosmeticThumbnail(item,Modifier.fillMaxWidth().height(120.dp))
            Text(item.name,color=Color.White,fontWeight=FontWeight.Bold)
            Text(if(item.seasonOnly)"Recompensa exclusiva da temporada" else "Disponível na loja · ${item.price} moedas",color=SeasonMuted,fontSize=12.sp)
        }}
    }
}
