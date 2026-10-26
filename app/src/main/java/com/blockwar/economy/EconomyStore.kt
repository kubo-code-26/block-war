package com.blockwar.economy

import android.content.Context
import com.blockwar.storage.commit
import com.blockwar.model.PlayerInventory
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

data class EconomyOperation(val id: String, val nickname: String, val kind: String,
                            val itemId: String = "", val amount: Int = 0)
data class EconomyResult(val accepted: Boolean, val inventory: PlayerInventory, val message: String = "")

object EconomyPolicy {
    fun apply(inventory: PlayerInventory, operation: EconomyOperation): EconomyResult {
        if (operation.kind == "CREDIT") {
            return EconomyResult(false, inventory, "Recarga de teste desativada.")
        }
        val item = ShopCatalog.find(operation.itemId)
            ?: return EconomyResult(false, inventory, "Item não encontrado na loja.")
        return when (operation.kind) {
            "BUY" -> when {
                item.id in inventory.boughtItems -> EconomyResult(true, inventory)
                item.seasonOnly -> EconomyResult(false, inventory, "Resgate este item na página Temporada.")
                inventory.coins < item.price -> EconomyResult(false, inventory, "Blockoins insuficientes para esta compra.")
                else -> EconomyResult(true, inventory.copy(coins = inventory.coins - item.price,
                    boughtItems = (inventory.boughtItems + item.id).distinct()))
            }
            "EQUIP" -> if (item.id !in inventory.boughtItems)
                EconomyResult(false, inventory, "Compre o item antes de equipá-lo.")
                else EconomyResult(true, inventory.copy(equippedItems = inventory.equippedItems + (item.itemCategory to item.id)))
            else -> EconomyResult(false, inventory, "Operação inválida.")
        }
    }
}

class EconomyStore(context: Context) {
    private val prefs = context.getSharedPreferences("economy_operations_v1", Context.MODE_PRIVATE)
    private fun key(nickname: String) = nickname.trim().lowercase(Locale.ROOT).ifEmpty { "guest" }
    private fun read(nickname: String): JSONObject = try {
        JSONObject(prefs.getString(key(nickname), "{}") ?: "{}")
    } catch (_: Exception) { JSONObject() }
    private fun write(nickname: String, data: JSONObject) {
        check(prefs.commit { putString(key(nickname), data.toString()) }) { "Não foi possível salvar a operação da loja." }
    }
    fun inventory(nickname: String): PlayerInventory? {
        val data = read(nickname).optJSONObject("inventory") ?: return null
        val bought = data.optJSONArray("boughtItems") ?: JSONArray()
        val equipped = data.optJSONObject("equippedItems") ?: JSONObject()
        return PlayerInventory(data.optInt("coins"), (0 until bought.length()).map { bought.getString(it) },
            equipped.keys().asSequence().associateWith { equipped.getString(it) })
    }
    fun cache(nickname: String, inventory: PlayerInventory) {
        val data = read(nickname)
        data.put("inventory", JSONObject().put("coins", inventory.coins)
            .put("boughtItems", JSONArray(inventory.boughtItems)).put("equippedItems", JSONObject(inventory.equippedItems)))
        write(nickname, data)
    }
    fun pending(nickname: String): List<EconomyOperation> {
        val array = read(nickname).optJSONArray("pending") ?: JSONArray()
        return (0 until array.length()).map { i ->
            val e = array.getJSONObject(i)
            EconomyOperation(e.getString("id"), nickname, e.getString("kind"), e.optString("itemId"), e.optInt("amount"))
        }
    }
    private fun savePending(nickname: String, operations: List<EconomyOperation>) {
        val array = JSONArray()
        operations.forEach { array.put(JSONObject().put("id", it.id).put("kind", it.kind)
            .put("itemId", it.itemId).put("amount", it.amount)) }
        write(nickname, read(nickname).put("pending", array))
    }
    fun enqueue(operation: EconomyOperation) = savePending(operation.nickname, pending(operation.nickname) + operation)
    fun remove(operation: EconomyOperation) = savePending(operation.nickname, pending(operation.nickname).filterNot { it.id == operation.id })
}
