"use strict";
const themes=[
  {id:"forja",name:"Forja da Arena",skin:"real_obsidiana",effect:"real_fagulhas",board:"real_fundicao"},
  {id:"glacial",name:"Coroa Glacial",skin:"real_gelo",effect:"real_estilhacos",board:"real_catedral"},
  {id:"cristal",name:"Império de Cristal",skin:"real_cristal",effect:"real_prismas",board:"real_caverna"}
];
function definition(period) {
  const ordinal=Number(period.slice(0,4))*12+Number(period.slice(5,7))-1;
  const theme=themes[((ordinal%3)+3)%3];
  return {...theme,period,tiers:[
    {goal:100,title:"Aprendiz",items:[`season_frame_${theme.id}`]},
    {goal:300,title:"Especialista",items:[theme.skin]},
    {goal:700,title:"Guardião",items:[theme.effect]},
    {goal:1500,title:"Mestre da temporada",items:[theme.board,`season_banner_${theme.id}`,`season_anim_${theme.id}`]}
  ].concat(theme.id==="forja"?[
    {goal:2000,title:"Sentinela",items:["avatar_forja"]},
    {goal:2500,title:"Cora\u00e7\u00e3o da Forja",items:["fundo_forja"]},
    {goal:3500,title:"Soberano do Magma",items:["real_magma"]}
  ]:[])};
}
function progress(user,period) {
  const def=definition(period),record=user.seasons?.[period]||{xp:0,claimed:{}};
  return {...def,xp:record.xp||0,tiers:def.tiers.map((t,i)=>({...t,claimed:record.claimed?.[i]===true}))};
}
function advance(user,period,verified,won,online,forfeit) {
  if(!online||forfeit)return;
  user.seasons??={};const s=user.seasons[period]??={xp:0,claimed:{}};
  s.xp=Math.min(2147483647,s.xp+(won?30:10)+Math.min(100,Math.max(0,Number(verified.lines)||0)));
}
function claim(user,period,index) {
  if(!Number.isInteger(index)||index<0||index>=definition(period).tiers.length)throw new Error("Recompensa de temporada inválida.");
  const current=progress(user,period),tier=current.tiers[index];
  if(current.xp<tier.goal)throw new Error("Complete a meta de XP para resgatar esta recompensa.");
  const s=user.seasons[period];s.claimed??={};
  if(s.claimed[index])return current;
  user.inventory??={boughtItems:[],equippedItems:{}};
  user.inventory.boughtItems=[...new Set([...(user.inventory.boughtItems||[]),...tier.items])];
  s.claimed[index]=true;
  return progress(user,period);
}
const materials=[
  ["real_aco","Aço Escovado",1800],["real_titanio","Titânio Lunar",2400],
  ["real_obsidiana","Obsidiana Fundida",2200],["real_magma","Magma Vivo",2800],
  ["real_vidro","Vidro Temperado",1900],["real_gelo","Gelo Boreal",2200],
  ["real_cristal","Cristal Prismático",2500],["real_cobre","Cobre Envelhecido",1800],
  ["real_marmore","Mármore Imperial",2300],["real_madeira","Madeira Nobre",1600]
];
const items=materials.map(([id,name,price])=>({id,name,category:"blocos",price}));
items.push({id:"avatar_forja",name:"Sentinela da Fundição",category:"avatares",price:1800});
items.push({id:"fundo_forja",name:"Coração da Fundição",category:"fundos",price:2400});
for(const [id,name] of [["real_fundicao","Fundição"],["real_catedral","Catedral Glacial"],["real_caverna","Caverna Prismática"]])
  items.push({id,name,category:"tabuleiros",price:2600});
for(const [id,name] of [["real_fagulhas","Fagulhas de Metal"],["real_estilhacos","Estilhaços de Gelo"],["real_prismas","Fragmentos Prismáticos"]])
  items.push({id,name,category:"efeitos",price:2000});
for(const theme of themes)for(const [kind,category,label] of [["frame","molduras","Moldura"],["banner","banners","Estandarte"],["anim","animacoes","Finalização"]])
  items.push({id:`season_${kind}_${theme.id}`,name:`${label} · ${theme.name}`,category,price:0,seasonOnly:true});
module.exports={definition,progress,advance,claim,items};
