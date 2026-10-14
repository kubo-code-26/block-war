package com.blockwar.ui.components

import com.blockwar.ui.theme.ArenaStyle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.blockwar.achievements.*
import com.blockwar.model.*
import java.text.NumberFormat
import java.util.Locale

private val AchievementCyan = ArenaStyle.Cyan
private val AchievementMuted = ArenaStyle.Muted
private fun achievementNumber(n: Int) = NumberFormat.getIntegerInstance(Locale.forLanguageTag("pt-BR")).format(n)

@Composable
fun AchievementsEntry(count: Int, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(Modifier.fillMaxWidth().clip(shape).background(ArenaStyle.SurfaceLow)
        .border(1.dp, ArenaStyle.Outline.copy(alpha=.5f), shape)
        .clickable(onClick=onClick).testTag("profile_section_achievements").padding(16.dp),
        verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Default.MilitaryTech, null, tint=Color(0xFFFFC947), modifier=Modifier.size(28.dp))
        Column(Modifier.weight(1f), verticalArrangement=Arrangement.spacedBy(3.dp)) {
            Text("CONQUISTAS",color=Color.White,fontSize=15.sp,fontWeight=FontWeight.Bold)
            Text("$count de 42 escudos · ver coleção",color=AchievementMuted,fontSize=12.sp)
        }
        Icon(Icons.Default.ChevronRight,"Abrir conquistas",tint=Color(0xFFFFC947))
    }
}

@Composable
fun AchievementsScreen(stats: AggregatedStats, inventory: PlayerInventory, shop: List<ShopItem>, onClose: () -> Unit,
    featured:FeaturedBadge?=null,message:String="",onFeature:(String,Int)->Unit={_,_->}) {
    val progress = AchievementCatalog.progress(stats, inventory, shop)
    var selected by remember { mutableStateOf<String?>(null) }
    val selectedProgress = progress.firstOrNull { it.category.id == selected }
    val gridState = rememberLazyGridState()
    val back: () -> Unit = { if(selected == null) onClose() else selected = null }
    Dialog(onDismissRequest=back, properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
        Column(Modifier.fillMaxSize().background(ArenaStyle.Background).appSafeDrawingPadding().testTag("achievements_screen")) {
            ArenaHeader(selectedProgress?.category?.name ?: "CONQUISTAS", back, Icons.Default.MilitaryTech, "achievements_back") {
                if(featured!=null)androidx.compose.material3.TextButton(onClick={onFeature("",-1)}){Text("Ocultar",color=AchievementCyan)}
            }
            Text(if(selectedProgress==null) "${progress.sumOf { it.unlockedCount }} de 42 escudos" else "${selectedProgress.unlockedCount} de 7 escudos",
                color=AchievementMuted,fontSize=12.sp,modifier=Modifier.padding(horizontal=16.dp,vertical=10.dp))
            if(selectedProgress == null) {
                LazyVerticalGrid(columns=GridCells.Adaptive(150.dp),state=gridState,
                    modifier=Modifier.weight(1f).fillMaxWidth().testTag("achievement_categories"),
                    contentPadding=PaddingValues(start=16.dp,end=16.dp,top=16.dp,bottom=64.dp),
                    horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    items(progress,key={it.category.id}) { item ->
                        AchievementTile(item) { selected=item.category.id }
                    }
                }
            } else {
                AchievementDetails(selectedProgress,featured,message,onFeature)
            }
        }
    }
}

