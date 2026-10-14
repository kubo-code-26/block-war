package com.blockwar.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blockwar.network.EngagementBackend
import com.blockwar.ui.theme.ArenaStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

data class FriendContact(val nickname:String,val displayName:String,val status:String,val avatar:String,val invitation:String?)
class FriendsViewModel:ViewModel() {
    var contacts by mutableStateOf<List<FriendContact>>(emptyList());private set
    var busy by mutableStateOf(false);private set
    var message by mutableStateOf("");private set
    fun call(owner:String,action:String="list",target:String="") {
        if(busy||owner.isBlank())return
        busy=true
        if(action!="list")message=""
        EngagementBackend.call("bwFriends",mapOf("nickname" to owner,"target" to target.trim(),"action" to action),{data->
            contacts=(data["contacts"] as? List<*>)?.mapNotNull { value ->
                val r=value as? Map<*,*>?:return@mapNotNull null
                val n=r["nickname"] as? String?:return@mapNotNull null
                FriendContact(n,r["displayName"]?.toString()?:n,r["status"]?.toString()?:"outgoing",
                    r["avatar"]?.toString()?:"avatar_default",(r["invitation"] as? Map<*,*>)?.get("code") as? String)
            }.orEmpty()
            busy=false
            if(action!="list")message=when(action){"request"->"Pedido confirmado. Confira a lista abaixo.";"accept"->"Amizade confirmada!";"block"->"Jogador bloqueado.";"unblock"->"Jogador desbloqueado.";else->"Lista atualizada."}
        },{busy=false;message=it})
    }
}
@Composable internal fun friendsModel(owner:String):FriendsViewModel = viewModel(key="friends-${owner.lowercase(Locale.ROOT)}")

@Composable internal fun AddOpponentFriendButton(owner:String,target:String) {
    val vm=friendsModel(owner)
    LaunchedEffect(owner,target){vm.call(owner)}
    val relation=vm.contacts.firstOrNull{it.nickname.equals(target,true)}?.status
    val available=relation !in listOf("friend","outgoing","blocked")
    Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally) {
        OutlinedButton(onClick={vm.call(owner,if(relation=="incoming")"accept" else "request",target)},enabled=available&&!vm.busy,
            modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("add_opponent_friend"),shape=CutCornerShape(6.dp)) {
            Icon(Icons.Default.PersonAdd,null,modifier=Modifier.size(20.dp));Spacer(Modifier.width(6.dp))
            Text(when {vm.busy->"AGUARDE…";relation=="friend"->"VOCÊS JÁ SÃO AMIGOS";relation=="outgoing"->"PEDIDO ENVIADO";relation=="blocked"->"JOGADOR BLOQUEADO";relation=="incoming"->"ACEITAR AMIZADE";else->"ADICIONAR AMIGO"},fontSize=12.sp)
        }
        if(vm.message.isNotBlank())Text(vm.message,color=ArenaStyle.Muted,fontSize=11.sp,modifier=Modifier.padding(top=4.dp))
    }
}

@Composable internal fun FriendsScreen(owner:String,onClose:()->Unit,onInvite:(String)->Unit,onJoin:(String)->Unit) {
    val vm=friendsModel(owner)
    var confirmation by remember {mutableStateOf<Pair<String,String>?>(null)}
    var selectedPlayer by remember {mutableStateOf<String?>(null)}
    val lifecycleOwner=LocalLifecycleOwner.current
    LaunchedEffect(owner,lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while(isActive){vm.call(owner);delay(10000)}
        }
    }
    Dialog(onDismissRequest=onClose,properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
        Box(Modifier.fillMaxSize().background(ArenaStyle.Background).appSafeDrawingPadding().imePadding()) {
            FriendsPageContent(vm.contacts,vm.busy,vm.message,onClose,{vm.call(owner)},
                {vm.call(owner,"request",it)},
                {action,target->if(action in listOf("block","remove"))confirmation=action to target else vm.call(owner,action,target)},
                onInvite,onJoin,{selectedPlayer=it})
        }
    }
    selectedPlayer?.let{target->PublicPlayerDialog(owner,target,{selectedPlayer=null},{selectedPlayer=null;onInvite(it)})}
    confirmation?.let { (action,target)->
        AlertDialog(onDismissRequest={confirmation=null},title={Text(if(action=="block")"Bloquear jogador?" else "Remover amizade?")},
            text={Text(if(action=="block")"Vocês deixarão de ser amigos e novos pedidos ficarão bloqueados." else "Você poderá enviar um novo pedido depois.")},
            confirmButton={TextButton(onClick={confirmation=null;vm.call(owner,action,target)}){Text("Confirmar")}},
            dismissButton={TextButton(onClick={confirmation=null}){Text("Cancelar")}})
    }
}

