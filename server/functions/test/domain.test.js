"use strict";
const test=require("node:test"),assert=require("node:assert/strict"),D=require("../domain");
function user(){return D.initialProfile("Kender","uid",D.newPin("1234"));}
const score={score:16,lines:4,attacks:16,maxCombo:1};
test("100 blocões apenas na criação; vínculo é por UID",()=>{
  const u=user();assert.equal(u.coins,100);assert.equal(u.pontos_bw,0);assert.ok(D.owned(u,"uid"));assert.equal(D.owned(u,"other"),false);
});
test("PINs antigos e scrypt são verificados sem expor PIN no retorno",()=>{
  const u=user();assert.ok(D.verifyPin(u,"1234"));assert.equal(D.verifyPin(u,"0000"),false);
  assert.ok(D.verifyPin({pin_hash:D.key("" )||require("node:crypto").createHash("sha256").update("salt:1234").digest("hex"),pin_salt:"salt"},"1234"));
  assert.ok(D.verifyPin({pin:"1234"},"1234"));assert.equal(D.publicProfile(u).pin_scrypt,undefined);
});
test("recarga arbitrária é recusada e mantém saldo antigo",()=>{
  const u=user();u.coins=100655;u.inventory.coins=100655;
  assert.equal(D.economy(u,{id:"credit",kind:"CREDIT",amount:999999}).accepted,false);assert.equal(u.coins,100655);
});
test("compra usa preço do catálogo e é idempotente",()=>{
  const u=user();u.coins=1000;u.inventory.coins=1000;
  assert.ok(D.economy(u,{id:"buy",kind:"BUY",itemId:"gb",price:0}).accepted);
  assert.equal(u.coins,800);D.economy(u,{id:"buy",kind:"BUY",itemId:"gb"});assert.equal(u.coins,800);
  D.economy(u,{id:"second",kind:"BUY",itemId:"gb"});assert.equal(u.coins,800);
});
test("não é possível equipar item sem compra",()=>{
  const u=user();assert.equal(D.economy(u,{id:"equip",kind:"EQUIP",itemId:"gold"}).accepted,false);
});
test("vitória e derrota usam BW e blocões separados; saldo não fica negativo",()=>{
  const u=user();D.reward(u,"win",score,true,true,false,100000,50);assert.equal(u.pontos_bw,36);assert.equal(u.coins,150);
  D.reward(u,"loss",{...score,score:0},false,false,false,200000,50);assert.equal(u.pontos_bw,26);assert.equal(u.coins,170);
  D.reward(u,"again",{...score,score:0},false,true,true,300000,50);assert.equal(u.pontos_bw,16);assert.equal(u.coins,170);
  u.pontos_bw=1;D.reward(u,"last",{...score,score:0},false,true,true,400000,50);assert.equal(u.pontos_bw,0);
});
test("repetição do recibo não duplica BW, moedas, vitória, missão ou estatísticas",()=>{
  const u=user();D.reward(u,"once",score,true,true,false,100000,50);const before=structuredClone(u);
  assert.equal(D.reward(u,"once",score,true,true,false,100000,50),false);assert.deepEqual(u,before);
});
test("estatísticas incluem só online e usam BW da partida",()=>{
  const u=user();D.reward(u,"cpu",{...score,score:999},true,false,false,100000,20);
  D.reward(u,"online",score,true,true,false,200000,50);assert.equal(u.onlineStats.totalMatches,1);
  assert.equal(u.onlineStats.highScore,16);assert.equal(u.onlineStats.totalDurationSeconds,50);
});
test("sequência é cronológica mesmo com entregas fora de ordem",()=>{
  const u=user();D.reward(u,"third",score,true,true,false,300000,40);D.reward(u,"first",score,true,true,false,100000,50);
  D.reward(u,"second",score,false,true,false,200000,60);assert.equal(u.onlineStats.currentWinStreak,1);
  assert.equal(u.onlineStats.totalWins,2);assert.equal(u.onlineStats.fastestWinSeconds,40);
});
test("missões pagam uma vez e não aceitam recompensa definida pelo cliente",()=>{
  const u=user();D.reward(u,"win",score,true,false,false,Date.now(),20);
  const before=u.coins;D.missions(u,"daily_first_win");assert.equal(u.coins,before+75);
  D.missions(u,"daily_first_win");assert.equal(u.coins,before+75);
});
test("calendário respeita Brasília e segunda-feira",()=>{
  const p=D.periods(Date.parse("2026-10-13T01:00:00Z"));assert.equal(p.DAILY,"2026-10-12");assert.equal(p.WEEKLY,"2026-10-12");
});


test("combos online persistem uma vez, preservam legado e ignoram CPU",()=>{
  const u=user();
  D.reward(u,"legacy",score,true,true,false,100000,40);
  D.reward(u,"cpu-combo",{...score,combos:999},true,false,false,150000,40);
  D.reward(u,"new",{...score,combos:3},true,true,false,200000,40);
  D.reward(u,"new",{...score,combos:999},true,true,false,200000,40);
  assert.equal(u.onlineStats.totalMatches,2);
  assert.equal(u.onlineStats.totalCombos,3);
  assert.equal(u.appliedMatchRewards.new.onlineStatistics.combos,3);
  delete u.appliedMatchRewards.legacy.onlineStatistics.combos;
  D.statistics(u);assert.equal(u.onlineStats.totalCombos,3);
});
