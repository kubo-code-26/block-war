"use strict";
const test=require("node:test"),assert=require("node:assert/strict"),R=require("../rematch");
const room=()=>({code:"ABCD",estado:"finalizada",finishedAt:1000,p1Uid:"a",p2Uid:"b",p1Id:"P_a",p2Id:"P_b",p1Nickname:"ana",p2Nickname:"bia"});
test("dois jogadores recebem a mesma reserva, privada e dirigida ao adversário",()=>{
  const old=R.reserve(room(),"b",1100,"EFGH");
  R.reserve(old,"a",1200,"ZZZZ");
  const next=R.createRoom(old,old.rematch.code,1200);
  assert.equal(next.code,"EFGH");assert.equal(next.p1Uid,"b");assert.equal(next.invitedUid,"a");
  assert.equal(next.quickMatch,false);assert.equal(next.p2Id,"");assert.equal(next.estado,"aguardando_jogadores");
});
test("terceiros, partidas ativas e convites antigos não podem gerar revanche",()=>{
  assert.throws(()=>R.reserve(room(),"stranger",1100,"ABCD"));
  assert.throws(()=>R.reserve({...room(),estado:"em_jogo"},"a",1100,"ABCD"));
  assert.throws(()=>R.reserve(room(),"a",1001+R.TTL,"ABCD"));
});
