package com.blockwar.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blockwar.R

@Composable
fun BlockWarLogo(modifier: Modifier = Modifier) {
    Image(painter = painterResource(R.drawable.block_war_logo), contentDescription = "Block War",
        modifier = modifier, contentScale = ContentScale.Fit)
}

@Composable
fun BlockoinIcon(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Image(painter = painterResource(R.drawable.blockoin_coin), contentDescription = "Blocões",
        modifier = modifier.size(size), contentScale = ContentScale.Fit)
}
