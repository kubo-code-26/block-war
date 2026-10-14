package com.blockwar.achievements

import com.blockwar.model.AggregatedStats
import com.blockwar.model.PlayerInventory
import com.blockwar.model.ShopItem

data class AchievementTier(val name: String, val title: String, val color: Long)
data class AchievementCategory(val id: String, val name: String, val description: String,
    val unit: String, val goals: List<Int>)
data class AchievementProgress(val category: AchievementCategory, val value: Int) {
    val unlockedCount: Int get() = category.goals.count { value >= it }
    val currentTier: AchievementTier? get() = AchievementCatalog.tiers.getOrNull(unlockedCount - 1)
    val nextGoal: Int? get() = category.goals.getOrNull(unlockedCount)
    val fraction: Float get() = nextGoal?.let { (value.toFloat() / it).coerceIn(0f, 1f) } ?: 1f
}

object AchievementCatalog {
    val tiers = listOf(
        AchievementTier("Bronze", "Iniciante", 0xFFCD8750),
        AchievementTier("Prata", "Competidor", 0xFFBCC9DB),
        AchievementTier("Ouro", "Profissional", 0xFFFFC947),
        AchievementTier("Platina", "Veterano", 0xFF51E0BD),
        AchievementTier("Diamante", "Mestre", 0xFF55CEFF),
        AchievementTier("Ônix", "Grão-mestre", 0xFFB588F5),
        AchievementTier("Lendário", "Rei da Arena", 0xFFFF526B)
    )
    val categories = listOf(
        AchievementCategory("wins", "Vitórias", "Vença partidas online", "vitórias", listOf(10,50,150,400,1000,2500,5000)),
        AchievementCategory("attacks", "Atacante", "Envie blocos em ataques online", "blocos enviados", listOf(500,2000,10000,30000,100000,300000,1000000)),
        AchievementCategory("lines", "Destruidor", "Limpe linhas em partidas online", "linhas limpas", listOf(100,500,2000,7500,20000,60000,150000)),
        AchievementCategory("combos", "Combos", "Limpe linhas com duas peças consecutivas ou mais", "combos", listOf(25,100,500,1500,5000,15000,40000)),
        AchievementCategory("matches", "Veterano", "Conclua partidas online", "partidas", listOf(100,250,500,1000,2500,5000,10000)),
        AchievementCategory("collector", "Colecionador", "Adquira itens pagos diferentes", "itens diferentes", listOf(5,10,20,30,45,60,73))
    )
    fun progress(stats: AggregatedStats, inventory: PlayerInventory, shop: List<ShopItem>): List<AchievementProgress> {
        val paid = shop.filter { it.price > 0 }.map { it.id }.toSet()
        val items = inventory.boughtItems.toSet().count { it in paid }
        val counts = mapOf("wins" to stats.totalWins, "attacks" to stats.totalAttacksSent,
            "lines" to stats.totalLinesCleared, "combos" to stats.totalCombos,
            "matches" to stats.totalMatches, "collector" to items)
        return categories.map { AchievementProgress(it, (counts[it.id] ?: 0).coerceAtLeast(0)) }
    }
}
