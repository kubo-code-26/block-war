"use strict";
const D=require("./domain"),Badges=require("./badges");
function amount(x){return Math.min(2147483647,Math.max(0,Number(x)||0));}
function recent(user) {
 return Object.entries(user.appliedMatchRewards||{}).filter(([,r])=>r.onlineStatistics?.online===true&&Number.isFinite(r.onlineStatistics.finishedAt))
  .sort(([a,x],[b,y])=>y.onlineStatistics.finishedAt-x.onlineStatistics.finishedAt||a.localeCompare(b)).slice(0,5);
}
function project(user,catalog,opponents={}) {
 // Reconstruct from verified receipts in memory; never return the private profile object.
 const copy=structuredClone(user);D.statistics(copy);const s=copy.onlineStats;
 const paid=new Set(Object.values(catalog).filter(i=>i.price>0).map(i=>i.id));
 const counts={wins:s.totalWins,attacks:s.totalAttacksSent,lines:s.totalLinesCleared,combos:s.totalCombos,matches:s.totalMatches,
  collector:[...new Set(user.inventory?.boughtItems||[])].filter(id=>paid.has(id)).length};
 const equipped=(category,fallback)=>{
  const id=user.inventory?.equippedItems?.[category];return catalog[id]?.category===category&&(user.inventory?.boughtItems||[]).includes(id)?id:fallback;
 };
 return {nickname:user.nickname,pontosBw:amount(user.pontos_bw),avatar:equipped("avatares","avatar_default"),frame:equipped("molduras","moldura_simple"),
  featuredBadge:Badges.read(copy,catalog),
  statistics:{matches:s.totalMatches,wins:s.totalWins,losses:s.totalLosses,winRate:s.winRate,lines:s.totalLinesCleared,attacks:s.totalAttacksSent,
   combos:s.totalCombos,highScore:s.highScore,bestWinStreak:s.bestWinStreak,averageDuration:s.avgDurationSeconds},
  achievements:Object.entries(Badges.goals).map(([id,goals])=>({id,value:amount(counts[id]),unlocked:goals.filter(g=>counts[id]>=g).length})),
  history:recent(copy).map(([id,r])=>({won:r.isVictory===true,finishedAt:r.onlineStatistics.finishedAt,
   opponent:opponents[id]||"Adversário",score:amount(r.onlineStatistics.bw),lines:amount(r.onlineStatistics.lines),
   attacks:amount(r.onlineStatistics.attacks),duration:amount(r.onlineStatistics.durationSeconds)}))};
}
module.exports={recent,project};
