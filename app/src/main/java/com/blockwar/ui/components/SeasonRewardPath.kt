package com.blockwar.ui.components

import com.blockwar.ui.theme.ArenaStyle

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blockwar.economy.ShopCatalog

private val PathCyan=ArenaStyle.Cyan
private val PathGold=Color(0xFFFFB244)

@Composable
internal fun SeasonRewardPath(tiers:List<Map<*,*>>,xp:Int,busy:Boolean,accent:Color,onClaim:(Int)->Unit) {
    val shape=CutCornerShape(12.dp)
    Box(Modifier.fillMaxWidth().clip(shape).background(Color(0xFF090F17))
        .border(3.dp,Brush.linearGradient(listOf(Color(0xFF70442C),Color(0xFFADC2CB),Color(0xFF322A25),accent,Color(0xFF503021))),shape)
        .padding(5.dp).border(1.dp,Color(0xFF77604B),CutCornerShape(8.dp))) {
        Canvas(Modifier.matchParentSize()) {
            // Brushed metal and ember-lit corners, behind the readable content.
            for(i in 0..40) {
                val y=size.height*i/40f
                drawLine(Color(0xFF71818C).copy(alpha=.035f),Offset(0f,y),Offset(size.width,y),1f)
            }
            for(x in listOf(0f,size.width))for(y in listOf(0f,size.height))
                drawCircle(Brush.radialGradient(listOf(accent.copy(alpha=.22f),Color.Transparent),Offset(x,y),70.dp.toPx()),70.dp.toPx(),Offset(x,y))
        }
        Column(Modifier.padding(horizontal=10.dp,vertical=14.dp)) {
            Text("CAMINHO DE RECOMPENSAS",color=ArenaStyle.Text,fontWeight=FontWeight.Black,fontSize=16.sp)
            val count=tiers.sumOf{(it["items"] as? List<*>)?.size?:0}
            Text("$count itens para conquistar",color=ArenaStyle.Muted,fontSize=11.sp,modifier=Modifier.padding(top=5.dp,bottom=12.dp))
            tiers.forEachIndexed { index,tier ->
                val items=(tier["items"] as? List<*>)?.mapNotNull{ShopCatalog.find(it.toString())}.orEmpty()
                val goal=(tier["goal"] as? Number)?.toInt()?:Int.MAX_VALUE
                val claimed=tier["claimed"]==true
                val available=xp>=goal&&!claimed
                val color=if(claimed)PathCyan else if(available)PathGold else Color(0xFF687583)
                items.forEachIndexed { itemIndex,item ->
                    val lastItem=itemIndex==items.lastIndex
                    val lastRow=index==tiers.lastIndex&&lastItem
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).testTag("season_reward_item_${item.id}"),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.width(25.dp).fillMaxHeight(),horizontalAlignment=Alignment.CenterHorizontally) {
                            Box(Modifier.size(25.dp).clip(CircleShape).background(color.copy(alpha=.12f)).border(1.5.dp,color,CircleShape),contentAlignment=Alignment.Center) {
                                if(available)Box(Modifier.size(9.dp).clip(CircleShape).background(color))
                                else Icon(if(claimed)Icons.Default.Check else Icons.Default.Lock,if(claimed)"Resgatado" else "Bloqueado",tint=color,modifier=Modifier.size(16.dp))
                            }
                            if(!lastRow)Box(Modifier.width(2.dp).weight(1f).background(Brush.verticalGradient(listOf(color,color.copy(alpha=.22f)))))
                        }
                        Column(Modifier.weight(1f).padding(bottom=12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                            Text("$goal XP · ${tier["title"]}",color=if(available)PathGold else ArenaStyle.Text,fontWeight=FontWeight.Bold,fontSize=13.sp)
                            Text(item.name,color=ArenaStyle.Muted,fontSize=11.sp)
                            Row(Modifier.fillMaxWidth().padding(vertical=3.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                                CosmeticThumbnail(item,Modifier.width(94.dp).height(64.dp))
                                val statusShape=CutCornerShape(7.dp)
                                if(available&&lastItem)Button(onClick={onClaim(index)},enabled=!busy,
                                    modifier=Modifier.widthIn(min=112.dp).heightIn(min=44.dp).testTag("season_claim_$index"),
                                    shape=statusShape,contentPadding=PaddingValues(horizontal=10.dp),
                                    colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFFF9D24),contentColor=Color(0xFF231001))) {
                                    Text(if(items.size>1)"RESGATAR ${items.size}" else "RESGATAR",fontWeight=FontWeight.Black,fontSize=10.sp)
                                } else Box(Modifier.widthIn(min=112.dp).heightIn(min=40.dp).clip(statusShape)
                                    .background(color.copy(alpha=.10f)).border(1.dp,color.copy(alpha=.65f),statusShape).padding(8.dp),contentAlignment=Alignment.Center) {
                                    Text(if(claimed)"✓ RESGATADO" else if(available)"RESGATE CONJUNTO" else "BLOQUEADO",color=color,fontSize=9.sp,fontWeight=FontWeight.Bold)
                                }
                            }
                            if(items.size>1)Text("Pacote de ${items.size} itens · mesma meta",color=Color(0xFF879BAA),fontSize=10.sp)
                            if(!lastRow)HorizontalDivider(color=Color(0xFF31404A),modifier=Modifier.padding(top=3.dp))
                        }
                    }
                }
            }
        }
    }
}