"use strict";
const test=require("node:test"),assert=require("node:assert/strict"),P=require("../player-profile"),D=require("../domain"),{CATALOG}=require("../catalog");
test("public player projection reveals only allowed fields and five newest verified online matches",()=>{
 const u=D.initialProfile("Ana","private-uid",D.newPin("1234"));u.email="private@example.com";u.googlePlayGamesId="secret";u.coins=999;u.onlineStats={totalWins:999999};
 for(let i=0;i<8;i++)D.reward(u,"m"+i,{score:i*5,lines:i,attacks:i,combos:1},i%2===0,true,false,10000+i*1000,30);
 D.reward(u,"cpu",{score:999,lines:99,attacks:99},true,false,false,999999,20);
 const before=JSON.stringify(u),v=P.project(u,CATALOG,{m7:"Bia"});assert.equal(JSON.stringify(u),before);
 assert.equal(v.statistics.matches,8);assert.equal(v.statistics.wins,4);assert.equal(v.history.length,5);assert.equal(v.history[0].opponent,"Bia");assert.equal(v.history[0].score,35);
 assert.deepEqual(v.history.map(x=>x.finishedAt),[17000,16000,15000,14000,13000]);
 for(const k of ["email","googlePlayGamesId","pin_scrypt","ownerUids","inventory","coins","appliedMatchRewards"])assert.ok(!(k in v));
 assert.ok(!JSON.stringify(v).includes("private-uid"));assert.ok(!("id" in v.history[0]));
 assert.equal(v.achievements.find(x=>x.id==="wins").unlocked,0);
});
test("new profiles have honest empty history and never expose unequipped or unowned cosmetics",()=>{
 const u=D.initialProfile("New","uid",D.newPin("1234"));u.inventory.equippedItems.avatares="avatar_forja";
 const v=P.project(u,CATALOG);assert.equal(v.avatar,"avatar_default");assert.deepEqual(v.history,[]);assert.equal(v.statistics.matches,0);assert.equal(v.statistics.winRate,0);
});
