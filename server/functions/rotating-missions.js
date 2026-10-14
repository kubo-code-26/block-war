"use strict";
const metrics=[
  ["matches","Na Arena","Conclua", "partidas sem abandonar"],
  ["lines","Abrindo Caminho","Limpe","linhas"],
  ["attacks","Ofensiva","Envie","blocos em ataques"],
  ["wins","Rumo ao Topo","Vença","partidas"],
  ["onlineWins","Duelo Online","Vença","partidas online"],
  ["maxCombo","Em Sequência","Alcance um combo x",""],
];
const goals={DAILY:[5,40,100,2,1,4],WEEKLY:[25,200,600,10,5,5],MONTHLY:[100,1000,3000,40,20,6]};
const rewards={DAILY:[75,100,125,100,100,150],WEEKLY:[250,300,400,350,400,450],MONTHLY:[800,1000,1200,1200,1500,1500]};
function rotatingMissions(periods) {
  return ["DAILY","WEEKLY","MONTHLY"].flatMap(type=>{
    const period=periods[type];
    const day=Math.floor(Date.parse(`${period.length===7?period+"-01":period}T00:00:00Z`)/86400000);
    const ordinal=type==="WEEKLY"?Math.floor(day/7):type==="MONTHLY"?
      Number(period.slice(0,4))*12+Number(period.slice(5,7)):day;
    return Array.from({length:3},(_,offset)=>{
      const i=((ordinal+offset)%metrics.length+metrics.length)%metrics.length;
      const [metric,title,verb,unit]=metrics[i],goal=goals[type][i];
      return {id:`rot_${type.toLowerCase()}_${metric}`,title,metric,goal,type,
        description:metric==="maxCombo"?`${verb}${goal}`:`${verb} ${goal} ${unit}`,
        reward:rewards[type][i],rewardType:"BLOCKOIN",progress:0};
    });
  });
}
module.exports={rotatingMissions};
