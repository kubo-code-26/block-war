package com.blockwar.network

import com.blockwar.economy.*
import com.blockwar.model.PlayerInventory
import com.google.firebase.database.MutableData
import com.google.firebase.database.snapshot.Node
import com.google.firebase.database.snapshot.NodeUtilities
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],manifest=Config.NONE)
class ChangePinTransactionTest {
    private fun data(value: Any?): MutableData {
        val constructor=MutableData::class.java.getDeclaredConstructor(Node::class.java)
        constructor.isAccessible=true
        return constructor.newInstance(NodeUtilities.NodeFromJSON(value))
    }
    private fun profile()=mapOf("nickname" to "alice", "pin_hash" to FirebaseRealtimeManager.hashPin("1234","old"),
        "pin_salt" to "old", "coins" to 999, "pontos_bw" to 579)
    @Test fun correctOldPinChangesOnlyCredentialsAndPreservesBalances() {
        val user=data(profile())
        assertTrue(ChangePinTransaction("1234","4321","","device") {_,_,_->}.doTransaction(user).isSuccess)
        val salt=user.child("pin_salt").value.toString()
        assertEquals(FirebaseRealtimeManager.hashPin("4321",salt),user.child("pin_hash").value)
        assertEquals(999L,user.child("coins").value);assertEquals(579L,user.child("pontos_bw").value)
    }
    @Test fun invalidOldPinLeavesTheProfileUntouched() {
        val user=data(profile());val before=user.value
        assertFalse(ChangePinTransaction("9999","4321","","device") {_,_,_->}.doTransaction(user).isSuccess)
        assertEquals(before,user.value)
    }
    @Test fun verifiedGoogleOwnerCanChangeThePinAndAnotherUidCannot() {
        val user=data(profile()+mapOf("firebaseAuthUid" to "owner"));val before=user.value
        assertFalse(ChangePinTransaction("","4321","other","device") {_,_,_->}.doTransaction(user).isSuccess)
        assertEquals(before,user.value)
        assertTrue(ChangePinTransaction("","4321","owner","device") {_,_,_->}.doTransaction(user).isSuccess)
    }
    @Test fun emptyCacheDoesNotCreateAnAccountAndInvalidNewPinIsRejected() {
        val empty=data(null);ChangePinTransaction("1234","4321","","device") {_,_,_->}.doTransaction(empty)
        assertNull(empty.value)
        val user=data(profile())
        assertFalse(ChangePinTransaction("1234","abc","","device") {_,_,_->}.doTransaction(user).isSuccess)
    }
    @Test fun newInventoryStartsWith100AndExistingCoinsAreNeverResetByDeniedCredit() {
        assertEquals(100,PlayerInventory().coins)
        val old=PlayerInventory(coins=100655)
        val result=EconomyPolicy.apply(old,EconomyOperation("test","alice","CREDIT",amount=1000))
        assertFalse(result.accepted);assertEquals(old,result.inventory)
    }
}
