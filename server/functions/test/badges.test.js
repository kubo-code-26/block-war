"use strict";
const test=require("node:test"),assert=require("node:assert/strict"),B=require("../badges");
test("escudo exige meta confirmada e aceita somente categoria e nível válidos",()=>{
  const user={onlineStats:{totalWins:10}};
  assert.deepEqual(B.select(user,"wins",0,{}),{category:"wins",tier:0});
  assert.throws(()=>B.select(user,"wins",1,{}));
  assert.throws(()=>B.select(user,"fake",0,{}));
  assert.throws(()=>B.select(user,"wins",7,{}));
  assert.equal(B.select(user,null,0,{}),null);
  assert.equal(B.read({...user,featuredBadge:{category:"wins",tier:6}},{}),null);
});
test("colecionador conta itens pagos distintos, não itens gratuitos ou duplicados",()=>{
  const catalog=Object.fromEntries(Array.from({length:5},(_,i)=>[`i${i}`,{id:`i${i}`,price:10}]));
  const user={inventory:{boughtItems:[...Object.keys(catalog),"i0","free"]}};
  assert.deepEqual(B.select(user,"collector",0,catalog),{category:"collector",tier:0});
});
