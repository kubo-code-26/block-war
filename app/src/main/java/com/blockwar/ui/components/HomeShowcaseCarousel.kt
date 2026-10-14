package com.blockwar.ui.components

import com.blockwar.ui.theme.ArenaStyle

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId

internal data class HomeSeason(val name:String,val art:String,val color:Color)
internal fun homeSeason(month:YearMonth):HomeSeason = when((month.year*12+month.monthValue-1)%3) {
    1->HomeSeason("COROA GLACIAL","real_catedral",Color(0xFF9BE4FF))
    2->HomeSeason("IMPÉRIO DE CRISTAL","real_caverna",Color(0xFFCCAAFF))
    else->HomeSeason("FORJA DA ARENA","hero_forja",Color(0xFFFFB44D))
}

/** Two explicit pages. No automatic advance and no change to the player's equipped banner. */
@Composable
internal fun HomeShowcaseCarousel(
    bannerId:String,
    onOpenSeason:()->Unit,
    onCustomizeBanner:()->Unit,
    modifier:Modifier=Modifier,
    month:YearMonth=YearMonth.now(ZoneId.of("America/Sao_Paulo"))
) {
    val pager=rememberPagerState(pageCount={2})
    val coroutine=rememberCoroutineScope()
    val season=remember(month){homeSeason(month)}
    val shape=CutCornerShape(10.dp)
    Column(modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally) {
        HorizontalPager(state=pager,modifier=Modifier.fillMaxWidth().testTag("home_showcase_carousel"),
            pageSpacing=12.dp,beyondViewportPageCount=0) {page->
            val accent=if(page==0)season.color else Color(0xFF87CEFF)
            Box(Modifier.fillMaxWidth().height(148.dp).clip(shape)
                .background(Color(0xFF101521))
                .border(1.5.dp,Brush.linearGradient(listOf(accent,Color(0xFF283C52),accent)),shape)
                .clickable(role=Role.Button,onClick=if(page==0)onOpenSeason else onCustomizeBanner)
                .testTag(if(page==0)"season_button" else "home_banner_button")) {
                if(page==0)CosmeticScene(season.art,Modifier.fillMaxSize())
                else AnimatedBanner(bannerId,Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(
                    if(page==0)Brush.horizontalGradient(listOf(Color(0xFF040A12).copy(alpha=.84f),Color(0xFF040A12).copy(alpha=.05f)))
                    else Brush.verticalGradient(listOf(Color.Black.copy(alpha=.22f),Color.Transparent,Color.Black.copy(alpha=.72f)))))
                Column(Modifier.fillMaxSize().padding(14.dp),verticalArrangement=Arrangement.SpaceBetween) {
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        Icon(if(page==0)Icons.Default.WorkspacePremium else Icons.Default.Flag,null,tint=accent,modifier=Modifier.size(18.dp))
                        Text(if(page==0)"TEMPORADA" else "SEU BANNER",color=accent,fontSize=11.sp,fontWeight=FontWeight.Black)
                    }
                    if(page==0)Text(season.name,color=Color.White,fontSize=23.sp,lineHeight=25.sp,
                        fontWeight=FontWeight.Black,style=TextStyle(shadow=Shadow(Color.Black,blurRadius=8f)),
                        modifier=Modifier.fillMaxWidth(.76f))
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        if(page==1)Icon(Icons.Default.Edit,null,tint=Color.White,modifier=Modifier.size(16.dp))
                        Text(if(page==0)"Ver recompensas" else "Personalizar",color=Color.White,fontSize=12.sp,fontWeight=FontWeight.SemiBold)
                        Icon(Icons.Default.ChevronRight,null,tint=Color.White,modifier=Modifier.size(18.dp))
                    }
                }
            }
        }
        Row(Modifier.padding(top=4.dp),horizontalArrangement=Arrangement.spacedBy(2.dp)) {
            for(page in 0..1)Box(Modifier.size(32.dp)
                .semantics{contentDescription=if(page==0)"Mostrar temporada" else "Mostrar seu banner"}
                .clickable(role=Role.Button){coroutine.launch{pager.animateScrollToPage(page)}}
                .testTag("home_showcase_dot_$page"),contentAlignment=Alignment.Center) {
                Box(Modifier.size(if(pager.currentPage==page)8.dp else 6.dp).clip(CircleShape)
                    .background(if(pager.currentPage==page)ArenaStyle.Cyan else Color(0xFF526478)))
            }
        }
        Text("↔  Deslize para o lado",color=Color(0xFF92A9BC),fontSize=10.sp,lineHeight=12.sp)
    }
}
