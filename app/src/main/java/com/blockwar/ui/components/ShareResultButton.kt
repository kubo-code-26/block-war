package com.blockwar.ui.components

import com.blockwar.ui.theme.ArenaStyle

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.blockwar.model.GameMode
import com.blockwar.model.MatchStatus
import com.blockwar.model.VersusGameState
import com.blockwar.social.ResultCard
import com.blockwar.social.ResultCardData
import kotlinx.coroutines.launch

@Composable fun ShareResultButton(state:VersusGameState,seconds:Long,attacks:Int,modifier:Modifier=Modifier,compact:Boolean=false) {
    ShareCardButton(ResultCardData(state.nickname,state.matchStatus==MatchStatus.VICTORY,state.player.score,
        state.player.linesCleared,attacks,state.player.maxCombo,seconds,
        if(state.gameMode==GameMode.ONLINE_1V1)"BATALHA ONLINE" else "BATALHA VS CPU",state.featuredBadge),modifier,compact)
}
@Composable fun ShareCardButton(data:ResultCardData,modifier:Modifier=Modifier,compact:Boolean=false) {
    val context=LocalContext.current.applicationContext
    val scope=rememberCoroutineScope();var busy by remember{mutableStateOf(false)}
    OutlinedButton(enabled=!busy,modifier=modifier.fillMaxWidth().testTag("share_result_button"),onClick={
        scope.launch {
            busy=true
            try {
                ResultCard.share(context,data)
            } catch(cancelled:kotlinx.coroutines.CancellationException){throw cancelled}
            catch(_:Exception){Toast.makeText(context,"Não foi possível abrir o compartilhamento. Tente novamente.",Toast.LENGTH_LONG).show()}
            finally{busy=false}
        }
    }){Text(if(busy)"Preparando cartão…" else if(compact)"Compartilhar" else "COMPARTILHAR RESULTADO",color=ArenaStyle.Cyan)}
}
