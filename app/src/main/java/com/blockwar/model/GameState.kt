package com.blockwar.model

import androidx.compose.ui.graphics.Color

data class ButtonPos(val x: Float = 0f, val y: Float = 0f)

enum class ControlMode {
    BUTTONS, GESTURES
}

data class ControlSettings(
    val scale: Float = 1.0f,
    val opacity: Float = 1.0f,
    val posLeft: ButtonPos = ButtonPos(0f, 0f),
    val posRotate: ButtonPos = ButtonPos(0f, 0f),
    val posRight: ButtonPos = ButtonPos(0f, 0f),
    val posSoftDrop: ButtonPos = ButtonPos(0f, 0f),
    val posHardDrop: ButtonPos = ButtonPos(0f, 0f),
    val buttonGap: Float = 8.0f,
    val controlMode: ControlMode = ControlMode.BUTTONS,
    val gestureSensitivity: Float = 1.0f
)

data class Cell(
    val color: Color,
    val highlightColor: Color,
    val shadowColor: Color,
    val isGarbage: Boolean = false
) {
    companion object {
        val GARBAGE = Cell(
            color = Color(0xFF616161),
            highlightColor = Color(0xFF9E9E9E),
            shadowColor = Color(0xFF424242),
            isGarbage = true
        )

        fun fromPiece(pieceType: PieceType): Cell {
            return Cell(
                color = pieceType.primaryColor,
                highlightColor = pieceType.highlightColor,
                shadowColor = pieceType.shadowColor,
                isGarbage = false
            )
        }
    }
}

enum class MatchStatus {
    STARTING, PLAYING, PAUSED, VICTORY, DEFEAT
}

enum class GameMode {
    VS_CPU, ONLINE_1V1
}

enum class CpuDifficulty(
    val displayName: String,
    val description: String,
    val stepDelayMs: Long,
    val dropDelayMs: Long
) {
    EASY("FÁCIL", "Lento, decisões simples", 220L, 120L),
    MEDIUM("MÉDIO", "Moderado, preenche lacunas", 120L, 60L),
    HARD("DIFÍCIL", "Rápido, foca em combos!", 50L, 25L)
}

enum class AppScreen {
    MENU, GAME_VS_CPU, ONLINE_LOBBY, GAME_ONLINE, CUSTOMIZE_CONTROLS
}

data class AttackEvent(
    val blocks: Int,
    val fromPlayer: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class FallingGarbagePiece(
    val id: Long,
    val size: Int,
    val startCol: Int,
    val y: Int,
    val targetY: Int = 19,
    val spawnDelayTicks: Int = 0
)

data class PlayerState(
    val board: List<List<Cell?>> = List(BOARD_ROWS) { List(BOARD_COLS) { null } },
    val currentPiece: BlockPiece? = null,
    val ghostPiece: BlockPiece? = null,
    val nextPiece: PieceType = PieceType.LINE_4,
    val score: Int = 0,
    val linesCleared: Int = 0,
    val pendingGarbage: Int = 0,
    val fallingGarbagePieces: List<FallingGarbagePiece> = emptyList(),
    val combo: Int = 0,
    val isGameOver: Boolean = false,
    val clearingRows: List<Int> = emptyList(),
    val lastClearCount: Int = 0,
    val lastClearTime: Long = 0L,
    val isClearingLines: Boolean = false,
    val lastImpactTime: Long = 0L,
    val impactIntensity: Float = 0f,
    val impactColumn: Float = 5f,
    val impactRow: Float = 18f,
    val impactPieceType: PieceType? = null,
    val maxCombo: Int = 0
) {
    companion object {
        const val BOARD_ROWS = 20
        const val BOARD_COLS = 10
    }
}

data class VersusGameState(
    val onlineStats: AggregatedStats = AggregatedStats(),
    val economyMessage: String = "",
    val missionSaveMessage: String = "",
    val onlineConnectionMessage: String = "",
    val onlineSessionCancelled: Boolean = false,
    val player: PlayerState = PlayerState(),
    val opponent: PlayerState = PlayerState(),
    val matchStatus: MatchStatus = MatchStatus.STARTING,
    val currentScreen: AppScreen = AppScreen.MENU,
    val gameMode: GameMode = GameMode.VS_CPU,
    val cpuDifficulty: CpuDifficulty = CpuDifficulty.MEDIUM,
    val wins: Int = 0,
    val losses: Int = 0,
    val attackEvents: List<AttackEvent> = emptyList(),
    val isAudioEnabled: Boolean = true,
    val isSearchingOnlineOpponent: Boolean = false,
    val onlineRoomCode: String = "",
    val isPlayer1: Boolean = true,
    val onlineRoomStatus: String = "menu",
    val onlineErrorMessage: String = "",
    val controlSettings: ControlSettings = ControlSettings(),
    val matchDurationSeconds: Long = 0L,
    val totalAttacksSent: Int = 0,
    val nicknameErrorMessage: String = "",
    val nickname: String = "",
    val googlePlayGamesId: String = "",
    val playGamesBusy: Boolean = false,
    val playGamesConnected: Boolean = false,
    val playGamesAccountName: String = "",
    val playGamesMessage: String = "",
    val email: String = "",
    val nicknameShowPinPrompt: Boolean = false,
    val nicknameToVerify: String = "",
    val pontos_bw: Int = 0,
    val coins: Int = 100,
    val inventory: PlayerInventory = PlayerInventory(),
    val dailyMissions: List<Mission> = emptyList(),
    val secureStartErrorMessage: String = "",
    val rewardSaveMessage: String = ""
)
