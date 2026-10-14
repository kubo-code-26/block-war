"use strict";
const test=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),path=require('node:path');
function delivery(failFirst=false){
 const message={uid:'receiver',id:'receipt',blocks:8,queue:'attacksForP2'},pending='partidas/ABCD/pendingVerifiedAttacks/receipt',receipt='privateAttacks/ABCD/receiver/receipt',queue='partidas/ABCD/attacksForP2/receipt';
 const records=new Map([[pending,message]]);let writes=0;
 const db={ref(key=''){return {async get(){return {exists:()=>records.has(key),val:()=>records.get(key)};},async remove(){records.delete(key);},async update(values){if(failFirst){failFirst=false;throw Error('Simulated transient database failure');}writes++;for(const [k,v]of Object.entries(values))records.set(k,v);}};}};
 const exported={};const load=id=>{
  if(id==='firebase-functions/v2/https')return {HttpsError:Error,onCall:(_,fn)=>fn};
  if(id==='firebase-functions/v2/database')return {onValueWritten:(options,fn)=>Object.assign(fn,{auditOptions:options})};
  if(id==='firebase-admin')return {initializeApp(){},database:()=>db};
  return require(id.startsWith('.')?path.resolve(__dirname,'..',id):id);
 };
 vm.runInNewContext(fs.readFileSync(path.resolve(__dirname,'../index.js'),'utf8'),{require:load,exports:exported,process:{env:{}},console,Buffer});
 return {run:()=>exported.bwDeliverAttack({params:{code:'ABCD',id:'receipt'},data:{after:{val:()=>message}}}),options:exported.bwDeliverAttack.auditOptions,records,pending,receipt,queue,writes:()=>writes};
}
test('attack delivery writes receipt and inbox once, even after acknowledgement and duplicate event',async()=>{
 const d=delivery();await d.run();assert.equal(d.records.get(d.queue),8);assert.equal(d.records.get(d.receipt).blocks,8);assert.ok(!d.records.has(d.pending));
 d.records.delete(d.queue);await d.run();assert.ok(!d.records.has(d.queue));assert.equal(d.writes(),1);
});
test('audit: transient failure preserves the outbox but automatic delivery retry is disabled',async()=>{
 const d=delivery(true);await assert.rejects(d.run(),/transient/);assert.ok(d.records.has(d.pending));assert.ok(!d.records.has(d.receipt));assert.ok(!d.records.has(d.queue));assert.equal(d.options.retry,false);
 // A later manual re-invocation succeeds; no mechanism automatically requests it here.
 await d.run();assert.equal(d.records.get(d.queue),8);assert.equal(d.writes(),1);
});