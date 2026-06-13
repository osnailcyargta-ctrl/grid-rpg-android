package com.gridrpg.game

import kotlin.math.abs
import kotlin.random.Random

class GameEngine {

    var player = Player()
    val enemies = mutableListOf<Enemy>()
    var wave = 1
    var score = 0
    var enemySpawnReduction = 0f  // 0.0 to 0.5
    private var enemyIdCounter = 0
    private var waveStartTime = System.currentTimeMillis()

    // Callbacks
    var onGameOver: (() -> Unit)? = null
    var onWaveComplete: ((Int) -> Unit)? = null
    var onUpgradeNeeded: ((List<UpgradeType>) -> Unit)? = null
    var onStateChanged: (() -> Unit)? = null

    private var waitingForUpgrade = false
    private var pendingWave = 1

    fun start() {
        player = Player()
        enemies.clear()
        wave = 1
        score = 0
        enemySpawnReduction = 0f
        waveStartTime = System.currentTimeMillis()
        waitingForUpgrade = false
        spawnEnemiesForWave()
        onStateChanged?.invoke()
    }

    fun tick() {
        if (!player.alive || waitingForUpgrade) return

        val now = System.currentTimeMillis()
        val elapsed = now - waveStartTime

        // Enemy movement tick - move enemies every 1.5 seconds
        moveEnemiesStep()

        // Check if enemies reached player
        checkEnemyCollisions()

        // Wave timer check
        if (elapsed >= WAVE_DURATION_MS) {
            endWave()
        }

        onStateChanged?.invoke()
    }

    fun moveEnemiesStep() {
        enemies.filter { it.alive }.forEach { enemy ->
            val newPos = stepToward(enemy.pos, player.pos)
            // Don't move to a position occupied by another enemy
            if (enemies.none { it.alive && it.id != enemy.id && it.pos == newPos }) {
                enemy.pos = newPos
            }
        }
    }

    private fun stepToward(from: Position, to: Position): Position {
        val dx = to.x - from.x
        val dy = to.y - from.y
        return when {
            abs(dx) >= abs(dy) && dx != 0 -> Position(from.x + dx.coerceIn(-1, 1), from.y)
            dy != 0 -> Position(from.x, from.y + dy.coerceIn(-1, 1))
            else -> from
        }
    }

    private fun checkEnemyCollisions() {
        enemies.filter { it.alive && it.pos == player.pos }.forEach { enemy ->
            player.hp -= enemy.damage
            enemy.alive = false
            if (player.hp <= 0) {
                player.hp = 0
                player.alive = false
                onGameOver?.invoke()
                return
            }
        }
    }

    fun movePlayer(dx: Int, dy: Int) {
        if (!player.alive || waitingForUpgrade) return
        val newX = (player.pos.x + dx).coerceIn(PLAYER_MOVE_OFFSET, PLAYER_MOVE_OFFSET + PLAYER_MOVE_AREA - 1)
        val newY = (player.pos.y + dy).coerceIn(PLAYER_MOVE_OFFSET, PLAYER_MOVE_OFFSET + PLAYER_MOVE_AREA - 1)
        player.pos = Position(newX, newY)
        checkEnemyCollisions()
        onStateChanged?.invoke()
    }

    fun attack() {
        if (!player.alive || waitingForUpgrade) return
        // Attack one enemy in 3x3 area around player (including player tile)
        val target = enemies.filter { it.alive && isAdjacent(it.pos, player.pos) }
            .minByOrNull { distance(it.pos, player.pos) }
        target?.let {
            it.hp -= player.damage
            if (it.hp <= 0) {
                it.alive = false
                score++
            }
        }
        onStateChanged?.invoke()
    }

    private fun isAdjacent(a: Position, b: Position): Boolean {
        return abs(a.x - b.x) <= 1 && abs(a.y - b.y) <= 1
    }

    private fun distance(a: Position, b: Position): Int {
        return abs(a.x - b.x) + abs(a.y - b.y)
    }

    private fun endWave() {
        wave++
        val completedWave = wave - 1
        onWaveComplete?.invoke(completedWave)

        if (completedWave % UPGRADE_WAVE_INTERVAL == 0) {
            // Trigger upgrade selection
            waitingForUpgrade = true
            pendingWave = wave
            val options = UpgradeType.values().toList().shuffled().take(2)
            onUpgradeNeeded?.invoke(options)
        } else {
            startNextWave()
        }
    }

    fun selectUpgrade(upgrade: UpgradeType) {
        when (upgrade) {
            UpgradeType.MORE_DAMAGE -> player.damage += 2
            UpgradeType.MORE_HP -> {
                player.maxHp += 2
                player.hp = minOf(player.hp + 2, player.maxHp)
            }
            UpgradeType.HEAL -> {
                player.hp = minOf(player.maxHp, player.hp + (player.maxHp / 2))
            }
            UpgradeType.LESS_ENEMY -> {
                enemySpawnReduction = minOf(0.5f, enemySpawnReduction + 0.1f)
            }
        }
        waitingForUpgrade = false
        startNextWave()
    }

    private fun startNextWave() {
        enemies.removeAll { !it.alive }
        spawnEnemiesForWave()
        waveStartTime = System.currentTimeMillis()
        onStateChanged?.invoke()
    }

    fun getWaveTimeRemaining(): Long {
        if (waitingForUpgrade) return 0
        val elapsed = System.currentTimeMillis() - waveStartTime
        return maxOf(0L, WAVE_DURATION_MS - elapsed)
    }

    private fun spawnEnemiesForWave() {
        val baseCount = 2 + wave
        val spawnCount = (baseCount * (1f - enemySpawnReduction)).toInt().coerceAtLeast(1)
        val enemyHp = 2 + wave / 2
        val enemyDmg = 1 + wave / 3

        val availableSpots = OUTER_POSITIONS.toMutableList()
        availableSpots.shuffle()

        repeat(minOf(spawnCount, availableSpots.size)) { i ->
            enemies.add(
                Enemy(
                    id = enemyIdCounter++,
                    pos = availableSpots[i],
                    hp = enemyHp,
                    maxHp = enemyHp,
                    damage = enemyDmg
                )
            )
        }
    }

    fun isWaitingForUpgrade() = waitingForUpgrade
}
