package com.gridrpg.game

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var engine: GameEngine? = null

    private val paintBg = Paint().apply { color = Color.parseColor("#1a1a2e") }
    private val paintGridBorderPaint = Paint().apply {
        color = Color.parseColor("#444466")
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val paintPlayer = Paint().apply {
        color = Color.parseColor("#00e5ff")
        style = Paint.Style.FILL
    }
    private val paintPlayerBorder = Paint().apply {
        color = Color.parseColor("#ffffff")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val paintEnemy = Paint().apply {
        color = Color.parseColor("#ff4444")
        style = Paint.Style.FILL
    }
    private val paintEnemyBorder = Paint().apply {
        color = Color.parseColor("#ff8888")
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val paintHpBarBg = Paint().apply {
        color = Color.parseColor("#440000")
        style = Paint.Style.FILL
    }
    private val paintHpBar = Paint().apply {
        color = Color.parseColor("#ff4444")
        style = Paint.Style.FILL
    }
    private val paintText = Paint().apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 24f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }
    private val paintOuterTile = Paint().apply {
        color = Color.parseColor("#2a1010")
        style = Paint.Style.FILL
    }
    private val paintInnerTile = Paint().apply {
        color = Color.parseColor("#102010")
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val eng = engine ?: return

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintBg)

        val tileSize = min(width, height) / GRID_SIZE.toFloat()
        val offsetX = (width - tileSize * GRID_SIZE) / 2f
        val offsetY = (height - tileSize * GRID_SIZE) / 2f

        // Draw tiles
        for (x in 0 until GRID_SIZE) {
            for (y in 0 until GRID_SIZE) {
                val left = offsetX + x * tileSize
                val top = offsetY + y * tileSize
                val right = left + tileSize
                val bottom = top + tileSize

                val isOuter = x == 0 || x == GRID_SIZE - 1 || y == 0 || y == GRID_SIZE - 1
                canvas.drawRect(left + 1, top + 1, right - 1, bottom - 1,
                    if (isOuter) paintOuterTile else paintInnerTile)
                canvas.drawRect(left, top, right, bottom, paintGridBorderPaint)
            }
        }

        // Draw enemies
        eng.enemies.filter { it.alive }.forEach { enemy ->
            val cx = offsetX + enemy.pos.x * tileSize + tileSize / 2f
            val cy = offsetY + enemy.pos.y * tileSize + tileSize / 2f
            val r = tileSize * 0.35f

            canvas.drawCircle(cx, cy, r, paintEnemy)
            canvas.drawCircle(cx, cy, r, paintEnemyBorder)

            // HP bar
            val barW = tileSize * 0.7f
            val barH = 6f
            val barLeft = cx - barW / 2
            val barTop = cy + r + 4f
            canvas.drawRect(barLeft, barTop, barLeft + barW, barTop + barH, paintHpBarBg)
            val hpFrac = enemy.hp.toFloat() / enemy.maxHp
            canvas.drawRect(barLeft, barTop, barLeft + barW * hpFrac, barTop + barH, paintHpBar)

            val smallPaint = Paint(paintText).apply { textSize = tileSize * 0.22f }
            canvas.drawText("E", cx, cy + tileSize * 0.1f, smallPaint)
        }

        // Draw player
        if (eng.player.alive) {
            val cx = offsetX + eng.player.pos.x * tileSize + tileSize / 2f
            val cy = offsetY + eng.player.pos.y * tileSize + tileSize / 2f
            val r = tileSize * 0.38f

            canvas.drawCircle(cx, cy, r, paintPlayer)
            canvas.drawCircle(cx, cy, r, paintPlayerBorder)

            val smallPaint = Paint(paintText).apply {
                textSize = tileSize * 0.22f
                color = Color.parseColor("#003344")
            }
            canvas.drawText("P", cx, cy + tileSize * 0.1f, smallPaint)
        }
    }
}