@Composable
private fun AchievementTile(progress: AchievementProgress, onClick: () -> Unit) {
    val color=progress.currentTier?.let { Color(it.color) } ?: AchievementCyan
    val shape=RoundedCornerShape(14.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(ArenaStyle.SurfaceLow)
        .border(1.dp,ArenaStyle.Outline,shape).clickable(onClick=onClick)
        .testTag("achievement_${progress.category.id}").padding(12.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            AchievementShield(progress.category.id,progress.unlockedCount-1,progress.unlockedCount>0,Modifier.size(40.dp))
            Spacer(Modifier.weight(1f))
            Text("${progress.unlockedCount}/7",color=AchievementMuted,fontSize=11.sp)
            Icon(Icons.Default.ChevronRight,null,tint=AchievementMuted,modifier=Modifier.size(16.dp))
        }
        Text(progress.category.name,color=Color.White,fontSize=14.sp,fontWeight=FontWeight.Bold,minLines=2,maxLines=2)
        Text(progress.currentTier?.name ?: "Sem escudo",color=color,fontSize=11.sp)
        Text(progress.nextGoal?.let { "${achievementNumber(progress.value)} / ${achievementNumber(it)}" }
            ?: "Nível máximo",color=AchievementMuted,fontSize=11.sp)
        AchievementBar(progress.fraction,color)
    }
}

@Composable
private fun AchievementBar(fraction: Float,color: Color) {
    Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(ArenaStyle.Outline)) {
        if(fraction>0f) Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(color))
    }
}

@Composable
private fun ColumnScope.AchievementDetails(progress: AchievementProgress,featured:FeaturedBadge?,message:String,onFeature:(String,Int)->Unit) {
    LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("achievement_details"),
        contentPadding=PaddingValues(start=16.dp,end=16.dp,top=16.dp,bottom=64.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        item {
            Column(Modifier.fillMaxWidth().padding(bottom=8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                if(message.isNotBlank())Text(message,color=AchievementCyan)
                Text(progress.category.description,color=AchievementMuted,fontSize=13.sp)
                Text("${achievementNumber(progress.value)} ${progress.category.unit}",color=Color.White,fontSize=18.sp,fontWeight=FontWeight.Bold)
                AchievementBar(progress.fraction,AchievementCyan)
                Text(progress.nextGoal?.let { "Faltam ${achievementNumber(it-progress.value)} para ${AchievementCatalog.tiers[progress.unlockedCount].name}" }
                    ?: "Todos os escudos desta categoria foram conquistados!",color=AchievementCyan,fontSize=12.sp)
            }
        }
        itemsIndexed(AchievementCatalog.tiers,key={index,_->index}) { index,tier ->
            val unlocked=index<progress.unlockedCount
            val current=index==progress.unlockedCount-1
            val shape=RoundedCornerShape(12.dp)
            Row(Modifier.fillMaxWidth().clip(shape).background(ArenaStyle.SurfaceLow)
                .border(1.dp,if(current) Color(tier.color) else ArenaStyle.Outline,shape)
                .padding(12.dp).testTag("achievement_tier_$index"),verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                AchievementShield(progress.category.id,index,unlocked,Modifier.size(42.dp))
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                    Text("${tier.name} · ${tier.title}",color=Color(tier.color),fontSize=13.sp,fontWeight=FontWeight.Bold)
                    Text("${achievementNumber(progress.category.goals[index])} ${progress.category.unit}",color=AchievementMuted,fontSize=12.sp)
                    Text(if(current) "Nível atual" else if(unlocked) "Obtido" else "Bloqueado",color=if(unlocked) Color(tier.color) else AchievementMuted,fontSize=10.sp)
                    if(unlocked) androidx.compose.material3.TextButton(onClick={onFeature(progress.category.id,index)}) {
                        Text(if(featured?.category==progress.category.id&&featured.tier==index)"Em destaque" else "Exibir no perfil",color=AchievementCyan,fontSize=11.sp)
                    }
                }
                if(unlocked) Icon(Icons.Default.Check,null,tint=Color(tier.color),modifier=Modifier.size(18.dp))
            }
        }
        item {
            Text(if(progress.category.id=="combos") "Cada sequência com duas ou mais peças limpando linhas conta um combo. Registros antigos não possuem esse contador."
                else if(progress.category.id=="collector") "Contam apenas itens pagos diferentes. Itens iniciais gratuitos não entram na contagem."
                else "Progresso acumulado de partidas online confirmadas.",color=AchievementMuted,fontSize=11.sp,modifier=Modifier.padding(top=8.dp))
        }
    }
}

