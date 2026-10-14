"use strict";
const test=require("node:test"),assert=require("node:assert/strict");
const G=require("../board-geometry"),R=require("../replay"),Rematch=require("../rematch");
test("board versions are explicit and matchmaking separates widths",()=>{
 assert.equal(G.columns(undefined),10);assert.equal(G.columns(12),12);
 for(const invalid of [0,11,13,"12",{},NaN])assert.throws(()=>G.columns(invalid));
 assert.equal(G.compatible({},10),true);assert.equal(G.compatible({},12),false);
 assert.equal(G.compatible({boardCols:12},12),true);assert.equal(G.compatible({boardCols:12},10),false);
});
test("12-column replay clears a complete row and validates the right edge",()=>{
 const cols=12,type=new R.Sequence(1).next(4)+1,count=cols-type;
 const pieces=Array.from({length:count},(_,i)=>({id:i+1,size:1,col:i,y:-1-i,target:19}));
 const events=[{kind:"wave",actor:0,t:0,receipt:"attack",pieces}];
 for(let i=0;i<21+count;i++)events.push({kind:"step",actor:0,t:i+1,active:R.spawn(type,cols)});
 events.push({kind:"lock",actor:0,t:100,type,x:count,y:19,r:0},{kind:"commit",actor:0,t:300});
 const result=R.verifyReplay({boardCols:12,seed:1,opponentSeed:2,mode:"ONLINE"},events,{attack:{blocks:count}}).player;
 assert.equal(result.lines,1);assert.equal(result.score,5);assert.equal(result.board.length,20);
 assert.ok(result.board.every(row=>row.length===12));
 const board=Array.from({length:20},()=>Array(12).fill(0));
 assert.ok(R.valid(board,{type:2,x:10,y:19,r:0}));assert.ok(!R.valid(board,{type:2,x:11,y:19,r:0}));
 board[19].fill(1,0,10);assert.equal(R.clear(board).count,0);
});
test("CPU can target the twelfth column and rematches preserve geometry",()=>{
 const board=Array.from({length:20},(_,y)=>Array.from({length:12},(_,x)=>y===19&&x!==11?1:0));
 const move=R.cpuDecision(board,1,"HARD",new R.Sequence(1));assert.equal(move.targetX,11);
 const old={code:"ABCD",boardCols:12,estado:"finalizada",finishedAt:1000,p1Uid:"a",p2Uid:"b",p1Id:"P_a",p2Id:"P_b",p1Nickname:"ana",p2Nickname:"bia"};
 Rematch.reserve(old,"a",1100,"EFGH");assert.equal(Rematch.createRoom(old,"EFGH",1100).boardCols,12);
});