const test=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const {strategicDecision}=require('../strategic-cpu'),{spawn}=require('../replay');
const fixture=path.resolve(__dirname,'../../../app/build/strategic-cpu-fixture.json');
test('Kotlin and Node choose exactly the same strategic move and score on 80 boards',{skip:!fs.existsSync(fixture)},()=>{
 const samples=JSON.parse(fs.readFileSync(fixture,'utf8'));assert.equal(samples.length,80);
 for(const [i,s] of samples.entries()) {
  const d=strategicDecision(s.board,spawn(s.type,12),s.next);
  assert.deepEqual([d.targetX,d.rotation,d.score],[s.x,s.r,s.score],`sample ${i}`);
 }
});
