const test=require('node:test'),assert=require('node:assert/strict');
const {strategicDecision}=require('../strategic-cpu'),{Sequence,spawn,valid,clear,verifyReplay}=require('../replay');
test('strategic CPU movement and attacks survive a complete server replay',()=>{
 const session={seed:1,opponentSeed:23,boardCols:12,mode:'CPU',difficulty:'HARD',cpuVersion:2};
 const seq=new Sequence(23);let type=seq.next(4)+1,next=seq.next(4)+1,b=Array.from({length:20},()=>Array(12).fill(0)),t=0,lines=0;const events=[];
 for(let turn=0;turn<100;turn++) {
  let p=spawn(type,12);if(!valid(b,p))break;
  const d=strategicDecision(b,p,next);events.push({kind:'decision',actor:1,t,targetX:d.targetX,rotation:d.rotation});
  while(p.r!==d.rotation){const r=(p.r+1)%4;let q;for(const dx of [0,-1,1,-2,2]){const a={...p,x:p.x+dx,r};if(valid(b,a)){q=a;break;}}assert.ok(q);p=q;events.push({kind:'rotate',actor:1,t});t+=50;}
  while(p.x!==d.targetX){const dx=Math.sign(d.targetX-p.x);p={...p,x:p.x+dx};assert.ok(valid(b,p));events.push({kind:'move',actor:1,t,dx,dy:0});t+=50;}
  while(valid(b,{...p,y:p.y+1})){p={...p,y:p.y+1};events.push({kind:'move',actor:1,t,dx:0,dy:1});t+=25;}
  events.push({kind:'lock',actor:1,t,...p});t+=200;events.push({kind:'commit',actor:1,t});t+=100;
  const copy=b.map(row=>row.slice());for(let i=0;i<type;i++)copy[p.y+(p.r%2?i:0)][p.x+(p.r%2?0:i)]=1;
  const result=clear(copy);b=result.board;lines+=result.count;type=next;next=seq.next(4)+1;
 }
 const verified=verifyReplay(session,events);assert.equal(verified.opponent.lines,lines);assert.ok(lines>10);
});
