"use strict";
const test=require("node:test"),assert=require("node:assert/strict");
const fs=require("node:fs"),vm=require("node:vm"),path=require("node:path");

function endpoints() {
  const user={nickname:"Kender",pin:"1234",ownerUids:Object.fromEntries(
    Array.from({length:20},(_,i)=>[`uid${i}`,true]))};
  const records=new Map([["users/kender",user]]);
  const db={ref(key){return {
    async get(){return {val:()=>key.startsWith("activeProfiles/")?"kender":records.get(key),exists:()=>records.has(key)};},
    async transaction(update){const next=update(records.get(key)??null);
      if(next===undefined)return {committed:false};records.set(key,next);
      return {committed:true,snapshot:{val:()=>next,exists:()=>next!==null}};
    },async set(value){records.set(key,value);}
  };}};
  class HttpsError extends Error{constructor(code,message){super(message);this.code=code;}}
  const admin={initializeApp(){},database:()=>db,auth:()=>({getUser:async()=>({
    providerData:[{providerId:"playgames.google.com",uid:"player"}]
  })})};
  const output={};
  const load=id=>{
    if(id==="firebase-functions/v2/https")return {HttpsError,onCall:(_,handler)=>handler};
    if(id==="firebase-functions/v2/database")return {onValueWritten:()=>()=>{}};
    if(id==="firebase-admin")return admin;
    return require(id.startsWith(".")?path.resolve(__dirname,"..",id):id);
  };
  vm.runInNewContext(fs.readFileSync(path.resolve(__dirname,"../index.js"),"utf8"),
    {require:load,exports:output,process:{env:{}},console,Buffer});
  return {output,records};
}
test("login, troca de PIN e vínculo compartilham orçamento contra rotação de UID",async()=>{
  const {output,records}=endpoints();
  const handlers=["bwProfileLogin","bwChangePin","bwPlayLink"];
  for(let i=0;i<12;i++) {
    await assert.rejects(output[handlers[i%3]]({auth:{uid:`uid${i}`},data:{
      nickname:" KENDER ",pin:"0000",oldPin:"0000",newPin:"9999"
    }}),error=>i<10?error.code!=="resource-exhausted":error.code==="resource-exhausted");
  }
  assert.equal(records.get("users/kender").pin,"1234");
  assert.equal(records.get("users/kender").firebaseAuthUid,undefined);
});
test("conta Google vinculada pode entrar mesmo com orçamento do PIN esgotado",async()=>{
  const {output,records}=endpoints();
  const user=records.get("users/kender");user.firebaseAuthUid="uid0";
  const {profileKey}=require("../pin-attempts");
  records.set(`privateLimits/profilePin/${profileKey("kender")}`,{startedAt:Date.now(),count:10});
  const result=await output.bwProfileLogin({auth:{uid:"uid0"},data:{nickname:"kender",pin:""}});
  assert.equal(result.nickname,"Kender");
  assert.equal(records.get("activeProfiles/uid0"),"kender");
});
