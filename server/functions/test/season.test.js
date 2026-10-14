"use strict";
const test=require("node:test"),assert=require("node:assert/strict"),S=require("../season"),D=require("../domain");
test("temporadas alternam o tema, preservam histórico e não dão XP por CPU ou abandono",()=>{
  assert.notEqual(S.definition("2026-10").id,S.definition("2026-11").id);
  const u=D.initialProfile("ana","a",D.newPin("1234"));
  S.advance(u,"2026-10",{lines:5},true,false,false);assert.equal(u.seasons,undefined);
  S.advance(u,"2026-10",{lines:5},false,true,true);assert.equal(u.seasons,undefined);
  S.advance(u,"2026-10",{lines:5},true,true,false);assert.equal(S.progress(u,"2026-10").xp,35);
  assert.equal(S.progress(u,"2026-11").xp,0);
});
test("recompensas são idempotentes, exigem meta e não podem ser compradas por zero moedas",()=>{
  const u=D.initialProfile("ana","a",D.newPin("1234"));
  assert.throws(()=>S.claim(u,"2026-10",0));
  const exclusive=S.definition("2026-10").tiers[0].items[0];
  assert.equal(D.economy(u,{id:"cheat",kind:"BUY",itemId:exclusive}).accepted,false);
  u.seasons={"2026-10":{xp:1500,claimed:{}}};
  for(let i=0;i<4;i++){S.claim(u,"2026-10",i);S.claim(u,"2026-10",i)}
  assert.equal(new Set(u.inventory.boughtItems).size,u.inventory.boughtItems.length);
  assert.ok(u.inventory.boughtItems.includes(exclusive));assert.ok(S.progress(u,"2026-10").tiers.slice(0,4).every(t=>t.claimed));
});

test("forge has nine rewards, preserves original milestones and validates extra claims",()=>{
  const tiers=S.definition("2026-10").tiers;
  assert.deepEqual(tiers.map(t=>t.goal),[100,300,700,1500,2000,2500,3500]);
  assert.equal(new Set(tiers.flatMap(t=>t.items)).size,9);
  const u=D.initialProfile("ana","a",D.newPin("1234"));
  u.seasons={"2026-10":{xp:1500,claimed:{0:true,1:true,2:true,3:true}}};
  assert.throws(()=>S.claim(u,"2026-10",4));
  u.seasons["2026-10"].xp=3500;
  for(let i=4;i<tiers.length;i++){S.claim(u,"2026-10",i);S.claim(u,"2026-10",i);}
  assert.ok(S.progress(u,"2026-10").tiers.every(t=>t.claimed));
  for(const id of ["avatar_forja","fundo_forja","real_magma"])assert.equal(u.inventory.boughtItems.filter(x=>x===id).length,1);
  assert.throws(()=>S.claim(u,"2026-10",7));
  assert.throws(()=>S.claim(u,"2026-11",4));
});
