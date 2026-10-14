"use strict";
const {onCall,HttpsError}=require("firebase-functions/v2/https");
const {onValueWritten}=require("firebase-functions/v2/database");
const admin=require("firebase-admin"),crypto=require("node:crypto");
admin.initializeApp(process.env.FUNCTIONS_EMULATOR==="true"?
  {projectId:"demo-block-war",databaseURL:"https://demo-block-war.firebaseio.com"}:undefined);
const db=admin.database();const D=require("./domain"),{verifyReplay}=require("./replay");
const {reserveProfileAttempt}=require("./pin-attempts");
const Rematch=require("./rematch");
const Friends=require("./friends");
const Season=require("./season");
const BoardGeometry=require("./board-geometry");
const Badges=require("./badges"),{CATALOG}=require("./catalog");
const options={region:"us-central1",maxInstances:3,memory:"512MiB",timeoutSeconds:60};
const fn=handler=>onCall(options,async request=>{
  if(!request.auth)throw new HttpsError("unauthenticated","Entre no perfil para continuar.");
  try{return await handler(request.data||{},request.auth.uid,request);}
  catch(e){if(e instanceof HttpsError)throw e;throw new HttpsError("failed-precondition",e.message||"Operação rejeitada.");}
});
function name(value) { const n=D.key(value);if(!D.validName(n))throw new HttpsError("invalid-argument","Nome inválido. Use até 12 caracteres, sem . # $ [ ] /.");return n; }
function appearance(user,category,fallback) {
  const id=user.inventory?.equippedItems?.[category];
  return CATALOG[id]?.category===category&&(user.inventory?.boughtItems||[]).includes(id)?id:fallback;
}
async function limit(uid,kind,max=30,window=300000) {
  const bucket=Math.floor(Date.now()/window);let accepted=false;
  await db.ref(`privateLimits/${kind}/${uid}`).transaction(x=>{
    const count=x?.bucket===bucket?x.count:0;
    accepted=count<max;if(!accepted)return;
    return {bucket,count:count+1};
  });
  if(!accepted)throw new HttpsError("resource-exhausted","Muitas tentativas. Aguarde alguns minutos.");
}
async function owned(nickname,uid) {
  const n=name(nickname),u=(await db.ref(`users/${n}`).get()).val();
  const active=(await db.ref(`activeProfiles/${uid}`).get()).val();
  if(!D.owned(u,uid)||active!==n)throw new HttpsError("permission-denied","Entre nesse perfil novamente com o PIN.");
  return [n,u];
}
async function protectProfilePin(n,user,uid) {
  // A confirmed Google account does not need to guess a PIN and remains usable
  // even while an attacker has exhausted this profile's public PIN budget.
  if(user?.firebaseAuthUid===uid)return;
  if(!await reserveProfileAttempt(db,n))throw new HttpsError("resource-exhausted",
    "Muitas tentativas para este perfil. Aguarde até 15 minutos ou entre com a conta Google vinculada.");
}
async function mutate(n,uid,action) {
  const ref=db.ref(`users/${n}`);let payload,failure;
  const tx=await ref.transaction(u=>{
    if(u===null)return null;
    if(!D.owned(u,uid))return;
    const before=JSON.stringify(u);
    try {payload=action(u);} catch(error) {failure=error;return;}
    if(JSON.stringify(u)!==before)u.revision=(Number(u.revision)||0)+1;
    return u;
  });
  if(failure)throw failure;
  if(!tx.committed||!tx.snapshot.exists())throw new HttpsError("permission-denied","Perfil não autorizado.");
  return {user:tx.snapshot.val(),payload};
}
async function provider(uid) {
  const user=await admin.auth().getUser(uid);
  const p=user.providerData.find(x=>x.providerId==="playgames.google.com");
  if(!p)throw new HttpsError("permission-denied","Autentique a conta do Play Games novamente.");
  return {id:p.uid,email:user.email||""};
}
exports.bwProfileExists=fn(async data=>({exists:(await db.ref(`users/${name(data.nickname)}`).get()).exists()}));
exports.bwProfileLogin=fn(async(data,uid)=>{
  await limit(uid,"login");const n=name(data.nickname),ref=db.ref(`users/${n}`);
  const previous=(await ref.get()).val();
  if(!previous&&!data.createIfMissing)throw new Error("Perfil não encontrado. Nenhuma conta foi criada.");
  if(previous)await protectProfilePin(n,previous,uid);
  const authorized=previous&&(previous.firebaseAuthUid===uid||D.verifyPin(previous,data.pin));
  if(previous&&!authorized)throw new HttpsError("permission-denied","PIN errado.");
  const fingerprint=previous?D.pinFingerprint(previous):null;
  const pin=previous?.pin_scrypt||(data.pin?D.newPin(data.pin):null);
  if(!previous&&!pin)throw new Error("Escolha um PIN de quatro números.");
  const tx=await ref.transaction(u=>{
    if(!u) {if(previous)return null;return D.initialProfile(data.nickname.trim(),uid,pin);}
    if(!previous||D.pinFingerprint(u)!==fingerprint)return;
    u.ownerUids??={};u.ownerUids[uid]=true;
    if(pin)u.pin_scrypt=pin;
    u.revision=(u.revision||0)+1;return u;
  });
  if(!tx.committed||!tx.snapshot.exists())throw new Error("O perfil mudou durante a operação. Tente novamente.");
  const oldProfile=(await db.ref(`activeProfiles/${uid}`).get()).val();
  if(oldProfile&&oldProfile!==n) {
    for(const member of ["p1","p2"]) {
      const rooms=await db.ref("partidas").orderByChild(`${member}Uid`).equalTo(uid).get();
      for(const entry of Object.values(rooms.val()||{})) {
        if(entry[`${member}Nickname`]!==oldProfile)continue;
        await db.ref(`partidas/${entry.code}`).transaction(r=>{
          if(r===null)return null;
          if(r[`${member}Uid`]!==uid||r[`${member}Nickname`]!==oldProfile)return;
          if(["aguardando_jogadores","buscando"].includes(r.estado)&&member==="p1"&&!r.p2Id)return null;
          if(r.estado==="em_jogo") {r[`${member}Connected`]=false;if(!r[`${member}DisconnectedAt`])r[`${member}DisconnectedAt`]=Date.now();}
          return r;
        });
      }
    }
  }
  await db.ref(`activeProfiles/${uid}`).set(n);
  return {...D.publicProfile(tx.snapshot.val()),pinVerified:D.verifyPin(tx.snapshot.val(),data.pin)};
});
exports.bwChangePin=fn(async(data,uid)=>{
  await limit(uid,"pin");const [n,u]=await owned(data.nickname,uid);
  await protectProfilePin(n,u,uid);
  if(u.firebaseAuthUid!==uid&&!D.verifyPin(u,data.oldPin))throw new Error("Não foi possível confirmar o PIN atual.");
  const fingerprint=D.pinFingerprint(u),pin=D.newPin(data.newPin);
  await mutate(n,uid,user=>{
    if(D.pinFingerprint(user)!==fingerprint)throw new Error("O PIN mudou. Entre novamente no perfil.");
    user.pin_scrypt=pin;delete user.pin;delete user.pin_hash;delete user.pin_salt;return true;
  });return {ok:true};
});
exports.bwSaveLayout=fn(async(data,uid)=>{
  const [n]=await owned(data.nickname,uid);const layout=data.layout;
  if(!layout||typeof layout!=="object"||JSON.stringify(layout).length>12000)throw new Error("Layout inválido.");
  await mutate(n,uid,u=>{u.layout=layout;return true;});return {ok:true};
});
exports.bwPlayFind=fn(async(_,uid)=>{
  await provider(uid);const a=(await db.ref(`playGamesAccounts/${uid}`).get()).val();
  if(!a?.nickname)return {nickname:null};
  const u=(await db.ref(`users/${D.key(a.nickname)}`).get()).val();
  return {nickname:u?.firebaseAuthUid===uid?a.nickname:null};
});
exports.bwPlayLink=fn(async(data,uid)=>{
  await limit(uid,"link");const p=await provider(uid),n=name(data.nickname);
  const u=(await db.ref(`users/${n}`).get()).val();if(!u)throw new Error("Perfil não encontrado.");
  if(u.firebaseAuthUid&&u.firebaseAuthUid!==uid)throw new Error("Esse perfil já está vinculado a outra conta Google.");
  await protectProfilePin(n,u,uid);
  if(u.firebaseAuthUid!==uid&&!D.verifyPin(u,data.pin))throw new Error("Entre no perfil com o PIN correto antes de vincular.");
  const map=db.ref(`playGamesAccounts/${uid}`);
  const claim=await map.transaction(a=>a&&D.key(a.nickname)!==n?undefined:{nickname:u.nickname});
  if(!claim.committed)throw new Error("Esta conta Google já está vinculada a outro perfil.");
  const fingerprint=D.pinFingerprint(u);
  const tx=await db.ref(`users/${n}`).transaction(user=>{
    if(user===null)return null;
    if(!user||D.pinFingerprint(user)!==fingerprint||(user.firebaseAuthUid&&user.firebaseAuthUid!==uid))return;
    user.ownerUids??={};user.ownerUids[uid]=true;
    user.firebaseAuthUid=uid;user.google_play_id=p.id;user.googlePlayGamesId=p.id;if(p.email)user.email=p.email;
    user.revision=(user.revision||0)+1;return user;
  });
  if(!tx.committed) {
    const current=(await db.ref(`users/${n}`).get()).val();
    if(current?.firebaseAuthUid&&current.firebaseAuthUid!==uid)
      await map.transaction(a=>D.key(a?.nickname)===n?null:undefined);
    throw new Error("O perfil mudou durante o vínculo. Tente novamente.");
  }
  await db.ref(`activeProfiles/${uid}`).set(n);
  return {ok:true};
});
exports.bwEconomy=fn(async(data,uid)=>{
  const [n]=await owned(data.nickname,uid);const {payload}=await mutate(n,uid,u=>D.economy(u,data));return payload;
});
exports.bwMissions=fn(async(data,uid)=>{
  const [n]=await owned(data.nickname,uid);
  const {user,payload}=await mutate(n,uid,u=>D.missions(u,data.claimId||null));
  // Client progress/events/reward definitions are deliberately ignored.
  return {missions:payload,coins:user.coins??user.inventory?.coins??100};
});
exports.bwSeason=fn(async(data,uid)=>{
  await limit(uid,"season",30,60000);
  const [n]=await owned(data.nickname,uid),period=D.periods().MONTHLY;
  const {user}=await mutate(n,uid,u=>data.claim===undefined?null:Season.claim(u,period,data.claim));
  const raw=(await db.ref(`seasonLeaderboards/${period}`).orderByChild("xp").limitToLast(50).get()).val()||{};
  const leaderboard=Object.values(raw).sort((a,b)=>b.xp-a.xp||a.nickname.localeCompare(b.nickname));
  // Only the public, owned cosmetic IDs of the three podium participants leave the server.
  const podium=await Promise.all(leaderboard.slice(0,3).map(async row=>{
    const profile=(await db.ref(`users/${name(row.nickname)}`).get()).val()||{};
    return {...row,avatar:appearance(profile,"avatares","avatar_default"),frame:appearance(profile,"molduras","none")};
  }));
  return {...Season.progress(user,period),leaderboard:[...podium,...leaderboard.slice(3)]};
});
exports.bwEquipBadge=fn(async(data,uid)=>{
  await limit(uid,"badge",20,60000);
  const [n]=await owned(data.nickname,uid);
  const {user}=await mutate(n,uid,u=>{u.featuredBadge=Badges.select(u,data.category,data.tier,CATALOG);return true;});
  return {featuredBadge:user.featuredBadge||null};
});
function roomCode(code) {if(!/^[A-Z0-9]{4}$/.test(code||""))throw new Error("Código de sala inválido.");return code;}
function slot(room,uid) { if(room.p1Uid===uid)return "p1";if(room.p2Uid===uid)return "p2";throw new Error("Você não participa dessa sala."); }
function result(room,loser,reason,now) {
  if(room.estado!=="em_jogo")return;
  const prefix=room.p1Id===loser?"p1":room.p2Id===loser?"p2":null;if(!prefix)throw new Error("Jogador inválido.");
  room.estado="finalizada";room.loserId=loser;room.vencedor=prefix==="p1"?room.p2Id:room.p1Id;
  room.finishReason=reason;room.finishedAt=now;room[`${prefix}GameOver`]=true;
}
function disconnect(room,reconnecting,now) {
  if(room.estado!=="em_jogo")return;
  const missing=["p1","p2"].filter(p=>{
    const since=room[`${p}HasConnected`] ? (room[`${p}ArmDisconnectedAt`] || room[`${p}DisconnectedAt`]) : room[`${p}JoinedAt`];
    return !room[`${p}Connected`]&&since>0&&now-since>=30000;
  });
  if(missing.length===2) {room.estado="cancelada";room.finishedAt=now;room.finishReason="ambos_desconectados";}
  else if(missing.length===1)result(room,room[`${missing[0]}Id`],"desconexao",now);
}
exports.bwCreateRoom=fn(async(data,uid)=>{
  await limit(uid,"room",20,60000);const active=(await db.ref(`activeProfiles/${uid}`).get()).val();const [,profile]=await owned(active,uid);
  const code=roomCode(data.roomCode);if(!/^P_[a-zA-Z0-9_-]{1,100}$/.test(data.playerId||""))throw new Error("Jogador inválido.");
  const boardCols=BoardGeometry.columns(data.boardCols);
  const tx=await db.ref(`partidas/${code}`).transaction(r=>r?undefined:{code,protocolVersion:3,boardCols,quickMatch:data.isPrivate!==true,
    estado:"aguardando_jogadores",p1Id:data.playerId,p1Uid:uid,p1Nickname:active,p1Badge:Badges.read(profile,CATALOG),
    p1Avatar:appearance(profile,"avatares","avatar_default"),p1Skin:appearance(profile,"blocos","neon"),p1Theme:appearance(profile,"tabuleiros","tab_neon"),p1Connected:false,
    p1DisconnectedAt:0,p1JoinedAt:Date.now(),p1HasConnected:false,p2Id:""});
  if(!tx.committed)throw new Error("Código de sala já utilizado.");return {ok:true};
});
async function joinRoom(data,uid) {
  const active=(await db.ref(`activeProfiles/${uid}`).get()).val();const [,profile]=await owned(active,uid);
  const code=roomCode(data.roomCode);
  const tx=await db.ref(`partidas/${code}`).transaction(r=>{
    if(r===null)return null;
    if(!BoardGeometry.compatible(r,data.boardCols))throw new Error("Sala usa outro tamanho de tabuleiro. Os dois jogadores precisam da mesma vers\u00e3o do jogo.");
    if((r.invitedNickname&&r.invitedNickname!==active)||(r.invitedUid&&r.invitedUid!==uid)||(r.expiresAt&&r.expiresAt<Date.now()))return;
    if(r.protocolVersion!==3||!r.p1Connected||r.p1Uid===uid||r.p1Id===data.playerId||r.p1Nickname===active)return;
    if(r.p2Uid===uid&&r.p2Id===data.playerId&&r.estado==="em_jogo")return r;
    if(!["buscando","aguardando_jogadores"].includes(r.estado)||r.p2Id)return;
    r.p2Id=data.playerId;r.p2Uid=uid;r.p2Nickname=active;r.p2Badge=Badges.read(profile,CATALOG);r.p2Connected=false;r.p2DisconnectedAt=0;
    r.p2Avatar=appearance(profile,"avatares","avatar_default");r.p2Skin=appearance(profile,"blocos","neon");r.p2Theme=appearance(profile,"tabuleiros","tab_neon");
    r.p2JoinedAt=Date.now();r.p2HasConnected=false;r.estado="em_jogo";return r;
  });if(!tx.committed||!tx.snapshot.exists())throw new Error("Sala indisponível");return {ok:true};
}
exports.bwJoinRoom=fn(joinRoom);
exports.bwRematch=fn(async(data,uid)=>{
  await limit(uid,"rematch",10,60000);
  const active=(await db.ref(`activeProfiles/${uid}`).get()).val();await owned(active,uid);
  const ref=db.ref(`partidas/${roomCode(data.roomCode)}`),now=Date.now();
  const generated=Array.from({length:4},()=>"ABCDEFGHJKLMNPQRSTUVWXYZ23456789"[crypto.randomInt(32)]).join("");
  const tx=await ref.transaction(r=>{
    const p=r?.p1Uid===uid?"p1":r?.p2Uid===uid?"p2":null;
    if(!p||r[`${p}Nickname`]!==active)throw new Error("Entre no perfil usado na partida.");
    if(!BoardGeometry.compatible(r,data.boardCols))throw new Error("Crie uma nova sala com a vers\u00e3o atual do jogo.");
    return Rematch.reserve(r,uid,now,generated);
  });
  const old=tx.snapshot.val(),reservation=old.rematch;
  const p=old.p1Uid===uid?"p1":"p2";
  if(old[`${p}Nickname`]!==active)throw new Error("Entre novamente no perfil usado na partida.");
  const target=db.ref(`partidas/${reservation.code}`);
  const created=await target.transaction(r=>r===null?Rematch.createRoom(old,reservation.code,now):
    r.rematchOf===old.code&&r.p1Uid===reservation.hostUid?r:undefined);
  if(!created.committed)throw new Error("Não foi possível reservar a revanche. Crie uma sala privada.");
  const isHost=reservation.hostUid===uid;
  if(!isHost)await joinRoom({roomCode:reservation.code,playerId:old[`${p}Id`],boardCols:BoardGeometry.columns(old.boardCols)},uid);
  return {roomCode:reservation.code,isHost};
});
exports.bwFindQuickRoom=fn(async(data,uid)=>{
  const rooms=await db.ref("partidas").orderByChild("quickMatch").equalTo(true).get();
  for(const r of Object.values(rooms.val()||{}).sort(()=>Math.random()-0.5)) {
    if(!r.p1Connected||r.estado!=="aguardando_jogadores"||r.p2Id||r.p1Uid===uid||!BoardGeometry.compatible(r,data.boardCols))continue;
    try {await joinRoom({...data,roomCode:r.code},uid);return {roomCode:r.code};}
    catch(error) {if(!String(error.message).includes("Sala indisponível"))throw error;}
  }
  return {roomCode:null};
});
exports.bwCancelJoinedRoom=fn(async(data,uid)=>{
  await db.ref(`partidas/${roomCode(data.roomCode)}`).transaction(r=>{
    if(r===null)return null;
    if(r.p2Uid!==uid||r.p2Id!==data.playerId||r.p2Connected||r.secureSessions?.[uid])return;
    r.estado="cancelada";r.finishReason="entrada_cancelada";r.finishedAt=Date.now();return r;
  });return {ok:true};
});
exports.bwPreparePresence=fn(async(data,uid)=>{
  const ref=db.ref(`partidas/${roomCode(data.roomCode)}`),old=(await ref.get()).val();
  if(!old)throw new Error("Sala não encontrada.");await owned(old[`${slot(old,uid)}Nickname`],uid);
  const tx=await ref.transaction(r=>{
    if(r===null)return null;
    const p=r.p1Uid===uid?"p1":r.p2Uid===uid?"p2":null;
    if(!p||r[`${p}Id`]!==data.playerId)return;
    disconnect(r,null,Date.now());
    if(!["finalizada","cancelada"].includes(r.estado)) {
      if(!r[`${p}Connected`]&&r[`${p}HasConnected`])
        r[`${p}ArmDisconnectedAt`]=r[`${p}ArmDisconnectedAt`]||r[`${p}DisconnectedAt`]||Date.now();
      r[`${p}DisconnectedAt`]=0;
    }
    return r;
  });
  if(!tx.committed||!tx.snapshot.exists()||["finalizada","cancelada"].includes(tx.snapshot.val().estado))throw new Error("Sala encerrada ou indisponível.");
  return {ok:true};
});
exports.bwRoomPresence=fn(async(data,uid)=>{
  const old=(await db.ref(`partidas/${roomCode(data.roomCode)}`).get()).val();
  if(!old)throw new Error("Sala não encontrada.");await owned(old[`${slot(old,uid)}Nickname`],uid);
  const tx=await db.ref(`partidas/${roomCode(data.roomCode)}`).transaction(r=>{
    if(r===null)return null;const p=slot(r,uid);if(r[`${p}Id`]!==data.playerId)return;
    disconnect(r,p,Date.now());
    if(!["finalizada","cancelada"].includes(r.estado)){r[`${p}Connected`]=true;r[`${p}DisconnectedAt`]=0;r[`${p}ArmDisconnectedAt`]=0;r[`${p}HasConnected`]=true;}return r;
  });if(!tx.committed||!tx.snapshot.exists())throw new Error("Sala indisponível.");return {ok:true};
});
exports.bwRoomTimeout=fn(async(data,uid)=>{
  const old=(await db.ref(`partidas/${roomCode(data.roomCode)}`).get()).val();
  if(!old)throw new Error("Sala não encontrada.");await owned(old[`${slot(old,uid)}Nickname`],uid);
  await db.ref(`partidas/${roomCode(data.roomCode)}`).transaction(r=>{if(r===null)return null;slot(r,uid);disconnect(r,null,Date.now());return r;});return {ok:true};
});
exports.bwDeleteRoom=fn(async(data,uid)=>{
  const old=(await db.ref(`partidas/${roomCode(data.roomCode)}`).get()).val();
  if(!old)return {ok:true};await owned(old.p1Nickname,uid);
  await db.ref(`partidas/${roomCode(data.roomCode)}`).transaction(r=>{
    if(!r)return null;if(r.p1Uid!==uid||r.p2Id||!["aguardando_jogadores","buscando"].includes(r.estado))return;
    return null;
  });return {ok:true};
});
async function session(id,uid) {
  if(!/^[a-zA-Z0-9_-]{1,100}$/.test(id||""))throw new Error("Partida inválida.");
  const s=(await db.ref(`privateMatches/${id}`).get()).val();
  if(!s)throw new Error("Partida sem registro verificável no servidor.");
  // Linking an anonymous/PIN account to Play Games changes its Auth UID, not the match owner.
  if(s.uid!==uid)await owned(s.nickname,uid);
  return s;
}
async function replay(s,events) {
  const attacks=s.roomCode?(await db.ref(`privateAttacks/${s.roomCode}/${s.uid}`).get()).val()||{}:{};
  const v=verifyReplay(s,events,attacks);
  if(v.lastEventMillis>Date.now()-s.startedAt+5000 || v.player.locks>Math.floor((Date.now()-s.startedAt)/20)+10)
    throw new Error("Tempo de partida inválido.");
  return v;
}
exports.bwBeginMatch=fn(async(data,uid)=>{
  await limit(uid,"match",30,60000);const [n]=await owned(data.nickname,uid);
  const boardCols=BoardGeometry.columns(data.boardCols);
  const mode=data.mode==="ONLINE"?"ONLINE":"CPU",code=mode==="ONLINE"?roomCode(data.roomCode):"";
  if(code) {
    const r=(await db.ref(`partidas/${code}`).get()).val();if(!r||r.estado!=="em_jogo")throw new Error("Sala não iniciou.");
    if(!BoardGeometry.compatible(r,boardCols))throw new Error("Sala incompat\u00edvel com esta vers\u00e3o do tabuleiro.");
    const p=slot(r,uid);if(r[`${p}Nickname`]!==n)throw new Error("O perfil mudou depois da entrada na sala.");
    const previous=r.secureSessions?.[uid];if(previous){const s=await session(previous,uid);return {id:previous,...s};}
  }
  const difficulty=["EASY","MEDIUM","HARD"].includes(data.difficulty)?data.difficulty:"MEDIUM";
  const id=crypto.randomUUID(),s={uid,nickname:n,mode,boardCols,cpuVersion:data.cpuVersion===2?2:1,roomCode:code,difficulty,startedAt:Date.now(),seed:crypto.randomInt(1,2147483647),opponentSeed:crypto.randomInt(1,2147483647)};
  await db.ref(`privateMatches/${id}`).set(s);
  if(code)await db.ref(`partidas/${code}/secureSessions/${uid}`).set(id);
  return {id,...s};
});
exports.bwSendAttack=fn(async(data,uid)=>{
  const s=await session(data.matchId,uid);if(s.mode!=="ONLINE")throw new Error("Partida online inválida.");
  const verified=await replay(s,data.events),code=s.roomCode;
  let target;
  const ref=db.ref(`partidas/${code}`);
  const tx=await ref.transaction(r=>{
    if(r===null)return null;
    if(r.estado!=="em_jogo")return;const p=slot(r,uid),other=p==="p1"?"p2":"p1";
    const sent=r.verifiedAttackTotals?.[uid]||0,delta=verified.player.attacks-sent;
    if(delta<=0)return r;
    r.verifiedAttackTotals??={};r.verifiedAttackTotals[uid]=verified.player.attacks;
    const sender=slot(r,uid);r[`${sender}Score`]=verified.player.score;r[`${sender}Lines`]=verified.player.lines;
    r.pendingVerifiedAttacks??={};
    for(let cursor=sent;cursor<verified.player.attacks;) {
      const blocks=Math.min(16,verified.player.attacks-cursor);cursor+=blocks;
      target={uid:r[`${other}Uid`],id:`${data.matchId}_${cursor}`,blocks,queue:other==="p1"?"attacksForP1":"attacksForP2"};
      r.pendingVerifiedAttacks[target.id]=target;
    }
    return r;
  });
  if(!tx.committed||!tx.snapshot.exists())throw new Error("Sala encerrada.");
  // Outbox persists atomically with the consumed attack budget. Repeated calls retry delivery.
  return {ok:true};
});
exports.bwDeliverAttack=onValueWritten({ref:"/partidas/{code}/pendingVerifiedAttacks/{id}",region:"us-central1",
  maxInstances:1,concurrency:1,retry:false},async event=>{
  const message=event.data.after.val();if(!message)return;
  const {code,id}=event.params;
  const receipt=db.ref(`privateAttacks/${code}/${message.uid}/${id}`);
  if(!(await receipt.get()).exists())
    await db.ref().update({[`privateAttacks/${code}/${message.uid}/${id}`]:{blocks:message.blocks},
      [`partidas/${code}/${message.queue}/${id}`]:message.blocks});
  await db.ref(`partidas/${code}/pendingVerifiedAttacks/${id}`).remove();
});
exports.bwFinishRoom=fn(async(data,uid)=>{
  const code=roomCode(data.roomCode),ref=db.ref(`partidas/${code}`),r=(await ref.get()).val();
  if(!r)return {ok:true};
  let p;
  try {p=slot(r,uid);} catch(e) {
    p=r.p1Id===data.playerId?"p1":r.p2Id===data.playerId?"p2":null;
    if(!p)throw e;await owned(r[`${p}Nickname`],uid);
  }
  if(r[`${p}Id`]!==data.playerId)throw new Error("Jogador inválido.");
  await owned(r[`${p}Nickname`],uid);
  if(["finalizada","cancelada"].includes(r.estado))return {ok:true};
  const reason=["abandono","processo_encerrado"].includes(data.reason)?"abandono":"ko";
  if(reason==="ko") {
    const s=await session(data.matchId,uid),v=await replay(s,data.events);
    if(s.roomCode!==code||!v.player.dead)throw new Error("Derrota ainda não confirmada pelo tabuleiro.");
  }
  await ref.transaction(room=>{if(room===null)return null;if(room[`${p}Id`]!==data.playerId)return;result(room,data.playerId,reason,Date.now());return room;});return {ok:true};
});
exports.bwSettleMatch=fn(async(data,uid)=>{
  const s=await session(data.matchId,uid),[n,u]=await owned(s.nickname,uid);
  if(!["CPU","ONLINE"].includes(s.mode))throw new Error("Este resultado pertence ao desafio diário, sem prêmio de partida comum.");
  if(u.appliedMatchRewards?.[data.matchId])return {...D.publicProfile(u),rewardReceipt:{matchId:data.matchId,...u.appliedMatchRewards[data.matchId]}};
  let won,forfeit=false,finishedAt=Date.now();
  const v=await replay(s,data.events);
  if(s.mode==="ONLINE") {
    const r=(await db.ref(`partidas/${s.roomCode}`).get()).val();
    if(!r||r.estado!=="finalizada")throw new Error("Aguardando o resultado confirmado da sala.");
    const p=slot(r,s.uid);won=r.vencedor===r[`${p}Id`];finishedAt=r.finishedAt;
    forfeit=!won&&["desconexao","abandono"].includes(r.finishReason);
  } else {
    if(!v.player.dead&&!v.opponent.dead)throw new Error("A partida ainda não terminou no replay.");
    won=!v.player.dead&&v.opponent.dead;
  }
  const duration=Math.max(1,Math.floor((finishedAt-s.startedAt)/1000));
  const {user}=await mutate(n,uid,user=>D.reward(user,data.matchId,v.player,won,s.mode==="ONLINE",forfeit,finishedAt,duration));
  return {...D.publicProfile(user),rewardReceipt:{matchId:data.matchId,...user.appliedMatchRewards[data.matchId]}};
});
exports.bwPublishRanking=onValueWritten({ref:"/users/{nickname}",region:"us-central1",maxInstances:2},async event=>{
  const u=event.data.after.val(),n=event.params.nickname,ref=db.ref(`ranking_public/${n}`);
  if(!u){await ref.remove();return;}
  const before=event.data.before.val();
  if(before&&before.nickname===u.nickname&&before.pontos_bw===u.pontos_bw&&before.total_vitorias===u.total_vitorias)return;
  await ref.transaction(old=>old&&(old.revision||0)>(u.revision||0)?undefined:
    {nickname:u.nickname||n,pontos_bw:u.pontos_bw||0,total_vitorias:u.total_vitorias||0,revision:u.revision||0});
});
exports.bwPublishSeason=onValueWritten({ref:"/users/{nickname}/seasons/{period}",region:"us-central1",maxInstances:2},async event=>{
  const value=event.data.after.val(),{nickname,period}=event.params,ref=db.ref(`seasonLeaderboards/${period}/${nickname}`);
  if(!value){await ref.remove();return;}
  await ref.transaction(old=>old&&(old.xp||0)>(value.xp||0)?undefined:{nickname,xp:value.xp||0});
});

