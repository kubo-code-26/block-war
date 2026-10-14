package com.blockwar.model

data class FeaturedBadge(val category:String="",val tier:Int=0)

enum class MissionType {
    DAILY, WEEKLY, MONTHLY
}

data class Mission(
    val id: String,
    val title: String,
    val description: String,
    val reward: Int,
    val progress: Int,
    val goal: Int,
    val isCompleted: Boolean = false,
    val isClaimed: Boolean = false,
    val type: MissionType = MissionType.DAILY,
    val rewardType: String = "BLOCKOIN"
)

data class ShopItem(
    val id: String,
    val name: String,
    val itemCategory: String, // "blocos", "efeitos", "musicas", "fundos"
    val price: Int,
    val description: String,
    val seasonOnly: Boolean = false
)

data class PlayerInventory(
    val coins: Int = 100,
    val boughtItems: List<String> = listOf("neon", "sparks", "arcade", "space", "anim_flash", "tab_neon", "banner_bot", "avatar_default", "moldura_simple"),
    val equippedItems: Map<String, String> = mapOf(
        "blocos" to "neon",
        "efeitos" to "sparks",
        "musicas" to "arcade",
        "fundos" to "space",
        "animacoes" to "anim_flash",
        "tabuleiros" to "tab_neon",
        "banners" to "banner_bot",
        "avatares" to "avatar_default",
        "molduras" to "moldura_simple"
    )
)

data class MatchRecord(
    val id: String,
    val timestamp: Long,
    val gameMode: String,
    val isVictory: Boolean,
    val linesCleared: Int,
    val attacksSent: Int,
    val durationSeconds: Long,
    val score: Int,
    val maxCombo: Int
)

data class AggregatedStats(
    val totalMatches: Int = 0,
    val totalWins: Int = 0,
    val totalLosses: Int = 0,
    val winRate: Float = 0f,
    val winRatePercent: Int = 0,
    val totalLinesCleared: Int = 0,
    val totalAttacksSent: Int = 0,
    val totalScore: Int = 0,
    val highScore: Int = 0,
    val averageDurationSeconds: Long = 0L,
    val currentWinStreak: Int = 0,
    val bestWinStreak: Int = 0,
    val fastestWinSeconds: Long = 0L,
    val totalCombos: Int = 0,
    val totalDurationSeconds: Long = 0L
)
