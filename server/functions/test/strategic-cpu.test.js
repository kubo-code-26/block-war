"use strict";
const test=require("node:test"),assert=require("node:assert/strict");
const {strategicDecision}=require("../strategic-cpu"),{Sequence,cpuDecision,spawn,valid,clear,verifyReplay}=require("../replay");
const empty=()=>Array.from({length:20},()=>Array(12).fill(0));
function land(board,type,d){let p={type,x:d.targetX,y:0,r:d.rotation};while(valid(board,{...p,y:p.y+1}))p.y++;
 const copy=board.map(row=>row.slice());for(let i=0;i<type;i++)copy[p.y+(p.r%2?i:0)][p.x+(p.r%2?0:i)]=1;return clear(copy);}
test("hard completes a four-row well instead of stacking over it",()=>{
 const b=empty();for(let y=16;y<20;y++)b[y].fill(1);for(let y=16;y<20;y++)b[y][11]=0;
 const d=strategicDecision(b,spawn(4,12),2);assert.equal(d.targetX,11);assert.equal(d.rotation,1);assert.equal(land(b,4,d).count,4);
});
test("hard takes an immediate attack with the correct line reward",()=>{
 const b=empty();b[19].fill(1);b[19][6]=b[19][7]=0;
 const d=strategicDecision(b,spawn(2,12),4);assert.equal(land(b,2,d).count,1);
});
test("hard is deterministic and never chooses a path through a spawn obstruction",()=>{
 const b=empty();for(let y=0;y<20;y++)b[y][8]=1;
 const p=spawn(1,12),a=strategicDecision(b,p,3);assert.deepEqual(a,strategicDecision(b,p,3));assert.ok(a.targetX<8);
});
test("new sessions verify strategic decisions and still reject forged choices",()=>{
 const s={seed:1,opponentSeed:2,boardCols:12,mode:"CPU",difficulty:"HARD",cpuVersion:2},seq=new Sequence(2),type=seq.next(4)+1,next=seq.next(4)+1;
 const d=strategicDecision(empty(),spawn(type,12),next),e={kind:"decision",actor:1,t:0,targetX:d.targetX,rotation:d.rotation};
 assert.doesNotThrow(()=>verifyReplay(s,[e]));assert.throws(()=>verifyReplay(s,[{...e,targetX:(e.targetX+1)%12}]),/Decisão/);
});
test("seeded benchmark: strategic hard clears more lines than legacy hard",()=>{
 let oldLines=0,newLines=0;
 for(let seed=1;seed<=8;seed++)for(const advanced of [false,true]) {
  const seq=new Sequence(seed),random=new Sequence(seed^18523);let type=seq.next(4)+1,next=seq.next(4)+1,b=empty(),lines=0;
  for(let turn=0;turn<200;turn++) {
   if(!valid(b,spawn(type,12)))break;
   const d=advanced?strategicDecision(b,spawn(type,12),next):cpuDecision(b,type,"HARD",random);
   const result=land(b,type,d);b=result.board;lines+=result.count;type=next;next=seq.next(4)+1;
  }
  if(advanced)newLines+=lines;else oldLines+=lines;
 }
 console.log(JSON.stringify({benchmark:"8 seeds x 200 pieces",oldLines,newLines}));assert.ok(newLines>oldLines);
});
