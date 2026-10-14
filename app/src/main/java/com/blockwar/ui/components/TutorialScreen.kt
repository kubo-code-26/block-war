package com.blockwar.ui.components

import com.blockwar.ui.theme.ArenaStyle

import com.blockwar.model.PlayerState

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.blockwar.tutorial.TutorialSession
import kotlin.math.abs

private val TutorialCyan = ArenaStyle.Cyan
private val SessionSaver = listSaver<TutorialSession, Int>(
    save={ listOf(it.lesson,it.x,it.rotation,if(it.completed) 1 else 0,if(it.retry) 1 else 0) },
    restore={ TutorialSession(it[0],it[1],it[2],it[3]==1,it[4]==1) }
)

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun TutorialScreen(onClose: (completed: Boolean) -> Unit) {
    var started by rememberSaveable { mutableStateOf(false) }
    var session by rememberSaveable(stateSaver=SessionSaver) { mutableStateOf(TutorialSession()) }
    val scrollState=rememberScrollState()
    LaunchedEffect(session.lesson,started) { scrollState.scrollTo(0) }
    Dialog(onDismissRequest={onClose(false)},properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
        Column(Modifier.fillMaxSize().background(ArenaStyle.Background).appSafeDrawingPadding().testTag("tutorial_screen")) {
            ArenaHeader("APRENDA A JOGAR", {onClose(false)}) {
                TextButton(onClick={onClose(false)},modifier=Modifier.testTag("tutorial_skip")) { Text("Pular",color=TutorialCyan) }
            }
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scrollState).padding(horizontal=20.dp,vertical=12.dp),
                horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)) {
                if(!started) {
                    Text("Sua primeira batalha começa aqui",color=Color.White,style=MaterialTheme.typography.headlineSmall)
                    Text("Pratique no seu ritmo: mova, gire e complete linhas para enviar ataques. São três desafios curtos, sem cronômetro.",color=ArenaStyle.Muted)
                    Text("O treino não altera seu saldo, ranking ou conquistas. Você pode repeti-lo nos Ajustes.",color=ArenaStyle.Muted)
                    Button(onClick={started=true},modifier=Modifier.fillMaxWidth().testTag("tutorial_start")) { Text("Começar treino") }
                } else {
                    Text("PASSO ${session.lesson+1} DE 3",color=TutorialCyan,fontWeight=FontWeight.Bold)
                    Text(when(session.lesson){0->"Mova e encaixe";1->"Gire para caber";else->"Transforme linhas em ataques"},
                        color=Color.White,style=MaterialTheme.typography.titleLarge)
                    Text(when(session.lesson){
                        0->"Mova a peça para a direita até a sombra ocupar o espaço vazio. Depois toque em Soltar."
                        1->"Gire a peça e alinhe-a com a abertura vertical para limpar três linhas."
                        else->"Alinhe a peça com a abertura. Limpar uma linha com sua peça envia 5 blocos de ataque."
                    },color=ArenaStyle.Muted)
                    TutorialBoard(session,onMove={session=session.move(it)},onRotate={session=session.rotate()},onDrop={session=session.drop()})
                    if(session.completed) {
                        Text("${session.clearedLines} linha(s) limpa(s) · ${session.attack} blocos de ataque",color=TutorialCyan,
                            modifier=Modifier.testTag("tutorial_success"))
                        if(session.lesson==2) {
                            Text("Treino concluído! Os blocos cinzas recebidos não geram contra-ataque sozinhos. Agora é com você!",color=Color.White)
                        }
                        Button(onClick={if(session.lesson==2) onClose(true) else session=session.next()},
                            modifier=Modifier.fillMaxWidth().testTag("tutorial_next")) {
                            Text(if(session.lesson==2) "Concluir tutorial" else "Próximo desafio")
                        }
                    } else {
                        if(session.retry) Text("Quase! A peça voltou ao início. Use a sombra para alinhar com a abertura e tente outra vez.",color=Color(0xFFFFC947))
                        FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick={session=session.move(-1)},modifier=Modifier.widthIn(min=80.dp).testTag("tutorial_left")) { Text("←",color=TutorialCyan,modifier=Modifier.semantics{contentDescription="Mover para a esquerda"}) }
                            OutlinedButton(onClick={session=session.rotate()},modifier=Modifier.widthIn(min=80.dp).testTag("tutorial_rotate")) { Text("Girar",color=TutorialCyan) }
                            OutlinedButton(onClick={session=session.move(1)},modifier=Modifier.widthIn(min=80.dp).testTag("tutorial_right")) { Text("→",color=TutorialCyan,modifier=Modifier.semantics{contentDescription="Mover para a direita"}) }
                        }
                        Button(onClick={session=session.drop()},modifier=Modifier.fillMaxWidth().testTag("tutorial_drop")) { Text("Soltar") }
                        Text("No tabuleiro: arraste para os lados, toque para girar ou deslize rapidamente para baixo para soltar. Os botões também funcionam neste treino.",color=ArenaStyle.Muted,style=MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun TutorialBoard(session:TutorialSession,onMove:(Int)->Unit,onRotate:()->Unit,onDrop:()->Unit) {
    val move by rememberUpdatedState(onMove)
    val rotate by rememberUpdatedState(onRotate)
    val drop by rememberUpdatedState(onDrop)
    val enabled by rememberUpdatedState(!session.completed)
    Canvas(Modifier.widthIn(max=180.dp).fillMaxWidth().aspectRatio(PlayerState.BOARD_COLS.toFloat()/PlayerState.BOARD_ROWS).background(Color(0xFF080C14))
        .testTag("tutorial_board").semantics {contentDescription="Tabuleiro de treino. Use os botões abaixo para mover, girar e soltar a peça."}
        .pointerInput(Unit) {detectTapGestures(onTap={if(enabled) rotate()})}
        .pointerInput(Unit) {
            var dx=0f;var dy=0f;var began=0L
            detectDragGestures(onDragStart={dx=0f;dy=0f;began=System.currentTimeMillis()},onDragEnd={
                if(enabled) {
                    val threshold=24.dp.toPx()
                    if(abs(dx)>abs(dy)&&abs(dx)>threshold) move(if(dx>0) 1 else -1)
                    else if(dy>threshold&&System.currentTimeMillis()-began<350) drop()
                }
            },onDrag={change,amount->change.consume();dx+=amount.x;dy+=amount.y})
        }) {
        val cell=size.width/PlayerState.BOARD_COLS
        val board=session.displayBoard
        for(row in 0 until PlayerState.BOARD_ROWS) for(col in 0 until PlayerState.BOARD_COLS) {
            val value=board[row][col]
            val origin=Offset(col*cell+1,row*cell+1)
            drawRect(value?.color?:Color(0xFF17202C),origin,Size(cell-2,cell-2),style=if(value==null) Stroke(1f) else androidx.compose.ui.graphics.drawscope.Fill)
        }
        if(!session.completed) {
            for((x,y) in session.landing.getOccupiedCoordinates()) if(y>=0) {
                drawRect(TutorialCyan.copy(alpha=.65f),Offset(x*cell+1,y*cell+1),Size(cell-2,cell-2),style=Stroke(2f))
            }
            for((x,y) in session.piece.getOccupiedCoordinates()) if(y>=0) {
                drawRect(session.type.primaryColor,Offset(x*cell+1,y*cell+1),Size(cell-2,cell-2))
            }
        }
    }
}
