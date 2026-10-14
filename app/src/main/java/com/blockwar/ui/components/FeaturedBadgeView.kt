package com.blockwar.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blockwar.achievements.AchievementCatalog
import com.blockwar.model.FeaturedBadge

@Composable fun FeaturedBadgeView(badge:FeaturedBadge?,compact:Boolean=false) {
    if(badge==null)return
    val tier=AchievementCatalog.tiers.getOrNull(badge.tier)?:return
    val category=AchievementCatalog.categories.firstOrNull{it.id==badge.category}?:return
    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        AchievementShield(badge.category,badge.tier,true,Modifier.size(if(compact)22.dp else 36.dp),allowAnimation=!compact)
        if(!compact)Text("${category.name} · ${tier.name}",color=Color(tier.color),fontSize=12.sp)
    }
}
