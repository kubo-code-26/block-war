"use strict";
function columns(value) {
  if(value===undefined||value===null)return 10;
  if(value!==10&&value!==12)throw new Error("Tamanho de tabuleiro inv\u00e1lido.");
  return value;
}
function compatible(room,value) {return columns(room?.boardCols)===columns(value);}
module.exports={columns,compatible};
