"use strict";
// Keep integer weights, iteration order and reachable paths identical to StrategicCpu.kt.
function valid(b,p) {
 for(let i=0;i<p.type;i++) {const x=p.x+(p.r%2?0:i),y=p.y+(p.r%2?i:0);
  if(x<0||x>=b[0].length||y>=b.length||(y>=0&&b[y][x]))return false;
 }return true;
}
function rotate(b,p) {
 for(const dx of [0,-1,1,-2,2]) {const q={...p,x:p.x+dx,r:(p.r+1)%4};if(valid(b,q))return q;}return null;
}
function evaluate(b) {
 const h=Array(b[0].length).fill(0);let holes=0,covered=0,bump=0,wells=0;
 for(let x=0;x<h.length;x++) {let blocks=0;for(let y=0;y<b.length;y++) {
  if(b[y][x]) {if(!blocks)h[x]=b.length-y;blocks++;}else if(blocks){holes++;covered+=blocks;}
 }}
 const max=Math.max(...h);
 for(let x=0;x<h.length;x++) {
  if(x)bump+=Math.abs(h[x]-h[x-1]);
  const depth=Math.max(0,Math.min(x?h[x-1]:b.length,x===h.length-1?b.length:h[x+1])-h[x]-3);wells+=depth*depth;
 }
 return -8*h.reduce((a,v)=>a+v,0)-120*holes-10*covered-6*bump-2*max*max-12*wells;
}
function candidates(b,initial) {
 const result=[];
 for(let r=0;r<=1;r++)for(let x=0;x<b[0].length;x++) {
  let p={...initial};while(!valid(b,p)&&p.y>-4)p={...p,y:p.y-1};if(!valid(b,p))continue;
  let turns=0;while(p.r!==r&&turns++<4){const q=rotate(b,p);if(!q)break;p=q;}if(p.r!==r)continue;
  while(p.x!==x){const q={...p,x:p.x+Math.sign(x-p.x)};if(!valid(b,q))break;p=q;}if(p.x!==x)continue;
  while(valid(b,{...p,y:p.y+1}))p={...p,y:p.y+1};if(p.y<0)continue;
  const copy=b.map(row=>row.slice());for(let i=0;i<p.type;i++)copy[p.y+(r%2?i:0)][p.x+(r%2?0:i)]=1;
  const remaining=copy.filter(row=>row.some(v=>!v)),lines=b.length-remaining.length;
  const cleared=Array.from({length:lines},()=>Array(b[0].length).fill(0)).concat(remaining);
  const reward=[0,5,8,12,16][Math.min(4,lines)]*40;
  result.push({x,r,board:cleared,reward,score:reward+evaluate(cleared)});
 }return result;
}
function strategicDecision(board,piece,next) {
 let best=null,bestScore=-2147483648;
 for(const c of candidates(board,piece)) {
  const future=candidates(c.board,{type:next,x:Math.floor((board[0].length-next)/2),y:0,r:0});
  const futureScore=future.length?Math.max(...future.map(v=>v.score)):-1000000;
  const score=c.reward*10+(c.score-c.reward)*3+futureScore*7;
  if(score>bestScore){bestScore=score;best=c;}
 }
 return {targetX:best?.x??piece.x,rotation:best?.r??piece.r,score:bestScore};
}
module.exports={strategicDecision};
