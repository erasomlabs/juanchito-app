package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

class ChronoTriggerGame : Game {
    override val name = "Chrono Trigger"
    override val genre = "RPG"
    private var w = 0; private var h = 0
    private var state = 0 // 0 explore, 1 battle, 2 victory/defeat
    private var px = 0f; private var py = 0f
    private var hp = 50; private var maxHp = 50
    private var enemyHp = 30; private var enemyMaxHp = 30
    private var atb = 0f; private var enemyAtb = 0f
    private var message = "Explora. Encuentra enemigos."
    private var encounterTimer = 0f
    private var won = false
    override var isGameOver = false
    override var score = 0
    private var highScore = 0

    private val playerPaint = Paint().apply { color = Color.parseColor("#FECA57"); isAntiAlias = true }
    private val enemyPaint = Paint().apply { color = Color.parseColor("#FF6B6B"); isAntiAlias = true }
    private val textPaint = Paint().apply { color = Color.parseColor("#F5F5F5"); textSize = 24f; isAntiAlias = true; textAlign = Paint.Align.CENTER }
    private val smallPaint = Paint().apply { color = Color.parseColor("#48DBFB"); textSize = 18f; isAntiAlias = true; textAlign = Paint.Align.CENTER }
    private val hpPaint = Paint().apply { isAntiAlias = true }

    override fun init(width: Int, height: Int) { w = width; h = height; reset() }

    override fun reset() {
        px = w/2f; py = h/2f; hp = 50; maxHp = 50; enemyHp = 30; enemyMaxHp = 30
        atb = 0f; enemyAtb = 0f; state = 0; message = "Explora. Encuentra enemigos."
        encounterTimer = 3f; score = 0; isGameOver = false; won = false
    }

    override fun update(dt: Float, input: GameInput) {
        if (isGameOver) { if (input.start) reset(); return }
        if (state == 0) {
            // explore
            if (input.up) py -= 120f * dt
            if (input.down) py += 120f * dt
            if (input.left) px -= 120f * dt
            if (input.right) px += 120f * dt
            px = px.coerceIn(40f, w - 40f); py = py.coerceIn(40f, h - 40f)
            encounterTimer -= dt
            if (encounterTimer <= 0f) {
                state = 1; enemyHp = 20 + Random.nextInt(20); enemyMaxHp = enemyHp
                atb = 0f; enemyAtb = 0f; message = "¡Enemigo aparece!"
                encounterTimer = 5f
            }
        } else if (state == 1) {
            atb = (atb + dt * 0.5f).coerceAtMost(1f)
            enemyAtb = (enemyAtb + dt * 0.4f).coerceAtMost(1f)
            if (input.a && atb >= 1f) {
                // attack
                val dmg = 8 + Random.nextInt(8)
                enemyHp -= dmg; atb = 0f; message = "Atacas por $dmg"
                if (enemyHp <= 0) { score += 20; if (score > highScore) highScore = score; state = 0; message = "¡Victoria!"; encounterTimer = 4f; if (score >= 100) { won = true; isGameOver = true } }
            }
            if (input.b && atb >= 1f) {
                // heal
                hp = (hp + 12).coerceAtMost(maxHp); atb = 0f; message = "Te curas +12"
            }
            if (enemyAtb >= 1f) {
                val dmg = 5 + Random.nextInt(8)
                if (Random.nextFloat() < 0.3f) { message = "Enemigo falla" } else {
                    hp -= dmg; message = "Enemigo ataca por $dmg"
                }
                enemyAtb = 0f
                if (hp <= 0) { isGameOver = true; won = false }
            }
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#1A1A2E"))
        if (state == 0) {
            // map: simple grid
            val gridPaint = Paint().apply { color = Color.parseColor("#2D3748"); strokeWidth = 1f }
            var x = 0f
            while (x < w) { canvas.drawLine(x, 0f, x, h.toFloat(), gridPaint); x += 40f }
            var y = 0f
            while (y < h) { canvas.drawLine(0f, y, w.toFloat(), y, gridPaint); y += 40f }
            canvas.drawCircle(px, py, 12f, playerPaint)
            textPaint.textSize = 20f
            canvas.drawText(message, w/2f, h - 30f, textPaint)
        } else {
            // battle
            canvas.drawCircle(w * 0.25f, h * 0.4f, 30f, playerPaint)
            canvas.drawCircle(w * 0.75f, h * 0.4f, 30f, enemyPaint)
            // HP bars
            hpPaint.color = Color.parseColor("#55EFC4")
            canvas.drawRect(w*0.1f, h*0.7f, w*0.1f + (w*0.35f)*(hp/maxHp.toFloat()), h*0.7f+20f, hpPaint)
            hpPaint.color = Color.parseColor("#2D3748")
            canvas.drawRect(w*0.1f + (w*0.35f)*(hp/maxHp.toFloat()), h*0.7f, w*0.45f, h*0.7f+20f, hpPaint)
            hpPaint.color = Color.parseColor("#FF6B6B")
            canvas.drawRect(w*0.55f, h*0.7f, w*0.55f + (w*0.35f)*(enemyHp/enemyMaxHp.toFloat()), h*0.7f+20f, hpPaint)
            hpPaint.color = Color.parseColor("#2D3748")
            canvas.drawRect(w*0.55f + (w*0.35f)*(enemyHp/enemyMaxHp.toFloat()), h*0.7f, w*0.9f, h*0.7f+20f, hpPaint)
            textPaint.textSize = 20f
            canvas.drawText("HP: $hp/$maxHp", w*0.275f, h*0.7f - 8f, textPaint)
            canvas.drawText("HP: $enemyHp/$enemyMaxHp", w*0.725f, h*0.7f - 8f, textPaint)
            // ATB bars
            hpPaint.color = Color.parseColor("#48DBFB")
            canvas.drawRect(w*0.1f, h*0.78f, w*0.1f + (w*0.35f)*atb, h*0.78f+10f, hpPaint)
            canvas.drawRect(w*0.55f, h*0.78f, w*0.55f + (w*0.35f)*enemyAtb, h*0.78f+10f, hpPaint)
            // message
            canvas.drawText(message, w/2f, h*0.6f, textPaint)
            smallPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("A: Atacar   B: Curar", 20f, h - 20f, smallPaint)
            smallPaint.textAlign = Paint.Align.CENTER
        }
        // HUD score
        textPaint.textSize = 22f
        canvas.drawText("Score: $score", w/2f, 30f, textPaint)
        if (isGameOver) {
            val msg = if (won) "¡VICTORIA!" else "GAME OVER"
            val col = if (won) Color.parseColor("#55EFC4") else Color.parseColor("#FF6B6B")
            canvas.drawText(msg, w/2f, h/2f, textPaint.apply { textSize = 56f; color = col })
            canvas.drawText("Pulsa START", w/2f, h/2f + 60f, smallPaint.apply { textSize = 24f; color = Color.parseColor("#F5F5F5") })
        }
    }
}