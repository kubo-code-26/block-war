"use strict";
const test=require('node:test'),assert=require('node:assert/strict'),D=require('../domain'),S=require('../season');
const now=Date.parse('2026-10-23T15:00:00Z');
test('audit reward matrix: 16 CPU/online, win/loss and score combinations pay exact BW, coins and XP once',()=>{
 for(const online of [false,true])for(const won of [false,true])for(const score of [0,5,32,64]){
  const user=D.initialProfile('audit','uid',D.newPin('1234'));user.pontos_bw=100;
  const result={score,lines:7,attacks:12,maxCombo:3,combos:1};D.reward(user,'audit',result,won,online,false,now,90);
  assert.equal(user.pontos_bw,100+score+(won?20:-10));assert.equal(user.coins,100+(online?50:20));assert.equal(S.progress(user,'2026-10').xp,online?(won?30:10)+7:0);
  const before=JSON.stringify(user);D.reward(user,'audit',result,won,online,false,now,90);assert.equal(JSON.stringify(user),before);
 }
});
test('audit abandonment: only the penalty applies; verified score does not pay coins or season XP',()=>{
 for(const online of [false,true]){
  const user=D.initialProfile('audit','uid',D.newPin('1234'));user.pontos_bw=100;
  D.reward(user,'abandon',{score:64,lines:99,attacks:99,maxCombo:4},false,online,true,now,90);
  assert.equal(user.pontos_bw,90);assert.equal(user.coins,100);assert.equal(S.progress(user,'2026-10').xp,0);
 }
});
test('result receipt contains actual capped BW and exact season XP, including retries',()=>{
 const user=D.initialProfile('receipt','uid',D.newPin('1234'));
 D.reward(user,'loss',{score:0,lines:2,attacks:0},false,true,false,now,20);
 assert.equal(user.appliedMatchRewards.loss.awardedBw,0);assert.equal(user.appliedMatchRewards.loss.seasonXp,12);
 assert.equal(user.appliedMatchRewards.loss.blockoins,50);
 const before=JSON.stringify(user.appliedMatchRewards.loss);D.reward(user,'loss',{score:99,lines:9},true,true,false,now,20);
 assert.equal(JSON.stringify(user.appliedMatchRewards.loss),before);
});
