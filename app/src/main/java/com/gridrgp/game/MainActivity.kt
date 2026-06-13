package com.gridrpg.game

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var gameView: GameView
    private lateinit var tvHp: TextView
    private lateinit var tvWave: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvScore: TextView
    private lateinit var tvDamage: TextView
    private lateinit var btnUp: Button
    private lateinit var btnDown: Button
    private lateinit var btnLeft: Button
    private lateinit var btnRight: Button
    private lateinit var btnAttack: Button

    private val engine = GameEngine()
    private val handler = Handler(Looper.getMainLooper())
    private var tickInterval = 1500L // ms per enemy move tick

    private val tickRunnable = object : Runnable {
        override fun run() {
            engine.tick()
            updateHud()
            if (engine.player.alive && !engine.isWaitingForUpgrade()) {
                handler.postDelayed(this, tickInterval)
            }
        }
    }

    private val timerRunnable = object : Runnable {
        override fun run() {
            updateHud()
            if (engine.player.alive) {
                handler.postDelayed(this, 500)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gameView = findViewById(R.id.gameView)
        tvHp = findViewById(R.id.tvHp)
        tvWave = findViewById(R.id.tvWave)
        tvTimer = findViewById(R.id.tvTimer)
        tvScore = findViewById(R.id.tvScore)
        tvDamage = findViewById(R.id.tvDamage)
        btnUp = findViewById(R.id.btnUp)
        btnDown = findViewById(R.id.btnDown)
        btnLeft = findViewById(R.id.btnLeft)
        btnRight = findViewById(R.id.btnRight)
        btnAttack = findViewById(R.id.btnAttack)

        gameView.engine = engine

        setupEngineCallbacks()
        setupControls()
        startGame()
    }

    private fun setupEngineCallbacks() {
        engine.onStateChanged = {
            runOnUiThread {
                gameView.invalidate()
                updateHud()
            }
        }

        engine.onGameOver = {
            runOnUiThread {
                handler.removeCallbacks(tickRunnable)
                handler.removeCallbacks(timerRunnable)
                showGameOver()
            }
        }

        engine.onWaveComplete = { waveNum ->
            runOnUiThread {
                // visual feedback handled by upgrade dialog or hud
            }
        }

        engine.onUpgradeNeeded = { options ->
            runOnUiThread {
                handler.removeCallbacks(tickRunnable)
                showUpgradeDialog(options)
            }
        }
    }

    private fun setupControls() {
        btnUp.setOnClickListener { engine.movePlayer(0, -1) }
        btnDown.setOnClickListener { engine.movePlayer(0, 1) }
        btnLeft.setOnClickListener { engine.movePlayer(-1, 0) }
        btnRight.setOnClickListener { engine.movePlayer(1, 0) }
        btnAttack.setOnClickListener { engine.attack() }
    }

    private fun startGame() {
        engine.start()
        handler.post(tickRunnable)
        handler.post(timerRunnable)
        updateHud()
    }

    private fun updateHud() {
        val p = engine.player
        val secs = engine.getWaveTimeRemaining() / 1000
        tvHp.text = "HP: ${p.hp}/${p.maxHp}"
        tvWave.text = "Wave: ${engine.wave}"
        tvTimer.text = "Time: ${secs}s"
        tvScore.text = "Kill: ${engine.score}"
        tvDamage.text = "ATK: ${p.damage}"
    }

    private fun showUpgradeDialog(options: List<UpgradeType>) {
        val builder = AlertDialog.Builder(this, R.style.UpgradeDialogTheme)
        builder.setTitle("🌟 Wave Complete! Choose Upgrade")
        builder.setCancelable(false)

        val items = options.map { "${it.title}\n${it.desc}" }.toTypedArray()
        builder.setItems(items) { _, which ->
            engine.selectUpgrade(options[which])
            handler.post(tickRunnable)
        }

        builder.show()
    }

    private fun showGameOver() {
        val builder = AlertDialog.Builder(this, R.style.UpgradeDialogTheme)
        builder.setTitle("💀 GAME OVER")
        builder.setMessage("Wave: ${engine.wave}\nKills: ${engine.score}")
        builder.setCancelable(false)
        builder.setPositiveButton("Restart") { _, _ ->
            startGame()
            handler.post(tickRunnable)
            handler.post(timerRunnable)
        }
        builder.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(tickRunnable)
        handler.removeCallbacks(timerRunnable)
    }
}
