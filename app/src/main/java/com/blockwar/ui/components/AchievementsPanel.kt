package com.blockwar.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
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
import com.blockwar.achievements.*
import com.blockwar.model.*
import java.text.NumberFormat
import java.util.Locale

private val AchievementCyan = Color(0xFF00F0FF)
private fun achievementNumber(n: Int) = NumberFormat.getIntegerInstance(Locale.forLanguageTag("pt-BR")).format(n)

@Composable
fun AchievementsPanel(stats: AggregatedStats, inventory: PlayerInventory, shop: List<ShopItem>) {
    val progress = AchievementCatalog.progress(stats, inventory, shop)
    val count = progress.sumOf { it.unlockedCount }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.testTag("achievements_panel")) {
        Text("$count / 42 ESCUDOS CONQUISTADOS", color = AchievementCyan,
            fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("Cada categoria evolui por conta própria. Toque para ver os sete escudos e suas metas.",
            color = Color(0xFFB9CACB), fontSize = 12.sp)
        progress.forEach { AchievementCategoryCard(it) }
        Text("Resultados online confirmados pelo servidor. Combos: cada sequência com duas ou mais peças limpando linhas conta uma vez. O contador de combos começa nas partidas registradas com esta atualização.",
            color = Color(0xFFB9CACB), fontSize = 11.sp)
    }
}

@Composable
private fun AchievementCategoryCard(progress: AchievementProgress) {
    var expanded by remember(progress.category.id) { mutableStateOf(false) }
    val color = progress.currentTier?.let { Color(it.color) } ?: AchievementCyan
    val shape = RoundedCornerShape(14.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(Color(0xFF10131B))
        .border(1.dp, color.copy(alpha = 0.35f), shape).padding(12.dp)
        .testTag("achievement_${progress.category.id}"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }
            .semantics { contentDescription = "${progress.category.name}, ${progress.currentTier?.name ?: "sem escudo"}. Ver metas" },
            verticalAlignment = Alignment.CenterVertically) {
            AchievementShield(progress.category.id, progress.unlockedCount - 1, progress.unlockedCount > 0, Modifier.size(60.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(progress.category.name.uppercase(Locale.forLanguageTag("pt-BR")), color = Color.White,
                    fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(progress.currentTier?.let { "${it.name} · ${it.title}" } ?: "Sem escudo · rumo ao Bronze",
                    color = color, fontSize = 12.sp)
                Text(progress.category.description, color = Color(0xFFB9CACB), fontSize = 11.sp)
            }
            Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if(expanded) "Recolher metas" else "Expandir metas", tint = color)
        }
        val next = progress.nextGoal
        Text(if(next != null) "${achievementNumber(progress.value)} / ${achievementNumber(next)} ${progress.category.unit}"
            else "${achievementNumber(progress.value)} ${progress.category.unit} · nível máximo",
            color = Color.White, fontSize = 12.sp)
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF29323E))) {
            if(progress.fraction > 0f) Box(Modifier.fillMaxWidth(progress.fraction).fillMaxHeight().background(color))
        }
        if(next != null) Text("Faltam ${achievementNumber(next - progress.value)} para ${AchievementCatalog.tiers[progress.unlockedCount].name}",
            color = Color(0xFFB9CACB), fontSize = 11.sp)
        if(expanded) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AchievementCatalog.tiers.forEachIndexed { index, tier ->
                    val unlocked = index < progress.unlockedCount
                    Column(Modifier.width(78.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        AchievementShield(progress.category.id, index, unlocked, Modifier.size(64.dp))
                        Text(tier.name, color = if(unlocked) Color(tier.color) else Color(0xFF9CA6B5), fontSize = 10.sp)
                        Text(if(unlocked) "Obtido" else "Bloqueado", color = Color(0xFFB9CACB), fontSize = 9.sp)
                    }
                }
            }
            AchievementCatalog.tiers.forEachIndexed { index, tier ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    Text(if(index < progress.unlockedCount) "✓" else "○", color = if(index < progress.unlockedCount) Color(tier.color) else Color(0xFFB9CACB))
                    Column(Modifier.weight(1f)) {
                        Text("${tier.name} · ${tier.title}", color = Color(tier.color), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("${achievementNumber(progress.category.goals[index])} ${progress.category.unit}", color = Color(0xFFB9CACB), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AchievementShield(category: String, level: Int, unlocked: Boolean, modifier: Modifier = Modifier) {
    val tier = AchievementCatalog.tiers.getOrNull(level)
    val color = if(unlocked && tier != null) Color(tier.color) else Color(0xFF667184)
    val icon = when(category) {
        "wins" -> Icons.Default.EmojiEvents
        "attacks" -> Icons.Default.Bolt
        "lines" -> Icons.Default.GridView
        "combos" -> Icons.Default.LocalFireDepartment
        "matches" -> Icons.Default.SportsEsports
        else -> Icons.Default.ShoppingBag
    }
    Box(modifier.semantics { contentDescription = "$category, ${tier?.name ?: "sem escudo"}, ${if(unlocked) "conquistado" else "bloqueado"}" }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            val shield = Path().apply {
                moveTo(w*0.5f,h*0.04f); lineTo(w*0.9f,h*0.21f);lineTo(w*0.84f,h*0.62f)
                quadraticTo(w*0.74f,h*0.85f,w*0.5f,h*0.96f)
                quadraticTo(w*0.26f,h*0.85f,w*0.16f,h*0.62f);lineTo(w*0.1f,h*0.21f);close()
            }
            drawPath(shield, Brush.verticalGradient(listOf(color.copy(alpha=0.32f),Color(0xFF10131B))))
            drawPath(shield, color, style=Stroke(width=w*0.035f))
            if(unlocked && level >= 3) drawPath(shield, color.copy(alpha=0.22f), style=Stroke(width=w*0.10f))
            drawLine(color.copy(alpha=0.5f), androidx.compose.ui.geometry.Offset(w*0.5f,h*0.08f), androidx.compose.ui.geometry.Offset(w*0.5f,h*0.9f),strokeWidth=w*0.015f)
            if(unlocked) repeat(level+1) { i ->
                val x=w*(0.25f+0.5f*(i+0.5f)/(level+1))
                drawCircle(if(level==6) Color(0xFFFFC947) else color,radius=w*0.019f,center=androidx.compose.ui.geometry.Offset(x,h*0.77f))
            }
        }
        Icon(icon, contentDescription=null,tint=color,modifier=Modifier.size(24.dp).offset(y=(-3).dp))
        if(!unlocked) Icon(Icons.Default.Lock,contentDescription=null,tint=Color(0xFFB9CACB),modifier=Modifier.align(Alignment.BottomEnd).size(16.dp))
    }
}

