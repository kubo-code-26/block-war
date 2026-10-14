package com.blockwar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.blockwar.ui.theme.ArenaStyle

@Composable
internal fun ArenaHeader(
    title: String,
    onBack: () -> Unit,
    icon: ImageVector? = null,
    backTag: String = "arena_back",
    actions: @Composable RowScope.() -> Unit = {}
) {
    Column(Modifier.fillMaxWidth().background(ArenaStyle.Recessed)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 68.dp).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val shape = CutCornerShape(7.dp)
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)
                .background(ArenaStyle.Surface, shape)
                .border(1.dp, ArenaStyle.Outline.copy(alpha = .6f), shape).testTag(backTag)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = ArenaStyle.Text)
            }
            if (icon != null) Icon(icon, null, tint = ArenaStyle.Cyan, modifier = Modifier.size(24.dp))
            Text(title, modifier = Modifier.weight(1f), color = ArenaStyle.Text,
                style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            actions()
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Brush.horizontalGradient(
            listOf(ArenaStyle.Cyan.copy(alpha = .35f), ArenaStyle.Outline.copy(alpha = .2f)))))
    }
}
