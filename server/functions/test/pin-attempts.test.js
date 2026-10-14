"use strict";
const test=require("node:test"),assert=require("node:assert/strict");
const {reserveAttempt,reserveProfileAttempt,profileKey,WINDOW_MS,MAX_ATTEMPTS}=require("../pin-attempts");
test("limite do perfil persiste mesmo com identidades diferentes e concorrentes",async()=>{
  const records=new Map();
  const paths=[];
  const db={ref(path){paths.push(path);return {async transaction(update){
    const next=update(records.get(path));
    if(next===undefined)return {committed:false};
    records.set(path,next);return {committed:true};
  }};}};
  const results=await Promise.all(Array.from({length:30},(_,i)=>
    reserveProfileAttempt(db,i%2?" Kender ":"kender",1000)));
  assert.equal(results.filter(Boolean).length,MAX_ATTEMPTS);
  assert.equal(new Set(paths).size,1);
  assert.equal(await reserveProfileAttempt(db,"outro",1000),true);
});
test("rejeições não prolongam bloqueio e janela expira no limite exato",()=>{
  let state;
  for(let i=0;i<MAX_ATTEMPTS;i++)state=reserveAttempt(state,1000+i);
  const before=structuredClone(state);
  assert.equal(reserveAttempt(state,1000+WINDOW_MS-1),undefined);
  assert.deepEqual(state,before);
  assert.deepEqual(reserveAttempt(state,1000+WINDOW_MS),{startedAt:1000+WINDOW_MS,count:1});
});
test("chave do perfil não expõe apelido e normaliza caixa e espaços",()=>{
  assert.equal(profileKey(" KENDER "),profileKey("kender"));
  assert.match(profileKey("kender"),/^[a-f0-9]{64}$/);
  assert.notEqual(profileKey("kender"),profileKey("outro"));
});
test("estado ausente ou janela futura reinicia orçamento",()=>{
  assert.deepEqual(reserveAttempt(null,1000),{startedAt:1000,count:1});
  assert.deepEqual(reserveAttempt({startedAt:2000,count:10},1000),{startedAt:1000,count:1});
});
