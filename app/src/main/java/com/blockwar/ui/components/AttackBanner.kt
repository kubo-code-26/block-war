package com.blockwar.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blockwar.model.AttackEvent
import kotlinx.coroutines.delay

@Composable
fun AttackBanner(
    attackEvents: List<AttackEvent>,
    modifier: Modifier = Modifier
) {
    val latestEvent = attackEvents.lastOrNull()
    var visible by remember { mutableStateOf(false) }
    var currentDisplayEvent by remember { mutableStateOf<AttackEvent?>(null) }

    LaunchedEffect(latestEvent) {
        if (latestEvent != null) {
            currentDisplayEvent = latestEvent
            visible = true
            delay(1800)
            visible = false
        }
    }

    AnimatedVisibility(
        visible = visible && currentDisplayEvent != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier.testTag("attack_banner")
    ) {
        val event = currentDisplayEvent ?: return@AnimatedVisibility
        val isPlayerAttack = event.fromPlayer

        val bannerColor = if (isPlayerAttack) Color(0xFF00E5FF) else Color(0xFFFF1744)
        val textMessage = if (isPlayerAttack) {
            when (event.blocks) {
                7 -> "ATAQUE 75%! +7 BLOCOS (1x2 / 1x3)"
                15 -> "ATAQUE DUPLO! +15 BLOCOS (1x2 / 1x3)"
                22 -> "ATAQUE TRIPLO! +22 BLOCOS (1x2 / 1x3)"
                30 -> "BLOCK WAR 75%! +30 BLOCOS (1x2 / 1x3)"
                else -> "ATAQUE 75%! +${event.blocks} BLOCOS (1x2 / 1x3)"
            }
        } else {
            "ALERTA! +${event.blocks} BLOCOS (1x2 / 1x3) CAINDO!"
        }

        Box(
            modifier = Modifier
                .shadow(10.dp, RoundedCornerShape(20.dp), spotColor = bannerColor)
                .border(
                    width = 2.dp,
                    brush = Brush.horizontalGradient(
                        listOf(bannerColor, Color.White, bannerColor)
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0D101E))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isPlayerAttack) Icons.Default.Bolt else Icons.Default.Warning,
                    contentDescription = null,
                    tint = bannerColor
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = textMessage,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
