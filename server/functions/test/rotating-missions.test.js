"use strict";
const test=require("node:test"),assert=require("node:assert/strict");
const D=require("../domain"),{rotatingMissions}=require("../rotating-missions");
const at=value=>Date.parse(value);
const current=at("2026-10-21T12:00:00Z");
test("três tarefas extras por período, estáveis no mesmo dia e diferentes no próximo",()=>{
  const list=rotatingMissions(D.periods(current));
  for(const type of ["DAILY","WEEKLY","MONTHLY"])assert.equal(list.filter(m=>m.type===type).length,3);
  assert.equal(new Set(list.map(m=>m.id)).size,9);
  assert.deepEqual(list,rotatingMissions(D.periods(current+1000)));
  const changed=(type,next)=>assert.notDeepEqual(list.filter(m=>m.type===type).map(m=>m.id),
    rotatingMissions(D.periods(at(next))).filter(m=>m.type===type).map(m=>m.id));
  changed("DAILY","2026-10-22T12:00:00Z");
  changed("WEEKLY","2026-10-28T12:00:00Z");
  changed("MONTHLY","2026-11-21T12:00:00Z");
});
test("missões extras usam resultados verificados, não pagam por abandono e não duplicam prêmio",()=>{
  const u=D.initialProfile("Kender","uid",D.newPin("1234"));
  const result={score:16,lines:10000,attacks:10000,maxCombo:20};
  D.reward(u,"forfeit",result,false,true,true,current,60);
  assert.ok(D.missions(u,null,current).filter(m=>m.metric).every(m=>m.progress===0));
  D.reward(u,"valid",result,true,true,false,current+1,60);
  const extra=D.missions(u,null,current).find(m=>m.metric&&m.isCompleted);
  assert.ok(extra);
  const before=u.coins;
  D.missions(u,extra.id,current);assert.equal(u.coins,before+extra.reward);
  D.missions(u,extra.id,current);assert.equal(u.coins,before+extra.reward);
  D.reward(u,"valid",result,true,true,false,current+1,60);
  assert.equal(u.coins,before+extra.reward);
});
test("virada em Brasília preserva o histórico e reinicia o novo período",()=>{
  const u=D.initialProfile("Kender","uid",D.newPin("1234"));
  const before=at("2026-10-22T02:59:59Z"),after=before+1000;
  D.reward(u,"one",{score:0,lines:50,attacks:100,maxCombo:5},true,true,false,before,60);
  const saved=structuredClone(u.missions.DAILY[D.periods(before).DAILY]);
  const fresh=D.missions(u,null,after).filter(m=>m.type==="DAILY");
  assert.ok(fresh.every(m=>m.progress===0&&!m.isClaimed));
  assert.deepEqual(u.missions.DAILY[D.periods(before).DAILY],saved);
  assert.equal(D.missions(u,null,after).length,18);
});
