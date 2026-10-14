"use strict";
const test=require("node:test"),assert=require("node:assert/strict"),F=require("../friends");
test("friend request is idempotent, recipient accepts, crossed requests accept",()=>{
 const r=F.change(null,"ana","bob","request",1);assert.equal(F.pair("ana","bob"),F.pair("bob","ana"));assert.throws(()=>F.pair("ana","ana"));
 assert.deepEqual(F.change(r,"ana","bob","request",2),r);assert.throws(()=>F.change(r,"ana","bob","accept"));
 assert.equal(F.visible(r,"bob").status,"incoming");assert.equal(F.visible(r,"ana").status,"outgoing");
 assert.equal(F.change(r,"bob","ana","accept").status,"accepted");assert.equal(F.change(r,"bob","ana","request").status,"accepted");
});
test("reject, cancel and remove respect request direction",()=>{
 const r=F.change(null,"ana","bob","request");assert.deepEqual(F.change(r,"ana","bob","reject"),r);
 assert.equal(F.change(r,"bob","ana","reject"),null);assert.equal(F.change(r,"ana","bob","cancel"),null);
 const a=F.change(r,"bob","ana","accept");assert.equal(F.change(a,"ana","bob","remove"),null);
});
test("blocking cancels friendship/invites and both blocks must be removed",()=>{
 let r=F.change(null,"ana","bob","request");r=F.change(r,"bob","ana","accept");r.invitation={from:"ana",code:"ABCD",expiresAt:100};
 assert.equal(F.visible(r,"bob",1).invitation.code,"ABCD");assert.equal(F.visible(r,"bob",101).invitation,null);
 r=F.change(r,"ana","bob","block");assert.equal(F.visible(r,"bob"),null);assert.equal(F.visible(r,"ana").status,"blocked");assert.equal(r.invitation,null);
 assert.throws(()=>F.change(r,"bob","ana","request"));r=F.change(r,"bob","ana","block");r=F.change(r,"ana","bob","unblock");assert.equal(r.status,"blocked");assert.equal(F.change(r,"bob","ana","unblock"),null);
});
