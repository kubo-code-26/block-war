"use strict";
const assert=require('node:assert/strict'),fs=require('node:fs'),{initializeTestEnvironment}=require('@firebase/rules-unit-testing');
const {ref,get,update}=require('firebase/database'),{initializeApp,deleteApp}=require('firebase/app'),{getAuth,connectAuthEmulator,signInAnonymously}=require('firebase/auth'),{getFunctions,connectFunctionsEmulator,httpsCallable}=require('firebase/functions'),R=require('./replay'),D=require('./domain');
const project='demo-block-war',address=process.env.FIREBASE_DATABASE_EMULATOR_HOST;
if(!address||!['127.0.0.1','localhost'].includes(address.split(':')[0]))throw Error('Local demo emulator required');
const [host,port]=address.split(':');
(async()=>{
const env=await initializeTestEnvironment({projectId:project,database:{host,port:Number(port),rules:fs.readFileSync('database.rules.json','utf8')}}),apps=[];
let namespace=project;
async function databaseRequest(key,method='GET',body){
 const r=await fetch(`http://${host}:${port}/${key}.json?ns=${namespace}`,{method,headers:{Authorization:'Bearer owner','Content-Type':'application/json'},...(body===undefined?{}:{body:JSON.stringify(body)})});
 if(!r.ok)throw Error('Local database request failed '+r.status);return r.json();
}
const read=key=>databaseRequest(key),patch=(key,body)=>databaseRequest(key,'PATCH',body);
function client(name){const app=initializeApp({projectId:project,apiKey:'demo-key',databaseURL:`http://${host}:${port}?ns=${project}`},name);apps.push(app);const auth=getAuth(app);connectAuthEmulator(auth,'http://127.0.0.1:9099',{disableWarnings:true});const f=getFunctions(app,'us-central1');connectFunctionsEmulator(f,'127.0.0.1',5001);return {auth,call:async(n,d)=>{try{return (await httpsCallable(f,n)(d)).data;}catch(error){
 console.error('CALL FAILED',n,'body bytes',JSON.stringify(d).length,error.code,error.message);
 if(error.code==='functions/internal'){
  try{const token=await auth.currentUser.getIdToken();const r=await fetch(`http://127.0.0.1:5001/${project}/us-central1/${n}`,{method:'POST',headers:{'Content-Type':'application/json',Authorization:'Bearer '+token},body:JSON.stringify({data:d})});const body=await r.json();if(r.ok&&body.result)return body.result;console.error('DIRECT STATUS',r.status,'ERROR',body.error?.message);}catch(e){console.error('DIRECT FAILURE',e.message,e.cause?.code);}
 }
 throw error;
}}};}
try{
const a=client('audit-a'),b=client('audit-b');await Promise.all([signInAnonymously(a.auth),signInAnonymously(b.auth)]);
for(const [c,n]of [[a,'auditana'],[b,'auditbia']])assert.equal((await c.call('bwProfileLogin',{nickname:n,pin:'1234',createIfMissing:true})).coins,100);
if(!await read('users/auditana'))namespace=project+'-default-rtdb';assert.ok(await read('users/auditana'));console.log('Local audit namespace:',namespace);
async function room(code){await a.call('bwCreateRoom',{roomCode:code,playerId:'P_a',isPrivate:true,boardCols:12});await a.call('bwRoomPresence',{roomCode:code,playerId:'P_a'});await b.call('bwJoinRoom',{roomCode:code,playerId:'P_b',boardCols:12});await b.call('bwRoomPresence',{roomCode:code,playerId:'P_b'});}

await assert.rejects(a.call('bwFriends',{nickname:'auditbia',action:'list'}));
await assert.rejects(a.call('bwFriends',{nickname:'auditana',action:'request',target:'auditana'}));
await a.call('bwFriends',{nickname:'auditana',action:'request',target:'auditbia'});
await a.call('bwFriends',{nickname:'auditana',action:'request',target:'auditbia'});
let social=await b.call('bwFriends',{nickname:'auditbia',action:'list'});assert.equal(social.contacts.length,1);assert.equal(social.contacts[0].status,'incoming');
await b.call('bwFriends',{nickname:'auditbia',action:'accept',target:'auditana'});
assert.equal((await a.call('bwFriends',{nickname:'auditana',action:'list'})).contacts[0].status,'friend');
await a.call('bwCreateRoom',{roomCode:'FRND',playerId:'P_a',isPrivate:true,boardCols:12});await a.call('bwRoomPresence',{roomCode:'FRND',playerId:'P_a'});
await a.call('bwFriends',{nickname:'auditana',action:'invite',target:'auditbia',roomCode:'FRND'});
social=await b.call('bwFriends',{nickname:'auditbia',action:'list'});assert.equal(social.contacts[0].invitation.code,'FRND');
const c=client('audit-c');await signInAnonymously(c.auth);await c.call('bwProfileLogin',{nickname:'auditcat',pin:'1234',createIfMissing:true});
await assert.rejects(c.call('bwJoinRoom',{roomCode:'FRND',playerId:'P_c',boardCols:12}));
await b.call('bwJoinRoom',{roomCode:'FRND',playerId:'P_b',boardCols:12});
await a.call('bwFriends',{nickname:'auditana',action:'block',target:'auditbia'});
assert.equal((await b.call('bwFriends',{nickname:'auditbia',action:'list'})).contacts.length,0);
await assert.rejects(b.call('bwFriends',{nickname:'auditbia',action:'request',target:'auditana'}));
await assert.rejects(b.call('bwPlayerProfile',{nickname:'auditbia',target:'auditana'}));
await a.call('bwFriends',{nickname:'auditana',action:'unblock',target:'auditbia'});
assert.equal((await a.call('bwFriends',{nickname:'auditana',action:'list'})).contacts.length,0);
console.log('PASS friendship authorization, duplicate requests, acceptance, targeted invitation, blocking and unblock');

await room('AUDT');
const sa=await a.call('bwBeginMatch',{nickname:'auditana',mode:'ONLINE',roomCode:'AUDT',boardCols:12}),sb=await b.call('bwBeginMatch',{nickname:'auditbia',mode:'ONLINE',roomCode:'AUDT',boardCols:12});assert.equal(sa.boardCols,12);assert.equal((await read('privateMatches/'+sa.id)).boardCols,12);
await patch('privateMatches/'+sa.id,{startedAt:Date.now()-300000});
let events=[],v=R.verifyReplay(sa,events),time=0;
for(let i=0;i<40&&v.player.lines<1&&!v.player.dead;i++){const p=v.player.piece,x=v.player.board[19].findIndex(c=>!c);const rotation=p.type<=12-x?0:1;let piece={type:p.type,x,y:0,r:rotation};while(R.valid(v.player.board,{...piece,y:piece.y+1}))piece.y++;events.push({kind:'lock',actor:0,t:time+=100,...piece},{kind:'commit',actor:0,t:time+=250});v=R.verifyReplay(sa,events);}
assert.ok(v.player.lines>0);console.log('Generated replay',events.length,'events,',v.player.lines,'lines,',v.player.attacks,'attacks');await a.call('bwSendAttack',{matchId:sa.id,events});let queue;
for(let i=0;i<60;i++){queue=await read('partidas/AUDT/attacksForP2');if(queue&&Object.values(queue).reduce((s,n)=>s+n,0)===v.player.attacks)break;await new Promise(r=>setTimeout(r,500));}
assert.ok(queue);assert.equal(Object.values(queue).reduce((s,n)=>s+n,0),v.player.attacks);await a.call('bwSendAttack',{matchId:sa.id,events});assert.deepEqual(await read('partidas/AUDT/attacksForP2'),queue);
console.log('PASS attacks delivered once',v.player.attacks,'lines',v.player.lines,'maxCombo',v.player.maxCombo);
await b.call('bwFinishRoom',{roomCode:'AUDT',playerId:'P_b',reason:'abandono'});
let p=await a.call('bwSettleMatch',{matchId:sa.id,events});assert.equal(p.rewardReceipt.matchId,sa.id);assert.equal(p.rewardReceipt.awardedBw,v.player.score+20);assert.equal(p.rewardReceipt.seasonXp,30+v.player.lines);assert.equal(p.coins,150);assert.equal(p.pontos_bw,v.player.score+20);
await Promise.all([a.call('bwSettleMatch',{matchId:sa.id,events}),a.call('bwSettleMatch',{matchId:sa.id,events})]);p=await a.call('bwProfileLogin',{nickname:'auditana',pin:'1234'});assert.equal(p.coins,150);assert.equal(p.pontos_bw,v.player.score+20);
const lost=await b.call('bwSettleMatch',{matchId:sb.id,events:[]});assert.equal(lost.coins,100);assert.equal(lost.pontos_bw,0);
let season=await a.call('bwSeason',{nickname:'auditana'});assert.equal(season.xp,30+Math.min(100,v.player.lines));assert.equal((await b.call('bwSeason',{nickname:'auditbia'})).xp,0);
const shown=await b.call('bwPlayerProfile',{nickname:'auditbia',target:'auditana'});
assert.equal(shown.statistics.matches,1);assert.equal(shown.history.length,1);assert.equal(shown.history[0].opponent,'auditbia');
assert.ok(!('coins' in shown));assert.ok(!('email' in shown));assert.ok(!('ownerUids' in shown));
await assert.rejects(a.call('bwPlayerProfile',{nickname:'auditbia',target:'auditana'}));
console.log('PASS player profile privacy, verified history, opponent identity and owner authorization');
console.log('PASS exact BW, coins, XP, duplicate requests and abandonment');
const period=D.periods().MONTHLY;await patch('users/auditana',{coins:1000,[`seasons/${period}/xp`]:3500});season=await a.call('bwSeason',{nickname:'auditana'});
for(let i=0;i<season.tiers.length;i++)await a.call('bwSeason',{nickname:'auditana',claim:i});season=await a.call('bwSeason',{nickname:'auditana',claim:season.tiers.length-1});assert.ok(season.tiers.every(t=>t.claimed));
let user=await read('users/auditana');assert.ok(season.tiers.flatMap(t=>t.items).every(id=>user.inventory.boughtItems.includes(id)));assert.equal(new Set(user.inventory.boughtItems).size,user.inventory.boughtItems.length);await assert.rejects(a.call('bwSeason',{nickname:'auditana',claim:999}));
assert.equal((await a.call('bwEconomy',{nickname:'auditana',id:'audit-buy',kind:'BUY',itemId:'gb'})).accepted,true);await a.call('bwEconomy',{nickname:'auditana',id:'audit-buy',kind:'BUY',itemId:'gb'});assert.equal((await read('users/auditana')).coins,800);
console.log('PASS season rewards and purchases remain idempotent');
for(const [code,expired]of [['REC1',false],['REC2',true]]){await room(code);await patch('partidas/'+code,{p1Connected:false,p1DisconnectedAt:Date.now()-(expired?31000:1000)});if(expired){await a.call('bwRoomTimeout',{roomCode:code});assert.equal((await read('partidas/'+code)).loserId,'P_a');}else{await a.call('bwPreparePresence',{roomCode:code,playerId:'P_a'});await a.call('bwRoomPresence',{roomCode:code,playerId:'P_a'});assert.equal((await read('partidas/'+code)).estado,'em_jogo');}}
console.log('PASS reconnection before and after the 30-second deadline');
}finally{await env.cleanup();await Promise.all(apps.map(deleteApp));}
})().catch(e=>{console.error(e);process.exitCode=1;});
