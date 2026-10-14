"use strict";
const TTL=10*60*1000;
function reserve(room,uid,now,code) {
  if(!room||room.estado!=="finalizada"||![room.p1Uid,room.p2Uid].includes(uid))throw new Error("Revanche disponível apenas aos participantes de uma partida encerrada.");
  if(!room.finishedAt||now-room.finishedAt>TTL)throw new Error("O convite de revanche expirou. Crie uma sala privada.");
  if(!room.rematch||now-room.rematch.createdAt>TTL)room.rematch={code,hostUid:uid,
    invitedUid:uid===room.p1Uid?room.p2Uid:room.p1Uid,createdAt:now};
  return room;
}
function createRoom(old,code,now) {
  const p=old.rematch.hostUid===old.p1Uid?"p1":"p2";
  return {code,protocolVersion:3,boardCols:require("./board-geometry").columns(old.boardCols),quickMatch:false,estado:"aguardando_jogadores",rematchOf:old.code,
    invitedUid:old.rematch.invitedUid,expiresAt:old.rematch.createdAt+TTL,
    p1Id:old[`${p}Id`],p1Uid:old[`${p}Uid`],p1Nickname:old[`${p}Nickname`],p1Badge:old[`${p}Badge`]||null,
    p1Skin:old[`${p}Skin`]||"neon",p1Theme:old[`${p}Theme`]||"tab_neon",
    p1Connected:false,p1DisconnectedAt:0,p1JoinedAt:now,p1HasConnected:false,p2Id:""};
}
module.exports={reserve,createRoom,TTL};
