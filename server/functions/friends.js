"use strict";
const crypto=require("node:crypto");
function pair(a,b){if(a===b)throw new Error("Escolha outro jogador.");return crypto.createHash("sha256").update(JSON.stringify([a,b].sort())).digest("hex");}
function change(old,me,other,action,now=Date.now()) {
 const row=old?structuredClone(old):{a:[me,other].sort()[0],b:[me,other].sort()[1]};
 if(![row.a,row.b].includes(me)||![row.a,row.b].includes(other))throw new Error("Relação inválida.");
 const blocked=row.blockedBy||{};
 if(action==="block")return {...row,status:"blocked",blockedBy:{...blocked,[me]:true},invitation:null,requestedBy:null};
 if(action==="unblock") {delete blocked[me];return Object.keys(blocked).length?{...row,blockedBy:blocked}:null;}
 if(Object.keys(blocked).length)throw new Error("Esta solicitação não está disponível.");
 if(action==="request") {
  if(row.status==="accepted")return row;
  if(row.status==="pending"&&row.requestedBy!==me)return {...row,status:"accepted",requestedBy:null,acceptedAt:now};
  return {...row,status:"pending",requestedBy:me,createdAt:row.createdAt||now};
 }
 if(action==="accept") {
  if(row.status==="accepted")return row;
  if(row.status!=="pending"||row.requestedBy===me)throw new Error("Pedido de amizade indisponível.");
  return {...row,status:"accepted",requestedBy:null,acceptedAt:now};
 }
 if(action==="reject") {if(row.status==="pending"&&row.requestedBy!==me)return null;return old;}
 if(action==="cancel") {if(row.status==="pending"&&row.requestedBy===me)return null;return old;}
 if(action==="remove")return row.status==="accepted"?null:old;
 if(action==="dismissInvite") {if(row.invitation?.from!==me)row.invitation=null;return row;}
 throw new Error("Ação de amizade inválida.");
}
function visible(row,me,now=Date.now()) {
 if(!row)return null;
 const other=row.a===me?row.b:row.a;
 if(row.blockedBy?.[other])return null;
 return {nickname:other,status:row.blockedBy?.[me]?"blocked":row.status==="accepted"?"friend":row.requestedBy===me?"outgoing":"incoming",
  invitation:row.status==="accepted"&&row.invitation?.from!==me&&row.invitation?.expiresAt>now?{code:row.invitation.code,expiresAt:row.invitation.expiresAt}:null};
}
module.exports={pair,change,visible};
