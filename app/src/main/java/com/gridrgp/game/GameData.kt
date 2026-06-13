package com.gridrpg.game

// Grid constants
const val GRID_SIZE = 5
const val PLAYER_MOVE_AREA = 3  // 3x3 center
const val PLAYER_MOVE_OFFSET = 1  // starts at tile (1,1)

// Wave timing
const val WAVE_DURATION_MS = 30_000L  // 30 seconds per wave
const val UPGRADE_WAVE_INTERVAL = 5   // every 5 waves

data class Position(val x: Int, val y: Int)

data class Player(
    var pos: Position = Position(2, 2),  // center of 5x5
    var hp: Int = 10,
    var maxHp: Int = 10,
    var damage: Int = 3,
    var alive: Boolean = true
)

data class Enemy(
    val id: Int,
    var pos: Position,
    var hp: Int = 3,
    var maxHp: Int = 3,
    var damage: Int = 1,
    var alive: Boolean = true
)

enum class UpgradeType(val title: String, val desc: String) {
    MORE_DAMAGE("More Damage", "+2 Attack Damage"),
    MORE_HP("More HP", "+2 Max HP"),
    HEAL("Heal", "Restore 50% HP"),
    LESS_ENEMY("Less Enemy", "-10% Enemy Spawn Rate (max 50%)")
}

// Outer ring positions of 5x5 grid (spawn locations)
val OUTER_POSITIONS: List<Position> by lazy {
    val list = mutableListOf<Position>()
    for (x in 0 until GRID_SIZE) {
        for (y in 0 until GRID_SIZE) {
            if (x == 0 || x == GRID_SIZE - 1 || y == 0 || y == GRID_SIZE - 1) {
                list.add(Position(x, y))
            }
        }
    }
    list
}

// Center 3x3 positions (player movement area)
val CENTER_POSITIONS: List<Position> by lazy {
    val list = mutableListOf<Position>()
    for (x in 1..3) {
        for (y in 1..3) {
            list.add(Position(x, y))
        }
    }
    list
}
