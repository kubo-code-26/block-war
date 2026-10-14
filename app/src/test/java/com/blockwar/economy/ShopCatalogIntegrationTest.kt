package com.blockwar.economy

import com.blockwar.model.PlayerInventory
import org.junit.Assert.*
import org.junit.Test

class ShopCatalogIntegrationTest {
    @Test fun everyItemCanBeBoughtAndEquippedWithoutChangingAnotherCategory() {
        for (item in ShopCatalog.items) {
            val initial = PlayerInventory(coins = 100000)
            val owned = item.id in initial.boughtItems
            val buy = EconomyPolicy.apply(initial, EconomyOperation("buy-${item.id}", "tester", "BUY", item.id))
            assertTrue(item.id, buy.accepted)
            assertEquals(item.id, initial.coins - if (owned) 0 else item.price, buy.inventory.coins)
            assertTrue(item.id, item.id in buy.inventory.boughtItems)
            val equip = EconomyPolicy.apply(buy.inventory, EconomyOperation("equip-${item.id}", "tester", "EQUIP", item.id))
            assertTrue(item.id, equip.accepted)
            assertEquals(item.id, equip.inventory.equippedItems[item.itemCategory])
            initial.equippedItems.filterKeys { it != item.itemCategory }.forEach { (category, value) ->
                assertEquals(category, value, equip.inventory.equippedItems[category])
            }
            assertEquals(buy.inventory.coins, equip.inventory.coins)
        }
    }

    @Test fun repeatedPurchaseNeverChargesAgain() {
        for (item in ShopCatalog.items) {
            val first = EconomyPolicy.apply(PlayerInventory(coins = 100000), EconomyOperation("1", "tester", "BUY", item.id))
            val repeated = EconomyPolicy.apply(first.inventory, EconomyOperation("2", "tester", "BUY", item.id))
            assertEquals(item.id, first.inventory, repeated.inventory)
        }
    }

    @Test fun catalogueIdentifiersAreUniqueAndOldItemsRemainAvailable() {
        assertEquals(ShopCatalog.items.size, ShopCatalog.items.map { it.id }.distinct().size)
        PlayerInventory().boughtItems.forEach { assertNotNull(it, ShopCatalog.find(it)) }
        assertEquals("Cristais de Luz", ShopCatalog.find("animacao_level_up_brilhante")?.name)
    }
}
