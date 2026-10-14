"use strict";
const goals={wins:[10,50,150,400,1000,2500,5000],attacks:[500,2000,10000,30000,100000,300000,1000000],
  lines:[100,500,2000,7500,20000,60000,150000],combos:[25,100,500,1500,5000,15000,40000],
  matches:[100,250,500,1000,2500,5000,10000],collector:[5,10,20,30,45,60,73]};
function select(user,category,tier,catalog) {
  if(category===null)return null;
  if(!goals[category]||!Number.isInteger(tier)||tier<0||tier>6)throw new Error("Escudo inválido.");
  const s=user.onlineStats||{},paid=new Set(Object.values(catalog).filter(i=>i.price>0).map(i=>i.id));
  const counts={wins:s.totalWins||0,attacks:s.totalAttacksSent||0,lines:s.totalLinesCleared||0,
    combos:s.totalCombos||0,matches:s.totalMatches||0,collector:[...new Set(user.inventory?.boughtItems||[])].filter(id=>paid.has(id)).length};
  if(counts[category]<goals[category][tier])throw new Error("Conquiste este escudo antes de exibi-lo.");
  return {category,tier};
}
function read(user,catalog) {
  const value=user.featuredBadge;if(!value)return null;
  try{return select(user,value.category,value.tier,catalog);}catch{return null;}
}
module.exports={select,read,goals};