@Composable internal fun FriendsPageContent(
    contacts:List<FriendContact>,busy:Boolean,message:String,onClose:()->Unit,onRefresh:()->Unit,onRequest:(String)->Unit,
    onAction:(String,String)->Unit,onInvite:(String)->Unit,onJoin:(String)->Unit,onProfile:(String)->Unit
) {
    var query by androidx.compose.runtime.saveable.rememberSaveable {mutableStateOf("")}
    var tab by androidx.compose.runtime.saveable.rememberSaveable {mutableIntStateOf(0)}
    val friends=contacts.filter{it.status=="friend"}
    val requests=contacts.filter{it.status in listOf("incoming","outgoing")}
    val incoming=requests.count{it.status=="incoming"}
    val blocked=contacts.filter{it.status=="blocked"}
    val rows=when(tab){0->friends;1->requests;else->blocked}
    Column(Modifier.fillMaxSize().testTag("friends_screen")) {
        ArenaHeader("AMIGOS",onClose,Icons.Default.People) {
            IconButton(onClick=onRefresh,enabled=!busy){Icon(Icons.Default.Refresh,"Atualizar amigos",tint=ArenaStyle.Cyan)}
        }
        if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
        LazyColumn(Modifier.weight(1f).fillMaxWidth(),contentPadding=PaddingValues(12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            item {
                Row(Modifier.fillMaxWidth().friendPanel().padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.People,null,tint=ArenaStyle.Cyan,modifier=Modifier.size(34.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Sua próxima batalha tem companhia",color=ArenaStyle.Text,fontWeight=FontWeight.Bold,fontSize=15.sp)
                        Text("Adicione jogadores e convide para a arena.",color=ArenaStyle.Muted,fontSize=12.sp)
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value=query,onValueChange={query=it.take(12)},label={Text("Apelido do jogador",fontSize=12.sp)},singleLine=true,
                        modifier=Modifier.weight(1f).testTag("friend_nickname"),enabled=!busy,shape=CutCornerShape(6.dp))
                    FilledIconButton(onClick={onRequest(query)},enabled=query.isNotBlank()&&!busy,
                        shape=CutCornerShape(6.dp),modifier=Modifier.size(56.dp).testTag("send_friend_request")) {
                        Icon(Icons.Default.PersonAdd,"Enviar pedido de amizade")
                    }
                }
            }
            if(message.isNotBlank())item {Text(message,color=ArenaStyle.Muted,fontSize=12.sp)}
            item {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                    listOf("Amigos ${friends.size}","Pedidos${if(incoming>0)" $incoming" else ""}","Bloqueados").forEachIndexed{i,label->
                        Box(Modifier.weight(1f).heightIn(min=48.dp).friendPanel(if(tab==i)ArenaStyle.Cyan else ArenaStyle.Outline)
                            .clickable(role=androidx.compose.ui.semantics.Role.Tab){tab=i}.padding(7.dp).testTag("friends_tab_$i"),contentAlignment=Alignment.Center) {
                            Text(label,color=if(tab==i)ArenaStyle.Cyan else ArenaStyle.Muted,fontSize=11.sp,fontWeight=FontWeight.Bold)
                        }
                    }
                }
            }
            if(tab==0)items(friends.filter{it.invitation!=null},key={"invitation-${it.nickname}"}){friend->
                Column(Modifier.fillMaxWidth().friendPanel(ArenaStyle.Purple).padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.SportsEsports,null,tint=ArenaStyle.Purple,modifier=Modifier.size(28.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${friend.displayName} convidou você",color=ArenaStyle.Text,fontWeight=FontWeight.Bold)
                            Text("Sala privada · convite temporário",color=ArenaStyle.Muted,fontSize=11.sp)
                        }
                    }
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        Button(onClick={friend.invitation?.let(onJoin)},enabled=!busy,modifier=Modifier.weight(1f)){Text("ENTRAR NA SALA",fontSize=11.sp)}
                        TextButton(onClick={onAction("dismissInvite",friend.nickname)},enabled=!busy){Text("Dispensar",fontSize=11.sp)}
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal=4.dp,vertical=5.dp),horizontalArrangement=Arrangement.SpaceBetween) {
                    Text(when(tab){0->"SUA EQUIPE";1->"PEDIDOS DE AMIZADE";else->"JOGADORES BLOQUEADOS"},color=ArenaStyle.Text,fontWeight=FontWeight.Bold,fontSize=12.sp)
                    Text(rows.size.toString(),color=ArenaStyle.Muted,fontSize=12.sp)
                }
            }
            if(rows.isEmpty())item {
                Column(Modifier.fillMaxWidth().friendPanel().padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Icon(if(tab==2)Icons.Default.Block else Icons.Default.PeopleOutline,null,tint=ArenaStyle.Muted,modifier=Modifier.size(32.dp))
                    Text(if(busy)"Carregando…" else when(tab){0->"Sua equipe começa com um convite.";1->"Nenhum pedido pendente.";else->"Nenhum jogador bloqueado."},color=ArenaStyle.Muted,fontSize=12.sp)
                }
            }
            items(rows,key={it.nickname}){friend->
                var menu by remember(friend.nickname){mutableStateOf(false)}
                Column(Modifier.fillMaxWidth().friendPanel().padding(10.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(48.dp).clickable(enabled=friend.status!="blocked",role=androidx.compose.ui.semantics.Role.Button,onClickLabel="Ver perfil de ${friend.displayName}"){onProfile(friend.nickname)}.testTag("friend_avatar_${friend.nickname}"),contentAlignment=Alignment.Center) {
                            AvatarWithFrame(friend.avatar,"moldura_simple",42.dp)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(friend.displayName,color=ArenaStyle.Text,fontWeight=FontWeight.Bold,fontSize=14.sp)
                            Text(when(friend.status){"incoming"->"Quer adicionar você";"outgoing"->"Aguardando resposta";"blocked"->"Bloqueado";else->"Amigo"},color=ArenaStyle.Muted,fontSize=11.sp)
                        }
                        if(friend.status=="friend")OutlinedButton(onClick={onInvite(friend.nickname)},enabled=!busy,contentPadding=PaddingValues(horizontal=10.dp),shape=CutCornerShape(6.dp)) {Text("CONVIDAR",fontSize=10.sp,fontWeight=FontWeight.Bold)}
                        if(friend.status!="blocked")Box {
                            IconButton(onClick={menu=true},enabled=!busy,modifier=Modifier.testTag("friend_menu_${friend.nickname}")){Icon(Icons.Default.MoreVert,"Opções de ${friend.displayName}",tint=ArenaStyle.Muted)}
                            DropdownMenu(expanded=menu,onDismissRequest={menu=false}) {
                                if(friend.status=="friend")DropdownMenuItem(text={Text("Remover amizade")},onClick={menu=false;onAction("remove",friend.nickname)})
                                DropdownMenuItem(text={Text("Bloquear jogador")},onClick={menu=false;onAction("block",friend.nickname)})
                            }
                        }
                    }
                    when(friend.status) {
                        "incoming"->Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            Button(onClick={onAction("accept",friend.nickname)},enabled=!busy,modifier=Modifier.weight(1f)){Text("ACEITAR")}
                            TextButton(onClick={onAction("reject",friend.nickname)},enabled=!busy){Text("Recusar")}
                        }
                        "outgoing"->TextButton(onClick={onAction("cancel",friend.nickname)},enabled=!busy){Text("Cancelar pedido")}
                        "blocked"->TextButton(onClick={onAction("unblock",friend.nickname)},enabled=!busy){Text("Desbloquear")}
                    }
                }
            }
            item {Spacer(Modifier.height(8.dp))}
        }
    }
}
private fun Modifier.friendPanel(outline:androidx.compose.ui.graphics.Color=ArenaStyle.Outline)=background(ArenaStyle.SurfaceLow,CutCornerShape(8.dp))
    .border(1.dp,outline.copy(alpha=.7f),CutCornerShape(8.dp))
