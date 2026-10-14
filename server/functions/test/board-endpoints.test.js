"use strict";
const test=require("node:test"),assert=require("node:assert/strict"),fs=require("node:fs"),vm=require("node:vm"),path=require("node:path");
function backend() {
 const records=new Map([["activeProfiles/a","ana"],["users/ana",{nickname:"ana",ownerUids:{a:true}}]]);
 const db={ref(key){return {
  async get(){return {val:()=>records.get(key),exists:()=>records.has(key)};},
  async set(value){records.set(key,value);},
  async transaction(update){const value=update(records.get(key)??null);if(value===undefined)return {committed:false};records.set(key,value);return {committed:true,snapshot:{val:()=>value,exists:()=>value!==null}};}
 };}};
 class HttpsError extends Error {constructor(code,message){super(message);this.code=code;}}
 const exported={};const load=id=>{
  if(id==="firebase-functions/v2/https")return {HttpsError,onCall:(_,f)=>f};
  if(id==="firebase-functions/v2/database")return {onValueWritten:()=>()=>{}};
  if(id==="firebase-admin")return {initializeApp(){},database:()=>db};
  return require(id.startsWith(".")?path.resolve(__dirname,"..",id):id);
 };
 vm.runInNewContext(fs.readFileSync(path.resolve(__dirname,"../index.js"),"utf8"),{require:load,exports:exported,process:{env:{}},console,Buffer});
 return {records,api:exported};
}
test("match start persists server-owned width for new and legacy clients",async()=>{
 const {api,records}=backend();
 for(const boardCols of [undefined,12]){
  const r=await api.bwBeginMatch({auth:{uid:"a"},data:{nickname:"ana",mode:"CPU",boardCols}});
  assert.equal(r.boardCols,boardCols??10);assert.equal(records.get('privateMatches/'+r.id).boardCols,boardCols??10);
 }
 await assert.rejects(()=>api.bwBeginMatch({auth:{uid:"a"},data:{nickname:"ana",mode:"CPU",boardCols:50}}));
});
test("room join rejects cross-width clients and accepts a matching board",async()=>{
 const {api,records}=backend();
 records.set("partidas/ABCD",{code:"ABCD",protocolVersion:3,boardCols:12,p1Connected:true,p1Uid:"b",p1Id:"P_b",p1Nickname:"bia",estado:"aguardando_jogadores",p2Id:""});
 await assert.rejects(()=>api.bwJoinRoom({auth:{uid:"a"},data:{roomCode:"ABCD",playerId:"P_a"}}),/tamanho/);
 assert.equal(records.get("partidas/ABCD").p2Id,"");
 await api.bwJoinRoom({auth:{uid:"a"},data:{roomCode:"ABCD",playerId:"P_a",boardCols:12}});
 assert.equal(records.get("partidas/ABCD").estado,"em_jogo");
});
test("CPU strategy is negotiated per session without changing old clients",async()=>{
 const {api}=backend();
 for(const cpuVersion of [undefined,1,2,999]) {
  const r=await api.bwBeginMatch({auth:{uid:"a"},data:{nickname:"ana",mode:"CPU",boardCols:12,cpuVersion}});
  assert.equal(r.cpuVersion,cpuVersion===2?2:1);
 }
});
