package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

class MortalKombatGame : Game {
    override val name = "Pixel Kombat"
    override val genre = "Lucha"
    private var w = 0; private var h = 0
    private val groundY get() = h * 0.85f
    private var p1x = 0f; private var p1y = 0f
    private var p2x = 0f; private var p2y = 0f
    private var p1hp = 100; private var p2hp = 100
    private var p1blocking = false; private var p2blocking = false
    private var p1Cooldown = 0f; private var p2Cooldown = 0f
    private var p1Stun = 0f; private var p2Stun = 0f
    private var round = 1; private val rounds = 2
    private var p1Wins = 0; private var p2Wins = 0
    private var roundTimer = 60f
    override var isGameOver = false
    override var score = 0
    private var highScore = 0
    private var won = false

    private val p1Paint = Paint().apply { color = Color.parseColor("#FECA57"); isAntiAlias = true }
    private val p2Paint = Paint().apply { color = Color.parseColor("#FF6B6B"); isAntiAlias = true }
    private val groundPaint = Paint().apply { color = Color.parseColor("#2D3748"); isAntiAlias = true }
    private val textPaint = Paint().apply { color = Color.parseColor("#F5F5F5"); textSize = 28f; isAntiAlias = true; textAlign = Paint.Align.CENTER }
    private val smallPaint = Paint().apply { color = Color.parseColor("#48DBFB"); textSize = 20f; isAntiAlias = true; textAlign = Paint.Align.CENTER }
    private val hpPaint = Paint().apply { isAntiAlias = true }

    override fun init(width: Int, height: Int) { w = width; h = height; reset() }

    override fun reset() {
        p1x = w * 0.25f; p1y = groundY - 50f
        p2x = w * 0.75f; p2y = groundY - 50f
        p1hp = 100; p2hp = 100
        p1Cooldown = 0f; p2Cooldown = 0f; p1Stun = 0f; p2Stun = 0f
        p1blocking = false; p2blocking = false
        round = 1; p1Wins = 0; p2Wins = 0; roundTimer = 60f
        score = 0; isGameOver = false; won = false
    }

    override fun update(dt: Float, input: GameInput) {
        if (isGameOver) { if (input.start) reset(); return }
        roundTimer -= dt
        if (roundTimer <= 0f) { // time out: higher hp wins
            if (p1hp >= p2hp) { p1Wins++ } else { p2Wins++ }
            startNextRound(); return
        }
        p1Cooldown = (p1Cooldown - dt).coerceAtLeast(0f)
        p2Cooldown = (p2Cooldown - dt).coerceAtLeast(0f)
        p1Stun = (p1Stun - dt).coerceAtLeast(0f)
        p2Stun = (p2Stun - dt).coerceAtLeast(0f)
        // player 1 controls
        if (p1Stun <= 0f) {
            if (input.left) p1x -= 200f * dt
            if (input.right) p1x += 200f * dt
            p1x = p1x.coerceIn(20f, w/2f - 20f)
            p1blocking = input.down
            if (input.a && p1Cooldown <= 0f && !p1blocking) { // punch
                p1Cooldown = 0.4f
                if (kotlin.math.abs(p1x - p2x) < 80f && !p2blocking) { p2hp -= 8; p2Stun = 0.3f }
                else if (kotlin.math.abs(p1x - p2x) < 80f && p2blocking) { /* blocked */ }
            }
            if (input.b && p1Cooldown <= 0f && !p1blocking) { // kick
                p1Cooldown = 0.5f
                if (kotlin.math.abs(p1x - p2x) < 100f && !p2blocking) { p2hp -= 12; p2Stun = 0.4f }
            }
        }
        // AI for player 2
        if (p2Stun <= 0f) {
            if (p2x > p1x + 60f) p2x -= 120f * dt
            else if (p2x < p1x - 60f) p2x += 120f * dt
            p2x = p2x.coerceIn(w/2f + 20f, w - 20f)
            p2blocking = Random.nextFloat() < 0.3f && kotlin.math.abs(p2x - p1x) < 90f
            if (p2Cooldown <= 0f && !p2blocking && kotlin.math.abs(p2x - p1x) < 90f) {
                if (Random.nextFloat() < 0.5f) {
                    p2Cooldown = 0.5f
                    if (!p1blocking) { p1hp -= 10; p1Stun = 0.35f }
                } else {
                    p2Cooldown = 0.6f
                    if (!p1blocking) { p1hp -= 14; p1Stun = 0.45f }
                }
            }
        }
        // check round end
        if (p1hp <= 0 || p2hp <= 0) {
            if (p1hp > p2hp) p1Wins++ else p2Wins++
            startNextRound()
        }
    }

    private fun startNextRound() {
        if (p1Wins >= rounds || p2Wins >= rounds) {
            won = p1Wins >= rounds
            isGameOver = true
            score = p1Wins * 100 + (if (won) 500 else 0)
            if (score > highScore) highScore = score
            return
        }
        round++
        p1x = w * 0.25f; p2x = w * 0.75f
        p1hp = 100; p2hp = 100
        p1Stun = 0f; p2Stun = 0f
        roundTimer = 60f
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#1A1A2E"))
        canvas.drawRect(0f, groundY, w.toFloat(), h.toFloat(), groundPaint)
        // fighters
        val p1h = if (p1blocking) 30f else 50f
        canvas.drawRoundRect(RectF(p1x - 12f, (if (p1blocking) groundY - 30f else p1y), p1x + 12f, groundY), 4f, 4f, p1Paint)
        val p2h = if (p2blocking) 30f else 50f
        canvas.drawRoundRect(RectF(p2x - 12f, (if (p2blocking) groundY - 30f else p2y), p2x + 12f, groundY), 4f, 4f, p2Paint)
        // HP bars
        hpPaint.color = Color.parseColor("#FF6B6B")
        canvas.drawRect(20f, 20f, 20f + (w/2f - 40f) * (p1hp / 100f), 40f, hpPaint)
        canvas.drawRect(w - 20f - (w/2f - 40f) * (p2hp / 100f), 20f, w - 20f, 40f, hpPaint)
        hpPaint.color = Color.parseColor("#2D3748")
        canvas.drawRect(20f + (w/2f - 40f)*(p1hp/100f), 20f, w/2f - 20f, 40f, hpPaint)
        canvas.drawRect(w/2f + 20f, 20f, w - 20f - (w/2f - 40f)*(p2hp/100f), 40f, hpPaint)
        // HUD
        textPaint.textSize = 22f
        canvas.drawText("Ronda $round  P1: $p1Wins  P2: $p2Wins  Tiempo: ${roundTimer.toInt()}", w/2f, 70f, textPaint)
        smallPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("A: Puño  B: Patada  Abajo: Bloqueo", 20f, h - 20f, smallPaint)
        smallPaint.textAlign = Paint.Align.CENTER
        if (isGameOver) {
            val msg = if (won) "¡VICTORIA!" else "DERROTA"
            val col = if (won) Color.parseColor("#55EFC4") else Color.parseColor("#FF6B6B")
            canvas.drawText(msg, w/2f, h/2f, textPaint.apply { textSize = 56f; color = col })
            canvas.drawText("Pulsa START", w/2f, h/2f + 60f, smallPaint.apply { textSize = 24f; color = Color.parseColor("#F5F5F5") })
        }
    }
}