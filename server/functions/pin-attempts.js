"use strict";
const crypto=require("node:crypto");
const WINDOW_MS=15*60*1000,MAX_ATTEMPTS=10;

// A rejected request never extends the window. All callers for a profile share it.
function reserveAttempt(previous,now) {
  const valid=previous&&Number.isFinite(previous.startedAt)&&previous.startedAt<=now&&
    now-previous.startedAt<WINDOW_MS;
  const count=valid?Math.max(0,Number(previous.count)||0):0;
  if(count>=MAX_ATTEMPTS)return undefined;
  return {startedAt:valid?previous.startedAt:now,count:count+1};
}
function profileKey(nickname) {
  return crypto.createHash("sha256").update(String(nickname).trim().toLowerCase()).digest("hex");
}
async function reserveProfileAttempt(db,nickname,now=Date.now()) {
  const tx=await db.ref(`privateLimits/profilePin/${profileKey(nickname)}`)
    .transaction(previous=>reserveAttempt(previous,now));
  return tx.committed;
}
module.exports={reserveAttempt,reserveProfileAttempt,profileKey,WINDOW_MS,MAX_ATTEMPTS};
