"use strict";

// Logical replay verifier. It rebuilds the board and points rather than accepting a client score.
class Sequence {
  constructor(seed) { this.state = BigInt(seed) & 2147483647n; }
  next(bound) {
    if(!Number.isInteger(bound)||bound<1||bound>8388608)throw new Error("Limite aleatório inválido.");
    let value;const limit=Math.floor(8388608/bound)*bound;
    do {this.state=(1103515245n*this.state+12345n)&2147483647n;value=Number(this.state>>8n);} while(value>=limit);
    return value%bound;
  }
}
const basePoints = [0, 5, 8, 12, 16];
function fail(message) { throw new Error(message); }
function cells(p) {
  return Array.from({ length: p.type }, (_, i) => [p.x + (p.r % 2 ? 0 : i), p.y + (p.r % 2 ? i : 0)]);
}
function valid(board, p) {
  return cells(p).every(([x,y]) => x >= 0 && x < board[0].length && y < board.length && (y < 0 || !board[y][x]));
}
function clear(board) {
  const remaining = board.filter(row => row.some(x => !x));
  const count = board.length - remaining.length;
  while (remaining.length < board.length) remaining.unshift(Array(board[0].length).fill(0));
  return { board: remaining, count };
}
function spawn(type,cols=10) { return { type, x: Math.floor((cols - type) / 2), y: 0, r: 0 }; }
function evaluate(board) {
  let score=0,holes=0,lines=0;
  for(let x=0;x<board[0].length;x++) {
    let found=false;
    for(let y=0;y<board.length;y++) {
      if(board[y][x]) {if(!found)score-=(board.length-y)*0.5;found=true;}
      else if(found)holes++;
    }
  }
  for(const row of board)if(row.every(Boolean))lines++;
  return score-holes*4+lines*10;
}
function cpuDecision(board,type,difficulty,random) {
  let best={targetX:Math.floor(board[0].length/2),rotation:0,score:-Number.MAX_VALUE};
  for(let r=0;r<=1;r++)for(let x=-2;x<board[0].length+2;x++) {
    let p={type,x,y:0,r};if(!valid(board,p)&&!valid(board,{...p,y:-1}))continue;
    while(valid(board,{...p,y:p.y+1}))p={...p,y:p.y+1};
    const copy=board.map(row=>row.slice());for(const [c,y] of cells(p))if(y>=0&&y<board.length&&c>=0&&c<board[0].length)copy[y][c]=type;
    const score=evaluate(clear(copy).board);if(score>best.score)best={targetX:x,rotation:r,score};
  }
  if(difficulty==="EASY"&&random.next(1000000)<300000)return {targetX:random.next(board[0].length),rotation:random.next(2),score:0};
  return best;
}
function rotate(board,p) {
  const r = {...p,r:(p.r+1)%4};
  for (const dx of [0,-1,1,-2,2]) { const next={...r,x:r.x+dx}; if(valid(board,next)) return next; }
  return null;
}
function reachable(board, from, to, cpu) {
  if (from.type !== to.type || to.x < 0 || to.x >= board[0].length || to.y < -4 || to.y >= board.length || to.r < 0 || to.r > 3) return false;
  const key=p=>`${p.x},${p.y},${p.r}`;
  const queue=[from],seen=new Set([key(from)]);
  for(let i=0;i<queue.length;i++) {
    const p=queue[i];if(key(p)===key(to))return true;
    const possible=[{...p,x:p.x-1},{...p,x:p.x+1},{...p,y:p.y+1},rotate(board,p)];
    // The existing CPU lifts an active piece when a falling garbage block intersects it.
    if(cpu && !valid(board,p) && p.y>-4) possible.push({...p,y:p.y-1});
    for(const n of possible) if(n && n.y>=-4 && valid(board,n) && !seen.has(key(n))) { seen.add(key(n));queue.push(n); }
  }
  return false;
}
function actor(seed,cols=10) {
  const random=new Sequence(seed), first=random.next(4)+1, next=random.next(4)+1;
  return { board:Array.from({length:20},()=>Array(cols).fill(0)), random, piece:spawn(first,cols), next,
    falling:[], score:0,lines:0,combo:0,maxCombo:0,combos:0,attacks:0,dead:false,locks:0,budget:0 };
}
function verifyReplay(session, events, allowedAttacks={}) {
  if(!Array.isArray(events) || events.length>50000) fail("Registro de partida inválido ou grande demais.");
  const cols=require("./board-geometry").columns(session.boardCols);
  const players=[actor(session.seed,cols),actor(session.opponentSeed,cols)], used=new Set();
  const cpuRandom=new Sequence(Number(BigInt(session.opponentSeed)^18523n));
  const delay={EASY:[220,120],MEDIUM:[120,60],HARD:[50,25]}[session.difficulty||"MEDIUM"];
  let nextCpuAt=0;
  let last=0;
  for(const event of events) {
    if(!event || ![0,1].includes(event.actor) || !["lock","commit","wave","step","decision","move","rotate","lift"].includes(event.kind)) fail("Evento inválido.");
    if(session.mode==="ONLINE" && event.actor!==0)fail("O cliente não controla o adversário online.");
    if(!Number.isSafeInteger(event.t) || event.t<last || event.t<0 || event.t>86400000)fail("Tempo de replay inválido.");
    last=event.t;
    const a=players[event.actor];
    if(["decision","move","rotate","lift"].includes(event.kind)) {
      if(event.actor!==1||a.dead||a.pending)fail("Ação de CPU inválida.");
      if(event.kind==="decision") {
        const d=session.cpuVersion===2 && session.difficulty==="HARD"
          ? require("./strategic-cpu").strategicDecision(a.board,a.piece,a.next)
          : cpuDecision(a.board,a.piece.type,session.difficulty||"MEDIUM",cpuRandom);
        if(d.targetX!==event.targetX||d.rotation!==event.rotation)fail("Decisão de CPU alterada pelo cliente.");
        a.decision=d;a.lifted=false;
      } else {
        if(!a.decision)fail("CPU sem decisão.");
        if(event.t+20<nextCpuAt)fail("CPU avançou antes do tempo permitido.");
        if(event.kind==="rotate") {
          if(a.piece.r===a.decision.rotation)fail("Rotação extra de CPU.");
          const n=rotate(a.board,a.piece);if(!n)fail("Rotação de CPU impossível.");a.piece=n;nextCpuAt=event.t+delay[0];
        } else if(event.kind==="lift") {
          if(valid(a.board,a.piece))fail("CPU elevada sem colisão.");
          let p=a.piece;while(!valid(a.board,p)&&p.y>-4)p={...p,y:p.y-1};
          if(p.x!==event.x||p.y!==event.y||p.r!==event.r||p.type!==event.type)fail("Elevação de CPU inválida.");
          a.piece=p;a.lifted=true;
        } else {
          const {dx,dy}=event;
          if(![[1,0],[-1,0],[0,1]].some(([x,y])=>x===dx&&y===dy))fail("Movimento de CPU inválido.");
          if(dy===0) {
            if(dx!==Math.sign(a.decision.targetX-a.piece.x))fail("CPU desviou do alvo.");
            if(!a.lifted&&a.piece.r!==a.decision.rotation&&rotate(a.board,a.piece))fail("CPU ignorou a rotação.");
          } else if(!a.lifted&&a.piece.x!==a.decision.targetX&&valid(a.board,{...a.piece,x:a.piece.x+Math.sign(a.decision.targetX-a.piece.x)}))
            fail("CPU ignorou o deslocamento lateral.");
          const n={...a.piece,x:a.piece.x+dx,y:a.piece.y+dy};if(!valid(a.board,n))fail("Movimento de CPU impossível.");
          a.piece=n;nextCpuAt=event.t+(dy?delay[1]:delay[0]);
        }
      }
      continue;
    }
    if(event.kind==="lock") {
      if(a.dead)fail("Movimento após o fim da partida.");
      const p={type:event.type,x:event.x,y:event.y,r:event.r};
      const positionValid=event.actor===1 ? a.decision && p.type===a.piece.type&&p.x===a.piece.x&&p.y===a.piece.y&&p.r===a.piece.r : reachable(a.board,a.piece,p,false);
      if(!Object.values(p).every(Number.isSafeInteger) || !positionValid)fail("Peça fora da sequência ou movimento impossível.");
      if(valid(a.board,{...p,y:p.y+1}))fail("Peça travada antes de pousar.");
      for(const [x,y] of cells(p)) {
        if(y<0)a.dead=true;
        else if(y<a.board.length&&x>=0&&x<cols)a.board[y][x]=p.type;
        else fail("Peça fora do tabuleiro.");
      }
      if(a.pending)fail("Peça anterior ainda não confirmada.");
      a.pending=true;
    } else if(event.kind==="commit") {
      if(!a.pending)fail("Confirmação sem peça travada.");
      a.pending=false;
      const result=clear(a.board);a.board=result.board;
      if(result.count>4)fail("Limpeza de linhas inválida.");
      a.combo=result.count?a.combo+1:0;a.maxCombo=Math.max(a.maxCombo,a.combo);
      if(a.combo===2)a.combos++;
      a.score+=basePoints[result.count]*Math.max(1,Math.min(4,a.combo));a.lines+=result.count;
      a.attacks+=basePoints[result.count];players[1-event.actor].budget+=basePoints[result.count];
      a.locks++;a.piece=spawn(a.next,cols);a.next=a.random.next(4)+1;
      a.decision=null;if(event.actor===1)nextCpuAt=event.t+delay[0]*2;
      if(!valid(a.board,a.piece))a.dead=true;
    } else if(event.kind==="wave") {
      if(!Array.isArray(event.pieces) || event.pieces.length>64)fail("Ataque inválido.");
      const total=event.pieces.reduce((s,p)=>s+p.size,0);
      if(session.mode==="ONLINE") {
        const receipt=allowedAttacks[event.receipt];
        if(!receipt || used.has(event.receipt) || receipt.blocks!==total)fail("Ataque sem confirmação do servidor.");
        used.add(event.receipt);
      } else {
        if(total>a.budget)fail("Blocos cinza sem linhas do adversário.");
        a.budget-=total;
      }
      const temp=a.board.map(row=>row.slice());
      for(const p of a.falling) if(p.target>=0&&p.target<a.board.length)for(let c=p.col;c<p.col+p.size;c++)temp[p.target][c]=5;
      const ids=new Set(a.falling.map(p=>p.id));
      event.pieces.forEach((p,i)=>{
        if(!Number.isSafeInteger(p.id)||p.id<=0||![1,2].includes(p.size)||!Number.isSafeInteger(p.col)||p.col<0||p.col+p.size>cols||p.y!==-1-i||ids.has(p.id))fail("Formato de bloco cinza inválido.");
        let target=a.board.length-1;
        for(let y=0;y<a.board.length;y++) if(temp[y].slice(p.col,p.col+p.size).some(Boolean)){target=y-1;break;}
        if(p.target!==target)fail("Pouso de bloco cinza inválido.");
        if(target>=0)for(let c=p.col;c<p.col+p.size;c++)temp[target][c]=5;
        a.falling.push({...p});ids.add(p.id);
      });
    } else {
      if(event.active) {
        const p=event.active;
        const positionValid=event.actor===1?p.type===a.piece.type&&p.x===a.piece.x&&p.y===a.piece.y&&p.r===a.piece.r:reachable(a.board,a.piece,p,false);
        if(!Object.values(p).every(Number.isSafeInteger)||!positionValid)fail("Posição ativa inválida.");
        a.piece={...p};
      }
      const remaining=[];
      for(const p of a.falling) {
        const y=p.y+1, collision=y>=a.board.length||(y>=0&&a.board[y].slice(p.col,p.col+p.size).some(Boolean));
        if(collision) {
          if(p.y<0)a.dead=true;
          else for(let c=p.col;c<p.col+p.size;c++)a.board[p.y][c]=5;
        } else remaining.push({...p,y});
      }
      a.falling=remaining;a.board=clear(a.board).board;
    }
  }
  return {player:players[0],opponent:players[1],lastEventMillis:last};
}
module.exports={Sequence,basePoints,verifyReplay,valid,clear,spawn,cpuDecision};
