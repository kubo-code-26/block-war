"use strict";
const test=require("node:test"),assert=require("node:assert/strict");
const {Sequence,verifyReplay,spawn}=require("../replay");
const session={seed:1,opponentSeed:2,mode:"ONLINE"};
function line() {
  const n=new Sequence(1).next(4)+1, count=10-n;
  const pieces=Array.from({length:count},(_,i)=>({id:i+1,size:1,col:i,y:-1-i,target:19}));
  const events=[{kind:"wave",actor:0,t:0,receipt:"attack",pieces}];
  for(let i=0;i<21+count;i++)events.push({kind:"step",actor:0,t:i+1,active:spawn(n)});
  events.push({kind:"lock",actor:0,t:100,type:n,x:count,y:19,r:0},{kind:"commit",actor:0,t:300});
  return {events,allowed:{attack:{blocks:count}}};
}
test("gerador tem sequência compartilhável e determinística",()=>{
  const a=new Sequence(1),b=new Sequence(1);assert.deepEqual(Array.from({length:30},()=>a.next(4)),Array.from({length:30},()=>b.next(4)));
});
test("servidor reconstrói tabuleiro e calcula cinco BW sem usar score informado",()=>{
  const {events,allowed}=line();events[events.length-2].score=999999;
  const r=verifyReplay(session,events,allowed);assert.equal(r.player.score,5);assert.equal(r.player.lines,1);assert.equal(r.player.attacks,5);
});
test("peça inventada e posição fora do tabuleiro são recusadas",()=>{
  const {events,allowed}=line();events[events.length-2].type=events[events.length-2].type===4?1:4;
  assert.throws(()=>verifyReplay(session,events,allowed));
  const other=line();other.events[other.events.length-2].x=20;assert.throws(()=>verifyReplay(session,other.events,other.allowed));
});
test("ataque cinza sem recibo do servidor não é aceito",()=>{
  const {events}=line();assert.throws(()=>verifyReplay(session,events,{}),/Ataque sem confirmação/);
});
test("o mesmo ataque não pode entrar duas vezes no replay",()=>{
  const {events,allowed}=line();events.push({...events[0],t:400});assert.throws(()=>verifyReplay(session,events,allowed),/Ataque sem confirmação/);
});
test("travamento suspenso e compromisso sem peça são recusados",()=>{
  const n=new Sequence(1).next(4)+1;
  assert.throws(()=>verifyReplay(session,[{kind:"lock",actor:0,t:1,type:n,x:Math.floor((10-n)/2),y:3,r:0}]));
  assert.throws(()=>verifyReplay(session,[{kind:"commit",actor:0,t:1}]));
});
test("cliente não controla o adversário online",()=>{
  assert.throws(()=>verifyReplay(session,[{kind:"step",actor:1,t:1}]),/não controla/);
});
test("CPU segue a decisão calculada no servidor e rejeita aceleração",()=>{
  const s={seed:1,opponentSeed:2,mode:"CPU",difficulty:"MEDIUM"};
  const {cpuDecision}=require("../replay"),type=new Sequence(2).next(4)+1;
  const d=cpuDecision(Array.from({length:20},()=>Array(10).fill(0)),type,"MEDIUM",new Sequence(2^18523));
  const decision={kind:"decision",actor:1,t:0,targetX:d.targetX,rotation:d.rotation};
  assert.doesNotThrow(()=>verifyReplay(s,[decision]));
  assert.throws(()=>verifyReplay(s,[{...decision,targetX:d.targetX+1}]),/Decisão/);
  if(d.rotation===0&&d.targetX!==spawn(type).x) {
    const dx=Math.sign(d.targetX-spawn(type).x);
    assert.throws(()=>verifyReplay(s,[decision,{kind:"move",actor:1,t:0,dx,dy:0},{kind:"move",actor:1,t:1,dx,dy:0}]),/tempo/);
  }
});
test("timestamps invertidos e lote exagerado são recusados",()=>{
  assert.throws(()=>verifyReplay(session,[{kind:"step",actor:0,t:2},{kind:"step",actor:0,t:1}]));
  assert.throws(()=>verifyReplay(session,Array(50001).fill({kind:"step",actor:0,t:1})));
});


test("três limpezas consecutivas formam um combo verificado, sem aceitar contador do cliente",()=>{
  const sequence=new Sequence(1),types=Array.from({length:4},()=>sequence.next(4)+1);
  const events=[],allowed={};
  for(let k=0;k<3;k++) {
    const n=types[k],count=10-n,t=k*500,receipt="combo-"+k;
    const pieces=Array.from({length:count},(_,i)=>({id:i+1,size:1,col:i,y:-1-i,target:19}));
    allowed[receipt]={blocks:count};events.push({kind:"wave",actor:0,t,receipt,pieces});
    for(let i=0;i<21+count;i++)events.push({kind:"step",actor:0,t:t+i+1,active:spawn(n)});
    events.push({kind:"lock",actor:0,t:t+100,type:n,x:count,y:19,r:0},
      {kind:"commit",actor:0,t:t+300,combos:999999});
  }
  const result=verifyReplay(session,events,allowed).player;
  assert.equal(result.lines,3);assert.equal(result.maxCombo,3);assert.equal(result.combos,1);
  const one=line();assert.equal(verifyReplay(session,one.events,one.allowed).player.combos,0);
});
