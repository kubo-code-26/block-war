"use strict";
const crypto=require("node:crypto");
const {MISSIONS,CATALOG,DEFAULT_INVENTORY}=require("./catalog");
const {rotatingMissions}=require("./rotating-missions");
const Season=require("./season");
for(const item of Season.items)CATALOG[item.id]=item;
const MAX=2147483647;
const add=(n,d)=>Math.min(MAX,Math.max(0,(Number(n)||0)+d));
const key=n=>String(n||"").trim().toLowerCase();
function validName(n) { return typeof n==="string" && n.length>=1 && n.length<=12 && !/[.#$\[\]\/\u0000-\u001f\u007f]/.test(n); }
function hash(pin,salt) { return crypto.createHash("sha256").update(`${salt}:${pin}`).digest("hex"); }
function safeEqual(a,b) {
  const x=Buffer.from(String(a)),y=Buffer.from(String(b));return x.length===y.length&&crypto.timingSafeEqual(x,y);
}
function pinFingerprint(u) { return JSON.stringify([u?.pin_scrypt,u?.pin_hash,u?.pin,u?.pin_salt]); }
function verifyPin(u,pin) {
  if(!/^\d{4}$/.test(pin||""))return false;
  if(u.pin_scrypt) {
    const [salt,expected]=u.pin_scrypt.split(":");
    return safeEqual(crypto.scryptSync(pin,salt,32).toString("hex"),expected);
  }
  const expected=u.pin_hash||u.pin||"";
  return expected && safeEqual(u.pin_salt?hash(pin,u.pin_salt):pin,expected);
}
function newPin(pin) {
  if(!/^\d{4}$/.test(pin||""))throw new Error("O PIN deve ter quatro números.");
  const salt=crypto.randomBytes(16).toString("hex");
  return `${salt}:${crypto.scryptSync(pin,salt,32).toString("hex")}`;
}
function initialProfile(nickname,uid,pinScrypt) {
  return {nickname,ownerUids:{[uid]:true},pin_scrypt:pinScrypt,pontos_bw:0,coins:100,
    inventory:structuredClone(DEFAULT_INVENTORY),total_vitorias:0,revision:1};
}
function publicProfile(u) {
  const coins=add(u.coins??u.inventory?.coins??100,0);
  return {nickname:u.nickname,pontos_bw:add(u.pontos_bw,0),coins,featuredBadge:u.featuredBadge||null,
    inventory:{...DEFAULT_INVENTORY,...u.inventory,coins},
    total_vitorias:add(u.total_vitorias,0),layout:u.layout||{},googlePlayGamesId:u.googlePlayGamesId||u.google_play_id||"",email:u.email||""};
}
function owned(u,uid) { return !!u && (u.ownerUids?.[uid]===true||u.firebaseAuthUid===uid); }
function economy(u,operation) {
  const id=operation.id;
  if(!/^[a-zA-Z0-9_-]{1,100}$/.test(id||""))throw new Error("Operação inválida.");
  u.economyOperations??={};
  if(u.economyOperations[id])return u.economyOperations[id];
  const item=CATALOG[operation.itemId],inv={...DEFAULT_INVENTORY,...u.inventory};
  const bought=Array.isArray(inv.boughtItems)?inv.boughtItems:[];
  let result={accepted:false,message:"Operação inválida."};
  const balance=u.coins??inv.coins??100;
  if(operation.kind==="BUY"&&item) {
    if(item.seasonOnly&&!bought.includes(item.id))return {accepted:false,message:"Este item é uma recompensa da temporada. Resgate-o na página Temporada."};
    if(bought.includes(item.id))result={accepted:true,message:""};
    else if(balance>=item.price) {
      u.coins=balance-item.price;inv.coins=u.coins;inv.boughtItems=[...bought,item.id];
      result={accepted:true,message:""};
    } else result={accepted:false,message:"Blockoins insuficientes."};
  } else if(operation.kind==="EQUIP"&&item) {
    if(bought.includes(item.id)){inv.equippedItems={...inv.equippedItems,[item.category]:item.id};result={accepted:true,message:""};}
    else result={accepted:false,message:"Compre o item antes de equipá-lo."};
  } else if(operation.kind==="CREDIT") result={accepted:false,message:"Recarga de teste desativada."};
  u.inventory=inv;u.economyOperations[id]=result;return result;
}
function periods(now=Date.now()) {
  const parts=new Intl.DateTimeFormat("en-CA",{timeZone:"America/Sao_Paulo",year:"numeric",month:"2-digit",day:"2-digit"})
    .formatToParts(new Date(now));
  const v=Object.fromEntries(parts.map(p=>[p.type,p.value]));
  const local=new Date(Date.UTC(Number(v.year),Number(v.month)-1,Number(v.day))),iso=d=>d.toISOString().slice(0,10);
  const monday=new Date(local);monday.setUTCDate(local.getUTCDate()-((local.getUTCDay()+6)%7));
  return {DAILY:iso(local),WEEKLY:iso(monday),MONTHLY:iso(local).slice(0,7)};
}
function missions(u,claimId=null,now=Date.now()) {
  const p=periods(now);u.missions??={};
  return [...MISSIONS,...rotatingMissions(p)].map(m=>{
    const type=u.missions[m.type]??={};const period=type[p[m.type]]??={};
    const record=period[m.id]??={progress:0,completed:false,claimed:false};
    if(m.id==="monthly_ranking_legend")record.progress=Math.max(record.progress,Math.min(m.goal,u.pontos_bw||0));
    record.completed=record.progress>=m.goal;
    if(claimId===m.id&&record.completed&&!record.claimed) {
      u.coins=add(u.coins??u.inventory?.coins??100,m.reward);u.inventory??=structuredClone(DEFAULT_INVENTORY);
      u.inventory.coins=u.coins;record.claimed=true;
    }
    return {...m,...record,isCompleted:record.completed,isClaimed:record.claimed};
  });
}
function advanceMissions(u,result,won,online,forfeit,now) {
  const p=periods(now);
  const changes={daily_first_win:won?1:0,daily_cleaner:result.lines,daily_combo_master:result.maxCombo,
    weekly_online_warrior:online&&won?1:0,weekly_heavy_attack:result.attacks,weekly_constancy:forfeit?0:1,
    monthly_demolisher:result.lines,monthly_collector:won?1:0};
  missions(u,null,now);
  const extra={matches:1,lines:result.lines,attacks:result.attacks,wins:won?1:0,
    onlineWins:online&&won?1:0,maxCombo:result.maxCombo};
  for(const m of [...MISSIONS,...rotatingMissions(p)]) {
    const r=u.missions[m.type][p[m.type]][m.id];const amount=changes[m.id]||0;
    const delta=m.metric?(forfeit?0:Math.max(0,Number(extra[m.metric])||0)):amount;
    r.progress=Math.min(m.goal,m.id==="daily_combo_master"||m.metric==="maxCombo"?
      Math.max(r.progress,delta):r.progress+delta);
    r.completed=r.progress>=m.goal;
  }
}
function statistics(u) {
  const matches=Object.entries(u.appliedMatchRewards||{}).filter(([,r])=>r.onlineStatistics?.online===true)
    .map(([id,r])=>({...r.onlineStatistics,id,won:r.isVictory===true})).sort((a,b)=>a.finishedAt-b.finishedAt||a.id.localeCompare(b.id));
  let wins=0,streak=0,best=0,lines=0,attacks=0,points=0,duration=0,high=0,fast=0,combos=0;
  for(const r of matches) {
    if(r.won){wins++;streak++;best=Math.max(best,streak);if(r.durationSeconds>0)fast=fast?Math.min(fast,r.durationSeconds):r.durationSeconds;}
    else streak=0;
    lines=add(lines,r.lines);attacks=add(attacks,r.attacks);points=add(points,r.bw);
    combos=add(combos,r.combos||0);
    duration+=r.durationSeconds;high=Math.max(high,r.bw);
  }
  const total=matches.length;
  u.onlineStats={totalMatches:total,totalWins:wins,totalLosses:total-wins,totalLinesCleared:lines,
    totalAttacksSent:attacks,totalCombos:combos,totalScore:points,highScore:high,currentWinStreak:streak,bestWinStreak:best,
    totalDurationSeconds:duration,fastestWinSeconds:fast,onlineMatches:total,cpuMatches:0,
    winRate:total?100*wins/total:0,avgDurationSeconds:total?Math.floor(duration/total):0};
}
function reward(u,id,result,won,online,forfeit,now,durationSeconds) {
  u.appliedMatchRewards??={};if(u.appliedMatchRewards[id])return false;
  const beforeBw=add(u.pontos_bw,0),period=periods(now).MONTHLY,beforeXp=u.seasons?.[period]?.xp||0;
  const bwDelta=forfeit?-10:result.score+(won?20:-10),coins=forfeit?0:online?50:20;
  u.pontos_bw=add(u.pontos_bw,bwDelta);u.coins=add(u.coins??u.inventory?.coins??100,coins);
  u.inventory??=structuredClone(DEFAULT_INVENTORY);u.inventory.coins=u.coins;
  if(won)u.total_vitorias=add(u.total_vitorias,1);
  const receipt=u.appliedMatchRewards[id]={bwDelta,blockoins:coins,isVictory:won};
  if(online)receipt.onlineStatistics={online:true,bw:result.score,lines:result.lines,attacks:result.attacks,
    durationSeconds,finishedAt:now,combos:Math.max(0,result.combos||0)};
  advanceMissions(u,result,won,online,forfeit,now);missions(u,null,now);
  Season.advance(u,periods(now).MONTHLY,result,won,online,forfeit);
  receipt.awardedBw=u.pontos_bw-beforeBw;
  receipt.seasonXp=(u.seasons?.[period]?.xp||0)-beforeXp;
  statistics(u);return true;
}
module.exports={key,validName,verifyPin,newPin,pinFingerprint,initialProfile,publicProfile,owned,economy,missions,reward,periods,statistics,add};