// Relationships are private, profile-owned and mutated only through this callable.
exports.bwFriends=fn(async(data,uid)=>{
 const [me]=await owned(data.nickname,uid),action=data.action||"list";
 const readRows=async n=>{
  const values=await Promise.all(["a","b"].map(key=>db.ref("friendships").orderByChild(key).equalTo(n).limitToFirst(220).get()));
  return values.flatMap(snapshot=>Object.values(snapshot.val()||{}));
 };
 const list=async()=>{
  const rows=(await readRows(me)).map(row=>Friends.visible(row,me));
  return {contacts:await Promise.all(rows.filter(Boolean).map(async row=>{
   const u=(await db.ref(`users/${row.nickname}`).get()).val();
   return {...row,displayName:u?.nickname||row.nickname,avatar:appearance(u||{},"avatares","avatar_default")};
  }))};
 };
 if(action==="list"){await limit(uid,"friendsRead",60,60000);return list();}
 if(!["request","accept","reject","cancel","remove","block","unblock","dismissInvite","invite"].includes(action))throw new Error("Ação de amizade inválida.");
 await limit(uid,"friendsWrite",20,60000);
 const other=name(data.target),id=Friends.pair(me,other);
 if(!(await db.ref(`users/${other}`).get()).exists())throw new Error("Jogador não encontrado.");
 const ref=db.ref(`friendships/${id}`);
 if(action==="request"||action==="block") {
  for(const n of [me,other]) {
   const rows=await readRows(n);
   if(!(await ref.get()).exists()&&rows.length>=200)throw new Error("Limite de contatos atingido.");
  }
 }
 if(action==="invite") {
  const row=(await ref.get()).val();
  if(row?.status!=="accepted"||Object.keys(row.blockedBy||{}).length)throw new Error("Adicione este jogador aos amigos primeiro.");
  const code=roomCode(data.roomCode),now=Date.now();
  const room=await db.ref(`partidas/${code}`).transaction(r=>{
   if(r===null)return null;
   if(r.p1Uid!==uid||r.p1Nickname!==me||r.quickMatch!==false||r.p2Id||r.estado!=="aguardando_jogadores")return;
   if(r.invitedNickname&&r.invitedNickname!==other)return;
   return {...r,invitedNickname:other,expiresAt:now+300000};
  });
  if(!room.committed||!room.snapshot.exists())throw new Error("Crie uma sala privada disponível para convidar.");
  const tx=await ref.transaction(r=>r===null?null:r.status==="accepted"&&!Object.keys(r.blockedBy||{}).length?
   {...r,invitation:{code,from:me,expiresAt:now+300000}}:undefined);
  if(!tx.committed||!tx.snapshot.exists())throw new Error("A amizade não está mais disponível.");
 } else {
  let failure;
  const changed=await ref.transaction(row=>{
   if(row===null&&!['request','block'].includes(action))return null;
   try {return Friends.change(row,me,other,action);}catch(error){failure=error;return;}
  });
  if(failure)throw failure;
  if(action==='accept'&&!changed.snapshot.exists())throw new Error("Pedido de amizade indisponível.");
 }
 return list();
});

exports.bwPlayerProfile=fn(async(data,uid)=>{
 const [viewer]=await owned(data.nickname,uid),target=name(data.target);
 await limit(uid,"playerProfileRead",40,60000);
 if(viewer!==target) {
  const relation=(await db.ref(`friendships/${Friends.pair(viewer,target)}`).get()).val();
  if(Object.keys(relation?.blockedBy||{}).length)throw new Error("Perfil indisponível.");
 }
 const user=(await db.ref(`users/${target}`).get()).val();
 if(!user)throw new Error("Perfil indisponível.");
 const PublicPlayer=require("./player-profile"),opponents={};
 await Promise.all(PublicPlayer.recent(user).map(async([id])=>{
  const session=(await db.ref(`privateMatches/${id}`).get()).val();
  if(session?.nickname!==target||session?.mode!=="ONLINE"||!session.roomCode)return;
  const room=(await db.ref(`partidas/${session.roomCode}`).get()).val();
  if(room?.p1Nickname===target)opponents[id]=room.p2Nickname;
  else if(room?.p2Nickname===target)opponents[id]=room.p1Nickname;
 }));
 return PublicPlayer.project(user,CATALOG,opponents);
});
